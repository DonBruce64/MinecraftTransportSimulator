package minecraftflightsimulator.minecrafthelpers;

import minecraftflightsimulator.MFSRegistry;
import net.minecraft.entity.player.EntityPlayer;
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
		return getHeldStack(player) != null ? ItemStackHelper.getItemFromStack(getHeldStack(player)).equals(MFSRegistry.wrench) : false;
	}
	
	public static boolean isPlayerCreative(EntityPlayer player){
		return player.capabilities.isCreativeMode;
	}
	
	public static String getTranslatedText(String text){
		return StatCollector.translateToLocal(text);
	}
}
