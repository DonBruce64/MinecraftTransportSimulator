package minecrafttransportsimulator.blocks.decor;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityRotatable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockDecor6AxisSign extends BlockDecor6AxisOriented{
	public static final PropertyDirection ROTATION = PropertyDirection.create("rotation");
    
	public BlockPos lastClickedPos;
	
	public BlockDecor6AxisSign(Material material, float hardness, float resistance){
		super(material, hardness, resistance);
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
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ){
		if(Math.sqrt(player.getDistanceSq(pos)) < 5){
			if(world.isRemote){
				lastClickedPos = pos;
				MTS.proxy.openGUI(this, player);
			}
		}
		return true;
	}
	
	@Override
	public TileEntityRotatable createNewTileEntity(World worldIn, int meta){
		return new TileEntityDecor6AxisSign();
	}
}
