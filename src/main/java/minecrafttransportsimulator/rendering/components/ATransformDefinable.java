package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**Extended class for animations that have definitions attached.
 *
 * @author don_bruce
 */
public abstract class ATransformDefinable<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	public final JSONAnimationDefinition definition;
	
	public ATransformDefinable(JSONAnimationDefinition definition){
		super();
		this.definition = definition;
	}
}
