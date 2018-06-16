package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartGroundDevice extends AItemPart{
	
	public ItemPartGroundDevice(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValueInRange(float minValue, float maxValue){
		float diameter = PackParserSystem.getPartPack(partName).groundDevice.diameter;
		return minValue <= diameter && maxValue >= diameter;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartGroundDevice) stack.getItem()).partName); 
		tooltipLines.add(I18n.format("info.item.ground_device.diameter") + pack.groundDevice.diameter*1.5F);
		tooltipLines.add(I18n.format("info.item.ground_device.motivefriction") + pack.groundDevice.motiveFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.lateralfriction") + pack.groundDevice.lateralFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.rotatesonshaft_" + String.valueOf(pack.groundDevice.rotatesOnShaft)));
		tooltipLines.add(I18n.format("info.item.ground_device.canfloat_" + String.valueOf(pack.groundDevice.canFloat)));
	}
}
