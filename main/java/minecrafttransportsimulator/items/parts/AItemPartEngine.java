package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AItemPartEngine extends AItemPart{
	
	public AItemPartEngine(Class<? extends APartEngine> engine){
		super(engine);
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems){
		if(MTSRegistry.packTabs.get(item.getRegistryName().getResourceDomain()).equals(tab)){
			subItems.add(getStackWithData(this, false));
			subItems.add(getStackWithData(this, true));
		}
    }
	
	public static ItemStack getStackWithData(AItemPartEngine item, boolean isCreative){
		ItemStack stack = new ItemStack(item);
		NBTTagCompound stackTag = new NBTTagCompound();
		stackTag.setBoolean("isCreative", isCreative);
		stackTag.setBoolean("isAutomatic", item.engineItem.isAutomatic);
		stackTag.setByte("numberGears", item.engineItem.numberGears);
		stackTag.setByte("starterPower", item.engineItem.starterPower);
		stackTag.setByte("starterIncrement", item.engineItem.starterIncrement);
		stackTag.setInteger("maxRPM", item.engineItem.maxRPM);
		stackTag.setInteger("maxSafeRPM", EntityEngine.getMaxSafeRPM(item.engineItem.maxRPM));
		stackTag.setFloat("fuelConsumption", item.engineItem.fuelConsumption);
		stackTag.setDouble("hours", 0);
		stack.setTagCompound(stackTag);
		return stack;
	}
}
