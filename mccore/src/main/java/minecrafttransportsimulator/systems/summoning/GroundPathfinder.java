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

/**
 * Block-grid pathfinding for ground vehicles.
 * <p>
 * Each grid cell is one block column.  A cell is drivable at height y if the block below has a
 * solid top, the cell isn't water or lava, and a box as wide and tall as the vehicle fits above it.
 * Moving between cells can climb 1 block or drop 2.  Results are cached, as neighbouring searches
 * test the same cells over and over.
 */
final class GroundPathfinder {
    static final int NO_SURFACE = Integer.MIN_VALUE;
    private static final int MAX_STEP_UP = 1;
    private static final int MAX_STEP_DOWN = 2;
    /**Clearance kept between the body and the ground, so bumps and slabs don't count as walls.**/
    private static final double GROUND_CLEARANCE = 0.6;
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    final AWrapperWorld world;
    final VehicleShape shape;
    private final Map<Long, Boolean> drivableCache = new HashMap<>();

    GroundPathfinder(AWrapperWorld world, VehicleShape shape) {
        this.world = world;
        this.shape = shape;
    }

    /**
     * Returns true if the vehicle can sit with its wheels at height y in this column.
     * Only checks a box as wide as the vehicle, not its full length, so it can turn through gaps.
     */
    boolean isDrivable(int x, int y, int z) {
        long key = ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        Boolean cached = drivableCache.get(key);
        if (cached == null) {
            cached = checkDrivable(x, y, z, shape.halfWidth);
            drivableCache.put(key, cached);
        }
        return cached;
    }

    /**
     * Returns true if the whole vehicle (a square as wide as it is long) can sit here.
     * Used for places it parks or teleports to, rather than places it drives through.
     */
    boolean fitsWhole(int x, int y, int z) {
        return checkDrivable(x, y, z, Math.max(shape.halfWidth, shape.halfLength));
    }

    private boolean checkDrivable(int x, int y, int z, double horizontalRadius) {
        Point3D ground = new Point3D(x + 0.5, y - 0.5, z + 0.5);
        if (!world.isBlockSolid(ground, Axis.UP)) {
            return false;
        }
        if (world.isBlockLiquid(new Point3D(x + 0.5, y + 0.5, z + 0.5))) {
            return false;
        }
        double bodyHeight = Math.max(0.5, shape.height() - GROUND_CLEARANCE);
        BoundingBox body = new BoundingBox(new Point3D(x + 0.5, y + GROUND_CLEARANCE + bodyHeight / 2D, z + 0.5), horizontalRadius, bodyHeight / 2D, horizontalRadius);
        return !world.checkForCollisions(body, new Point3D(), false, false);
    }

    /**
     * Finds the drivable height in this column between refY + up and refY - down, searching top
     * down so we prefer driving over things.  Returns {@link #NO_SURFACE} if there isn't one.
     */
    int findSurface(int x, int z, int refY, int up, int down) {
        for (int y = refY + up; y >= refY - down; --y) {
            if (isDrivable(x, y, z)) {
                return y;
            }
        }
        return NO_SURFACE;
    }

    /**Like {@link #findSurface}, but for parking the whole vehicle.**/
    int findWholeSurface(int x, int z, int refY, int up, int down) {
        for (int y = refY + up; y >= refY - down; --y) {
            if (fitsWhole(x, y, z)) {
                return y;
            }
        }
        return NO_SURFACE;
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
                for (int[] direction : DIRECTIONS) {
                    expand(current, direction[0], direction[1]);
                }
            }
            return finished;
        }

        private void expand(Node current, int dx, int dz) {
            int x = current.x + dx;
            int z = current.z + dz;
            int y = findSurface(x, z, current.y, MAX_STEP_UP, MAX_STEP_DOWN);
            if (y == NO_SURFACE) {
                return;
            }
            boolean diagonal = dx != 0 && dz != 0;
            if (diagonal && (findSurface(current.x + dx, current.z, current.y, MAX_STEP_UP, MAX_STEP_DOWN) == NO_SURFACE || findSurface(current.x, current.z + dz, current.y, MAX_STEP_UP, MAX_STEP_DOWN) == NO_SURFACE)) {
                //Don't cut corners.
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
