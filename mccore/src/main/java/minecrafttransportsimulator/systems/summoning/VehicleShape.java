package minecrafttransportsimulator.systems.summoning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;

/**
 * The space a vehicle takes up, in its own local space, taken from the boxes it actually
 * collides with blocks with and from its wheels.  Used to check the vehicle fits where we path,
 * teleport or land it.
 * <p>
 * Heights are measured up from the bottom of the wheels, which is where the ground is when the
 * vehicle is parked.  Like the vehicle's own collision boxes, each box stays square and
 * axis-aligned in the world as the vehicle turns: only its center rotates with the vehicle.
 */
final class VehicleShape {
    /**Boxes that overlap a block by less than this are not counted as being in it.**/
    private static final double CELL_OVERLAP_TOLERANCE = 0.15;

    /**Everything that needs room: the collision boxes and the wheels.**/
    final List<ShapeBox> boxes;
    /**Where the wheels touch the ground.  These need ground under them.**/
    final List<ShapeBox> supports;
    /**Lowest point below the vehicle origin (negative), usually the bottom of the wheels.**/
    final double bottom;
    /**Highest point above the vehicle origin.**/
    final double top;
    /**Furthest point forwards of the vehicle origin.**/
    final double front;
    /**Distance from the origin to the furthest point of the vehicle, so it fits in this circle at any yaw.**/
    private final double radius;

    private VehicleShape(List<ShapeBox> boxes, List<ShapeBox> supports, double bottom, double top, double front, double radius) {
        this.boxes = boxes;
        this.supports = supports;
        this.bottom = bottom;
        this.top = top;
        this.front = front;
        this.radius = radius;
    }

    /**Radius of a circle around the vehicle origin the vehicle fits in at any yaw.**/
    double radius() {
        return radius;
    }

    /**How far above the ground the vehicle origin sits when parked.**/
    double groundOffset() {
        return -bottom + 0.1;
    }

    /**
     * Returns the block columns the vehicle covers with its origin at the passed-in point and
     * facing the passed-in yaw, with the height range each column needs clear above the ground.
     * Columns are keyed by {@link #columnKey(int, int)}.
     */
    Map<Long, Column> getFootprint(double originX, double originZ, double yaw) {
        Map<Long, Column> footprint = new HashMap<>();
        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));
        for (ShapeBox box : boxes) {
            double centerX = originX + box.x * cos + box.z * sin;
            double centerZ = originZ - box.x * sin + box.z * cos;
            int minX = (int) Math.floor(centerX - box.radius + CELL_OVERLAP_TOLERANCE);
            int maxX = (int) Math.floor(centerX + box.radius - CELL_OVERLAP_TOLERANCE);
            int minZ = (int) Math.floor(centerZ - box.radius + CELL_OVERLAP_TOLERANCE);
            int maxZ = (int) Math.floor(centerZ + box.radius - CELL_OVERLAP_TOLERANCE);
            //Small boxes might not overlap any block by enough, but still need the block they're in.
            if (maxX < minX) {
                minX = maxX = (int) Math.floor(centerX);
            }
            if (maxZ < minZ) {
                minZ = maxZ = (int) Math.floor(centerZ);
            }
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    long key = columnKey(x, z);
                    Column column = footprint.get(key);
                    if (column == null) {
                        footprint.put(key, new Column(x, z, box.low, box.high));
                    } else {
                        column.low = Math.min(column.low, box.low);
                        column.high = Math.max(column.high, box.high);
                    }
                }
            }
        }
        return footprint;
    }

    /**
     * Returns the block columns under the wheels, with the vehicle origin at the passed-in point
     * and facing the passed-in yaw.
     */
    List<Column> getSupports(double originX, double originZ, double yaw) {
        List<Column> columns = new ArrayList<>();
        double sin = Math.sin(Math.toRadians(yaw));
        double cos = Math.cos(Math.toRadians(yaw));
        for (ShapeBox support : supports) {
            int x = (int) Math.floor(originX + support.x * cos + support.z * sin);
            int z = (int) Math.floor(originZ - support.x * sin + support.z * cos);
            columns.add(new Column(x, z, support.low, support.high));
        }
        return columns;
    }

    static long columnKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Takes the vehicle's shape as it is now.  The vehicle must have ticked at least once, so its
     * collision boxes are in place.
     */
    static VehicleShape of(EntityVehicleF_Physics vehicle) {
        List<double[]> rawBoxes = new ArrayList<>();
        List<double[]> rawWheels = new ArrayList<>();
        for (BoundingBox box : vehicle.allBlockCollisionBoxes) {
            rawBoxes.add(toLocal(vehicle, box.globalCenter, Math.max(box.widthRadius, box.depthRadius), box.heightRadius));
        }
        for (APart part : vehicle.allParts) {
            if (part instanceof PartGroundDevice) {
                rawWheels.add(toLocal(vehicle, part.position, Math.max(part.getWidth(), part.getHeight()) / 2D, part.getHeight() / 2D));
            }
        }
        if (rawBoxes.isEmpty() && rawWheels.isEmpty()) {
            //Nothing to go on.  Treat it as a block-sized box.
            rawBoxes.add(new double[] { 0, 0, 0, 0.5, 0.5 });
        }

        //The ground is where the wheels touch it, or failing that, the bottom of the lowest box.
        double bottom = Double.MAX_VALUE;
        for (double[] wheel : rawWheels) {
            bottom = Math.min(bottom, wheel[1] - wheel[4]);
        }
        if (rawWheels.isEmpty()) {
            for (double[] box : rawBoxes) {
                bottom = Math.min(bottom, box[1] - box[4]);
            }
        }

        List<ShapeBox> boxes = new ArrayList<>();
        double top = bottom + 1;
        double front = 0;
        double radius = 0;
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] raw : rawBoxes) {
            boxes.add(new ShapeBox(raw, bottom));
        }
        for (double[] raw : rawWheels) {
            boxes.add(new ShapeBox(raw, bottom));
        }
        for (ShapeBox box : boxes) {
            top = Math.max(top, bottom + box.high);
            front = Math.max(front, box.z + box.radius);
            //Corner furthest from the origin.
            radius = Math.max(radius, Math.hypot(Math.abs(box.x) + box.radius, Math.abs(box.z) + box.radius));
            minX = Math.min(minX, box.x - box.radius);
            maxX = Math.max(maxX, box.x + box.radius);
            minZ = Math.min(minZ, box.z - box.radius);
            maxZ = Math.max(maxZ, box.z + box.radius);
        }

        //Without wheels, it sits on the corners of its underside.
        List<ShapeBox> supports = new ArrayList<>();
        if (!rawWheels.isEmpty()) {
            for (double[] raw : rawWheels) {
                supports.add(new ShapeBox(raw, bottom));
            }
        } else {
            double insetX = (maxX - minX) * 0.1;
            double insetZ = (maxZ - minZ) * 0.1;
            for (double x : new double[] { minX + insetX, maxX - insetX }) {
                for (double z : new double[] { minZ + insetZ, maxZ - insetZ }) {
                    supports.add(new ShapeBox(new double[] { x, bottom, z, 0, 0 }, bottom));
                }
            }
        }
        return new VehicleShape(boxes, supports, bottom, top, Math.max(0.5, front), radius);
    }

    /**Returns the box as {x, y, z, horizontal radius, vertical radius} in the vehicle's local space.**/
    private static double[] toLocal(EntityVehicleF_Physics vehicle, Point3D globalCenter, double horizontalRadius, double verticalRadius) {
        Point3D local = globalCenter.copy().subtract(vehicle.position).reOrigin(vehicle.orientation);
        return new double[] { local.x, local.y, local.z, horizontalRadius, verticalRadius };
    }

    /**A box of the vehicle: square in the horizontal, with heights measured up from the ground.**/
    static final class ShapeBox {
        final double x;
        final double z;
        final double radius;
        final double low;
        final double high;

        private ShapeBox(double[] raw, double ground) {
            this.x = raw[0];
            this.z = raw[2];
            this.radius = raw[3];
            this.low = raw[1] - raw[4] - ground;
            this.high = raw[1] + raw[4] - ground;
        }
    }

    /**A block column the vehicle covers, and the height range above the ground it needs clear there.**/
    static final class Column {
        final int x;
        final int z;
        double low;
        double high;

        private Column(int x, int z, double low, double high) {
            this.x = x;
            this.z = z;
            this.low = low;
            this.high = high;
        }
    }
}
