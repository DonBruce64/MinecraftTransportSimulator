package minecrafttransportsimulator.items.core;

import minecrafttransportsimulator.blocks.core.BlockDecor;
import minecrafttransportsimulator.blocks.core.TileEntityDecor;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.PackDecorObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemDecor extends Item{
	public final String decorName;
	
	public ItemDecor(String decorName){
		super();
		this.decorName = decorName;
		this.setUnlocalizedName(decorName.replace(":", "."));
		this.setCreativeTab(MTSRegistry.packTabs.get(decorName.substring(0, decorName.indexOf(':'))));
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
			String blockName = ((ItemDecor) heldStack.getItem()).decorName;
			PackDecorObject pack = PackParserSystem.getDecor(blockName);
			if(!pack.general.oriented && !pack.general.lighted){
				world.setBlockState(pos, MTSRegistry.decorBasicDark.getDefaultState().withProperty(BlockDecor.FACING, player.getHorizontalFacing().getOpposite()));
			}else if(pack.general.oriented && !pack.general.lighted){
				world.setBlockState(pos, MTSRegistry.decorOrientedDark.getDefaultState().withProperty(BlockDecor.FACING, player.getHorizontalFacing().getOpposite()));
			}else if(!pack.general.oriented && pack.general.lighted){
				world.setBlockState(pos, MTSRegistry.decorBasicLight.getDefaultState().withProperty(BlockDecor.FACING, player.getHorizontalFacing().getOpposite()));
			}else if(pack.general.oriented && pack.general.lighted){
				world.setBlockState(pos, MTSRegistry.decorOrientedLight.getDefaultState().withProperty(BlockDecor.FACING, player.getHorizontalFacing().getOpposite()));
			}
			
			//Set the decor name for rendering.
			((TileEntityDecor) world.getTileEntity(pos)).decorName = blockName;
	        
			//Use up the item we used to spawn this block if we are not in creative.
			if(!player.capabilities.isCreativeMode){
				player.inventory.clearMatchingItems(heldStack.getItem(), heldStack.getItemDamage(), 1, heldStack.getTagCompound());
			}
			return EnumActionResult.SUCCESS;
		}
		return EnumActionResult.FAIL;
	}
}
