package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;

/**Class designed for maintaining the state of a duration/delay for an animation.
 * This is used anything that queries animation states.
 *
 * @author don_bruce
 */
public class DurationDelayClock{
	public final JSONAnimationDefinition animation;
	public boolean movedThisUpdate;
	private Long timeCommandedForwards = 0L;
	private Long timeCommandedReverse = 0L;
	private boolean startedForwardsMovement = false;
	private boolean endedForwardsMovement = false;
	private boolean startedReverseMovement = false;
	private boolean endedReverseMovement = false;
	
	public DurationDelayClock(JSONAnimationDefinition animation){
		this.animation = animation;
	}
	
	/**
	 *  Returns true if this clock should be queried for {@link #getFactoredState(AEntityC_Definable, double)}.
	 *  This can be false to bypass factored states that won't affect the variable value.  
	 */
	public boolean isUseful(){
		return animation.duration != 0 || animation.forwardsDelay != 0 || animation.reverseDelay != 0 || animation.forwardsStartSound != null || animation.forwardsEndSound != null || animation.reverseStartSound != null || animation.reverseEndSound != null; 
	}
	
	/**
	 *  Returns the actual 0-1 value for a state-based duration/delay variable.
	 *  Optionally plays sounds if the state changes appropriately.
	 */
	public double getFactoredState(AEntityC_Definable<?> entity, double value){
		boolean commandForwards = value == 1;
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
				}
				if(timeForwards >= forwardsCycleTime - animation.reverseDelay*50){
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
					movementFactor = timeMoved/(double)(animation.duration*50);
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
					movementFactor = timeMoved/(double)(animation.duration*50);
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
		
		return movementFactor;
	}
}
