package minecrafttransportsimulator.helpers;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Created for hosting the few helper methods that are actually needed
 *
 * All helpers should be more than wrapping one method
 */
public class EntityHelper {

    public static EntityMultipartBase getEntityByUUID(World world, String UUID){
    	if(!UUID.equals("")){
	        for(Object obj : world.loadedEntityList){
	            if(obj instanceof EntityMultipartBase){
	                if(UUID.equals(((EntityMultipartBase) obj).UUID)){
	                    return (EntityMultipartBase) obj;
	                }
	            }
	        }
    	}
        return null;
    }

    public static boolean isPlayerHoldingWrench(EntityPlayer player){
        return player.inventory.getCurrentItem() != null && player.inventory.getCurrentItem().getItem().equals(MTSRegistry.wrench);
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

	public static AxisAlignedBB getOffsetBoundingBox(AxisAlignedBB box, double x, double y, double z)
	{
		return new AxisAlignedBB(box.minX + x, box.minY + y, box.minZ + z, box.maxX + x, box.maxY + y, box.maxZ + z);
	}
}
