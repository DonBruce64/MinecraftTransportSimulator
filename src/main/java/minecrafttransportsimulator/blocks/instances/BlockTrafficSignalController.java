package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityTrafficSignalController;
import minecrafttransportsimulator.guis.instances.GUITrafficSignalController;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

public class BlockTrafficSignalController extends ABlockBase implements IBlockTileEntity{
	
	public BlockTrafficSignalController(){
		super(10.0F, 5.0F);
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, WrapperPlayer player){
		if(world.isClient()){
			WrapperGUI.openGUI(new GUITrafficSignalController((TileEntityTrafficSignalController) world.getTileEntity(point)));
		}
		return true;
	}
	
	@Override
	public TileEntityTrafficSignalController createTileEntity(){
		return new TileEntityTrafficSignalController();
	}
}
