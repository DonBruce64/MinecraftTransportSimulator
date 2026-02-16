package minecrafttransportsimulator.baseclasses;

public class PIDController {
    private double kp;
    private double ki;
    private double kd;
    private double prevError;
    private double integral;

    public PIDController(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    public double loop(double error, double step) {
        integral += error * step;
        double derivative = (error - prevError) / step;
        prevError = error;
        return kp * error + ki * integral + kd * derivative;
    }

    public void clear() {
        integral = 0;
        prevError = 0;
    }
}
