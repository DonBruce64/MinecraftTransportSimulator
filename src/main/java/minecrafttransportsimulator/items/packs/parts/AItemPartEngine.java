package minecrafttransportsimulator.items.packs.parts;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AItemPartEngine extends AItemPart{
	
	public AItemPartEngine(JSONPart definition){
		super(definition);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		return packPart.minValue <= definition.engine.fuelConsumption && packPart.maxValue >= definition.engine.fuelConsumption ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems){
		if(this.getCreativeTab().equals(tab)){
			subItems.add(new ItemStack(this));
			
			ItemStack engineStackCreative = new ItemStack(this);
			NBTTagCompound stackTag = new NBTTagCompound();
			stackTag.setBoolean("isCreative", true);
			engineStackCreative.setTagCompound(stackTag);
			subItems.add(engineStackCreative);
		}
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		NBTTagCompound stackTag = stack.getTagCompound();
		
		if(stackTag != null && stackTag.getBoolean("isCreative")){
			tooltipLines.add(TextFormatting.DARK_PURPLE + I18n.format("info.item.engine.creative"));
		}
		tooltipLines.add(I18n.format("info.item.engine.maxrpm") + definition.engine.maxRPM);
		tooltipLines.add(I18n.format("info.item.engine.maxsaferpm") + APartEngine.getSafeRPMFromMax(definition.engine.maxRPM));
		tooltipLines.add(I18n.format("info.item.engine.fuelconsumption") + definition.engine.fuelConsumption);
		tooltipLines.add(I18n.format("info.item.engine.fueltype") + definition.engine.fuelType);
		tooltipLines.add(I18n.format("info.item.engine.hours") + (stackTag != null ? Math.round(stackTag.getDouble("hours")*100D)/100D : 0));
		
		addExtraInformation(stack, definition, tooltipLines);
		
		if(stackTag != null){
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
	}
	
	@SideOnly(Side.CLIENT)
	protected abstract void addExtraInformation(ItemStack stack, JSONPart pack, List<String> tooltipLines);
}
