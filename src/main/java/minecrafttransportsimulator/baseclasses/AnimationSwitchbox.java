package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;

/**A helper class of sorts for doing switch-based animations for {@link JSONAnimationDefinition}
 * when parired with {@link DurationDelayClock}s.  This wraps the rather long and boilerplate-code
 * switch statement into something a bit more compact.  After the switchbox runs, the various variables
 * will be set in this class, and will stay set until the next call.  Note that in the case of the returned
 * matrix, all operations are done as pre-operations.  So a translation operation followed by a rotate operation
 * is valid, but the internal code here will take the rotation matrix and multiply it by the translation matrix.  Then
 * any further calls will the calling matrix transform multiplied by the net transform. 
 *
 * @author don_bruce
 */
public class AnimationSwitchbox{
	public final TransformationMatrix netMatrix = new TransformationMatrix();
	public final RotationMatrix rotation = new RotationMatrix();
	public final Point3D translation = new Point3D();
	public double scale;
	public boolean anyClockMovedThisUpdate;
	
	//Computational variables.
	protected final AEntityD_Definable<?> entity;
	private final List<DurationDelayClock> clocks = new ArrayList<DurationDelayClock>();
	private final Point3D helperPoint = new Point3D();
	private final Point3D helperScalingVector = new Point3D();
	private final RotationMatrix helperRotationMatrix = new RotationMatrix();
	private final TransformationMatrix helperOffsetOperationMatrix = new TransformationMatrix();
	private boolean inhibitAnimations;
	
	public AnimationSwitchbox(AEntityD_Definable<?> entity, List<JSONAnimationDefinition> animations){
		this.entity = entity;
		for(JSONAnimationDefinition animation : animations){
			clocks.add(new DurationDelayClock(animation));
		}
	}
	
	public boolean runSwitchbox(float partialTicks){
		inhibitAnimations = false;
		anyClockMovedThisUpdate = false;
		scale = 1.0;
		netMatrix.resetTransforms();
		translation.set(0, 0, 0);
		rotation.setToZero();
		
		for(DurationDelayClock clock : clocks){
			switch(clock.animation.animationType){
				case TRANSLATION :{
					if(!inhibitAnimations){
						runTranslation(clock, partialTicks);
					}
					break;
				}
				case ROTATION :{
					if(!inhibitAnimations){
						runRotation(clock, partialTicks);
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
				case SCALING :{
					if(!inhibitAnimations){
						runScaling(clock, partialTicks);
					}
					break;
				}
			}
		}
		return true;
	}
	
	public void runTranslation(DurationDelayClock clock, float partialTicks){
		//Found translation.  This gets applied in the translation axis direction directly.
		double variableValue = entity.getAnimatedVariableValue(clock, clock.animationAxisMagnitude, partialTicks);
		if(variableValue != 0){
			helperPoint.set(clock.animationAxisNormalized);
			helperPoint.scale(variableValue);
			netMatrix.applyTranslation(helperPoint);
			translation.add(helperPoint);
		}
	}
	
	public void runRotation(DurationDelayClock clock, float partialTicks){
		//Found rotation.  Get angles that needs to be applied.
		double variableValue = entity.getAnimatedVariableValue(clock, clock.animationAxisMagnitude, partialTicks);
		if(variableValue != 0){
			helperRotationMatrix.setAxisAngleRotation(clock.animationAxisNormalized, variableValue);
			
			//If we have a center offset, do special translation code to handle it.
			//Otherwise, don't bother, as it'll just take cycles.
			if(clock.animation.centerPoint.x != 0 || clock.animation.centerPoint.y != 0 || clock.animation.centerPoint.z != 0){
				//First translate to the center point.
				helperPoint.set(clock.animation.centerPoint);
				helperOffsetOperationMatrix.resetTransforms();
				helperOffsetOperationMatrix.setTranslation(helperPoint);
				
				//Now do rotation.
				helperOffsetOperationMatrix.applyRotation(helperRotationMatrix);
				
				//Translate back.  This requires inverting the translation.
				helperPoint.invert();
				helperOffsetOperationMatrix.applyTranslation(helperPoint);
				
				//Apply that net value to our main matrix.
				netMatrix.multiply(helperOffsetOperationMatrix);
				
				//Get the translation value from the offset matrix and apply it to our net translation.
				translation.add(helperOffsetOperationMatrix.m03, helperOffsetOperationMatrix.m13, helperOffsetOperationMatrix.m23);
			}else{
				netMatrix.applyRotation(helperRotationMatrix);
			}
		}
		rotation.multiply(helperRotationMatrix);
	}
	
	public void runScaling(DurationDelayClock clock, float partialTicks){
		//Found scaling.  Get scale that needs to be applied.
		double variableValue = entity.getAnimatedVariableValue(clock, clock.animationAxisMagnitude, partialTicks);
		helperScalingVector.set(clock.animation.axis).scale(variableValue);
		//Check for 0s and remove them.
		if(helperScalingVector.x == 0)helperScalingVector.x = 1.0;
		if(helperScalingVector.y == 0)helperScalingVector.z = 1.0;
		if(helperScalingVector.z == 0)helperScalingVector.z = 1.0;
		
		//If we have a center offset, do special translation code to handle it.
		//Otherwise, don't bother, as it'll just take cycles.
		if(clock.animation.centerPoint.x != 0 || clock.animation.centerPoint.y != 0 || clock.animation.centerPoint.z != 0){
			//First translate to the center point.
			helperPoint.set(clock.animation.centerPoint);
			helperOffsetOperationMatrix.resetTransforms();
			helperOffsetOperationMatrix.setTranslation(helperPoint);
			
			//Now do scaling.
			helperOffsetOperationMatrix.applyScaling(helperScalingVector);
			
			//Translate back.  This requires inverting the translation.
			helperPoint.invert();
			helperOffsetOperationMatrix.applyTranslation(helperPoint);
			
			//Apply that net value to our main matrix.
			netMatrix.multiply(helperOffsetOperationMatrix);
		}else{
			netMatrix.applyScaling(helperScalingVector);
		}
	}
}
