package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Now that we have an existing vehicle its time to add the ability to collide with it,
 * and for it to do collision with other entities in the world.  This is where collision
 * bounds are added, as well as the mass of the entity is calculated, as that's required
 * for collision physics forces.  We also add vectors here for the vehicle's orientation,
 * as those are required for us to know how the vehicle collided in the first place.
 *
 * @author don_bruce
 */
abstract class AEntityVehicleC_Colliding extends AEntityG_Towable<JSONVehicle> {

    //Internal states.
    private float hardnessHitThisTick = 0;
    public double currentMass;
    public double axialVelocity;
    public final Point3D headingVector = new Point3D();

    /**
     * Cached value for speedFactor.  Saves us from having to use the long form all over.
     */
    public final double speedFactor;

    public AEntityVehicleC_Colliding(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, data);
        this.speedFactor = (definition.motorized.isAircraft ? ConfigSystem.settings.general.aircraftSpeedFactor.value : ConfigSystem.settings.general.carSpeedFactor.value) * ConfigSystem.settings.general.packSpeedFactors.value.get(definition.packID);
        double vehicleScale = ConfigSystem.settings.general.packVehicleScales.value.get(definition.packID);
        scale.set(vehicleScale, vehicleScale, vehicleScale);
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("VehicleC_Level", true);

        //Set vectors to current velocity and orientation.
        world.beginProfiling("SetVectors", true);
        headingVector.set(0D, 0D, 1D);
        headingVector.rotate(orientation);
        axialVelocity = Math.abs(motion.dotProduct(headingVector, false));

        //Update mass.
        world.beginProfiling("SetMass", false);
        currentMass = getMass();

        //Auto-close any open doors that should be closed.
        //Only do this once a second to prevent lag.
        if (velocity > 0.5 && ticksExisted % 20 == 0) {
            world.beginProfiling("CloseDoors", false);
            variables.keySet().removeIf(s -> s.startsWith("door"));
        }

        //Set hardness hit this tick to 0 to reset collision force calculations.
        hardnessHitThisTick = 0;
        world.endProfiling();
        world.endProfiling();
    }

    /**
     * Checks collisions and returns the collision depth for a box.
     * Returns -1 if collision was hard enough to destroy the vehicle.
     * Returns -2 if the vehicle hit a block but had to stop because blockBreaking was disabled.
     * Otherwise, we return the collision depth in the specified axis.
     */
    protected double getCollisionForAxis(BoundingBox box, boolean xAxis, boolean yAxis, boolean zAxis) {
        //Get the motion the entity is trying to move, and add it to the passed-in box value.
        Point3D collisionMotion = motion.copy().scale(speedFactor);

        //If we collided, so check to see if we can break some blocks or if we need to explode.
        //Don't bother with this logic if it's impossible for us to break anything.
        if (box.updateCollidingBlocks(world, collisionMotion)) {
            float hardnessHitThisBox = 0;
            for (Point3D blockPosition : box.collidingBlockPositions) {
                float blockHardness = world.getBlockHardness(blockPosition);
                if (!world.isBlockLiquid(blockPosition)) {
                    if (ConfigSystem.settings.general.blockBreakage.value && blockHardness <= velocity * currentMass / 250F && blockHardness >= 0) {
                        hardnessHitThisBox += blockHardness;
                        if (!yAxis) {
                            //Only add hardness if we hit in XZ movement.  Don't want to blow up from falling fast, just break tons of dirt.
                            hardnessHitThisTick += blockHardness;
                        }
                        motion.scale(Math.max(1.0F - blockHardness * 0.5F / ((1000F + currentMass) / 1000F), 0.0F));
                        if (!world.isClient()) {
                            if (ticksExisted > 500) {
                                world.destroyBlock(blockPosition, true);
                                if (box.groupDef != null && blockHardness > 0) {
                                    damageCollisionBox(box, blockHardness >= 20 ? blockHardness * 2 : blockHardness * 4);
                                }
                            } else {
                                motion.set(0D, 0D, 0D);
                                return -1;
                            }
                        }
                    } else {
                        hardnessHitThisTick = 0;
                        motion.set(0, 0, 0);
                        return -2;
                    }
                }
            }

            if (ConfigSystem.settings.general.vehicleDestruction.value && hardnessHitThisTick > currentMass / (0.75 + velocity) / 250F) {
                if (!world.isClient()) {
                    APart partHit = getPartWithBox(box);
                    if (partHit != null) {
                        hardnessHitThisTick -= hardnessHitThisBox;
                        removePart(partHit, null);
                    } else {
                        destroy(box);
                    }
                }
                return -1;
            } else if (xAxis) {
                return box.currentCollisionDepth.x;
            } else if (yAxis) {
                return box.currentCollisionDepth.y;
            } else if (zAxis) {
                return box.currentCollisionDepth.z;
            } else {
                throw new IllegalArgumentException("Collision requested but no axis was specified!");
            }
        } else {
            return 0;
        }
    }

    @Override
    public void destroy(BoundingBox box) {
        //Get drops from parts.  Vehicle just blows up.
        List<IWrapperItemStack> drops = new ArrayList<>();

        //Do part things before we call super, as that will remove the parts from this vehicle.
        IWrapperEntity controller = getController();
        Damage controllerCrashDamage = new Damage(ConfigSystem.settings.damage.crashDamageFactor.value * velocity * 20, null, this, null, JSONConfigLanguage.DEATH_CRASH_NULL);
        LanguageEntry language = controller != null ? JSONConfigLanguage.DEATH_CRASH_PLAYER : JSONConfigLanguage.DEATH_CRASH_NULL;
        Damage passengerCrashDamage = new Damage(ConfigSystem.settings.damage.crashDamageFactor.value * velocity * 20, null, this, controller, language);
        for (APart part : allParts) {
            //Damage riders.
            if (part.rider != null) {
                if (part.rider == controller) {
                    part.rider.attack(controllerCrashDamage);
                } else {
                    part.rider.attack(passengerCrashDamage);
                }
            }

            //Add drops.
            part.addDropsToList(drops);
        }

        //Now call super and spawn drops.
        super.destroy(box);
        drops.forEach(stack -> world.spawnItemStack(stack, box.globalCenter));
    }

    @Override
    public double getMass() {
        return super.getMass() + definition.motorized.emptyMass;
    }
}
