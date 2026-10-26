package minecrafttransportsimulator.systems.summoning;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;

/**
 * Flies a summoned aircraft to a point, ignoring its normal flight physics.
 * <p>
 * The server's summon job sets the target in variables on the vehicle, which are synced to clients.
 * This runs on both sides every tick, just before the vehicle is moved, so clients fly the
 * same path instead of fighting server corrections.  It overrides the vehicle's motion and
 * rotation for the tick, so collisions still apply.
 */
public final class VehicleSummonFlight {
    static final String VAR_ACTIVE = "phone_fly";
    static final String VAR_X = "phone_fly_x";
    static final String VAR_Y = "phone_fly_y";
    static final String VAR_Z = "phone_fly_z";
    /**Top speed, in blocks per tick.**/
    static final String VAR_SPEED = "phone_fly_speed";

    /**Fraction of the remaining distance covered each tick, so we ease into the target.**/
    private static final double APPROACH_RATE = 0.08;
    /**Fraction of any pitch or roll levelled out each tick.**/
    private static final double LEVEL_RATE = 0.15;
    /**Fastest the aircraft turns to face where it's going, in degrees per tick.**/
    private static final double MAX_YAW_RATE = 3;

    private VehicleSummonFlight() {
    }

    public static void apply(EntityVehicleF_Physics vehicle) {
        if (!vehicle.getOrCreateVariable(VAR_ACTIVE).isActive) {
            return;
        }
        Point3D toTarget = getTarget(vehicle).subtract(vehicle.position);
        double distance = toTarget.length();
        double speed = Math.min(vehicle.getOrCreateVariable(VAR_SPEED).currentValue, distance * APPROACH_RATE);
        Point3D velocity = distance > 0.01 ? toTarget.scale(speed / distance) : new Point3D();
        vehicle.motion.set(velocity).scale(1D / vehicle.speedFactor);

        //Face where we're going while moving sideways.  Hold heading when climbing or descending.
        double yawDelta = 0;
        if (Math.hypot(velocity.x, velocity.z) > 0.15) {
            double desiredYaw = Math.toDegrees(Math.atan2(velocity.x, velocity.z));
            yawDelta = clampAngle(desiredYaw - vehicle.orientation.angles.y) * 0.1;
            yawDelta = Math.max(-MAX_YAW_RATE, Math.min(MAX_YAW_RATE, yawDelta));
        }
        vehicle.rotation.angles.set(-vehicle.orientation.angles.x * LEVEL_RATE, yawDelta, -vehicle.orientation.angles.z * LEVEL_RATE);
    }

    static Point3D getTarget(EntityVehicleF_Physics vehicle) {
        return new Point3D(vehicle.getOrCreateVariable(VAR_X).currentValue, vehicle.getOrCreateVariable(VAR_Y).currentValue, vehicle.getOrCreateVariable(VAR_Z).currentValue);
    }

    /**Server only: points the aircraft at a new target.**/
    static void flyTo(EntityVehicleF_Physics vehicle, Point3D target, double speed) {
        vehicle.getOrCreateVariable(VAR_X).setTo(target.x, true);
        vehicle.getOrCreateVariable(VAR_Y).setTo(target.y, true);
        vehicle.getOrCreateVariable(VAR_Z).setTo(target.z, true);
        vehicle.getOrCreateVariable(VAR_SPEED).setTo(speed, true);
        vehicle.getOrCreateVariable(VAR_ACTIVE).setTo(1, true);
    }

    /**Server only: hands the aircraft back to its normal physics.**/
    static void stop(EntityVehicleF_Physics vehicle) {
        vehicle.getOrCreateVariable(VAR_ACTIVE).setTo(0, true);
    }

    static double clampAngle(double angle) {
        angle %= 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }
}
