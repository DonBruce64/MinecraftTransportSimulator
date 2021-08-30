package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Radio tile entity.
 *
 * @author don_bruce
 */
public class TileEntityRadio extends TileEntityDecor{
	
	public TileEntityRadio(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, position, placingPlayer, data);
		//Set position here as we don't tick so the radio won't get update() calls.
		radio.position.setTo(position);
	}

	@Override
	public boolean hasRadio(){
		return true;
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		return radio.interact(player);
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		//Radio-specific variables.
		switch(variable){
			case("radio_active"): return radio.isPlaying() ? 1 : 0;	
			case("radio_volume"): return radio.volume;
			case("radio_preset"): return radio.preset;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
}
