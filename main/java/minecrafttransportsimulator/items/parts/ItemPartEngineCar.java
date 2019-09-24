package minecrafttransportsimulator.items.parts;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
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
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		NBTTagCompound stackTag = stack.getTagCompound();
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartEngineCar) stack.getItem()).partName); 
		
		if(stackTag != null && stackTag.getBoolean("isCreative")){
			tooltipLines.add(TextFormatting.DARK_PURPLE + I18n.format("info.item.engine.creative"));
		}
		tooltipLines.add(I18n.format("info.item.engine.maxrpm") + pack.engine.maxRPM);
		tooltipLines.add(I18n.format("info.item.engine.maxsaferpm") + APartEngine.getSafeRPMFromMax(pack.engine.maxRPM));
		tooltipLines.add(I18n.format("info.item.engine.fuelconsumption") + pack.engine.fuelConsumption);
		tooltipLines.add(I18n.format("info.item.engine.hours") + (stackTag != null ? Math.round(stackTag.getDouble("hours")*100D)/100D : 0));
		tooltipLines.add(pack.engine.isAutomatic ? I18n.format("info.item.engine.automatic") : I18n.format("info.item.engine.manual"));
		tooltipLines.add(I18n.format("info.item.engine.gearratios"));
		for(byte i=0; i<pack.engine.gearRatios.length; i+=3){
			String gearRatios = String.valueOf(pack.engine.gearRatios[i]);
			if(i+1 < pack.engine.gearRatios.length){
				gearRatios += ",   " + String.valueOf(pack.engine.gearRatios[i+1]);
			}
			if(i+2 < pack.engine.gearRatios.length){
				gearRatios += ",   " + String.valueOf(pack.engine.gearRatios[i+2]);
			}
			tooltipLines.add(gearRatios);
		}
		
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
}
