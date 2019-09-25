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

public final class ItemPartEngineAircraft extends AItemPartEngine{
	
	public ItemPartEngineAircraft(String partName){
		super(partName);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		NBTTagCompound stackTag = stack.getTagCompound();
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartEngineAircraft) stack.getItem()).partName); 
		
		if(stackTag != null && stackTag.getBoolean("isCreative")){
			tooltipLines.add(TextFormatting.DARK_PURPLE + I18n.format("info.item.engine.creative"));
		}
		tooltipLines.add(I18n.format("info.item.engine.maxrpm") + pack.engine.maxRPM);
		tooltipLines.add(I18n.format("info.item.engine.maxsaferpm") + APartEngine.getSafeRPMFromMax(pack.engine.maxRPM));
		tooltipLines.add(I18n.format("info.item.engine.fuelconsumption") + pack.engine.fuelConsumption);
		tooltipLines.add(I18n.format("info.item.engine.hours") + (stackTag != null ? Math.round(stackTag.getDouble("hours")*100D)/100D : 0));
		tooltipLines.add(I18n.format("info.item.engine.gearratios") + pack.engine.gearRatios[0]);
		
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
