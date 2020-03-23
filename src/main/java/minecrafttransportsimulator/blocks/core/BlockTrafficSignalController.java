package minecrafttransportsimulator.blocks.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockTrafficSignalController extends ABlockRotatable implements ITileEntityProvider{
	
	public BlockTrafficSignalController(){
		this.setCreativeTab(MTSRegistry.coreTab);
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		if(Math.sqrt(player.getDistanceSq(pos)) < 5){
			if(world.isRemote){
				MTS.proxy.openGUI(world.getTileEntity(pos), player);
			}
		}
		return true;
	}
	
	@Override
	public TileEntityTrafficSignalController createNewTileEntity(World worldIn, int meta){
		return new TileEntityTrafficSignalController();
	}
}
