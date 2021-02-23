package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.rendering.components.AAnimationsBase;

/**This class contains methods for player gun animations.
 * Nothing is done here as those entities don't have animations
 * as they don't have a model.
 *
 * @author don_bruce
 */
public final class AnimationsPlayerGun extends AAnimationsBase<EntityPlayerGun>{
	
	@Override
	public double getRawVariableValue(EntityPlayerGun gun, String variable, float partialTicks){
		return 0;
	}
}
