package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.rendering.components.AAnimationsBase;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This class contains methods for pole component animations.
 * Methods here are mainly for connectors and the like.
 *
 * @author don_bruce
 */
public final class AnimationsPoleComponent extends AAnimationsBase<ATileEntityPole_Component>{
	
	@Override
	public double getRawVariableValue(ATileEntityPole_Component component, String variable, float partialTicks){
		double value = getBaseVariableValue(component, variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}
		
		//Check connector variables.
		if(variable.startsWith("neighbor_present_")){
			Axis axis = Axis.valueOf(variable.substring("neighbor_present_".length()).toUpperCase());
			return component.world.getBlock(component.core.position).equals(component.world.getBlock(axis.getOffsetPoint(component.position))) ? 1 : 0;
		}
		//Check solid block variables.
		if(variable.startsWith("solid_present_")){
			Axis axis = Axis.valueOf(variable.substring("solid_present_".length()).toUpperCase());
			return component.world.isBlockSolid(axis.getOffsetPoint(component.position), axis.getOpposite()) ? 1 : 0;
		}
		//Check slab variables.
		switch(variable){
			case("slab_present_up") : return component.world.isBlockTopSlab(Axis.UP.getOffsetPoint(component.position)) ? 1 : 0;
			case("slab_present_down") : return component.world.isBlockBottomSlab(Axis.DOWN.getOffsetPoint(component.position)) ? 1 : 0;
		}
		
		//Not a base variable, or a decor variable.  Return 0 to prevent crashes, but only if we aren't in devMode.
		if(ConfigSystem.configObject.clientControls.devMode.value){
			throw new IllegalArgumentException("Was told to find pole compoment variable:" + variable + " for component:" + component.definition.packID + ":" + component.definition.systemName + ", but such a variable does not exist.  Check your spelling and try again.");
		}else{
			return 0;
		}
	}
}
