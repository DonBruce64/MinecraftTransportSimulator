package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.rendering.components.AAnimationsBase;

/**This class contains methods for pole animations.
 * We don't use any here, as its pole compoments that animate things.
 *
 * @author don_bruce
 */
public final class AnimationsPole extends AAnimationsBase<TileEntityPole>{
	
	@Override
	public double getRawVariableValue(TileEntityPole pole, String variable, float partialTicks){
		double value = getBaseVariableValue(pole, variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}else{
			return 0;
		}
	}
}
