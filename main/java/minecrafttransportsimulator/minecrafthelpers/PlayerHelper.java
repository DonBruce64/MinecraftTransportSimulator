package minecrafttransportsimulator.minecrafthelpers;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class PlayerHelper{

	public static ItemStack getHeldStack(EntityPlayer player){
		return player.inventory.getCurrentItem();
	}
	
	public static void removeItemFromHand(EntityPlayer player, int amountToRemove){
		if(player.inventory.getCurrentItem().stackSize == amountToRemove){
			player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
		}else{
			player.inventory.getCurrentItem().stackSize -= amountToRemove;
		}
	}
	
	public static boolean isPlayerHoldingWrench(EntityPlayer player){
		return getHeldStack(player) != null ? ItemStackHelper.getItemFromStack(getHeldStack(player)).equals(MTSRegistry.wrench) : false;
	}
	
	public static int getQtyOfItemInInventory(Item item, short damage, EntityPlayer player){
		int qty = 0;
		for(byte i=0; i<player.inventory.mainInventory.length; ++i){
			ItemStack stack = player.inventory.mainInventory[i];
			if(stack != null){
				if(ItemStackHelper.getItemFromStack(stack).equals(item)){
					if(ItemStackHelper.getItemDamage(stack) == damage){
						qty += ItemStackHelper.getStackSize(stack);
					}
				}
			}
		}
		return qty;
	}
	
	public static void removeQtyOfItemInInventory(Item item, int qtyToRemove, short damage, EntityPlayer player){
		for(byte i=0; i<player.inventory.mainInventory.length; ++i){
			ItemStack stack = player.inventory.mainInventory[i];
			if(stack != null){
				if(ItemStackHelper.getItemFromStack(stack).equals(item)){
					if(ItemStackHelper.getItemDamage(stack) == damage){
						if(ItemStackHelper.getStackSize(stack) <= qtyToRemove){
							qtyToRemove -= ItemStackHelper.getStackSize(stack);
							player.inventory.mainInventory[i] = null;
						}else{
							//We could decrement here, but 1.11 mucks up the ItemStack system so we don't.
							player.inventory.mainInventory[i] = new ItemStack(ItemStackHelper.getItemFromStack(stack), ItemStackHelper.getStackSize(stack) - qtyToRemove, ItemStackHelper.getItemDamage(stack));
							qtyToRemove = 0;
							return;
						}
					}
				}
			}
		}
	}
	
	public static boolean isPlayerCreative(EntityPlayer player){
		return player.capabilities.isCreativeMode;
	}
	
	public static String getTranslatedText(String text){
		return StatCollector.translateToLocal(text);
	}
}
