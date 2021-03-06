package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a set of transforms that should be applied prior to rendering an object.
 * No rendering is performed in this class, as such rendering may change and is left to the calling class.
 * Rather, this class is a way to collect a series of {@link ATransform} objects via passing-in a list
 * of {@link JSONAnimatedObject}s at construction.  These will be used to apply the appropriate transforms,
 * which will ensure the rendering state is correct when rendering is performed.
 *
 * @author don_bruce
 */
public class RenderableTransform<AnimationEntity extends AEntityC_Definable<?>>{
	public final List<ATransform<AnimationEntity>> transforms = new ArrayList<ATransform<AnimationEntity>>();
	
	public RenderableTransform(List<JSONAnimationDefinition> animations){
		for(JSONAnimationDefinition animation : animations){
			switch(animation.animationType){
				case TRANSLATION :{
					transforms.add(new TransformTranslatable<AnimationEntity>(animation));
					break;
				}
				case ROTATION :{
					transforms.add(new TransformRotatable<AnimationEntity>(animation));
					break;
				}
				case SCALING :{
					transforms.add(new TransformScaleable<AnimationEntity>(animation));
					break;
				}
				case VISIBILITY :{
					transforms.add(new TransformVisibile<AnimationEntity>(animation));
					break;
				}
				case INHIBITOR :{
					transforms.add(new TransformInhibitor<AnimationEntity>(animation));
					break;
				}
				case ACTIVATOR :{
					transforms.add(new TransformActivator<AnimationEntity>(animation));
					break;
				}
			}
		}
	}
	
	/**
	 *  Does all the transforms for this object.  If the object should render, return true. 
	 *  If the object should not render due to a transform, return false.
	 */
	public boolean doPreRenderTransforms(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		double priorOffset = 0;
		boolean inhibitAnimations = false;
		for(ATransform<AnimationEntity> transform : transforms){
			if(inhibitAnimations){
				if(transform.shouldActivate(entity, blendingEnabled, partialTicks)){
					inhibitAnimations = false;
				}
			}else{
				if(transform.shouldInhibit(entity, blendingEnabled, partialTicks)){
					inhibitAnimations = true;
				}else if(!transform.shouldRender(entity, blendingEnabled, partialTicks)){
					return false;
				}else{
					//If the transform is a cumulative offset, send the prior operation's offset down the pipeline. 
					if(transform instanceof ATransformDefinable && ((ATransformDefinable<AnimationEntity>) transform).definition.addPriorOffset){
						priorOffset = transform.applyTransform(entity, blendingEnabled, partialTicks, priorOffset);
					}else{
						priorOffset = transform.applyTransform(entity, blendingEnabled, partialTicks, 0);
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
	public void doPostRenderTransforms(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		for(ATransform<AnimationEntity> transform : transforms){
			transform.doPostRenderLogic(entity, blendingEnabled, partialTicks);
		}
	}
}
