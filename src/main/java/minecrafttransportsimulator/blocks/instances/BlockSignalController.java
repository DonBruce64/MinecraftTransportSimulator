package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUISignalController;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockSignalController extends ABlockBaseDecor<TileEntitySignalController>{
	
	public BlockSignalController(){
		super();
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		if(world.isClient()){
			InterfaceGUI.openGUI(new GUISignalController((TileEntitySignalController) world.getTileEntity(position)));
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
