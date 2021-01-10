package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRadio;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockRadio extends ABlockBaseDecor<TileEntityRadio>{
	
	public BlockRadio(){
		super();
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer player){
		if(world.isClient()){
			InterfaceGUI.openGUI(new GUIRadio((TileEntityRadio) world.getTileEntity(point)));
		}
		return true;
	}

	@Override
	public TileEntityRadio createTileEntity(WrapperWorld world, Point3i position, WrapperNBT data){
		return new TileEntityRadio(world, position, data);
	}

	@Override
	public Class<TileEntityRadio> getTileEntityClass(){
		return TileEntityRadio.class;
	}
}
