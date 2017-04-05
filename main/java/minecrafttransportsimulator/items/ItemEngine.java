package minecrafttransportsimulator.items;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import minecrafttransportsimulator.minecrafthelpers.ItemStackHelper;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

public abstract class ItemEngine extends Item{
	
	public ItemEngine(){
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
	}
	
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean p_77624_4_){
		NBTTagCompound stackTag = ItemStackHelper.getStackNBT(stack);
		if(stackTag.getFloat("fuelConsumption") == 0){
			list.add(EnumChatFormatting.DARK_PURPLE + PlayerHelper.getTranslatedText("info.item.engine.creative"));
		}
		list.add(PlayerHelper.getTranslatedText("info.item.engine.maxrpm") + stackTag.getInteger("maxRPM"));
		list.add(PlayerHelper.getTranslatedText("info.item.engine.maxsaferpm") + stackTag.getInteger("maxSafeRPM"));
		list.add(PlayerHelper.getTranslatedText("info.item.engine.fuelconsumption") + stackTag.getFloat("fuelConsumption"));
		list.add(PlayerHelper.getTranslatedText("info.item.engine.hours") + Math.round(stackTag.getDouble("hours")*100D)/100D);
		if(stackTag.getBoolean("oilLeak")){
			list.add(EnumChatFormatting.RED + PlayerHelper.getTranslatedText("info.item.engine.oilleak"));
		}
		if(stackTag.getBoolean("fuelLeak")){
			list.add(EnumChatFormatting.RED + PlayerHelper.getTranslatedText("info.item.engine.fuelleak"));
		}
		if(stackTag.getBoolean("brokenStarter")){
			list.add(EnumChatFormatting.RED + PlayerHelper.getTranslatedText("info.item.engine.brokenstarter"));
		}
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(ItemStack stack : this.getAllPossibleStacks()){
			itemList.add(stack);
		}
    }
	
	private static ItemStack getStackWithData(Item item, int maxRPM, float fuelConsumption){
		ItemStack stack = new ItemStack(item);
		NBTTagCompound stackTag = new NBTTagCompound();
		stackTag.setInteger("maxRPM", maxRPM);
		stackTag.setInteger("maxSafeRPM", EntityEngine.getMaxSafeRPM(maxRPM));
		stackTag.setFloat("fuelConsumption", fuelConsumption);
		stackTag.setDouble("hours", 0);
		ItemStackHelper.setStackNBT(stack, stackTag);
		return stack;
	}
	
	public abstract ItemStack[] getAllPossibleStacks();
	
	public static class ItemEngineAircraftSmall extends ItemEngine{
		@Override
		public ItemStack[] getAllPossibleStacks(){
			ItemStack[] stacks = new ItemStack[3];
			stacks[0] = getStackWithData(MTSRegistry.engineAircraftSmall, 2700, 0.3F);
			stacks[1] = getStackWithData(MTSRegistry.engineAircraftSmall, 2900, 0.4F);
			stacks[2] = getStackWithData(MTSRegistry.engineAircraftSmall, 2900, 0.0F);
			return stacks;
		}
	}
	
	public static class ItemEngineAircraftLarge extends ItemEngine{
		@Override
		public ItemStack[] getAllPossibleStacks() {
			ItemStack[] stacks = new ItemStack[3];
			stacks[0] = getStackWithData(MTSRegistry.engineAircraftLarge, 2000, 0.5F);
			stacks[1] = getStackWithData(MTSRegistry.engineAircraftLarge, 2400, 0.7F);
			stacks[2] = getStackWithData(MTSRegistry.engineAircraftLarge, 2400, 0.0F);
			return stacks;
		}
	}
}
