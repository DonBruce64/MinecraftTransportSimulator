package minecrafttransportsimulator.blocks;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSBlockTileEntity;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.packets.general.ChatPacket;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockPropellerBench extends MTSBlockTileEntity{

	public BlockPropellerBench(){
		super(Material.IRON, 5.0F, 10.0F);
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ){
		if(player.getDistance(pos.getX(), pos.getY(),  pos.getZ()) < 5){
			TileEntityPropellerBench bench = (TileEntityPropellerBench) world.getTileEntity(pos);
			if(!world.isRemote){
				if(bench.getPropellerOnBench() != null){
					bench.dropPropellerAt(player.posX, player.posY, player.posZ);
				}else if(bench.isRunning()){
					MTS.MFSNet.sendTo(new ChatPacket("interact.failure.propellerbenchworking"), (EntityPlayerMP) player);
				}
			}else{
				if(!bench.isRunning() && bench.getPropellerOnBench() == null){
					MTS.proxy.openGUI(bench, player);
				}
			}
		}
		return true;
	}
	
	@Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
		if(!world.isRemote){
			((TileEntityPropellerBench) world.getTileEntity(pos)).dropPropellerAt(pos.getX(), pos.getY(), pos.getZ());
		}
		super.breakBlock(world, pos, state);
    }
	
	@Override
	public MTSTileEntity getTileEntity(){
		return new TileEntityPropellerBench();
	}	
}
