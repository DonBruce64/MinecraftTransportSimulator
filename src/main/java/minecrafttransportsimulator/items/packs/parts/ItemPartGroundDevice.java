package minecrafttransportsimulator.items.packs.parts;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartGroundDevice extends AItemPart{
	
	public ItemPartGroundDevice(JSONPart definition){
		super(definition);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		return packPart.minValue <= definition.ground.height && packPart.maxValue >= definition.ground.height ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		super.addInformation(stack, world, tooltipLines, flagIn);
		tooltipLines.add(I18n.format("info.item.ground_device.diameter") + definition.ground.height);
		tooltipLines.add(I18n.format("info.item.ground_device.motivefriction") + definition.ground.motiveFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.lateralfriction") + definition.ground.lateralFriction);
		tooltipLines.add(I18n.format(definition.ground.isWheel ? "info.item.ground_device.rotatesonshaft_true" : "info.item.ground_device.rotatesonshaft_false"));
		tooltipLines.add(I18n.format(definition.ground.canFloat ? "info.item.ground_device.canfloat_true" : "info.item.ground_device.canfloat_false"));
	}
}
