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
 * A route state is the vehicle's origin standing at the center of a block column, facing one of
 * {@link #HEADINGS} headings.  The vehicle fits there if every block column its shape covers at that
 * heading has room for the part of the vehicle over it, and its wheels have ground under them.  Long
 * vehicles therefore need room for their whole length in the direction they're going.
 * <p>
 * From each state the vehicle can drive straight on, or turn one heading either way along an arc no
 * tighter than it can actually steer, checking it fits at every block along the way.  So routes start
 * from the way the vehicle is facing and never ask it to turn tighter than it can.
 * <p>
 * Moving between blocks can climb 1 block or drop 2.  Results are cached, as neighbouring states
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
    /**How many headings a vehicle may face, evenly spaced round a full turn.  Heading n faces yaw n * 22.5.**/
    static final int HEADINGS = 16;
    /**
     * Headings to try placing the vehicle at, relative to the preferred one: as preferred, side-on
     * both ways, reversed, then the diagonals.
     */
    private static final int[] HEADING_ORDER = { 0, 4, -4, 8, 2, -2, 6, -6 };
    /**
     * The block step for driving straight on at each heading.  Headings between the 8 compass
     * directions step like a knight in chess, which is within 4 degrees of their yaw.
     */
    private static final int[][] HEADING_STEPS = new int[HEADINGS][];
    static {
        int[][] compassSteps = { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 1, -1 }, { 0, -1 }, { -1, -1 }, { -1, 0 }, { -1, 1 } };
        for (int heading = 0; heading < HEADINGS; ++heading) {
            int[] step = compassSteps[heading / 2];
            if (heading % 2 == 1) {
                int[] nextStep = compassSteps[(heading / 2 + 1) % compassSteps.length];
                step = new int[] { step[0] + nextStep[0], step[1] + nextStep[1] };
            }
            HEADING_STEPS[heading] = step;
        }
    }
    /**Extra cost of a turn on top of its length, so routes don't weave when going straight is as short.**/
    private static final double TURN_COST = 0.3;
    /**How far apart the vehicle is checked along a move, in blocks.**/
    private static final double MOVE_CHECK_SPACING = 0.5;
    /**
     * How far off a block center a turn may end before we widen it to land closer, in blocks.  Routes
     * snap to block centers, so this keeps each turn as wide as planned to within this.
     */
    private static final double TURN_END_TOLERANCE = 0.2;
    /**
     * How much the distance left counts for against the distance travelled when choosing what to try
     * next.  Over 1 finds a route much faster, at the cost of it sometimes being a little longer than
     * the shortest.
     */
    private static final double HEURISTIC_WEIGHT = 1.5;

    final AWrapperWorld world;
    final VehicleShape shape;
    /**The vehicle's footprint and wheel columns at each heading, for its origin at the center of cell 0,0.  Worked out when first needed.**/
    @SuppressWarnings("unchecked")
    private final List<Column>[] footprints = new List[HEADINGS];
    @SuppressWarnings("unchecked")
    private final List<Column>[] supports = new List[HEADINGS];
    @SuppressWarnings("unchecked")
    private final Map<Long, Boolean>[] drivableCache = new Map[HEADINGS];
    private final Map<Long, Map<Integer, Boolean>> clearCache = new HashMap<>();
    private final Map<Long, Boolean> liquidCache = new HashMap<>();
    private final Map<Long, Boolean> solidCache = new HashMap<>();
    /**The moves the vehicle can make from each heading.**/
    private final List<List<Move>> moves = new ArrayList<>();

    /**
     * Makes a pathfinder for a vehicle of the shape, which turns on a circle of turnRadius blocks.
     * Pass {@link Double#POSITIVE_INFINITY} for a vehicle that can't turn, or one that won't be driven.
     */
    GroundPathfinder(AWrapperWorld world, VehicleShape shape, double turnRadius) {
        this.world = world;
        this.shape = shape;
        for (int heading = 0; heading < HEADINGS; ++heading) {
            drivableCache[heading] = new HashMap<>();
            moves.add(Move.getMoves(heading, turnRadius));
        }
    }

    /**The heading closest to the yaw.**/
    static int getHeading(double yaw) {
        return Math.floorMod((int) Math.round(yaw * HEADINGS / 360D), HEADINGS);
    }

    /**The yaw of the heading.**/
    static double getHeadingYaw(int heading) {
        return VehicleSummonFlight.clampAngle(heading * 360D / HEADINGS);
    }

    /**
     * Returns true if the vehicle can drive at this cell with its wheels at height y, facing the heading.
     * Parts of it may hang over terrain up to {@link #DRIVING_CLEARANCE} high, or over steps it can pitch onto.
     */
    boolean isDrivable(int x, int y, int z, int heading) {
        long key = blockKey(x, y, z);
        Boolean cached = drivableCache[heading].get(key);
        if (cached == null) {
            cached = checkDrivable(x, y, z, heading);
            drivableCache[heading].put(key, cached);
        }
        return cached;
    }

    /**
     * The checks for {@link #isDrivable}, cheapest and most likely to fail first: ground under the
     * middle, then each column the vehicle covers, then ground under each wheel.
     */
    private boolean checkDrivable(int x, int y, int z, int heading) {
        if (!isSolid(x, y - 1, z)) {
            return false;
        }
        for (Column column : getFootprint(heading)) {
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
        for (Column support : getSupports(heading)) {
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
     * Returns true if the vehicle can be placed with its origin at the center of the cell, its wheels on
     * the ground at height y, and facing the heading.  Unlike driving, everything it covers must be clear
     * and every wheel must be on level ground, as it's being put there rather than driving over bumps.
     */
    boolean canParkAt(int x, int y, int z, int heading) {
        if (!isSolid(x, y - 1, z)) {
            return false;
        }
        for (Column column : getFootprint(heading)) {
            int columnX = x + column.x;
            int columnZ = z + column.z;
            if (isLiquid(columnX, y, columnZ) || !isClear(columnX, y, columnZ, Math.max(column.low, PARKING_CLEARANCE), column.high)) {
                return false;
            }
        }
        for (Column support : getSupports(heading)) {
            if (!isSolid(x + support.x, y - 1, z + support.z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if an aircraft facing the heading can come straight down from its origin being at fromY
     * to land with its origin over the center of the cell and its wheels on the ground at height y.  Every
     * column it covers must be clear all the way down, and each wheel must come down within a block of
     * that ground, so it doesn't land half off a ledge.
     */
    boolean canLandAt(int x, int y, int z, int heading, double fromY) {
        double descent = fromY + shape.bottom - y;
        for (Column column : getFootprint(heading)) {
            int columnX = x + column.x;
            int columnZ = z + column.z;
            if (isLiquid(columnX, y, columnZ) || !isClear(columnX, y, columnZ, Math.max(column.low, PARKING_CLEARANCE), descent + column.high)) {
                return false;
            }
        }
        for (Column support : getSupports(heading)) {
            if (!isSolid(x + support.x, y - 1, z + support.z) && !isSolid(x + support.x, y - 2, z + support.z)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds where the vehicle can be placed in the column at x,z.  Heights between refY + up and
     * refY - down are tried nearest refY first, and at each height the headings are tried in
     * {@link #HEADING_ORDER} from the preferred one.  Returns a placement for each height it fits at,
     * nearest first, with the first heading that fits there.  If firstOnly is set, stops at the first.
     */
    List<Placement> findPlacements(int x, int z, int refY, int up, int down, int preferredHeading, boolean firstOnly) {
        List<Placement> placements = new ArrayList<>();
        for (int offset = 0; offset <= Math.max(up, down); ++offset) {
            for (int y : offset == 0 ? new int[] { refY } : new int[] { refY + offset, refY - offset }) {
                if ((y > refY && y - refY > up) || (y < refY && refY - y > down)) {
                    continue;
                }
                //Nothing can stand here at any heading without ground under its middle.
                if (!isSolid(x, y - 1, z)) {
                    continue;
                }
                for (int relativeHeading : HEADING_ORDER) {
                    int heading = Math.floorMod(preferredHeading + relativeHeading, HEADINGS);
                    if (canParkAt(x, y, z, heading)) {
                        placements.add(new Placement(x, y, z, heading));
                        if (firstOnly) {
                            return placements;
                        }
                        break;
                    }
                }
            }
        }
        return placements;
    }

    /**
     * Returns the first heading in {@link #HEADING_ORDER} from the preferred one that an aircraft can
     * land at here (see {@link #canLandAt}), or -1 if there isn't one.
     */
    int findLandingHeading(int x, int y, int z, int preferredHeading, double fromY) {
        for (int relativeHeading : HEADING_ORDER) {
            int heading = Math.floorMod(preferredHeading + relativeHeading, HEADINGS);
            if (canLandAt(x, y, z, heading, fromY)) {
                return heading;
            }
        }
        return -1;
    }

    private List<Column> getFootprint(int heading) {
        if (footprints[heading] == null) {
            footprints[heading] = new ArrayList<>(shape.getFootprint(0.5, 0.5, getHeadingYaw(heading)).values());
            supports[heading] = shape.getSupports(0.5, 0.5, getHeadingYaw(heading));
        }
        return footprints[heading];
    }

    private List<Column> getSupports(int heading) {
        getFootprint(heading);
        return supports[heading];
    }

    /**
     * Finds the drivable height in this column between refY + up and refY - down, facing the heading.
     * Searches top down, so we prefer driving over things.  Returns {@link #NO_SURFACE} if there isn't one.
     */
    int findSurface(int x, int z, int refY, int up, int down, int heading) {
        for (int y = refY + up; y >= refY - down; --y) {
            if (isDrivable(x, y, z, heading)) {
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

    private static long stateKey(int x, int z, int heading) {
        return ((long) (x & 0x3FFFFFF) << 30) | ((long) (z & 0x3FFFFFF) << 4) | heading;
    }

    /**
     * One move the vehicle can make from a state: straight on, or a turn of one heading along an
     * arc.  Offsets are from the starting block.
     */
    private static final class Move {
        /**The blocks the vehicle's origin passes through, in order, as {x offset, z offset, heading}.  The last is where it ends up.**/
        final int[][] steps;
        final double cost;

        private Move(int[][] steps, double cost) {
            this.steps = steps;
            this.cost = cost;
        }

        int[] getEnd() {
            return steps[steps.length - 1];
        }

        /**
         * Returns the moves from the heading: straight on, then turning each way if the vehicle can.
         * <p>
         * A turn follows an arc from the center of the starting block, facing the old heading for the
         * first half and the new one for the second.  Routes snap to block centers, so the arc is
         * widened from turnRadius until it ends within {@link #TURN_END_TOLERANCE} of one, so snapping
         * never makes a turn tighter than the vehicle can follow.  Vehicles that turn tighter than a
         * block still need a straight step to make each turn.
         */
        static List<Move> getMoves(int heading, double turnRadius) {
            List<Move> moves = new ArrayList<>();
            int[] step = HEADING_STEPS[heading];
            double stepLength = Math.hypot(step[0], step[1]);
            moves.add(new Move(sampleLine(step, heading), stepLength));
            if (Double.isInfinite(turnRadius)) {
                return moves;
            }
            double turnAngle = 2 * Math.PI / HEADINGS;
            double yaw = Math.toRadians(getHeadingYaw(heading));
            double forwardX = Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            for (int side = -1; side <= 1; side += 2) {
                int newHeading = Math.floorMod(heading + side, HEADINGS);
                //The way the arc bends: the forward direction turned 90 degrees towards the side.
                double sideX = side * forwardZ;
                double sideZ = -side * forwardX;

                //Widen the arc until it ends near a block center.
                double radius = turnRadius;
                for (double tryRadius = turnRadius; tryRadius <= turnRadius + 3; tryRadius += 0.05) {
                    double endX = 0.5 + tryRadius * (Math.sin(turnAngle) * forwardX + (1 - Math.cos(turnAngle)) * sideX);
                    double endZ = 0.5 + tryRadius * (Math.sin(turnAngle) * forwardZ + (1 - Math.cos(turnAngle)) * sideZ);
                    if (Math.hypot(endX - Math.floor(endX) - 0.5, endZ - Math.floor(endZ) - 0.5) <= TURN_END_TOLERANCE) {
                        radius = tryRadius;
                        break;
                    }
                }

                double arcLength = radius * turnAngle;
                int samples = Math.max(1, (int) Math.ceil(arcLength / MOVE_CHECK_SPACING));
                List<int[]> steps = new ArrayList<>();
                for (int i = 1; i <= samples; ++i) {
                    double angle = turnAngle * i / samples;
                    double along = radius * Math.sin(angle);
                    double across = radius * (1 - Math.cos(angle));
                    int x = (int) Math.floor(0.5 + along * forwardX + across * sideX);
                    int z = (int) Math.floor(0.5 + along * forwardZ + across * sideZ);
                    addStep(steps, x, z, angle < turnAngle / 2 ? heading : newHeading);
                }
                int[] end = steps.isEmpty() ? null : steps.get(steps.size() - 1);
                if (end == null || (end[0] == 0 && end[1] == 0)) {
                    //Turns tighter than a block: make the turn while stepping straight on in the new heading.
                    int[] newStep = HEADING_STEPS[newHeading];
                    moves.add(new Move(sampleLine(newStep, newHeading), Math.hypot(newStep[0], newStep[1]) + TURN_COST));
                } else {
                    //Make sure it ends facing the new way.
                    if (end[2] != newHeading) {
                        steps.add(new int[] { end[0], end[1], newHeading });
                    }
                    moves.add(new Move(steps.toArray(new int[0][]), Math.max(1, arcLength) + TURN_COST));
                }
            }
            return moves;
        }

        /**The blocks the origin passes through going straight from the center of block 0,0 by the step.**/
        private static int[][] sampleLine(int[] step, int heading) {
            double length = Math.hypot(step[0], step[1]);
            int samples = Math.max(1, (int) Math.ceil(length / MOVE_CHECK_SPACING));
            List<int[]> steps = new ArrayList<>();
            for (int i = 1; i <= samples; ++i) {
                double fraction = (double) i / samples;
                addStep(steps, (int) Math.floor(0.5 + step[0] * fraction), (int) Math.floor(0.5 + step[1] * fraction), heading);
            }
            //Land exactly on the step's block, whatever rounding did on the way.
            int[] last = steps.get(steps.size() - 1);
            if (last[0] != step[0] || last[1] != step[1]) {
                steps.add(new int[] { step[0], step[1], heading });
            }
            return steps.toArray(new int[0][]);
        }

        /**Adds the block to the steps if it's a new block or heading, skipping any samples before leaving the starting block.**/
        private static void addStep(List<int[]> steps, int x, int z, int heading) {
            int[] last = steps.isEmpty() ? null : steps.get(steps.size() - 1);
            if (last == null ? (x != 0 || z != 0) : (last[0] != x || last[1] != z || last[2] != heading)) {
                steps.add(new int[] { x, z, heading });
            }
        }
    }

    /**
     * A weighted A* search over states, that runs a bit at a time so a long search spreads over
     * several ticks.  It may start from several states at once, such as each way a vehicle could be
     * placed at a spot, and finds the route from whichever is best.
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

        /**Starts a search from each of the states, given as {x, y, z, heading}.**/
        Search(List<int[]> starts, Point3D goal, double goalRadius, int maxExpansions) {
            this.goal = goal;
            this.goalRadius = goalRadius;
            this.maxExpansions = maxExpansions;
            for (int[] start : starts) {
                Node node = new Node(start[0], start[1], start[2], start[3], null, null, 0);
                nodes.put(node.key(), node);
                open.add(node);
                if (best == null || node.h < best.h) {
                    best = node;
                }
            }
            finished = starts.isEmpty();
        }

        /**
         * Expands states until the search is over, budget states have been expanded, or maxNanos
         * have passed, whichever comes first.  Returns true once the search is over, found or not.
         */
        boolean step(int budget, long maxNanos) {
            long startTime = System.nanoTime();
            while (!finished && budget-- > 0 && System.nanoTime() - startTime < maxNanos) {
                Node current = open.poll();
                if (current == null || ++expansions > maxExpansions) {
                    finished = true;
                    break;
                }
                //Skip states we've closed, or found a cheaper way to since this was queued.
                if (current.closed || nodes.get(current.key()) != current) {
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
                for (Move move : moves.get(current.heading)) {
                    expand(current, move);
                }
            }
            return finished;
        }

        /**
         * Tries a move from the state.  The order is cheapest first: skip it if it ends somewhere
         * already finished with, then walk it block by block, stopping at the first block the vehicle
         * doesn't fit.  Each block's check is cached, so it's only done once whichever move gets there.
         */
        private void expand(Node current, Move move) {
            int[] end = move.getEnd();
            int endX = current.x + end[0];
            int endZ = current.z + end[1];
            Node existing = nodes.get(stateKey(endX, endZ, end[2]));
            if (existing != null && existing.closed) {
                return;
            }

            int[] stepHeights = new int[move.steps.length];
            int y = current.y;
            int lastX = current.x;
            int lastZ = current.z;
            for (int i = 0; i < move.steps.length; ++i) {
                int[] step = move.steps[i];
                int x = current.x + step[0];
                int z = current.z + step[1];
                int newY = findSurface(x, z, y, MAX_STEP_UP, MAX_STEP_DOWN, step[2]);
                if (newY == NO_SURFACE) {
                    return;
                }
                //Don't cut corners: going diagonally between blocks, the vehicle sweeps both blocks beside the corner.
                if (x != lastX && z != lastZ && (findSurface(x, lastZ, y, MAX_STEP_UP, MAX_STEP_DOWN, step[2]) == NO_SURFACE || findSurface(lastX, z, y, MAX_STEP_UP, MAX_STEP_DOWN, step[2]) == NO_SURFACE)) {
                    return;
                }
                y = newY;
                lastX = x;
                lastZ = z;
                stepHeights[i] = y;
            }

            double g = current.g + move.cost + Math.abs(y - current.y) * 0.5;
            if (existing == null || g < existing.g) {
                Node node = new Node(endX, y, endZ, end[2], current, move, g);
                node.stepHeights = stepHeights;
                nodes.put(node.key(), node);
                open.add(node);
            }
        }

        boolean succeeded() {
            return found != null;
        }

        /**The route to the goal, or null if there isn't one.**/
        List<Point3D> getPath() {
            return found != null ? toPath(found) : null;
        }

        /**The heading the route to the goal starts in.**/
        int getStartHeading() {
            return getRoot(found).heading;
        }

        /**The route to wherever we got closest to the goal.**/
        List<Point3D> getBestPath() {
            return toPath(best);
        }

        /**The heading the route to wherever we got closest starts in.**/
        int getBestStartHeading() {
            return getRoot(best).heading;
        }

        /**How close to the goal we got, in blocks.**/
        double getBestDistance() {
            return best.h;
        }

        /**How many states the search has expanded.**/
        int getExpansions() {
            return expansions;
        }

        private Node getRoot(Node end) {
            Node node = end;
            while (node.parent != null) {
                node = node.parent;
            }
            return node;
        }

        /**Every block the route passes through, in order, including the blocks along each turn.**/
        private List<Point3D> toPath(Node end) {
            List<Point3D> path = new ArrayList<>();
            for (Node node = end; node.parent != null; node = node.parent) {
                for (int i = node.move.steps.length - 1; i >= 0; --i) {
                    int[] step = node.move.steps[i];
                    Point3D point = new Point3D(node.parent.x + step[0] + 0.5, node.stepHeights[i], node.parent.z + step[1] + 0.5);
                    //Turns can change heading without changing block, so only add each block once.
                    if (path.isEmpty() || !path.get(path.size() - 1).equals(point)) {
                        path.add(point);
                    }
                }
            }
            Node root = getRoot(end);
            Point3D start = new Point3D(root.x + 0.5, root.y, root.z + 0.5);
            if (path.isEmpty() || !path.get(path.size() - 1).equals(start)) {
                path.add(start);
            }
            Collections.reverse(path);
            return path;
        }

        private final class Node {
            final int x, y, z;
            final int heading;
            final Node parent;
            /**The move from the parent that got here, and the ground height at each of its steps.**/
            final Move move;
            int[] stepHeights;
            final double g;
            final double h;
            final double f;
            boolean closed;

            Node(int x, int y, int z, int heading, Node parent, Move move, double g) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.heading = heading;
                this.parent = parent;
                this.move = move;
                this.g = g;
                this.h = Math.hypot(x + 0.5 - goal.x, z + 0.5 - goal.z);
                this.f = g + HEURISTIC_WEIGHT * h;
            }

            long key() {
                return stateKey(x, z, heading);
            }
        }
    }

    /**A spot the vehicle fits: its origin over the center of a cell, wheels on the ground at y, facing a heading.**/
    static final class Placement {
        final int x;
        final int y;
        final int z;
        final int heading;

        Placement(int x, int y, int z, int heading) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.heading = heading;
        }

        /**The ground point under the vehicle origin.**/
        Point3D getGround() {
            return new Point3D(x + 0.5, y, z + 0.5);
        }

        double getYaw() {
            return getHeadingYaw(heading);
        }
    }
}
