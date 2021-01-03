package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a set of transforms that can be applied prior to rendering an object.
 * No rendering is performed in this class, as such rendering may change and is left to the calling class.
 * Rather, this class is a way to collect a series of {@link ATransform} objects via passing-in a list
 * of {@link JSONAnimatedObject}s at construction.  These will be used to apply the appropriate transforms,
 * which will ensure the rendering state is correct when rendering is performed.
 *
 * @author don_bruce
 */
public class RenderableTransform{
	public final List<ATransform> transforms = new ArrayList<ATransform>();
	
	public RenderableTransform(List<JSONAnimationDefinition> animations){
		for(JSONAnimationDefinition animation : animations){
			switch(animation.animationType){
				case("translation") :{
					transforms.add(new TransformTranslatable(animation));
					break;
				}
				case("rotation") :{
					transforms.add(new TransformRotatable(animation));
					break;
				}
				case("visibility") :{
					transforms.add(new TransformVisibile(animation));
					break;
				}
				case("inhibitor") :{
					transforms.add(new TransformInhibitor(animation));
					break;
				}
				case("activator") :{
					transforms.add(new TransformActivator(animation));
					break;
				}
			}
		}
	}
	
	/**
	 *  Does all the transforms for this object.  If the object should render, return true. 
	 *  If the object should not render due to a transform, return false.
	 */
	public boolean doPreRenderTransforms(IAnimationProvider provider, float partialTicks){
		double priorOffset = 0;
		boolean inhibitAnimations = false;
		for(ATransform transform : transforms){
			if(inhibitAnimations){
				if(transform.shouldActivate(provider, partialTicks)){
					inhibitAnimations = false;
				}
			}else{
				if(transform.shouldInhibit(provider, partialTicks)){
					inhibitAnimations = true;
				}else if(!transform.shouldRender(provider, partialTicks)){
					return false;
				}else{
					//If the transform is a cumulative offset, send the prior operation's offset down the pipeline. 
					if(transform.definition != null && transform.definition.addPriorOffset){
						priorOffset = transform.applyTransform(provider, partialTicks, priorOffset);
					}else{
						priorOffset = transform.applyTransform(provider, partialTicks, 0);
					}
				}
			}
		}
		return true;
	}
	
	/**
	 *  Does post-render transform logic.  This is transform-dependent, and should be done after rendering
	 *  has been completed.
	 */
	public void doPostRenderTransforms(IAnimationProvider provider, float partialTicks){
		for(ATransform transform : transforms){
			transform.doPostRenderLogic(provider, partialTicks);
		}
	}
}
