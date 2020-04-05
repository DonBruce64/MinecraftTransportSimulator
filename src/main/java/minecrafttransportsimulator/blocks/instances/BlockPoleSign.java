package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleSign;
import minecrafttransportsimulator.guis.GUISign;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class BlockPoleSign extends BlockPole implements IBlockTileEntity{
    
	@Override
	public void onPlaced(WrapperWorld world, Point3i point, WrapperPlayer player){
		//Load the NBT from the sign that was dropped prior.
		if(player.getHeldStack().hasTagCompound()){
			world.getTileEntity(point).load(new WrapperNBT(player.getHeldStack().getTagCompound()));
		}else{
			//Open the GUI for the sign when we first place it.
			onClicked(world, point, player);
		}
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, WrapperPlayer player){
		if(world.isClient()){
			//TODO need to covert this when we get the chance.
			FMLCommonHandler.instance().showGuiScreen(new GUISign((TileEntityPoleSign) world.getTileEntity(point)));
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
