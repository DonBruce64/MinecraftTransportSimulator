package minecrafttransportsimulator.items;

import java.util.List;

import minecrafttransportsimulator.dataclasses.MTSCreativeTabs;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class ItemEngine extends Item{
	
	public ItemEngine(){
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		NBTTagCompound stackTag = stack.getTagCompound();
		if(stackTag.getFloat("fuelConsumption") == 0){
			tooltipLines.add(TextFormatting.DARK_PURPLE + I18n.format("info.item.engine.creative"));
		}
		tooltipLines.add(I18n.format("info.item.engine.maxrpm") + stackTag.getInteger("maxRPM"));
		tooltipLines.add(I18n.format("info.item.engine.maxsaferpm") + stackTag.getInteger("maxSafeRPM"));
		tooltipLines.add(I18n.format("info.item.engine.fuelconsumption") + stackTag.getFloat("fuelConsumption"));
		tooltipLines.add(I18n.format("info.item.engine.hours") + Math.round(stackTag.getDouble("hours")*100D)/100D);
		if(stackTag.getBoolean("oilLeak")){
			tooltipLines.add(TextFormatting.RED + I18n.format("info.item.engine.oilleak"));
		}
		if(stackTag.getBoolean("fuelLeak")){
			tooltipLines.add(TextFormatting.RED + I18n.format("info.item.engine.fuelleak"));
		}
		if(stackTag.getBoolean("brokenStarter")){
			tooltipLines.add(TextFormatting.RED + I18n.format("info.item.engine.brokenstarter"));
		}
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems){
		if(MTSCreativeTabs.tabMTS.equals(tab)){
			subItems.add(getStackWithData(this, false));
			subItems.add(getStackWithData(this, true));
		}
    }
	
	public static ItemStack getStackWithData(ItemEngine item, boolean creative){
		ItemStack stack = new ItemStack(item);
		NBTTagCompound stackTag = new NBTTagCompound();
		stackTag.setInteger("maxRPM", item.getMaxRPM());
		stackTag.setInteger("maxSafeRPM", EntityEngine.getMaxSafeRPM(item.getMaxRPM()));
		stackTag.setFloat("fuelConsumption", creative ? 0.0F : item.getFuelConsumption());
		stackTag.setDouble("hours", 0);
		stack.setTagCompound(stackTag);
		return stack;
	} 
	
	public abstract int getMaxRPM();
	
	public abstract float getFuelConsumption();
	
	public static class ItemEngineCar extends ItemEngine{
		@Override
		public int getMaxRPM(){
			return 7500;
		}
		
		@Override
		public float getFuelConsumption(){
			return 0.5F;
		}
	}
	
	public static class ItemEngineAircraftSmall extends ItemEngine{
		@Override
		public int getMaxRPM(){
			return 2900;
		}
		
		@Override
		public float getFuelConsumption(){
			return 0.4F;
		}
	}
	
	public static class ItemEngineAircraftLarge extends ItemEngine{
		@Override
		public int getMaxRPM(){
			return 2400;
		}
		
		@Override
		public float getFuelConsumption(){
			return 0.7F;
		}
	}
}
