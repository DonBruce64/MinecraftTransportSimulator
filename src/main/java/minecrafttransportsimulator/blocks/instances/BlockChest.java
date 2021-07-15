package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityChest;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;

public class BlockChest extends ABlockBaseDecor<TileEntityChest>{
	
	public BlockChest(){
		super();
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		if(!world.isClient()){
			player.sendPacket(new PacketEntityGUIRequest(world.getTileEntity(position), player, PacketEntityGUIRequest.EntityGUIType.INVENTORY_CHEST));
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
