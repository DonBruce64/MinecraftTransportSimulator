package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.PackPartObject;
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
	public boolean isPartValueInRange(float minValue, float maxValue){
		float propellerDiameter = PackParserSystem.getPartPack(partName).propeller.diameter;
		return minValue <= propellerDiameter && maxValue >= propellerDiameter;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartPropeller) stack.getItem()).partName); 
		tooltipLines.add(I18n.format("info.item.propeller.numberBlades") + pack.propeller.numberBlades);
		tooltipLines.add(I18n.format("info.item.propeller.pitch") + pack.propeller.pitch);
		tooltipLines.add(I18n.format("info.item.propeller.diameter") + pack.propeller.diameter);
		tooltipLines.add(I18n.format("info.item.propeller.numberBlades") + pack.propeller.numberBlades);
		tooltipLines.add(I18n.format("info.item.propeller.maxrpm") + Math.round(60*340.29/(0.0254*Math.PI*pack.propeller.diameter)));
		if(stack.hasTagCompound()){
			tooltipLines.add(I18n.format("info.item.propeller.health") + (pack.propeller.startingHealth - stack.getTagCompound().getFloat("damage")));
		}else{
			tooltipLines.add(I18n.format("info.item.propeller.health") + pack.propeller.startingHealth);
		}
	}
}
