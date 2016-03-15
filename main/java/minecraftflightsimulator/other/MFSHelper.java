package minecraftflightsimulator.other;

import minecraftflightsimulator.MFS;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class MFSHelper{
	public static float calculateInventoryWeight(IInventory inventory){
		float weight = 0;
		for(int i=0; i<inventory.getSizeInventory(); ++i){
			ItemStack stack = inventory.getStackInSlot(i);
			if(stack != null){
				weight += 1F*stack.stackSize/stack.getMaxStackSize()*(MFS.heavyItems.contains(stack.getItem().getUnlocalizedName().substring(5)) ? 2 : 1);
			}
		}
		return weight;
	}
}
