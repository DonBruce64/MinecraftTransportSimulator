package minecrafttransportsimulator.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Created for hosting the few helper methods that are actually needed
 *
 * All helpers should be more than wrapping one method
 */
@Deprecated
public class EntityHelper {
    
    public static boolean isPlayerOP(EntityPlayer player){
    	return player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
    }

    public static void removeItemFromHand(EntityPlayer player){
        if(player.inventory.getCurrentItem().stackSize == 1){
            player.inventory.removeStackFromSlot(player.inventory.currentItem);
        }else{
            player.inventory.getCurrentItem().stackSize -= 1;
        }
    }
    
    public static int getQtyOfItemPlayerHas(EntityPlayer player, Item item, int damage){
    	int qty = 0;
    	for(ItemStack stack : player.inventory.mainInventory){
    		if(stack != null){
    			if(stack.getItem().equals(item)){
    				if(stack.getItemDamage() == damage){
    					qty += stack.stackSize;
    				}
    			}
    		}
    	}
    	return qty;
    }
    
    public static boolean removeQtyOfItemsFromPlayer(EntityPlayer player, Item item, int damage, int qtyToRemove){
    	for(byte i=0; i<player.inventory.getSizeInventory(); ++i){
    		ItemStack stack = player.inventory.getStackInSlot(i);
			if(stack != null){
				if(stack.getItem().equals(item)){
					if(stack.getItemDamage() == damage){
						if(stack.stackSize >= qtyToRemove){
							qtyToRemove -= stack.stackSize;
							player.inventory.removeStackFromSlot(i);
						}else{
							stack.stackSize -= qtyToRemove;
							qtyToRemove = 0;
						}
						if(qtyToRemove == 0){
							return true;
						}
					}
				}
			}
		}
    	return false;
    }
}
