package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;

/**This class represents a translucent object of a model.  The transform will permit rendering only if blending
 * is enabled for this rendering call.
 *
 * @author don_bruce
 */
public class TransformTranslucent<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	
	public TransformTranslucent(){
		super(null);
	}
	
	@Override
	public boolean shouldRenderWithBlendState(boolean blendingEnabled){
		return blendingEnabled;
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		//Do nothing.
		return 0;
	}
}
