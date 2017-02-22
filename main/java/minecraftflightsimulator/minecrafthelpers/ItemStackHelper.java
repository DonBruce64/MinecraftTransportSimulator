package minecraftflightsimulator.minecrafthelpers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class ItemStackHelper{

	public static Item getItemFromStack(ItemStack stack){
		return stack.getItem();
	}
	
	public static int getItemDamage(ItemStack stack){
		return stack.getItemDamage();
	}
	
	public static NBTTagCompound getStackNBT(ItemStack stack){
		return stack.getTagCompound();
	}
	
	public static void setStackNBT(ItemStack stack, NBTTagCompound tag){
		stack.setTagCompound(tag);
	}
	
	public static int getStackSize(ItemStack stack){
		return stack.stackSize;
	}
}
