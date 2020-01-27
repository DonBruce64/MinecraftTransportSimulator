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

public class ItemPartCrate extends AItemPart{
	
	public ItemPartCrate(JSONPart definition){
		super(definition);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		return packPart.minValue <= definition.crate.rows && packPart.maxValue >= definition.crate.rows ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		super.addInformation(stack, world, tooltipLines, flagIn);
		tooltipLines.add(I18n.format("info.item.crate.capacity") + definition.crate.rows*9);
	}
}
