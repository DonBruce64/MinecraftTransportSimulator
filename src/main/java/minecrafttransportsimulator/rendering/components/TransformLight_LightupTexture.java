package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**This class represents a light on a model.  Inputs are the JSON definition for the light, which is used
 * to obtain the brightness value of said light.
 *
 * @author don_bruce
 */
public class TransformLight_LightupTexture<AnimationEntity extends AEntityC_Definable<?>> extends ATransformLight<AnimationEntity>{
	
	public TransformLight_LightupTexture(JSONLight definition){
		super(definition, false);
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		if(lightLevel > 0){
			InterfaceRender.setLightingState(false);
		}
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		if(lightLevel > 0){
			InterfaceRender.setLightingState(true);
		}
	}
}
