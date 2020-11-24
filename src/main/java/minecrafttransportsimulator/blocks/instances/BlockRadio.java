package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRadio;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;

public class BlockRadio extends ABlockBaseDecor<TileEntityRadio>{
	
	public BlockRadio(){
		super();
	}
	
	@Override
	public boolean onClicked(IWrapperWorld world, Point3i point, Axis axis, IWrapperPlayer player){
		if(world.isClient()){
			MasterLoader.guiInterface.openGUI(new GUIRadio((TileEntityRadio) world.getTileEntity(point)));
		}
		return true;
	}

	@Override
	public TileEntityRadio createTileEntity(IWrapperWorld world, Point3i position, IWrapperNBT data){
		return new TileEntityRadio(world, position, data);
	}

	@Override
	public Class<TileEntityRadio> getTileEntityClass(){
		return TileEntityRadio.class;
	}
}
