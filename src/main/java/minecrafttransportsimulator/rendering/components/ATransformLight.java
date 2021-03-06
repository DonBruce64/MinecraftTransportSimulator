package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONLight;

/**This class represents a light on a model.  Inputs are the JSON definition for the light, which is used
 * to obtain the brightness value of said light.  Note that the {@link #lightLevel} will be populated
 * before the transform in the {@link #shouldRender(AEntityC_Definable, boolean, float)}, method.
 *
 * @author don_bruce
 */
public abstract class ATransformLight<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	private final boolean rendersWithLights;
	protected final JSONLight definition;
	protected float lightLevel;
	
	public ATransformLight(JSONLight definition, boolean rendersWithLights){
		this.definition = definition;
		this.rendersWithLights = rendersWithLights;
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		lightLevel = entity.lightBrightnessValues.get(definition);
		return rendersWithLights ? lightLevel > 0 : true;
	}
}
