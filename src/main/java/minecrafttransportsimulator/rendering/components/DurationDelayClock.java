package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;

/**Class designed for maintaining the state of a duration/delay for an animation.
 * This is used anything that queries animation states.
 *
 * @author don_bruce
 */
public class DurationDelayClock{
	public final JSONAnimationDefinition animation;
	public final boolean isUseful;
	public boolean movedThisUpdate;
	private Long timeCommandedForwards = 0L;
	private Long timeCommandedReverse = 0L;
	
	private final boolean shouldDoFactoring;
	private boolean startedForwardsMovement = false;
	private boolean endedForwardsMovement = false;
	private boolean startedReverseMovement = false;
	private boolean endedReverseMovement = false;
	
	public DurationDelayClock(JSONAnimationDefinition animation){
		this.animation = animation;
		this.shouldDoFactoring = animation.duration != 0 || animation.forwardsDelay != 0 || animation.reverseDelay != 0;
		this.isUseful = shouldDoFactoring || animation.animationType.equals(AnimationComponentType.VISIBILITY)  || animation.animationType.equals(AnimationComponentType.INHIBITOR)  || animation.animationType.equals(AnimationComponentType.ACTIVATOR) || animation.forwardsStartSound != null || animation.forwardsEndSound != null || animation.reverseStartSound != null || animation.reverseEndSound != null;
	}
	
	/**
	 *  Returns the actual 0-1 value for a state-based duration/delay variable.
	 *  Optionally plays sounds if the state changes appropriately.
	 */
	public double getFactoredState(AEntityC_Definable<?> entity, double value){
		boolean commandForwards = value > 0;
		long currentTime = System.currentTimeMillis();
		long forwardsCycleTime = animation.forwardsDelay*50;
		if(!animation.skipForwardsMovement){
			forwardsCycleTime += animation.duration*50 + animation.reverseDelay*50;
		}
		long reverseCycleTime = animation.reverseDelay*50;
		if(!animation.skipReverseMovement){
			reverseCycleTime += animation.duration*50 + animation.forwardsDelay*50;
		}
		movedThisUpdate = false;
		
		//If we don't have an existing command, just set ourselves to the end of our command path.
		if(timeCommandedForwards == 0 && timeCommandedReverse == 0){
			if(commandForwards){
				timeCommandedForwards = currentTime - forwardsCycleTime;
			}else{
				timeCommandedReverse = currentTime - reverseCycleTime;
			}
			startedForwardsMovement = true;
			endedForwardsMovement = true;
			startedReverseMovement = true;
			endedReverseMovement = true;
		}else if(timeCommandedForwards != 0){
			if(!commandForwards){
				//Going forwards, need to reverse.
				timeCommandedReverse = currentTime;
				long timeForwards = currentTime - timeCommandedForwards;
				if(timeForwards < forwardsCycleTime){
					//Didn't make it to the end of the cycle.  Adjust start time to compensate.
					timeCommandedReverse += timeForwards - forwardsCycleTime;
				}else{
					//Made it to the end of travel, so we aren't in the reversing process.
					startedReverseMovement = false;
				}
				endedReverseMovement = false;
				timeCommandedForwards = 0L;
			}
		}else{
			if(commandForwards){
				//Going in reverse, need to go forwards.
				timeCommandedForwards = currentTime;
				long timeReverse = currentTime - timeCommandedReverse;
				if(timeReverse < reverseCycleTime){
					//Didn't make it to the end of the cycle.  Adjust start time to compensate.
					timeCommandedForwards += timeReverse - reverseCycleTime;
				}else{
					//Made it to the end of travel, so we aren't in the forwards process.
					startedForwardsMovement = false;
				}
				endedForwardsMovement = false;
				timeCommandedReverse = 0L;
			}
		}
		
		double movementFactor = 0;
		if(commandForwards){
			long timedelayed = currentTime - timeCommandedForwards;
			if(timedelayed >= animation.forwardsDelay*50){
				long timeMoved = currentTime - (timeCommandedForwards + animation.forwardsDelay*50);
				if(timeMoved < animation.duration*50 && !animation.skipForwardsMovement){
					movedThisUpdate = true;
					movementFactor =  getEasingValue(timeMoved, false);
					
					
				}else{
					movementFactor = 1;
					if(!endedForwardsMovement){
						endedForwardsMovement = true;
						movedThisUpdate = true;
						if(animation.forwardsEndSound != null && entity.world.isClient()){
							InterfaceSound.playQuickSound(new SoundInstance(entity, animation.forwardsEndSound));
						}
					}
				}
				if(!startedForwardsMovement){
					startedForwardsMovement = true;
					if(animation.forwardsStartSound != null && entity.world.isClient()){
						InterfaceSound.playQuickSound(new SoundInstance(entity, animation.forwardsStartSound));
					}
				}
			}
		}else{
			long timedelayed = currentTime - timeCommandedReverse;
			if(timedelayed >= animation.reverseDelay*50){
				long timeMoved = currentTime - (timeCommandedReverse + animation.reverseDelay*50);
				if(timeMoved < animation.duration*50 && !animation.skipReverseMovement){
					movedThisUpdate = true;
					movementFactor =  getEasingValue(timeMoved, true);
				}else{
					movementFactor = 1;
					if(!endedReverseMovement){
						endedReverseMovement = true;
						movedThisUpdate = true;
						if(animation.reverseEndSound != null && entity.world.isClient()){
							InterfaceSound.playQuickSound(new SoundInstance(entity, animation.reverseEndSound));
						}
					}
				}
				if(!startedReverseMovement){
					startedReverseMovement = true;
					if(animation.reverseStartSound != null && entity.world.isClient()){
						InterfaceSound.playQuickSound(new SoundInstance(entity, animation.reverseStartSound));
					}
				}
			}
			movementFactor = 1 - movementFactor;
		}
		
		return shouldDoFactoring ? movementFactor : value;
	}
	
	/**
	 * Returns the interpolated animation values
	 * It calls the interpolation methods equivalent to the easing type
	 */
	public double getEasingValue(long timeMoved, boolean isReverse){
		//If the animation is in reverse, and has reverse easing, use that.
		//If it's in forwards, and has forwards easing, use that.
		//Otherwise, just use the linear value.
		double time = timeMoved/(double)(animation.duration*50);
		if(isReverse && animation.reverseEasing != null){
			return DurationDelayEasing.getEasingType(animation.reverseEasing, time);
			
		}else if(!isReverse && animation.forwardsEasing != null){
			return DurationDelayEasing.getEasingType(animation.forwardsEasing, time);
		}else{
			return time;
		}
	}	
}
