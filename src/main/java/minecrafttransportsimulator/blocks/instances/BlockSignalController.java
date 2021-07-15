package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;

public class BlockSignalController extends ABlockBaseDecor<TileEntitySignalController>{
	
	public BlockSignalController(){
		super();
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		if(!world.isClient()){
			player.sendPacket(new PacketEntityGUIRequest(world.getTileEntity(position), player, PacketEntityGUIRequest.EntityGUIType.SIGNAL_CONTROLLER));
		}
		return true;
	}

	@Override
	public TileEntitySignalController createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntitySignalController(world, position, data);
	}

	@Override
	public Class<TileEntitySignalController> getTileEntityClass(){
		return TileEntitySignalController.class;
	}
}
