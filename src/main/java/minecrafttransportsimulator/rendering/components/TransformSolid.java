package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;

/**This class represents a solid object of a model.  The transform will make it only render on the solid pass.
 *
 * @author don_bruce
 */
public class TransformSolid<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	
	public TransformSolid(){
		super(null);
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, float partialTicks){
		return InterfaceRender.getRenderPass() != 1;
	}

	@Override
	public double applyTransform(AnimationEntity entity, float partialTicks, double offset){
		return offset;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, float partialTicks){
		//Do nothing.
	}
}
