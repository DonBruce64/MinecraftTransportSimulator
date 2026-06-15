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

    // Smoothed helicopter cyclic commands to avoid tick-to-tick attitude jitter.
    private static double heliElevatorCommand = 0;
    private static double heliAileronCommand = 0;

    /** Aim direction as a unit vector in world space. */
    public static final Point3D mouseAimForward = new Point3D(0, 0, 1);

    /** How quickly the camera follows the aim (higher = faster, 0-1 range per tick). */
    private static final double CAM_SMOOTH_SPEED = 5.0;

    /** Autopilot proportional gain for control surfaces (fixed-wing). */
    private static final double AUTOPILOT_GAIN = 2.0;

    /** Rudder gain for helicopter arcade yaw, in control angle per degree of yaw error. */
    private static final double HELI_YAW_GAIN = 0.45;

    /** Maximum nose-down or nose-up pitch angle for helicopter arcade attitude control. */
    private static final double HELI_MAX_PITCH_ANGLE = 35.0;

    /** Nose-down attitude applied when the aim point is forward of the helicopter. */
    private static final double HELI_FORWARD_PITCH_ANGLE = 22.0;

    /** Maximum left or right bank angle for helicopter arcade attitude control. */
    private static final double HELI_MAX_ROLL_ANGLE = 35.0;

    /** Attitude correction gain for helicopter arcade controls when heli auto-level is disabled. */
    private static final double HELI_ATTITUDE_GAIN = 0.65;

    /** Rotation-rate damping gain for helicopter arcade controls when heli auto-level is disabled. */
    private static final double HELI_RATE_DAMPING_GAIN = 0.7;

    /** Maximum cyclic input when helicopter auto-level is disabled. */
    private static final double HELI_MAX_RATE_INPUT = 18.0;

    /** Attitude error ignored to avoid small oscillations around the target. */
    private static final double HELI_ATTITUDE_DEADBAND = 0.75;

    /** How quickly helicopter cyclic commands move toward their targets each tick. */
    private static final double HELI_COMMAND_RESPONSE = 0.18;

    /** Angle threshold for blending between banking turn and wings-level. */
    private static final double AGGRESSIVE_TURN_ANGLE = 10.0;

    // Orientation matrices built from angles for camera use.
    private static final RotationMatrix aimOrientation = new RotationMatrix();
    private static final RotationMatrix camOrientation = new RotationMatrix();
    private static final RotationMatrix prevCamOrientation = new RotationMatrix();
    private static final RotationMatrix lineOfSightOrientation = new RotationMatrix();

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
        heliElevatorCommand = aircraft.elevatorInputVar.currentValue;
        heliAileronCommand = aircraft.aileronInputVar.currentValue;
        rebuildOrientations();
    }

    /**
     * Deactivates mouse flight.
     */
    public static void deactivate() {
        isMouseFlightActive = false;
        isHelicopter = false;
        heliElevatorCommand = 0;
        heliAileronCommand = 0;
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

        // 1. Update aim angles from mouse input.
        prevAimYaw = aimYaw;
        prevAimPitch = aimPitch;
        aimYaw += yawDelta;
        aimPitch += pitchDelta;
        aimPitch = clamp(aimPitch, -89, 89);

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

    public static boolean shouldUseCameraLineOfSight() {
        return isMouseFlightActive && InterfaceManager.clientInterface != null && InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON;
    }

    public static Point3D getCameraLineOfSight(Point3D store, double distance, double partialTicks) {
        getInterpolatedCameraOrientation(lineOfSightOrientation, partialTicks);
        return store.set(0, 0, distance).rotate(lineOfSightOrientation);
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
     * Uses banking turns for large heading changes and wings-level for small corrections.
     * Axes with active keyboard override are skipped to let keyboard take full control.
     */
    private static void runAutopilot(EntityVehicleF_Physics aircraft,
                                     boolean keyboardYaw, boolean keyboardPitch, boolean keyboardRoll) {
        // Transform aim direction into aircraft local space.
        tempLocal.set(mouseAimForward);
        aircraft.orientation.reOrigin(tempLocal);

        // Rudder: positive rudder = yaw right. Aim right = negative local X.
        double yawInput = -tempLocal.x * AUTOPILOT_GAIN;

        // Elevator: positive elevator = pitch up. Aim above = positive local Y.
        double pitchInput = tempLocal.y * AUTOPILOT_GAIN;

        // Calculate angle off target for roll blending.
        double dot = clamp(tempLocal.z, -1.0, 1.0);
        double angleOffTarget = Math.toDegrees(Math.acos(dot));

        // Roll: bank into the target direction.
        double aggressiveRoll = -tempLocal.x * AUTOPILOT_GAIN;

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
     * Runs the helicopter autopilot: converts the aim direction into stable attitude targets.
     * <p>
     * Helicopter control model:
     * - Yaw (rudder): turn the helicopter to face the aim heading
     * - Pitch (elevator): hold a bounded nose-up/down attitude from forward and vertical aim error
     * - Roll (aileron): hold a bounded bank attitude toward lateral aim error
     */
    private static void runHelicopterAutopilot(EntityVehicleF_Physics aircraft,
                                               boolean keyboardYaw, boolean keyboardPitch, boolean keyboardRoll) {
        // Transform aim direction into aircraft local space.
        tempLocal.set(mouseAimForward);
        aircraft.orientation.reOrigin(tempLocal);

        // Clamp to control surface limits.
        double maxAil = EntityVehicleF_Physics.MAX_AILERON_ANGLE;
        double maxElev = EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE;
        double maxRud = EntityVehicleF_Physics.MAX_RUDDER_ANGLE;

        // Yaw: use angular error so aiming behind the helicopter still produces a turn command.
        // Positive rudder yaws right; local X is negative when the aim is to the right.
        double yawError = Math.toDegrees(Math.atan2(tempLocal.x, tempLocal.z));
        double rudderValue = clamp(-yawError * HELI_YAW_GAIN, -maxRud, maxRud);

        // Positive vehicle pitch is nose-down, while positive elevator input commands nose-up.
        // A centered forward aim needs some nose-down cyclic to actually translate forward.
        double forwardTilt = Math.max(0, tempLocal.z) * HELI_FORWARD_PITCH_ANGLE;
        double verticalTilt = -tempLocal.y * HELI_MAX_PITCH_ANGLE;
        double targetPitch = clamp(forwardTilt + verticalTilt, -HELI_MAX_PITCH_ANGLE, HELI_MAX_PITCH_ANGLE);

        // Positive roll banks right; local X is negative when the aim is to the right.
        double targetRoll = clamp(-tempLocal.x * HELI_MAX_ROLL_ANGLE, -HELI_MAX_ROLL_ANGLE, HELI_MAX_ROLL_ANGLE);

        double elevatorValue;
        double aileronValue;
        if (aircraft.autolevelEnabledVar.isActive) {
            // Auto-level mode already interprets cyclic input as target pitch/roll attitudes.
            elevatorValue = clamp(-targetPitch, -maxElev, maxElev);
            aileronValue = clamp(targetRoll, -maxAil, maxAil);
        } else {
            // Without auto-level, cyclic input is a rotation-rate command.  Close the attitude
            // loop here and damp the current rotation to avoid continuous spin.
            double pitchError = applyDeadband(targetPitch - aircraft.orientation.angles.x, HELI_ATTITUDE_DEADBAND);
            double rollError = applyDeadband(targetRoll - aircraft.orientation.angles.z, HELI_ATTITUDE_DEADBAND);
            elevatorValue = clamp(-(pitchError * HELI_ATTITUDE_GAIN - aircraft.rotation.angles.x * HELI_RATE_DAMPING_GAIN), -HELI_MAX_RATE_INPUT, HELI_MAX_RATE_INPUT);
            aileronValue = clamp(rollError * HELI_ATTITUDE_GAIN - aircraft.rotation.angles.z * HELI_RATE_DAMPING_GAIN, -HELI_MAX_RATE_INPUT, HELI_MAX_RATE_INPUT);
        }

        // Only send autopilot commands for axes NOT overridden by keyboard.
        if (!keyboardRoll) {
            aileronValue = smoothCommand(heliAileronCommand, aileronValue);
            heliAileronCommand = aileronValue;
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.aileronInputVar, aileronValue));
        } else {
            heliAileronCommand = aircraft.aileronInputVar.currentValue;
        }
        if (!keyboardPitch) {
            elevatorValue = smoothCommand(heliElevatorCommand, elevatorValue);
            heliElevatorCommand = elevatorValue;
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.elevatorInputVar, elevatorValue));
        } else {
            heliElevatorCommand = aircraft.elevatorInputVar.currentValue;
        }
        if (!keyboardYaw) {
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double applyDeadband(double value, double deadband) {
        if (Math.abs(value) <= deadband) {
            return 0;
        }
        return value > 0 ? value - deadband : value + deadband;
    }

    private static double smoothCommand(double currentValue, double targetValue) {
        return currentValue + (targetValue - currentValue) * HELI_COMMAND_RESPONSE;
    }
}
