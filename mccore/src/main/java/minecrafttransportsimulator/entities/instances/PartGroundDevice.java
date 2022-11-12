package minecrafttransportsimulator.entities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartGroundDevice;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.systems.ConfigSystem;

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

    //External states for animations.
    public boolean drivenLastTick = true;
    public boolean skipAngularCalcs = false;
    public double angularPosition;
    public double prevAngularPosition;
    public double angularVelocity;

    //Internal properties
    @ModifiedValue
    private float currentMotiveFriction;
    @ModifiedValue
    private float currentLateralFriction;
    @ModifiedValue
    private float currentHeight;

    //Internal states for control and physics.
    public boolean isFlat;
    public boolean contactThisTick = false;
    private boolean animateAsOnGround;
    private int ticksCalcsSkipped = 0;
    private double prevAngularVelocity;
    private boolean prevActive = true;
    private final Point3D zeroReferencePosition;
    private final Point3D prevLocalOffset;
    private PartGroundDeviceFake fakePart;

    public PartGroundDevice(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, data);
        this.isFlat = data.getBoolean("isFlat");
        this.prevLocalOffset = localOffset.copy();
        this.zeroReferencePosition = position.copy();
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
            IWrapperNBT fakeData = InterfaceManager.coreInterface.getNewNBTWrapper();
            getItem().populateDefaultData(fakeData);
            fakePart = new PartGroundDeviceFake(this, placingPlayer, fakePlacementDef, fakeData);
            entityOn.addPart(fakePart, false);
        }
    }

    @Override
    public void attack(Damage damage) {
        super.attack(damage);
        if (!damage.isWater && (damage.isExplosion || Math.random() < 0.5 || damageAmount > definition.general.health)) {
            setFlatState(true);
        }
    }

    @Override
    public void update() {
        if (vehicleOn != null && !isSpare) {
            //Change ground device collective if we changed active state or offset.
            if (prevActive != isActive) {
                vehicleOn.groundDeviceCollective.updateMembers();
                vehicleOn.groundDeviceCollective.updateBounds();
                prevActive = isActive;
            }
            if (!localOffset.equals(prevLocalOffset)) {
                vehicleOn.groundDeviceCollective.updateBounds();
                prevLocalOffset.set(localOffset);
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
                    if (!vehicleOn.world.isClient() && !isFlat) {
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
                    LanguageEntry language = controller != null ? JSONConfigLanguage.DEATH_WHEEL_PLAYER : JSONConfigLanguage.DEATH_WHEEL_NULL;
                    Damage wheelDamage = new Damage(wheelDamageAmount, boundingBox, this, controller, language);
                    vehicleOn.world.attackEntities(wheelDamage, null, false);
                    boundingBox.widthRadius -= 0.25;
                    boundingBox.depthRadius -= 0.25;
                }
            } else {
                if (!drivenLastTick) {
                    if (vehicleOn.brake > 0 || vehicleOn.parkingBrakeOn) {
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
    protected void updateVariableModifiers() {
        currentMotiveFriction = definition.ground.motiveFriction;
        currentLateralFriction = definition.ground.lateralFriction;
        currentHeight = (float) ((isFlat ? definition.ground.flatHeight : definition.ground.height) * scale.y);

        //Adjust current variables to modifiers, if any exist.
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                switch (modifier.variable) {
                    case "motiveFriction":
                        currentMotiveFriction = adjustVariable(modifier, currentMotiveFriction);
                        break;
                    case "lateralFriction":
                        currentLateralFriction = adjustVariable(modifier, currentLateralFriction);
                        break;
                    case "height":
                    	currentHeight = adjustVariable(modifier, currentHeight);
                        break;
                    default:
                        setVariable(modifier.variable, adjustVariable(modifier, (float) getVariable(modifier.variable)));
                        break;
                }
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
        if (fakePart != null) {
            //Don't remove here, this could cause a CME if we're in a removal iteration loop.
            fakePart.isValid = false;
        }
    }

    @Override
    public void addDropsToList(List<IWrapperItemStack> drops) {
        if (!isFlat) {
            super.addDropsToList(drops);
        }
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("ground_rotation"):
                return vehicleOn != null ? vehicleOn.speedFactor * (partialTicks != 0 ? prevAngularPosition + (angularPosition - prevAngularPosition) * partialTicks : angularPosition) * 360D : 0;
            case ("ground_rotation_normalized"):
            	return vehicleOn != null ? Math.floorMod(Math.round(vehicleOn.speedFactor * (prevAngularPosition + (angularPosition - prevAngularPosition) * partialTicks) * 3600), 3600) / 10D : 0;
            case ("ground_onground"):
                return vehicleOn != null && animateAsOnGround ? 1 : 0;
            case ("ground_isflat"):
                return isFlat ? 1 : 0;
            case ("ground_contacted"):
                return contactThisTick ? 1 : 0;
            case ("ground_skidding"):
                return skipAngularCalcs ? 1 : 0;
            case ("ground_slipping"):
                return vehicleOn != null && vehicleOn.slipping && animateAsOnGround ? 1 : 0;
            case ("ground_distance"):
                return world.getHeight(zeroReferencePosition);
        }

        return super.getRawVariableValue(variable, partialTicks);
    }

    @Override
    public double getWidth() {
        return definition.ground.width * scale.x;
    }

    @Override
    public double getHeight() {
        return currentHeight;
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
                if (isFlat || definition.ground.flatHeight == 0 || !ConfigSystem.settings.damage.wheelBreakage.value) {
                    return;
                }
            } else {
                if (!isFlat) {
                    return;
                }
            }
            //Valid conditions, send packet before continuing.
            InterfaceManager.packetInterface.sendToAllClients(new PacketPartGroundDevice(this, setFlat));
        }

        //Set flat state and new bounding box.
        isFlat = setFlat;
        boundingBox.heightRadius = getHeight();
        if (vehicleOn != null) {
            vehicleOn.groundDeviceCollective.updateBounds();
        }
    }

    public float getFrictionLoss() {
        Point3D groundPosition = position.copy().add(0, -1, 0);
        if (!world.isAir(groundPosition)) {
            Float modifier = definition.ground.frictionModifiers.get(world.getBlockMaterial(groundPosition));
            if (modifier == null) {
                return world.getBlockSlipperiness(groundPosition) - 0.6F;
            } else {
                return world.getBlockSlipperiness(groundPosition) - 0.6F - modifier;
            }
        } else {
            return 0;
        }
    }

    public double getDesiredAngularVelocity() {
        if (vehicleOn != null && (definition.ground.isWheel || definition.ground.isTread)) {
            if (vehicleOn.skidSteerActive) {
                if (placementDefinition.pos.x > 0) {
                    return getLongPartOffset() == 0 ? vehicleOn.rudderAngle / 200D / (getHeight() * Math.PI) : vehicleOn.rudderAngle / 200D;
                } else if (placementDefinition.pos.x < 0) {
                    return getLongPartOffset() == 0 ? -vehicleOn.rudderAngle / 200D / (getHeight() * Math.PI) : -vehicleOn.rudderAngle / 200D;
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

    public float getMotiveFriction() {
        return !isFlat ? currentMotiveFriction : currentMotiveFriction / 10F;
    }

    public float getLateralFriction() {
        return !isFlat ? currentLateralFriction : currentLateralFriction / 10F;
    }

    public float getLongPartOffset() {
        return placementDefinition.extraCollisionBoxOffset != 0 ? placementDefinition.extraCollisionBoxOffset : definition.ground.extraCollisionBoxOffset;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setBoolean("isFlat", isFlat);
        return data;
    }
}
