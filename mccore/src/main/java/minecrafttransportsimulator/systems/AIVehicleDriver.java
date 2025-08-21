package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.entities.instances.AEntityVehicleE_Powered;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.jsondefs.JSONPart;

import java.util.List;

/**
 * Simple AI controller that can start engines, apply throttle and
 * shift gears for a vehicle.  This class is intended to be called
 * from a seat's update method when the rider is an AI‑controlled mob.
 *
 * The logic implemented here is deliberately straightforward:
 *  • It automatically engages each engine's auto‑starter until the engine
 *    reports itself as running.  The {@link PartEngine#autoStartEngine()}
 *    method sets the magneto and engages the starter when fuel is present【768831468537987†L1256-L1266】.
 *  • Once the engine is running, it sets a constant throttle level using
 *    the vehicle's {@link AEntityVehicleE_Powered#throttleVar}.  The throttle
 *    is updated via {@link minecrafttransportsimulator.baseclasses.ComputedVariable#setTo(double, boolean)};
 *    no network packet is sent because AI control runs solely on the server【72024928282637†L179-L184】.
 *  • It then monitors engine RPM and current gear, and uses
 *    {@link PartEngine#shiftUp()} and {@link PartEngine#shiftDown()} to
 *    change gears.  The simple strategy here is to shift from neutral
 *    into first gear when the vehicle starts moving, upshift when RPM
 *    exceeds 70 % of the engine's max RPM, and downshift when RPM falls
 *    close to the idle RPM.  These methods internally check whether
 *    shifting is legal (vehicle speed below {@code PartEngine.MAX_SHIFT_SPEED})
 *    and send packets to clients as needed【768831468537987†L1339-L1371】【768831468537987†L1374-L1407】.
 */
public class AIVehicleDriver {
    /** Controlled vehicle. */
    private final AEntityVehicleE_Powered vehicle;

    /**
     * Cruise throttle for AI driving.  This is the desired throttle when the
     * vehicle is travelling straight toward its target.  It will be scaled
     * down during turns or obstacle avoidance.  Valid range is between 0
     * and {@code 1.0}.
     */
    private final double cruiseThrottle;

    /**
     * Minimum throttle to apply when sharply turning or avoiding obstacles.
     * Using a non‑zero value prevents the vehicle from stopping entirely
     * during tight turns, which could stall the engine.  This can be
     * tuned based on the vehicle's behaviour; default is 0.2.
     */
    private final double minThrottle = 0.2;

    /**
     * Range, in blocks, within which the AI will search for a player to
     * follow.  If no player is found inside this radius, the AI will
     * disengage steering and throttle control for that tick.
     */
    private final double followRange = 64.0;

    /**
     * Distance, in blocks, to scan ahead of the vehicle for solid blocks.
     * If a block is detected within this distance, the AI will attempt to
     * steer away and reduce throttle.
     */
    private final double detectionDistance = 16.0;

    /**
     * Constructs an AI driver for the given vehicle.  A constant throttle
     * value can be supplied; use a value between 0 and {@code 1.0}
     * (see {@link AEntityVehicleE_Powered#MAX_THROTTLE}).
     */
    public AIVehicleDriver(AEntityVehicleE_Powered vehicle, double cruiseThrottle) {
        this.vehicle = vehicle;
        // Clamp the supplied cruise throttle into a valid range based on the vehicle's maximum throttle.
        this.cruiseThrottle = Math.max(0.0, Math.min(cruiseThrottle, AEntityVehicleE_Powered.MAX_THROTTLE));
    }

    /**
     * Called each tick to update the AI control state.  This method
     * starts engines if needed, applies throttle and shifts gears.  It
     * should be invoked only on the server to avoid unnecessary packet
     * traffic.
     */
    public void update() {
        // Release the parking brake and foot brake before doing anything else.  If we
        // don't do this then the vehicle will stay locked in place even when throttle
        // is applied.  Sending a packet ensures clients update animations and gauge
        // states accordingly.
        if (vehicle.parkingBrakeVar.isActive) {
            vehicle.parkingBrakeVar.setTo(0, true);
        }
        if (vehicle.brakeVar.currentValue > 0) {
            vehicle.brakeVar.setTo(0, true);
        }

        // Start any engines that are not running.  The default autoStartEngine() only
        // toggles the magneto and starter on the server side and does not send
        // any packets to clients.  To ensure the starter animations and sounds
        // play correctly on clients, explicitly set the magneto and starter
        // variables with packet dispatch after auto‑starting.
        for (PartEngine engine : vehicle.engines) {
            if (!engine.running) {
                // Engage the auto‑starter on the server.  This sets magnetoVar and
                // electricStarterVar to 1 for normal engines【768831468537987†L1256-L1266】.
                engine.autoStartEngine();
                // Sync magneto state to clients if it changed.
                if (engine.magnetoVar.currentValue > 0) {
                    engine.magnetoVar.setTo(engine.magnetoVar.currentValue, true);
                }
                // Only normal (piston) engines use the electric starter.  If this is
                // such an engine and the starter was engaged by autoStartEngine(),
                // propagate the state to clients.  Jet and electric engines will
                // ignore this call since electricStarterVar will remain at 0.
                if (engine.definition.engine.type == JSONPart.EngineType.NORMAL &&
                        engine.electricStarterVar.currentValue > 0) {
                    engine.electricStarterVar.setTo(engine.electricStarterVar.currentValue, true);
                }
            }
        }

        // Determine if at least one engine is running.  AI operations only
        // proceed once an engine is online.
        boolean anyEngineRunning = vehicle.engines.stream().anyMatch(e -> e.running);
        if (!anyEngineRunning) {
            return;
        }

        // Compute steering and dynamic throttle only if this vehicle supports
        // rudder control (i.e. is a physics-based vehicle) and a target
        // player exists in range.  Otherwise, just maintain cruise throttle.
        double desiredThrottle = cruiseThrottle;
        double desiredRudder = 0.0;

        // Only attempt steering if this vehicle is an instance of EntityVehicleF_Physics.
        if (vehicle instanceof EntityVehicleF_Physics) {
            EntityVehicleF_Physics physicsVehicle = (EntityVehicleF_Physics) vehicle;
            // Look for nearby players to follow.
            BoundingBox searchBox = new BoundingBox(vehicle.position, followRange);
            java.util.List<IWrapperPlayer> players = vehicle.world.getPlayersWithin(searchBox);
            if (!players.isEmpty()) {
                // Find the closest player.
                IWrapperPlayer closestPlayer = players.get(0);
                double closestDistance = vehicle.position.distanceTo(closestPlayer.getPosition());
                for (int i = 1; i < players.size(); ++i) {
                    IWrapperPlayer player = players.get(i);
                    double distance = vehicle.position.distanceTo(player.getPosition());
                    if (distance < closestDistance) {
                        closestPlayer = player;
                        closestDistance = distance;
                    }
                }

                // Compute vector to player in the horizontal plane.
                Point3D diff = closestPlayer.getPosition().copy().subtract(vehicle.position);
                diff.y = 0;
                if (diff.length() > 0.0001) {
                    double targetYaw = Math.toDegrees(Math.atan2(diff.x, diff.z));

                    // Update orientation angles and get current yaw.
                    physicsVehicle.orientation.convertToAngles();
                    double currentYaw = physicsVehicle.orientation.angles.y;

                    // Compute the yaw difference to the target in [-180, 180].
                    double deltaYaw = targetYaw - currentYaw;
                    while (deltaYaw > 180.0) {
                        deltaYaw -= 360.0;
                    }
                    while (deltaYaw < -180.0) {
                        deltaYaw += 360.0;
                    }

                    // Perform forward obstacle detection.  Build a unit vector for the current yaw.
                    double yawRadians = Math.toRadians(currentYaw);
                    Point3D forward = new Point3D(Math.sin(yawRadians), 0, Math.cos(yawRadians));
                    Point3D delta = forward.copy().scale(detectionDistance);
                    BlockHitResult hitResult = vehicle.world.getBlockHit(vehicle.position, delta);
                    boolean obstacleAhead = hitResult != null;

                    if (obstacleAhead) {
                        // If there is an obstacle directly ahead, search multiple directions
                        // to find the clearest path.  Test yaw offsets around the current
                        // heading and choose the one with the longest unobstructed distance.
                        double[] offsets = {15, -15, 30, -30, 45, -45, 60, -60, 90, -90};
                        double bestOffset = 0;
                        double bestClearDistance = -1;
                        for (double offset : offsets) {
                            double scanYaw = currentYaw + offset;
                            double rad = Math.toRadians(scanYaw);
                            Point3D dir = new Point3D(Math.sin(rad), 0, Math.cos(rad));
                            Point3D scanDelta = dir.copy().scale(detectionDistance);
                            BlockHitResult scanResult = vehicle.world.getBlockHit(vehicle.position, scanDelta);
                            double clearDistance;
                            if (scanResult == null) {
                                clearDistance = detectionDistance;
                            } else {
                                clearDistance = vehicle.position.distanceTo(scanResult.hitPosition);
                            }
                            if (clearDistance > bestClearDistance) {
                                bestClearDistance = clearDistance;
                                bestOffset = offset;
                            }
                        }
                        // Steer toward the chosen offset.  Convert offset to rudder input by
                        // negating it (rudder input is opposite to yaw difference).  Clamp to
                        // ±MAX_RUDDER_ANGLE.【527729042160951†L214-L229】
                        double rudder = -bestOffset;
                        double maxRudder = EntityVehicleF_Physics.MAX_RUDDER_ANGLE;
                        if (rudder > maxRudder) {
                            rudder = maxRudder;
                        } else if (rudder < -maxRudder) {
                            rudder = -maxRudder;
                        }
                        desiredRudder = rudder;
                        // Slow down to the minimum throttle while we manoeuvre around the obstacle.
                        desiredThrottle = minThrottle;
                    } else {
                        // No obstacle: steer directly toward the target yaw.
                        double rudder = -deltaYaw;
                        double maxRudder = EntityVehicleF_Physics.MAX_RUDDER_ANGLE;
                        if (rudder > maxRudder) {
                            rudder = maxRudder;
                        } else if (rudder < -maxRudder) {
                            rudder = -maxRudder;
                        }
                        desiredRudder = rudder;
                        // Scale throttle based on heading error.
                        double yawMagnitude = Math.min(90.0, Math.abs(deltaYaw));
                        double throttleRange = cruiseThrottle - minThrottle;
                        desiredThrottle = minThrottle + throttleRange * (1.0 - yawMagnitude / 90.0);
                    }
                }
            }
            // Apply rudder input with packet.  Using sendPacket=true ensures client animations
            // such as steering wheels update correctly.
            physicsVehicle.rudderInputVar.setTo(desiredRudder, true);
        }

        // Apply throttle with packet.  Using sendPacket=true ensures throttle animations
        // (e.g., engine sound pitch and gauge needles) are synced across clients.
        vehicle.throttleVar.setTo(desiredThrottle, true);

        // Perform shift logic for each running engine.
        for (PartEngine engine : vehicle.engines) {
            if (!engine.running) {
                continue;
            }
            double rpm = engine.rpm;
            double idleRPM = engine.idleRPMVar.currentValue;
            double maxRPM = engine.maxRPMVar.currentValue;
            int currentGear = (int) engine.currentGearVar.currentValue;

            // If engine is in neutral or reverse and vehicle nearly stopped,
            // shift into first gear when throttle is applied.
            if (vehicle.axialVelocity < 0.01 && desiredThrottle > 0.1 && currentGear <= 0) {
                engine.shiftUp();
            }

            // Upshift when RPM exceeds 70 % of max RPM and we have higher gears available.
            if (rpm > 0.7 * maxRPM && currentGear > 0 && currentGear < engine.forwardsGears) {
                engine.shiftUp();
            }

            // Downshift when RPM falls close to idle and we are above first gear.
            if (rpm < idleRPM * 1.2 && currentGear > 1) {
                engine.shiftDown();
            }
        }
    }
}