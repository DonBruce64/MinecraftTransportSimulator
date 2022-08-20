package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.sound.SoundInstance;

/**
 * Class designed for maintaining the state of a duration/delay for an animation.
 * This is used anything that queries animation states.
 * This also contains a method for calculating easing equations and returning the interpolated values
 * This is used for interpolating animation values with non-linear equations.
 *
 * @author don_bruce, TurboDefender
 */
public class DurationDelayClock {
    private static final double c1 = 1.70518;
    private static final double c2 = c1 * 1.525;
    private static final double c3 = c1 + 1;
    private static final double c4 = (2 * Math.PI) / 3;
    private static final double c5 = (2 * Math.PI) / 4.5;
    private static final double n1 = 7.5625;
    private static final double d1 = 2.75;

    public final JSONAnimationDefinition animation;
    public final double animationAxisMagnitude;
    public final Point3D animationAxisNormalized;
    public final boolean isUseful;
    public boolean movedThisUpdate;
    private Long timeCommandedForwards = 0L;
    private Long timeCommandedReverse = 0L;

    private final boolean shouldDoFactoring;
    private boolean startedForwardsMovement = false;
    private boolean endedForwardsMovement = false;
    private boolean startedReverseMovement = false;
    private boolean endedReverseMovement = false;

    public DurationDelayClock(JSONAnimationDefinition animation) {
        this.animation = animation;
        this.animationAxisMagnitude = animation.axis != null ? animation.axis.length() : 1.0;
        this.animationAxisNormalized = animation.axis != null ? animation.axis.copy().normalize() : null;
        this.shouldDoFactoring = animation.duration != 0 || animation.forwardsDelay != 0 || animation.reverseDelay != 0;
        this.isUseful = shouldDoFactoring || animation.animationType.equals(AnimationComponentType.VISIBILITY) || animation.animationType.equals(AnimationComponentType.INHIBITOR) || animation.animationType.equals(AnimationComponentType.ACTIVATOR) || animation.forwardsStartSound != null || animation.forwardsEndSound != null || animation.reverseStartSound != null || animation.reverseEndSound != null;
    }

    /**
     * Returns the actual 0-1 value for a state-based duration/delay variable.
     * Optionally plays sounds if the state changes appropriately.
     */
    public double getFactoredState(AEntityD_Definable<?> entity, double value, float partialTicks) {
        boolean commandForwards = value > 0;
        //We do all time here in milliseconds, not ticks.  This allows for partial ticks.
        long currentTime = (long) ((entity.ticksExisted + partialTicks) * 50D);
        long forwardsCycleTime = animation.forwardsDelay * 50L;
        if (!animation.skipForwardsMovement) {
            forwardsCycleTime += animation.duration * 50L + animation.reverseDelay * 50L;
        }
        long reverseCycleTime = animation.reverseDelay * 50L;
        if (!animation.skipReverseMovement) {
            reverseCycleTime += animation.duration * 50L + animation.forwardsDelay * 50L;
        }
        movedThisUpdate = false;

        //If we don't have an existing command, just set ourselves to the end of our command path.
        if (timeCommandedForwards == 0 && timeCommandedReverse == 0) {
            if (commandForwards) {
                timeCommandedForwards = currentTime - forwardsCycleTime;
            } else {
                timeCommandedReverse = currentTime - reverseCycleTime;
            }
            startedForwardsMovement = true;
            endedForwardsMovement = true;
            startedReverseMovement = true;
            endedReverseMovement = true;
        } else if (timeCommandedForwards != 0) {
            if (!commandForwards) {
                //Going forwards, need to reverse.
                timeCommandedReverse = currentTime;
                long timeForwards = currentTime - timeCommandedForwards;
                if (timeForwards < forwardsCycleTime) {
                    //Didn't make it to the end of the cycle.  Adjust start time to compensate.
                    timeCommandedReverse += timeForwards - forwardsCycleTime;
                } else {
                    //Made it to the end of travel, so we aren't in the reversing process.
                    startedReverseMovement = false;
                }
                endedReverseMovement = false;
                timeCommandedForwards = 0L;
            }
        } else {
            if (commandForwards) {
                //Going in reverse, need to go forwards.
                timeCommandedForwards = currentTime;
                long timeReverse = currentTime - timeCommandedReverse;
                if (timeReverse < reverseCycleTime) {
                    //Didn't make it to the end of the cycle.  Adjust start time to compensate.
                    timeCommandedForwards += timeReverse - reverseCycleTime;
                } else {
                    //Made it to the end of travel, so we aren't in the forwards process.
                    startedForwardsMovement = false;
                }
                endedForwardsMovement = false;
                timeCommandedReverse = 0L;
            }
        }

        double movementFactor = 0;
        if (commandForwards) {
            long timedelayed = currentTime - timeCommandedForwards;
            if (timedelayed >= animation.forwardsDelay * 50L) {
                long timeMoved = currentTime - (timeCommandedForwards + animation.forwardsDelay * 50L);
                if (timeMoved < animation.duration * 50L && !animation.skipForwardsMovement) {
                    movedThisUpdate = true;
                    movementFactor = timeMoved / (double) (animation.duration * 50);
                    if (animation.forwardsEasing != null) {
                        movementFactor = getEasingType(animation.forwardsEasing, movementFactor);
                    }
                } else {
                    movementFactor = 1;
                    if (!endedForwardsMovement) {
                        endedForwardsMovement = true;
                        movedThisUpdate = true;
                        if (animation.forwardsEndSound != null && entity.world.isClient()) {
                            InterfaceManager.soundInterface.playQuickSound(new SoundInstance(entity, animation.forwardsEndSound));
                        }
                    }
                }
                if (!startedForwardsMovement) {
                    startedForwardsMovement = true;
                    if (animation.forwardsStartSound != null && entity.world.isClient()) {
                        InterfaceManager.soundInterface.playQuickSound(new SoundInstance(entity, animation.forwardsStartSound));
                    }
                }
            }
        } else {
            long timedelayed = currentTime - timeCommandedReverse;
            if (timedelayed >= animation.reverseDelay * 50L) {
                long timeMoved = currentTime - (timeCommandedReverse + animation.reverseDelay * 50L);
                if (timeMoved < animation.duration * 50L && !animation.skipReverseMovement) {
                    movedThisUpdate = true;
                    movementFactor = timeMoved / (double) (animation.duration * 50);
                    if (animation.reverseEasing != null) {
                        movementFactor = getEasingType(animation.reverseEasing, movementFactor);
                    }
                } else {
                    movementFactor = 1;
                    if (!endedReverseMovement) {
                        endedReverseMovement = true;
                        movedThisUpdate = true;
                        if (animation.reverseEndSound != null && entity.world.isClient()) {
                            InterfaceManager.soundInterface.playQuickSound(new SoundInstance(entity, animation.reverseEndSound));
                        }
                    }
                }
                if (!startedReverseMovement) {
                    startedReverseMovement = true;
                    if (animation.reverseStartSound != null && entity.world.isClient()) {
                        InterfaceManager.soundInterface.playQuickSound(new SoundInstance(entity, animation.reverseStartSound));
                    }
                }
            }
            movementFactor = 1 - movementFactor;
        }

        return shouldDoFactoring ? movementFactor : value;
    }

    /**
     * This is used to check the easing type defined in the JSON fields
     * and call the respective easing function to return a value
     *
     * @param direction The JSON field either {@code forwardsEasing} or {@code reverseEasing}.
     * @param time      The time that has elapsed for an animation or the percent complete from 0 to 1.
     */
    private static double getEasingType(JSONAnimationDefinition.AnimationEasingType direction, double time) {
        switch (direction) {
            case LINEAR:
                return time;
            case EASEINSINE:
                return 1 - Math.cos((time * Math.PI) / 2);
            case EASEOUTSINE:
                return Math.sin((time * Math.PI) / 2);
            case EASEINOUTSINE:
                return (-1 * (Math.cos(time * Math.PI) - 1)) / 2;
            case EASEINQUAD:
                return time * time;
            case EASEOUTQUAD:
                return time * (2 - time);
            case EASEINOUTQUAD:
                return time < 0.5 ? 2 * time * time : -1 + (4 - 2 * time) * time;
            case EASEINCUBIC:
                return time * time * time;
            case EASEOUTCUBIC:
                return --time * time * time + 1;
            case EASEINOUTCUBIC:
                return time < 0.5 ? 4 * time * time * time : (time - 1) * (2 * time - 2) * (2 * time - 2) + 1;
            case EASEINQUART:
                return time * time * time * time;
            case EASEOUTQUART:
                return 1 - (--time) * time * time * time;
            case EASEINOUTQUART:
                return time < 0.5 ? 8 * time * time * time * time : 1 - 8 * (--time) * time * time * time;
            case EASEINQUINT:
                return time * time * time * time * time;
            case EASEOUTQUINT:
                return 1 + (--time) * time * time * time * time;
            case EASEINOUTQUINT:
                return time < 0.5 ? 16 * time * time * time * time * time : 1 + 16 * (--time) * time * time * time * time;
            case EASEINCIRC:
                return 1 - Math.sqrt(1 - Math.pow(time, 2));
            case EASEOUTCIRC:
                return Math.sqrt(1 - Math.pow(time - 1, 2));
            case EASEINOUTCIRC:
                return time < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * time, 2))) / 2 : (Math.sqrt(1 - Math.pow(-2 * time + 2, 2)) + 1) / 2;
            case EASEINBACK:
                return c3 * time * time * time - c1 * time * time;
            case EASEOUTBACK:
                return 1 + c3 * Math.pow(time - 1, 3) + c1 * Math.pow(time - 1, 2);
            case EASEINOUTBACK:
                return time < 0.5 ? (Math.pow(2 * time, 2) * ((c2 + 1) * 2 * time - c2)) / 2 : (Math.pow(2 * time - 2, 2) * ((c2 + 1) * (time * 2 - 2) + c2) + 2) / 2;
            case EASEINELASTIC: {
                if (time == 0) {
                    return 0;
                } else if (time == 1) {
                    return 1;
                } else {
                    return -Math.pow(2, 10 * time - 10) * Math.sin((time * 10 - 10.75) * c4);
                }
            }
            case EASEOUTELASTIC: {
                if (time == 0) {
                    return 0;
                } else if (time == 1) {
                    return 1;
                } else {
                    return Math.pow(2, -10 * time) * Math.sin((time * 10 - 0.75) * c4) + 1;
                }
            }
            case EASEINOUTELASTIC: {
                if (time == 0) {
                    return 0;
                } else if (time == 1) {
                    return 1;
                } else if (time < 0.5) {
                    return -(Math.pow(2, 20 * time - 10) * Math.sin((20 * time - 11.125) * c5)) / 2;
                } else {
                    return (Math.pow(2, -20 * time + 10) * Math.sin((20 * time - 11.125) * c5)) / 2 + 1;
                }
            }
            case EASEINBOUNCE:
                return 1 - getEasingType(JSONAnimationDefinition.AnimationEasingType.EASEOUTBOUNCE, 1 - time);
            case EASEOUTBOUNCE: {
                if (time < 1 / d1) {
                    return n1 * time * time;
                } else if (time < 2 / d1) {
                    return n1 * (time -= 1.5 / d1) * time + 0.75;
                } else if (time < 2.5 / d1) {
                    return n1 * (time -= 2.25 / d1) * time + 0.9375;
                } else {
                    return n1 * (time -= 2.625 / d1) * time + 0.984375;
                }
            }
            case EASEINOUTBOUNCE:
                return time < 0.5 ? (1 - getEasingType(JSONAnimationDefinition.AnimationEasingType.EASEOUTBOUNCE, 1 - 2 * time)) / 2 : (1 + getEasingType(JSONAnimationDefinition.AnimationEasingType.EASEINBOUNCE, 2 * time - 1)) / 2;

            //Easing type is invalid. Default to linear.
            default:
                return time;
        }
    }
}
