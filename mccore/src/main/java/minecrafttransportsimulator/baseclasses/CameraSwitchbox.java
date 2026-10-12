package minecrafttransportsimulator.baseclasses;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.rendering.DurationDelayClock;

/**Applies camera rotations while keeping rider-driven inputs within their animation limits.**/
public class CameraSwitchbox extends AnimationSwitchbox {
    public final boolean hasRotationAnimations;
    private AEntityB_Existing riderProvider;
    private AEntityB_Existing cameraRider;
    private boolean rotated;
    private boolean inputClamped;

    public CameraSwitchbox(AEntityD_Definable<?> entity, List<JSONAnimationDefinition> animations) {
        super(entity, animations, null);
        boolean hasRotation = false;
        for (JSONAnimationDefinition animation : animations) {
            if (animation.animationType == AnimationComponentType.ROTATION && animation.axis != null && animation.axis.length() > 0) {
                hasRotation = true;
                break;
            }
        }
        hasRotationAnimations = hasRotation;
    }

    /**Evaluates current camera angles and reports whether an enabled rotation was applied.**/
    public boolean updateRider(AEntityB_Existing riderProvider) {
        this.cameraRider = riderProvider;
        this.riderProvider = riderProvider;
        rotated = false;
        inputClamped = false;
        double inputYaw = riderProvider.riderCameraInputOrientation.angles.y;
        double inputPitch = riderProvider.riderCameraInputOrientation.angles.x;
        try {
            boolean enabled = super.runSwitchbox(1, true);
            if (inputClamped) {
                riderProvider.riderCameraInputOrientation.updateToAngles();
                //Reevaluate earlier rotations when multiple animations use the same rider input.
                this.riderProvider = null;
                rotated = false;
                enabled = super.runSwitchbox(1, true);
            }
            boolean applied = enabled && rotated;
            if (!applied) {
                riderProvider.riderCameraInputOrientation.angles.y = inputYaw;
                riderProvider.riderCameraInputOrientation.angles.x = inputPitch;
                riderProvider.riderCameraInputOrientation.updateToAngles();
            }
            return applied;
        } finally {
            this.riderProvider = null;
        }
    }

    @Override
    protected double getAnimatedVariableValue(DurationDelayClock clock, double scaleFactor, float partialTicks) {
        if (cameraRider != null && cameraRider.isRiderCameraInputActive && cameraRider.activeCameraSwitchbox == this) {
            ComputedVariable variable = entity.getOrCreateVariable(clock.animation.variable);
            if (variable.entity == cameraRider) {
                double currentInput;
                double previousInput;
                if (variable.variableKey.equals("seat_rider_yaw")) {
                    currentInput = cameraRider.riderCameraInputOrientation.angles.y;
                    previousInput = cameraRider.prevRiderCameraInputOrientation.angles.y;
                } else if (variable.variableKey.equals("seat_rider_pitch")) {
                    currentInput = cameraRider.riderCameraInputOrientation.angles.x;
                    previousInput = cameraRider.prevRiderCameraInputOrientation.angles.x;
                } else {
                    return super.getAnimatedVariableValue(clock, scaleFactor, partialTicks);
                }
                double input = partialTicks != 0 ? previousInput + (currentInput - previousInput) * partialTicks : currentInput;
                return clock.clampAndScale(entity, input, scaleFactor, 0, partialTicks);
            }
        }
        return super.getAnimatedVariableValue(clock, scaleFactor, partialTicks);
    }

    @Override
    public void runRotation(DurationDelayClock clock, float partialTicks) {
        rotated |= clock.animationAxisMagnitude > 0;
        if (riderProvider != null) {
            ComputedVariable variable = entity.getOrCreateVariable(clock.animation.variable);
            //Factored rotations use rider input as a timed state command rather than an angle.
            if (variable.entity == riderProvider && clock.animationAxisMagnitude > 0 && !clock.isUseful) {
                if (variable.variableKey.equals("seat_rider_yaw")) {
                    riderProvider.riderCameraInputOrientation.angles.y = clampRiderInput(clock, riderProvider.riderCameraInputOrientation.angles.y);
                } else if (variable.variableKey.equals("seat_rider_pitch")) {
                    riderProvider.riderCameraInputOrientation.angles.x = clampRiderInput(clock, riderProvider.riderCameraInputOrientation.angles.x);
                }
            }
        }
        super.runRotation(clock, partialTicks);
    }

    private double clampRiderInput(DurationDelayClock clock, double input) {
        JSONAnimationDefinition animation = clock.animation;
        double value = (animation.absolute ? Math.abs(input) : input) * clock.animationAxisMagnitude + animation.offset;
        double clampedValue = value;
        if (animation.clampMin != 0 && value < animation.clampMin) {
            clampedValue = animation.clampMin;
        } else if (animation.clampMax != 0 && value > animation.clampMax) {
            clampedValue = animation.clampMax;
        }
        if (clampedValue != value) {
            double clampedInput = (clampedValue - animation.offset) / clock.animationAxisMagnitude;
            if (animation.absolute) {
                clampedInput = Math.copySign(Math.max(0, clampedInput), input);
            }
            inputClamped |= clampedInput != input;
            return clampedInput;
        }
        return input;
    }
}
