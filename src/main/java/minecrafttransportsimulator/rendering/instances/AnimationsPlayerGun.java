package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.rendering.components.AAnimationsBase;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;

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
