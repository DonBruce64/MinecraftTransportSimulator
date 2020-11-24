package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.guis.instances.GUISignalController;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;

public class BlockSignalController extends ABlockBaseDecor<TileEntitySignalController>{
	
	public BlockSignalController(){
		super();
	}
	
	@Override
	public boolean onClicked(IWrapperWorld world, Point3i point, Axis axis, IWrapperPlayer player){
		if(world.isClient()){
			MasterLoader.guiInterface.openGUI(new GUISignalController((TileEntitySignalController) world.getTileEntity(point)));
		}
		return true;
	}

	@Override
	public TileEntitySignalController createTileEntity(IWrapperWorld world, Point3i position, IWrapperNBT data){
		return new TileEntitySignalController(world, position, data);
	}

	@Override
	public Class<TileEntitySignalController> getTileEntityClass(){
		return TileEntitySignalController.class;
	}
}
