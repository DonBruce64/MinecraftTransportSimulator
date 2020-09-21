package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.Map;

import mcinterface.InterfaceAudio;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Class that when extended allows for {@link RenderableModelObject}s.
 *
 * @author don_bruce
 */
public abstract class ATransformRenderable{
	public final VehicleAnimationDefinition definition; 
	private final Map<EntityVehicleF_Physics, Boolean> playedForwardsSounds = new HashMap<EntityVehicleF_Physics, Boolean>();
	private final Map<EntityVehicleF_Physics, Boolean> playedReverseSounds = new HashMap<EntityVehicleF_Physics, Boolean>();
	private final Map<EntityVehicleF_Physics, Long> timesCommandedForwards = new HashMap<EntityVehicleF_Physics, Long>();
	private final Map<EntityVehicleF_Physics, Long> timesCommandedReverse = new HashMap<EntityVehicleF_Physics, Long>();
	
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
			boolean playedForwardsSound = playedForwardsSounds.containsKey(vehicle) ? playedForwardsSounds.get(vehicle) : false;
			boolean playedReverseSound = playedReverseSounds.containsKey(vehicle) ? playedReverseSounds.get(vehicle) : false;
			long timeCommandedForwards = timesCommandedForwards.containsKey(vehicle) ? timesCommandedForwards.get(vehicle) : 0;
			long timeCommandedReverse = timesCommandedReverse.containsKey(vehicle) ? timesCommandedReverse.get(vehicle) : 0;
			
			//If we don't have an existing command, just set ourselves to the end of our command path.
			if(timeCommandedForwards == 0 && timeCommandedReverse == 0){
				if(commandForwards){
					timeCommandedForwards = currentTime - cycleTime;
				}else{
					timeCommandedReverse = currentTime - cycleTime;
				}
				playedForwardsSound = true;
				playedReverseSound = true;
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
						playedReverseSound = false;
					}
					
					timeCommandedForwards = 0;
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
						playedForwardsSound = false;
					}
					
					timeCommandedReverse = 0;
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
					}
					if(!playedForwardsSound){
						InterfaceAudio.playQuickSound(new SoundInstance(vehicle, definition.forwardsSound));
						playedForwardsSound = true;
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
					}
					if(!playedReverseSound){
						InterfaceAudio.playQuickSound(new SoundInstance(vehicle, definition.reverseSound));
						playedReverseSound = true;
					}
				}
				movementFactor = 1 - movementFactor;
			}
			
			timesCommandedForwards.put(vehicle, timeCommandedForwards);
			timesCommandedReverse.put(vehicle, timeCommandedReverse);
			playedForwardsSounds.put(vehicle, playedForwardsSound);
			playedReverseSounds.put(vehicle, playedReverseSound);
			return movementFactor;
		}
	}
}
