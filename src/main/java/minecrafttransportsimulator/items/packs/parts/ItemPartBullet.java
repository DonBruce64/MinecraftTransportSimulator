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

public class ItemPartBullet extends AItemPart{
	
	public ItemPartBullet(JSONPart definition){
		super(definition);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		return packPart.minValue <= definition.bullet.diameter && packPart.maxValue >= definition.bullet.diameter ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		super.addInformation(stack, world, tooltipLines, flagIn);
		tooltipLines.add(I18n.format("info.item.bullet.type." + definition.bullet.type));
		tooltipLines.add(I18n.format("info.item.bullet.diameter") + definition.bullet.diameter);
		tooltipLines.add(I18n.format("info.item.bullet.quantity") + definition.bullet.quantity);
	}
}
