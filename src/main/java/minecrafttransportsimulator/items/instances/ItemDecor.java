package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockDecor;
import minecrafttransportsimulator.blocks.instances.BlockFuelPump;
import minecrafttransportsimulator.blocks.instances.BlockSignalController;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.items.components.IItemOBJProvider;
import minecrafttransportsimulator.jsondefs.JSONDecor;

/**Decor item.  Note that while this item can (and does) spawn decor blocks,
 * it can also spawn traffic signal controllers and fuel pumps depending on
 * the definition.  This item, therefore, is essentially a catch-all for all
 * pack, block-based things that aren't poles.
 * 
 * @author don_bruce
 */
public class ItemDecor extends AItemPack<JSONDecor> implements IItemBlock, IItemOBJProvider{
	
	public ItemDecor(JSONDecor definition){
		super(definition);
	}
	
	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		if(definition.general.type != null){
			switch(definition.general.type){
				case("fuel_pump") : return BlockFuelPump.class;
				case("signal_controller") : return BlockSignalController.class;
			}
		}
		//Normal decor is assumed to be default per legacy systems.
		return BlockDecor.class;
	}
	
	@Override
	public String getModelLocation(){
		return definition.general.modelName != null ? "objmodels/decors/" + definition.general.modelName + ".obj" : "objmodels/decors/" + definition.systemName + ".obj";
	}
	
	@Override
	public String getTextureLocation(){
		return "textures/decors/" + definition.systemName + ".png";
	}
}
