package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlNotification;

/**
 * Mouse-based flight controller inspired by War Thunder style controls.
 * The mouse controls an invisible aim reticle. An autopilot steers the
 * aircraft toward that reticle. The camera smoothly follows the aim
 * direction, decoupled from the aircraft orientation.
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
    private static double firstPersonCursorYawOffset = 0;
    private static double firstPersonCursorPitchOffset = 0;

    /** Aim direction as a unit vector in world space. */
    public static final Point3D mouseAimForward = new Point3D(0, 0, 1);

    /** How quickly the camera follows the aim (higher = faster, 0-1 range per tick). */
    private static final double CAM_SMOOTH_SPEED = 5.0;

    /** Autopilot proportional gain for control surfaces (fixed-wing). */
    private static final double AUTOPILOT_GAIN = 2.0;

    /** Autopilot proportional gain for helicopter controls (lower to prevent oscillation). */
    private static final double HELI_GAIN = 0.6;

    /** Angular error that should command full fixed-wing control authority. */
    private static final double FIXED_WING_RESPONSE_ANGLE = 60.0;

    /** Angular error that should command full helicopter control authority. */
    private static final double HELI_RESPONSE_ANGLE = 60.0;

    /** How quickly the first-person mouse-flight cursor recenters on the camera view. */
    private static final double FIRST_PERSON_CURSOR_RECENTER_SPEED = 4.0;

    /** Maximum first-person cursor offsets from the camera center. */
    private static final double FIRST_PERSON_CURSOR_MAX_YAW = 45.0;
    private static final double FIRST_PERSON_CURSOR_MAX_PITCH = 35.0;

    /** Angle threshold for blending between banking turn and wings-level. */
    private static final double AGGRESSIVE_TURN_ANGLE = 10.0;

    // Orientation matrices built from angles for camera use.
    private static final RotationMatrix aimOrientation = new RotationMatrix();
    private static final RotationMatrix camOrientation = new RotationMatrix();
    private static final RotationMatrix prevCamOrientation = new RotationMatrix();

    // Temp vectors for autopilot calculations.
    private static final Point3D tempLocal = new Point3D();
    private static final Point3D tempRight = new Point3D();

    /**
     * Activates mouse flight. Initializes aim to match aircraft heading.
     */
    public static void activate(EntityVehicleF_Physics aircraft, boolean hasRotors) {
        isMouseFlightActive = true;
        isHelicopter = hasRotors;
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
        firstPersonCursorYawOffset = 0;
        firstPersonCursorPitchOffset = 0;
        rebuildOrientations();
    }

    /**
     * Deactivates mouse flight.
     */
    public static void deactivate() {
        isMouseFlightActive = false;
        isHelicopter = false;
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
     * @param firstPersonAimForward If non-null, the first-person camera look direction to use as the aim direction.
     */
    public static void update(EntityVehicleF_Physics aircraft, float yawDelta, float pitchDelta,
                              boolean keyboardYaw, boolean keyboardPitch, boolean keyboardRoll, Point3D firstPersonAimForward) {
        if (!isMouseFlightActive) {
            return;
        }

        // 1. Update aim angles from mouse input.
        prevAimYaw = aimYaw;
        prevAimPitch = aimPitch;
        if (firstPersonAimForward != null) {
            aimOrientation.setToVector(firstPersonAimForward, true);
            double cameraYaw = getNearestAngle(aimYaw, aimOrientation.angles.y);
            double cameraPitch = aimOrientation.angles.x;
            firstPersonCursorYawOffset = clamp(firstPersonCursorYawOffset + yawDelta, -FIRST_PERSON_CURSOR_MAX_YAW, FIRST_PERSON_CURSOR_MAX_YAW);
            firstPersonCursorPitchOffset = clamp(firstPersonCursorPitchOffset + pitchDelta, -FIRST_PERSON_CURSOR_MAX_PITCH, FIRST_PERSON_CURSOR_MAX_PITCH);
            aimYaw = cameraYaw + firstPersonCursorYawOffset;
            aimPitch = clamp(cameraPitch + firstPersonCursorPitchOffset, -89, 89);
            double recenterFactor = Math.exp(-FIRST_PERSON_CURSOR_RECENTER_SPEED * 0.05);
            firstPersonCursorYawOffset *= recenterFactor;
            firstPersonCursorPitchOffset *= recenterFactor;
        } else {
            firstPersonCursorYawOffset = 0;
            firstPersonCursorPitchOffset = 0;
            aimYaw += yawDelta;
            aimPitch += pitchDelta;
            aimPitch = clamp(aimPitch, -89, 89);
        }

        // 2. Save previous camera for interpolation.
        prevCamYaw = camYaw;
        prevCamPitch = camPitch;

        // 3. Smooth camera toward aim using exponential damping.
        if (firstPersonAimForward != null) {
            aimOrientation.setToVector(firstPersonAimForward, true);
            camYaw = getNearestAngle(camYaw, aimOrientation.angles.y);
            camPitch = aimOrientation.angles.x;
        } else {
            double dt = 0.05; // 20 TPS
            double factor = 1.0 - Math.exp(-CAM_SMOOTH_SPEED * dt);
            camYaw += shortestAngleDelta(camYaw, aimYaw) * factor;
            camPitch += (aimPitch - camPitch) * factor;
        }

        // 4. Rebuild orientation matrices from angles.
        rebuildOrientations();

        // 5. Run autopilot (skipping axes overridden by keyboard).
        if (isHelicopter) {
            runHelicopterAutopilot(aircraft, keyboardYaw, keyboardPitch, keyboardRoll);
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
     * Uses banking turns for large heading changes and wings-level for small corrections.
     * Axes with active keyboard override are skipped to let keyboard take full control.
     */
    private static void runAutopilot(EntityVehicleF_Physics aircraft,
                                     boolean keyboardYaw, boolean keyboardPitch, boolean keyboardRoll) {
        // Transform aim direction into aircraft local space.
        tempLocal.set(mouseAimForward);
        aircraft.orientation.reOrigin(tempLocal);

        double headingError = getHeadingError(tempLocal);
        double pitchError = getPitchError(tempLocal);
        double headingInput = clamp(headingError / FIXED_WING_RESPONSE_ANGLE * AUTOPILOT_GAIN, -1.0, 1.0);

        // Rudder: positive rudder = yaw right.
        double yawInput = headingInput;

        // Elevator: positive elevator = pitch up. Add pull-through for large horizontal turns.
        double pitchInput = clamp(pitchError / FIXED_WING_RESPONSE_ANGLE * AUTOPILOT_GAIN, -1.0, 1.0);
        double turnPullInput = clamp((Math.abs(headingError) - AGGRESSIVE_TURN_ANGLE) / (180.0 - AGGRESSIVE_TURN_ANGLE), 0.0, 1.0);
        if (turnPullInput > Math.abs(pitchInput)) {
            pitchInput = (Math.abs(pitchError) > 1.0 ? Math.signum(pitchError) : 1.0) * turnPullInput;
        }

        // Calculate angle off target for roll blending.
        double dot = clamp(tempLocal.z, -1.0, 1.0);
        double angleOffTarget = Math.toDegrees(Math.acos(dot));

        // Roll: bank into the target direction.
        double aggressiveRoll = headingInput;

        // Wings level: counter current bank using aircraft's right vector world Y.
        // Negative because positive tempRight.y (banked right) needs negative aileron (roll left).
        tempRight.set(1, 0, 0).rotate(aircraft.orientation);
        double wingsLevelRoll = -tempRight.y * AUTOPILOT_GAIN;

        double rollInput;
        if (angleOffTarget > AGGRESSIVE_TURN_ANGLE) {
            rollInput = aggressiveRoll;
        } else {
            double blend = angleOffTarget / AGGRESSIVE_TURN_ANGLE;
            rollInput = wingsLevelRoll * (1.0 - blend) + aggressiveRoll * blend;
        }

        // Clamp to control surface limits.
        double maxAil = EntityVehicleF_Physics.MAX_AILERON_ANGLE;
        double maxElev = EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE;
        double maxRud = EntityVehicleF_Physics.MAX_RUDDER_ANGLE;

        // Only send autopilot commands for axes NOT overridden by keyboard.
        if (!keyboardRoll) {
            double aileronValue = clamp(rollInput * maxAil, -maxAil, maxAil);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.aileronInputVar, aileronValue));
        }
        if (!keyboardPitch) {
            double elevatorValue = clamp(pitchInput * maxElev, -maxElev, maxElev);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.elevatorInputVar, elevatorValue));
        }
        if (!keyboardYaw) {
            double rudderValue = clamp(yawInput * maxRud, -maxRud, maxRud);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.rudderInputVar, rudderValue));
        }
        InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(aircraft, InterfaceManager.clientInterface.getClientPlayer()));
    }

    /**
     * Runs the helicopter autopilot: uses direct pitch/yaw control without banking turns.
     * <p>
     * Helicopter control model:
     * - Yaw (rudder): turn the helicopter to face the aim heading
     * - Pitch (elevator): tilt forward/backward based on aim pitch relative to helicopter pitch
     * - Roll (aileron): auto-level to keep the helicopter upright
     */
    private static void runHelicopterAutopilot(EntityVehicleF_Physics aircraft,
                                               boolean keyboardYaw, boolean keyboardPitch, boolean keyboardRoll) {
        // Transform aim direction into aircraft local space (same as fixed-wing).
        tempLocal.set(mouseAimForward);
        aircraft.orientation.reOrigin(tempLocal);

        double headingError = getHeadingError(tempLocal);
        double pitchError = getPitchError(tempLocal);

        // Yaw: turn helicopter toward aim direction. Same sign convention as fixed-wing.
        double yawInput = clamp(headingError / HELI_RESPONSE_ANGLE * HELI_GAIN, -1.0, 1.0);

        // Pitch: tilt forward/backward toward aim. Same sign convention as fixed-wing.
        double pitchInput = clamp(pitchError / HELI_RESPONSE_ANGLE * HELI_GAIN, -1.0, 1.0);

        // Roll: auto-level. Counter the current bank using aircraft's right vector world Y.
        tempRight.set(1, 0, 0).rotate(aircraft.orientation);
        double rollInput = -tempRight.y * HELI_GAIN;

        // Clamp to control surface limits.
        double maxAil = EntityVehicleF_Physics.MAX_AILERON_ANGLE;
        double maxElev = EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE;
        double maxRud = EntityVehicleF_Physics.MAX_RUDDER_ANGLE;

        // Only send autopilot commands for axes NOT overridden by keyboard.
        if (!keyboardRoll) {
            double aileronValue = clamp(rollInput * maxAil, -maxAil, maxAil);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.aileronInputVar, aileronValue));
        }
        if (!keyboardPitch) {
            double elevatorValue = clamp(pitchInput * maxElev, -maxElev, maxElev);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.elevatorInputVar, elevatorValue));
        }
        if (!keyboardYaw) {
            double rudderValue = clamp(yawInput * maxRud, -maxRud, maxRud);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.rudderInputVar, rudderValue));
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

    private static double getNearestAngle(double reference, double angle) {
        return reference + shortestAngleDelta(reference, angle);
    }

    private static double getHeadingError(Point3D localAim) {
        return Math.toDegrees(Math.atan2(-localAim.x, localAim.z));
    }

    private static double getPitchError(Point3D localAim) {
        double horizontalDistance = Math.sqrt(localAim.x * localAim.x + localAim.z * localAim.z);
        return Math.toDegrees(Math.atan2(localAim.y, Math.max(horizontalDistance, 0.001)));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
