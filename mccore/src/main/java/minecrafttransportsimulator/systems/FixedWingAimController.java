package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;

/**
 * Per-aircraft mouse-aim feedback, executed before the control surfaces enter
 * the force solver.  Network latency delays the target rather than the feedback.
 */
public final class FixedWingAimController {
    private static final double AUTOPILOT_GAIN = 2.0;
    private static final double AUTOPILOT_INTEGRAL_GAIN = 0.05;
    private static final double MAX_INTEGRAL_INPUT = 0.4;
    private static final double AGGRESSIVE_TURN_ANGLE = 10.0;
    private static final double MAX_PRECISION_STIFFNESS = 0.1;
    private static final double SETTLING_ANGLE = 3.0;
    private static final double FLIGHT_PATH_CAPTURE_ANGLE = 1.5;
    private static final int FLIGHT_PATH_CAPTURE_TICKS = 3;
    private static final double CAPTURE_TARGET_STEP = 0.15;
    private static final double RELEASE_TARGET_STEP = 0.5;
    private static final double MIN_CAPTURE_SPEED = 8.0; //Observed blocks per second.
    private static final double SETTLING_DAMPING = 0.8;
    private static final double MAX_GUIDANCE_YAW = 45.0;
    private static final double MAX_GUIDANCE_PITCH = 30.0;
    private final Point3D integralInputs = new Point3D();
    private final Point3D dampingTimes = new Point3D();
    private final Point3D controlResponses = new Point3D();
    private final Point3D precisionGains = new Point3D();
    private final Point3D angularRate = new Point3D();
    private final Point3D tempLocal = new Point3D();
    private final Point3D tempRight = new Point3D();
    private final Point3D mouseAimForward = new Point3D();
    private final RotationMatrix aimOrientation = new RotationMatrix();
    private double previousAimYaw;
    private double previousAimPitch;
    private boolean hasPreviousAim;
    private int settledTicks;
    private boolean flightPathAligned;
    private boolean flightPathTrimPending;
    private double settlingDamping;
    private double lastTurnDirection = 1;

    public void reset() {
        integralInputs.set(0, 0, 0);
        hasPreviousAim = false;
        settledTicks = 0;
        flightPathAligned = false;
        flightPathTrimPending = false;
        settlingDamping = 0;
        lastTurnDirection = 1;
    }

    public double getSettlingDamping() {
        return settlingDamping;
    }

    public boolean isFlightPathAligned() {
        return flightPathAligned;
    }

    /**Rebase the learned trim once velocity no longer has an angle to the nose.**/
    public void rebaseFlightPathTrim(EntityVehicleF_Physics aircraft) {
        if (flightPathTrimPending) {
            integralInputs.x = clamp((EntityVehicleF_Physics.ELEVATOR_LIFT_OFFSET - aircraft.elevatorTrimVar.currentValue)
                    / EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, -MAX_INTEGRAL_INPUT, MAX_INTEGRAL_INPUT);
            integralInputs.y = clamp(-aircraft.rudderTrimVar.currentValue / EntityVehicleF_Physics.MAX_RUDDER_ANGLE, -MAX_INTEGRAL_INPUT, MAX_INTEGRAL_INPUT);
            aircraft.elevatorInputVar.setTo(integralInputs.x * EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, false);
            aircraft.rudderInputVar.setTo(integralInputs.y * EntityVehicleF_Physics.MAX_RUDDER_ANGLE, false);
            flightPathTrimPending = false;
        }
    }

    private void updateAssistance(EntityVehicleF_Physics aircraft, double aimYaw, double aimPitch, int overrides, double angleOffTarget) {
        double yawChange = hasPreviousAim ? (aimYaw - previousAimYaw) % 360D : 0;
        if (yawChange > 180D) yawChange -= 360D;
        if (yawChange < -180D) yawChange += 360D;
        double aimChange = Math.hypot(yawChange, hasPreviousAim ? aimPitch - previousAimPitch : 0);
        previousAimYaw = aimYaw;
        previousAimPitch = aimPitch;
        hasPreviousAim = true;
        boolean flying = overrides == 0 && aircraft.groundDeviceCollective.isAirborne()
                && aircraft.motion.length() * aircraft.speedFactor * 20D >= MIN_CAPTURE_SPEED;
        double settleBlend = clamp((SETTLING_ANGLE - angleOffTarget) / (SETTLING_ANGLE - 1D), 0, 1);
        settlingDamping = flying && aimChange <= CAPTURE_TARGET_STEP ? SETTLING_DAMPING * settleBlend : 0;
        if (!flying || angleOffTarget > SETTLING_ANGLE || aimChange > RELEASE_TARGET_STEP) {
            settledTicks = 0;
            flightPathAligned = false;
            flightPathTrimPending = false;
        } else if (!flightPathAligned) {
            settledTicks = angleOffTarget <= FLIGHT_PATH_CAPTURE_ANGLE && aimChange <= CAPTURE_TARGET_STEP ? settledTicks + 1 : 0;
            if (settledTicks >= FLIGHT_PATH_CAPTURE_TICKS) {
                flightPathAligned = true;
                flightPathTrimPending = true;
            }
        }
    }

    public void update(EntityVehicleF_Physics aircraft, double aimYaw, double aimPitch, int overrides) {
        boolean keyboardYaw = (overrides & 1) != 0;
        boolean keyboardPitch = (overrides & 2) != 0;
        boolean keyboardRoll = (overrides & 4) != 0;
        aimOrientation.angles.set(aimPitch, aimYaw, 0);
        aimOrientation.updateToAngles();
        mouseAimForward.set(0, 0, 1).rotate(aimOrientation);
        // Transform aim direction into aircraft local space.
        tempLocal.set(mouseAimForward);
        aircraft.orientation.reOrigin(tempLocal);

        //Use the actual target for capture, but steer through a forward waypoint
        //on large turns.  A target directly behind has no lateral component and
        //must not turn a tiny pitch error into an unintended vertical reversal.
        double angleOffTarget = Math.toDegrees(Math.acos(clamp(tempLocal.z, -1.0, 1.0)));
        updateAssistance(aircraft, aimYaw, aimPitch, overrides, angleOffTarget);
        tempRight.set(0, 0, 1).rotate(aircraft.orientation);
        double headingYaw = Math.toDegrees(Math.atan2(tempRight.x, tempRight.z));
        double headingPitch = -Math.toDegrees(Math.asin(clamp(tempRight.y, -1, 1)));
        double yawError = (aimYaw - headingYaw) % 360D;
        if (yawError > 180D) yawError -= 360D;
        if (yawError < -180D) yawError += 360D;
        if (Math.abs(yawError) > 179.999D) {
            yawError = Math.abs(yawError) * lastTurnDirection;
        } else if (Math.abs(yawError) > 1D) {
            lastTurnDirection = Math.signum(yawError);
        }
        if (Math.abs(yawError) > MAX_GUIDANCE_YAW || Math.abs(aimPitch - headingPitch) > MAX_GUIDANCE_PITCH) {
            aimOrientation.angles.set(headingPitch + clamp(aimPitch - headingPitch, -MAX_GUIDANCE_PITCH, MAX_GUIDANCE_PITCH),
                    headingYaw + clamp(yawError, -MAX_GUIDANCE_YAW, MAX_GUIDANCE_YAW), 0);
            aimOrientation.updateToAngles();
            tempLocal.set(0, 0, 1).rotate(aimOrientation).reOrigin(aircraft.orientation);
        }

        // Rudder: positive rudder = yaw right. Aim right = negative local X.
        double yawInput = -tempLocal.x * AUTOPILOT_GAIN;

        // Elevator: positive elevator = pitch up. Aim above = positive local Y.
        double pitchInput = tempLocal.y * AUTOPILOT_GAIN;

        // Roll: bank into the target direction.
        double aggressiveRoll = -tempLocal.x * AUTOPILOT_GAIN;

        // Wings level: counter current bank using aircraft's right vector world Y.
        // Negative because positive tempRight.y (banked right) needs negative aileron (roll left).
        tempRight.set(1, 0, 0).rotate(aircraft.orientation);
        double wingsLevelRoll = -tempRight.y * AUTOPILOT_GAIN;

        //Counter bank throughout a large turn instead of holding full aileron
        //until capture.  The bounded turn command keeps the wings below knife-edge.
        aggressiveRoll = clamp(aggressiveRoll, -1.5D, 1.5D);
        double blend = Math.min(1D, angleOffTarget / AGGRESSIVE_TURN_ANGLE);
        double rollInput = wingsLevelRoll + aggressiveRoll * blend;

        // A proportional command alone can balance the aircraft's restoring
        // moments before the nose reaches the cursor.  Learn only the small trim
        // correction needed to remove that residual error, retaining the original
        // response for turns.  Never accumulate against the ground or a manual input.
        boolean canIntegrate = aircraft.groundDeviceCollective.isAirborne()
                && aircraft.axialVelocity > 0.25D && angleOffTarget < AGGRESSIVE_TURN_ANGLE;
        if (canIntegrate) {
            aircraft.getControlDampingTime(dampingTimes, controlResponses, precisionGains, AUTOPILOT_GAIN, MAX_PRECISION_STIFFNESS);
            aircraft.getAngularVelocity(angularRate);
        }
        //Learn steady trim after the turn has slowed; integrating a large
        //transient error stores an extra turn command that causes an arrival wobble.
        double pitchTrimError = Math.abs(angularRate.x) < 0.2D || pitchInput * integralInputs.x < 0 ? pitchInput : 0;
        double yawTrimError = Math.abs(angularRate.y) < 0.2D || yawInput * integralInputs.y < 0 ? yawInput : 0;
        double trimLearning = flightPathAligned ? 0.1D : 1D;
        integralInputs.set(updateIntegral(integralInputs.x, pitchTrimError * trimLearning, canIntegrate && !keyboardPitch),
                updateIntegral(integralInputs.y, yawTrimError * trimLearning, canIntegrate && !keyboardYaw),
                updateIntegral(integralInputs.z, rollInput, canIntegrate && !keyboardRoll));
        //Fade the correction out before leaving the precision window, avoiding
        //a sudden change in surface deflection when a turn crosses its boundary.
        double precisionBlend = clamp(2D * (AGGRESSIVE_TURN_ANGLE - angleOffTarget) / AGGRESSIVE_TURN_ANGLE, 0, 1);
        if (canIntegrate) {
            //Limit only the final capture loop's stiffness, not the aircraft's
            //physical rate or the original commands used for larger maneuvers.
            pitchInput *= 1D - precisionBlend * (1D - precisionGains.x);
            yawInput *= 1D - precisionBlend * (1D - precisionGains.y);
            rollInput *= 1D - precisionBlend * (1D - precisionGains.z);
        }
        pitchInput += integralInputs.x * precisionBlend;
        yawInput += integralInputs.y * precisionBlend;
        rollInput += integralInputs.z * precisionBlend;
        if (canIntegrate) {
            pitchInput = dampApproach(pitchInput, -Math.toRadians(angularRate.x) * AUTOPILOT_GAIN, precisionBlend * dampingTimes.x,
                    aircraft.elevatorInputVar.currentValue / EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, controlResponses.x);
            yawInput = dampApproach(yawInput, -Math.toRadians(angularRate.y) * AUTOPILOT_GAIN, precisionBlend * dampingTimes.y,
                    aircraft.rudderInputVar.currentValue / EntityVehicleF_Physics.MAX_RUDDER_ANGLE, controlResponses.y);
            rollInput = dampApproach(rollInput, Math.toRadians(angularRate.z) * AUTOPILOT_GAIN, precisionBlend * dampingTimes.z,
                    aircraft.aileronInputVar.currentValue / EntityVehicleF_Physics.MAX_AILERON_ANGLE, controlResponses.z);
        }

        // Clamp to control surface limits.
        double maxAil = EntityVehicleF_Physics.MAX_AILERON_ANGLE;
        double maxElev = EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE;
        double maxRud = EntityVehicleF_Physics.MAX_RUDDER_ANGLE;

        //Leave axes under manual control untouched.
        if (!keyboardRoll) {
            double aileronValue = clamp(rollInput * maxAil, -maxAil, maxAil);
            aircraft.aileronInputVar.setTo(aileronValue, false);
        }
        if (!keyboardPitch) {
            double elevatorValue = clamp(pitchInput * maxElev, -maxElev, maxElev);
            aircraft.elevatorInputVar.setTo(elevatorValue, false);
        }
        if (!keyboardYaw) {
            double rudderValue = clamp(yawInput * maxRud, -maxRud, maxRud);
            aircraft.rudderInputVar.setTo(rudderValue, false);
        }
    }

    private static double dampApproach(double input, double closingRate, double lead, double currentInput, double response) {
        //Brake the accumulated rotation, including the rate change caused by this
        //new command.  Solving the rate feedback implicitly avoids alternating
        //full deflections when the angular response is faster than the tick rate.
        double feedback = lead * response;
        return Double.isFinite(closingRate) ? (input - closingRate * lead + feedback * currentInput) / (1D + feedback) : input;
    }

    private static double updateIntegral(double correction, double input, boolean enabled) {
        if (!enabled) {
            return 0;
        }
        double updated = clamp(correction + input * AUTOPILOT_INTEGRAL_GAIN, -MAX_INTEGRAL_INPUT, MAX_INTEGRAL_INPUT);
        double output = input + updated;
        //Allow unwinding, but do not build up correction while the surface is saturated.
        return Math.abs(output) <= 1D || output * input < 0 ? updated : correction;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
