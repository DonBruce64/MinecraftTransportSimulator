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

public final class ItemEngine extends Item{
	private final Engines engineItem;
	
	public ItemEngine(Engines engineItem){
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
		this.engineItem = engineItem;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		NBTTagCompound stackTag = stack.getTagCompound();
		if(stackTag.getFloat("fuelConsumption") == 0){
			tooltipLines.add(TextFormatting.DARK_PURPLE + I18n.format("info.item.engine.creative"));
		}
		tooltipLines.add(stackTag.getBoolean("isAutomatic") ? I18n.format("info.item.engine.automatic") : I18n.format("info.item.engine.manual"));
		tooltipLines.add(I18n.format("info.item.engine.numbergears") + stackTag.getByte("numberGears"));
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
		stackTag.setBoolean("isAutomatic", item.engineItem.isAutomatic);
		stackTag.setByte("numberGears", item.engineItem.numberGears);
		stackTag.setByte("starterPower", item.engineItem.starterPower);
		stackTag.setByte("starterIncrement", item.engineItem.starterIncrement);
		stackTag.setInteger("maxRPM", item.engineItem.maxRPM);
		stackTag.setInteger("maxSafeRPM", EntityEngine.getMaxSafeRPM(item.engineItem.maxRPM));
		stackTag.setFloat("fuelConsumption", creative ? 0.0F : item.engineItem.fuelConsumption);
		stackTag.setDouble("hours", 0);
		stack.setTagCompound(stackTag);
		return stack;
	}
	
	
	public enum Engines{
		LYCOMING_O360(true, (byte) 1, (byte) 50, (byte) 4, 2900, 0.4F),
		BRISTOL_MERCURY(true, (byte) 1, (byte) 25, (byte) 13, 2900, 0.4F),
		AMC_I4(true, (byte) 4, (byte) 50, (byte) 3, 7500, 0.5F),
		DETROIT_DIESEL(false, (byte) 9, (byte) 25, (byte) 4, 3500, 0.75F);
		
		private final boolean isAutomatic;
		private final byte numberGears;
		private final byte starterPower;
		private final byte starterIncrement;
		private final int maxRPM;
		private final float fuelConsumption;
		
		private Engines(boolean isAutomatic, byte numberGears, byte starterPower, byte starterIncrement, int maxRPM, float fuelConsumption){
			this.isAutomatic = isAutomatic;
			this.numberGears = numberGears;
			this.starterPower = starterPower;
			this.starterIncrement = starterIncrement;
			this.maxRPM = maxRPM;
			this.fuelConsumption = fuelConsumption;
		}
	}
}
