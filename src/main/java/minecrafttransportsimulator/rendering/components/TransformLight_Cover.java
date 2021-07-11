package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**This class represents a light object of a model.  Inputs are the name of the name model
* and the name of the light.
*
* @author don_bruce
*/
public class TransformLight_Cover<AnimationEntity extends AEntityC_Definable<?>> extends ATransformLight<AnimationEntity>{
	
	public TransformLight_Cover(JSONLight definition){
		super(definition);
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		super.applyTransform(entity, blendingEnabled, partialTicks, offset);
		if(lightLevel > 0){
			InterfaceRender.setLightingState(false);
		}
		InterfaceRender.bindTexture("minecraft:textures/blocks/glass.png");
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		if(lightLevel > 0){
			InterfaceRender.setLightingState(true);
		}
		InterfaceRender.recallTexture();
	}
}
