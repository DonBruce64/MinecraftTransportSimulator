package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONPart.LockOnType;
import minecrafttransportsimulator.jsondefs.JSONPart.TargetType;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitBlock;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitExternalEntity;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitGeneric;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;

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
    public boolean waitingOnActionPacket;
    private int impactDespawnTimer = -1;
    private Point3D targetPosition;
    private final Point3D helperPoint = new Point3D();
    public double targetDistance;
    private double distanceTraveled;
    public double armorPenetrated;

    private Point3D targetVector;
    private Point3D normalizedConeVector = new Point3D();
    private Point3D normalizedEntityVector = new Point3D();
    private PartEngine engineTargeted;
    private IWrapperEntity externalEntityTargeted;
    public HitType lastHit;
    public Axis sideHit;
    private Point3D relativeGunPos;
    private Point3D prevRelativeGunPos;
    private final List<AEntityF_Multipart<?>> multiparts = new ArrayList<>();

    /**
     * Generic constructor for no target.
     **/
    public EntityBullet(Point3D position, Point3D motion, RotationMatrix orientation, PartGun gun) {
        super(gun.world, position, motion, ZERO_FOR_CONSTRUCTOR, gun.loadedBullet);
        this.gun = gun;
        this.bulletNumber = gun.bulletsFired;
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
                        //goes for what it can see, even if bullet lock is broken.
                        //will search for its own target if fired without one
                        //cant get this to work w/o crashing atm
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
                        Point3D startPoint = position;
                        Point3D searchVector = new Point3D(0, 0, definition.bullet.seekerRange).rotate(orientation);
                        double coneAngle = definition.bullet.seekerMaxAngle;
                        //Always knows where the target is once fired.
                        if (externalEntityTargeted != null) {
                            //check to see if target is able to be locked on to still.(seeker can see it)
                            normalizedConeVector.set(searchVector).normalize();
                            normalizedEntityVector.set(externalEntityTargeted.getPosition()).subtract(startPoint).normalize();
                            double targetAngle = Math.abs(Math.toDegrees(Math.acos(normalizedConeVector.dotProduct(normalizedEntityVector, false))));
                            if (externalEntityTargeted.isValid() && targetAngle < coneAngle || world.getBlockHit(startPoint, targetPosition) != null || targetPosition.distanceTo(position) > definition.bullet.seekerRange) {
                                targetPosition.set(externalEntityTargeted.getPosition()).add(0, externalEntityTargeted.getBounds().heightRadius, 0);
                            } else {
                                //Entity is dead. Don't target it anymore.
                                externalEntityTargeted = null;
                                targetPosition = null;
                            }
                        } else if (engineTargeted != null) {
                            normalizedConeVector.set(searchVector).normalize();
                            normalizedEntityVector.set(engineTargeted.vehicleOn.position).subtract(startPoint).normalize();
                            double targetAngle = Math.abs(Math.toDegrees(Math.acos(normalizedConeVector.dotProduct(normalizedEntityVector, false))));
                            //Don't need to update the position variable for engines, as it auto-syncs.
                            //Do need to check if the engine is still warm and valid, however.
                            if (!engineTargeted.isValid || targetAngle > coneAngle || world.getBlockHit(startPoint, targetPosition) != null || targetPosition.distanceTo(position) > definition.bullet.seekerRange) {// || engineTargeted.temp <= PartEngine.COLD_TEMP){
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
                    double ticksToTarget = targetPosition.distanceTo(position) / (velocity / 20D);
                    if (engineTargeted != null && (gun.definition.gun.targetType == TargetType.ALL || gun.definition.gun.targetType == TargetType.AIRCRAFT || gun.definition.gun.targetType == TargetType.GROUND)) {
                        targetVector.set(targetPosition).addScaled(engineTargeted.vehicleOn.motion, (engineTargeted.vehicleOn.speedFactor / 20D) * ticksToTarget).subtract(position).reOrigin(orientation).getAngles(true);
                    } else if (externalEntityTargeted != null && (gun.definition.gun.targetType == TargetType.ALL || gun.definition.gun.targetType == TargetType.SOFT)) {
                        targetVector.set(targetPosition).addScaled(externalEntityTargeted.getVelocity(), (externalEntityTargeted.getVelocity().length() / 20D) * ticksToTarget).subtract(position).reOrigin(orientation).getAngles(true);
                    } else {
                        targetVector.set(targetPosition).subtract(position).reOrigin(orientation).getAngles(true);
                    }

                    //Clamp angular delta to match turn rate and apply.
                    if (ticksExisted > definition.bullet.guidanceDelay) {
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
                        if (ticksExisted > definition.bullet.burnTime) {
                            motion.y -= definition.bullet.gravitationalVelocity;
                            motion.set(targetVector.x, motion.y, targetVector.z);
                        } else {
                            motion.set(targetVector);
                        }
                        //Update target distance.
                        targetDistance = targetPosition.distanceTo(position);
                    }
                }
            }

            //Long-range bullets and those fired by NPCs do checks on server only, all others do so on clients.
            //We only do client checks on the primary client, not all clients.
            if (((definition.bullet.isLongRange || !(gun.lastController instanceof IWrapperPlayer)) ^ world.isClient()) && (!world.isClient() || InterfaceManager.clientInterface.getClientPlayer().getID().equals(gun.lastController.getID()))) {
                //Now that we have an accurate motion, check for collisions.
                //First get a damage object to try to attack entities with.
                double amount = definition.bullet.isHeat ? definition.bullet.damage : (velocity / initialVelocity) * definition.bullet.damage * ConfigSystem.settings.damage.bulletDamageFactor.value * ConfigSystem.settings.damage.packBulletDamageFactors.value.get(gun.lastLoadedBullet.definition.packID);
                Damage damage = new Damage(gun, boundingBox, amount);

                //Declare variables that may be used for hit logic.
                AEntityF_Multipart<?> hitMultipart = null;
                Collection<BoundingBoxHitResult> hitMultipartBoxes = null;
                IWrapperEntity hitExternalEntity = null;
                BlockHitResult hitBlock = world.getBlockHit(position, motion);
                
                //Check for collided external entities.
                List<IWrapperEntity> attackedEntities = world.attackEntities(damage, motion, true);
                for (IWrapperEntity entity : attackedEntities) {
                    //Check to make sure we don't hit our controller.
                    //This can happen with hand-held guns at speed.
                    if (!entity.equals(gun.lastController)) {
                        //Make sure there's not a block in the way.
                        if (hitBlock != null && position.isFirstCloserThanSecond(hitBlock.hitPosition, entity.getPosition())) {
                            continue;
                        }

                        //Check if already-found entity is closer.
                        if (hitExternalEntity != null) {
                            //Need to use helper here since the position object will be re-used on next call to other entity.
                            helperPoint.set(hitExternalEntity.getPosition());
                            if (position.isFirstCloserThanSecond(helperPoint, entity.getPosition())) {
                                continue;
                            }
                        }
                        hitExternalEntity = entity;
                    }
                }

                //If we hit a entity, and we have a block hit, we need to discard the block.
                //The only way tne entity could be hit is if it was in front of the block, and thus the block shouldn't be hit.
                if (hitExternalEntity != null) {
                    hitBlock = null;
                }

                //Populate multiparts for following functions.
                multiparts.clear();
                multiparts.addAll(world.getEntitiesOfType(EntityVehicleF_Physics.class));
                multiparts.addAll(world.getEntitiesOfType(EntityPlacedPart.class));

                //Check for collided internal entities.
                //This is a bit more involved, as we need to check all possible types and check hitbox distance.
                Point3D endPoint = position.copy().add(motion);
                BoundingBox bulletMovementBounds = new BoundingBox(position, endPoint);
                for (AEntityF_Multipart<?> multipart : multiparts) {
                    //Don't attack the entity that has the gun that fired us.
                    if (!multipart.allParts.contains(gun)) {
                        Collection<BoundingBoxHitResult> hitResults = multipart.getHitBoxes(position, endPoint, bulletMovementBounds, true);
                        if (hitResults != null) {
                            //Check boxes hit in the last-found multipart against each other to pick the closest part.
                            boolean anyHitboxCanBeHit = false;
                            for (BoundingBoxHitResult hitResult : hitResults) {
                                boolean hitboxCanBeHit = true;

                                //Check the prior multipart, if any of its hit hitboxes are closer, we can't be hit.
                                if(hitMultipart != null) {
                                    for (BoundingBoxHitResult oldHitResult : hitMultipartBoxes) {
                                        if (position.isFirstCloserThanSecond(oldHitResult.position, hitResult.position)) {
                                            hitboxCanBeHit = false;
                                            break;
                                        }
                                    }
                                    if (!hitboxCanBeHit) {
                                        break;
                                    }
                                }
                                
                                //Can't hit hitboxes behind blocks.
                                if (hitboxCanBeHit && hitBlock != null && position.isFirstCloserThanSecond(hitBlock.hitPosition, hitResult.position)) {
                                    hitboxCanBeHit = false;
                                }

                                //Can't hit hitboxes behind other entities.
                                if (hitboxCanBeHit && hitExternalEntity != null && position.isFirstCloserThanSecond(hitExternalEntity.getPosition(), hitResult.position)) {
                                    hitboxCanBeHit = false;
                                }

                                if (hitboxCanBeHit) {
                                    anyHitboxCanBeHit = true;
                                    break;
                                }
                            }

                            if (anyHitboxCanBeHit) {
                                hitMultipart = multipart;
                                hitMultipartBoxes = hitResults;
                            }
                        }
                    }
                }

                //At this point, whatever we have to attack should be attacked.
                //First attack the entity, since the bullet could go through it and hit a entity or block on the other side.
                if (hitMultipart != null && hitMultipart.attackProjectile(damage, this, hitMultipartBoxes) != null) {
                    return;
                }
                
                //Now attack the block or external entity, if we have either.
                if (hitExternalEntity != null) {
                    if (world.isClient()) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitExternalEntity(hitExternalEntity, damage));
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(gun, bulletNumber, hitExternalEntity.getPosition(), Axis.getFromVector(motion), HitType.ENTITY));
                        waitingOnActionPacket = true;
                    } else {
                        performExternalEntityHitLogic(hitExternalEntity, damage);
                        performGenericHitLogic(gun, bulletNumber, hitExternalEntity.getPosition(), Axis.getFromVector(motion), HitType.ENTITY);
                    }
                    displayDebugMessage("HIT MC ENTITY " + hitExternalEntity.getName());
                    return;
                }

                if (hitBlock != null) {
                    //Need to remove a super small amount of position if we hit a positive side on the block.
                    //If we don't, our actual position will be in the wrong block.
                    if (hitBlock.side.xOffset > 0) {
                        hitBlock.hitPosition.x -= 0.000001;
                    }
                    if (hitBlock.side.yOffset > 0) {
                        hitBlock.hitPosition.y -= 0.000001;
                    }
                    if (hitBlock.side.zOffset > 0) {
                        hitBlock.hitPosition.z -= 0.000001;
                    }
                    if (world.isClient()) {
                        //It is CRITICAL that the generic packet gets sent first.  This allows the bullet on the client to get the request for
                        //particles and sounds prior to the request from the internal system for the destruction of this block.
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(gun, bulletNumber, hitBlock.hitPosition, hitBlock.side, HitType.BLOCK));
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitBlock(gun, bulletNumber, hitBlock.blockPosition, hitBlock.side));
                        waitingOnActionPacket = true;
                    } else {
                        performGenericHitLogic(gun, bulletNumber, hitBlock.hitPosition, hitBlock.side, HitType.BLOCK);
                        performBlockHitLogic(gun, bulletNumber, hitBlock.blockPosition, hitBlock.side);
                    }
                    displayDebugMessage("HIT BLOCK AT " + hitBlock.blockPosition + " WITH ACTUAL POSITION " + hitBlock.hitPosition);
                    return;
                }

                //Hit nothing this tick.  Check proximity fuze against our target and blocks.
                if (definition.bullet.proximityFuze != 0 && distanceTraveled > definition.bullet.proximityFuze * 3) {
                    HitType hitType = null;
                    Point3D targetToHit = null;
                    if (targetPosition != null) {
                        //Have an entity target, check if we got close enough to them.
                        if (position.distanceTo(targetPosition) < definition.bullet.proximityFuze + velocity) {
                            targetToHit = targetPosition;
                            hitType = engineTargeted != null ? HitType.VEHICLE : HitType.ENTITY;
                            displayDebugMessage("PROX FUZE HIT TRACKED TARGET");
                        }
                    } else {
                        //No entity target, first check blocks.
                        hitBlock = world.getBlockHit(position, motion.copy().normalize().scale(definition.bullet.proximityFuze + velocity));
                        if (hitBlock != null) {
                            targetToHit = hitBlock.hitPosition;
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
                                    //Don't attack the entity that has the gun that fired us.
                                    if (!multipart.allParts.contains(gun)) {
                                        if (multipart.encompassingBox.intersects(proxBounds)) {
                                            //Could have hit this multipart, check all boxes.
                                            for (BoundingBox box : multipart.allCollisionBoxes) {
                                                if ((box.collisionTypes.contains(CollisionType.ATTACK) || box.collisionTypes.contains(CollisionType.BULLET)) && box.globalCenter.isDistanceToCloserThan(proxBounds.globalCenter, definition.bullet.proximityFuze)) {
                                                    targetToHit = box.globalCenter.copy();
                                                    hitType = HitType.VEHICLE;
                                                    displayDebugMessage("PROX FUZE HIT VEHICLE");
                                                    break;
                                                }
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
                                        Point3D entityPos = entity.getPosition();
                                        if (entityPos.isDistanceToCloserThan(proxBounds.globalCenter, definition.bullet.proximityFuze)) {
                                            targetToHit = entityPos.copy();
                                            hitType = HitType.ENTITY;
                                            displayDebugMessage("PROX FUZE HIT ENTITY " + entity.getName());
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
                    if (hitType != null) {
                        double distanceToTarget = position.distanceTo(targetToHit);
                        if (distanceToTarget > definition.bullet.proximityFuze) {
                            //We will hit this target this tick, but we need to move right to the prox distance before detonating.
                            position.interpolate(targetToHit, (distanceToTarget - definition.bullet.proximityFuze) / definition.bullet.proximityFuze);
                        }
                        if (world.isClient()) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(gun, bulletNumber, position, Axis.getFromVector(motion), hitType));
                            waitingOnActionPacket = true;
                        } else {
                            performGenericHitLogic(gun, bulletNumber, position, Axis.getFromVector(motion), hitType);
                        }
                        return;
                    }
                }

                //Didn't hit a block either. Check the air-burst time, if it was used.
                if (definition.bullet.airBurstDelay != 0) {
                    if (ticksExisted > definition.bullet.airBurstDelay) {
                        if (world.isClient()) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(gun, bulletNumber, position, Axis.NONE, HitType.BURST));
                            waitingOnActionPacket = true;
                        } else {
                            performGenericHitLogic(gun, bulletNumber, position, Axis.NONE, HitType.BURST);
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

    public static void performEntityHitLogic(AEntityE_Interactable<?> entity, Damage damage) {
        if (!entity.world.isClient()) {
            entity.attack(damage);
        }
    }

    public static void performExternalEntityHitLogic(IWrapperEntity entity, Damage damage) {
        if (!entity.getWorld().isClient()) {
            entity.attack(damage);
        }
    }

    public static void performBlockHitLogic(PartGun gun, int bulletNumber, Point3D blockPosition, Axis blockSide) {
        //This is for block state-changes.  Particles and animations are handled in generic.
        if (!gun.world.isClient()) {
            if (gun.lastLoadedBullet.definition.bullet.types.contains(BulletType.WATER)) {
                gun.world.extinguish(blockPosition, blockSide);
            } else if (ConfigSystem.settings.damage.bulletBlockBreaking.value) {
                float hardnessHit = gun.world.getBlockHardness(blockPosition);
                if (hardnessHit > 0 && hardnessHit <= (Math.random() * 0.3F + 0.3F * gun.lastLoadedBullet.definition.bullet.diameter / 20F)) {
                    gun.world.destroyBlock(blockPosition, true);
                } else if (gun.lastLoadedBullet.definition.bullet.types.contains(BulletType.INCENDIARY)) {
                    //Couldn't break block, but we might be able to set it on fire.
                    gun.world.setToFire(blockPosition, blockSide);
                }
            }
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitBlock(gun, bulletNumber, blockPosition, blockSide));
        } else if (gun.lastLoadedBullet.definition.bullet.types.isEmpty()) {
        	//Don't do a state-change on the client, just make a breaking sound.
            //Fancy bullets don't make block-breaking sounds.  They do other things instead.
            InterfaceManager.clientInterface.playBlockBreakSound(blockPosition);
        }
    }

    public static void performGenericHitLogic(PartGun gun, int bulletNumber, Point3D position, Axis hitSide, HitType hitType) {
        //Query up return packets first.  This ensures that we get to do this generic logic which spawns particles on clients before
        //any block-breaking packets arrive.
        if (!gun.world.isClient()) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityBulletHitGeneric(gun, bulletNumber, position, hitSide, hitType));
        }

        //Spawn an explosion if we are an explosive bullet on the server.
        if (!gun.world.isClient() && ConfigSystem.settings.damage.bulletExplosions.value && gun.lastLoadedBullet.definition.bullet.types.contains(BulletType.EXPLOSIVE)) {
            float blastSize = gun.lastLoadedBullet.definition.bullet.blastStrength == 0 ? gun.lastLoadedBullet.definition.bullet.diameter / 10F : gun.lastLoadedBullet.definition.bullet.blastStrength;
            gun.world.spawnExplosion(position, blastSize, gun.lastLoadedBullet.definition.bullet.types.contains(BulletType.INCENDIARY));
        }

        EntityBullet bullet = gun.world.getBullet(gun.uniqueUUID, bulletNumber);
        if (bullet != null) {
            bullet.position.set(position);
            bullet.lastHit = hitType;
            bullet.sideHit = hitSide;
            bullet.impactDespawnTimer = bullet.definition.bullet.impactDespawnTime;

            //If we are on the client, do one last particle check.
            //This lets systems query the blocks we hit before the server adjusts them the next tick.
            if (bullet.world.isClient()) {
                bullet.spawnParticles(0);
                bullet.updateSounds(0);
            }
        }
        if (gun.currentBullet != null && gun.currentBullet.bulletNumber <= bulletNumber) {
            gun.currentBullet = null;
        }
    }

    public void displayDebugMessage(String message) {
        if (ConfigSystem.settings.general.devMode.value && gun.lastController instanceof IWrapperPlayer) {
            if (!world.isClient()) {
                IWrapperPlayer player = (IWrapperPlayer) gun.lastController;
                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.SYSTEM_DEBUG, message));
            } else {
                ((IWrapperPlayer) gun.lastController).displayChatMessage(LanguageSystem.SYSTEM_DEBUG, message);
            }
        }
    }

    @Override
    public ComputedVariable createComputedVariable(String variable) {
        switch (variable) {
            case ("bullet_hit"):
                return new ComputedVariable(this, variable, partialTicks -> lastHit != null ? 1 : 0, false);
            case ("bullet_burntime"):
                return new ComputedVariable(this, variable, partialTicks -> ticksExisted > definition.bullet.burnTime ? 0 : definition.bullet.burnTime - ticksExisted, false);
            case ("bullet_hit_block"):
                return new ComputedVariable(this, variable, partialTicks -> HitType.BLOCK == lastHit ? 1 : 0, false);
            case ("bullet_hit_entity"):
                return new ComputedVariable(this, variable, partialTicks -> HitType.ENTITY == lastHit ? 1 : 0, false);
            case ("bullet_hit_vehicle"):
                return new ComputedVariable(this, variable, partialTicks -> HitType.VEHICLE == lastHit ? 1 : 0, false);
            case ("bullet_hit_armor"):
                return new ComputedVariable(this, variable, partialTicks -> HitType.ARMOR == lastHit ? 1 : 0, false);
            case ("bullet_hit_burst"):
                return new ComputedVariable(this, variable, partialTicks -> HitType.BURST == lastHit ? 1 : 0, false);
            default:
                return super.createComputedVariable(variable);
        }
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
