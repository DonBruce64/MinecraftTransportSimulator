package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRadio;
import minecrafttransportsimulator.rendering.components.AAnimationsBase;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This class contains methods for decor animations.
 * These are used to animate decor blocks in the world.
 *
 * @author don_bruce
 */
public final class AnimationsDecor extends AAnimationsBase<TileEntityDecor>{
	
	@Override
	public double getRawVariableValue(TileEntityDecor decor, String variable, float partialTicks){
		double value = getBaseVariableValue(decor, variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}
		
		//Check generic variables.
		switch(variable){
			case("redstone_active"): return decor.world.getRedstonePower(decor.position) > 0 ? 1 : 0;	
			case("redstone_level"): return decor.world.getRedstonePower(decor.position);
		}
		
		//Check decor-specific variables.
		if(decor instanceof TileEntityFuelPump){
			TileEntityFuelPump pump = (TileEntityFuelPump) decor;
			switch(variable){
				case("fuelpump_active"): return pump.connectedVehicle != null ? 1 : 0;	
				case("fuelpump_stored"): return pump.getTank().getFluidLevel();
				case("fuelpump_dispensed"): return pump.getTank().getAmountDispensed();
			}
		}else if(decor instanceof TileEntityRadio){
			TileEntityRadio radio = (TileEntityRadio) decor;
			switch(variable){
				case("radio_active"): return radio.getRadio().isPlaying() ? 1 : 0;	
				case("radio_volume"): return radio.getRadio().volume;
				case("radio_preset"): return radio.getRadio().preset;
			}
		}
		
		//Not a base variable, or a decor variable.  Return 0 to prevent crashes, but only if we aren't in devMode.
		if(ConfigSystem.configObject.clientControls.devMode.value){
			throw new IllegalArgumentException("Was told to find decor variable:" + variable + " for decor:" + decor.definition.packID + ":" + decor.definition.systemName + ", but such a variable does not exist.  Check your spelling and try again.");
		}else{
			return 0;
		}
	}
}
