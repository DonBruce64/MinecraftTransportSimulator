package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlNotification;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;

/**
 * Mouse-based flight controller inspired by War Thunder style controls.
 * The mouse controls an invisible aim reticle. An autopilot steers the
 * aircraft toward that reticle. In third-person, the camera smoothly
 * follows the aim direction, decoupled from the aircraft orientation.
 * <p>
 * Supports both fixed-wing aircraft and helicopters with different
 * autopilot logic for each type.
 * <p>
 * Uses simple Euler angles (yaw/pitch) instead of raw matrix operations
 * to avoid quaternion/matrix composition issues and numerical drift.
 */
public class MouseFlightController {

    /** Whether mouse flight mode is currently active. */
    public static boolean isMouseFlightActive = false;

    /** Whether the current vehicle is a helicopter (has rotor propellers). */
    public static boolean isHelicopter = false;
    private static EntityVehicleF_Physics activeAircraft;

    /** Stored mouse deltas captured before updateRider consumes them. */
    public static float storedYawDelta = 0;
    public static float storedPitchDelta = 0;

    // Aim angles (where the player wants to fly).
    private static double aimYaw = 0;
    private static double aimPitch = 0;
    // Previous-tick aim angles for partial-tick interpolation in render.
    private static double prevAimYaw = 0;
    private static double prevAimPitch = 0;

    // Camera angles (smoothly follows aim, what the player sees).
    private static double camYaw = 0;
    private static double camPitch = 0;
    private static double prevCamYaw = 0;
    private static double prevCamPitch = 0;

    /** Aim direction as a unit vector in world space. */
    public static final Point3D mouseAimForward = new Point3D(0, 0, 1);

    /** How quickly the camera follows the aim (higher = faster, 0-1 range per tick). */
    private static final double CAM_SMOOTH_SPEED = 5.0;

    /** Fixed-wing attitude responses and angular rates, in degrees per tick. */
    private static final double AIRPLANE_AIM_RESPONSE = 0.12;
    private static final double AIRPLANE_ROLL_RESPONSE = 0.16;
    private static final double AIRPLANE_RATE_DAMPING = 0.18;
    private static final double AIRPLANE_MAX_AIM_RATE = 3.0;
    private static final double AIRPLANE_MAX_ROLL_RATE = 4.0;
    private static final double MAX_BANK_ANGLE = 60.0;
    private static final double MAX_SIDESLIP_ANGLE = 15.0;
    private static final double BANK_PER_HEADING_ERROR = 2.0;
    private static double lastAirplaneYawError = 0;

    /** Mouse-aim instructor tuning for helicopters.  Rates are in degrees per tick. */
    private static final double HELI_AIM_DEAD_ZONE = 0.35;
    private static final double HELI_BANK_DEAD_ZONE = 0.50;
    private static final double HELI_PITCH_RESPONSE = 0.08;
    private static final double HELI_YAW_RESPONSE_HOVER = 0.09;
    private static final double HELI_YAW_RESPONSE_FORWARD = 0.06;
    private static final double HELI_ROLL_RESPONSE = 0.12;
    private static final double HELI_RATE_DAMPING = 0.18;
    private static final double HELI_MAX_PITCH_RATE = 1.8;
    private static final double HELI_MAX_YAW_RATE = 2.4;
    private static final double HELI_MAX_ROLL_RATE = 2.0;
    private static final double HELI_MAX_BANK_ANGLE = 30.0;
    private static final double HELI_BANK_PER_YAW_ERROR = 0.45;
    private static final double HELI_BANK_START_SPEED = 2.0;
    private static final double HELI_BANK_FULL_SPEED = 12.0;
    private static final double HELI_VERTICAL_AIM_BLEND = 0.05;
    private static double lastHelicopterYawError = 0;

    // Orientation matrices built from angles for camera use.
    private static final RotationMatrix aimOrientation = new RotationMatrix();
    private static final RotationMatrix camOrientation = new RotationMatrix();
    private static final RotationMatrix prevCamOrientation = new RotationMatrix();

    // Temp vectors for autopilot calculations.
    private static final Point3D tempLocal = new Point3D();
    private static final Point3D tempRight = new Point3D();
    private static final Point3D tempUp = new Point3D();
    private static final Point3D tempVelocity = new Point3D();
    private static final Point3D neutralControls = new Point3D();
    private static final Point3D controlSurfaceRates = new Point3D();
    private static final Point3D aimRotationRate = new Point3D();

    /**
     * Activates mouse flight. Initializes aim to match aircraft heading.
     */
    public static void activate(EntityVehicleF_Physics aircraft, boolean hasRotors) {
        isMouseFlightActive = true;
        isHelicopter = hasRotors;
        activeAircraft = aircraft;
        lastAirplaneYawError = 0;
        lastHelicopterYawError = 0;
        if (hasRotors) {
            aircraft.mouseAimControlVar.setActive(true, false);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.mouseAimControlVar, 1));
        }
        // Extract aircraft yaw/pitch from its orientation.
        aircraft.orientation.convertToAngles();
        aimYaw = aircraft.orientation.angles.y;
        aimPitch = aircraft.orientation.angles.x;
        prevAimYaw = aimYaw;
        prevAimPitch = aimPitch;
        camYaw = aimYaw;
        camPitch = aimPitch;
        prevCamYaw = camYaw;
        prevCamPitch = camPitch;
        rebuildOrientations();
    }

    /**
     * Deactivates mouse flight.
     */
    public static void deactivate() {
        if (isHelicopter && activeAircraft != null) {
            activeAircraft.mouseAimControlVar.setActive(false, false);
            if (InterfaceManager.packetInterface != null) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(activeAircraft.mouseAimControlVar, 0));
            }
        }
        isMouseFlightActive = false;
        isHelicopter = false;
        activeAircraft = null;
        lastAirplaneYawError = 0;
        lastHelicopterYawError = 0;
    }

    /**
     * Main update, called every tick from ControlSystem.
     *
     * @param aircraft       The controlled aircraft.
     * @param yawDelta       Raw mouse yaw delta (from rider.getYawDelta).
     * @param pitchDelta     Raw mouse pitch delta (from rider.getPitchDelta).
     * @param keyboardYaw    True if keyboard yaw keys are pressed (overrides autopilot yaw).
     * @param keyboardPitch  True if keyboard pitch keys are pressed (overrides autopilot pitch).
     * @param keyboardRoll   True if keyboard roll keys are pressed (overrides autopilot roll).
     */
    public static void update(EntityVehicleF_Physics aircraft, float yawDelta, float pitchDelta,
                              boolean keyboardYaw, boolean keyboardPitch, boolean keyboardRoll) {
        if (!isMouseFlightActive) {
            return;
        }

        // 1. Update aim angles from mouse input.  In first-person, those deltas have already
        // been applied to the normal rider view, so use that view directly as the flight aim.
        prevAimYaw = aimYaw;
        prevAimPitch = aimPitch;
        if (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON) {
            RotationMatrix viewOrientation = InterfaceManager.clientInterface.getClientPlayer().getOrientation();
            aimYaw = viewOrientation.angles.y;
            aimPitch = viewOrientation.angles.x;
        } else {
            aimYaw += yawDelta;
            aimPitch += pitchDelta;
            aimPitch = clamp(aimPitch, -89, 89);
        }

        // 2. Save previous camera for interpolation.
        prevCamYaw = camYaw;
        prevCamPitch = camPitch;

        // 3. Smooth camera toward aim using exponential damping.
        double dt = 0.05; // 20 TPS
        double factor = 1.0 - Math.exp(-CAM_SMOOTH_SPEED * dt);
        camYaw += shortestAngleDelta(camYaw, aimYaw) * factor;
        camPitch += (aimPitch - camPitch) * factor;

        // 4. Rebuild orientation matrices from angles.
        rebuildOrientations();

        // 5. Run autopilot (skipping axes overridden by keyboard).
        if (isHelicopter) {
            runHelicopterInstructor(aircraft, keyboardYaw, keyboardPitch, keyboardRoll);
        } else {
            runAutopilot(aircraft, keyboardYaw, keyboardPitch, keyboardRoll);
        }
    }

    /**
     * Writes the interpolated aim-forward unit vector into {@code store} for smooth rendering.
     * Interpolates between the previous and current tick aim angles using {@code partialTicks}.
     */
    public static void getInterpolatedAimForward(Point3D store, double partialTicks) {
        double interpYaw   = prevAimYaw   + shortestAngleDelta(prevAimYaw,   aimYaw)   * partialTicks;
        double interpPitch = prevAimPitch + (aimPitch - prevAimPitch) * partialTicks;
        aimOrientation.angles.set(interpPitch, interpYaw, 0);
        aimOrientation.updateToAngles();
        store.set(0, 0, 1).rotate(aimOrientation);
    }

    /**
     * Gets the interpolated camera orientation for rendering.
     */
    public static void getInterpolatedCameraOrientation(RotationMatrix store, double partialTicks) {
        double interpYaw = prevCamYaw + shortestAngleDelta(prevCamYaw, camYaw) * partialTicks;
        double interpPitch = prevCamPitch + (camPitch - prevCamPitch) * partialTicks;
        store.angles.set(interpPitch, interpYaw, 0);
        store.updateToAngles();
    }

    /**
     * Gets the interpolated aim offset relative to the mouse-flight camera, in pitch/yaw degrees.
     */
    public static void getInterpolatedAimCameraOffset(Point3D store, double partialTicks) {
        double interpAimYaw = prevAimYaw + shortestAngleDelta(prevAimYaw, aimYaw) * partialTicks;
        double interpAimPitch = prevAimPitch + (aimPitch - prevAimPitch) * partialTicks;
        double interpCamYaw = prevCamYaw + shortestAngleDelta(prevCamYaw, camYaw) * partialTicks;
        double interpCamPitch = prevCamPitch + (camPitch - prevCamPitch) * partialTicks;
        store.set(interpAimPitch - interpCamPitch, shortestAngleDelta(interpCamYaw, interpAimYaw), 0);
    }

    /**
     * Returns the aim yaw angle.
     */
    public static double getAimYaw() {
        return aimYaw;
    }

    /**
     * Returns the aim pitch angle.
     */
    public static double getAimPitch() {
        return aimPitch;
    }

    /**
     * Returns the camera yaw angle.
     */
    public static double getCamYaw() {
        return camYaw;
    }

    /**
     * Returns the camera pitch angle.
     */
    public static double getCamPitch() {
        return camPitch;
    }

    /**
     * Returns the previous camera yaw for interpolation.
     */
    public static double getPrevCamYaw() {
        return prevCamYaw;
    }

    /**
     * Returns the previous camera pitch for interpolation.
     */
    public static double getPrevCamPitch() {
        return prevCamPitch;
    }

    /**
     * Runs the fixed-wing autopilot: converts aim direction into control surface deflections.
     * Compensates for airflow and trim before applying aim corrections, and banks
     * to bring the flight path around while damping rotation near the target.
     * Axes with active keyboard override are skipped to let keyboard take full control.
     */
    private static void runAutopilot(EntityVehicleF_Physics aircraft,
                                     boolean keyboardYaw, boolean keyboardPitch, boolean keyboardRoll) {
        // Transform aim direction into aircraft local space.
        tempLocal.set(mouseAimForward);
        aircraft.orientation.reOrigin(tempLocal);

        // Use angular errors so a target behind the aircraft still commands a turn.
        double planarAimLength = Math.hypot(tempLocal.x, tempLocal.z);
        double pitchError = Math.toDegrees(Math.atan2(tempLocal.y, planarAimLength));
        double yawError = Math.toDegrees(Math.atan2(tempLocal.x, tempLocal.z));
        if (tempLocal.z < 0 && Math.abs(tempLocal.x) < 0.02D && Math.abs(lastAirplaneYawError) > 90D) {
            yawError = Math.copySign(180D, lastAirplaneYawError);
        }
        lastAirplaneYawError = yawError;
        // Let the bank turn the flight path on large heading changes.  Trying to
        // yaw the nose all the way around first would put the aircraft broadside
        // to its velocity and remove the airflow needed by every control surface.
        tempVelocity.set(aircraft.motion).normalize();
        aircraft.orientation.reOrigin(tempVelocity);
        if (tempVelocity.z > 0.1D) {
            double sideslipAngle = Math.toDegrees(Math.atan2(tempVelocity.x, tempVelocity.z));
            yawError = clamp(yawError, sideslipAngle - MAX_SIDESLIP_ANGLE, sideslipAngle + MAX_SIDESLIP_ANGLE);
        }
        yawError *= clamp(planarAimLength / 0.05D, 0, 1);
        // Follow a moving cursor at its angular rate, rather than requiring a
        // permanent attitude error to keep turning while tracking a moving target.
        double aimYawRadians = Math.toRadians(aimYaw);
        double aimPitchChange = aimPitch - prevAimPitch;
        aimRotationRate.set(aimPitchChange * Math.cos(aimYawRadians), shortestAngleDelta(prevAimYaw, aimYaw),
                -aimPitchChange * Math.sin(aimYawRadians));
        aimRotationRate.addScaled(mouseAimForward, -aimRotationRate.dotProduct(mouseAimForward, false));
        aircraft.orientation.reOrigin(aimRotationRate);
        double rateBlend = clamp(tempLocal.z, 0, 1);
        double targetYawRate = clampFinite(aimRotationRate.y, -AIRPLANE_MAX_AIM_RATE, AIRPLANE_MAX_AIM_RATE) * rateBlend;
        double targetPitchRate = clampFinite(aimRotationRate.x, -AIRPLANE_MAX_AIM_RATE, AIRPLANE_MAX_AIM_RATE) * rateBlend;
        double yawRate = clamp(targetYawRate + yawError * AIRPLANE_AIM_RESPONSE
                - (clampFinite(aircraft.rotation.angles.y, -5D, 5D) - targetYawRate) * AIRPLANE_RATE_DAMPING,
                -AIRPLANE_MAX_AIM_RATE, AIRPLANE_MAX_AIM_RATE);
        double pitchRate = clamp(targetPitchRate - pitchError * AIRPLANE_AIM_RESPONSE
                - (clampFinite(aircraft.rotation.angles.x, -5D, 5D) - targetPitchRate) * AIRPLANE_RATE_DAMPING,
                -AIRPLANE_MAX_AIM_RATE, AIRPLANE_MAX_AIM_RATE);

        // Bank from flight-path error, not just nose error.  The nose can reach the
        // cursor before inertia has brought the velocity around to the same heading.
        tempVelocity.set(aircraft.motion).normalize();
        double horizontalVelocityLength = Math.hypot(tempVelocity.x, tempVelocity.z);
        if (horizontalVelocityLength < 0.1D) {
            tempVelocity.set(aircraft.headingVector);
        }
        double horizontalAimLength = Math.hypot(mouseAimForward.x, mouseAimForward.z);
        double headingError = Math.toDegrees(Math.atan2(tempVelocity.z * mouseAimForward.x - tempVelocity.x * mouseAimForward.z,
                tempVelocity.x * mouseAimForward.x + tempVelocity.z * mouseAimForward.z));
        double targetBank = clamp(-headingError * BANK_PER_HEADING_ERROR, -MAX_BANK_ANGLE, MAX_BANK_ANGLE)
                * clamp(horizontalAimLength / 0.1D, 0, 1);
        tempRight.set(1, 0, 0).rotate(aircraft.orientation);
        tempUp.set(0, 1, 0).rotate(aircraft.orientation);
        double currentBank = Math.toDegrees(Math.atan2(tempRight.y, tempUp.y));
        double horizontalForwardLength = Math.hypot(aircraft.headingVector.x, aircraft.headingVector.z);
        double bankError = horizontalForwardLength < 0.1D ? 0 : shortestAngleDelta(currentBank, targetBank);
        double rollRate = clamp(bankError * AIRPLANE_ROLL_RESPONSE - clampFinite(aircraft.rotation.angles.z, -5D, 5D) * AIRPLANE_RATE_DAMPING,
                -AIRPLANE_MAX_ROLL_RATE, AIRPLANE_MAX_ROLL_RATE);

        // A zero stick is not a zero aerodynamic moment when angle of attack,
        // sideslip, or trim is present.  Correct about the actual neutral controls
        // so those moments cannot balance the instructor at a persistent aim error.
        aircraft.getNeutralControlSurfaceAngles(neutralControls);
        aircraft.getControlSurfaceRotationRates(controlSurfaceRates);

        // Clamp to control surface limits.
        double maxAil = EntityVehicleF_Physics.MAX_AILERON_ANGLE;
        double maxElev = EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE;
        double maxRud = EntityVehicleF_Physics.MAX_RUDDER_ANGLE;

        // Only send autopilot commands for axes NOT overridden by keyboard.
        if (!keyboardRoll) {
            double aileronValue = getControlSurfaceInput(rollRate, neutralControls.z, controlSurfaceRates.z, maxAil);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.aileronInputVar, aileronValue));
        }
        if (!keyboardPitch) {
            double elevatorValue = getControlSurfaceInput(pitchRate, neutralControls.x, controlSurfaceRates.x, maxElev);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.elevatorInputVar, elevatorValue));
        }
        if (!keyboardYaw) {
            double rudderValue = getControlSurfaceInput(yawRate, neutralControls.y, controlSurfaceRates.y, maxRud);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.rudderInputVar, rudderValue));
        }
        InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(aircraft, InterfaceManager.clientInterface.getClientPlayer()));
    }

    /**
     * Runs the helicopter mouse-aim instructor.  The cursor is a direction command,
     * not a direct cyclic input: pitch and pedals turn the nose toward the command,
     * while roll is held level in a hover and blended into a coordinated bank as
     * forward speed increases.  Collective remains on the normal throttle controls.
     */
    private static void runHelicopterInstructor(EntityVehicleF_Physics aircraft,
                                                boolean keyboardYaw, boolean keyboardPitch, boolean keyboardRoll) {
        tempLocal.set(mouseAimForward);
        aircraft.orientation.reOrigin(tempLocal);

        double planarAimLength = Math.hypot(tempLocal.x, tempLocal.z);
        double pitchError = removeDeadZone(Math.toDegrees(Math.atan2(tempLocal.y, planarAimLength)), HELI_AIM_DEAD_ZONE);
        double rawYawError = Math.toDegrees(Math.atan2(tempLocal.x, tempLocal.z));
        if (tempLocal.z < 0 && Math.abs(tempLocal.x) < 0.02D && Math.abs(lastHelicopterYawError) > 90D) {
            rawYawError = Math.copySign(180D, lastHelicopterYawError);
        }
        lastHelicopterYawError = rawYawError;
        double yawError = removeDeadZone(rawYawError, HELI_AIM_DEAD_ZONE) * clamp(planarAimLength / HELI_VERTICAL_AIM_BLEND, 0, 1);

        // Bank from the horizontal heading error rather than local aim X.  This
        // prevents an existing pitch or bank angle from feeding back into the turn.
        double horizontalAimLength = Math.hypot(mouseAimForward.x, mouseAimForward.z);
        double horizontalForwardLength = Math.hypot(aircraft.headingVector.x, aircraft.headingVector.z);
        double headingError = 0;
        if (horizontalAimLength > 0.0001D && horizontalForwardLength > 0.0001D) {
            double headingCross = aircraft.headingVector.z * mouseAimForward.x - aircraft.headingVector.x * mouseAimForward.z;
            double headingDot = aircraft.headingVector.x * mouseAimForward.x + aircraft.headingVector.z * mouseAimForward.z;
            headingError = removeDeadZone(Math.toDegrees(Math.atan2(headingCross, headingDot)), HELI_BANK_DEAD_ZONE)
                    * clamp(horizontalAimLength / HELI_VERTICAL_AIM_BLEND, 0, 1);
        }

        // At low speed pedals do nearly all of the turn.  In forward flight the
        // instructor progressively banks into the turn, up to the WT roll-hold limit.
        // speedFactor converts the internal motion into observed blocks per second.
        double forwardSpeed = Math.max(0, aircraft.motion.dotProduct(aircraft.headingVector, false) * aircraft.speedFactor * 20D);
        double bankBlend = smoothStep(clamp((forwardSpeed - HELI_BANK_START_SPEED) / (HELI_BANK_FULL_SPEED - HELI_BANK_START_SPEED), 0, 1));
        double targetBank = clamp(-headingError * HELI_BANK_PER_YAW_ERROR * bankBlend, -HELI_MAX_BANK_ANGLE, HELI_MAX_BANK_ANGLE);

        // atan2(right.y, up.y) yields bank independently of yaw.  Near vertical
        // pitch the bank axis is ill-conditioned, so only damp the current roll rate.
        tempRight.set(1, 0, 0).rotate(aircraft.orientation);
        tempUp.set(0, 1, 0).rotate(aircraft.orientation);
        double currentBank = Math.toDegrees(Math.atan2(tempRight.y, tempUp.y));
        double bankError = horizontalForwardLength < 0.1D ? 0
                : removeDeadZone(shortestAngleDelta(currentBank, targetBank), HELI_BANK_DEAD_ZONE);

        double pitchAuthorityFactor = getAuthorityFactor(aircraft.elevatorAreaVar.currentValue);
        double yawAuthorityFactor = getAuthorityFactor(aircraft.rudderAreaVar.currentValue);
        double rollAuthorityFactor = getAuthorityFactor(aircraft.aileronAreaVar.currentValue);

        double pitchRateLimit = Math.min(HELI_MAX_PITCH_RATE, 5D * pitchAuthorityFactor);
        double yawRateLimit = Math.min(HELI_MAX_YAW_RATE, 5D * yawAuthorityFactor);
        double rollRateLimit = Math.min(HELI_MAX_ROLL_RATE, 5D * rollAuthorityFactor);
        double pitchRate = clamp(-pitchError * HELI_PITCH_RESPONSE - clampFinite(aircraft.rotation.angles.x, -4D, 4D) * HELI_RATE_DAMPING,
                -pitchRateLimit, pitchRateLimit);
        double yawResponse = HELI_YAW_RESPONSE_HOVER + (HELI_YAW_RESPONSE_FORWARD - HELI_YAW_RESPONSE_HOVER) * bankBlend;
        double yawRate = clamp(yawError * yawResponse - clampFinite(aircraft.rotation.angles.y, -5D, 5D) * HELI_RATE_DAMPING,
                -yawRateLimit, yawRateLimit);
        double rollRate = clamp(bankError * HELI_ROLL_RESPONSE - clampFinite(aircraft.rotation.angles.z, -4D, 4D) * HELI_RATE_DAMPING,
                -rollRateLimit, rollRateLimit);

        double maxAil = EntityVehicleF_Physics.MAX_AILERON_ANGLE;
        double maxElev = EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE;
        double maxRud = EntityVehicleF_Physics.MAX_RUDDER_ANGLE;

        if (!keyboardPitch) {
            double elevatorValue = -pitchRate * maxElev / (5D * pitchAuthorityFactor);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.elevatorInputVar, clamp(elevatorValue, -maxElev, maxElev)));
        }
        if (!keyboardYaw) {
            double rudderValue = -yawRate * maxRud / (5D * yawAuthorityFactor);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.rudderInputVar, clamp(rudderValue, -maxRud, maxRud)));
        }
        if (!keyboardRoll) {
            double aileronValue = rollRate * maxAil / (5D * rollAuthorityFactor);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.aileronInputVar, clamp(aileronValue, -maxAil, maxAil)));
        }
        InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(aircraft, InterfaceManager.clientInterface.getClientPlayer()));
    }

    /**
     * Rebuilds orientation matrices from the current yaw/pitch angles.
     */
    private static void rebuildOrientations() {
        aimOrientation.angles.set(aimPitch, aimYaw, 0);
        aimOrientation.updateToAngles();
        mouseAimForward.set(0, 0, 1).rotate(aimOrientation);

        camOrientation.angles.set(camPitch, camYaw, 0);
        camOrientation.updateToAngles();

        prevCamOrientation.angles.set(prevCamPitch, prevCamYaw, 0);
        prevCamOrientation.updateToAngles();
    }

    /**
     * Returns the shortest angle delta from 'from' to 'to', handling wraparound.
     */
    private static double shortestAngleDelta(double from, double to) {
        double delta = to - from;
        while (delta > 180) delta -= 360;
        while (delta < -180) delta += 360;
        return delta;
    }

    private static double getControlSurfaceInput(double rate, double neutral, double response, double limit) {
        double correction = Double.isFinite(response) && Math.abs(response) > 0.000001D ? rate / response : 0;
        return clamp(neutral + correction, -limit, limit);
    }

    private static double removeDeadZone(double value, double deadZone) {
        if (value > deadZone) {
            return value - deadZone;
        } else if (value < -deadZone) {
            return value + deadZone;
        } else {
            return 0;
        }
    }

    private static double smoothStep(double value) {
        return value * value * (3D - 2D * value);
    }

    private static double getAuthorityFactor(double area) {
        return Double.isFinite(area) ? Math.max(0.1D, 1D + area) : 1D;
    }

    private static double clampFinite(double value, double min, double max) {
        return Double.isFinite(value) ? clamp(value, min, max) : 0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

}
