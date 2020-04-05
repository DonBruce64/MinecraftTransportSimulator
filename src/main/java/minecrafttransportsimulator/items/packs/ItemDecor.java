package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockDecor;
import minecrafttransportsimulator.items.core.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.wrappers.WrapperBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemDecor extends AItemPack<JSONDecor> implements IItemBlock{
	
	public ItemDecor(JSONDecor definition){
		super(definition);
	}
	
	@Override
	public ABlockBase createBlock(){
		return new BlockDecor(definition);
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			WrapperBlock wrapper = WrapperBlock.blockWrapperMap.get(getBlock());
	    	ItemStack itemstack = player.getHeldItem(hand);
	    	if(!world.getBlockState(pos).getBlock().isReplaceable(world, pos)){
	            pos = pos.offset(facing);
	        }
	    	if(!itemstack.isEmpty() && player.canPlayerEdit(pos, facing, itemstack) && world.mayPlace(wrapper, pos, false, facing, null)){
	            IBlockState newState = wrapper.getStateForPlacement(world, pos, facing, 0, 0, 0, 0, player, hand);
	            if(world.setBlockState(pos, newState, 11)){
	                itemstack.shrink(1);
	            }
	            return EnumActionResult.SUCCESS;
	        }
		}
		return EnumActionResult.FAIL;
	}
}
