package minecrafttransportsimulator.items.parts;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartPropeller extends AItemPart{
	
	public ItemPartPropeller(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		float propellerDiameter = PackParserSystem.getPartPack(partName).propeller.diameter;
		return packPart.minValue <= propellerDiameter && packPart.maxValue >= propellerDiameter ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		JSONPart pack = PackParserSystem.getPartPack(((ItemPartPropeller) stack.getItem()).partName); 
		tooltipLines.add(I18n.format(pack.propeller.isDynamicPitch ? "info.item.propeller.dynamicPitch" : "info.item.propeller.staticPitch"));
		tooltipLines.add(I18n.format("info.item.propeller.numberBlades") + pack.propeller.numberBlades);
		tooltipLines.add(I18n.format("info.item.propeller.pitch") + pack.propeller.pitch);
		tooltipLines.add(I18n.format("info.item.propeller.diameter") + pack.propeller.diameter);
		tooltipLines.add(I18n.format("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*pack.propeller.diameter)));
		if(stack.hasTagCompound()){
			tooltipLines.add(I18n.format("info.item.propeller.health") + (pack.propeller.startingHealth - stack.getTagCompound().getFloat("damage")));
		}else{
			tooltipLines.add(I18n.format("info.item.propeller.health") + pack.propeller.startingHealth);
		}
	}
}
