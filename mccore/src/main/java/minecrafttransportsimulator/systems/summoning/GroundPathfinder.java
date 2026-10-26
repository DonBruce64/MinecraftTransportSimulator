package minecrafttransportsimulator.systems.summoning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.systems.summoning.VehicleShape.Column;

/**
 * Block-grid pathfinding and space checks for ground vehicles.
 * <p>
 * Each grid cell is one block column, and a path node is the vehicle's origin standing at the
 * center of a cell, facing the way it moved into that cell.  The vehicle fits at a node if every
 * block column its shape covers at that heading has room for the part of the vehicle over it, and
 * its wheels have ground under them.  Long vehicles therefore need room for their whole length
 * in the direction they're going, and can't cut corners their nose or tail wouldn't clear.
 * <p>
 * Moving between cells can climb 1 block or drop 2.  Results are cached, as neighbouring nodes
 * share most of their columns.
 */
final class GroundPathfinder {
    static final int NO_SURFACE = Integer.MIN_VALUE;
    private static final int MAX_STEP_UP = 1;
    private static final int MAX_STEP_DOWN = 2;
    /**
     * Height above the ground that parts of the body may hang over terrain while driving.  This lets
     * the body pass over bumps and slabs that the wheels climb, and over blocks it pitches up onto.
     */
    private static final double DRIVING_CLEARANCE = 0.6;
    /**Height the vehicle is placed above the ground when teleported, so nothing lower needs to be clear.**/
    private static final double PARKING_CLEARANCE = 0.1;
    /**The 8 directions a path can move in, as x and z steps.  Each is a heading the vehicle may face.**/
    static final int[][] DIRECTIONS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

    final AWrapperWorld world;
    final VehicleShape shape;
    /**The vehicle's footprint and wheel columns at each direction, for the origin at the center of cell 0,0.**/
    private final List<List<Column>> footprints = new ArrayList<>();
    private final List<List<Column>> supports = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private final Map<Long, Boolean>[] drivableCache = new Map[DIRECTIONS.length];
    private final Map<Long, Map<Integer, Boolean>> clearCache = new HashMap<>();
    private final Map<Long, Boolean> liquidCache = new HashMap<>();
    private final Map<Long, Boolean> solidCache = new HashMap<>();

    GroundPathfinder(AWrapperWorld world, VehicleShape shape) {
        this.world = world;
        this.shape = shape;
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            double yaw = getYaw(i);
            footprints.add(new ArrayList<>(shape.getFootprint(0.5, 0.5, yaw).values()));
            supports.add(shape.getSupports(0.5, 0.5, yaw));
            drivableCache[i] = new HashMap<>();
        }
    }

    /**The yaw the vehicle faces when moving in the direction.**/
    static double getYaw(int direction) {
        return Math.toDegrees(Math.atan2(DIRECTIONS[direction][0], DIRECTIONS[direction][1]));
    }

    /**The direction closest to the yaw.**/
    static int getDirection(double yaw) {
        int closest = 0;
        double closestDelta = Double.MAX_VALUE;
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            double delta = Math.abs(VehicleSummonFlight.clampAngle(yaw - getYaw(i)));
            if (delta < closestDelta) {
                closest = i;
                closestDelta = delta;
            }
        }
        return closest;
    }

    /**
     * Returns true if the vehicle can drive at this cell with its wheels at height y, facing the direction.
     * Parts of it may hang over terrain up to {@link #DRIVING_CLEARANCE} high, or over steps it can pitch onto.
     */
    boolean isDrivable(int x, int y, int z, int direction) {
        long key = blockKey(x, y, z);
        Boolean cached = drivableCache[direction].get(key);
        if (cached == null) {
            cached = checkDrivable(x, y, z, direction);
            drivableCache[direction].put(key, cached);
        }
        return cached;
    }

    private boolean checkDrivable(int x, int y, int z, int direction) {
        if (!isSolid(x, y - 1, z)) {
            return false;
        }
        for (Column column : footprints.get(direction)) {
            int columnX = x + column.x;
            int columnZ = z + column.z;
            double low = Math.max(column.low, DRIVING_CLEARANCE);
            if (isLiquid(columnX, y, columnZ)) {
                return false;
            }
            //Clear at this height, or clear above a step the vehicle can pitch up onto.
            if (!isClear(columnX, y, columnZ, low, column.high) && !(isSolid(columnX, y, columnZ) && isClear(columnX, y + 1, columnZ, low, column.high))) {
                return false;
            }
        }
        for (Column support : supports.get(direction)) {
            int supportX = x + support.x;
            int supportZ = z + support.z;
            //Ground level, a step up, or a dip it can roll through.
            if (!isSolid(supportX, y - 1, supportZ) && !isSolid(supportX, y, supportZ) && !isSolid(supportX, y - 2, supportZ)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the vehicle can be placed with its origin at the point, its wheels on the
     * ground at height y, and facing the yaw.  Unlike driving, everything it covers must be clear and
     * every wheel must be on level ground, as it's being put there rather than driving over bumps.
     */
    boolean canParkAt(double originX, int y, double originZ, double yaw) {
        if (!isSolid((int) Math.floor(originX), y - 1, (int) Math.floor(originZ))) {
            return false;
        }
        for (Column column : shape.getFootprint(originX, originZ, yaw).values()) {
            if (isLiquid(column.x, y, column.z) || !isClear(column.x, y, column.z, Math.max(column.low, PARKING_CLEARANCE), column.high)) {
                return false;
            }
        }
        for (Column support : shape.getSupports(originX, originZ, yaw)) {
            if (!isSolid(support.x, y - 1, support.z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if an aircraft facing the yaw can come straight down from its origin being at fromY
     * to land with its origin over the point and its wheels on the ground at height y.  Every column it
     * covers must be clear all the way down, and each wheel must come down within a block of that ground,
     * so it doesn't land half off a ledge.
     */
    boolean canLandAt(double originX, int y, double originZ, double yaw, double fromY) {
        double descent = fromY + shape.bottom - y;
        for (Column column : shape.getFootprint(originX, originZ, yaw).values()) {
            if (isLiquid(column.x, y, column.z) || !isClear(column.x, y, column.z, Math.max(column.low, PARKING_CLEARANCE), descent + column.high)) {
                return false;
            }
        }
        for (Column support : shape.getSupports(originX, originZ, yaw)) {
            if (!isSolid(support.x, y - 1, support.z) && !isSolid(support.x, y - 2, support.z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the drivable height in this column between refY + up and refY - down, facing the direction.
     * Searches top down, so we prefer driving over things.  Returns {@link #NO_SURFACE} if there isn't one.
     */
    int findSurface(int x, int z, int refY, int up, int down, int direction) {
        for (int y = refY + up; y >= refY - down; --y) {
            if (isDrivable(x, y, z, direction)) {
                return y;
            }
        }
        return NO_SURFACE;
    }

    /**Like {@link #findSurface}, but for placing the vehicle with its origin at the center of the cell.**/
    int findParkingSurface(int x, int z, int refY, int up, int down, double yaw) {
        for (int y = refY + up; y >= refY - down; --y) {
            if (canParkAt(x + 0.5, y, z + 0.5, yaw)) {
                return y;
            }
        }
        return NO_SURFACE;
    }

    /**
     * Returns true if nothing in the block column at x,z collides between low and high above height y.
     * Heights are rounded outwards to 1/8 of a block so nearby checks share cached results.
     */
    boolean isClear(int x, int y, int z, double low, double high) {
        int lowEighths = (int) Math.floor(low * 8);
        int highEighths = (int) Math.ceil(high * 8);
        if (highEighths <= lowEighths) {
            return true;
        }
        Map<Integer, Boolean> results = clearCache.computeIfAbsent(blockKey(x, y, z), k -> new HashMap<>());
        int rangeKey = (lowEighths << 16) | (highEighths & 0xFFFF);
        Boolean clear = results.get(rangeKey);
        if (clear == null) {
            double bottomY = y + lowEighths / 8D;
            double topY = y + highEighths / 8D;
            BoundingBox box = new BoundingBox(new Point3D(x + 0.5, (bottomY + topY) / 2D, z + 0.5), 0.49, (topY - bottomY) / 2D, 0.49);
            clear = !world.checkForCollisions(box, new Point3D(), false, false);
            results.put(rangeKey, clear);
        }
        return clear;
    }

    /**Returns true if the block at the position has a solid top to stand on.**/
    boolean isSolid(int x, int y, int z) {
        return solidCache.computeIfAbsent(blockKey(x, y, z), k -> world.isBlockSolid(new Point3D(x + 0.5, y + 0.5, z + 0.5), Axis.UP));
    }

    private boolean isLiquid(int x, int y, int z) {
        return liquidCache.computeIfAbsent(blockKey(x, y, z), k -> world.isBlockLiquid(new Point3D(x + 0.5, y + 0.5, z + 0.5)));
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static long nodeKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * An A* search that runs a bit at a time, so a long search spreads over several ticks.
     */
    final class Search {
        private final PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        private final Map<Long, Node> nodes = new HashMap<>();
        private final Point3D goal;
        private final double goalRadius;
        private final int maxExpansions;
        private int expansions;
        private Node best;
        private Node found;
        private boolean finished;

        Search(int startX, int startY, int startZ, Point3D goal, double goalRadius, int maxExpansions) {
            this.goal = goal;
            this.goalRadius = goalRadius;
            this.maxExpansions = maxExpansions;
            Node start = new Node(startX, startY, startZ, null, 0);
            nodes.put(start.key(), start);
            open.add(start);
            best = start;
        }

        /**Expands up to budget nodes.  Returns true once the search is over, found or not.**/
        boolean step(int budget) {
            while (!finished && budget-- > 0) {
                Node current = open.poll();
                if (current == null || ++expansions > maxExpansions) {
                    finished = true;
                    break;
                }
                if (current.closed) {
                    continue;
                }
                current.closed = true;
                if (current.h < best.h) {
                    best = current;
                }
                if (current.h <= goalRadius) {
                    found = current;
                    finished = true;
                    break;
                }
                for (int direction = 0; direction < DIRECTIONS.length; ++direction) {
                    expand(current, direction);
                }
            }
            return finished;
        }

        private void expand(Node current, int direction) {
            int dx = DIRECTIONS[direction][0];
            int dz = DIRECTIONS[direction][1];
            int x = current.x + dx;
            int z = current.z + dz;
            int y = findSurface(x, z, current.y, MAX_STEP_UP, MAX_STEP_DOWN, direction);
            if (y == NO_SURFACE) {
                return;
            }
            boolean diagonal = dx != 0 && dz != 0;
            if (diagonal && (findSurface(current.x + dx, current.z, current.y, MAX_STEP_UP, MAX_STEP_DOWN, direction) == NO_SURFACE || findSurface(current.x, current.z + dz, current.y, MAX_STEP_UP, MAX_STEP_DOWN, direction) == NO_SURFACE)) {
                //Don't cut corners: the vehicle sweeps through both cells beside a diagonal step.
                return;
            }
            double g = current.g + (diagonal ? 1.4142 : 1.0) + Math.abs(y - current.y) * 0.5;
            long key = nodeKey(x, z);
            Node existing = nodes.get(key);
            if (existing == null || (!existing.closed && g < existing.g)) {
                Node node = new Node(x, y, z, current, g);
                nodes.put(key, node);
                open.add(node);
            }
        }

        boolean succeeded() {
            return found != null;
        }

        /**The path to the goal, or null if there isn't one.**/
        List<Point3D> getPath() {
            return found != null ? toPath(found) : null;
        }

        /**The path to wherever we got closest to the goal.**/
        List<Point3D> getBestPath() {
            return toPath(best);
        }

        /**How close to the goal we got, in blocks.**/
        double getBestDistance() {
            return best.h;
        }

        private List<Point3D> toPath(Node end) {
            List<Point3D> path = new ArrayList<>();
            for (Node node = end; node != null; node = node.parent) {
                path.add(new Point3D(node.x + 0.5, node.y, node.z + 0.5));
            }
            Collections.reverse(path);
            return path;
        }

        private final class Node {
            final int x, y, z;
            final Node parent;
            final double g;
            final double h;
            final double f;
            boolean closed;

            Node(int x, int y, int z, Node parent, double g) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.parent = parent;
                this.g = g;
                this.h = Math.hypot(x + 0.5 - goal.x, z + 0.5 - goal.z);
                this.f = g + h;
            }

            long key() {
                return nodeKey(x, z);
            }
        }
    }
}
