package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityCameraChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * This class is the base for all parts and should be extended for any entity-compatible parts.
 * Use {@link AEntityF_Multipart#addPart(APart, boolean)} to add parts
 * and {@link AEntityF_Multipart#removePart(APart, boolean, boolean)} to remove them.
 * You may extend {@link AEntityF_Multipart} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 *
 * @author don_bruce
 */
public abstract class APart extends AEntityF_Multipart<JSONPart> {

    //JSON properties.
    public final JSONPartDefinition placementDefinition;
    public final int placementSlot;

    //Instance properties.
    /**
     * The entity this part has been placed on,  can be a vehicle or a part.
     */
    public final AEntityF_Multipart<?> entityOn;
    /**
     * The top-most entity for this part.  May be the {@link #entityOn} if the part is only one level deep.
     */
    public final AEntityF_Multipart<?> masterEntity;
    /**
     * The vehicle this part has been placed on.  Identical to {@link #masterEntity}, just saves a cast.
     * Will be null, however, if this part isn't on a vehicle (say if it's on a decor).
     */
    public final EntityVehicleF_Physics vehicleOn;
    /**
     * The part this part is on, or null if it's on a base entity.
     */
    public final APart partOn;
    /**
     * All linked parts for this part.  Updated whenever the part set changes.
     */
    public final List<APart> linkedParts = new ArrayList<>();

    public boolean isInvisible = false;
    public boolean isActive = true;
    public boolean isPermanent = false;
    public final boolean isMoveable;
    public final boolean turnsWithSteer;
    public final boolean isSpare;
    public final boolean isMirrored;
    private boolean requestedForcedCamera;
    private final ComputedVariable newlyAddedVar;
    public final ComputedVariable isExteriorVar;

    /**
     * The local offset from this part, to the master entity.  This may not be the offset from the part to the entity it is
     * on if the entity is a part itself.
     */
    public final Point3D localOffset;
    public final RotationMatrix localOrientation;
    public final RotationMatrix zeroReferenceOrientation;
    public final RotationMatrix prevZeroReferenceOrientation;
    public final Point3D externalAnglesRotated = new Point3D();
    private final AnimationSwitchbox placementActiveSwitchbox;
    private final AnimationSwitchbox internalActiveSwitchbox;
    private final AnimationSwitchbox placementMovementSwitchbox;
    private final AnimationSwitchbox internalMovementSwitchbox;

    public APart(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, AItemPart item, IWrapperNBT data) {
        super(entityOn.world, placingPlayer, item, data);
        //Init locals.
        this.localOffset = placementDefinition.pos.copy();
        this.localOrientation = new RotationMatrix();
        this.zeroReferenceOrientation = new RotationMatrix();
        this.prevZeroReferenceOrientation = new RotationMatrix();

        //Add slot definition constants.
        if (placementDefinition.constantValues != null) {
            placementDefinition.constantValues.forEach((constantKey, constantValue) -> {
                ComputedVariable newVariable = new ComputedVariable(this, constantKey);
                newVariable.setTo(constantValue, false);
                addVariable(newVariable);
            });
        }

        //Set initial position, rotation, and scale.  This ensures part doesn't "warp" the first tick.
        //Note that this isn't exact, as we can't calculate the exact locals until after the first tick
        //when we initialize all of our animations.
        position.set(localOffset).rotate(entityOn.orientation).add(entityOn.position);
        prevPosition.set(position);
        orientation.set(entityOn.orientation);
        if (placementDefinition.rot != null) {
            orientation.multiply(placementDefinition.rot);
        }
        prevOrientation.set(orientation);
        scale.set(entityOn.scale);
        if (placementDefinition.partScale != null) {
            scale.multiply(placementDefinition.partScale);
        }
        prevScale.set(scale);

        //Set switchbox animation properties.
        //Placement movement needs to take into account applyAfter animations.
        if (placementDefinition.animations != null || placementDefinition.applyAfter != null) {
            List<JSONAnimationDefinition> animations = new ArrayList<>();
            if (placementDefinition.animations != null) {
                animations.addAll(placementDefinition.animations);
            }
            this.placementMovementSwitchbox = new AnimationSwitchbox(entityOn, animations, placementDefinition.applyAfter);
        } else {
            this.placementMovementSwitchbox = null;
        }
        this.internalMovementSwitchbox = definition.generic.movementAnimations != null ? new AnimationSwitchbox(this, definition.generic.movementAnimations, null) : null;
        this.placementActiveSwitchbox = placementDefinition.activeAnimations != null ? new AnimationSwitchbox(entityOn, placementDefinition.activeAnimations, null) : null;
        this.internalActiveSwitchbox = definition.generic.activeAnimations != null ? new AnimationSwitchbox(this, definition.generic.activeAnimations, null) : null;

        //Now set entity properties.  These care about the other things above.
        this.entityOn = entityOn;
        AEntityF_Multipart<?> parentEntity = entityOn;
        while (parentEntity instanceof APart) {
            parentEntity = ((APart) parentEntity).entityOn;
        }
        this.masterEntity = parentEntity;
        this.vehicleOn = parentEntity instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) parentEntity : null;
        this.partOn = entityOn instanceof APart ? (APart) entityOn : null;
        this.placementDefinition = placementDefinition;
        this.placementSlot = entityOn.definition.parts.indexOf(placementDefinition);

        this.isMoveable = placementMovementSwitchbox != null || internalMovementSwitchbox != null;
        this.turnsWithSteer = placementDefinition.turnsWithSteer || (partOn != null && partOn.turnsWithSteer);
        this.isSpare = placementDefinition.isSpare || (partOn != null && partOn.isSpare);
        this.isMirrored = placementDefinition.isMirrored || (partOn != null && partOn.isMirrored);

        addVariable(this.newlyAddedVar = new ComputedVariable(this, "newlyAdded", data));
        addVariable(this.isExteriorVar = new ComputedVariable(this, "part_isExterior", data));
        if (placingPlayer != null) {
            newlyAddedVar.setActive(true, false);
        }
    }

    @Override
    public void update() {
        super.update();
        if (ticksExisted == 5) {
            newlyAddedVar.setActive(false, false);
        }

        world.beginProfiling("PartAlignment", true);
        isInvisible = partOn != null ? partOn.isInvisible : false;
        
        //Update forced camera mode if we are supposed to be forcing it.
        //We need to one-shot this though to ensure that we don't sent infinite packets.
        world.beginProfiling("CameraModeCheck", true);
        if (!requestedForcedCamera && placementDefinition.forceCameras && world.isClient() && riderIsClient && activeCamera == null && InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCameraChange(this));
        }else if(requestedForcedCamera && activeCamera != null) {
        	requestedForcedCamera = false;
        }

        //Update active state.
        world.beginProfiling("ActiveStateCheck", false);
        isActive = partOn != null ? partOn.isActive : true;
        if (isActive && placementActiveSwitchbox != null) {
            isActive = placementActiveSwitchbox.runSwitchbox(0, false);
        }
        if (isActive && internalActiveSwitchbox != null) {
            isActive = internalActiveSwitchbox.runSwitchbox(0, false);
        }
        if (!isActive && rider != null) {
            //Kick out rider from inactive seat.
            removeRider();
        }

        //Set initial offsets.
        world.beginProfiling("BaseStateCheck", false);
        motion.set(entityOn.motion);
        position.set(entityOn.position);
        orientation.set(entityOn.orientation);
        localOffset.set(placementDefinition.pos);
        if (definition.generic.slotOffset != null) {
            localOffset.add(definition.generic.slotOffset);
        }
        
        //Update permanent-ness
        isPermanent = placementDefinition.lockingVariables != null ? !isVariableListTrue(placementDefinition.lockingVariables) : placementDefinition.isPermanent;

        //Update zero-reference.
        prevZeroReferenceOrientation.set(zeroReferenceOrientation);
        if (partOn != null) {
            zeroReferenceOrientation.set(partOn.zeroReferenceOrientation);
        } else {
            zeroReferenceOrientation.set(entityOn.orientation);
        }
        if (placementDefinition.rot != null) {
            zeroReferenceOrientation.multiply(placementDefinition.rot);
        }

        //Init orientation.
        localOrientation.setToZero();

        //First apply part slot animation translation and rotation.
        //This will rotate us to our proper slot position.
        world.beginProfiling("SwitchboxCheck", false);
        if (!isInvisible && placementMovementSwitchbox != null) {
            isInvisible = !placementMovementSwitchbox.runSwitchbox(0, false);
            //Offset needs to move according to full transform.
            //This is because these coords are from what we are on.
            //Orientation just needs to update according to new rotation.
            localOffset.transform(placementMovementSwitchbox.netMatrix);
            localOrientation.multiply(placementMovementSwitchbox.rotation);
            externalAnglesRotated.set(placementMovementSwitchbox.rotation.convertToAngles());
        } else {
            externalAnglesRotated.set(0, 0, 0);
        }

        //Now rotate us to face the slot's requested orientation.
        world.beginProfiling("SwitchboxApply", false);
        if (placementDefinition.rot != null) {
            localOrientation.multiply(placementDefinition.rot);
        }

        //Now scale.  Needs to happen after placement operations to scale their transforms properly.
        scale.set(entityOn.scale);
        localOffset.multiply(scale);
        if (placementDefinition.partScale != null) {
            scale.multiply(placementDefinition.partScale);
        }

        //Finally, apply our internal translation and rotation.
        if (internalMovementSwitchbox != null) {
            isInvisible = !internalMovementSwitchbox.runSwitchbox(0, false) || isInvisible;
            //Offset here, to apply to locals, needs to be multiplied by scale and local orientation.
            //If we don't do this, then we won't calculate the locals right.
            localOffset.add(internalMovementSwitchbox.translation.multiply(scale).rotate(localOrientation));
            localOrientation.multiply(internalMovementSwitchbox.rotation);
        }

        //Set global position to reflect new local position.
        world.beginProfiling("AlignmentApply", false);
        Point3D localPositionDelta = new Point3D().set(localOffset).rotate(orientation);
        position.add(localPositionDelta);
        orientation.multiply(localOrientation);

        //Adjust localOffset to align with actual local offset.  This happens if we are a sub-part.
        if (partOn != null) {
            localOffset.reOrigin(partOn.localOrientation).add(partOn.localOffset);
        }

        //Update bounding box, as scale changes width/height.
        boundingBox.widthRadius = getWidth() / 2D * scale.x;
        boundingBox.heightRadius = getHeight() / 2D * scale.y;
        boundingBox.depthRadius = getWidth() / 2D * scale.z;
        world.endProfiling();
        world.endProfiling();
    }

    @Override
    public EntityAutoUpdateTime getUpdateTime() {
        //Sync with what were are on.
        return entityOn.getUpdateTime();
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return entityOn.requiresDeltaUpdates() || isMoveable || super.requiresDeltaUpdates();
    }

    @Override
    public void setVariableDefaults() {
        super.setVariableDefaults();
        isExteriorVar.setActive(placementDefinition.isExterior, false);
    }

    @Override
    protected void updateCollisionBoxes(boolean requiresDeltaUpdates) {
        //Add collision if we aren't a fake part.
        if (!isFake()) {
            super.updateCollisionBoxes(requiresDeltaUpdates);
            collisionBoxes.add(boundingBox);
        }
    }

    @Override
    public void attack(Damage damage) {
        //Check if we can be removed by this attack.
        if (!isPermanent && definition.generic.canBeRemovedByHand && damage.isHand) {
            //Attacked a removable part, remove us to the player's inventory.
            //If the inventory can't fit us, don't remove us.
            IWrapperPlayer player = (IWrapperPlayer) damage.entityResponsible;
            if (vehicleOn != null && vehicleOn.lockedVar.isActive) {
                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
            } else {
            	LanguageEntry partResult = checkForRemoval(player);
            	if(partResult != null) {
            		player.sendPacket(new PacketPlayerChatMessage(player, partResult));
            		return;
            	}else if (!player.getInventory().addStack(getStack())) {
                	world.spawnItemStack(getStack(), position, null);
                }
                entityOn.removePart(this, false, true);
            }
        } else {
            //Not a removable part, or is an actual attack.
            super.attack(damage);
            if (definition.generic.forwardsDamageMultiplier != 0) {
                //Need to re-create damage object to use on entity.  Use null for box since we want to hurt the core entity.
                masterEntity.attack(new Damage(damage, definition.generic.forwardsDamageMultiplier, null));
            }
            if (outOfHealth && definition.generic.destroyable) {
                destroy(damage.box);
                world.spawnExplosion(position, 0F, false, false);
            }
        }
    }

    @Override
    public void destroy(BoundingBox box) {
        //Don't remove normally, remove as a part.
        entityOn.removePart(this, true, true);
    }

    @Override
    public void updatePartList() {
        super.updatePartList();
        linkedParts.clear();
        addLinkedPartsToList(linkedParts, APart.class);
    }

    @Override
    public double getMass() {
        return super.getMass() + definition.generic.mass;
    }

    @Override
    public boolean shouldSavePosition() {
        return false;
    }

    @Override
    public boolean canBeClicked() {
        return entityOn.isVariableListTrue(placementDefinition.interactableVariables) && entityOn.canBeClicked();
    }

    /**
     * Updates the tone of the part to its appropriate type.
     * If the part can't match the tone of this vehicle, then it is not modified.
     * Code checks first one level up, then the top-level, if the level up doesn't have a tone.
     */
    public void updateTone(boolean recursive) {
        if (placementDefinition.toneIndex != 0) {
            String partTone = null;
            if (entityOn.subDefinition.partTones != null && entityOn.subDefinition.partTones.size() >= placementDefinition.toneIndex) {
                partTone = entityOn.subDefinition.partTones.get(placementDefinition.toneIndex - 1);
            }
            if (partTone == null) {
                if (masterEntity.subDefinition.partTones != null && masterEntity.subDefinition.partTones.size() >= placementDefinition.toneIndex) {
                    partTone = masterEntity.subDefinition.partTones.get(placementDefinition.toneIndex - 1);
                }
            }
            if (partTone != null) {
                for (JSONSubDefinition subDefinition : definition.definitions) {
                    if (subDefinition.subName.equals(partTone)) {
                        updateSubDefinition(partTone);
                        break;
                    }
                }
            }
        }

        if (recursive && !parts.isEmpty()) {
            for (APart part : parts) {
                part.updateTone(true);
            }
        }
    }

    /**
     * Adds all linked parts to the passed-in list.  This method is semi-recursive.  If a part is
     * in a linked slot, and it matches the class, then that part is added and the next slot is checked.
     * If the part doesn't match, then all child parts of that part are checked to see if they match.
     * This is done irrespective of the slot match on the sub-part, but will respect the part class.
     * This is done because wheels and other parts will frequently be attached to other parts in specific
     * slots, such as custom axles or gun mounting hard-points.  This method will also check in reverse, in
     * that if a part is linked to the slot of this part, then it will act as if this part had a linking to
     * the slot of the other part, provided the class matches the passed-in class.  Note that for all cases,
     * the JSON values are 1-indexed, whereas the map is 0-indexed.
     */
    public <PartClass extends APart> void addLinkedPartsToList(List<PartClass> partList, Class<PartClass> partClass) {
        //Check for parts we are linked to.
        if (placementDefinition.linkedParts != null) {
            for (int partIndex : placementDefinition.linkedParts) {
                APart partAtIndex = entityOn.partsInSlots.get(partIndex - 1);
                if (partClass.isInstance(partAtIndex)) {
                    partList.add(partClass.cast(partAtIndex));
                }
                if (partAtIndex != null) {
                    for (APart part : partAtIndex.allParts) {
                        if (partClass.isInstance(part)) {
                            partList.add(partClass.cast(part));
                        }
                    }
                }
            }
        }

        //If we are defined to always link to anything, get our master entity and all its parts and link them.
        if (definition.generic.forceAllLinks) {
            for (APart part : masterEntity.allParts) {
                if (part != this && partClass.isInstance(part) && !partList.contains(part)) {
                    partList.add(partClass.cast(part));
                }
            }
        }

        //Now check for parts linked to us.
        for (APart part : entityOn.parts) {
            if (part != this) {
                if (part.placementDefinition.linkedParts != null) {
                    for (int partIndex : part.placementDefinition.linkedParts) {
                        if (partIndex - 1 == this.placementSlot) {
                            if (partClass.isInstance(part) && !partList.contains(part)) {
                                //Part class matches, add it as linked.
                                partList.add(partClass.cast(part));
                            } else {
                                //Index matches, but not class, add all sub-parts that match (probably a generic part).
                                for (APart part2 : part.allParts) {
                                    if (partClass.isInstance(part2) && !partList.contains(part2)) {
                                        partList.add(partClass.cast(part2));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //If any part is defined to always link to anything, and it could link to us, link it.
        for (APart part : masterEntity.allParts) {
            if (part != this && part.definition.generic.forceAllLinks && partClass.isInstance(part) && !partList.contains(part)) {
                partList.add(partClass.cast(part));
            }
        }

        //Add all parts our parent is linked to, if we have one that's a part.
        //This allows parts multiple levels deep to query their parent.
        //We know that our parent will have their lists correct at this point as
        //they update them first, then update us.
        if (partOn != null) {
            for (APart part : partOn.linkedParts) {
                if (partClass.isInstance(part) && !partList.contains(part)) {
                    partList.add(partClass.cast(part));
                }
            }
        }
    }

    /**
     * Returns true if this part is in liquid.
     */
    public boolean isInLiquid() {
        return world.isBlockLiquid(position);
    }

    /**
     * Checks if this part can be removed with a wrench/screwdriver.  If so, then null is returned.
     * If not, a {@link LanguageEntry} is returned with the message of why it cannot be.
     */
    public LanguageEntry checkForRemoval(IWrapperPlayer player) {
        for (APart childPart : parts) {
            if (!childPart.isPermanent && !childPart.placementDefinition.allowParentRemoval) {
                return LanguageSystem.INTERACT_PARTREMOVE_HASPARTS;
            }
        }
        if (player.isHoldingItemType(ItemComponentType.WRENCH) && definition.generic.mustBeRemovedByScrewdriver) {
        	return LanguageSystem.INTERACT_PARTREMOVE_SCREWDRIVER;
        } else if (player.isHoldingItemType(ItemComponentType.SCREWDRIVER) && !definition.generic.mustBeRemovedByScrewdriver) {
        	return LanguageSystem.INTERACT_PARTREMOVE_WRENCH;
        }
        return null;
    }

    /**
     * This is called during part save/load calls.  Fakes parts are
     * added to entities, but they aren't saved with the NBT.  Rather,
     * they should be re-created in the constructor of the part that added
     * them in the first place.
     */
    public boolean isFake() {
        return false;
    }

    public double getWidth() {
        return definition.generic.width != 0 ? definition.generic.width : 0.75F;
    }

    public double getHeight() {
        return definition.generic.height != 0 ? definition.generic.height : 0.75F;
    }

    //--------------------START OF SOUND AND ANIMATION CODE--------------------
    @Override
    public float getLightProvided() {
        return entityOn.getLightProvided();
    }

    @Override
    public boolean shouldRenderBeams() {
        return entityOn.shouldRenderBeams();
    }

    @Override
    public String getTexture() {
        if (subDefinition.useVehicleTexture) {
            if (vehicleOn != null) {
                return vehicleOn.getTexture();
            } else if (definition.generic.benchTexture != null) {
                return PackResourceLoader.getPackResource(definition, ResourceType.PNG, definition.generic.benchTexture);
            } else {
                return null;
            }
        } else {
            return super.getTexture();
        }
    }

    @Override
    public boolean renderTextLit() {
        return entityOn.renderTextLit();
    }

    private void reinitVariable(String variable) {
        ComputedVariable computedVar = getOrCreateVariable(variable);
        if (variable.startsWith(ComputedVariable.INVERTED_PREFIX)) {
            computedVar.setFunctionTo(createComputedVariable(variable.substring(ComputedVariable.INVERTED_PREFIX.length()), true).invertedVariable);
        } else {
            computedVar.setFunctionTo(createComputedVariable(variable, true));
        }
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        if (entityOn == null) {
            //We don't have the entity set yet if we're constructing, defer to later.
            return new ComputedVariable(this, variable, partialTicks -> {
                //Reset ourselves since we should have a value now that we're being asked for one.
                reinitVariable(variable);
                return 0;
            }, false);
        } else {
            //If the variable is prefixed with "parent_" or "vehicle_", then we need to get our parent's or vehicle's value.
            if (variable.startsWith("vehicle_")) {
                if (vehicleOn != null) {
                    return entityOn.createComputedVariable(variable.substring("vehicle_".length()), true);
                } else {
                    //Not on a vehicle, value will always be 0.
                    return new ComputedVariable(false);
                }
            } else if (variable.startsWith("parent_")) {
                return entityOn.createComputedVariable(variable.substring("parent_".length()), true);
            } else {
                //Not a parent variable we know about, check for part variables.
                switch (variable) {
                    case ("part_present"):
                        return new ComputedVariable(true);
                    case ("part_ismirrored"):
                        return new ComputedVariable(isMirrored);
                    case ("part_isonfront"):
                        return new ComputedVariable(placementDefinition.pos.z > 0);
                    case ("part_isspare"):
                        return new ComputedVariable(isSpare);
                    case ("part_onvehicle"):
                        return new ComputedVariable(vehicleOn != null);
                    case ("part_added_vehicle"):
                        return new ComputedVariable(this, variable, partialTicks -> (vehicleOn != null && newlyAddedVar.isActive && ticksExisted == 2) ? 1 : 0, false);
                    case ("part_removed_vehicle"):
                        return new ComputedVariable(this, variable, partialTicks -> (vehicleOn != null && !isValid) ? 1 : 0, false);
                    case ("part_added_ground"):
                        return new ComputedVariable(this, variable, partialTicks -> (entityOn instanceof EntityPlacedPart && newlyAddedVar.isActive && ticksExisted == 2) ? 1 : 0, false);
                    case ("part_removed_ground"):
                        return new ComputedVariable(this, variable, partialTicks -> (entityOn instanceof EntityPlacedPart && !isValid) ? 1 : 0, false);
                    default: {
                        ComputedVariable computedVariable = super.createComputedVariable(variable, false);
                        if (computedVariable == null) {
                            //Not a basic part variable or something that the core classes make.
                            //Check any entities we are on, up to the top-most parent.
                            AEntityF_Multipart<?> testEntity = entityOn;
                            while (testEntity != null) {
                                if (testEntity.containsVariable(variable)) {
                                    //Variable exists, get as-is.
                                    return testEntity.getOrCreateVariable(variable);
                                } else {
                                    //Try to create the variable, it might be a dynamic variable property.
                                    computedVariable = testEntity.createComputedVariable(variable, false);
                                    if (computedVariable == null && testEntity instanceof APart) {
                                        testEntity = ((APart) testEntity).entityOn;
                                    } else {
                                        testEntity = null;
                                    }
                                }
                            }
                            if (computedVariable != null) {
                                return computedVariable;
                            } else {
                                //System.out.println("DID NOT FIND VARIABLE ANYWHERE ON " + this + "  " + variable);
                                if (createDefaultIfNotPresent) {
                                    ComputedVariable newVariable = new ComputedVariable(this, variable, null);
                                    addVariable(newVariable);
                                    return newVariable;
                                } else {
                                    return null;
                                }
                            }
                        } else {
                            return computedVariable;
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getWorldLightValue() {
        //Use master for lighting consistency.
        return masterEntity.worldLightValue;
    }

    @Override
    public boolean disableRendering() {
        return super.disableRendering() || isFake() || isInvisible;
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        if (canBeClicked()) {
            super.renderBoundingBoxes(transform);
        }
    }
}
