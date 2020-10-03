package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Class that when extended allows for {@link RenderableModelObject}s.
 *
 * @author don_bruce
 */
public abstract class ATransformRenderable{
	public final VehicleAnimationDefinition definition; 
	private final Map<EntityVehicleF_Physics, SoundState> soundStates = new HashMap<EntityVehicleF_Physics, SoundState>();
	
	public ATransformRenderable(VehicleAnimationDefinition definition){
		this.definition = definition;
	}
	
	/**
	 *  This method should return true if this transform should be rendered.
	 *  By default, all transforms are rendered, but this may be overridden should
	 *  the object this transform is on need to be made invisible.
	 */
	public boolean shouldRender(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){
		return true;
	}
	
	/**
	 *  This method applies any transforms this object has prior to rendering.
	 *  Vehicle and part (if this model is on a part) is passed-in for state-based transform logic.
	 *  Passed-in offset may or may not be used depending on the transform.
	 *  Returned offset should be able to be fed to the next transform down the JSON for its use, if required.
	 */
	public abstract double applyTransform(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, double offset);
	
	/**
	 *  This method allows for post-render logic.  This allows for additional transforms,
	 *  or transform clean-up (say if lighting was modified).
	 */
	public void doPostRenderLogic(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){};
	
	/**
	 *  Helper method for calculating definition.duration/definition.delay code for a state-based (0/1) variable.
	 */
	public double getFactoredState(EntityVehicleF_Physics vehicle, double variable){
		if(definition.duration == 0 && definition.forwardsDelay == 0 && definition.reverseDelay == 0){
			return variable;
		}else{
			boolean commandForwards = variable == 1;
			long currentTime = System.currentTimeMillis();
			long cycleTime = definition.duration*50 + definition.forwardsDelay*50 + definition.reverseDelay*50;
			
			//Pull variables out of saved map for use in rendering.
			if(!soundStates.containsKey(vehicle)){
				soundStates.put(vehicle, new SoundState());
			}
			SoundState state = soundStates.get(vehicle);
			
			//If we don't have an existing command, just set ourselves to the end of our command path.
			if(state.timeCommandedForwards == 0 && state.timeCommandedReverse == 0){
				if(commandForwards){
					state.timeCommandedForwards = currentTime - cycleTime;
				}else{
					state.timeCommandedReverse = currentTime - cycleTime;
				}
				state.playedForwardsStartSound = true;
				state.playedForwardsEndSound = true;
				state.playedReverseStartSound = true;
				state.playedReverseEndSound = true;
			}else if(state.timeCommandedForwards != 0){
				if(!commandForwards){
					//Going forwards, need to reverse.
					state.timeCommandedReverse = currentTime;
					long timeForwards = currentTime - state.timeCommandedForwards;
					if(timeForwards < cycleTime){
						//Didn't make it to the end of the cycle.  Adjust start time to compensate.
						state.timeCommandedReverse += timeForwards - cycleTime;
					}
					if(timeForwards >= definition.duration*50 + definition.forwardsDelay*50){
						//Made it to the end of travel, so need to play sound when we start travel back up.
						state.playedReverseStartSound = false;
					}
					state.playedReverseEndSound = false;
					state.timeCommandedForwards = 0L;
				}
			}else{
				if(commandForwards){
					//Going in reverse, need to go forwards.
					state.timeCommandedForwards = currentTime;
					long timeReverse = currentTime - state.timeCommandedReverse;
					if(timeReverse < cycleTime){
						//Didn't make it to the end of the cycle.  Adjust start time to compensate.
						state.timeCommandedForwards += timeReverse - cycleTime;
					}else{
						//Made it to the end of travel, so need to play sound when we start travel back up.
						state.playedForwardsStartSound = false;
					}
					state.playedForwardsEndSound = false;
					state.timeCommandedReverse = 0L;
				}
			}
			
			double movementFactor = 0;
			if(commandForwards){
				long timedelayed = currentTime - state.timeCommandedForwards;
				if(timedelayed >= definition.forwardsDelay*50){
					long timeMoved = currentTime - (state.timeCommandedForwards + definition.forwardsDelay*50);
					if(timeMoved < definition.duration*50){
						movementFactor = timeMoved/(double)(definition.duration*50);
					}else{
						movementFactor = 1;
						if(!state.playedForwardsEndSound){
							MasterLoader.audioInterface.playQuickSound(new SoundInstance(vehicle, definition.forwardsEndSound));
							state.playedForwardsEndSound = true;
						}
					}
					if(!state.playedForwardsStartSound){
						MasterLoader.audioInterface.playQuickSound(new SoundInstance(vehicle, definition.forwardsStartSound));
						state.playedForwardsStartSound = true;
					}
				}
			}else{
				long timedelayed = currentTime - state.timeCommandedReverse;
				if(timedelayed >= definition.reverseDelay*50){
					long timeMoved = currentTime - (state.timeCommandedReverse + definition.reverseDelay*50);
					if(timeMoved < definition.duration*50){
						movementFactor = timeMoved/(double)(definition.duration*50);
					}else{
						movementFactor = 1;
						if(!state.playedReverseEndSound){
							MasterLoader.audioInterface.playQuickSound(new SoundInstance(vehicle, definition.reverseEndSound));
							state.playedReverseEndSound = true;
						}
					}
					if(!state.playedReverseStartSound){
						MasterLoader.audioInterface.playQuickSound(new SoundInstance(vehicle, definition.reverseStartSound));
						state.playedReverseStartSound = true;
					}
				}
				movementFactor = 1 - movementFactor;
			}
			
			return movementFactor;
		}
	}
	
	private static class SoundState{
		private Long timeCommandedForwards = 0L;
		private Long timeCommandedReverse = 0L;
		private boolean playedForwardsStartSound = false;
		private boolean playedForwardsEndSound = false;
		private boolean playedReverseStartSound = false;
		private boolean playedReverseEndSound = false;
	}
}
