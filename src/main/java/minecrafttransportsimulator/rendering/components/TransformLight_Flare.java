package minecrafttransportsimulator.rendering.components;

import java.awt.Color;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This class represents a light object of a model.  Inputs are the name of the name model
* and the name of the light.
*
* @author don_bruce
*/
public class TransformLight_Flare<AnimationEntity extends AEntityC_Definable<?>> extends ATransformLight<AnimationEntity>{
	private final Color color;
	
	public TransformLight_Flare(JSONLight definition){
		super(definition);
		color = Color.decode(definition.color);
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		super.applyTransform(entity, blendingEnabled, partialTicks, offset);
		float flareBrightness = Math.min((1 - entity.world.getLightBrightness(entity.position, false))*lightLevel, 1);
		if(flareBrightness > 0){
			InterfaceRender.bindTexture("mts:textures/rendering/lensflare.png");
			if(ConfigSystem.configObject.clientRendering.flaresBright.value){
				InterfaceRender.setLightingState(false);
				InterfaceRender.setBlendBright(true);
			}
			InterfaceRender.setColorState(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, flareBrightness);
		}
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		if(lightLevel > 0){
			if(ConfigSystem.configObject.clientRendering.flaresBright.value){
				InterfaceRender.setBlendBright(false);
				InterfaceRender.setLightingState(true);
			}
			InterfaceRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
			InterfaceRender.recallTexture();
		}
	}
}
