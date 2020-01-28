package minecrafttransportsimulator.blocks.pole;

import minecrafttransportsimulator.MTS;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockPoleSign extends BlockPoleAttachment{
    
	public BlockPos lastClickedPos;
	
	public BlockPoleSign(float poleRadius){
		super(poleRadius);
	}
	
	@Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack){
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        if(world.isRemote && entity instanceof EntityPlayer){
			lastClickedPos = pos;
			MTS.proxy.openGUI(this, (EntityPlayer) entity);
		}
    }
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		if(Math.sqrt(player.getDistanceSq(pos)) < 5){
			if(world.isRemote){
				lastClickedPos = pos;
				MTS.proxy.openGUI(this, player);
			}
		}
		return true;
	}
	
	@Override
	public TileEntityPoleSign createNewTileEntity(World worldIn, int meta){
		return new TileEntityPoleSign();
	}
}
