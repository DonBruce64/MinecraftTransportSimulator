package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.guis.instances.GUISignalController;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

public class BlockSignalController extends ABlockBase implements IBlockTileEntity<JSONDecor>{
	
	public BlockSignalController(){
		super(10.0F, 5.0F);
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer player){
		if(world.isClient()){
			WrapperGUI.openGUI(new GUISignalController((TileEntitySignalController) world.getTileEntity(point)));
		}
		return true;
	}
	
	@Override
	public void onWorldUpdate(WrapperWorld world, Point3i location, boolean redstonePower){
		//Need to set the signal controller redstone state.
		//We invert state to keep us from needing a redstone block on all controllers.
		TileEntitySignalController controller = (TileEntitySignalController) world.getTileEntity(location);
		if(controller != null){
			controller.lightsOn = !redstonePower;
		}
	}
	
	@Override
	public TileEntitySignalController createTileEntity(){
		return new TileEntitySignalController();
	}
}
