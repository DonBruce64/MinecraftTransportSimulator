package minecrafttransportsimulator.wrappers;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.items.core.AItemBase;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**Wrapper for MC GUI classes.  Constructor takes a type of {@link AItemBase}, but
 * is only visible when calling {@link #createItem(AItemBase)}.  This will automatically
 * construct the wrapper and will return the created instance of the item (not wrapper)
 * for use in the code.  The wrapper instance is cached and saved to be registered
 * in the MC systems.  When interfacing with MC systems use this class, but when
 * doing code in MTS use the item, NOT the wrappoer!
 *
 * @author don_bruce
 */
public class WrapperItem extends Item{	
	
	private final AItemBase item;
	
	private WrapperItem(AItemBase item, boolean isStackable){
		super();
		this.item = item;
		setFull3D();
		if(!isStackable){
			this.setMaxStackSize(1);
		}
	}
	
	/**
	 *  This is called by the main MC system to get the displayName for the item.
	 *  Normally this is a translated version of the unlocalized name.
	 */
	@Override
	public String getItemStackDisplayName(ItemStack stack){
        return item.getItemName();
	}
	
	/**
	 *  This is called by the main MC system to add tooltip lines to the item.
	 *  The ItemStack is passed-in here as it contains NBT data that may be used
	 *  to change the display of the tooltip.
	 */
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		item.addTooltipLines(tooltipLines, stack.getTagCompound());
	}
	
	/**
	 *  This is called by the main MC system when this item is used.  Can either be main-hand or offhand.
	 *  Whether or not we forward the call to the item depends on the value of {@link AItemBase#getInteractionSide()}.
	 *  Note that this won't get called if the player has clicked a block or entity.  In the case of the entity, it gets
	 *  the call itself.  In the case of the block, we get the call on the method after this one.
	 */
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand){
		if(item.getInteractionSide().equals(AItemBase.InteractionSide.NONE)){
			//Treat the item as a regular nothing item.  Return pass to let MC do what it does.
			return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
		}else{
			if(item.getInteractionSide().interactOn(world.isRemote)){
				item.onUsed(player.posX, player.posY, player.posZ, player);
			}
			//Return FAIL to prevent MC from doing other interaction bits as we should be doing interaction.
			return new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
		}
	}
	
	/**
	 *  This is called by the main MC system when this item is used on a block.  Can either be main-hand or offhand.
	 *  Whether or not we forward the call to the item depends on the value of {@link AItemBase#getInteractionSide()}.
	 *  Note that if this or the block returns success, processing will stop.
	 */
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		//TODO we may not need this method if we don't care about block-specific item interaction.
		if(item.getInteractionSide().equals(AItemBase.InteractionSide.NONE)){
			//Treat the item as a regular nothing item.  Return pass to let MC do what it does.
			return EnumActionResult.PASS;
		}else{
			if(item.getInteractionSide().interactOn(world.isRemote)){
				item.onUsed(hitX, hitY, hitZ, player);
			}
			//Return FAIL to prevent MC from doing other interaction bits as we should be doing interaction.
			return EnumActionResult.FAIL;
		}
	}
	
	
	//--------------------START OF INSTANCE HELPER METHODS--------------------	


	
	//--------------------START OF STATIC HELPER METHODS--------------------
	/**
	 *  Creates a wrapper for the the passed-in Item, saving the wrapper to be registered later.
	 *  This wrapper instance will interact with all MC code via passthrough of the item's methods.
	 *  Returns the passed-in item for constructor convenience.
	 */
	public static void registerItem(AItemBase item){
		//TODO save item in a list here for registration.
	}
}
