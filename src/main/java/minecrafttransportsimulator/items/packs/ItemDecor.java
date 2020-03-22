package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.blocks.core.BlockDecor;
import minecrafttransportsimulator.blocks.core.TileEntityDecor;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemDecor extends AItemPack<JSONDecor>{
	
	public ItemDecor(JSONDecor definition){
		super(definition);
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			//Set position to correct spawning position.
			if(!world.getBlockState(pos).getBlock().isReplaceable(world, pos)){
	            pos = pos.offset(facing);
	        }
			
			//Based on the block type and light, pick a registered block template.
			if(!definition.general.oriented && !definition.general.lighted){
				world.setBlockState(pos, MTSRegistry.decorBasicDark.getDefaultState().withProperty(BlockDecor.FACING, player.getHorizontalFacing().getOpposite()));
			}else if(definition.general.oriented && !definition.general.lighted){
				world.setBlockState(pos, MTSRegistry.decorOrientedDark.getDefaultState().withProperty(BlockDecor.FACING, player.getHorizontalFacing().getOpposite()));
			}else if(!definition.general.oriented && definition.general.lighted){
				world.setBlockState(pos, MTSRegistry.decorBasicLight.getDefaultState().withProperty(BlockDecor.FACING, player.getHorizontalFacing().getOpposite()));
			}else if(definition.general.oriented && definition.general.lighted){
				world.setBlockState(pos, MTSRegistry.decorOrientedLight.getDefaultState().withProperty(BlockDecor.FACING, player.getHorizontalFacing().getOpposite()));
			}
			
			//Set the decor definition for rendering.
			((TileEntityDecor) world.getTileEntity(pos)).definition = definition;
	        
			//Use up the item we used to spawn this block if we are not in creative.
			if(!player.capabilities.isCreativeMode){
				player.inventory.clearMatchingItems(heldStack.getItem(), heldStack.getItemDamage(), 1, heldStack.getTagCompound());
			}
			return EnumActionResult.SUCCESS;
		}
		return EnumActionResult.FAIL;
	}
}
