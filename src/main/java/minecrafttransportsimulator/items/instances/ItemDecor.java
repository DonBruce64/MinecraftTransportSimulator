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
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

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
		return DecorComponentType.getBlockClass(definition.general.type);
	}
	
	public static enum DecorComponentType{
		@JSONDescription("Will make the decor have beacon functionality.")
		BEACON,
		@JSONDescription("Will make the decor have signal controller functionality.")
		SIGNAL_CONTROLLER,
		@JSONDescription("Will make the decor have fuel pump functionality.  Text rendering may be added by adding textObjects in the rendering section.  These are hard-coded to render the loader's internal fluid name, level, and amount dispensed, in that order.  Adding more textObject entries starts this cycle over.")
		FUEL_PUMP,
		@JSONDescription("Will make the decor have fluid loader functionality.  Text cannot be rendered on loaders like on fuel pumps.")
		FLUID_LOADER,
		@JSONDescription("Will make the decor have radio functionality.  Exact same system as vehicles.  It even syncs up with them!")
		RADIO;
		
		/**
		 *  Helper method to get the block class for this decor.
		 */
		public static Class<? extends ABlockBase> getBlockClass(DecorComponentType type){
			if(type != null){
				switch(type){
					case BEACON : return BlockBeacon.class;	
					case SIGNAL_CONTROLLER : return BlockSignalController.class;
					case FUEL_PUMP : return BlockFuelPump.class;
					case FLUID_LOADER : return BlockFluidLoader.class;
					case RADIO : return BlockRadio.class;
				}
			}
			//Normal decor is assumed to be default per legacy systems.
			return BlockDecor.class;
		}
	}
}
