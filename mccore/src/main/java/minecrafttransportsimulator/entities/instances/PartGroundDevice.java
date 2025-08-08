package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.instances.ItemPartGroundDevice;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartGroundDevice;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * A ground device is simply a part of a vehicle that touches the ground.
 * This class is used to perform ground physics, which include steering,
 * turning, and hill climbing.  Can be a wheel-based part that rolls and
 * provides power from engines, a solid part that doesn't provide power but
 * still allows for movement, a longer part with multiple hitboxes, a
 * floating part, etc.  Each property is set via the JSON definition, though
 * a few are vehicle-dependent.
 *
 * @author don_bruce
 */
public class PartGroundDevice extends APart {
    public static final Point3D groundDetectionOffset = new Point3D(0, -0.05F, 0);
    public static final Point3D groundOperationOffset = new Point3D(0, -0.25F, 0);
    
    //Variables
    public static final String FLAT_VARIABLE = "isFlat";
    public final ComputedVariable flatVar;

    //External states for animations.
    public boolean drivenLastTick = true;
    public boolean skipAngularCalcs = false;
    public double angularPosition;
    public double prevAngularPosition;
    public double angularVelocity;

    //Internal properties
    public final ComputedVariable motiveFrictionVar;
    public final ComputedVariable lateralFrictionVar;
    private final ComputedVariable heightVar;
    private double lastHeight;
    private final Point3D groundPosition = new Point3D();
    private BlockMaterial blockMaterialBelow;
    private String blockNameBelow;
    public final Point3D wheelbasePoint;

    //Internal states for control and physics.
    public boolean contactThisTick = false;
    public boolean animateAsOnGround;
    private int ticksCalcsSkipped = 0;
    private double prevAngularVelocity;
    private boolean prevActive = true;
    private final Point3D zeroReferencePosition;
    private final Point3D prevLocalOffset;
    public PartGroundDeviceFake fakePart;

    public PartGroundDevice(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartGroundDevice item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        this.prevLocalOffset = localOffset.copy();
        this.zeroReferencePosition = position.copy();
        this.wheelbasePoint = placementDefinition.pos.copy().multiply(scale);
        AEntityF_Multipart<?> parent = entityOn;
        while(parent instanceof APart) {
            APart parentPart = (APart) parent;
            if(parentPart.placementDefinition.rot != null) {
                wheelbasePoint.rotate(parentPart.placementDefinition.rot);
            }
            wheelbasePoint.add(parentPart.placementDefinition.pos);
            parent = parentPart.entityOn;
        }
        
        addVariable(this.flatVar = new ComputedVariable(this, FLAT_VARIABLE, data));
        addVariable(this.motiveFrictionVar = new ComputedVariable(this, "motiveFriction"));
        addVariable(this.lateralFrictionVar = new ComputedVariable(this, "lateralFriction"));
        addVariable(this.heightVar = new ComputedVariable(this, "height"));
    }

    @Override
    public void addPartsPostAddition(IWrapperPlayer placingPlayer, IWrapperNBT data) {
        //Create the initial boxes and slots.
        super.addPartsPostAddition(placingPlayer, data);

        //If we are a long ground device, add a fake ground device at the offset to make us
        //have a better contact area.  We don't need to check if we are a fake part since
        //we block this method call from fake parts and just add the fake part directly.
        //Also set some parameters manually as fake parts have a few special properties.
        if (!isFake() && getLongPartOffset() != 0 && !isSpare) {
            //Need to swap placement for fake part so it uses the offset.
            JSONPartDefinition fakePlacementDef = JSONParser.duplicateJSON(placementDefinition);
            fakePlacementDef.pos.z += getLongPartOffset();
            fakePart = new PartGroundDeviceFake(this, placingPlayer, fakePlacementDef, (ItemPartGroundDevice) getStack().getItem(), null);
            entityOn.addPart(fakePart, false);
        }
    }

    @Override
    public void attack(Damage damage) {
        super.attack(damage);
        if (!damage.isWater && (outOfHealth || damage.isExplosion || (damage.damgeSource != null && Math.random() < 0.5))) {
            setFlatState(true);
        }
    }

    @Override
    public void update() {
        if (vehicleOn != null && !isSpare) {
            //Change ground device collective if we changed active state or offset or height.
            if (prevActive != isActive) {
                vehicleOn.groundDeviceCollective.updateMembers();
                vehicleOn.groundDeviceCollective.updateBounds();
                prevActive = isActive;
            }
            if (!localOffset.equals(prevLocalOffset)) {
                vehicleOn.groundDeviceCollective.updateBounds();
                prevLocalOffset.set(localOffset);
            }
            if (lastHeight != heightVar.currentValue) {
                vehicleOn.groundDeviceCollective.updateBounds();
                boundingBox.heightRadius = heightVar.currentValue;
            }

            //Set reference position for animation vars if we call them later.
            zeroReferencePosition.set(placementDefinition.pos).rotate(entityOn.orientation).add(entityOn.position);

            //If we are on the ground, adjust rotation.
            if (vehicleOn.groundDeviceCollective.groundedGroundDevices.contains(this)) {
                animateAsOnGround = true;

                //If we aren't skipping angular calcs, change our velocity accordingly.
                if (!skipAngularCalcs) {
                    prevAngularVelocity = angularVelocity;
                    angularVelocity = getDesiredAngularVelocity();
                }

                //Set contact for wheel skidding effects.
                if (definition.ground.isWheel) {
                    contactThisTick = false;
                    if (Math.abs(prevAngularVelocity) / (vehicleOn.groundVelocity / (getHeight() * Math.PI)) < 0.25 && vehicleOn.velocity > 0.3) {
                        //Sudden angular velocity increase.  Mark for skidding effects if the block below us is hard.
                        Point3D blockPositionBelow = position.copy().add(0, -1, 0);
                        if (!world.isAir(blockPositionBelow) && world.getBlockHardness(blockPositionBelow) >= 1.25) {
                            contactThisTick = true;
                        }
                    }

                    //If we have a slipping wheel, count down and possibly pop it.
                    if (!vehicleOn.world.isClient() && !flatVar.isActive) {
                        if (!skipAngularCalcs) {
                            if (ticksCalcsSkipped > 0) {
                                --ticksCalcsSkipped;
                            }
                        } else {
                            ++ticksCalcsSkipped;
                            if (Math.random() * 50000 < ticksCalcsSkipped) {
                                setFlatState(true);
                            }
                        }
                    }
                }

                //Check for colliding entities and damage them.
                if (!vehicleOn.world.isClient() && vehicleOn.velocity >= ConfigSystem.settings.damage.wheelDamageMinimumVelocity.value) {
                    boundingBox.widthRadius += 0.25;
                    boundingBox.depthRadius += 0.25;
                    final double wheelDamageAmount;
                    if (!ConfigSystem.settings.damage.wheelDamageIgnoreVelocity.value) {
                        wheelDamageAmount = ConfigSystem.settings.damage.wheelDamageFactor.value * vehicleOn.velocity * vehicleOn.currentMass / 1000F;
                    } else {
                        wheelDamageAmount = ConfigSystem.settings.damage.wheelDamageFactor.value * vehicleOn.currentMass / 1000F;
                    }
                    IWrapperEntity controller = vehicleOn.getController();
                    LanguageEntry language = controller != null ? LanguageSystem.DEATH_WHEEL_PLAYER : LanguageSystem.DEATH_WHEEL_NULL;
                    Damage wheelDamage = new Damage(wheelDamageAmount, boundingBox, this, controller, language);
                    vehicleOn.world.attackEntities(wheelDamage, null, false);
                    boundingBox.widthRadius -= 0.25;
                    boundingBox.depthRadius -= 0.25;
                }

                //Check for name/material below.
                groundPosition.set(position);
                groundPosition.y -= getHeight() / 2D;
                double yPositionFraction = groundPosition.y % 1;
                if (1 - yPositionFraction < -groundDetectionOffset.y) {
                    //We are within a detection offset below a block.  Could be FPE, so ceiling us.
                    groundPosition.y = Math.ceil(groundPosition.y) - 1;
                } else {
                    //We are above a block, but not close to the top, floor to use current block position.
                    groundPosition.y = Math.floor(groundPosition.y) - 1;
                }
                blockNameBelow = world.getBlockName(groundPosition);
                blockMaterialBelow = world.getBlockMaterial(groundPosition);
            } else {
                if (!drivenLastTick) {
                    if (vehicleOn.brakeVar.isActive || vehicleOn.parkingBrakeVar.isActive) {
                        angularVelocity = 0;
                    } else if (angularVelocity > 0) {
                        angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
                    }
                } else {
                    drivenLastTick = false;
                }
                if (animateAsOnGround && !vehicleOn.groundDeviceCollective.isActuallyOnGround(this)) {
                    animateAsOnGround = false;
                }
                blockNameBelow = null;
                blockMaterialBelow = null;
            }
            prevAngularPosition = angularPosition;

            //Invert rotation for all ground devices except treads.
            if (isMirrored && !definition.ground.isTread) {
                angularPosition -= angularVelocity;
            } else {
                angularPosition += angularVelocity;
            }
        }
        //Now that we have our wheel position, call super.
        super.update();
    }

    @Override
    public void setVariableDefaults() {
        super.setVariableDefaults();
        float frictionLoss = getFrictionLoss();
        double currentMotiveFriction = definition.ground.motiveFriction - frictionLoss;
        double currentLateralFriction = definition.ground.lateralFriction - frictionLoss;
        if (flatVar.isActive) {
            currentMotiveFriction /= 10;
            currentLateralFriction /= 10;
        }
        if (currentMotiveFriction < 0) {
            currentMotiveFriction = 0;
        }
        if (currentLateralFriction < 0) {
            currentLateralFriction = 0;
        }

        motiveFrictionVar.setTo(currentMotiveFriction, false);
        lateralFrictionVar.setTo(currentLateralFriction, false);
        lastHeight = heightVar.currentValue;
        heightVar.setTo((flatVar.isActive ? definition.ground.flatHeight : definition.ground.height) * scale.y, false);
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
            case ("ground_rotation"):
                return new ComputedVariable(this, variable, partialTicks -> vehicleOn != null ? vehicleOn.speedFactor * (partialTicks != 0 ? prevAngularPosition + (angularPosition - prevAngularPosition) * partialTicks : angularPosition) * 360D : 0, true);
            case ("ground_rotation_normalized"):
                return new ComputedVariable(this, variable, partialTicks -> vehicleOn != null ? Math.floorMod(Math.round(vehicleOn.speedFactor * (prevAngularPosition + (angularPosition - prevAngularPosition) * partialTicks) * 3600), 3600) / 10D : 0, true);
            case ("ground_onground"):
                return new ComputedVariable(this, variable, partialTicks -> vehicleOn != null && animateAsOnGround ? 1 : 0, false);
            case ("ground_isflat"):
                return flatVar;
            case ("ground_contacted"):
                return new ComputedVariable(this, variable, partialTicks -> contactThisTick ? 1 : 0, false);
            case ("ground_skidding"):
                return new ComputedVariable(this, variable, partialTicks -> skipAngularCalcs ? 1 : 0, false);
            case ("ground_slipping"):
                return new ComputedVariable(this, variable, partialTicks -> vehicleOn != null && vehicleOn.slipping && animateAsOnGround ? 1 : 0, false);
            case ("ground_distance"):
                return new ComputedVariable(this, variable, partialTicks -> world.getHeight(zeroReferencePosition), false);
            default: {
                if (variable.startsWith("ground_blockname")) {
                    final String blockName = variable.substring("ground_blockname_".length()).toLowerCase();
                    return new ComputedVariable(this, variable, partialTicks -> blockNameBelow != null && blockNameBelow.equals(blockName) ? 1 : 0, false);
                } else if (variable.startsWith("ground_blockmaterial")) {
                    final String materialName = variable.substring("ground_blockmaterial_".length()).toUpperCase();
                    return new ComputedVariable(this, variable, partialTicks -> blockMaterialBelow != null && blockMaterialBelow.name().equals(materialName) ? 1 : 0, false);
                } else {
                    return super.createComputedVariable(variable, createDefaultIfNotPresent);
                }
            }
        }
    }

    @Override
    public double getWidth() {
        return definition.ground.width * scale.x;
    }

    @Override
    public double getHeight() {
        return heightVar.currentValue;
    }

    /**
     * Attempts to set the ground device flat state to the passed-in state.  Checks to make
     * sure the ground device can actually go flat if it is being requested to do so.
     */
    public void setFlatState(boolean setFlat) {
        if (!world.isClient()) {
            //On the server, can we go flat and does the config let us?
            //Or if we are repairing, are we flat in the first place?
            if (setFlat) {
                if (flatVar.isActive || definition.ground.flatHeight == 0 || !ConfigSystem.settings.damage.wheelBreakage.value) {
                    return;
                }
            } else {
                if (!flatVar.isActive) {
                    return;
                }
            }
            //Valid conditions, send packet before continuing.
            InterfaceManager.packetInterface.sendToAllClients(new PacketPartGroundDevice(this, setFlat));
        }
        flatVar.setActive(setFlat, false);
    }

    public double getDesiredAngularVelocity() {
        if (vehicleOn != null && (definition.ground.isWheel || definition.ground.isTread)) {
            if (vehicleOn.skidSteerActive) {
                if (placementDefinition.pos.x > 0) {
                    return getLongPartOffset() == 0 ? vehicleOn.rudderAngleVar.currentValue / 200D / (getHeight() * Math.PI) : vehicleOn.rudderAngleVar.currentValue / 200D;
                } else if (placementDefinition.pos.x < 0) {
                    return getLongPartOffset() == 0 ? -vehicleOn.rudderAngleVar.currentValue / 200D / (getHeight() * Math.PI) : -vehicleOn.rudderAngleVar.currentValue / 200D;
                } else {
                    return 0;
                }
            } else {
                if (vehicleOn.goingInReverse) {
                    return getLongPartOffset() == 0 ? -vehicleOn.groundVelocity / (getHeight() * Math.PI) : -vehicleOn.groundVelocity;
                } else {
                    return getLongPartOffset() == 0 ? vehicleOn.groundVelocity / (getHeight() * Math.PI) : vehicleOn.groundVelocity;
                }
            }
        } else {
            return 0;
        }
    }

    private float getFrictionLoss() {
        if (!world.isAir(groundPosition)) {
            float penalty = world.getBlockSlipperiness(groundPosition) - 0.6F;
            Float modifier = definition.ground.frictionModifiers.get(blockMaterialBelow);
            if (modifier != null) {
                penalty -= modifier;
            }
            groundPosition.y += 1;
            if (world.getRainStrength(groundPosition) > 0) {
                penalty += definition.ground.wetFrictionPenalty;
            }
            groundPosition.y -= 1;
            return penalty;
        } else {
            return 0;
        }
    }

    public float getLongPartOffset() {
        return placementDefinition.extraCollisionBoxOffset != 0 ? placementDefinition.extraCollisionBoxOffset : definition.ground.extraCollisionBoxOffset;
    }
}
