package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;

/**This class contains methods for pole animations.
 * These are used to animate pole components in the world.
 *
 * @author don_bruce
 */
public final class AnimationsPole extends AAnimationsBase<ATileEntityPole_Component>{
	
	@Override
	public double getRawVariableValue(ATileEntityPole_Component component, String variable, float partialTicks){
		double value = getBaseVariableValue(component, variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}else{
			return 0;
		}
	}
}
