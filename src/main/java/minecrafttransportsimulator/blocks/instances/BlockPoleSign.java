package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleSign;
import minecrafttransportsimulator.guis.GUISign;
import minecrafttransportsimulator.wrappers.WrapperBlockAxial;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class BlockPoleSign extends BlockPole implements WrapperTileEntity.IProvider{
    	
	public BlockPoleSign(WrapperBlockAxial wrapperReference){
		super(wrapperReference);
	}
	
	@Override
	public void onPlaced(WrapperWorld world, Point3i point, WrapperPlayer player){
		//FIXME set sign NBT here.
		//Open the GUI for the sign when we place it.
		onClicked(world, point, player);
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, WrapperPlayer player){
		if(world.isClient()){
			//TODO need to covert this when we get the chance.
			FMLCommonHandler.instance().showGuiScreen(new GUISign((TileEntityPoleSign) world.getTileEntity(point), player));
		}
		return true;
	}
	
	@Override
	public boolean canConnectOnFront(){
		return false;
	}

	@Override
	public TileEntityPoleSign createTileEntity(){
		return new TileEntityPoleSign();
	}
}
