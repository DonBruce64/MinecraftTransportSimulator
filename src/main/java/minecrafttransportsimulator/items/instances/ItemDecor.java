package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockBeacon;
import minecrafttransportsimulator.blocks.instances.BlockDecor;
import minecrafttransportsimulator.blocks.instances.BlockFluidLoader;
import minecrafttransportsimulator.blocks.instances.BlockFuelPump;
import minecrafttransportsimulator.blocks.instances.BlockRadio;
import minecrafttransportsimulator.blocks.instances.BlockSignalController;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONDecor;

/**Decor item.  Note that while this item can (and does) spawn decor blocks,
 * it can also spawn traffic signal controllers and fuel pumps depending on
 * the definition.  This item, therefore, is essentially a catch-all for all
 * pack, block-based things that aren't poles.
 * 
 * @author don_bruce
 */
public class ItemDecor extends AItemSubTyped<JSONDecor> implements IItemBlock{
	
	public ItemDecor(JSONDecor definition, String subName){
		super(definition, subName);
	}
	
	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		if(definition.general.type != null){
			switch(definition.general.type){
				case("beacon") : return BlockBeacon.class;	
				case("fuel_pump") : return BlockFuelPump.class;
				case("fluid_loader") : return BlockFluidLoader.class;
				case("signal_controller") : return BlockSignalController.class;
				case("radio") : return BlockRadio.class;
			}
		}
		//Normal decor is assumed to be default per legacy systems.
		return BlockDecor.class;
	}
}
