package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

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
	public final Matrix4d netMatrix = new Matrix4d();
	public final Matrix4d rotationMatrix = new Matrix4d();
	public final Vector3d translation = new Vector3d();
	public double animationScale;
	public boolean anyClockMovedThisUpdate;
	
	//Computational variables.
	protected final AEntityD_Definable<?> entity;
	private final List<DurationDelayClock> clocks = new ArrayList<DurationDelayClock>();
	protected final Vector3d helperVector = new Vector3d();
	protected final AxisAngle4d helperRotator = new AxisAngle4d();
	protected final Matrix4d helperTranslationMatrix = new Matrix4d();
	protected final Matrix4d helperRotationMatrix = new Matrix4d();
	protected final Matrix4d helperScalingMatrix = new Matrix4d();
	protected final Matrix4d helperOffsetOperationMatrix = new Matrix4d();
	private boolean inhibitAnimations;
	
	public AnimationSwitchbox(AEntityD_Definable<?> entity, List<JSONAnimationDefinition> animations){
		this.entity = entity;
		for(JSONAnimationDefinition animation : animations){
			clocks.add(new DurationDelayClock(animation));
		}
		helperTranslationMatrix.setIdentity();
	}
	
	public boolean runSwitchbox(float partialTicks){
		inhibitAnimations = false;
		anyClockMovedThisUpdate = false;
		animationScale = 1.0;
		netMatrix.setIdentity();
		translation.set(0, 0, 0);
		rotationMatrix.setIdentity();
		
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
		helperVector.set(clock.animationAxisNormalized.x, clock.animationAxisNormalized.y, clock.animationAxisNormalized.z);
		helperVector.scale(variableValue);
		helperTranslationMatrix.setIdentity();
		helperTranslationMatrix.setTranslation(helperVector);
		netMatrix.mul(helperTranslationMatrix);
		helperTranslationMatrix.get(helperVector);
		translation.add(helperVector);
	}
	
	public void runRotation(DurationDelayClock clock, float partialTicks){
		//Found rotation.  Get angles that needs to be applied.
		double variableValue = entity.getAnimatedVariableValue(clock, clock.animationAxisMagnitude, partialTicks);
		helperRotator.set(clock.animationAxisNormalized.x, clock.animationAxisNormalized.y, clock.animationAxisNormalized.z, Math.toRadians(variableValue));
		helperRotationMatrix.setIdentity();
		helperRotationMatrix.setRotation(helperRotator);
		
		//If we have a center offset, do special translation code to handle it.
		//Otherwise, don't bother, as it'll just take cycles.
		if(clock.animation.centerPoint.x != 0 || clock.animation.centerPoint.y != 0 || clock.animation.centerPoint.z != 0){
			helperVector.set(clock.animation.centerPoint);
			
			//Get a net matrix representing the total rotation.
			helperOffsetOperationMatrix.setIdentity();
			helperOffsetOperationMatrix.setTranslation(helperVector);
			helperOffsetOperationMatrix.mul(helperRotationMatrix);
			
			//Translate back.  This requires inverting the translation.
			helperVector.negate();
			helperTranslationMatrix.setTranslation(helperVector);
			helperOffsetOperationMatrix.mul(helperTranslationMatrix);
			
			//Apply that net value to our matrix components.
			netMatrix.mul(helperOffsetOperationMatrix);
			rotationMatrix.mul(helperRotationMatrix);
			helperOffsetOperationMatrix.get(helperVector);
			translation.add(helperVector);
		}else{
			netMatrix.mul(helperRotationMatrix);
			rotationMatrix.mul(helperRotationMatrix);
		}
	}
	
	public void runScaling(DurationDelayClock clock, float partialTicks){
		//Found scaling.  Get scale that needs to be applied.
		double variableValue = entity.getAnimatedVariableValue(clock, clock.animationAxisMagnitude, partialTicks);
		helperVector.set(clock.animation.axis.x, clock.animation.axis.y, clock.animation.axis.z);
		helperVector.scale(variableValue);
		//Check for 0s and remove them.
		if(helperVector.x == 0)helperVector.x = 1.0;
		if(helperVector.y == 0)helperVector.z = 1.0;
		if(helperVector.z == 0)helperVector.z = 1.0;
		
		//Set the scaling matrix.
		helperScalingMatrix.m00 = helperVector.x;
		helperScalingMatrix.m11 = helperVector.y;
		helperScalingMatrix.m22 = helperVector.z;
		helperScalingMatrix.m33 = 1;
		
		//If we have a center offset, do special translation code to handle it.
		//Otherwise, don't bother, as it'll just take cycles.
		if(clock.animation.centerPoint.x != 0 || clock.animation.centerPoint.y != 0 || clock.animation.centerPoint.z != 0){
			helperVector.set(clock.animation.centerPoint);
			
			//Get a net matrix representing the total scaling.
			helperOffsetOperationMatrix.setIdentity();
			helperOffsetOperationMatrix.setTranslation(helperVector);
			helperOffsetOperationMatrix.mul(helperScalingMatrix);
			
			//Translate back.  This requires inverting the translation.
			helperVector.negate();
			helperTranslationMatrix.setTranslation(helperVector);
			helperOffsetOperationMatrix.mul(helperTranslationMatrix);
			
			//Apply that net value to our matrix components.
			netMatrix.mul(helperOffsetOperationMatrix);
		}else{
			netMatrix.mul(helperScalingMatrix);
		}
	}
}
