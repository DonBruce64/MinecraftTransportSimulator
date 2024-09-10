package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition.JSONGroundDevicePosition;

/**
 * This class is a IWrapper for vehicle ground device collision points.  It's used to get a point
 * to reference for ground collisions, and contains helper methods for doing calculations of those
 * points.  Four of these can be used in a set to get four ground device points to use in
 * ground device operations on a vehicle.  Note that this class differentiates between floating
 * and non-floating objects, and includes collision boxes for the latter.  This ensures a
 * seamless transition from a floating to ground state in movement.
 *
 * @author don_bruce
 */
public class VehicleGroundDeviceBox {
    private final EntityVehicleF_Physics vehicle;
    private final boolean isFront;
    private final boolean isLeft;
    private boolean isLongTread;
    private double treadZBestOffset;
    private float climbHeight;
    private static final Set<CollisionType> groundBoxCollisionTypes = new HashSet<>(Arrays.asList(CollisionType.BLOCK));
    private final BoundingBox solidBox = new BoundingBox(new Point3D(), new Point3D(), 0D, 0D, 0D, false, groundBoxCollisionTypes);
    private final BoundingBox liquidBox = new BoundingBox(new Point3D(), new Point3D(), 0D, 0D, 0D, true, groundBoxCollisionTypes);
    private final List<BoundingBox> liquidCollisionBoxes = new ArrayList<>();
    private final List<PartGroundDevice> groundDevices = new ArrayList<>();
    private final List<PartGroundDevice> liquidDevices = new ArrayList<>();
    private final Point3D solidBoxNormalPos = new Point3D();

    public boolean isBlockedVertically;
    public boolean contactedEntity;
    public boolean isAirborne;
    public boolean isCollided;
    public boolean isGrounded;
    public boolean isAbleToDoGroundOperations;
    public boolean isUsingLiquidBoxes;
    public double collisionDepth;
    /**
     * The point where this box contacts the world, in local coords to the vehicle it is on
     **/
    public final Point3D contactPoint = new Point3D();

    private static final Point3D testOffset = new Point3D();
    private static final double MAX_DELTA_FROM_ZERO = 0.00001;

    public VehicleGroundDeviceBox(EntityVehicleF_Physics vehicle, boolean isFront, boolean isLeft) {
        this.vehicle = vehicle;
        this.isFront = isFront;
        this.isLeft = isLeft;
    }

    /**
     * Updates what objects make up this GDB.  These should change as parts are added and removed.
     */
    public void updateMembers() {
        //Get all liquid collision boxes.  Parts can add these via their collision boxes.
        liquidCollisionBoxes.clear();
        for (BoundingBox box : vehicle.allCollisionBoxes) {
            if (box.collisionTypes.contains(CollisionType.BLOCK)) {
                APart partOn = vehicle.getPartWithBox(box);
                if (box.collidesWithLiquids) {
                    final boolean boxFront;
                    final boolean boxLeft;
                    final boolean boxRight;
                    if (partOn != null) {
                        Point3D relativePosition = partOn.position.copy().subtract(partOn.vehicleOn.position).reOrigin(partOn.vehicleOn.orientation);
                        boxFront = relativePosition.z > 0;
                        boxLeft = relativePosition.x >= -MAX_DELTA_FROM_ZERO;
                        boxRight = relativePosition.x <= MAX_DELTA_FROM_ZERO;
                    } else {
                        boxFront = box.localCenter.z > 0;
                        boxLeft = box.localCenter.x >= 0;
                        boxRight = box.localCenter.x <= 0;
                    }
                    if (isFront && boxFront) {
                        if (isLeft && boxLeft) {
                            liquidCollisionBoxes.add(box);
                        } else if (!isLeft && boxRight) {
                            liquidCollisionBoxes.add(box);
                        }
                    } else if (!isFront && !boxFront) {
                        if (isLeft && boxLeft) {
                            liquidCollisionBoxes.add(box);
                        } else if (!isLeft && boxRight) {
                            liquidCollisionBoxes.add(box);
                        }
                    }
                }
            }
        }

        //Get all part-based collision boxes.  This includes solid and liquid ground devices.
        groundDevices.clear();
        liquidDevices.clear();
        float totalClimbHeight = 0;
        for (APart part : vehicle.allParts) {
            if (part instanceof PartGroundDevice) {
                if (!part.isSpare) {
                    PartGroundDevice ground = (PartGroundDevice) part;
                    APart currentPart = part;

                    //Get a JSON-defined ground position from our placement def, or that from a parent if we're sub-parted.
                    JSONGroundDevicePosition groundPosition;
                    do {
                        groundPosition = currentPart.placementDefinition.groundDevicePosition;
                        if (groundPosition == null) {
                            if (currentPart.entityOn instanceof APart) {
                                currentPart = (APart) currentPart.entityOn;
                            } else {
                                currentPart = null;
                            }
                        }
                    } while (groundPosition == null && currentPart != null);

                    //Create a position if none is defined.
                    if (groundPosition == null) {
                        if (ground.wheelbasePoint.z > 0) {
                            if (ground.wheelbasePoint.x == 0) {
                                groundPosition = JSONGroundDevicePosition.FRONT_CENTER;
                            } else if (ground.wheelbasePoint.x >= 0) {
                                groundPosition = JSONGroundDevicePosition.FRONT_LEFT;
                            } else {
                                groundPosition = JSONGroundDevicePosition.FRONT_RIGHT;
                            }
                        } else {
                            if (ground.wheelbasePoint.x == 0) {
                                groundPosition = JSONGroundDevicePosition.REAR_CENTER;
                            } else if (ground.wheelbasePoint.x >= 0) {
                                groundPosition = JSONGroundDevicePosition.REAR_LEFT;
                            } else {
                                groundPosition = JSONGroundDevicePosition.REAR_RIGHT;
                            }
                        }
                    }

                    //Add us if our requested position matches our found position.
                    if (!(isFront ^ groundPosition.isFront) && (isLeft && groundPosition.isLeft || !isLeft && groundPosition.isRight)) {
                        groundDevices.add(ground);
                        totalClimbHeight += ground.definition.ground.climbHeight;
                        if (ground.definition.ground.canFloat) {
                            liquidDevices.add(ground);
                        }
                    }
                }
            }
        }
        this.climbHeight = !groundDevices.isEmpty() ? totalClimbHeight / groundDevices.size() : 0;
    }

    /**
     * Updates this boxes' bounds to match the included members.  This should only be done when we
     * change members, or if a member has changed position/size in the local sphere.
     */
    public void updateBounds() {
        //Update solid box local center and size.
        //We use the lowest-contacting ground device for size.
        //Position is average for XZ, and min for Y.
        solidBox.localCenter.set(0D, Double.MAX_VALUE, 0D);
        solidBox.widthRadius = 0;
        solidBox.heightRadius = 0;
        isLongTread = false;
        for (APart groundDevice : groundDevices) {
            solidBox.localCenter.x += groundDevice.localOffset.x;
            solidBox.localCenter.z += groundDevice.localOffset.z;
            if (groundDevice.localOffset.y - groundDevice.getHeight() / 2D < solidBox.localCenter.y - solidBox.heightRadius) {
                solidBox.localCenter.y = groundDevice.localOffset.y;
                solidBox.heightRadius = groundDevice.getHeight() / 2D;
                solidBox.widthRadius = groundDevice.getWidth() / 2D;
            }
            if (groundDevice.definition.ground.isTread && ((PartGroundDevice) groundDevice).getLongPartOffset() != 0) {
                isLongTread = true;
            }
        }
        solidBox.depthRadius = solidBox.widthRadius;
        solidBox.localCenter.x *= 1D / groundDevices.size();
        solidBox.localCenter.z *= 1D / groundDevices.size();
        solidBoxNormalPos.set(solidBox.localCenter);
        treadZBestOffset = solidBoxNormalPos.z;

        //Update liquid box local center and size.
        liquidBox.localCenter.set(0D, Double.MAX_VALUE, 0D);
        liquidBox.widthRadius = 0;
        liquidBox.heightRadius = 0;
        for (APart groundDevice : liquidDevices) {
            liquidBox.localCenter.x += groundDevice.localOffset.x;
            liquidBox.localCenter.z += groundDevice.localOffset.z;
            if (groundDevice.localOffset.y - groundDevice.getHeight() / 2D < liquidBox.localCenter.y - liquidBox.heightRadius) {
                liquidBox.localCenter.y = groundDevice.localOffset.y;
                liquidBox.heightRadius = groundDevice.getHeight() / 2D;
                liquidBox.widthRadius = groundDevice.getWidth() / 2D;
            }
        }
        for (BoundingBox box : liquidCollisionBoxes) {
            liquidBox.localCenter.x += box.localCenter.x;
            liquidBox.localCenter.z += box.localCenter.z;
            if (box.localCenter.y - box.heightRadius < liquidBox.localCenter.y - liquidBox.heightRadius) {
                liquidBox.localCenter.y = box.localCenter.y;
                liquidBox.heightRadius = box.heightRadius;
                liquidBox.widthRadius = box.widthRadius;
            }
        }
        liquidBox.depthRadius = liquidBox.widthRadius;
        liquidBox.localCenter.x *= 1D / (liquidDevices.size() + liquidCollisionBoxes.size());
        liquidBox.localCenter.z *= 1D / (liquidDevices.size() + liquidCollisionBoxes.size());
    }

    /**
     * Updates this boxes' collision properties to take into account its new position.
     * If the passed-in list is non-null, all grounded ground devices will be added to it.
     */
    public void updateCollisionStatuses(Set<PartGroundDevice> groundedGroundDevices, boolean updateGroundDeviceTreadPosition) {
        //Initialize all values.
        isBlockedVertically = false;
        isAirborne = true;
        isCollided = false;
        isGrounded = false;
        isAbleToDoGroundOperations = false;
        isUsingLiquidBoxes = false;
        collisionDepth = 0;

        Point3D vehicleMotionOffset = vehicle.motion.copy().scale(vehicle.speedFactor);
        if (vehicleMotionOffset.y == 0 && vehicle.towedByConnection != null && !vehicle.towedByConnection.hookupConnection.mounted) {
            //Need to add a super-small amount of -y motion here.
            //If we don't, then we won't do trailer physics right as those always need to check for blocks below them.
            vehicleMotionOffset.y = -0.00001;
        }
        Point3D groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundDetectionOffset);
        if (!groundDevices.isEmpty()) {
            if (updateGroundDeviceTreadPosition && isLongTread) {
                //If we are over an air block, move the collision towards the center to account for this.
                //Of all the places we collide, pick the one that's the most collided.
                int treadSteps;
                double treadZCurrentOffset;
                if (solidBoxNormalPos.z > 0) {
                    treadSteps = (int) (Math.floor(solidBoxNormalPos.z) + 1) * 2;
                } else {
                    treadSteps = (int) (Math.floor(-solidBoxNormalPos.z) + 1) * 2;
                }
                treadZCurrentOffset = solidBoxNormalPos.z;
                treadZBestOffset = solidBoxNormalPos.z;

                boolean foundCollidingBlock = false;
                for (int currentStep = 0; currentStep <= treadSteps; ++currentStep) {
                    solidBox.localCenter.set(solidBoxNormalPos);
                    solidBox.localCenter.z = treadZCurrentOffset;
                    contactPoint.set(solidBox.localCenter);
                    contactPoint.add(0D, -solidBox.heightRadius, 0D);
                    solidBox.globalCenter.set(solidBox.localCenter).rotate(vehicle.orientation).rotate(vehicle.rotation).add(vehicle.position).add(vehicleMotionOffset);

                    //Check for air at position and below.  We might not be colliding with any blocks, but might have air below some.
                    //We need to not be either in or on top of air blocks to function properly.
                    boolean airAtPosition = vehicle.world.isAir(solidBox.globalCenter);
                    if (airAtPosition) {
                        --solidBox.globalCenter.y;
                        if (!vehicle.world.isAir(solidBox.globalCenter)) {
                            airAtPosition = false;
                        }
                        ++solidBox.globalCenter.y;
                    }
                    if (!airAtPosition) {
                        //Check the collisions at this solid block area to see if it's more collided than others.
                        //Use a debounce of 0.02 here to prevent small collisions from changing the boxes constantly.
                        vehicle.world.updateBoundingBoxCollisions(solidBox, vehicleMotionOffset, false);
                        if (-solidBox.currentCollisionDepth.y > collisionDepth) {
                            foundCollidingBlock = true;
                            collisionDepth = -solidBox.currentCollisionDepth.y;
                            treadZBestOffset = treadZCurrentOffset;
                        }
                    }

                    //Adjust offset and try again, if applicable.
                    treadZCurrentOffset -= solidBoxNormalPos.z / treadSteps;
                }

                //If we never found a colliding block to have a good offset, re-do some calculations since these won't have been done before.
                if (!foundCollidingBlock) {
                    solidBox.localCenter.set(solidBoxNormalPos);
                    solidBox.localCenter.z = treadZBestOffset;
                    contactPoint.set(solidBox.localCenter);
                    contactPoint.add(0D, -solidBox.heightRadius, 0D);
                    solidBox.globalCenter.set(solidBox.localCenter).rotate(vehicle.orientation).rotate(vehicle.rotation).add(vehicle.position).add(vehicleMotionOffset);
                    vehicle.world.updateBoundingBoxCollisions(solidBox, vehicleMotionOffset, false);
                }

                //Always check for entities here since we don't in the above loop.
                contactedEntity = checkEntityCollisions(vehicleMotionOffset);
                isCollided = contactedEntity || !solidBox.collidingBlockPositions.isEmpty();
            } else {
                //Just use current best offset.  For wheels, this will just be 0 and won't matter.
                solidBox.localCenter.set(solidBoxNormalPos);
                solidBox.localCenter.z = treadZBestOffset;
                contactPoint.set(solidBox.localCenter);
                contactPoint.add(0D, -solidBox.heightRadius, 0D);
                solidBox.globalCenter.set(solidBox.localCenter).rotate(vehicle.orientation).rotate(vehicle.rotation).add(vehicle.position).add(vehicleMotionOffset);
                vehicle.world.updateBoundingBoxCollisions(solidBox, vehicleMotionOffset, false);
                contactedEntity = checkEntityCollisions(vehicleMotionOffset);
                isCollided = contactedEntity || !solidBox.collidingBlockPositions.isEmpty();
                collisionDepth = -solidBox.currentCollisionDepth.y;
            }

            //If we are collided, do other things now to handle collision.
            if (isCollided) {
                isGrounded = true;
                isAirborne = false;
                isBlockedVertically = true;

                //We search top-down, since it's more efficient with situations where we are within climb height bounds.
                //Make sure our ground devices have thickness though: ground devices can have 0 thickness which would
                //lead to an infinite loop here.
                final float offsetDelta = (float) (solidBox.heightRadius * 2);
                if (offsetDelta > 0) {
                    float offset = (float) (climbHeight + solidBox.heightRadius) + offsetDelta;
                    do {
                        offset -= offsetDelta;
                        if (offset < offsetDelta) {
                            offset = offsetDelta;
                        }
                        testOffset.y = offset;
                        if (!vehicle.world.checkForCollisions(solidBox, testOffset, false, false)) {
                            isBlockedVertically = false;
                            break;
                        }
                    } while (offset != offsetDelta);
                    if (isBlockedVertically) {
                        vehicle.allBlockCollisionBoxes.add(solidBox);
                    }
                }
            } else {
                solidBox.globalCenter.add(PartGroundDevice.groundDetectionOffset);
                vehicle.world.updateBoundingBoxCollisions(solidBox, groundCollisionOffset, false);
                contactedEntity = checkEntityCollisions(groundCollisionOffset);
                solidBox.globalCenter.subtract(PartGroundDevice.groundDetectionOffset);
                isGrounded = contactedEntity || !solidBox.collidingBlockPositions.isEmpty();
            }

            if (isGrounded) {
                isAbleToDoGroundOperations = true;
                isAirborne = false;
            } else {
                groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundOperationOffset);
                solidBox.globalCenter.add(PartGroundDevice.groundOperationOffset);
                vehicle.world.updateBoundingBoxCollisions(solidBox, groundCollisionOffset, false);
                contactedEntity = checkEntityCollisions(groundCollisionOffset);
                solidBox.globalCenter.subtract(PartGroundDevice.groundOperationOffset);
                isAbleToDoGroundOperations = contactedEntity || !solidBox.collidingBlockPositions.isEmpty();
            }
        }

        //Try liquids if we don't have the solid boxes on the ground.
        if (!isAbleToDoGroundOperations && (!liquidDevices.isEmpty() || !liquidCollisionBoxes.isEmpty())) {
            liquidBox.globalCenter.set(liquidBox.localCenter).rotate(vehicle.orientation).rotate(vehicle.rotation).add(vehicle.position).add(vehicleMotionOffset);
            vehicle.world.updateBoundingBoxCollisions(liquidBox, vehicleMotionOffset, false);
            isCollided = !liquidBox.collidingBlockPositions.isEmpty();
            collisionDepth = -liquidBox.currentCollisionDepth.y;

            if (isCollided) {
                isGrounded = true;
                isAirborne = false;
            } else {
                liquidBox.globalCenter.add(PartGroundDevice.groundDetectionOffset);
                vehicle.world.updateBoundingBoxCollisions(liquidBox, groundCollisionOffset, false);
                liquidBox.globalCenter.subtract(PartGroundDevice.groundDetectionOffset);
                isGrounded = !liquidBox.collidingBlockPositions.isEmpty();
            }

            if (isGrounded) {
                isAbleToDoGroundOperations = !liquidDevices.isEmpty();
                isAirborne = false;
            } else {
                groundCollisionOffset = vehicleMotionOffset.copy().add(PartGroundDevice.groundOperationOffset);
                liquidBox.globalCenter.add(PartGroundDevice.groundOperationOffset);
                vehicle.world.updateBoundingBoxCollisions(liquidBox, groundCollisionOffset, false);
                liquidBox.globalCenter.subtract(PartGroundDevice.groundOperationOffset);
                isAbleToDoGroundOperations = !liquidDevices.isEmpty() && !liquidBox.collidingBlockPositions.isEmpty();
            }

            contactPoint.set(liquidBox.localCenter);
            contactPoint.add(0D, -liquidBox.heightRadius, 0D);
            isUsingLiquidBoxes = true;
        } else {
            isUsingLiquidBoxes = false;
        }

        //Add ground devices to the list, but only if we can do ground operations.
        if (groundedGroundDevices != null && isAbleToDoGroundOperations) {
            groundedGroundDevices.addAll(groundDevices);
        }
    }

    /**
     * Returns true if this box if it collides with any boxes using the passed-in transformation.
     * This transformation is a local transform to the vehicle the box is on.  Does not change any state-flags
     * of this box.
     */
    public boolean collidedWithTransform(TransformationMatrix transform, Point3D groundMotion) {
        //Transform operates off contact points, so get the world-based transform delta the transform will apply to our contact point.
        Point3D vehicleMotionOffset = contactPoint.copy().transform(transform).subtract(contactPoint).rotate(vehicle.orientation).rotate(vehicle.rotation).addScaled(vehicle.motion, vehicle.speedFactor).add(groundMotion);
        if (!groundDevices.isEmpty()) {
            if (vehicle.world.checkForCollisions(solidBox, vehicleMotionOffset, false, false)) {
                return false;
            }
        }

        if (!liquidDevices.isEmpty() || !liquidCollisionBoxes.isEmpty()) {
            return !vehicle.world.checkForCollisions(liquidBox, vehicleMotionOffset, false, false);
        }
        return true;
    }

    /**
     * Helper method for checking for entity collisions.
     */
    private boolean checkEntityCollisions(Point3D collisionMotion) {
        boolean didCollision = false;
        for (EntityVehicleF_Physics otherVehicle : vehicle.world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
            if (!otherVehicle.equals(vehicle) && vehicle.canCollideWith(otherVehicle) && !otherVehicle.collidedEntities.contains(vehicle) && otherVehicle.encompassingBox.intersects(solidBox)) {
                //We know we could have hit this entity.  Check if we actually did.
                BoundingBox collidingBox = null;
                double boxCollisionDepth;
                for (BoundingBox box : otherVehicle.allCollisionBoxes) {
                    if (box.collisionTypes.contains(CollisionType.VEHICLE) && box.intersects(solidBox)) {
                        if (collisionMotion.y > 0) {
                            boxCollisionDepth = solidBox.globalCenter.y + solidBox.heightRadius - (box.globalCenter.y - box.heightRadius);
                            if (boxCollisionDepth > solidBox.currentCollisionDepth.y) {
                                solidBox.currentCollisionDepth.y = boxCollisionDepth;
                                collidingBox = box;
                            }
                        } else {
                            boxCollisionDepth = solidBox.globalCenter.y - solidBox.heightRadius - (box.globalCenter.y + box.heightRadius);
                            if (boxCollisionDepth < solidBox.currentCollisionDepth.y) {
                                solidBox.currentCollisionDepth.y = boxCollisionDepth;
                                collidingBox = box;
                            }
                        }

                    }
                }
                if (collidingBox != null) {
                    vehicle.collidedEntities.add(otherVehicle);
                    didCollision = true;
                }
            }
        }
        return didCollision;
    }

    /**
     * Returns true if this box has any boxes and is ready for collision operations.
     */
    public boolean isReady() {
        return !groundDevices.isEmpty() || !liquidCollisionBoxes.isEmpty() || !liquidDevices.isEmpty();
    }

    /**
     * Returns the bounding box that currently represents this box.  This can change depending on what
     * ground devices we have and if we are colliding with liquids or solids.
     */
    public BoundingBox getBoundingBox() {
        return isUsingLiquidBoxes ? liquidBox : solidBox;
    }

    /**
     * Returns true if the passed-in ground device is part of this box.
     */
    public boolean isPartofBox(PartGroundDevice groundDevice) {
        return groundDevices.contains(groundDevice) || liquidDevices.contains(groundDevice);
    }

    /**
     * Returns the list of all the ground devices that are part of this box.
     */
    public List<PartGroundDevice> getGroundDevices() {
        return groundDevices;
    }
}
