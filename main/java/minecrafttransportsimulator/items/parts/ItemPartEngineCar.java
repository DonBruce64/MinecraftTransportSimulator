package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemPartEngineCar extends AItemPartEngine{
	
	public ItemPartEngineCar(String partName){
		super(partName);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		NBTTagCompound stackTag = stack.getTagCompound();
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartEngineCar) stack.getItem()).partName); 
		
		if(stackTag.getBoolean("isCreative")){
			tooltipLines.add(TextFormatting.DARK_PURPLE + I18n.format("info.item.engine.creative"));
		}
		tooltipLines.add(I18n.format("info.item.engine.maxrpm") + pack.engine.maxRPM);
		tooltipLines.add(I18n.format("info.item.engine.maxsaferpm") + APartEngine.getSafeRPMFromMax(pack.engine.maxRPM));
		tooltipLines.add(I18n.format("info.item.engine.fuelconsumption") + pack.engine.fuelConsumption);
		tooltipLines.add(I18n.format("info.item.engine.hours") + Math.round(stackTag.getDouble("hours")*100D)/100D);
		tooltipLines.add(I18n.format("info.item.engine.gearratios"));
		for(byte i=1; i<pack.engine.gearRatios.length; i+=2){
			String gearRatios = String.valueOf(pack.engine.gearRatios[i]);
			if(i+1 < pack.engine.gearRatios.length){
				gearRatios += ",   " + String.valueOf(pack.engine.gearRatios[i+1]);
			}
			tooltipLines.add(gearRatios);
		}
		
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
