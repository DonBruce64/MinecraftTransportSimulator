package minecrafttransportsimulator.helpers;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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
    
    //TODO add method for "get qty of specific items in player inventory"
    //TODO add method for "remove qty of specific items in player inventory"  Should return success/fail.
    
    public static Entity getRider(Entity entityRidden){
    	return !entityRidden.getPassengers().isEmpty() ? entityRidden.getPassengers().get(0) : null;
    }
    
    public static boolean isBoxCollidingWithBlocks(World world, AxisAlignedBB box, boolean countLiquids){
    	if(!world.getCollisionBoxes(box).isEmpty()){
    		return true;
    	}else{
    		if(!countLiquids){
    			return false;
    		}else{
    			int minX = (int) Math.floor(box.minX);
    	    	int maxX = (int) Math.floor(box.maxX + 1.0D);
    	    	int minY = (int) Math.floor(box.minY);
    	    	int maxY = (int) Math.floor(box.maxY + 1.0D);
    	    	int minZ = (int) Math.floor(box.minZ);
    	    	int maxZ = (int) Math.floor(box.maxZ + 1.0D);
    	    	
    	    	for(int i = minX; i < maxX; ++i){
    	    		for(int j = minY; j < maxY; ++j){
    	    			for(int k = minZ; k < maxZ; ++k){
    	    				if(world.getBlockState(new BlockPos(i, j, k)).getMaterial().isLiquid()){
    	    					return true;
    	    				}
    	    			}
    	    		}
    	    	}
    	    	return false;
    		}
    	}
    }
    
    public static List<BlockPos> getCollidingBlocks(World world, AxisAlignedBB box, boolean countLiquids){
    	int minX = (int) Math.floor(box.minX);
    	int maxX = (int) Math.floor(box.maxX + 1.0D);
    	int minY = (int) Math.floor(box.minY);
    	int maxY = (int) Math.floor(box.maxY + 1.0D);
    	int minZ = (int) Math.floor(box.minZ);
    	int maxZ = (int) Math.floor(box.maxZ + 1.0D);
    	 List<BlockPos> listToAddTo = new ArrayList<BlockPos>();
    	
    	for(int i = minX; i < maxX; ++i){
    		for(int j = minY; j < maxY; ++j){
    			for(int k = minZ; k < maxZ; ++k){
    				BlockPos pos = new BlockPos(i, j, k);
    				AxisAlignedBB blockBox = world.getBlockState(pos).getCollisionBoundingBox(world, pos);
    				if(blockBox != null && box.intersectsWith(blockBox) || (countLiquids && world.getBlockState(pos).getMaterial().isLiquid())){
    					listToAddTo.add(pos);
    				}
    			}
    		}
    	}
    	return listToAddTo;
    }
}
