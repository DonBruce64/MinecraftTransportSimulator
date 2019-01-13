package minecrafttransportsimulator.items.core;

import minecrafttransportsimulator.blocks.core.TileEntityDecor;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackDecorObject;
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
	public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(!world.isRemote && player.getHeldItem(hand) != null){
			ItemStack heldStack = player.getHeldItem(hand);
			if(heldStack.getItem() != null){
				//We want to spawn above this block.
				pos = pos.up();
				
				//Based on the block type and light, pick a registered block template.
				String blockName = ((ItemDecor) heldStack.getItem()).decorName;
				PackDecorObject pack = PackParserSystem.getDecor(blockName);
				if(!pack.general.oriented && !pack.general.lighted){
					world.setBlockState(pos, MTSRegistry.decorBasicDark.getDefaultState());
				}else if(pack.general.oriented && !pack.general.lighted){
					world.setBlockState(pos, MTSRegistry.decorOrientedDark.getDefaultState());
				}else if(!pack.general.oriented && pack.general.lighted){
					world.setBlockState(pos, MTSRegistry.decorBasicLight.getDefaultState());
				}else if(pack.general.oriented && pack.general.lighted){
					world.setBlockState(pos, MTSRegistry.decorOrientedLight.getDefaultState());
				}
				
				//Get the TE and set states for it.
				TileEntityDecor decorTile = ((TileEntityDecor) world.getTileEntity(pos));
				decorTile.decorName = blockName;
				if(pack.general.oriented){
					decorTile.rotation = (byte) Math.floor(((player.rotationYawHead + 45)%360/90F));
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
