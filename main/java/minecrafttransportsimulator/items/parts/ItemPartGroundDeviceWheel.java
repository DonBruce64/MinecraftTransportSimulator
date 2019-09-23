package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartGroundDeviceWheel extends AItemPart{
	
	public ItemPartGroundDeviceWheel(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(PackPart packPart){
		float diameter = PackParserSystem.getPartPack(partName).wheel.diameter;
		return packPart.minValue <= diameter && packPart.maxValue >= diameter ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartGroundDeviceWheel) stack.getItem()).partName); 
		tooltipLines.add(I18n.format("info.item.ground_device.diameter") + pack.wheel.diameter);
		tooltipLines.add(I18n.format("info.item.ground_device.motivefriction") + pack.wheel.motiveFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.lateralfriction") + pack.wheel.lateralFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.rotatesonshaft_true"));
		tooltipLines.add(I18n.format("info.item.ground_device.canfloat_false"));
	}
}
