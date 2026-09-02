package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONRendering.ModelType;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableModelObject.TreadPreview;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Client-only render tree used to preview a vehicle at its deployment position.  Only this
 * root entity is registered with the client world.  Its vehicle and part model states are
 * detached render nodes, so constructing or displaying a preview cannot create physics,
 * collision, sound, packet, or Minecraft-entity state.
 */
public final class EntityVehiclePreview extends AEntityC_Renderable {
    private static final float PREVIEW_ALPHA = 0.45F;

    private final PreviewModel<JSONVehicle> vehicleModel;
    private final TransformationMatrix absoluteTransform = new TransformationMatrix();
    private boolean renderablesDestroyed;

    /**
     * Compatibility constructor for callers that do not have a saved stack.  This produces the
     * same default-part tree as placing a newly-created stack.
     */
    public EntityVehiclePreview(AWrapperWorld world, ItemVehicle vehicleItem, Point3D blockPosition, double placingPlayerYaw) {
        this(world, vehicleItem, vehicleItem.getNewStack(null), blockPosition, placingPlayerYaw);
    }

    /**
     * Creates a preview from the exact stack reserved for this deployment operation.
     * The preview must be registered with {@link AWrapperWorld#addEntity} rather than
     * {@link AWrapperWorld#spawnEntity} to keep it out of Minecraft's entity system.
     */
    public EntityVehiclePreview(AWrapperWorld world, ItemVehicle vehicleItem, IWrapperItemStack vehicleStack, Point3D blockPosition, double placingPlayerYaw) {
        super(world, blockPosition.copy().add(0.5D, 1.0D, 0.5D), new Point3D(), new Point3D(0.0D, placingPlayerYaw + 90.0D, 0.0D));
        if (!world.isClient()) {
            throw new IllegalArgumentException("Vehicle deployment previews may only be created in a client world.");
        }
        if (vehicleStack == null || vehicleStack.isEmpty() || vehicleStack.getItem() != vehicleItem) {
            throw new IllegalArgumentException("Vehicle deployment preview stack does not match its vehicle item.");
        }

        double vehicleScale = ConfigSystem.settings.general.packVehicleScales.value.get(vehicleItem.definition.packID);
        scale.set(vehicleScale, vehicleScale, vehicleScale);
        prevScale.set(scale);

        IWrapperNBT vehicleData = vehicleStack.getData();
        vehicleModel = new PreviewModel<>(this, vehicleItem, vehicleData, null, null);
        vehicleModel.initializeVehicleState(vehicleData);
        populateParts(vehicleModel, vehicleData);
    }

    @Override
    public boolean shouldSync() {
        return false;
    }

    @Override
    public void updateSounds(float partialTicks) {
        //Preview entities never create or update sounds.
    }

    @Override
    protected boolean disableRendering() {
        return renderablesDestroyed || super.disableRendering();
    }

    @Override
    protected void renderModel(TransformationMatrix transform, boolean blendingEnabled, float partialTicks) {
        absoluteTransform.resetTransforms();
        absoluteTransform.setTranslation(position);
        absoluteTransform.applyRotation(orientation);
        absoluteTransform.applyScaling(scale);
        vehicleModel.renderPreview(transform, absoluteTransform, position, orientation, scale, true, true, blendingEnabled, 0.0F);
    }

    @Override
    public void remove() {
        if (!renderablesDestroyed) {
            renderablesDestroyed = true;
            vehicleModel.destroyPreview();
        }
        super.remove();
    }

    private void populateParts(PreviewModel<?> parent, IWrapperNBT parentData) {
        if (!(parent.definition instanceof AJSONPartProvider)) {
            return;
        }

        AJSONPartProvider provider = (AJSONPartProvider) parent.definition;
        if (provider.parts == null) {
            return;
        }

        for (int slotIndex = 0; slotIndex < provider.parts.size(); ++slotIndex) {
            JSONPartDefinition slotDefinition = provider.parts.get(slotIndex);
            try {
                if (parentData != null) {
                    IWrapperNBT partData = parentData.getData("part_" + slotIndex);
                    if (partData != null) {
                        AItemPack<?> packItem = PackParser.getItem(partData.getString("packID"), partData.getString("systemName"), partData.getString("subName"));
                        if (packItem instanceof AItemPart) {
                            addPart(parent, slotDefinition, (AItemPart) packItem, partData, false);
                        }
                    }
                } else {
                    PreviewModel<JSONPart> addedPart = null;
                    if (slotDefinition.conditionalDefaultParts != null) {
                        for (Entry<String, String> conditionalDefault : slotDefinition.conditionalDefaultParts.entrySet()) {
                            if (parent.getOrCreateVariable(conditionalDefault.getKey()).isActive) {
                                addedPart = addDefaultPart(parent, slotDefinition, conditionalDefault.getValue());
                                break;
                            }
                        }
                    }
                    if (addedPart == null && slotDefinition.defaultPart != null) {
                        addDefaultPart(parent, slotDefinition, slotDefinition.defaultPart);
                    }
                }
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("Could not load part slot " + slotIndex + " for vehicle deployment preview " + parent + ".");
                e.printStackTrace();
            }
        }
    }

    private PreviewModel<JSONPart> addDefaultPart(PreviewModel<?> parent, JSONPartDefinition slotDefinition, String partName) {
        if (partName == null || partName.isEmpty()) {
            return null;
        }

        int separatorIndex = partName.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == partName.length() - 1) {
            InterfaceManager.coreInterface.logError("Could not parse default part '" + partName + "' for vehicle deployment preview.  Expected packID:systemName.");
            return null;
        }

        AItemPack<?> packItem = PackParser.getItem(partName.substring(0, separatorIndex), partName.substring(separatorIndex + 1));
        if (!(packItem instanceof AItemPart)) {
            InterfaceManager.coreInterface.logError("Could not find default part '" + partName + "' for vehicle deployment preview.");
            return null;
        }

        AItemPart partItem = (AItemPart) packItem;
        if (!partItem.isPartValidForPackDef(slotDefinition, parent.subDefinition, !slotDefinition.bypassSlotMinMax)) {
            return null;
        }
        return addPart(parent, slotDefinition, partItem, null, true);
    }

    private PreviewModel<JSONPart> addPart(PreviewModel<?> parent, JSONPartDefinition slotDefinition, AItemPart partItem, IWrapperNBT partData, boolean alignTone) {
        PreviewModel<JSONPart> partModel = new PreviewModel<>(this, partItem, partData, parent, slotDefinition);
        if (alignTone) {
            alignPartTone(partModel, parent, slotDefinition);
        }
        parent.children.add(partModel);
        populateParts(partModel, partData);
        return partModel;
    }

    private void alignPartTone(PreviewModel<JSONPart> partModel, PreviewModel<?> parent, JSONPartDefinition slotDefinition) {
        if (slotDefinition.toneIndex == 0) {
            return;
        }

        String partTone = getPartTone(parent.subDefinition, slotDefinition.toneIndex);
        if (partTone == null) {
            partTone = getPartTone(vehicleModel.subDefinition, slotDefinition.toneIndex);
        }
        if (partTone != null) {
            for (JSONSubDefinition subDefinition : partModel.definition.definitions) {
                if (subDefinition.subName.equals(partTone)) {
                    partModel.updateSubDefinition(partTone);
                    break;
                }
            }
        }
    }

    private static String getPartTone(JSONSubDefinition subDefinition, int toneIndex) {
        return subDefinition.partTones != null && subDefinition.partTones.size() >= toneIndex ? subDefinition.partTones.get(toneIndex - 1) : null;
    }

    /**
     * Detached definition-backed model state.  This reuses the normal model, text, light,
     * animation, and instrument render pipeline, but is never registered or ticked as an entity.
     */
    private static final class PreviewModel<JSONDefinition extends AJSONInteractableEntity> extends AEntityE_Interactable<JSONDefinition> implements TreadPreview {
        private static final String[] VEHICLE_STATE_VARIABLES = {
            "left_turn_signal", "right_turn_signal", "brake", "p_brake", "locked",
            "running_light", "headlight", "navigation_light", "strobe_light", "taxi_light", "landing_light", "horn",
            "gear_setpoint", "throttle", "reverser", "electric_usage", "batteryCapacity", "simplethrottle",
            "input_aileron", "aileron", "trim_aileron", "input_elevator", "elevator", "trim_elevator",
            "input_rudder", "rudder", "trim_rudder", "flaps_setpoint", "flaps_actual", "autopilot", "auto_level",
            "hasOpenTop", "ballastControl"
        };

        private final EntityVehiclePreview preview;
        private final PreviewModel<?> parent;
        private final JSONPartDefinition placementDefinition;
        private final List<PreviewModel<?>> children = new ArrayList<>();
        private final TransformationMatrix modelTransform = new TransformationMatrix();
        private final TransformationMatrix worldTransform = new TransformationMatrix();
        private final Point3D localOffset = new Point3D();
        private final Point3D internalOffset = new Point3D();
        private final Point3D globalScale = new Point3D(1.0D, 1.0D, 1.0D);
        private final RotationMatrix localOrientation = new RotationMatrix();
        private final RotationMatrix globalOrientation = new RotationMatrix();
        private final AnimationSwitchbox placementMovementSwitchbox;
        private final AnimationSwitchbox internalMovementSwitchbox;
        private final AnimationSwitchbox placementActiveSwitchbox;
        private final AnimationSwitchbox internalActiveSwitchbox;
        private double previewElectricPower;
        private boolean destroyed;

        private PreviewModel(EntityVehiclePreview preview, AItemSubTyped<JSONDefinition> item, IWrapperNBT data, PreviewModel<?> parent, JSONPartDefinition placementDefinition) {
            super(preview.world, InterfaceManager.clientInterface.getClientPlayer(), item, data);
            this.preview = preview;
            this.parent = parent;
            this.placementDefinition = placementDefinition;
            this.ticksExisted = 1;

            if (placementDefinition != null) {
                if (placementDefinition.constantValues != null) {
                    placementDefinition.constantValues.forEach((constantKey, constantValue) -> {
                        ComputedVariable variable = new ComputedVariable(this, constantKey);
                        variable.setTo(constantValue, false);
                        addVariable(variable);
                    });
                }

                if (placementDefinition.animations != null || placementDefinition.applyAfter != null) {
                    List<JSONAnimationDefinition> animations = new ArrayList<>();
                    if (placementDefinition.animations != null) {
                        animations.addAll(placementDefinition.animations);
                    }
                    placementMovementSwitchbox = new AnimationSwitchbox(parent, animations, placementDefinition.applyAfter, "preview part placement " + placementDefinition.types);
                } else {
                    placementMovementSwitchbox = null;
                }
                placementActiveSwitchbox = placementDefinition.activeAnimations != null ? new AnimationSwitchbox(parent, placementDefinition.activeAnimations, null) : null;

                JSONPart partDefinition = (JSONPart) definition;
                internalMovementSwitchbox = partDefinition.generic.movementAnimations != null ? new AnimationSwitchbox(this, partDefinition.generic.movementAnimations, null) : null;
                internalActiveSwitchbox = partDefinition.generic.activeAnimations != null ? new AnimationSwitchbox(this, partDefinition.generic.activeAnimations, null) : null;
                getOrCreateVariable("part_isExterior").setActive(placementDefinition.isExterior, false);
                getOrCreateVariable("part_active").setActive(true, false);
            } else {
                placementMovementSwitchbox = null;
                internalMovementSwitchbox = null;
                placementActiveSwitchbox = null;
                internalActiveSwitchbox = null;
            }
        }

        private void initializeVehicleState(IWrapperNBT data) {
            JSONVehicle vehicleDefinition = (JSONVehicle) definition;
            for (String variableName : VEHICLE_STATE_VARIABLES) {
                addVariable(new ComputedVariable(this, variableName, data));
            }

            setStaticVariable("steeringForceIgnoresSpeed", vehicleDefinition.motorized.steeringForceIgnoresSpeed ? 1.0D : 0.0D);
            setStaticVariable("steeringForceFactor", vehicleDefinition.motorized.steeringForceFactor);
            setStaticVariable("brakingFactor", vehicleDefinition.motorized.brakingFactor);
            setStaticVariable("climbSpeed", vehicleDefinition.motorized.climbSpeed != 0.0D ? vehicleDefinition.motorized.climbSpeed : ConfigSystem.settings.general.climbSpeed.value);
            setStaticVariable("overSteer", vehicleDefinition.motorized.overSteer);
            setStaticVariable("underSteer", vehicleDefinition.motorized.underSteer);
            setStaticVariable("maxTiltAngle", vehicleDefinition.motorized.maxTiltAngle);
            setStaticVariable("hasSkidSteer", vehicleDefinition.motorized.hasSkidSteer ? 1.0D : 0.0D);
            setStaticVariable("batteryCapacity", vehicleDefinition.motorized.batteryCapacity);
            setStaticVariable("aileronArea", vehicleDefinition.motorized.aileronArea);
            setStaticVariable("elevatorArea", vehicleDefinition.motorized.elevatorArea);
            setStaticVariable("rudderArea", vehicleDefinition.motorized.rudderArea);
            setStaticVariable("wingArea", vehicleDefinition.motorized.wingArea);
            setStaticVariable("wingSpan", vehicleDefinition.motorized.wingSpan);
            setStaticVariable("dragCoefficient", vehicleDefinition.motorized.dragCoefficient);
            setStaticVariable("ballastVolume", vehicleDefinition.motorized.ballastVolume);
            setStaticVariable("waterBallastFactor", vehicleDefinition.motorized.waterBallastFactor);
            setStaticVariable("gravityFactor", vehicleDefinition.motorized.gravityFactor != 0.0D ? vehicleDefinition.motorized.gravityFactor : vehicleDefinition.motorized.isAircraft ? 1.0D : ConfigSystem.settings.general.gravityFactor.value);
            setStaticVariable("axleRatio", vehicleDefinition.motorized.axleRatio);
            getOrCreateVariable("aileron").setTo(getOrCreateVariable("input_aileron").currentValue, false);
            getOrCreateVariable("elevator").setTo(getOrCreateVariable("input_elevator").currentValue, false);
            getOrCreateVariable("rudder").setTo(getOrCreateVariable("input_rudder").currentValue, false);
            getOrCreateVariable("hasOpenTop").setActive(vehicleDefinition.motorized.hasOpenTop, false);
            getOrCreateVariable("ballastControl").setTo(getOrCreateVariable("input_elevator").currentValue, false);
            previewElectricPower = data != null ? data.getDouble("electricPower") : vehicleDefinition.motorized.batteryCapacity * AEntityVehicleE_Powered.BATTERY_DEFAULT_CHARGE;
        }

        private void setStaticVariable(String variableName, double value) {
            ComputedVariable variable = new ComputedVariable(this, variableName);
            variable.setTo(value, false);
            addVariable(variable);
        }

        private void renderPreview(TransformationMatrix parentModelTransform, TransformationMatrix parentWorldTransform, Point3D parentPosition, RotationMatrix parentOrientation, Point3D parentScale, boolean parentVisible, boolean parentActive, boolean blendingEnabled, float partialTicks) {
            boolean visible = parentVisible;
            boolean active = parentActive;
            modelTransform.set(parentModelTransform);
            worldTransform.set(parentWorldTransform);

            if (placementDefinition == null) {
                position.set(parentPosition);
                orientation.set(parentOrientation);
                globalScale.set(parentScale);
            } else {
                localOffset.set(placementDefinition.pos);
                JSONPart partDefinition = (JSONPart) definition;
                if (partDefinition.generic.slotOffset != null) {
                    localOffset.add(partDefinition.generic.slotOffset);
                }
                localOrientation.setToZero();

                if (visible && placementMovementSwitchbox != null) {
                    visible = placementMovementSwitchbox.runSwitchbox(partialTicks, false);
                    if (visible) {
                        localOffset.transform(placementMovementSwitchbox.netMatrix);
                        localOrientation.multiply(placementMovementSwitchbox.rotation);
                    }
                }
                if (placementDefinition.rot != null) {
                    localOrientation.multiply(placementDefinition.rot);
                }

                globalScale.set(parentScale);
                if (placementDefinition.partScale != null) {
                    globalScale.multiply(placementDefinition.partScale);
                }

                if (visible && internalMovementSwitchbox != null) {
                    visible = internalMovementSwitchbox.runSwitchbox(partialTicks, false);
                    if (visible) {
                        internalOffset.set(internalMovementSwitchbox.translation).multiply(globalScale).rotate(localOrientation);
                        if (parentScale.x != 0.0D) internalOffset.x /= parentScale.x;
                        if (parentScale.y != 0.0D) internalOffset.y /= parentScale.y;
                        if (parentScale.z != 0.0D) internalOffset.z /= parentScale.z;
                        localOffset.add(internalOffset);
                        localOrientation.multiply(internalMovementSwitchbox.rotation);
                    }
                }

                if (active && placementActiveSwitchbox != null) {
                    active = placementActiveSwitchbox.runSwitchbox(partialTicks, false);
                }
                if (active && internalActiveSwitchbox != null) {
                    active = internalActiveSwitchbox.runSwitchbox(partialTicks, false);
                }
                getOrCreateVariable("part_active").setActive(active, false);

                modelTransform.applyTranslation(localOffset);
                modelTransform.applyRotation(localOrientation);
                worldTransform.applyTranslation(localOffset);
                worldTransform.applyRotation(localOrientation);
                if (placementDefinition.partScale != null) {
                    modelTransform.applyScaling(placementDefinition.partScale);
                    worldTransform.applyScaling(placementDefinition.partScale);
                }

                position.set(0.0D, 0.0D, 0.0D);
                worldTransform.transform(position);
                globalOrientation.set(parentOrientation).multiply(localOrientation);
                orientation.set(globalOrientation);
            }

            scale.set(globalScale);
            prevPosition.set(position);
            prevOrientation.set(orientation);
            prevScale.set(scale);
            worldLightValue = preview.worldLightValue;

            if (visible && definition.rendering != null && definition.rendering.modelType != ModelType.NONE) {
                super.renderModel(modelTransform, blendingEnabled, partialTicks);
            }
            for (PreviewModel<?> child : children) {
                child.renderPreview(modelTransform, worldTransform, position, orientation, globalScale, visible, active, blendingEnabled, partialTicks);
            }
        }

        private void destroyPreview() {
            if (!destroyed) {
                destroyed = true;
                children.forEach(PreviewModel::destroyPreview);
                resetModelsAndAnimations();
                for (List<RenderableData> renderables : instrumentRenderables) {
                    if (renderables != null) {
                        renderables.stream().filter(renderable -> renderable != null).forEach(RenderableData::destroy);
                    }
                }
                isValid = false;
            }
        }

        @Override
        public boolean shouldSync() {
            return false;
        }

        @Override
        public float getRenderAlpha() {
            return PREVIEW_ALPHA;
        }

        @Override
        public void spawnParticles(float partialTicks) {
            //Detached preview nodes never spawn particles.
        }

        @Override
        public void updateSounds(float partialTicks) {
            //Detached preview nodes never create or update sounds.
        }

        @Override
        public String getTexture() {
            return placementDefinition != null && subDefinition.useVehicleTexture ? preview.vehicleModel.getTexture() : super.getTexture();
        }

        @Override
        public boolean renderTextLit() {
            return placementDefinition != null ? preview.vehicleModel.renderTextLit() : super.renderTextLit();
        }

        @Override
        public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
            if (definition instanceof JSONVehicle) {
                JSONVehicle vehicleDefinition = (JSONVehicle) definition;
                switch (variable) {
                    case "yaw":
                        return new ComputedVariable(this, variable, partialTicks -> orientation.angles.y, false);
                    case "pitch":
                        return new ComputedVariable(this, variable, partialTicks -> orientation.angles.x, false);
                    case "roll":
                        return new ComputedVariable(this, variable, partialTicks -> orientation.angles.z, false);
                    case "speed_factor":
                        return new ComputedVariable(this, variable, partialTicks -> (vehicleDefinition.motorized.isAircraft ? ConfigSystem.settings.general.aircraftSpeedFactor.value : ConfigSystem.settings.general.carSpeedFactor.value) * ConfigSystem.settings.general.packSpeedFactors.value.get(vehicleDefinition.packID), false);
                    case "autopilot_present":
                        return new ComputedVariable(this, variable, partialTicks -> vehicleDefinition.motorized.hasAutopilot ? 1.0D : 0.0D, false);
                    case "electric_power":
                        return new ComputedVariable(this, variable, partialTicks -> previewElectricPower, false);
                    case "door":
                        return new ComputedVariable(this, variable, partialTicks -> getOrCreateVariable("p_brake").isActive ? 1.0D : 0.0D, false);
                    case "gear_present":
                        return new ComputedVariable(this, variable, partialTicks -> vehicleDefinition.motorized.gearSequenceDuration != 0 ? 1.0D : 0.0D, false);
                    default:
                        break;
                }
            }

            if (parent != null) {
                if (variable.startsWith("vehicle_")) {
                    return preview.vehicleModel != null ? preview.vehicleModel.getOrCreateVariable(variable.substring("vehicle_".length())) : new ComputedVariable(false);
                }
                if (variable.startsWith("parent_")) {
                    return parent.getOrCreateVariable(variable.substring("parent_".length()));
                }
                switch (variable) {
                    case "part_present":
                        return new ComputedVariable(true);
                    case "part_ismirrored":
                        return new ComputedVariable(isMirrored());
                    case "part_isonfront":
                        return new ComputedVariable(placementDefinition.pos.z > 0.0D);
                    case "part_isspare":
                        return new ComputedVariable(isSpare());
                    case "part_onvehicle":
                        return new ComputedVariable(true);
                    case "part_added_vehicle":
                    case "part_removed_vehicle":
                    case "part_added_ground":
                    case "part_removed_ground":
                        return new ComputedVariable(false);
                    default:
                        ComputedVariable localVariable = super.createComputedVariable(variable, false);
                        if (localVariable != null) {
                            return localVariable;
                        }
                        PreviewModel<?> testParent = parent;
                        while (testParent != null) {
                            if (testParent.containsVariable(variable)) {
                                return testParent.getOrCreateVariable(variable);
                            }
                            ComputedVariable parentVariable = testParent.createComputedVariable(variable, false);
                            if (parentVariable != null) {
                                return parentVariable;
                            }
                            testParent = testParent.parent;
                        }
                        break;
                }
            }
            return super.createComputedVariable(variable, createDefaultIfNotPresent);
        }

        private boolean isMirrored() {
            return placementDefinition != null && (placementDefinition.isMirrored || parent != null && parent.isMirrored());
        }

        private boolean isSpare() {
            return placementDefinition != null && (placementDefinition.isSpare || parent != null && parent.isSpare());
        }

        @Override
        public boolean isTreadPreview() {
            return placementDefinition != null && ((JSONPart) definition).ground != null && ((JSONPart) definition).ground.isTread && !isSpare();
        }

        @Override
        public AJSONPartProvider getTreadParentDefinition() {
            return (AJSONPartProvider) parent.definition;
        }

        @Override
        public JSONPartDefinition getTreadPlacementDefinition() {
            return placementDefinition;
        }

        @Override
        public JSONPart getTreadDefinition() {
            return (JSONPart) definition;
        }

        @Override
        public Point3D getTreadLocalOffset() {
            return localOffset;
        }

        @Override
        public Point3D getTreadParentScale() {
            return parent.scale;
        }

        @Override
        public boolean isTreadAttachedToPart() {
            return parent.placementDefinition != null;
        }

        @Override
        public Object getTreadParentDescription() {
            return parent;
        }
    }
}
