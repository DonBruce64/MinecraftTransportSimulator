package minecrafttransportsimulator.blocks.instances;

import mcinterface.BuilderGUI;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.guis.instances.GUISignalController;
import minecrafttransportsimulator.jsondefs.JSONDecor;

public class BlockSignalController extends ABlockBase implements IBlockTileEntity<JSONDecor>{
	
	public BlockSignalController(){
		super(10.0F, 5.0F);
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer player){
		if(world.isClient()){
			BuilderGUI.openGUI(new GUISignalController((TileEntitySignalController) world.getTileEntity(point)));
		}
		return true;
	}
	
	@Override
	public TileEntitySignalController createTileEntity(){
		return new TileEntitySignalController();
	}
}
