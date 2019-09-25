package minecrafttransportsimulator.items.blocks;

import minecrafttransportsimulator.blocks.core.BlockRotatable;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBlockRotatable extends Item{
	public BlockRotatable[] blocks = new BlockRotatable[4];
	
	public ItemBlockRotatable(){
		super();
		this.setCreativeTab(MTSRegistry.coreTab);
	}
	
	public ItemBlockRotatable createBlocks(){
		for(byte i=0; i<EnumFacing.HORIZONTALS.length; ++i){
			blocks[i] = new BlockRotatable(EnumFacing.HORIZONTALS[i], this);
		}
		return this;
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			
			if(heldStack.getItem() != null){
				//We want to spawn above this block.
				pos = pos.up();
				
				//Based on the rotation of the player, pick from the block array.
				switch(player.getHorizontalFacing()){
					case SOUTH: world.setBlockState(pos, blocks[0].getDefaultState()); break;	
					case WEST: world.setBlockState(pos, blocks[1].getDefaultState()); break;
					case NORTH: world.setBlockState(pos, blocks[2].getDefaultState()); break;
					case EAST: world.setBlockState(pos, blocks[3].getDefaultState()); break;
					default: return EnumActionResult.FAIL;
				}
				
				//Use up the item we used to spawn this block if we are not in creative.
				if(!player.capabilities.isCreativeMode){
					player.inventory.clearMatchingItems(heldStack.getItem(), heldStack.getItemDamage(), 1, heldStack.getTagCompound());
				}
				return EnumActionResult.SUCCESS;
			}
		}
		return EnumActionResult.FAIL;
	}
}
