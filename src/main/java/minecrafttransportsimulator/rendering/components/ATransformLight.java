package minecrafttransportsimulator.rendering.components;

import java.awt.Color;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
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
	protected Color color;
	
	public ATransformLight(JSONLight definition, boolean rendersWithLights){
		this.definition = definition;
		this.rendersWithLights = rendersWithLights;
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		lightLevel = entity.lightBrightnessValues.get(definition);
		color = entity.lightColorValues.get(definition);
		if(definition.isElectric && entity instanceof EntityVehicleF_Physics){
			//Light start dimming at 10V, then go dark at 3V.
			double electricPower = ((EntityVehicleF_Physics) entity).electricPower;
			if(electricPower < 3){
				lightLevel = 0;
			}else if(electricPower < 10){
				lightLevel *= (electricPower - 3)/7D; 
			}
		}
		return rendersWithLights ? lightLevel > 0 : true;
	}
}
