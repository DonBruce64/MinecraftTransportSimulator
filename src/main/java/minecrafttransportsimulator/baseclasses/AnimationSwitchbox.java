package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;

/**A helper class of sorts for doing switch-based animations for {@link JSONAnimationDefinition}
 * when parired with {@link DurationDelayClock}s.  This wraps the rather long and boilerplate-code
 * switch statement into something a bit more compact.  After the switchbox runs, the various variables
 * will be set in this class, and will stay set until the next call.
 *
 * @author don_bruce
 */
public class AnimationSwitchbox{
	//Public variables.
	public final Point3d animationOffset = new Point3d();
	public Orientation3d animationOrientation = new Orientation3d(animationOffset);
	public double animationScale;
	public boolean anyClockMovedThisUpdate;
	
	//Computational variables.
	private final AEntityD_Definable<?> entity;
	private final List<DurationDelayClock> clocks = new ArrayList<DurationDelayClock>();
	private boolean inhibitAnimations;
	private final Point3d mutablePoint = new Point3d();
	private final Orientation3d mutableOrientation = new Orientation3d(mutablePoint);
	private final List<DurationDelayClock> clocksToRun = new ArrayList<DurationDelayClock>();
	
	public AnimationSwitchbox(AEntityD_Definable<?> entity, List<JSONAnimationDefinition> animations){
		this.entity = entity;
		for(JSONAnimationDefinition animation : animations){
			clocks.add(new DurationDelayClock(animation));
		}
	}
	
	public boolean runSwitchbox(Point3d initialOffset, float partialTicks){
		inhibitAnimations = false;
		anyClockMovedThisUpdate = false;
		if(initialOffset != null){
			animationOffset.setTo(initialOffset);
		}else{
			animationOffset.set(0, 0, 0);
		}
		animationOrientation.setRotation(0);
		animationScale = 1.0;
		clocksToRun.clear();
		for(DurationDelayClock clock : clocks){
			switch(clock.animation.animationType){
				case TRANSLATION :{
					if(!inhibitAnimations){
						clocksToRun.add(0, clock);
					}
					break;
				}
				case ROTATION :{
					if(!inhibitAnimations){
						clocksToRun.add(0, clock);
					}
					break;
				}
				case SCALING :{
					if(!inhibitAnimations){
						animationScale *= entity.getAnimatedVariableValue(clock, clock.animationAxisMagnitude, partialTicks);
					}
					break;
				}
				case VISIBILITY :{
					if(!inhibitAnimations){
						double variableValue = entity.getAnimatedVariableValue(clock, partialTicks);
						if(!anyClockMovedThisUpdate){
							anyClockMovedThisUpdate = clock.movedThisUpdate;
						}
						if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
							return false;
						}
					}
					break;
				}
				case INHIBITOR :{
					if(!inhibitAnimations){
						double variableValue = entity.getAnimatedVariableValue(clock, partialTicks);
						if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
							inhibitAnimations = true;
						}
					}
					break;
				}
				case ACTIVATOR :{
					if(inhibitAnimations){
						double variableValue = entity.getAnimatedVariableValue(clock, partialTicks);
						if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
							inhibitAnimations = false;
						}
					}
					break;
				}
			}
		}
		
		///Run clocks now.  They need to be run in reverse for the proper order.
		for(DurationDelayClock clock : clocksToRun){
			if(clock.animation.animationType.equals(AnimationComponentType.TRANSLATION)){
				runTranslation(clock, partialTicks);
			}else{
				runRotation(clock, partialTicks);
			}
		}
		
		return true;
	}
	
	public void runTranslation(DurationDelayClock clock, float partialTicks){
		//Found translation.  This gets applied in the translation axis direction directly.
		double variableValue = entity.getAnimatedVariableValue(clock, clock.animationAxisMagnitude, partialTicks);
		mutablePoint.setTo(clock.animationAxisNormalized).multiply(variableValue);
		
		//Add this point to the total translation, rotating it by the existing rotation.
		animationOrientation.rotateAndAddTo(mutablePoint, animationOffset);
	}
	
	public void runRotation(DurationDelayClock clock, float partialTicks){
		//Found rotation.  Get angles that needs to be applied.
		double variableValue = entity.getAnimatedVariableValue(clock, clock.animationAxisMagnitude, partialTicks);
		mutableOrientation.setTo(clock.animationAxisNormalized, variableValue);
		
		//Rotate existing point to new orientation, taking into account offset.
		mutableOrientation.rotateWithOffset(animationOffset, clock.animation.centerPoint);
		
		//Now apply orientation changes so orientation faces the proper way.
		animationOrientation.multiplyBy(mutableOrientation);
		
		//Rotate existing point to new orientation, taking into account offset.
		//animationOrientation.multiplyBy(mutableOrientation);
		//mutableOrientation.rotateWithOffset(animationOffset, clock.animation.centerPoint);
		
		//Now apply orientation changes so orientation faces the proper way.
		
	}
}
