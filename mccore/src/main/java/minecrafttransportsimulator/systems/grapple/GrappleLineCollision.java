package minecrafttransportsimulator.systems.grapple;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;

/**
 * Keeps a vehicle on a grapple line out of walls.
 *
 * Vehicles check their collision boxes against blocks as they move, but wheels only against the
 * ground under them.  A line pulling sideways drags the wheels into a wall, and once any box is
 * inside a block every move and turn is blocked, so the vehicle sticks.
 *
 * -{@link #clampToWalls}: cuts motion on any horizontal axis that would put a wheel or box into a
 * block, so the vehicle slides along the wall instead.
 * -{@link #limitTilt}: shrinks a tilt that would swing a wheel or collision box into a block.
 * -{@link #findPushOut}: steps an already-stuck vehicle out a little each tick, towards the nearest free spot.
 *
 * Wheels are checked by their top half only, so the ground they sit on (or a slab they're climbing) doesn't count.
 */
public final class GrappleLineCollision {
    /**How far the vehicle is pushed out of a wall each tick, in blocks.**/
    private static final double PUSH_STEP = 1D / 16D;
    /**How many steps out we search for free space before giving up.  8 steps is half a block.**/
    private static final int PUSH_MAX_STEPS = 8;
    /**Directions we push out in.  Not down, as that's into the ground.**/
    private static final Point3D[] PUSH_DIRECTIONS = {new Point3D(1, 0, 0), new Point3D(-1, 0, 0), new Point3D(0, 0, 1), new Point3D(0, 0, -1), new Point3D(0, 1, 0)};
    /**How many times a blocked move or tilt is halved before it's dropped: full, half, then quarter.**/
    private static final int CUT_TRIES = 3;
    private static final Point3D ZERO = new Point3D();

    private GrappleLineCollision() {
    }

    /**
     * Returns boxes for the top half of each wheel, at the wheel's current position.
     */
    public static List<BoundingBox> getWheelBoxes(EntityVehicleF_Physics vehicle) {
        List<BoundingBox> boxes = new ArrayList<>();
        for (APart part : vehicle.allParts) {
            if (part instanceof PartGroundDevice && !part.placementDefinition.isSpare) {
                BoundingBox wheel = part.boundingBox;
                double halfHeight = wheel.heightRadius / 2D;
                boxes.add(new BoundingBox(wheel.globalCenter.copy().add(0, halfHeight, 0), wheel.widthRadius, halfHeight, wheel.depthRadius));
            }
        }
        return boxes;
    }

    /**
     * Returns the wheel boxes plus the vehicle's block collision boxes.
     * Call this before the vehicle moves, while the collision list only holds the vehicle's own boxes.
     */
    public static List<BoundingBox> getAllBoxes(EntityVehicleF_Physics vehicle) {
        List<BoundingBox> boxes = getWheelBoxes(vehicle);
        boxes.addAll(vehicle.allBlockCollisionBoxes);
        return boxes;
    }

    /**
     * Returns the boxes that aren't already inside a block.  Stuck boxes block every move, so
     * checks that should still let the vehicle move use only these, and {@link #findPushOut} frees the rest.
     */
    public static List<BoundingBox> getFreeBoxes(EntityVehicleF_Physics vehicle, List<BoundingBox> boxes) {
        List<BoundingBox> free = new ArrayList<>();
        for (BoundingBox box : boxes) {
            if (!collides(vehicle, box, ZERO)) {
                free.add(box);
            }
        }
        return free;
    }

    /**
     * Cuts the vehicle's X and Z motion where it would put a wheel or collision box into a block.
     * The rest of the motion is kept, so the vehicle slides along the wall.  Vertical motion is left to
     * the vehicle, as wheels are meant to touch the ground.
     *
     * @return the horizontal movement that was cut, in blocks.
     */
    public static Point3D clampToWalls(EntityVehicleF_Physics vehicle, List<BoundingBox> freeBoxes) {
        Point3D before = new Point3D(vehicle.motion.x, 0, vehicle.motion.z);
        vehicle.motion.x = clampAxis(vehicle, freeBoxes, new Point3D(1, 0, 0), vehicle.motion.x);
        vehicle.motion.z = clampAxis(vehicle, freeBoxes, new Point3D(0, 0, 1), vehicle.motion.z);
        return before.subtract(new Point3D(vehicle.motion.x, 0, vehicle.motion.z)).scale(vehicle.speedFactor);
    }

    private static double clampAxis(EntityVehicleF_Physics vehicle, List<BoundingBox> boxes, Point3D axis, double axisMotion) {
        for (int i = 0; i < CUT_TRIES && axisMotion != 0; ++i, axisMotion /= 2) {
            if (!collidesAny(vehicle, boxes, axis.copy().scale(axisMotion * vehicle.speedFactor))) {
                return axisMotion;
            }
        }
        return 0;
    }

    /**
     * Returns the largest of the tilt, half of it or a quarter of it that doesn't swing a box into a
     * block, or 0 if none fit.
     *
     * @param pivot the point the vehicle turns about, in the vehicle's frame.
     * @param tilt the pitch to apply, in degrees.  Positive is nose down.
     */
    public static double limitTilt(EntityVehicleF_Physics vehicle, List<BoundingBox> freeBoxes, Point3D pivot, double tilt) {
        for (int i = 0; i < CUT_TRIES && tilt != 0; ++i, tilt /= 2) {
            if (!tiltCollides(vehicle, freeBoxes, pivot, tilt)) {
                return tilt;
            }
        }
        return 0;
    }

    private static boolean tiltCollides(EntityVehicleF_Physics vehicle, List<BoundingBox> boxes, Point3D pivot, double tilt) {
        RotationMatrix turn = new RotationMatrix().setToAxisAngle(1, 0, 0, tilt);
        for (BoundingBox box : boxes) {
            //Turn the box centre about the pivot in the vehicle's frame, then take it back to the world.
            Point3D turned = box.globalCenter.copy().subtract(vehicle.position).reOrigin(vehicle.orientation).subtract(pivot).rotate(turn).add(pivot);
            Point3D offset = turned.rotate(vehicle.orientation).add(vehicle.position).subtract(box.globalCenter);
            if (collides(vehicle, box, offset)) {
                return true;
            }
        }
        return false;
    }

    /**
     * If any box is inside a block, returns one push step towards the nearest position where every
     * box is free.  Returns null if nothing is stuck, or if there's no free spot within reach.
     */
    public static Point3D findPushOut(EntityVehicleF_Physics vehicle, List<BoundingBox> boxes) {
        if (getFreeBoxes(vehicle, boxes).size() == boxes.size()) {
            return null;
        }
        for (int step = 1; step <= PUSH_MAX_STEPS; ++step) {
            for (Point3D direction : PUSH_DIRECTIONS) {
                if (!collidesAny(vehicle, boxes, direction.copy().scale(step * PUSH_STEP))) {
                    return direction.copy().scale(PUSH_STEP);
                }
            }
        }
        return null;
    }

    private static boolean collidesAny(EntityVehicleF_Physics vehicle, List<BoundingBox> boxes, Point3D offset) {
        for (BoundingBox box : boxes) {
            if (collides(vehicle, box, offset)) {
                return true;
            }
        }
        return false;
    }

    private static boolean collides(EntityVehicleF_Physics vehicle, BoundingBox box, Point3D offset) {
        //Clear the air cache each time, as it lives across ticks and blocks may have been placed since.
        return vehicle.world.checkForCollisions(box, offset, true, false);
    }
}
