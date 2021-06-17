package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityChest;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIInventoryContainer;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockChest extends ABlockBaseDecor<TileEntityChest>{
	
	public BlockChest(){
		super();
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		if(world.isClient()){
			TileEntityChest chest = (TileEntityChest) world.getTileEntity(position);
			InterfaceGUI.openGUI(new GUIInventoryContainer(chest.inventory, chest.definition.decor.inventoryTexture));
		}
		return true;
	}

	@Override
	public TileEntityChest createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityChest(world, position, data);
	}

	@Override
	public Class<TileEntityChest> getTileEntityClass(){
		return TileEntityChest.class;
	}
}
