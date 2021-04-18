package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.rendering.components.AAnimationsBase;

/**This class contains methods for bullet animations.
 * We don't use any here, as bullets don't do animation.
 *
 * @author don_bruce
 */
public final class AnimationsBullet extends AAnimationsBase<EntityBullet>{
	
	@Override
	public double getRawVariableValue(EntityBullet bullet, String variable, float partialTicks){
		double value = getBaseVariableValue(bullet, variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}else{
			return 0;
		}
	}
}
