package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart.LockOnType;
import minecrafttransportsimulator.jsondefs.JSONPart.TargetType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld.BlockHitResult;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitBlock;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitCollision;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitEntity;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitExternalEntity;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitGeneric;
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
    public final int bulletNumber;
    private final boolean isBomb;
    public final double initialVelocity;
    private final double velocityToAddEachTick;
    private final Point3D motionToAddEachTick;
    private final int despawnTime;
    private final BoundingBox proxBounds;

    //States
    private boolean waitingOnActionPacket;
    private BlockHitResult serverBlockResult;
    private int impactDespawnTimer = -1;
    private Point3D targetPosition;
    public double targetDistance;
    private double distanceTraveled;
    private double armorPenetrated;
    private Point3D targetVector;
    private PartEngine engineTargeted;
    private IWrapperEntity externalEntityTargeted;
    private HitType lastHit;
    private Point3D relativeGunPos;
    private Point3D prevRelativeGunPos;
    private final List<AEntityF_Multipart<?>> multiparts = new ArrayList<>();

    /**
     * Generic constructor for no target.
     **/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun) {
        super(gun.world, position, motion, ZERO_FOR_CONSTRUCTOR, gun.loadedBullet);
        this.gun = gun;
        this.bulletNumber = ++gun.bulletsFired;
        gun.activeBullets.put(bulletNumber, this);
        gun.currentBullet = this;
        this.isBomb = gun.definition.gun.muzzleVelocity == 0;
        this.boundingBox.widthRadius = definition.bullet.diameter / 1000D / 2D;
        this.boundingBox.heightRadius = definition.bullet.diameter / 1000D / 2D;
        this.boundingBox.depthRadius = definition.bullet.diameter / 1000D / 2D;
        this.initialVelocity = motion.length();
        if (definition.bullet.accelerationTime > 0) {
            velocityToAddEachTick = (definition.bullet.maxVelocity / 20D - motion.length()) / definition.bullet.accelerationTime;
            this.motionToAddEachTick = new Point3D(0, 0, velocityToAddEachTick).rotate(gun.orientation);
        } else {
            velocityToAddEachTick = 0;
            motionToAddEachTick = null;
        }
        this.despawnTime = definition.bullet.despawnTime != 0 ? definition.bullet.despawnTime : 200;
        this.proxBounds = definition.bullet.proximityFuze != 0 ? new BoundingBox(position.copy(), definition.bullet.proximityFuze) : null;
        this.orientation.set(orientation);
        prevOrientation.set(orientation);
    }

    /**
     * Positional target.
     **/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun, Point3D targetPosition) {
        this(position, motion, orientation, gun);
        this.targetPosition = targetPosition;
    }

    /**
     * Engine target.
     **/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun, PartEngine engineTargeted) {
        this(position, motion, orientation, gun, engineTargeted.position);
        this.engineTargeted = engineTargeted;
        engineTargeted.vehicleOn.missilesIncoming.add(this);
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

        //Check remaining server block actions, if we have any.
        if (!world.isClient()) {
            if (serverBlockResult != null) {
                performBlockHitLogic(serverBlockResult);
                serverBlockResult = null;
            }
        }

        //Check if we impacted.  If so, don't process anything and just stay in place.
        if (impactDespawnTimer >= 0) {
            if (impactDespawnTimer-- == 0) {
                remove();
            }
            return;
        }

        //Check to make sure we haven't gone too many ticks.
        if (ticksExisted > definition.bullet.burnTime + despawnTime) {
            if (definition.bullet.isLongRange ^ world.isClient()) {
                displayDebugMessage("TIMEOUT");
            }
            remove();
            return;
        }

        //If we are waiting on an action packet from the server, don't do any updates and just hold for packet or expire timer.
        if (!waitingOnActionPacket) {
            //Update distance traveled.
            if (ticksExisted > 1) {
                distanceTraveled += velocity;
            }

            //Add gravity and slowdown forces, if we don't have a burning motor.
            if (ticksExisted > definition.bullet.burnTime || ticksExisted < definition.bullet.accelerationDelay) {
                if (definition.bullet.slowdownSpeed > 0) {
                    motion.add(motion.copy().normalize().scale(-definition.bullet.slowdownSpeed));
                }
                motion.y -= definition.bullet.gravitationalVelocity;
            }

            //Add motion requested watch tick we are accelerating.
            boolean notAcceleratingYet = definition.bullet.accelerationDelay != 0 && ticksExisted < definition.bullet.accelerationDelay;
            if (velocityToAddEachTick != 0 && !notAcceleratingYet && ticksExisted - definition.bullet.accelerationDelay < definition.bullet.accelerationTime) {
                motionToAddEachTick.set(0, 0, velocityToAddEachTick).rotate(orientation);
                motion.add(motionToAddEachTick);
            }

            //Guidance code for missiles
            //First check to see if it can move yet
            if (definition.bullet.turnRate > 0 && !notAcceleratingYet) {
                //First, get target positions based on guidance method.
                switch (definition.bullet.guidanceType) {
                    //Bullet will track whatever the gun was locked to, but if it can't see it,
                    //it will continue looking and track the closest thing that comes into its view cone.
                    case PASSIVE: {
                        //can't figure how to implement this yet.
                        break;
                    }
                    case SEMI_ACTIVE: {
                        //Gun must be locked to the target for the bullet to know where it is.
                        if (externalEntityTargeted != null) {
                            if (externalEntityTargeted.isValid()) {
                                targetPosition.set(externalEntityTargeted.getPosition()).add(0, externalEntityTargeted.getBounds().heightRadius, 0);
                            } else if (gun.entityTarget == null) {
                                targetPosition = null;
                            } else {
                                //Entity is dead. Don't target it anymore.
                                externalEntityTargeted = null;
                                targetPosition = null;
                            }
                        } else if (engineTargeted != null) {
                            if (gun.engineTarget == null) {
                                targetPosition = null;
                            }
                            //Don't need to update the position variable for engines, as it auto-syncs.
                            //Do need to check if the engine is still warm and valid, however.
                            if (!engineTargeted.isValid) {// || engineTargeted.temp <= PartEngine.COLD_TEMP){
                                engineTargeted.vehicleOn.missilesIncoming.remove(this);
                                engineTargeted = null;
                                targetPosition = null;
                            }
                        }
                        break;
                    }
                    case ACTIVE: {
                        //Always knows where the target is once fired.
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
                                engineTargeted.vehicleOn.missilesIncoming.remove(this);
                                engineTargeted = null;
                                targetPosition = null;
                            }
                        }
                        break;
                    }
                }

                if (targetPosition != null) {
                    //Get the angular delta between us and our target, in our local orientation coordinates.
                    if (targetVector == null) {
                        targetVector = new Point3D();
                    }
                    double ticksToTarget = targetPosition.distanceTo(position) / (velocity / 20D / 10D);
                    if (engineTargeted != null && (gun.definition.gun.targetType == TargetType.ALL || gun.definition.gun.targetType == TargetType.AIRCRAFT || gun.definition.gun.targetType == TargetType.GROUND)) {
                        targetVector.set(targetPosition).addScaled(engineTargeted.vehicleOn.motion, (engineTargeted.vehicleOn.speedFactor / 20D / 10D) * ticksToTarget).subtract(position).reOrigin(orientation).getAngles(true);
                    } else if (externalEntityTargeted != null && (gun.definition.gun.targetType == TargetType.ALL || gun.definition.gun.targetType == TargetType.SOFT)) {
                        targetVector.set(targetPosition).addScaled(externalEntityTargeted.getVelocity(), (externalEntityTargeted.getVelocity().length() / 20D / 10D) * ticksToTarget).subtract(position).reOrigin(orientation).getAngles(true);
                    } else {
                        targetVector.set(targetPosition).subtract(position).reOrigin(orientation).getAngles(true);
                    }

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

            //Long-range bullets do checks on server only.  Otherwise, they do them on clients.
            //We only do the primary client, however.
            if (definition.bullet.isLongRange ^ world.isClient()) {
                if (world.isClient()) {
                    if (!InterfaceManager.clientInterface.getClientPlayer().getID().equals(gun.lastController.getID())) {
                        //We aren't the client that's controlling the gun this bullet is on.
                        return;
                    }
                }

                //Now that we have an accurate motion, check for collisions.
                //First get a damage object to try to attack entities with.
                Damage damage = new Damage(this, boundingBox);

                //Check for collided external entities and attack them.
                List<IWrapperEntity> attackedEntities = world.attackEntities(damage, motion, true);
                for (IWrapperEntity entity : attackedEntities) {
                    //Check to make sure we don't hit our controller.
                    //This can happen with hand-held guns at speed.
                    if (!entity.equals(gun.lastController)) {
                        //Only attack the first entity.  Bullets don't get to attack multiple per scan.
                        if (world.isClient()) {
                            waitingOnActionPacket = true;
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitExternalEntity(this, entity));
                        } else {
                            performExternalEntityHitLogic(entity);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitExternalEntity(this, entity));
                        }
                        displayDebugMessage("HIT ENTITY");
                        return;
                    }
                }

                //Populate multiparts for following functions.
                multiparts.clear();
                multiparts.addAll(world.getEntitiesOfType(EntityVehicleF_Physics.class));
                multiparts.addAll(world.getEntitiesOfType(EntityPlacedPart.class));

                //Check for collided internal entities and attack them.
                //This is a bit more involved, as we need to check all possible types and check hitbox distance.
                Point3D endPoint = position.copy().add(motion);
                BoundingBox bulletMovementBounds = new BoundingBox(position, endPoint);
                for (AEntityF_Multipart<?> multipart : multiparts) {
                    //Don't attack the entity that has the gun that fired us.
                    if (!multipart.allParts.contains(gun)) {
                        //if(multipart.att

                        //Make sure that we could even possibly hit this vehicle before we try and attack it.
                        if (multipart.encompassingBox.intersects(bulletMovementBounds)) {
                            //Get all collision boxes on the vehicle, and check if we hit any of them.
                            //Sort them by distance for later.
                            TreeMap<Double, BoundingBox> hitBoxes = new TreeMap<>();
                            for (BoundingBox box : multipart.allInteractionBoxes) {
                                if (!multipart.allPartSlotBoxes.containsKey(box)) {
                                    Point3D delta = box.getIntersectionPoint(position, endPoint);
                                    if (delta != null) {
                                        hitBoxes.put(delta.distanceTo(position), box);
                                    }
                                }
                            }
                            for (BoundingBox box : multipart.allBulletCollisionBoxes) {
                                Point3D delta = box.getIntersectionPoint(position, endPoint);
                                if (delta != null) {
                                    hitBoxes.put(delta.distanceTo(position), box);
                                }
                            }

                            //Check all boxes for armor and see if we penetrated them.
                            for (BoundingBox hitBox : hitBoxes.values()) {
                                APart hitPart = multipart.getPartWithBox(hitBox);
                                AEntityE_Interactable<?> hitEntity = hitPart != null ? hitPart : multipart;

                                //First check if we need to reduce health of the hitbox.
                                if (!world.isClient() && hitBox.groupDef != null && hitBox.groupDef.health != 0 && !damage.isWater) {
                                    String variableName = "collision_" + (hitEntity.definition.collisionGroups.indexOf(hitBox.groupDef) + 1) + "_damage";
                                    double currentDamage = hitEntity.getVariable(variableName);
                                    displayDebugMessage("HIT HEALTH BOX.  BOX CURRENT DAMAGE: " + currentDamage + " OF " + hitBox.groupDef.health + "  ATTACKED FOR: " + damage.amount);

                                    //This is a server-only action that does NOT cause us to stop processing.
                                    if (world.isClient()) {
                                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitCollision(hitEntity, hitBox, damage.amount));
                                    } else {
                                        hitEntity.damageCollisionBox(hitBox, damage.amount);
                                    }
                                }

                                double armorThickness = hitBox.definition != null ? (definition.bullet.isHeat && hitBox.definition.heatArmorThickness != 0 ? hitBox.definition.heatArmorThickness : hitBox.definition.armorThickness) : 0;
                                double penetrationPotential = definition.bullet.isHeat ? definition.bullet.armorPenetration : definition.bullet.armorPenetration * velocity / initialVelocity;
                                if (armorThickness > 0) {
                                    armorPenetrated += armorThickness;
                                    displayDebugMessage("HIT ARMOR OF: " + (int) armorThickness);

                                    if (armorPenetrated > penetrationPotential) {
                                        //Hit too much armor.  We die now.
                                        if (world.isClient()) {
                                            waitingOnActionPacket = true;
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(this, hitBox.globalCenter, HitType.ARMOR));
                                        } else {
                                            performGenericHitLogic(hitBox.globalCenter, HitType.ARMOR);
                                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitGeneric(this, hitBox.globalCenter, HitType.ARMOR));
                                        }
                                        displayDebugMessage("HIT TOO MUCH ARMOR.  MAX PEN: " + (int) penetrationPotential);
                                        return;
                                    }
                                } else {
                                    //Need to re-create damage object to reference this hitbox.
                                    damage = new Damage(this, hitBox);

                                    //Apply damage if we hit a core group or a part.
                                    //Other hits are already taken care of.
                                    //Remove bullet if we are applying damage to a core group, or a part that forwards damage.
                                    boolean applyDamage = ((hitBox.groupDef != null && (hitBox.groupDef.health == 0 || damage.isWater)) || hitPart != null);
                                    boolean removeAfterDamage = applyDamage && (hitPart == null || hitPart.definition.generic.forwardsDamageMultiplier > 0);
                                    displayDebugMessage("HIT ENTITY BOX FOR DAMAGE: " + (int) damage.amount + " DAMAGE WAS AT " + (int) multipart.damageAmount);

                                    if (world.isClient()) {
                                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitEntity(this, hitEntity, hitBox, damage));
                                        if (removeAfterDamage) {
                                            waitingOnActionPacket = true;
                                            return;
                                        }
                                    } else {
                                        performEntityHitLogic(hitEntity, damage);
                                        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitEntity(this, hitEntity, hitBox, damage));
                                        if (removeAfterDamage) {
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
                    if (world.isClient()) {
                        waitingOnActionPacket = true;
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitBlock(this, hitResult));
                    } else {
                        performBlockHitLogic(hitResult);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitBlock(this, hitResult));
                    }
                    displayDebugMessage("HIT BLOCK AT " + hitResult.position);
                    return;
                }

                //Check proximity fuze against our target and blocks.
                if (definition.bullet.proximityFuze != 0 && distanceTraveled > definition.bullet.proximityFuze * 3) {
                    HitType hitType = null;
                    Point3D targetToHit = null;
                    if (targetPosition != null) {
                        //Have an entity target, check if we got close enough to them.
                        if (position.distanceTo(targetPosition) < definition.bullet.proximityFuze + velocity) {
                            targetToHit = targetPosition;
                        }
                    } else {
                        //No entity target, first check blocks.
                        hitResult = world.getBlockHit(position, motion.copy().normalize().scale(definition.bullet.proximityFuze + velocity));
                        if (hitResult != null) {
                            targetToHit = hitResult.position;
                            hitType = HitType.BLOCK;
                            displayDebugMessage("PROX FUZE HIT BLOCK");
                        } else {
                            //Need to get an entity target.
                            //Check at deltas of the prox fuze to see if we hit one along the path.
                            Point3D stepDelta = motion.copy().normalize().scale(definition.bullet.proximityFuze);
                            int maxSteps = (int) Math.floor(velocity / definition.bullet.proximityFuze);
                            proxBounds.globalCenter.set(position);
                            for (int step = 0; step < maxSteps; ++step) {
                                for (AEntityF_Multipart<?> multipart : multiparts) {
                                    if (multipart.encompassingBox.intersects(proxBounds)) {
                                        //Could have hit this multipart, check all boxes.
                                        for (BoundingBox box : multipart.allInteractionBoxes) {
                                            if (box.globalCenter.isDistanceToCloserThan(proxBounds.globalCenter, definition.bullet.proximityFuze)) {
                                                targetToHit = proxBounds.globalCenter.copy();
                                                hitType = HitType.VEHICLE;
                                                displayDebugMessage("PROX FUZE HIT VEHICLE");
                                                break;
                                            }
                                        }
                                    }
                                    if (targetToHit != null) {
                                        break;
                                    }
                                }

                                //If we didn't hit a vehicle, try entities.
                                if (targetToHit == null) {
                                    for (IWrapperEntity entity : world.getEntitiesWithin(proxBounds)) {
                                        if (entity.getPosition().isDistanceToCloserThan(proxBounds.globalCenter, definition.bullet.proximityFuze)) {
                                            targetToHit = proxBounds.globalCenter.copy();
                                            hitType = HitType.ENTITY;
                                            displayDebugMessage("PROX FUZE HIT ENTITY");
                                            break;
                                        }
                                    }
                                }

                                if (targetToHit != null) {
                                    break;
                                } else {
                                    //Add the step delta for the next check.
                                    proxBounds.globalCenter.add(stepDelta);
                                }
                            }
                        }
                    }
                    if (lastHit != null) {
                        if (targetToHit != null) {
                            double distanceToTarget = position.distanceTo(targetToHit);
                            if (distanceToTarget > definition.bullet.proximityFuze) {
                                //We will hit this target this tick, but we need to move right to the prox distance before detonating.
                                position.interpolate(targetToHit, (distanceToTarget - definition.bullet.proximityFuze) / definition.bullet.proximityFuze);
                            }
                        }
                        if (world.isClient()) {
                            waitingOnActionPacket = true;
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(this, targetToHit, hitType));
                        } else {
                            performGenericHitLogic(targetToHit, hitType);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitGeneric(this, targetToHit, hitType));
                        }
                        return;
                    }
                }

                //Didn't hit a block either. Check the air-burst time, if it was used.
                if (definition.bullet.airBurstDelay != 0) {
                    if (ticksExisted > definition.bullet.airBurstDelay) {
                        if (world.isClient()) {
                            waitingOnActionPacket = true;
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(this, position, HitType.BURST));
                        } else {
                            performGenericHitLogic(position, HitType.BURST);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitGeneric(this, position, HitType.BURST));
                        }
                        displayDebugMessage("BURST");
                        return;
                    }
                }
            }

            //Add our updated motion to the position.
            //Then set the angles to match the motion.
            //Doing this last lets us damage on the first update tick.
            position.add(motion);
            if (!isBomb && (definition.bullet.accelerationDelay == 0 || ticksExisted > definition.bullet.accelerationDelay)) {
                orientation.setToVector(motion, true);
            }

            //Set gun pos if the gun has requested it by creating it.
            if (relativeGunPos != null) {
                prevRelativeGunPos.set(relativeGunPos);
                relativeGunPos.set(position).subtract(gun.position).reOrigin(gun.orientation);
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
        if (gun.definition.gun.lockOnType == LockOnType.MANUAL) {
            gun.activeManualBullets.remove(this);
        }
        if (engineTargeted != null) {
            engineTargeted.vehicleOn.missilesIncoming.remove(this);
        }
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return true;
    }

    public double getRelativePos(int axisIndex, float partialTicks) {
        if (relativeGunPos == null) {
            relativeGunPos = position.copy().subtract(gun.position).reOrigin(gun.orientation);
            prevRelativeGunPos = relativeGunPos.copy();
        }
        switch (axisIndex) {
            case (1):
                return partialTicks != 0 ? prevRelativeGunPos.x + (relativeGunPos.x - prevRelativeGunPos.x) * partialTicks : relativeGunPos.x;
            case (2):
                return partialTicks != 0 ? prevRelativeGunPos.y + (relativeGunPos.y - prevRelativeGunPos.y) * partialTicks : relativeGunPos.y;
            case (3):
                return partialTicks != 0 ? prevRelativeGunPos.z + (relativeGunPos.z - prevRelativeGunPos.z) * partialTicks : relativeGunPos.z;
            default:
                throw new IllegalArgumentException("There are only three axis in the world you idiot!");
        }
    }

    public void performEntityHitLogic(AEntityE_Interactable<?> entity, Damage damage) {
        boolean applyDamage = ((damage.box.groupDef != null && (damage.box.groupDef.health == 0 || damage.isWater)) || entity instanceof APart);
        boolean removeAfterDamage = applyDamage && (!(entity instanceof APart) || ((APart) entity).definition.generic.forwardsDamageMultiplier > 0);

        if (!world.isClient()) {
            entity.attack(damage);
        }
        if (removeAfterDamage) {
            performGenericHitLogic(damage.box.globalCenter, HitType.VEHICLE);
        }
    }

    public void performExternalEntityHitLogic(IWrapperEntity entity) {
        if (!world.isClient()) {
            entity.attack(new Damage(this, null));
        }
        performGenericHitLogic(entity.getPosition(), HitType.ENTITY);
    }

    public void performBlockHitLogic(BlockHitResult hitResult) {
        //On the server, however, delay actions by 1 tick.
        //This lets the client get sent packets for particles and sounds that will occur before the packets
        //to do these actions which modify the blocks by blowing them up and such.
        if (!world.isClient() && serverBlockResult == null) {
            serverBlockResult = hitResult;
            return;
        }

        //Only change block state on the server.
        //Clients just spawn break sounds.
        if (!world.isClient()) {
            if (definition.bullet.types.contains(BulletType.WATER)) {
                world.extinguish(hitResult);
            } else {
                float hardnessHit = world.getBlockHardness(hitResult.position);
                if (ConfigSystem.settings.general.blockBreakage.value && hardnessHit > 0 && hardnessHit <= (Math.random() * 0.3F + 0.3F * definition.bullet.diameter / 20F)) {
                    world.destroyBlock(position, true);
                } else if (definition.bullet.types.contains(BulletType.INCENDIARY)) {
                    //Couldn't break block, but we might be able to set it on fire.
                    world.setToFire(hitResult);
                }
            }
        } else if (definition.bullet.types.isEmpty()) {
            //Fancy bullets don't make block-breaking sounds.  They do other things instead.
            InterfaceManager.clientInterface.playBlockBreakSound(position);
        }
        performGenericHitLogic(hitResult.position, HitType.BLOCK);
    }

    public void performGenericHitLogic(Point3D position, HitType hitType) {
        this.position.set(position);
        this.lastHit = hitType;

        //Spawn an explosion if we are an explosive bullet on the server.
        if (!world.isClient() && definition.bullet.types.contains(BulletType.EXPLOSIVE) && lastHit != null) {
            float blastSize = definition.bullet.blastStrength == 0 ? definition.bullet.diameter / 10F : definition.bullet.blastStrength;
            world.spawnExplosion(position, blastSize, definition.bullet.types.contains(BulletType.INCENDIARY));
        }

        //If we are on the client, do one last particle check.  This lets systems query the blocks we hit before the server adjusts them.
        if (world.isClient()) {
            spawnParticles(0);
        }

        //Do final state setting.
        impactDespawnTimer = definition.bullet.impactDespawnTime;
        gun.currentBullet = null;
        gun.activeBullets.remove(bulletNumber);
    }

    private void displayDebugMessage(String message) {
        if (ConfigSystem.settings.general.devMode.value && gun.lastController instanceof IWrapperPlayer) {
            if (!world.isClient()) {
                IWrapperPlayer player = (IWrapperPlayer) gun.lastController;
                player.sendPacket(new PacketPlayerChatMessage(player, message));
            } else {
                ((IWrapperPlayer) gun.lastController).displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, message);
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
                return HitType.BLOCK == lastHit ? 1 : 0;
            case ("bullet_hit_entity"):
                return HitType.ENTITY == lastHit ? 1 : 0;
            case ("bullet_hit_vehicle"):
                return HitType.VEHICLE == lastHit ? 1 : 0;
            case ("bullet_hit_armor"):
                return HitType.ARMOR == lastHit ? 1 : 0;
            case ("bullet_hit_burst"):
                return HitType.BURST == lastHit ? 1 : 0;
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

    public enum HitType {
        BLOCK,
        ENTITY,
        VEHICLE,
        ARMOR,
        BURST
    }
}
