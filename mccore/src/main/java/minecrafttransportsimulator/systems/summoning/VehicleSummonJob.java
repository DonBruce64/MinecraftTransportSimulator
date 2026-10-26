package minecrafttransportsimulator.systems.summoning;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
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

/**
 * One vehicle answering one phone call.  Runs on the server, one step per tick.
 * <p>
 * Ground vehicles: find (force-loading the chunk if needed), plan a route from a spot about
 * {@link VehiclePhone#VAR_SUMMON_DISTANCE} blocks away, teleport there and drive in with the
 * real controls.  If no route exists, teleport next to the player.  If there's no room by the
 * player, drive as close as possible.  If stuck, re-plan once, then teleport.
 * <p>
 * Aircraft that can hover (see {@link VehiclePhone#canFlyIn}): teleport in above the buildings,
 * fly over the player, switch to hover, and land straight down in the nearest clear spot within
 * {@link #LANDING_RADIUS} blocks, or hover there.  Other aircraft are teleported straight onto
 * the ground near the player.
 */
final class VehicleSummonJob {
    private static final int DEFAULT_SUMMON_DISTANCE = 30;
    private static final int LOAD_TIMEOUT = 200;
    /**How often to renew the chunk loading for a vehicle we're waking up, in ticks.**/
    private static final int CHUNK_LOAD_INTERVAL = 20;
    private static final int CANDIDATE_DIRECTIONS = 16;
    private static final int SEARCH_EXPANSIONS = 6000;
    private static final int SEARCH_BUDGET_PER_TICK = 1500;
    private static final double FIT_SPOT_MIN = 3;
    private static final double FIT_SPOT_MAX = 8;
    private static final double FALLBACK_SPOT_MAX = 16;
    /**Cruise speed, in m/s (about 30 km/h).**/
    private static final double CRUISE_SPEED = 8.3;
    private static final int STUCK_CHECK_INTERVAL = 100;
    private static final int DRIVE_TIMEOUT = 20 * 120;
    private static final double STOP_FRONT_DISTANCE = 3.5;
    private static final int LANDING_RADIUS = 20;
    private static final int LANDING_CHECKS_PER_TICK = 4;
    private static final double CRUISE_CLEARANCE = 10;
    private static final double FLY_SPEED = 1.0;
    private static final double DESCEND_SPEED = 0.2;
    /**How far below the ground the descent aims, so it reaches the ground at a steady rate.**/
    private static final double LANDING_OVERSHOOT = 1.0;
    /**Ticks to keep pressing down after first touching the ground.**/
    private static final int SETTLE_TICKS = 10;

    private enum Phase {
        LOCATE,
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

    final UUID vehicleID;
    final AWrapperWorld world;
    private final UUID playerID;
    private final String name;
    private Phase phase = Phase.LOCATE;
    private int phaseTicks;
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
    private Point3D fitSpot;
    private Point3D goal;
    private double goalRadius;
    private final List<Point3D> candidates = new ArrayList<>();
    private int candidateIndex;
    private Point3D candidate;
    private boolean candidateNeedsTeleport;
    private GroundPathfinder.Search search;
    private Point3D bestCandidate;
    private boolean bestCandidateNeedsTeleport;
    private List<Point3D> bestCandidatePath;
    private double bestCandidateDistance = Double.MAX_VALUE;

    //Driving.
    private List<Point3D> path;
    private int pathIndex;
    private Point3D lastProgressPosition;
    private int replans;
    private boolean drivingToClosest;

    //Flying.
    private double cruiseY;
    private final List<Point3D> landingCandidates = new ArrayList<>();
    private int landingIndex;
    private Point3D landingSpot;
    private double lastDescentHeight = Double.MAX_VALUE;
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

    /**Runs one tick.  Returns true when the job is finished and can be dropped.**/
    boolean tick() {
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
        switch (phase) {
            case LOCATE:
                return locate(player);
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
                message(player, LanguageSystem.INTERACT_PHONE_NOANSWER);
                releaseChunk();
                return true;
            }
            return false;
        }

        //Found it.  Check it can come.
        for (APart part : vehicle.allParts) {
            if (part.rider != null) {
                message(player, LanguageSystem.INTERACT_PHONE_INUSE);
                releaseChunk();
                return true;
            }
        }
        if (vehicle.outOfHealth) {
            message(player, LanguageSystem.INTERACT_PHONE_WRECKED);
            releaseChunk();
            return true;
        }
        boolean isAircraft = vehicle.definition.motorized.isAircraft;
        boolean flyIn = isAircraft && VehiclePhone.canFlyIn(vehicle);
        //Aircraft that can't fly in are just teleported, so they don't need fuel.
        if ((!isAircraft || flyIn) && needsFuel(isAircraft) && vehicle.fuelTank.getFluidLevel() == 0) {
            message(player, LanguageSystem.INTERACT_PHONE_NOFUEL);
            releaseChunk();
            return true;
        }
        shape = VehicleShape.of(vehicle);
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
        setPhase(Phase.PLAN_GROUND);
        pathfinder = new GroundPathfinder(world, shape);
        Point3D playerPosition = player.getPosition().copy();
        int playerY = (int) Math.floor(playerPosition.y);

        //Find somewhere next to the player the whole vehicle fits.
        fitSpot = findSpotNear(playerPosition, playerY, FIT_SPOT_MIN, FIT_SPOT_MAX);
        goal = fitSpot != null ? fitSpot : playerPosition;
        goalRadius = fitSpot != null ? 1.5 : 3;

        //Already within driving range: drive from where it is, never teleport.
        //Otherwise try arrival spots around the player, starting straight ahead of them so they
        //see it coming, then working round both sides.
        candidates.clear();
        if (driveOnly) {
            candidates.add(null);
        } else {
            double frontAngle = getLookAngle(player);
            double distance = getSummonDistance();
            for (int i = 0; i < CANDIDATE_DIRECTIONS; ++i) {
                int step = (i + 1) / 2;
                double angle = frontAngle + (i % 2 == 0 ? step : -step) * (2 * Math.PI / CANDIDATE_DIRECTIONS);
                candidates.add(new Point3D(playerPosition.x + Math.sin(angle) * distance, playerY, playerPosition.z + Math.cos(angle) * distance));
            }
        }
        candidateIndex = 0;
        search = null;
    }

    /**The horizontal direction the player is looking, as an angle for sin/cos (x, z).**/
    private static double getLookAngle(IWrapperPlayer player) {
        Point3D look = player.getLineOfSight(1).copy();
        return Math.atan2(look.x, look.z);
    }

    /**
     * Returns the closest ground spot to the position, between min and max blocks away, where the
     * whole vehicle fits.  Null if there isn't one.
     */
    private Point3D findSpotNear(Point3D center, int refY, double min, double max) {
        Point3D best = null;
        double bestDistance = Double.MAX_VALUE;
        int range = (int) Math.ceil(max);
        //Big aircraft search a wide area, so check fewer columns rather than lag the tick.
        int step = Math.max(1, range / 16);
        for (int dx = -range; dx <= range; dx += step) {
            for (int dz = -range; dz <= range; dz += step) {
                double distance = Math.hypot(dx, dz);
                if (distance < min || distance > max || distance >= bestDistance) {
                    continue;
                }
                int x = (int) Math.floor(center.x) + dx;
                int z = (int) Math.floor(center.z) + dz;
                int y = pathfinder.findWholeSurface(x, z, refY, 2, 3);
                if (y != GroundPathfinder.NO_SURFACE) {
                    best = new Point3D(x + 0.5, y, z + 0.5);
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private boolean planGround(IWrapperPlayer player) {
        if (search == null) {
            if (candidateIndex >= candidates.size()) {
                return groundFallback(player);
            }
            Point3D next = candidates.get(candidateIndex++);
            int x, y, z;
            if (next == null) {
                //Drive from where it is now.
                x = (int) Math.floor(vehicle.position.x);
                z = (int) Math.floor(vehicle.position.z);
                y = pathfinder.findSurface(x, z, (int) Math.floor(vehicle.position.y + shape.bottom + 0.5), 1, 2);
                candidateNeedsTeleport = false;
            } else {
                x = (int) Math.floor(next.x);
                z = (int) Math.floor(next.z);
                y = pathfinder.findWholeSurface(x, z, (int) next.y, 8, 8);
                candidateNeedsTeleport = true;
            }
            if (y == GroundPathfinder.NO_SURFACE) {
                return false;
            }
            candidate = new Point3D(x + 0.5, y, z + 0.5);
            search = pathfinder.new Search(x, y, z, goal, goalRadius, SEARCH_EXPANSIONS);
        }

        if (search.step(SEARCH_BUDGET_PER_TICK)) {
            if (search.succeeded()) {
                List<Point3D> foundPath = search.getPath();
                search = null;
                startRoute(player, candidate, candidateNeedsTeleport, foundPath, false, LanguageSystem.INTERACT_PHONE_ONITSWAY);
                return false;
            }
            if (search.getBestDistance() < bestCandidateDistance) {
                bestCandidateDistance = search.getBestDistance();
                bestCandidate = candidate;
                bestCandidateNeedsTeleport = candidateNeedsTeleport;
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
                startRoute(player, bestCandidate, false, bestCandidatePath, true, LanguageSystem.INTERACT_PHONE_NOWAYCLOSEST);
                return false;
            }
            park();
            message(player, LanguageSystem.INTERACT_PHONE_NOWAYFROMHERE);
            return true;
        }
        if (fitSpot != null) {
            //There's room by the player, we just can't drive there.  Bring it straight to them.
            teleportToGround(fitSpot, yawTowards(fitSpot, player.getPosition()), null, LanguageSystem.INTERACT_PHONE_NOWAYTELEPORT);
            return false;
        }
        if (bestCandidate != null && bestCandidatePath.size() > 1) {
            //No room by the player.  Get as close as we can.
            startRoute(player, bestCandidate, bestCandidateNeedsTeleport, bestCandidatePath, true, LanguageSystem.INTERACT_PHONE_NOROOMCLOSEST);
            return false;
        }
        Point3D spot = findSpotNear(player.getPosition(), (int) Math.floor(player.getPosition().y), FIT_SPOT_MIN, FALLBACK_SPOT_MAX);
        if (spot != null) {
            teleportToGround(spot, yawTowards(spot, player.getPosition()), null, LanguageSystem.INTERACT_PHONE_PARKEDCLOSEST);
            return false;
        }
        message(player, LanguageSystem.INTERACT_PHONE_NOROOM);
        releaseChunk();
        return true;
    }

    private void startRoute(IWrapperPlayer player, Point3D start, boolean teleport, List<Point3D> route, boolean toClosest, LanguageEntry startMessage) {
        path = route;
        pathIndex = 0;
        drivingToClosest = toClosest;
        if (teleport) {
            Point3D ahead = route.get(Math.min(5, route.size() - 1));
            double yaw = ahead.equals(start) ? yawTowards(start, player.getPosition()) : yawTowards(start, ahead);
            teleportToGround(start, yaw, Phase.DRIVE, startMessage);
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

        //Slow for turns and for the end of the path.
        double speed = vehicle.velocity * vehicle.speedFactor * 20;
        double targetSpeed = CRUISE_SPEED * Math.max(0.35, Math.min(1, 1 - Math.abs(angle) / 120));
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
            if (stuck || phaseTicks > DRIVE_TIMEOUT) {
                return onStuck(player);
            }
        }
        return false;
    }

    private boolean onStuck(IWrapperPlayer player) {
        stopControls();
        if (replans == 0 && phaseTicks <= DRIVE_TIMEOUT) {
            ++replans;
            int x = (int) Math.floor(vehicle.position.x);
            int z = (int) Math.floor(vehicle.position.z);
            int y = pathfinder.findSurface(x, z, (int) Math.floor(vehicle.position.y + shape.bottom + 0.5), 1, 2);
            if (y != GroundPathfinder.NO_SURFACE) {
                search = pathfinder.new Search(x, y, z, goal, goalRadius, SEARCH_EXPANSIONS);
                setPhase(Phase.REPLAN);
                return false;
            }
        }
        return stuckFallback(player);
    }

    private boolean replan(IWrapperPlayer player) {
        if (search.step(SEARCH_BUDGET_PER_TICK)) {
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
            teleportToGround(fitSpot, yawTowards(fitSpot, player.getPosition()), null, LanguageSystem.INTERACT_PHONE_STUCKTELEPORT);
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
        pathfinder = new GroundPathfinder(world, shape);
        Point3D playerPosition = player.getPosition().copy();
        double minDistance = Math.max(shape.halfWidth, shape.halfLength) + 1.5;
        Point3D spot = findSpotNear(playerPosition, (int) Math.floor(playerPosition.y), minDistance, minDistance + FALLBACK_SPOT_MAX);
        if (spot == null) {
            message(player, LanguageSystem.INTERACT_PHONE_NOROOM);
            releaseChunk();
            return true;
        }
        teleportToGround(spot, yawTowards(spot, playerPosition), null, LanguageSystem.INTERACT_PHONE_BROUGHT);
        return false;
    }

    private boolean startFlight(IWrapperPlayer player) {
        Point3D playerPosition = player.getPosition();
        cruiseY = getCruiseHeight(playerPosition);
        startEngines(true);
        set(vehicle.getOrCreateVariable(VehiclePhone.VAR_STAY_ON), 1);
        set(vehicle.getOrCreateVariable(VehiclePhone.VAR_HOVER), 0);
        set(vehicle.getOrCreateVariable(VehiclePhone.VAR_GEAR), 1);
        set(vehicle.parkingBrakeVar, 0);
        set(vehicle.throttleVar, 0);
        if (!driveOnly) {
            //Arrive from in front of the player.
            double frontAngle = getLookAngle(player);
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
                    set(vehicle.getOrCreateVariable(VehiclePhone.VAR_GEAR), 0);
                    //Aim a block below where the wheels meet the ground.  The flight eases off as it nears
                    //its target, so aiming at the ground itself would crawl the last bit and stop short.
                    //The ground stops us; we finish when the wheels touch.
                    VehicleSummonFlight.flyTo(vehicle, new Point3D(landingSpot.x, landingSpot.y + shape.groundOffset() - LANDING_OVERSHOOT, landingSpot.z), DESCEND_SPEED);
                    setPhase(Phase.DESCEND);
                }
                break;
            }
            case DESCEND: {
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
        landingCandidates.sort((a, b) -> Double.compare(horizontalDistance(a, center), horizontalDistance(b, center)));
        landingIndex = 0;
    }

    private boolean findLanding(IWrapperPlayer player) {
        for (int i = 0; i < LANDING_CHECKS_PER_TICK; ++i) {
            if (landingIndex >= landingCandidates.size()) {
                //Nowhere to land.  Stay overhead in hover, powered.
                VehicleSummonFlight.stop(vehicle);
                set(vehicle.getOrCreateVariable(VehiclePhone.VAR_HOVER), 1);
                set(vehicle.getOrCreateVariable(VehiclePhone.VAR_STAY_ON), 1);
                stopControls();
                message(player, LanguageSystem.INTERACT_PHONE_HOVERING);
                return true;
            }
            Point3D spot = landingCandidates.get(landingIndex++);
            double groundY = getLandingGround(spot);
            if (!Double.isNaN(groundY)) {
                landingSpot = new Point3D(spot.x, groundY, spot.z);
                VehicleSummonFlight.flyTo(vehicle, new Point3D(spot.x, cruiseY, spot.z), FLY_SPEED * 0.4);
                setPhase(Phase.FLY_TO_LANDING);
                return false;
            }
        }
        return false;
    }

    /**
     * Returns the ground height to land on at this spot, or NaN if the aircraft can't come
     * straight down there: the footprint at its current heading and the whole column above must be clear.
     */
    private double getLandingGround(Point3D spot) {
        int x = (int) Math.floor(spot.x);
        int z = (int) Math.floor(spot.z);
        int groundY = Integer.MIN_VALUE;
        for (int y = (int) Math.floor(cruiseY); y > cruiseY - 128; --y) {
            if (!world.isAir(new Point3D(x + 0.5, y + 0.5, z + 0.5))) {
                if (world.isBlockLiquid(new Point3D(x + 0.5, y + 0.5, z + 0.5)) || !world.isBlockSolid(new Point3D(x + 0.5, y + 0.5, z + 0.5), Axis.UP)) {
                    return Double.NaN;
                }
                groundY = y + 1;
                break;
            }
        }
        if (groundY == Integer.MIN_VALUE) {
            return Double.NaN;
        }

        //Footprint at the current heading.
        double yaw = Math.toRadians(vehicle.orientation.angles.y);
        double extentX = Math.abs(Math.cos(yaw)) * shape.halfWidth + Math.abs(Math.sin(yaw)) * shape.halfLength;
        double extentZ = Math.abs(Math.sin(yaw)) * shape.halfWidth + Math.abs(Math.cos(yaw)) * shape.halfLength;

        //Ground under each corner must be within a block of the middle, so we don't land half off a cliff.
        for (int cornerX = -1; cornerX <= 1; cornerX += 2) {
            for (int cornerZ = -1; cornerZ <= 1; cornerZ += 2) {
                Point3D corner = new Point3D(spot.x + cornerX * extentX * 0.8, groundY - 0.5, spot.z + cornerZ * extentZ * 0.8);
                if (!world.isBlockSolid(corner, Axis.UP) && !world.isBlockSolid(corner.add(0, -1, 0), Axis.UP)) {
                    return Double.NaN;
                }
            }
        }

        //Everything from just above the ground up to cruise height must be clear.
        double bottomY = groundY + 0.3;
        double topY = cruiseY + shape.top;
        BoundingBox column = new BoundingBox(new Point3D(spot.x, (bottomY + topY) / 2, spot.z), extentX, (topY - bottomY) / 2, extentZ);
        if (world.checkForCollisions(column, new Point3D(), false, false)) {
            return Double.NaN;
        }
        return groundY;
    }

    /**Height for the aircraft origin that clears everything around the position and the player.**/
    private double getCruiseHeight(Point3D around) {
        double highest = around.y;
        for (int dx = -12; dx <= 12; dx += 4) {
            for (int dz = -12; dz <= 12; dz += 4) {
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
        for (PartEngine engine : vehicle.engines) {
            if ((includeJets || !isJet(engine)) && !engine.running) {
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
     * True if any engine we'd start burns fuel from the tank.  Magic engines don't, so a vehicle
     * powered only by them never runs out.
     */
    private boolean needsFuel(boolean includeJets) {
        if (vehicle.isCreative || ConfigSystem.settings.general.fuelUsageFactor.value == 0) {
            return false;
        }
        for (PartEngine engine : vehicle.engines) {
            if ((includeJets || !isJet(engine)) && engine.definition.engine.type != EngineType.MAGIC) {
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
            //Airborne.  Hover where it is.
            VehicleSummonFlight.stop(vehicle);
            set(vehicle.getOrCreateVariable(VehiclePhone.VAR_HOVER), 1);
            set(vehicle.getOrCreateVariable(VehiclePhone.VAR_STAY_ON), 1);
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
