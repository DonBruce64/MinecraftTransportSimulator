package minecrafttransportsimulator.items.packs.parts;

import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartInteractable extends AItemPart{
	
	public ItemPartInteractable(JSONPart definition){
		super(definition);
	}
	
	@Override
	public boolean isPartValidForPackDef(VehiclePart packPart){
		return packPart.minValue <= definition.interactable.inventoryUnits && packPart.maxValue >= definition.interactable.inventoryUnits ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		super.addInformation(stack, world, tooltipLines, flagIn);
		if(definition.interactable.type.equals("crate")){
			tooltipLines.add(BuilderGUI.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*9);
		}else if(definition.interactable.type.equals("barrel")){
			tooltipLines.add(BuilderGUI.translate("info.item.interactable.capacity") + definition.interactable.inventoryUnits*1000 + "mb");
		}
	}
}
