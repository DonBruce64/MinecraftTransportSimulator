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

public class ItemPartGun extends AItemPart{
	
	public ItemPartGun(JSONPart definition){
		super(definition);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		return packPart.minValue <= definition.gun.diameter && packPart.maxValue >= definition.gun.diameter ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		super.addInformation(stack, world, tooltipLines, flagIn);
		tooltipLines.add(I18n.format("info.item.gun.type." + definition.general.type.substring("gun_".length())));
		tooltipLines.add(I18n.format("info.item.gun.diameter") + definition.gun.diameter);
		tooltipLines.add(I18n.format("info.item.gun.length") + definition.gun.length);
		tooltipLines.add(I18n.format("info.item.gun.fireDelay") + definition.gun.fireDelay);
		tooltipLines.add(I18n.format("info.item.gun.muzzleVelocity") + definition.gun.muzzleVelocity);
		tooltipLines.add(I18n.format("info.item.gun.capacity") + definition.gun.capacity);
		if(definition.gun.autoReload){
			tooltipLines.add(I18n.format("info.item.gun.autoReload"));
		}
		tooltipLines.add(I18n.format("info.item.gun.yawRange") + definition.gun.minYaw + "-" + definition.gun.maxYaw);
		tooltipLines.add(I18n.format("info.item.gun.pitchRange") + definition.gun.minPitch + "-" + definition.gun.maxPitch);
	}
}
