package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;

/**Class designed for maintaining the state of a duration/delay for an animation.
 * This is used both in model animations, and part movements.
 *
 * @author don_bruce
 */
public class DurationDelayClock{
	public final JSONAnimationDefinition definition;
	private Long timeCommandedForwards = 0L;
	private Long timeCommandedReverse = 0L;
	private boolean playedForwardsStartSound = false;
	private boolean playedForwardsEndSound = false;
	private boolean playedReverseStartSound = false;
	private boolean playedReverseEndSound = false;
	
	public DurationDelayClock(JSONAnimationDefinition definition){
		this.definition = definition;
	}
	
	/**
	 *  Returns the actual 0-1 value for a state-based duration/delay variable.
	 *  Optionally plays sounds if the state changes appropriately.
	 */
	public double getFactoredState(IAnimationProvider provider, double value){
		boolean commandForwards = value == 1;
		long currentTime = System.currentTimeMillis();
		long cycleTime = definition.duration*50 + definition.forwardsDelay*50 + definition.reverseDelay*50;
		
		//If we don't have an existing command, just set ourselves to the end of our command path.
		if(timeCommandedForwards == 0 && timeCommandedReverse == 0){
			if(commandForwards){
				timeCommandedForwards = currentTime - cycleTime;
			}else{
				timeCommandedReverse = currentTime - cycleTime;
			}
			playedForwardsStartSound = true;
			playedForwardsEndSound = true;
			playedReverseStartSound = true;
			playedReverseEndSound = true;
		}else if(timeCommandedForwards != 0){
			if(!commandForwards){
				//Going forwards, need to reverse.
				timeCommandedReverse = currentTime;
				long timeForwards = currentTime - timeCommandedForwards;
				if(timeForwards < cycleTime){
					//Didn't make it to the end of the cycle.  Adjust start time to compensate.
					timeCommandedReverse += timeForwards - cycleTime;
				}
				if(timeForwards >= definition.duration*50 + definition.forwardsDelay*50){
					//Made it to the end of travel, so need to play sound when we start travel back up.
					playedReverseStartSound = false;
				}
				playedReverseEndSound = false;
				timeCommandedForwards = 0L;
			}
		}else{
			if(commandForwards){
				//Going in reverse, need to go forwards.
				timeCommandedForwards = currentTime;
				long timeReverse = currentTime - timeCommandedReverse;
				if(timeReverse < cycleTime){
					//Didn't make it to the end of the cycle.  Adjust start time to compensate.
					timeCommandedForwards += timeReverse - cycleTime;
				}else{
					//Made it to the end of travel, so need to play sound when we start travel back up.
					playedForwardsStartSound = false;
				}
				playedForwardsEndSound = false;
				timeCommandedReverse = 0L;
			}
		}
		
		double movementFactor = 0;
		if(commandForwards){
			long timedelayed = currentTime - timeCommandedForwards;
			if(timedelayed >= definition.forwardsDelay*50){
				long timeMoved = currentTime - (timeCommandedForwards + definition.forwardsDelay*50);
				if(timeMoved < definition.duration*50){
					movementFactor = timeMoved/(double)(definition.duration*50);
				}else{
					movementFactor = 1;
					if(!playedForwardsEndSound && provider.getProviderWorld().isClient()){
						InterfaceSound.playQuickSound(new SoundInstance(provider, definition.forwardsEndSound));
						playedForwardsEndSound = true;
					}
				}
				if(!playedForwardsStartSound && provider.getProviderWorld().isClient()){
					InterfaceSound.playQuickSound(new SoundInstance(provider, definition.forwardsStartSound));
					playedForwardsStartSound = true;
				}
			}
		}else{
			long timedelayed = currentTime - timeCommandedReverse;
			if(timedelayed >= definition.reverseDelay*50){
				long timeMoved = currentTime - (timeCommandedReverse + definition.reverseDelay*50);
				if(timeMoved < definition.duration*50){
					movementFactor = timeMoved/(double)(definition.duration*50);
				}else{
					movementFactor = 1;
					if(!playedReverseEndSound && provider.getProviderWorld().isClient()){
						InterfaceSound.playQuickSound(new SoundInstance(provider, definition.reverseEndSound));
						playedReverseEndSound = true;
					}
				}
				if(!playedReverseStartSound && provider.getProviderWorld().isClient()){
					InterfaceSound.playQuickSound(new SoundInstance(provider, definition.reverseStartSound));
					playedReverseStartSound = true;
				}
			}
			movementFactor = 1 - movementFactor;
		}
		
		return movementFactor;
	}
}
