package minecrafttransportsimulator.entities.instances;

import java.util.List;
import java.util.TreeMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.mcinterface.AWrapperWorld.BlockHitResult;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitBlock;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * This part class is special, in that it does not extend APart.
 * This is because bullets do not render as vehicle parts, and instead
 * are particles.  This allows them to be independent of the
 * vehicle that fired them.
 * <p>
 * As particles, bullets are client-side only.  This prevents them from getting stuck
 * in un-loaded chunks on the server, and prevents the massive network usage that
 * would be required to spawn 100s of bullets from a machine gun into the world.
 *
 * @author don_bruce
 */

public class EntityBullet extends AEntityD_Definable<JSONBullet> {
    //Properties
    public final PartGun gun;
    private final boolean isBomb;
    public final double initialVelocity;
    private final double velocityToAddEachTick;
    private final Point3D motionToAddEachTick;

    //States
    private int impactDesapawnTimer = -1;
    private Point3D targetPosition;
    public double targetDistance;
    private double armorPenetrated;
    private Point3D targetVector;
    private PartEngine engineTargeted;
    private IWrapperEntity externalEntityTargeted;
    private HitType lastHit;

    /**
     * Generic constructor for no target.
     **/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun) {
        super(gun.world, position, motion, ZERO_FOR_CONSTRUCTOR, gun.loadedBullet);
        this.gun = gun;
        this.isBomb = gun.definition.gun.muzzleVelocity == 0;
        this.boundingBox.widthRadius = definition.bullet.diameter / 1000D / 2D;
        this.boundingBox.heightRadius = definition.bullet.diameter / 1000D / 2D;
        this.boundingBox.depthRadius = definition.bullet.diameter / 1000D / 2D;
        this.initialVelocity = motion.length();
        if (definition.bullet.accelerationTime > 0) {
            velocityToAddEachTick = (definition.bullet.maxVelocity / 20D / 10D - motion.length()) / definition.bullet.accelerationTime;
            this.motionToAddEachTick = new Point3D(0, 0, velocityToAddEachTick).rotate(gun.orientation);
        } else {
            velocityToAddEachTick = 0;
            motionToAddEachTick = null;
        }
        this.orientation.set(orientation);
        prevOrientation.set(orientation);
    }

    /**
     * Positional target.
     **/
    private EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun, Point3D targetPosition) {
        this(position, motion, orientation, gun);
        this.targetPosition = targetPosition;
    }

    /**
     * Engine target.
     **/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun, PartEngine engineTargeted) {
        this(position, motion, orientation, gun, engineTargeted.position);
        if (engineTargeted.vehicleOn != null) {
            engineTargeted.vehicleOn.acquireMissile(this);
        }
        this.engineTargeted = engineTargeted;
        displayDebugMessage("LOCKON ENGINE " + engineTargeted.definition.systemName + " @ " + targetPosition);
    }

    /**
     * IWrapper target.
     **/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun, IWrapperEntity externalEntityTargeted) {
        this(position, motion, orientation, gun, externalEntityTargeted.getPosition().copy());
        this.externalEntityTargeted = externalEntityTargeted;
        displayDebugMessage("LOCKON ENTITY " + externalEntityTargeted.getName() + " @ " + externalEntityTargeted.getPosition());
    }

    @Override
    public void update() {
        super.update();
        //Check if we impacted.  If so, don't process anything and just stay in place.
        if (impactDesapawnTimer >= 0) {
            if (impactDesapawnTimer-- == 0) {
                remove();
            }
            return;
        }

        //Add gravity and slowdown forces, if we don't have a burning motor.
        if (ticksExisted > definition.bullet.burnTime) {
            if (definition.bullet.slowdownSpeed > 0) {
                motion.add(motion.copy().normalize().scale(-definition.bullet.slowdownSpeed));
            }
            motion.y -= gun.definition.gun.gravitationalVelocity;

            //Check to make sure we haven't gone too many ticks.
            if (ticksExisted > definition.bullet.burnTime + 200) {
                displayDebugMessage("TIEMOUT");
                remove();
                return;
            }
        }

        //Add motion requested watch tick we are accelerating.
        boolean notAcceleratingYet = definition.bullet.accelerationDelay != 0 && ticksExisted < definition.bullet.accelerationDelay;
        if (velocityToAddEachTick != 0 && !notAcceleratingYet && ticksExisted - definition.bullet.accelerationDelay < definition.bullet.accelerationTime) {
            motionToAddEachTick.set(0, 0, velocityToAddEachTick).rotate(orientation);
            motion.add(motionToAddEachTick);
        }

        //We have a target. Go to it, unless we are waiting for acceleration.
        //If the target is an external entity, update target position.
        if (targetPosition != null && !notAcceleratingYet) {
            if (externalEntityTargeted != null) {
                if (externalEntityTargeted.isValid()) {
                    targetPosition.set(externalEntityTargeted.getPosition()).add(0, externalEntityTargeted.getBounds().heightRadius, 0);
                } else {
                    //Entity is dead. Don't target it anymore.
                    externalEntityTargeted = null;
                    targetPosition = null;
                }
            } else if (engineTargeted != null) {
                //Don't need to update the position variable for engines, as it auto-syncs.
                //Do need to check if the engine is still warm and valid, however.
                if (!engineTargeted.isValid) {// || engineTargeted.temp <= PartEngine.COLD_TEMP){
                    engineTargeted = null;
                    targetPosition = null;
                }
            }

            if (targetPosition != null) {
                //Get the angular delta between us and our target, in our local orientation coordinates.
                if (targetVector == null) {
                    targetVector = new Point3D();
                }
                targetVector.set(targetPosition).subtract(position).reOrigin(orientation).getAngles(true);

                //Clamp angular delta to match turn rate and apply.
                if (targetVector.y > definition.bullet.turnRate) {
                    targetVector.y = definition.bullet.turnRate;
                } else if (targetVector.y < -definition.bullet.turnRate) {
                    targetVector.y = -definition.bullet.turnRate;
                }
                orientation.rotateY(targetVector.y);

                if (targetVector.x > definition.bullet.turnRate) {
                    targetVector.x = definition.bullet.turnRate;
                } else if (targetVector.x < -definition.bullet.turnRate) {
                    targetVector.x = -definition.bullet.turnRate;
                }
                orientation.rotateX(targetVector.x);

                //Set motion to new orientation.
                targetVector.set(0, 0, motion.length()).rotate(orientation);
                motion.set(targetVector);

                //Update target distance.
                targetDistance = targetPosition.distanceTo(position);
            }
        }

        //Now that we have an accurate motion, check for collisions.
        //First get a damage object to try to attack entities with.
        Damage damage = new Damage((velocity / initialVelocity) * definition.bullet.damage * ConfigSystem.settings.damage.bulletDamageFactor.value, boundingBox, gun, gun.lastController, gun.lastController != null ? JSONConfigLanguage.DEATH_BULLET_PLAYER : JSONConfigLanguage.DEATH_BULLET_NULL);
        damage.setBullet(getItem());

        //Check for collided external entities and attack them.
        List<IWrapperEntity> attackedEntities = world.attackEntities(damage, motion, true);
        for (IWrapperEntity entity : attackedEntities) {
            //Check to make sure we don't hit our controller.
            //This can happen with hand-held guns at speed.
            if (!entity.equals(gun.lastController)) {
                //Only attack the first entity.  Bullets don't get to attack multiple per scan.
                position.set(entity.getPosition());
                lastHit = HitType.ENTITY;
                if (!world.isClient()) {
                    entity.attack(damage);
                }
                displayDebugMessage("HIT ENTITY");
                startDespawn();
                return;
            }
        }

        //Check for collided internal entities and attack them.
        //This is a bit more involved, as we need to check all possible types and check hitbox distance.
        Point3D endPoint = position.copy().add(motion);
        BoundingBox bulletMovmenetBounds = new BoundingBox(position, endPoint);
        for (EntityVehicleF_Physics hitVehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
            //Don't attack the entity that has the gun that fired us.
            if (!hitVehicle.allParts.contains(gun)) {
                //Make sure that we could even possibly hit this vehicle before we try and attack it.
                if (hitVehicle.encompassingBox.intersects(bulletMovmenetBounds)) {
                    //Get all collision boxes on the vehicle, and check if we hit any of them.
                    //Sort them by distance for later.
                    TreeMap<Double, BoundingBox> hitBoxes = new TreeMap<>();
                    for (BoundingBox box : hitVehicle.allInteractionBoxes) {
                        if (!hitVehicle.allPartSlotBoxes.containsKey(box)) {
                            Point3D delta = box.getIntersectionPoint(position, endPoint);
                            if (delta != null) {
                                hitBoxes.put(delta.distanceTo(position), box);
                            }
                        }
                    }
                    for (BoundingBox box : hitVehicle.allBulletCollisionBoxes) {
                        Point3D delta = box.getIntersectionPoint(position, endPoint);
                        if (delta != null) {
                            hitBoxes.put(delta.distanceTo(position), box);
                        }
                    }

                    //Check all boxes for armor and see if we penetrated them.
                    for (BoundingBox hitBox : hitBoxes.values()) {
                        APart hitPart = hitVehicle.getPartWithBox(hitBox);
                        AEntityE_Interactable<?> hitEntity = hitPart != null ? hitPart : hitVehicle;

                        //First check if we need to reduce health of the hitbox.
                        if (!world.isClient() && hitBox.groupDef != null && hitBox.groupDef.health != 0 && !damage.isWater) {
                            hitEntity.damageCollisionBox(hitBox, damage.amount);
                            String variableName = "collision_" + (hitEntity.definition.collisionGroups.indexOf(hitBox.groupDef) + 1) + "_damage";
                            double currentDamage = hitEntity.getVariable(variableName);
                            displayDebugMessage("HIT HEALTH BOX.  ATTACKED FOR: " + damage.amount + ".  BOX CURRENT DAMAGE: " + currentDamage + " OF " + hitBox.groupDef.health);
                        }

                        double armorThickness = hitBox.definition != null ? (definition.bullet.isHeat && hitBox.definition.heatArmorThickness != 0 ? hitBox.definition.heatArmorThickness : hitBox.definition.armorThickness) : 0;
                        double penetrationPotential = definition.bullet.isHeat ? definition.bullet.armorPenetration : definition.bullet.armorPenetration * velocity / initialVelocity;
                        if (armorThickness > 0) {
                            armorPenetrated += armorThickness;
                            displayDebugMessage("HIT ARMOR OF: " + (int) armorThickness);
                            if (armorPenetrated > penetrationPotential) {
                                //Hit too much armor.  We die now.
                                position.set(hitBox.globalCenter);
                                lastHit = HitType.ARMOR;
                                displayDebugMessage("HIT TOO MUCH ARMOR.  MAX PEN: " + (int) penetrationPotential);
                                startDespawn();
                                return;
                            }
                        } else {
                            //Need to re-create damage object to reference this hitbox.
                            damage = new Damage(damage.amount, hitBox, gun, null, null);

                            //Now check which damage we need to apply.
                            if (hitBox.groupDef != null) {
                                if (hitBox.groupDef.health == 0 || damage.isWater) {
                                    //This is a core hitbox, or a water bullet, so attack entity directly.
                                    //After this, we die.
                                    position.set(hitBox.globalCenter);
                                    lastHit = HitType.ENTITY;
                                    if (!world.isClient()) {
                                        hitEntity.attack(damage);
                                    }
                                    displayDebugMessage("HIT ENTITY CORE BOX FOR DAMAGE: " + (int) damage.amount + " DAMAGE NOW AT " + (int) hitVehicle.damageAmount);
                                    startDespawn();
                                    return;
                                }
                            } else if (hitPart != null) {
                                //Didn't have a group def, this must be a core part box.
                                //Damage part and keep going on, unless that part is flagged to forward damage, then we do so and die.
                                //Note that parts can get killed by too much damage and suddenly become null during iteration, hence the null check.

                                position.set(hitPart.position);
                                lastHit = HitType.PART;
                                if (!world.isClient()) {
                                    hitPart.attack(damage);
                                }
                                displayDebugMessage("HIT PART FOR DAMAGE: " + (int) damage.amount + " DAMAGE NOW AT " + (int) hitPart.damageAmount);
                                if (hitPart.definition.generic.forwardsDamage || hitPart instanceof PartEngine) {
                                    if (!world.isClient()) {
                                        hitVehicle.attack(damage);
                                    }
                                    displayDebugMessage("FORWARDING DAMAGE TO VEHICLE.  CURRENT DAMAGE IS: " + (int) hitVehicle.damageAmount);
                                    startDespawn();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        //Didn't hit an entity.  Check for blocks.
        BlockHitResult hitResult = world.getBlockHit(position, motion);
        if (hitResult != null) {
            //Only change block state on the server.
            if (!world.isClient()) {
                if (definition.bullet.types.contains(BulletType.WATER)) {
                    world.extinguish(hitResult);
                } else {
                    float hardnessHit = world.getBlockHardness(hitResult.position);
                    if (ConfigSystem.settings.general.blockBreakage.value && hardnessHit > 0 && hardnessHit <= (Math.random() * 0.3F + 0.3F * definition.bullet.diameter / 20F)) {
                        world.destroyBlock(hitResult.position, true);
                    } else if (definition.bullet.types.contains(BulletType.INCENDIARY)) {
                        //Couldn't break block, but we might be able to set it on fire.
                        world.setToFire(hitResult);
                    } else {
                        //Couldn't break the block or set it on fire.  Have clients do sounds.
                        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitBlock(hitResult.position));
                    }
                }
            }
            position.set(hitResult.position);
            lastHit = HitType.BLOCK;
            displayDebugMessage("HIT BLOCK");
            startDespawn();
            return;
        }

        //Check proximity fuze against our target and blocks.
        if (definition.bullet.proximityFuze != 0) {
            Point3D targetToHit;
            if (targetPosition != null) {
                targetToHit = targetPosition;
            } else {
                hitResult = world.getBlockHit(position, motion.copy().normalize().scale(definition.bullet.proximityFuze + velocity));
                targetToHit = hitResult != null ? hitResult.position : null;
            }
            if (targetToHit != null) {
                double distanceToTarget = position.distanceTo(targetToHit);
                if (distanceToTarget < definition.bullet.proximityFuze + velocity) {
                    if (distanceToTarget > definition.bullet.proximityFuze) {
                        position.interpolate(targetToHit, (distanceToTarget - definition.bullet.proximityFuze) / definition.bullet.proximityFuze);
                    }
                    if (externalEntityTargeted != null) {
                        lastHit = HitType.ENTITY;
                        displayDebugMessage("PROX FUZE HIT ENTITY");
                    } else if (engineTargeted != null) {
                        lastHit = HitType.PART;
                        displayDebugMessage("PROX FUZE HIT ENGINE");
                    } else {
                        lastHit = HitType.BLOCK;
                        displayDebugMessage("PROX FUZE HIT BLOCK");
                    }
                    startDespawn();
                    return;
                }
            }
        }

        //Didn't hit a block either. Check the air-burst time, if it was used.
        if (definition.bullet.airBurstDelay != 0) {
            if (ticksExisted > definition.bullet.airBurstDelay) {
                lastHit = HitType.BURST;
                displayDebugMessage("BURST");
                startDespawn();
                return;
            }
        }

        //Add our updated motion to the position.
        //Then set the angles to match the motion.
        //Doing this last lets us damage on the first update tick.
        position.add(motion);
        if (!isBomb && (definition.bullet.accelerationDelay == 0 || ticksExisted > definition.bullet.accelerationDelay)) {
            orientation.setToVector(motion, true);
        }
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return true;
    }

    private void startDespawn() {
        //Spawn an explosion if we are an explosive bullet.
        if (!world.isClient() && definition.bullet.types.contains(BulletType.EXPLOSIVE) && lastHit != null) {
            float blastSize = definition.bullet.blastStrength == 0 ? definition.bullet.diameter / 10F : definition.bullet.blastStrength;
            world.spawnExplosion(position, blastSize, definition.bullet.types.contains(BulletType.INCENDIARY));
        }
        impactDesapawnTimer = definition.bullet.impactDespawnTime;
    }

    private void displayDebugMessage(String message) {
        if (!world.isClient() && ConfigSystem.settings.general.devMode.value) {
            if (gun.lastController instanceof IWrapperPlayer) {
                IWrapperPlayer player = (IWrapperPlayer) gun.lastController;
                player.sendPacket(new PacketPlayerChatMessage(player, message));
            }
        }
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("bullet_hit"):
                return lastHit != null ? 1 : 0;
            case ("bullet_burntime"):
                return ticksExisted > definition.bullet.burnTime ? 0 : definition.bullet.burnTime - ticksExisted;
            case ("bullet_hit_block"):
                return HitType.BLOCK.equals(lastHit) ? 1 : 0;
            case ("bullet_hit_entity"):
                return HitType.ENTITY.equals(lastHit) ? 1 : 0;
            case ("bullet_hit_part"):
                return HitType.PART.equals(lastHit) ? 1 : 0;
            case ("bullet_hit_armor"):
                return HitType.ARMOR.equals(lastHit) ? 1 : 0;
            case ("bullet_hit_burst"):
                return HitType.BURST.equals(lastHit) ? 1 : 0;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }

    @Override
    public boolean shouldSync() {
        return false;
    }

    @Override
    public boolean shouldSavePosition() {
        return false;
    }

    private enum HitType {
        BLOCK,
        ENTITY,
        PART,
        ARMOR,
        BURST
    }
}
