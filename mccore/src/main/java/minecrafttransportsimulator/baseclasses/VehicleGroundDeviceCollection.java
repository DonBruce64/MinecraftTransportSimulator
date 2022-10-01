package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * This class is a collection for a set of four vehicle ground device points.  This allows for less
 * boilerplate code when we need to do operations on all four points in a vehicle.
 *
 * @author don_bruce
 */
public class VehicleGroundDeviceCollection {
    private final EntityVehicleF_Physics vehicle;
    private final VehicleGroundDeviceBox frontLeftGDB;
    private final VehicleGroundDeviceBox frontRightGDB;
    private final VehicleGroundDeviceBox rearLeftGDB;
    private final VehicleGroundDeviceBox rearRightGDB;
    private final Point3D translationApplied = new Point3D();
    private final RotationMatrix rotationApplied = new RotationMatrix();
    private final TransformationMatrix transformApplied = new TransformationMatrix();
    public final Set<PartGroundDevice> groundedGroundDevices = new HashSet<>();

    public VehicleGroundDeviceCollection(EntityVehicleF_Physics vehicle) {
        this.vehicle = vehicle;
        this.frontLeftGDB = new VehicleGroundDeviceBox(vehicle, true, true);
        this.frontRightGDB = new VehicleGroundDeviceBox(vehicle, true, false);
        this.rearLeftGDB = new VehicleGroundDeviceBox(vehicle, false, true);
        this.rearRightGDB = new VehicleGroundDeviceBox(vehicle, false, false);
    }

    /**
     * Updates the members of all GDBs.
     */
    public void updateMembers() {
        frontLeftGDB.updateMembers();
        frontRightGDB.updateMembers();
        rearLeftGDB.updateMembers();
        rearRightGDB.updateMembers();
    }

    /**
     * Updates the bounding boxes for all GDBs.
     */
    public void updateBounds() {
        frontLeftGDB.updateBounds();
        frontRightGDB.updateBounds();
        rearLeftGDB.updateBounds();
        rearRightGDB.updateBounds();
    }

    /**
     * Updates all the boxes collision properties to take into account their new positions.
     * Also re-calculates which ground devices are on the ground.
     */
    public void updateCollisions() {
        groundedGroundDevices.clear();
        frontLeftGDB.updateCollisionStatuses(groundedGroundDevices);
        frontRightGDB.updateCollisionStatuses(groundedGroundDevices);
        rearLeftGDB.updateCollisionStatuses(groundedGroundDevices);
        rearRightGDB.updateCollisionStatuses(groundedGroundDevices);
    }

    /**
     * Gets the max collision depth for all boxes.
     */
    public double getMaxCollisionDepth() {
        double maxDepth = frontLeftGDB.collisionDepth;
        if (frontRightGDB.collisionDepth > maxDepth) {
            maxDepth = frontRightGDB.collisionDepth;
        }
        if (rearLeftGDB.collisionDepth > maxDepth) {
            maxDepth = rearLeftGDB.collisionDepth;
        }
        if (rearRightGDB.collisionDepth > maxDepth) {
            maxDepth = rearRightGDB.collisionDepth;
        }
        return maxDepth;
    }

    /**
     * Gets the number of boxes in liquid.  Only valid if the box is allowed
     * to collide with liquids.
     */
    public int getNumberBoxesInLiquid() {
        int count = 0;
        if (frontLeftGDB.isCollidedLiquid || frontLeftGDB.isGroundedLiquid) {
            ++count;
        }
        if (frontRightGDB.isCollidedLiquid || frontRightGDB.isGroundedLiquid) {
            ++count;
        }
        if (rearLeftGDB.isCollidedLiquid || rearLeftGDB.isGroundedLiquid) {
            ++count;
        }
        if (rearRightGDB.isCollidedLiquid || rearRightGDB.isGroundedLiquid) {
            ++count;
        }
        return count;
    }

    /**
     * Gets the bounding boxes that make up this ground collective.
     * This will be the four ground points, or less if we don't have them.
     */
    public List<BoundingBox> getGroundBounds() {
        List<BoundingBox> groundBoxes = new ArrayList<>();
        groundBoxes.add(frontLeftGDB.getBoundingBox());
        groundBoxes.add(frontRightGDB.getBoundingBox());
        groundBoxes.add(rearLeftGDB.getBoundingBox());
        groundBoxes.add(rearRightGDB.getBoundingBox());
        return groundBoxes;
    }

    /**
     * Return the following point for this collective for either the front or rear vehicle points.
     * This is based on the average of the contact points for the ground devices.
     * If there are no ground devices for the contact point, null is returned.
     * Note that this point is in the vehicle's local coordinates.
     */
    public Point3D getContactPoint(boolean front) {
        if (front) {
            if (frontLeftGDB.contactPoint.isZero()) {
                if (frontRightGDB.contactPoint.isZero()) {
                    return null;
                } else {
                    return frontRightGDB.contactPoint.copy().add(PartGroundDevice.groundDetectionOffset);
                }
            } else {
                if (frontRightGDB.contactPoint.isZero()) {
                    return frontRightGDB.contactPoint.copy().add(PartGroundDevice.groundDetectionOffset);
                } else {
                    return frontRightGDB.contactPoint.copy().subtract(frontLeftGDB.contactPoint).scale(0.5).add(frontLeftGDB.contactPoint).add(PartGroundDevice.groundDetectionOffset);
                }
            }
        } else {
            if (rearLeftGDB.contactPoint.isZero()) {
                if (rearRightGDB.contactPoint.isZero()) {
                    return null;
                } else {
                    return rearRightGDB.contactPoint.copy().add(PartGroundDevice.groundDetectionOffset);
                }
            } else {
                if (rearRightGDB.contactPoint.isZero()) {
                    return rearRightGDB.contactPoint.copy().add(PartGroundDevice.groundDetectionOffset);
                } else {
                    return rearRightGDB.contactPoint.copy().subtract(rearLeftGDB.contactPoint).scale(0.5).add(rearLeftGDB.contactPoint).add(PartGroundDevice.groundDetectionOffset);
                }
            }
        }
    }

    /**
     * Returns true if the boxes are ready for ground calculations.  In essence, this checks for a front and back box,
     * plus a left or right box if one of those boxes aren't centered.
     */
    public boolean isReady() {
        boolean haveFrontPoint = false;
        boolean haveRearPoint = false;
        boolean haveCenterPoint = false;
        if (frontLeftGDB.isReady()) {
            haveFrontPoint = true;
            haveCenterPoint = frontLeftGDB.contactPoint.x == 0;
        }
        if (frontRightGDB.isReady()) {
            if (haveFrontPoint) {
                haveCenterPoint = true;
            } else {
                haveFrontPoint = true;
            }
            if (!haveCenterPoint) {
                haveCenterPoint = frontRightGDB.contactPoint.x == 0;
            }
        }
        if (haveFrontPoint) {
            if (rearLeftGDB.isReady()) {
                haveRearPoint = true;
                haveCenterPoint = rearLeftGDB.contactPoint.x == 0;
            }
            if (rearRightGDB.isReady()) {
                if (haveRearPoint) {
                    haveCenterPoint = true;
                } else {
                    haveRearPoint = true;
                }
                if (!haveCenterPoint) {
                    haveCenterPoint = rearRightGDB.contactPoint.x == 0;
                }
            }
        }
        return haveFrontPoint && haveRearPoint && haveCenterPoint;
    }

    /**
     * Returns true if any devices are on the ground.
     */
    public boolean isAnythingOnGround() {
        return !frontLeftGDB.isGrounded && !frontRightGDB.isGrounded && !rearLeftGDB.isGrounded && !rearRightGDB.isGrounded;
    }

    /**
     * Returns true if the passed-in device is actually on the ground.
     * This is different than the {@link #groundedGroundDevices}, as
     * this is the actual on-ground state rather than the state where physics
     * calculations can be performed.
     */
    public boolean isActuallyOnGround(PartGroundDevice groundDevice) {
        if (frontLeftGDB.isPartofBox(groundDevice))
            return frontLeftGDB.isGrounded;
        if (frontRightGDB.isPartofBox(groundDevice))
            return frontRightGDB.isGrounded;
        if (rearLeftGDB.isPartofBox(groundDevice))
            return rearLeftGDB.isGrounded;
        if (rearRightGDB.isPartofBox(groundDevice))
            return rearRightGDB.isGrounded;
        return false;
    }

    /**
     * Returns true if the boxes in this collective can do roll operations.
     * More formally, it checks that they aren't all aligned on the Z-axis.
     */
    public boolean canDoRollChecks() {
        double xAxisPoint = 0;
        if (frontLeftGDB != null) {
            xAxisPoint = frontLeftGDB.contactPoint.x;
        }
        if (frontRightGDB != null && xAxisPoint == 0) {
            xAxisPoint = frontRightGDB.contactPoint.x;
        }
        if (rearLeftGDB != null && xAxisPoint == 0) {
            xAxisPoint = rearLeftGDB.contactPoint.x;
        }
        if (rearRightGDB != null && xAxisPoint == 0) {
            xAxisPoint = rearRightGDB.contactPoint.x;
        }
        return xAxisPoint != 0;
    }

    /**
     * Corrects pitch for the GDBs.
     * This amount is determined by checking which GDBs are on the ground, and which are free.
     * Rotation angles are adjusted internally and then groundMotion is modified to level everything out.
     * Actual motion and position are not changed, despite rotation being.
     */
    public void performPitchCorrection(Point3D groundMotion) {
        if (vehicle.towedByConnection == null) {
            //Counter-clockwise rotation if both rear wheels are airborne
            if (rearLeftGDB.isAirborne && rearLeftGDB.isReady() && rearRightGDB.isAirborne && rearRightGDB.isReady()) {
                //Make sure front is not airborne on at least one wheel before rotating.
                if (!frontLeftGDB.isAirborne) {
                    if (!frontRightGDB.isAirborne) {
                        adjustAnglesMatrix(frontLeftGDB.contactPoint.z > frontRightGDB.contactPoint.z ? frontLeftGDB.contactPoint : frontRightGDB.contactPoint, rearLeftGDB, rearRightGDB, false, true, true, groundMotion);
                    } else {
                        adjustAnglesMatrix(frontLeftGDB.contactPoint, rearLeftGDB, rearRightGDB, false, true, true, groundMotion);
                    }
                    return;
                } else if (!frontRightGDB.isAirborne) {
                    adjustAnglesMatrix(frontRightGDB.contactPoint, rearLeftGDB, rearRightGDB, false, true, true, groundMotion);
                    return;
                }
            }

            //Clockwise rotation if both front wheels are airborne
            if (frontLeftGDB.isAirborne && frontLeftGDB.isReady() && frontRightGDB.isAirborne && frontRightGDB.isReady()) {
                //Make sure rear is not airborne on at least one wheel before rotating.
                if (!rearLeftGDB.isAirborne) {
                    if (!rearRightGDB.isAirborne) {
                        adjustAnglesMatrix(rearLeftGDB.contactPoint.z < rearRightGDB.contactPoint.z ? rearLeftGDB.contactPoint : rearRightGDB.contactPoint, frontLeftGDB, frontRightGDB, true, true, true, groundMotion);
                    } else {
                        adjustAnglesMatrix(rearLeftGDB.contactPoint, frontLeftGDB, frontRightGDB, true, true, true, groundMotion);
                    }
                    return;
                } else if (!rearRightGDB.isAirborne) {
                    adjustAnglesMatrix(rearRightGDB.contactPoint, frontLeftGDB, frontRightGDB, true, true, true, groundMotion);
                    return;
                }
            }

            //Counter-clockwise rotation if a front wheel is collided.
            //Make sure rear is grounded and not collided on both wheels before continuing.
            //This allows roll to take over if required.
            if (frontLeftGDB.isCollided || frontRightGDB.isCollided) {
                if (!vehicle.world.isClient() || (frontLeftGDB.collisionDepth > 0.1 || frontRightGDB.collisionDepth > 0.1)) {
                    if (rearLeftGDB.isGrounded && !rearLeftGDB.isCollided && rearRightGDB.isGrounded && !rearRightGDB.isCollided) {
                        adjustAnglesMatrix(rearLeftGDB.contactPoint.z < rearRightGDB.contactPoint.z ? rearLeftGDB.contactPoint : rearRightGDB.contactPoint, frontLeftGDB, frontRightGDB, false, true, true, groundMotion);
                        return;
                    }
                }
            }

            //Clockwise rotation if a rear wheel is collided.
            //Make sure front is grounded and not collided on both wheels before continuing.
            //Also make sure that we are on the server, or on a client with a significant enough collision to matter.
            //This allows roll to take over if required.
            if (rearLeftGDB.isCollided || rearRightGDB.isCollided) {
                if (!vehicle.world.isClient() || rearLeftGDB.collisionDepth > 0.1) {
                    if (frontLeftGDB.isGrounded && !frontLeftGDB.isCollided && frontRightGDB.isGrounded && !frontRightGDB.isCollided) {
                        adjustAnglesMatrix(frontLeftGDB.contactPoint.z > frontRightGDB.contactPoint.z ? frontLeftGDB.contactPoint : frontRightGDB.contactPoint, rearLeftGDB, rearRightGDB, true, true, true, groundMotion);
                    }
                }
            }
        } else {
            //Check if we are connected on the front or back.
            Point3D hookupRelativePosition = vehicle.towedByConnection.hookupCurrentPosition.copy().subtract(vehicle.position).reOrigin(vehicle.orientation);
            if (hookupRelativePosition.z > 0) {
                //Hookup is in front of the vehicle, need to rotate clockwise if wheels are collided, counter-clockwise if free.
                if ((rearLeftGDB.isAirborne || !rearLeftGDB.isReady()) && (rearRightGDB.isAirborne || !rearRightGDB.isReady()) && (rearLeftGDB.isAirborne || !rearLeftGDB.isReady()) && (rearRightGDB.isAirborne || !rearRightGDB.isReady())) {
                    //Ground devices are all free (or just don't exist).  Do rotation to put them on the ground.
                    if (rearLeftGDB.isReady() && rearRightGDB.isReady()) {
                        adjustAnglesMatrix(hookupRelativePosition, rearLeftGDB, rearRightGDB, false, true, true, groundMotion);
                    } else if (frontLeftGDB.isReady() && frontRightGDB.isReady()) {
                        adjustAnglesMatrix(hookupRelativePosition, frontLeftGDB, frontRightGDB, false, true, true, groundMotion);
                    }
                } else {
                    //Ground devices are either collided, or just don't exist.  Do rotation to un-collide them.
                    if ((rearLeftGDB.isCollided || rearRightGDB.isCollided) && rearLeftGDB.isReady() && rearRightGDB.isReady()) {
                        adjustAnglesMatrix(hookupRelativePosition, rearLeftGDB, rearRightGDB, true, true, false, groundMotion);
                    } else if ((frontLeftGDB.isCollided || frontRightGDB.isCollided) && frontLeftGDB.isReady() && frontRightGDB.isReady()) {
                        adjustAnglesMatrix(hookupRelativePosition, frontLeftGDB, frontRightGDB, true, true, false, groundMotion);
                    }
                }
            } else {
                //Hookup is in rear of the vehicle, need to rotate counter-clockwise if wheels are collided, clockwise if free.
                if ((rearLeftGDB.isAirborne || !rearLeftGDB.isReady()) && (rearRightGDB.isAirborne || !rearRightGDB.isReady()) && (rearLeftGDB.isAirborne || !rearLeftGDB.isReady()) && (rearRightGDB.isAirborne || !rearRightGDB.isReady())) {
                    //Ground devices are all free (or just don't exist).  Do rotation to put them on the ground.
                    if (frontLeftGDB.isReady() && frontRightGDB.isReady()) {
                        adjustAnglesMatrix(hookupRelativePosition, frontLeftGDB, frontRightGDB, true, true, true, groundMotion);
                    } else if (rearLeftGDB.isReady() && rearRightGDB.isReady()) {
                        adjustAnglesMatrix(hookupRelativePosition, rearLeftGDB, rearRightGDB, true, true, true, groundMotion);
                    }
                } else {
                    //Ground devices are either collided, or just don't exist.  Do rotation to un-collide them.
                    if ((frontLeftGDB.isCollided || frontRightGDB.isCollided) && frontLeftGDB.isReady() && frontRightGDB.isReady()) {
                        adjustAnglesMatrix(hookupRelativePosition, frontLeftGDB, frontRightGDB, false, true, false, groundMotion);
                    } else if ((rearLeftGDB.isCollided || rearRightGDB.isCollided) && rearLeftGDB.isReady() && rearRightGDB.isReady()) {
                        adjustAnglesMatrix(hookupRelativePosition, rearLeftGDB, rearRightGDB, false, true, false, groundMotion);
                    }
                }
            }
        }
    }

    /**
     * Corrects roll for the GDBs.
     * This amount is determined by checking which GDBs are on the ground, and which are free.
     * Rotation angles are adjusted internally and then groundMotion is modified to level everything out.
     * Actual motion and position are not changed, despite rotation being.
     */
    public void performRollCorrection(Point3D groundMotion) {
        //Counter-clockwise rotation if both left wheels are free
        if (!frontLeftGDB.isCollided && !frontLeftGDB.isGrounded && frontLeftGDB.isReady() && !rearLeftGDB.isCollided && !rearLeftGDB.isGrounded && rearLeftGDB.isReady()) {
            //Make sure right is grounded or collided on at least one wheel before rotating.
            //This prevents us from doing rotation in the air.
            if (frontRightGDB.isCollided || frontRightGDB.isGrounded) {
                if (rearRightGDB.isCollided || rearRightGDB.isGrounded) {
                    adjustAnglesMatrix(frontRightGDB.contactPoint.x < rearRightGDB.contactPoint.x ? frontRightGDB.contactPoint : rearRightGDB.contactPoint, frontLeftGDB, rearLeftGDB, false, false, true, groundMotion);
                } else {
                    adjustAnglesMatrix(frontRightGDB.contactPoint, frontLeftGDB, rearLeftGDB, false, false, true, groundMotion);
                }
            } else if (rearRightGDB.isCollided || rearRightGDB.isGrounded) {
                adjustAnglesMatrix(rearRightGDB.contactPoint, frontLeftGDB, rearLeftGDB, false, false, true, groundMotion);
            }
        }

        //Clockwise rotation if both right wheels are free
        if (!frontRightGDB.isCollided && !frontRightGDB.isGrounded && frontRightGDB.isReady() && !rearRightGDB.isCollided && !rearRightGDB.isGrounded && rearRightGDB.isReady()) {
            //Make sure left is grounded or collided on at least one wheel before rotating.
            //This prevents us from doing rotation in the air.
            if (frontLeftGDB.isCollided || frontLeftGDB.isGrounded) {
                if (rearLeftGDB.isCollided || rearLeftGDB.isGrounded) {
                    adjustAnglesMatrix(frontLeftGDB.contactPoint.x < rearLeftGDB.contactPoint.x ? frontLeftGDB.contactPoint : rearLeftGDB.contactPoint, frontRightGDB, rearRightGDB, true, false, true, groundMotion);
                } else {
                    adjustAnglesMatrix(frontLeftGDB.contactPoint, frontRightGDB, rearRightGDB, true, false, true, groundMotion);
                }
            } else if (rearLeftGDB.isCollided || rearLeftGDB.isGrounded) {
                adjustAnglesMatrix(rearLeftGDB.contactPoint, frontRightGDB, rearRightGDB, true, false, true, groundMotion);
            }
        }
    }

    /**
     * Helper function to adjust angles in a common manner for pitch and roll calculations.
     */
    private void adjustAnglesMatrix(Point3D originPoint, VehicleGroundDeviceBox testBox1, VehicleGroundDeviceBox testBox2, boolean clockwiseRotation, boolean pitch, boolean checkCollisions, Point3D groundMotion) {
        //Get the two box deltas.
        double box1Delta;
        double box2Delta;
        if (pitch) {
            box1Delta = Math.hypot(originPoint.z - testBox1.contactPoint.z, originPoint.y - testBox1.contactPoint.y);
            box2Delta = Math.hypot(originPoint.z - testBox2.contactPoint.z, originPoint.y - testBox2.contactPoint.y);
        } else {
            box1Delta = Math.hypot(originPoint.x - testBox1.contactPoint.x, originPoint.y - testBox1.contactPoint.y);
            box2Delta = Math.hypot(originPoint.x - testBox2.contactPoint.x, originPoint.y - testBox2.contactPoint.y);
        }

        //Get the angle to rotate by based on the farthest points and collision depth.
        double furthestDelta = Math.max(box1Delta, box2Delta);
        if (furthestDelta < ConfigSystem.settings.general.climbSpeed.value) {
            //This is too short of a wheelbase to do this function.
            return;
        }

        //Run though this loop until we have no collisions, or until we get a small enough delta.
        double heightDeltaAttempted = ConfigSystem.settings.general.climbSpeed.value;
        double angleApplied = 0;
        for (; heightDeltaAttempted > PartGroundDevice.groundDetectionOffset.y; heightDeltaAttempted -= ConfigSystem.settings.general.climbSpeed.value / 4) {
            angleApplied = Math.toDegrees(Math.asin(heightDeltaAttempted / furthestDelta));
            if (!clockwiseRotation) {
                angleApplied = -angleApplied;
            }

            //Set the box rotation transform.
            //This is how the box will move given the rotation we are rotating the box about.
            //This is done in the vehicle's local coordinates and applied to the box prior to vehicle offset.
            transformApplied.resetTransforms();
            transformApplied.setTranslation(originPoint);
            if (pitch) {
                rotationApplied.setToZero().rotateX(angleApplied);
            } else {
                rotationApplied.setToZero().rotateZ(angleApplied);
            }
            transformApplied.multiply(rotationApplied);
            transformApplied.applyInvertedTranslation(originPoint);

            //Check for collisions.
            if (!checkCollisions || (testBox1.collidedWithTransform(transformApplied, groundMotion) && testBox2.collidedWithTransform(transformApplied, groundMotion))) {
                break;
            }
        }

        //Rotation is set to appropriate bounds, apply to vehicle and return linear movement.
        //Don't do this if we didn't attempt any delta because we collided on everything.
        if (heightDeltaAttempted != 0) {
            if (pitch) {
                vehicle.rotation.angles.x += angleApplied;
            } else {
                vehicle.rotation.angles.z += angleApplied;
            }
            //Translation applied can be calculated by getting the vector inverted by the origin and applying our rotation to it.
            //This rotates the vehicle's center point locally and obtains a new point.  The delta between these points can then be taken.
            translationApplied.set(0, 0, 0).transform(transformApplied).rotate(vehicle.orientation);
            groundMotion.add(translationApplied);
        }
    }
}
