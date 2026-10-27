package minecrafttransportsimulator.systems.summoning;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONPart.EngineType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;
import minecrafttransportsimulator.systems.summoning.GroundPathfinder.Placement;

/**
 * One vehicle answering one phone call.  Runs on the server, one step per tick.
 * <p>
 * Ground vehicles: find (force-loading the chunk if needed), plan a route from a spot about
 * {@link VehiclePhone#VAR_SUMMON_DISTANCE} blocks away, teleport there and drive in with the
 * real controls.  If no route exists, teleport next to the player.  If there's no room by the
 * player, drive as close as possible.  If stuck, re-plan once, then teleport.
 * <p>
 * Aircraft that can hover (see {@link VehiclePhone#canFlyIn}): teleport in above the buildings,
 * fly over the player, switch to hover, and land straight down in the first clear spot within
 * {@link #LANDING_RADIUS} blocks, trying in front of the player first, then the sides, then behind.  If there's
 * nowhere to land, it hovers above the player until a player gets in.  Other aircraft are teleported straight onto
 * the ground near the player.
 */
final class VehicleSummonJob {
    private static final int DEFAULT_SUMMON_DISTANCE = 30;
    private static final int LOAD_TIMEOUT = 200;
    /**How often to renew the chunk loading for a vehicle we're waking up, in ticks.**/
    private static final int CHUNK_LOAD_INTERVAL = 20;
    /**
     * How far either side of straight ahead of the player a vehicle may arrive or be placed, in degrees.
     * Vehicles only come from in front, so players see them coming and searches stay small.
     */
    private static final double FRONT_LEEWAY = 45;
    /**Arrival directions are straight ahead, then this far round either side in turn, out to {@link #FRONT_LEEWAY}.**/
    private static final double ARRIVAL_ANGLE_STEP = 22.5;
    private static final int ARRIVAL_DIRECTIONS = 1 + 2 * (int) (FRONT_LEEWAY / ARRIVAL_ANGLE_STEP);
    /**How much nearer each arrival distance tried in a direction is, from the summon distance inwards, in blocks.**/
    private static final double ARRIVAL_DISTANCE_STEP = 6;
    /**Closest to the player a vehicle arrives before driving in, in blocks.**/
    private static final double MIN_ARRIVAL_DISTANCE = 12;
    /**How far above or below the player an arrival spot may be, in blocks.**/
    private static final int ARRIVAL_HEIGHT_RANGE = 8;
    /**How far above and below the player a spot next to them may be, in blocks.**/
    private static final int NEAR_HEIGHT_UP = 2;
    private static final int NEAR_HEIGHT_DOWN = 3;
    /**Most states a route search looks at.  States include the heading, so this is more than the blocks searched.**/
    private static final int SEARCH_EXPANSIONS = 20000;
    /**Most route states to look at each tick.  Each checks the vehicle's whole footprint, so keep this modest.**/
    private static final int SEARCH_BUDGET_PER_TICK = 500;
    private static final double FIT_SPOT_MIN = 3;
    private static final double FIT_SPOT_MAX = 8;
    private static final double FALLBACK_SPOT_MAX = 16;
    /**Cruise speed, in m/s (about 30 km/h).**/
    private static final double CRUISE_SPEED = 8.3;
    /**
     * Speed routes plan their turns for, in m/s.  Vehicles steer less the faster they go, so the
     * drive slows to this before turns to make them as tight as planned.
     */
    private static final double TURN_SPEED = 3;
    /**How much wider than the vehicle's tightest turn the route's turns are, to leave room for steering error.**/
    private static final double TURN_RADIUS_MARGIN = 1.25;
    /**How many route blocks ahead the drive looks for turns to slow down for.**/
    private static final int TURN_LOOKAHEAD = 10;
    private static final int STUCK_CHECK_INTERVAL = 100;
    /**
     * Longest a vehicle may take to drive in before it's brought straight to the player instead, in ticks.
     * Routes too long to drive in this time at {@link #TURN_SPEED} get longer: see {@link #getDriveTimeout()}.
     */
    private static final int MIN_DRIVE_TIMEOUT = 20 * 30;
    /**Extra time allowed on long routes for starting the engine and slowing down at the end, in seconds.**/
    private static final double DRIVE_TIMEOUT_MARGIN = 10;
    private static final double STOP_FRONT_DISTANCE = 3.5;
    private static final int LANDING_RADIUS = 20;
    /**
     * Longest an aircraft may spend flying in before it's put on the ground in front of the player instead, in ticks.
     * A normal flight takes 10 to 20 seconds, or around 40 when descending past tall buildings.
     */
    private static final int FLIGHT_TIMEOUT = 20 * 90;
    private static final double CRUISE_CLEARANCE = 10;
    private static final double FLY_SPEED = 1.0;
    private static final double DESCEND_SPEED = 0.2;
    /**How far below the ground the descent aims, so it reaches the ground at a steady rate.**/
    private static final double LANDING_OVERSHOOT = 1.0;
    /**
     * How high above the ground the origin waits while an aircraft finishes turning to its landing heading,
     * in blocks, so it never touches down crooked.
     */
    private static final double LANDING_TURN_HOLD = 2;
    /**Ticks to keep pressing down after first touching the ground.**/
    private static final int SETTLE_TICKS = 10;

    private enum Phase {
        LOCATE,
        FIND_SPOT,
        PLAN_GROUND,
        REPLAN,
        TELEPORT_SPAWN,
        DRIVE,
        CLIMB,
        FLY_TO_PLAYER,
        FIND_LANDING,
        FLY_TO_LANDING,
        DESCEND
    }

    /**What a spot search near the player is for, which says what to do with what it finds.**/
    private enum SpotPurpose {
        /**Where a ground vehicle drives to.**/
        GOAL,
        /**Where a ground vehicle is teleported when it can't drive in.**/
        FALLBACK,
        /**Where an aircraft that can't fly in is teleported.**/
        AIRCRAFT,
        /**Where an aircraft that took too long flying in is teleported.**/
        FLIGHT_TIMEOUT
    }

    final UUID vehicleID;
    final AWrapperWorld world;
    private final UUID playerID;
    private final String name;
    private Phase phase = Phase.LOCATE;
    private int phaseTicks;
    /**How long this tick's searches may take, in nanoseconds, as shared out by {@link VehicleSummonManager}.**/
    private long tickTime;
    private boolean cancelled;

    private EntityVehicleF_Physics vehicle;
    private VehicleShape shape;
    private Point3D forcedChunk;
    /**True when the vehicle was already loaded and in driving range, so it must never be teleported.**/
    private boolean driveOnly;

    //Teleporting is remove now, spawn next tick, so the old entity is fully gone first.
    private IWrapperNBT teleportData;
    private ItemVehicle teleportItem;
    private Phase afterTeleport;
    private LanguageEntry afterTeleportMessage;

    //Ground planning.
    private GroundPathfinder pathfinder;
    /**Where the player was, and the way they were looking, when planning started.  Spots are ranked from in front of them.**/
    private Point3D planCenter;
    private double frontAngle;
    private Placement fitSpot;
    private SpotSearch spotSearch;
    private SpotPurpose spotPurpose;
    private Point3D goal;
    private double goalRadius;
    /**Arrival spots found but not yet tried, and the next direction round from in front of the player to look in.**/
    private final Deque<Placement> pendingArrivals = new ArrayDeque<>();
    private int arrivalDirection;
    private boolean triedDrivingFromHere;
    private Point3D candidate;
    private boolean candidateNeedsTeleport;
    /**The heading the candidate was checked to fit at, if it's a teleport spot.**/
    private int candidateHeading;
    private GroundPathfinder.Search search;
    private Point3D bestCandidate;
    private boolean bestCandidateNeedsTeleport;
    private int bestCandidateHeading;
    private List<Point3D> bestCandidatePath;
    private double bestCandidateDistance = Double.MAX_VALUE;

    //Driving.
    private List<Point3D> path;
    private int pathIndex;
    private Point3D lastProgressPosition;
    private int replans;
    /**How long the current route may take to drive, in ticks.**/
    private int driveTimeout;
    private boolean drivingToClosest;

    //Flying.
    private double cruiseY;
    private final List<Point3D> landingCandidates = new ArrayList<>();
    private int landingIndex;
    private Point3D landingSpot;
    private int landingHeading;
    /**True if the aircraft can turn to its landing heading as it comes down, rather than having to finish turning first.**/
    private boolean landingTurnsOnWayDown;
    /**True while the descent is held just above the ground for the aircraft to finish turning.**/
    private boolean descentHeld;
    private double lastDescentHeight = Double.MAX_VALUE;
    /**How long the summon flight has been flying the aircraft in, in ticks, and whether it's run out of time.**/
    private int flightTicks;
    private boolean flightTimedOut;
    private int settleTicks;

    VehicleSummonJob(AWrapperWorld world, IWrapperPlayer player, UUID vehicleID, String name) {
        this.world = world;
        this.playerID = player.getID();
        this.vehicleID = vehicleID;
        this.name = name;
    }

    /**Stops the call.  Whatever is underway is left safe: stopped on the ground, or hovering.**/
    void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        if (phase == Phase.TELEPORT_SPAWN) {
            //Mid-teleport: the vehicle only exists as saved data right now, so put it back first.
            finishTeleport(null);
            return;
        }
        stopVehicle();
        releaseChunk();
    }

    /**
     * Runs one tick, spending at most about tickTime nanoseconds searching.  Driving and flying still
     * happen if it's 0.  Returns true when the job is finished and can be dropped.
     */
    boolean tick(long tickTime) {
        this.tickTime = tickTime;
        if (cancelled) {
            return true;
        }
        IWrapperPlayer player = (IWrapperPlayer) world.getExternalEntity(playerID);
        if (player == null) {
            //Player logged off or changed dimension.
            cancel();
            return true;
        }
        if (forcedChunk != null && world.getTime() % CHUNK_LOAD_INTERVAL == 0) {
            world.keepChunkLoaded(forcedChunk);
        }
        if (phase != Phase.LOCATE && phase != Phase.TELEPORT_SPAWN && (vehicle == null || !vehicle.isValid)) {
            message(player, LanguageSystem.INTERACT_PHONE_LOSTCONTACT);
            releaseChunk();
            return true;
        }
        ++phaseTicks;
        if (isFlying()) {
            if (VehiclePhone.hasPlayerRider(vehicle)) {
                //A player has got in mid-flight.  Hand it over to them, set up as it normally is.
                VehiclePhone.releaseHold(vehicle);
                stopControls();
                return true;
            }
            if (!flightTimedOut && ++flightTicks > FLIGHT_TIMEOUT) {
                //Taking too long, likely blocked.  Hold where it is while we find somewhere on the ground for it.
                flightTimedOut = true;
                VehicleSummonFlight.flyTo(vehicle, vehicle.position.copy(), FLY_SPEED);
                frontAngle = getLookAngle(player);
                double minDistance = shape.radius() + 1.5;
                startSpotSearch(player.getPosition(), minDistance, minDistance + FALLBACK_SPOT_MAX, SpotPurpose.FLIGHT_TIMEOUT);
                return false;
            }
        }
        switch (phase) {
            case LOCATE:
                return locate(player);
            case FIND_SPOT:
                return findSpot(player);
            case PLAN_GROUND:
                return planGround(player);
            case REPLAN:
                return replan(player);
            case TELEPORT_SPAWN:
                return finishTeleport(player);
            case DRIVE:
                return drive(player);
            case CLIMB:
            case FLY_TO_PLAYER:
            case FLY_TO_LANDING:
            case DESCEND:
                return fly(player);
            case FIND_LANDING:
                return findLanding(player);
        }
        return true;
    }

    /**True while the summon flight is flying the aircraft in, or holding it while we find it somewhere to go after timing out.**/
    private boolean isFlying() {
        return phase == Phase.CLIMB || phase == Phase.FLY_TO_PLAYER || phase == Phase.FIND_LANDING || phase == Phase.FLY_TO_LANDING || phase == Phase.DESCEND || (phase == Phase.FIND_SPOT && spotPurpose == SpotPurpose.FLIGHT_TIMEOUT);
    }

    private void setPhase(Phase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }

    //-----FINDING THE VEHICLE-----

    private boolean locate(IWrapperPlayer player) {
        vehicle = getLoadedVehicle();
        if (vehicle == null) {
            if (forcedChunk == null) {
                Point3D lastPosition = VehicleLocationTracker.getLastPosition(world, vehicleID);
                if (lastPosition == null) {
                    message(player, LanguageSystem.INTERACT_PHONE_NOTFOUND);
                    return true;
                }
                forcedChunk = lastPosition;
                world.keepChunkLoaded(forcedChunk);
                message(player, LanguageSystem.INTERACT_PHONE_CALLING);
            } else if (phaseTicks > LOAD_TIMEOUT) {
                //It's not where it was last seen, so it's been picked up or destroyed.  Stop looking for it there.
                message(player, LanguageSystem.INTERACT_PHONE_NOANSWER);
                VehicleLocationTracker.forget(world, vehicleID);
                releaseChunk();
                return true;
            }
            return false;
        }

        //Found it.  If it's only just loaded, wait for it to tick so its collision boxes are in place.
        if (vehicle.ticksExisted < 2) {
            if (phaseTicks > LOAD_TIMEOUT) {
                message(player, LanguageSystem.INTERACT_PHONE_NOANSWER);
                releaseChunk();
                return true;
            }
            return false;
        }

        //Check the caller may still call it.  Its owner may have given it a key since it was registered on this phone.
        if (!VehiclePhone.canControl(player, vehicle)) {
            message(player, LanguageSystem.INTERACT_PHONE_NEEDKEYCALL);
            releaseChunk();
            return true;
        }

        //Check it can come.
        for (APart part : vehicle.allParts) {
            if (part.rider != null) {
                message(player, LanguageSystem.INTERACT_PHONE_INUSE);
                releaseChunk();
                return true;
            }
        }
        //It has to be able to get here under its own power: not broken, with a working engine, and fuel if that needs it.
        boolean isAircraft = vehicle.definition.motorized.isAircraft;
        boolean flyIn = isAircraft && VehiclePhone.canFlyIn(vehicle);
        LanguageEntry notReady = null;
        if (vehicle.outOfHealth) {
            notReady = LanguageSystem.INTERACT_PHONE_WRECKED;
        } else if (getWorkingEngines(isAircraft).isEmpty()) {
            notReady = LanguageSystem.INTERACT_PHONE_NOENGINE;
        } else if (needsFuel(isAircraft) && vehicle.fuelTank.getFluidLevel() == 0) {
            notReady = LanguageSystem.INTERACT_PHONE_NOFUEL;
        }
        if (notReady != null) {
            message(player, notReady);
            releaseChunk();
            return true;
        }
        shape = VehicleShape.of(vehicle);
        planCenter = player.getPosition().copy();
        frontAngle = getLookAngle(player);
        //Already loaded and within range: it makes its own way over, no teleporting.
        driveOnly = forcedChunk == null && !isFar(player);
        if (flyIn) {
            if (isIndoors(player)) {
                message(player, LanguageSystem.INTERACT_PHONE_INDOORS);
                releaseChunk();
                return true;
            }
            return startFlight(player);
        }
        if (isAircraft) {
            return teleportAircraftToGround(player);
        }
        startGroundPlanning(player);
        return false;
    }

    private EntityVehicleF_Physics getLoadedVehicle() {
        Object entity = world.getEntity(vehicleID);
        return entity instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) entity : null;
    }

    /**Stops keeping the vehicle's old chunk loaded.  It unloads on its own shortly after.**/
    private void releaseChunk() {
        forcedChunk = null;
    }

    private double getSummonDistance() {
        double distance = vehicle.getOrCreateVariable(VehiclePhone.VAR_SUMMON_DISTANCE).currentValue;
        return distance > 0 ? distance : DEFAULT_SUMMON_DISTANCE;
    }

    private boolean isFar(IWrapperPlayer player) {
        Point3D playerPosition = player.getPosition();
        return Math.hypot(vehicle.position.x - playerPosition.x, vehicle.position.z - playerPosition.z) > getSummonDistance() + 8 || Math.abs(vehicle.position.y - playerPosition.y) > 30;
    }

    //-----GROUND PLANNING-----

    private void startGroundPlanning(IWrapperPlayer player) {
        pathfinder = new GroundPathfinder(world, shape, getTurnRadius());

        //Already within driving range: drive from where it is, never teleport.
        //Otherwise try arrival spots in front of the player, so they see it coming.  These are found as we go.
        pendingArrivals.clear();
        arrivalDirection = 0;
        triedDrivingFromHere = false;
        search = null;

        //First find somewhere in front of the player the whole vehicle fits, to drive to.
        startSpotSearch(planCenter, FIT_SPOT_MIN, FIT_SPOT_MAX, SpotPurpose.GOAL);
    }

    private void startSpotSearch(Point3D center, double min, double max, SpotPurpose purpose) {
        spotSearch = new SpotSearch(center, min, max);
        spotPurpose = purpose;
        setPhase(Phase.FIND_SPOT);
    }

    /**Carries on the spot search, and acts on its result when it's done.**/
    private boolean findSpot(IWrapperPlayer player) {
        if (!spotSearch.step(tickTime)) {
            return false;
        }
        Placement spot = spotSearch.found;
        spotSearch = null;
        switch (spotPurpose) {
            case GOAL: {
                fitSpot = spot;
                goal = fitSpot != null ? fitSpot.getGround() : planCenter;
                goalRadius = fitSpot != null ? 1.5 : 3;
                setPhase(Phase.PLAN_GROUND);
                return false;
            }
            case FALLBACK: {
                if (spot != null) {
                    teleportToGround(spot.getGround(), spot.getYaw(), null, LanguageSystem.INTERACT_PHONE_PARKEDCLOSEST);
                    return false;
                }
                break;
            }
            case AIRCRAFT: {
                if (spot != null) {
                    teleportToGround(spot.getGround(), spot.getYaw(), null, LanguageSystem.INTERACT_PHONE_BROUGHT);
                    return false;
                }
                break;
            }
            case FLIGHT_TIMEOUT: {
                //Stop the flight before saving the aircraft to move it, or it'd fly back to its old target once placed.
                VehicleSummonFlight.stop(vehicle);
                if (spot != null) {
                    //It'll be on the ground, so gear down and set up as it normally is.
                    set(vehicle.getOrCreateVariable(VehiclePhone.VAR_GEAR), 0);
                    VehiclePhone.restoreHoverDefaults(vehicle);
                    teleportToGround(spot.getGround(), spot.getYaw(), null, LanguageSystem.INTERACT_PHONE_BROUGHT);
                    return false;
                }
                //Nowhere on the ground either.  Stay up where it is until a player gets in.
                VehiclePhone.holdInAir(vehicle);
                stopControls();
                message(player, LanguageSystem.INTERACT_PHONE_HOVERINGSTUCK);
                return true;
            }
        }
        //Nothing fits in front of the player.
        message(player, LanguageSystem.INTERACT_PHONE_NOROOM);
        releaseChunk();
        return true;
    }

    /**
     * Returns the radius of the tightest circle the vehicle can drive round at {@link #TURN_SPEED},
     * plus {@link #TURN_RADIUS_MARGIN}, or infinity if it can't steer.
     * <p>
     * This follows the vehicle's own steering: at full lock it turns 45 / wheelbase * groundVelocity
     * * (speedFactor / 0.35) / 2 degrees a tick, scaled down above a ground velocity of 0.35 unless
     * it ignores speed, while moving groundVelocity * speedFactor blocks.  The speeds cancel out,
     * leaving a radius proportional to the wheelbase.
     */
    private double getTurnRadius() {
        double wheelbase = vehicle.definition.motorized.hasPermanentSkidSteer ? 1 : vehicle.groundDeviceCollective.getTurningWheelbase();
        if (wheelbase <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        double groundVelocity = TURN_SPEED / 20D / vehicle.speedFactor;
        double steeringFactor = vehicle.steeringForceFactorVar.currentValue;
        double steeringScale;
        if (vehicle.steeringForceIgnoresSpeedVar.isActive) {
            steeringScale = steeringFactor;
        } else {
            steeringScale = groundVelocity > 0.35 ? Math.pow(0.3, groundVelocity * (1 - steeringFactor) - 0.35) : 1;
        }
        if (steeringScale <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return TURN_RADIUS_MARGIN * 0.7 * Math.toDegrees(1) * wheelbase / (EntityVehicleF_Physics.MAX_RUDDER_ANGLE * steeringScale);
    }

    /**
     * Queues the spots the vehicle could arrive at in a direction in front of the player: 0 is straight
     * ahead, then alternately either side, out to {@link #FRONT_LEEWAY}.  It tries the summon distance,
     * then nearer, down to {@link #MIN_ARRIVAL_DISTANCE}, and queues every height it fits at the first
     * distance it fits at all, nearest the player's height first.
     */
    private void queueArrivals(int direction) {
        int step = (direction + 1) / 2;
        double angle = frontAngle + Math.toRadians((direction % 2 == 0 ? step : -step) * ARRIVAL_ANGLE_STEP);
        int playerY = (int) Math.floor(planCenter.y);
        double summonDistance = getSummonDistance();
        for (double distance = summonDistance; distance >= Math.min(summonDistance, MIN_ARRIVAL_DISTANCE); distance -= ARRIVAL_DISTANCE_STEP) {
            Point3D spot = new Point3D(planCenter.x + Math.sin(angle) * distance, playerY, planCenter.z + Math.cos(angle) * distance);
            //Arrive facing the player if it fits, so it sets off towards them.
            int heading = GroundPathfinder.getHeading(yawTowards(spot, planCenter));
            List<Placement> placements = pathfinder.findPlacements((int) Math.floor(spot.x), (int) Math.floor(spot.z), playerY, ARRIVAL_HEIGHT_RANGE, ARRIVAL_HEIGHT_RANGE, heading, false);
            if (!placements.isEmpty()) {
                pendingArrivals.addAll(placements);
                return;
            }
        }
    }

    /**
     * How far round from in front of the player an offset from them is: 0 ahead, 1 ahead to one side,
     * 2 to the side, 3 behind to one side, and 4 behind.
     */
    private int getSector(double dx, double dz) {
        return (int) Math.round(getAngleOffFront(dx, dz) / 45);
    }

    /**How far round from straight ahead of the player an offset from them is, in degrees from 0 to 180.**/
    private double getAngleOffFront(double dx, double dz) {
        return Math.abs(VehicleSummonFlight.clampAngle(Math.toDegrees(Math.atan2(dx, dz) - frontAngle)));
    }

    /**The horizontal direction the player is looking, as an angle for sin/cos (x, z).**/
    private static double getLookAngle(IWrapperPlayer player) {
        Point3D look = player.getLineOfSight(1).copy();
        return Math.atan2(look.x, look.z);
    }

    /**
     * A search for a ground spot between min and max blocks in front of the position, within
     * {@link #FRONT_LEEWAY} of where the player is looking, where the whole vehicle fits.  Straight
     * ahead comes before the sides, and nearest first in each.  At each spot, heights nearest the
     * position come first, and the vehicle faces the position if it fits that way, or else turns
     * until it does.  It runs a slice at a time so it can't stall the server.
     */
    private final class SpotSearch {
        private final Point3D center;
        private final int refY;
        private final List<int[]> offsets = new ArrayList<>();
        private int index;
        /**The spot found, or null if there isn't one or the search isn't done.**/
        Placement found;

        SpotSearch(Point3D center, double min, double max) {
            this.center = center.copy();
            this.refY = (int) Math.floor(center.y);
            int range = (int) Math.ceil(max);
            //Big aircraft search a wide area, so check fewer columns.
            int step = Math.max(1, range / 16);
            for (int dx = -range; dx <= range; dx += step) {
                for (int dz = -range; dz <= range; dz += step) {
                    double distance = Math.hypot(dx, dz);
                    if (distance >= min && distance <= max && getAngleOffFront(dx, dz) <= FRONT_LEEWAY + 0.01) {
                        offsets.add(new int[] { dx, dz, getSector(dx, dz) });
                    }
                }
            }
            offsets.sort((a, b) -> a[2] != b[2] ? Integer.compare(a[2], b[2]) : Double.compare(Math.hypot(a[0], a[1]), Math.hypot(b[0], b[1])));
        }

        /**Checks spots until one fits, they run out, or maxNanos pass.  Returns true once it's done.**/
        boolean step(long maxNanos) {
            long startTime = System.nanoTime();
            while (index < offsets.size() && System.nanoTime() - startTime < maxNanos) {
                int[] offset = offsets.get(index++);
                int x = (int) Math.floor(center.x) + offset[0];
                int z = (int) Math.floor(center.z) + offset[1];
                int heading = GroundPathfinder.getHeading(yawTowards(new Point3D(x + 0.5, refY, z + 0.5), center));
                List<Placement> placements = pathfinder.findPlacements(x, z, refY, NEAR_HEIGHT_UP, NEAR_HEIGHT_DOWN, heading, true);
                if (!placements.isEmpty()) {
                    found = placements.get(0);
                    return true;
                }
            }
            return index >= offsets.size();
        }
    }

    private boolean planGround(IWrapperPlayer player) {
        if (search == null) {
            int x, y, z;
            List<int[]> starts = new ArrayList<>();
            if (driveOnly) {
                //Drive from where it is now.
                if (triedDrivingFromHere) {
                    return groundFallback(player);
                }
                triedDrivingFromHere = true;
                x = (int) Math.floor(vehicle.position.x);
                z = (int) Math.floor(vehicle.position.z);
                y = pathfinder.findSurface(x, z, (int) Math.floor(vehicle.position.y + shape.bottom + 0.5), 1, 2, GroundPathfinder.getHeading(vehicle.orientation.angles.y));
                if (y == GroundPathfinder.NO_SURFACE) {
                    return groundFallback(player);
                }
                //It has to set off the way it's facing.
                starts.add(new int[] { x, y, z, GroundPathfinder.getHeading(vehicle.orientation.angles.y) });
                candidateNeedsTeleport = false;
            } else {
                if (pendingArrivals.isEmpty()) {
                    if (arrivalDirection >= ARRIVAL_DIRECTIONS) {
                        return groundFallback(player);
                    }
                    //One direction a tick, as each can try several distances, heights and headings.
                    if (tickTime > 0) {
                        queueArrivals(arrivalDirection++);
                    }
                    return false;
                }
                Placement next = pendingArrivals.poll();
                x = next.x;
                y = next.y;
                z = next.z;
                candidateHeading = next.heading;
                candidateNeedsTeleport = true;
                //It can be placed facing whichever way suits the route best, of the ways it fits.
                for (int heading = 0; heading < GroundPathfinder.HEADINGS; ++heading) {
                    if (pathfinder.canParkAt(x, y, z, heading)) {
                        starts.add(new int[] { x, y, z, heading });
                    }
                }
            }
            candidate = new Point3D(x + 0.5, y, z + 0.5);
            search = pathfinder.new Search(starts, goal, goalRadius, SEARCH_EXPANSIONS);
        }

        if (search.step(SEARCH_BUDGET_PER_TICK, tickTime)) {
            if (search.succeeded()) {
                List<Point3D> foundPath = search.getPath();
                int heading = search.getStartHeading();
                search = null;
                startRoute(player, candidate, candidateNeedsTeleport, heading, foundPath, false, LanguageSystem.INTERACT_PHONE_ONITSWAY);
                return false;
            }
            if (search.getBestDistance() < bestCandidateDistance) {
                bestCandidateDistance = search.getBestDistance();
                bestCandidate = candidate;
                bestCandidateNeedsTeleport = candidateNeedsTeleport;
                bestCandidateHeading = search.getBestStartHeading();
                bestCandidatePath = search.getBestPath();
            }
            search = null;
        }
        return false;
    }

    /**No route to the player from anywhere we tried.**/
    private boolean groundFallback(IWrapperPlayer player) {
        if (driveOnly) {
            //Close by, so no teleporting.  Get as near as the roads allow.
            if (bestCandidate != null && bestCandidatePath.size() > 1) {
                startRoute(player, bestCandidate, false, 0, bestCandidatePath, true, LanguageSystem.INTERACT_PHONE_NOWAYCLOSEST);
                return false;
            }
            park();
            message(player, LanguageSystem.INTERACT_PHONE_NOWAYFROMHERE);
            return true;
        }
        if (fitSpot != null) {
            //There's room by the player, we just can't drive there.  Bring it straight to them.
            teleportToGround(fitSpot.getGround(), fitSpot.getYaw(), null, LanguageSystem.INTERACT_PHONE_NOWAYTELEPORT);
            return false;
        }
        if (bestCandidate != null && bestCandidatePath.size() > 1) {
            //No room by the player.  Get as close as we can.
            startRoute(player, bestCandidate, bestCandidateNeedsTeleport, bestCandidateHeading, bestCandidatePath, true, LanguageSystem.INTERACT_PHONE_NOROOMCLOSEST);
            return false;
        }
        //Look a bit further out in front of the player for anywhere it fits.
        startSpotSearch(player.getPosition(), FIT_SPOT_MIN, FALLBACK_SPOT_MAX, SpotPurpose.FALLBACK);
        return false;
    }

    /**
     * Sets off along the route.  If teleporting to its start, the vehicle is placed facing the heading
     * the route was planned from.
     */
    private void startRoute(IWrapperPlayer player, Point3D start, boolean teleport, int heading, List<Point3D> route, boolean toClosest, LanguageEntry startMessage) {
        path = route;
        pathIndex = 0;
        drivingToClosest = toClosest;
        if (teleport) {
            teleportToGround(start, GroundPathfinder.getHeadingYaw(heading), Phase.DRIVE, startMessage);
        } else {
            message(player, startMessage);
            beginDriving();
        }
    }

    //-----TELEPORTING-----

    private void teleportToGround(Point3D groundSpot, double yaw, Phase next, LanguageEntry doneMessage) {
        teleport(new Point3D(groundSpot.x, groundSpot.y + shape.groundOffset(), groundSpot.z), yaw, next, doneMessage);
    }

    /**
     * Moves the vehicle by saving it, removing it, and spawning it again from that save at the
     * new spot next tick.  Clients only see vehicle movement through MTS's movement deltas, so
     * just changing the position on the server would leave them with the vehicle in the old place.
     * The vehicle keeps its UUID, so the phone still knows it.
     */
    private void teleport(Point3D position, double yaw, Phase next, LanguageEntry doneMessage) {
        vehicle.disconnectAllConnections();
        IWrapperNBT data = vehicle.save(InterfaceManager.coreInterface.getNewNBTWrapper());
        data.setPoint3d("position", position);
        data.setPoint3d("angles", new Point3D(0, yaw, 0));
        data.setPoint3d("motion", new Point3D());
        data.setPoint3d("serverDeltaM", new Point3D());
        data.setPoint3d("serverDeltaR", new Point3D());
        data.setDouble("serverDeltaP", 0);
        teleportData = data;
        teleportItem = (ItemVehicle) vehicle.cachedItem;
        afterTeleport = next;
        afterTeleportMessage = doneMessage;
        vehicle.remove();
        vehicle = null;
        setPhase(Phase.TELEPORT_SPAWN);
    }

    private boolean finishTeleport(IWrapperPlayer player) {
        vehicle = new EntityVehicleF_Physics(world, null, teleportItem, teleportData);
        vehicle.addPartsPostAddition(null, teleportData);
        world.spawnEntity(vehicle);
        VehicleLocationTracker.update(vehicle);
        releaseChunk();
        teleportData = null;
        if (cancelled) {
            stopVehicle();
            return true;
        }
        if (player != null && afterTeleportMessage != null) {
            message(player, afterTeleportMessage);
        }
        if (afterTeleport == null) {
            park();
            return true;
        }
        setPhase(afterTeleport);
        if (afterTeleport == Phase.DRIVE) {
            beginDriving();
        }
        return false;
    }

    //-----DRIVING-----

    private void beginDriving() {
        setPhase(Phase.DRIVE);
        startEngines(false);
        set(vehicle.parkingBrakeVar, 0);
        lastProgressPosition = vehicle.position.copy();
        driveTimeout = getDriveTimeout();
    }

    /**
     * How long the vehicle may take to drive the current route, in ticks: {@link #MIN_DRIVE_TIMEOUT}, or longer for
     * routes that need more time than that at {@link #TURN_SPEED}, the slowest it drives, plus {@link #DRIVE_TIMEOUT_MARGIN}.
     * So it's 30 seconds for any route up to 60 blocks, and a long winding route still gets time to finish.
     */
    private int getDriveTimeout() {
        double routeLength = 0;
        for (int i = 1; i < path.size(); ++i) {
            routeLength += horizontalDistance(path.get(i - 1), path.get(i));
        }
        return Math.max(MIN_DRIVE_TIMEOUT, (int) Math.ceil((routeLength / TURN_SPEED + DRIVE_TIMEOUT_MARGIN) * 20));
    }

    private boolean drive(IWrapperPlayer player) {
        for (APart part : vehicle.allParts) {
            if (part.rider != null) {
                //Someone got in.  Hand it over.
                stopControls();
                return true;
            }
        }
        //Put automatic gearboxes into drive once the engine's running.
        for (PartEngine engine : vehicle.engines) {
            if (engine.running && engine.currentGearVar.currentValue <= 0 && !isJet(engine)) {
                engine.shiftUp();
            }
        }

        Point3D position = vehicle.position;
        while (pathIndex < path.size() - 1 && horizontalDistance(path.get(pathIndex), position) < 2.5) {
            ++pathIndex;
        }
        Point3D end = path.get(path.size() - 1);
        double remaining = horizontalDistance(end, position);
        Point3D front = vehicle.headingVector.copy().scale(shape.front).add(position);
        if ((pathIndex >= path.size() - 1 && remaining < 1.5) || horizontalDistance(front, player.getPosition()) < STOP_FRONT_DISTANCE) {
            park();
            message(player, drivingToClosest ? LanguageSystem.INTERACT_PHONE_CLOSEST : LanguageSystem.INTERACT_PHONE_ARRIVED);
            return true;
        }

        //Steer at a point a few blocks down the path.
        Point3D lookahead = path.get(Math.min(pathIndex + 3, path.size() - 1));
        Point3D local = lookahead.copy().subtract(position).reOrigin(vehicle.orientation);
        double angle = Math.toDegrees(Math.atan2(local.x, local.z));
        //Positive rudder input steers right, and +X is left, so steer against the angle.
        set(vehicle.rudderInputVar, Math.max(-EntityVehicleF_Physics.MAX_RUDDER_ANGLE, Math.min(EntityVehicleF_Physics.MAX_RUDDER_ANGLE, -angle * 1.2)));

        //Slow for turns and for the end of the path.  Routes plan their turns for TURN_SPEED, so be down
        //to that by the time we reach one, or we'd steer wider than planned.
        double speed = vehicle.velocity * vehicle.speedFactor * 20;
        double targetSpeed = CRUISE_SPEED * Math.max(0.35, Math.min(1, 1 - Math.abs(angle) / 120));
        for (int i = pathIndex + 1; i <= Math.min(pathIndex + TURN_LOOKAHEAD, path.size() - 1); ++i) {
            Point3D ahead = path.get(i).copy().subtract(position).reOrigin(vehicle.orientation);
            if (Math.abs(Math.toDegrees(Math.atan2(ahead.x, ahead.z))) > 20) {
                targetSpeed = Math.min(targetSpeed, TURN_SPEED);
                break;
            }
        }
        if (remaining < 12) {
            targetSpeed = Math.min(targetSpeed, Math.max(1.5, remaining * 0.7));
        }
        if (speed < targetSpeed) {
            set(vehicle.throttleVar, Math.max(0, Math.min(0.8, 0.25 + (targetSpeed - speed) * 0.15)));
            set(vehicle.brakeVar, 0);
        } else if (speed > targetSpeed + 1.5) {
            set(vehicle.throttleVar, 0);
            set(vehicle.brakeVar, Math.min(1, (speed - targetSpeed) * 0.15));
        } else {
            set(vehicle.throttleVar, 0.15);
            set(vehicle.brakeVar, 0);
        }

        //Check we're getting somewhere.
        if (phaseTicks % STUCK_CHECK_INTERVAL == 0) {
            boolean stuck = lastProgressPosition.isDistanceToCloserThan(position, 1.5);
            lastProgressPosition = position.copy();
            if (stuck || phaseTicks > driveTimeout) {
                return onStuck(player);
            }
        }
        return false;
    }

    private boolean onStuck(IWrapperPlayer player) {
        stopControls();
        if (replans == 0 && phaseTicks <= driveTimeout) {
            ++replans;
            int x = (int) Math.floor(vehicle.position.x);
            int z = (int) Math.floor(vehicle.position.z);
            int y = pathfinder.findSurface(x, z, (int) Math.floor(vehicle.position.y + shape.bottom + 0.5), 1, 2, GroundPathfinder.getHeading(vehicle.orientation.angles.y));
            if (y != GroundPathfinder.NO_SURFACE) {
                List<int[]> starts = new ArrayList<>();
                starts.add(new int[] { x, y, z, GroundPathfinder.getHeading(vehicle.orientation.angles.y) });
                search = pathfinder.new Search(starts, goal, goalRadius, SEARCH_EXPANSIONS);
                setPhase(Phase.REPLAN);
                return false;
            }
        }
        return stuckFallback(player);
    }

    private boolean replan(IWrapperPlayer player) {
        if (search.step(SEARCH_BUDGET_PER_TICK, tickTime)) {
            List<Point3D> newPath = search.succeeded() ? search.getPath() : (drivingToClosest ? search.getBestPath() : null);
            search = null;
            if (newPath != null && newPath.size() > 1) {
                path = newPath;
                pathIndex = 0;
                beginDriving();
                return false;
            }
            return stuckFallback(player);
        }
        return false;
    }

    private boolean stuckFallback(IWrapperPlayer player) {
        if (fitSpot != null && !driveOnly) {
            teleportToGround(fitSpot.getGround(), fitSpot.getYaw(), null, LanguageSystem.INTERACT_PHONE_STUCKTELEPORT);
            return false;
        }
        park();
        message(player, LanguageSystem.INTERACT_PHONE_STUCK);
        return true;
    }

    //-----FLYING-----

    /**
     * For aircraft that can't hover in and land.  Puts it straight on the ground near the player,
     * far enough out that none of it lands on them.
     */
    private boolean teleportAircraftToGround(IWrapperPlayer player) {
        pathfinder = new GroundPathfinder(world, shape, Double.POSITIVE_INFINITY);
        //Far enough out that none of it lands on the player, whichever way it faces.
        double minDistance = shape.radius() + 1.5;
        startSpotSearch(player.getPosition(), minDistance, minDistance + FALLBACK_SPOT_MAX, SpotPurpose.AIRCRAFT);
        return false;
    }

    private boolean startFlight(IWrapperPlayer player) {
        Point3D playerPosition = player.getPosition();
        pathfinder = new GroundPathfinder(world, shape, Double.POSITIVE_INFINITY);
        cruiseY = getCruiseHeight(playerPosition);
        startEngines(true);
        set(vehicle.getOrCreateVariable(VehiclePhone.VAR_HOVER_HELD), 0);
        set(vehicle.getOrCreateVariable(VehiclePhone.VAR_STAY_ON), 1);
        set(vehicle.getOrCreateVariable(VehiclePhone.VAR_HOVER), 0);
        set(vehicle.getOrCreateVariable(VehiclePhone.VAR_GEAR), 1);
        set(vehicle.parkingBrakeVar, 0);
        set(vehicle.throttleVar, 0);
        if (!driveOnly) {
            //Arrive from in front of the player.
            double distance = getSummonDistance();
            Point3D spawn = new Point3D(playerPosition.x + Math.sin(frontAngle) * distance, cruiseY, playerPosition.z + Math.cos(frontAngle) * distance);
            //Make sure the arrival point clears what's under it too.
            spawn.y = Math.max(cruiseY, getCruiseHeight(spawn));
            cruiseY = spawn.y;
            teleport(spawn, yawTowards(spawn, playerPosition), Phase.FLY_TO_PLAYER, LanguageSystem.INTERACT_PHONE_ONITSWAY);
        } else {
            message(player, LanguageSystem.INTERACT_PHONE_ONITSWAY);
            VehicleSummonFlight.flyTo(vehicle, new Point3D(vehicle.position.x, cruiseY, vehicle.position.z), FLY_SPEED * 0.5);
            setPhase(Phase.CLIMB);
        }
        return false;
    }

    private boolean fly(IWrapperPlayer player) {
        Point3D position = vehicle.position;
        switch (phase) {
            case CLIMB: {
                if (Math.abs(position.y - cruiseY) < 1) {
                    setPhase(Phase.FLY_TO_PLAYER);
                }
                break;
            }
            case FLY_TO_PLAYER: {
                //Follow the player as they move.
                Point3D target = new Point3D(player.getPosition().x, cruiseY, player.getPosition().z);
                if (phaseTicks == 1 || phaseTicks % 10 == 0) {
                    VehicleSummonFlight.flyTo(vehicle, target, FLY_SPEED);
                }
                if (horizontalDistance(position, target) < 1.5 && Math.abs(position.y - cruiseY) < 1) {
                    //Overhead.  Switch to hover and look for somewhere to land.
                    set(vehicle.getOrCreateVariable(VehiclePhone.VAR_HOVER), 1);
                    startLandingSearch(player);
                }
                break;
            }
            case FLY_TO_LANDING: {
                if (horizontalDistance(position, landingSpot) < 0.75) {
                    //If there isn't room to turn on the way down, finish turning up here first.
                    if (!landingTurnsOnWayDown && !isFacingLandingHeading()) {
                        break;
                    }
                    set(vehicle.getOrCreateVariable(VehiclePhone.VAR_GEAR), 0);
                    setPhase(Phase.DESCEND);
                    //Until it's facing its landing heading, only come down to just above the ground and wait there.
                    descentHeld = !isFacingLandingHeading();
                    VehicleSummonFlight.flyTo(vehicle, descentHeld ? getDescentHold() : getDescentTarget(), DESCEND_SPEED);
                }
                break;
            }
            case DESCEND: {
                if (descentHeld) {
                    if (isFacingLandingHeading()) {
                        //Finished turning.  Carry on down.
                        descentHeld = false;
                        setPhase(Phase.DESCEND);
                        VehicleSummonFlight.flyTo(vehicle, getDescentTarget(), DESCEND_SPEED);
                    }
                    break;
                }
                double heightLeft = position.y - (landingSpot.y + shape.groundOffset());
                //Note MTS's isAnythingOnGround() is true when nothing is on the ground, so check the grounded set directly.
                boolean wheelsDown = !vehicle.groundDeviceCollective.groundedGroundDevices.isEmpty();
                //If we've stopped sinking, we're resting on something.  Call that landed rather than hang there.
                boolean stalled = false;
                if (phaseTicks % 40 == 0) {
                    stalled = phaseTicks > 40 && lastDescentHeight - heightLeft < 0.1;
                    lastDescentHeight = heightLeft;
                }
                //Keep pressing down for a moment after first contact, so every wheel settles onto
                //the ground instead of the first one to touch holding the rest up.
                if ((wheelsDown || stalled || heightLeft < -0.5) && settleTicks == 0) {
                    settleTicks = SETTLE_TICKS;
                }
                if (settleTicks > 0 && --settleTicks == 0) {
                    VehicleSummonFlight.stop(vehicle);
                    VehiclePhone.restoreHoverDefaults(vehicle);
                    park();
                    message(player, LanguageSystem.INTERACT_PHONE_LANDED);
                    return true;
                }
                break;
            }
            default:
                break;
        }
        return false;
    }

    private boolean isFacingLandingHeading() {
        return VehicleSummonFlight.isFacing(vehicle, GroundPathfinder.getHeadingYaw(landingHeading));
    }

    /**
     * Where the descent aims: a block below where the wheels meet the ground.  The flight eases off as it
     * nears its target, so aiming at the ground itself would crawl the last bit and stop short.  The ground
     * stops us; we finish when the wheels touch.
     */
    private Point3D getDescentTarget() {
        return new Point3D(landingSpot.x, landingSpot.y + shape.groundOffset() - LANDING_OVERSHOOT, landingSpot.z);
    }

    /**Where the descent waits for the aircraft to finish turning, just above the ground.**/
    private Point3D getDescentHold() {
        return new Point3D(landingSpot.x, landingSpot.y + shape.groundOffset() + LANDING_TURN_HOLD, landingSpot.z);
    }

    private void startLandingSearch(IWrapperPlayer player) {
        setPhase(Phase.FIND_LANDING);
        landingCandidates.clear();
        Point3D center = player.getPosition();
        for (int dx = -LANDING_RADIUS; dx <= LANDING_RADIUS; dx += 2) {
            for (int dz = -LANDING_RADIUS; dz <= LANDING_RADIUS; dz += 2) {
                double distance = Math.hypot(dx, dz);
                //Not on top of the player.
                if (distance <= LANDING_RADIUS && distance > shape.radius() + 1.5) {
                    landingCandidates.add(new Point3D(Math.floor(center.x) + dx + 0.5, 0, Math.floor(center.z) + dz + 0.5));
                }
            }
        }
        //In front of the player first, then to the sides, then behind, and the nearest first in each.
        frontAngle = getLookAngle(player);
        landingCandidates.sort((a, b) -> {
            int sectorA = getSector(a.x - center.x, a.z - center.z);
            int sectorB = getSector(b.x - center.x, b.z - center.z);
            return sectorA != sectorB ? Integer.compare(sectorA, sectorB) : Double.compare(horizontalDistance(a, center), horizontalDistance(b, center));
        });
        landingIndex = 0;
    }

    private boolean findLanding(IWrapperPlayer player) {
        long startTime = System.nanoTime();
        while (System.nanoTime() - startTime < tickTime) {
            if (landingIndex >= landingCandidates.size()) {
                //Nowhere to land.  Stay overhead until a player gets in.
                VehiclePhone.holdInAir(vehicle);
                stopControls();
                message(player, LanguageSystem.INTERACT_PHONE_HOVERING);
                return true;
            }
            Point3D spot = landingCandidates.get(landingIndex++);
            int x = (int) Math.floor(spot.x);
            int z = (int) Math.floor(spot.z);
            int groundY = getLandingGround(x, z);
            if (groundY == GroundPathfinder.NO_SURFACE) {
                continue;
            }
            //Land with its left side to the player if it fits, or else turned whichever way it does.
            //Its left is 90 degrees anticlockwise of its nose, so the nose points 90 degrees round from the player.
            int preferredHeading = GroundPathfinder.getHeading(yawTowards(spot, player.getPosition()) - 90);
            int[] heading = pathfinder.findLandingHeading(x, groundY, z, preferredHeading, GroundPathfinder.getHeading(vehicle.orientation.angles.y), cruiseY);
            if (heading != null) {
                landingSpot = new Point3D(spot.x, groundY, spot.z);
                landingHeading = heading[0];
                landingTurnsOnWayDown = heading[1] == 1;
                //Start turning now, smoothly, as it flies over to the spot.
                VehicleSummonFlight.turnTo(vehicle, GroundPathfinder.getHeadingYaw(landingHeading));
                VehicleSummonFlight.flyTo(vehicle, new Point3D(spot.x, cruiseY, spot.z), FLY_SPEED * 0.4);
                setPhase(Phase.FLY_TO_LANDING);
                return false;
            }
        }
        return false;
    }

    /**
     * Returns the height of the ground under the cruising aircraft in this column, or
     * {@link GroundPathfinder#NO_SURFACE} if it's liquid or something it can't stand on.
     */
    private int getLandingGround(int x, int z) {
        for (int y = (int) Math.floor(cruiseY); y > cruiseY - 128; --y) {
            Point3D block = new Point3D(x + 0.5, y + 0.5, z + 0.5);
            if (!world.isAir(block)) {
                return world.isBlockLiquid(block) || !world.isBlockSolid(block, Axis.UP) ? GroundPathfinder.NO_SURFACE : y + 1;
            }
        }
        return GroundPathfinder.NO_SURFACE;
    }

    /**Height for the aircraft origin that clears everything around the position and the player.**/
    private double getCruiseHeight(Point3D around) {
        double highest = around.y;
        //Cover everything the aircraft could be over while hovering here, at any heading.
        int range = 12 + (int) Math.ceil(shape.radius());
        for (int dx = -range; dx <= range; dx += 4) {
            for (int dz = -range; dz <= range; dz += 4) {
                highest = Math.max(highest, getColumnTop((int) Math.floor(around.x) + dx, (int) Math.floor(around.z) + dz, around.y));
            }
        }
        return highest + CRUISE_CLEARANCE - shape.bottom;
    }

    private double getColumnTop(int x, int z, double nearY) {
        int top = (int) Math.min(world.getMaxHeight() - 1, nearY + 128);
        for (int y = top; y > nearY - 32; --y) {
            if (!world.isAir(new Point3D(x + 0.5, y + 0.5, z + 0.5))) {
                return y + 1;
            }
        }
        return nearY;
    }

    private boolean isIndoors(IWrapperPlayer player) {
        Point3D position = player.getPosition();
        int top = (int) Math.min(world.getMaxHeight() - 1, position.y + 256);
        for (int y = (int) Math.floor(position.y) + 2; y <= top; ++y) {
            if (!world.isAir(new Point3D(position.x, y + 0.5, position.z))) {
                return true;
            }
        }
        return false;
    }

    //-----CONTROLS-----

    private void startEngines(boolean includeJets) {
        for (PartEngine engine : getWorkingEngines(includeJets)) {
            if (!engine.running) {
                if (engine.definition.engine.type == EngineType.MAGIC) {
                    //Magic engines start themselves once the magneto is on.  We can't use the
                    //auto-starter, as it refuses to start anything when the fuel tank is empty.
                    engine.magnetoVar.setTo(1, true);
                } else {
                    engine.autoStartEngine();
                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(engine, Signal.AS_ON));
                }
            }
        }
    }

    /**
     * The engines that aren't broken and that we'd use to bring the vehicle in.  Ground vehicles drive in,
     * so their jets don't count.
     */
    private List<PartEngine> getWorkingEngines(boolean includeJets) {
        List<PartEngine> engines = new ArrayList<>();
        for (PartEngine engine : vehicle.engines) {
            if ((includeJets || !isJet(engine)) && !engine.outOfHealth) {
                engines.add(engine);
            }
        }
        return engines;
    }

    /**
     * True if any working engine we'd start burns fuel from the tank.  Magic engines don't, so a vehicle
     * powered only by them never runs out.
     */
    private boolean needsFuel(boolean includeJets) {
        if (vehicle.isCreative || ConfigSystem.settings.general.fuelUsageFactor.value == 0) {
            return false;
        }
        for (PartEngine engine : getWorkingEngines(includeJets)) {
            if (engine.definition.engine.type != EngineType.MAGIC) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJet(PartEngine engine) {
        return engine.definition.generic.type.contains("jet");
    }

    private void stopControls() {
        set(vehicle.throttleVar, 0);
        set(vehicle.rudderInputVar, 0);
        set(vehicle.brakeVar, 0);
    }

    /**Stops and holds the vehicle where it is.  Engines are left running.**/
    private void park() {
        stopControls();
        set(vehicle.parkingBrakeVar, 1);
    }

    /**Makes whatever the vehicle is doing safe after a cancel.**/
    private void stopVehicle() {
        if (vehicle == null || !vehicle.isValid) {
            return;
        }
        if (vehicle.getOrCreateVariable(VehicleSummonFlight.VAR_ACTIVE).isActive) {
            //Airborne.  Stay up where it is until a player gets in.
            VehiclePhone.holdInAir(vehicle);
            stopControls();
        } else {
            park();
        }
    }

    /**Sets a control and syncs it to clients, only if it changed enough to matter.**/
    private static void set(ComputedVariable variable, double value) {
        if (Math.abs(variable.currentValue - value) > 0.005) {
            variable.setTo(value, true);
        }
    }

    //-----HELPERS-----

    private static double horizontalDistance(Point3D a, Point3D b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    private static double yawTowards(Point3D from, Point3D to) {
        return Math.toDegrees(Math.atan2(to.x - from.x, to.z - from.z));
    }

    private void message(IWrapperPlayer player, LanguageEntry language) {
        VehiclePhone.message(player, language, name);
    }
}
