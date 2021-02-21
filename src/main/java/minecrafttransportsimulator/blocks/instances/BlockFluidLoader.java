package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;

public class BlockFluidLoader extends ABlockBaseDecor<TileEntityFluidLoader>{
	
	public BlockFluidLoader(){
		super();
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		//Only check right-clicks on the server.
		if(!world.isClient()){
			TileEntityFluidLoader loader = (TileEntityFluidLoader) world.getTileEntity(position);
			loader.unloadMode = !loader.unloadMode;
			player.sendPacket(new PacketPlayerChatMessage(loader.unloadMode ? "interact.loader.unload" : "interact.loader.load"));
		}
		return true;
	}
	
    @Override
	public TileEntityFluidLoader createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityFluidLoader(world, position, data);
	}

	@Override
	public Class<TileEntityFluidLoader> getTileEntityClass(){
		return TileEntityFluidLoader.class;
	}
}
