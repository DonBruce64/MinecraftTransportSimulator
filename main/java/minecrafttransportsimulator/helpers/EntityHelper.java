package minecrafttransportsimulator.helpers;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * Created for hosting the few helper methods that are actually needed
 *
 * All helpers should be more than wrapping one method
 */
public class EntityHelper {

    public static EntityMultipartBase getEntityByUUID(World world, String UUID){
        for(Object obj : world.loadedEntityList){
            if(obj instanceof EntityMultipartBase){
                if(UUID.equals(((EntityMultipartBase) obj).UUID)){
                    return (EntityMultipartBase) obj;
                }
            }
        }
        return null;
    }

    public static boolean isPlayerHoldingWrench(EntityPlayer player){
        return player.inventory.getCurrentItem() != null && player.inventory.getCurrentItem().getItem().equals(MTSRegistry.wrench);
    }

    public static void removeItemFromHand(EntityPlayer player, int amountToRemove){
        if(player.inventory.getCurrentItem().stackSize == amountToRemove){
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        }else{
            player.inventory.getCurrentItem().stackSize -= amountToRemove;
        }
    }
}
