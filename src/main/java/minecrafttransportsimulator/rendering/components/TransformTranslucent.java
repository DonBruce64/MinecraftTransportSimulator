package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;

/**This class represents a translucent object of a model.  The transform will set the blending state of this object
 * to allow for translucent texture rendering.
 *
 * @author don_bruce
 */
public class TransformTranslucent<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	
	public TransformTranslucent(){
		super(null);
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, float partialTicks){
		return InterfaceRender.getRenderPass() != 0;
	}

	@Override
	public double applyTransform(AnimationEntity entity, float partialTicks, double offset){
		InterfaceRender.setBlendState(true, false);
		return offset;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, float partialTicks){
		InterfaceRender.setBlendState(InterfaceRender.getRenderPass() == 1, false);
	}
}
