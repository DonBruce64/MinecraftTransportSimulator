package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**Class that when extended allows for a common method of applying
 * transforms.  Transforms are a way to modify the rendering routines
 * that are designed to be universal across all systems and routines.
 * Transforms require a definition of what they are, but other than that
 * the implementation is left up to the specific transform.
 *
 * @author don_bruce
 */
public abstract class ATransform<AnimationEntity extends AEntityC_Definable<?>>{
	public final JSONAnimationDefinition definition; 
	private final Map<AnimationEntity, DurationDelayClock> clocks = new HashMap<AnimationEntity, DurationDelayClock>();
	
	public ATransform(JSONAnimationDefinition definition){
		this.definition = definition;
	}
	
	/**
	 *  This method should return true if this transform should be rendered.
	 *  By default, all transforms are rendered, but this may be overridden should
	 *  the object this transform is on need to be made invisible.
	 */
	public boolean shouldRender(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		return true;
	}
	
	/*
	 *  This method should return true if this transform should stop other transforms
	 *  from being rendered. This allows disabling certain animations if this animation's
	 *  criteria are met.
	 */
	public boolean shouldInhibit(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		return false;
	}
	
	/*
	 *  This method should return true if this transform should negate a previous inhibitor.
	 */
	public boolean shouldActivate(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		return false;
	}
	
	/**
	 *  This method applies any transforms this object has prior to rendering.
	 *  Passed-in offset may or may not be used depending on the transform.
	 *  Returned offset should be able to be fed to the next transform down the JSON for its use, if required.
	 */
	public abstract double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset);
	
	/**
	 *  This method allows for post-render logic.  This allows for additional transforms,
	 *  or transform clean-up (say if lighting was modified).
	 */
	public void doPostRenderLogic(AnimationEntity entity, boolean blendingEnabled, float partialTicks){};
	
	/**
	 *  Returns the current animation clock for the passed-in entity.  Clocks are not shared between
	 *  entities to allow each vehicle to have their own running clocks for each animation.
	 */
	public DurationDelayClock getClock(AnimationEntity entity){
		if(!clocks.containsKey(entity)){
			clocks.put(entity, new DurationDelayClock(definition));
		}
		return clocks.get(entity);
	}
}
