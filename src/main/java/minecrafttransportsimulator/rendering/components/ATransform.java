package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**Class that when extended allows for a common method of applying
 * transforms.  Transforms are a way to modify the rendering routines
 * that are designed to be universal across all systems and routines.
 * Transforms require a definition of what they are, but other than that
 * the implementation is left up to the specific transform.
 *
 * @author don_bruce
 */
public abstract class ATransform{
	public final JSONAnimationDefinition definition; 
	private final Map<IAnimationProvider, DurationDelayClock> clocks = new HashMap<IAnimationProvider, DurationDelayClock>();
	
	public ATransform(JSONAnimationDefinition definition){
		this.definition = definition;
	}
	
	/**
	 *  This method should return true if this transform should be rendered.
	 *  By default, all transforms are rendered, but this may be overridden should
	 *  the object this transform is on need to be made invisible.
	 */
	public boolean shouldRender(IAnimationProvider provider, float partialTicks){
		return true;
	}
	
	/*
	 *  This method should return true if this transform should stop other transforms
	 *  from being rendered. This allows disabling certain animations if this animation's
	 *  criteria are met.
	 */
	public boolean shouldInhibit(IAnimationProvider provider, float partialTicks){
		return false;
	}
	
	/*
	 *  This method should return true if this transform should negate a previous inhibitor.
	 */
	public boolean shouldActivate(IAnimationProvider provider, float partialTicks){
		return false;
	}
	
	/**
	 *  This method applies any transforms this object has prior to rendering.
	 *  Passed-in offset may or may not be used depending on the transform.
	 *  Returned offset should be able to be fed to the next transform down the JSON for its use, if required.
	 */
	public abstract double applyTransform(IAnimationProvider provider, float partialTicks, double offset);
	
	/**
	 *  This method allows for post-render logic.  This allows for additional transforms,
	 *  or transform clean-up (say if lighting was modified).
	 */
	public void doPostRenderLogic(IAnimationProvider provider, float partialTicks){};
	
	/**
	 *  Returns the current animation clock for the passed-in provider.  Clocks are not shared between
	 *  vehicle to allow each vehicle to have their own running clocks for each animation.
	 */
	public DurationDelayClock getClock(IAnimationProvider provider){
		if(!clocks.containsKey(provider)){
			clocks.put(provider, new DurationDelayClock(definition));
		}
		return clocks.get(provider);
	}
}
