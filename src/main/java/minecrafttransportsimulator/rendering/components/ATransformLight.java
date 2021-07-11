package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONLight;

/**This class represents a light on a model.  Inputs are the JSON definition for the light, which is used
 * to obtain the brightness value of said light.
 *
 * @author don_bruce
 */
public abstract class ATransformLight<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	protected final JSONLight definition;
	protected float lightLevel;
	
	public ATransformLight(JSONLight definition){
		this.definition = definition;
	}
	
	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		lightLevel = entity.lightBrightnessValues.get(definition);
		return 0;
	}
}
