package minecrafttransportsimulator.rendering.components;

import java.awt.Color;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**This class represents a light object of a model.  Inputs are the name of the name model
* and the name of the light.
*
* @author don_bruce
*/
public class TransformLight_Emissive<AnimationEntity extends AEntityC_Definable<?>> extends ATransformLight<AnimationEntity>{
	private final Color color;
	
	public TransformLight_Emissive(JSONLight definition){
		super(definition, true);
		color = Color.decode(definition.color);
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		InterfaceRender.bindTexture("mts:textures/rendering/light.png");
		InterfaceRender.setLightingState(false);
		InterfaceRender.setColorState(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F, lightLevel);
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		InterfaceRender.setLightingState(true);
		InterfaceRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
		InterfaceRender.recallTexture();
	}
}
