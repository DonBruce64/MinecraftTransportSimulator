package minecrafttransportsimulator.blocks;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSBlockRotateable;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.dataclasses.MTSAchievements;
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

public class BlockPropellerBench extends MTSBlockRotateable{

	public BlockPropellerBench(){
		super(Material.IRON);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ){
		if(Math.sqrt(player.getDistanceSq(pos)) < 5){
			TileEntityPropellerBench bench = (TileEntityPropellerBench) world.getTileEntity(pos);
			if(bench.getPropellerOnBench() != null){
				bench.dropPropellerAt(player.posX, player.posY, player.posZ);
				if(!world.isRemote){
					MTSAchievements.triggerPropeller(player);
				}
			}else{
				if(bench.isRunning()){
					if(!world.isRemote){
						MTS.MTSNet.sendTo(new ChatPacket("interact.failure.propellerbenchworking"), (EntityPlayerMP) player);
					}
				}else{
					if(world.isRemote){
						MTS.proxy.openGUI(bench, player);
					}
				}
			}
		}
		return true;
	}
	
	@Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
		if(((TileEntityPropellerBench) world.getTileEntity(pos)).getPropellerOnBench() != null){
			((TileEntityPropellerBench) world.getTileEntity(pos)).dropPropellerAt(pos.getX(), pos.getY(), pos.getZ());
		}
		super.breakBlock(world, pos, state);
    }
	
	@Override
	public MTSTileEntity getTileEntity(){
		return new TileEntityPropellerBench();
	}
	
	@Override
	protected boolean canRotateDiagonal(){
		return true;
	}
}
