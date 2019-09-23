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

public class ItemPartGroundDeviceSkid extends AItemPart{
	
	public ItemPartGroundDeviceSkid(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(PackPart packPart){
		float width = PackParserSystem.getPartPack(partName).skid.width;
		return packPart.minValue <= width && packPart.maxValue >= width ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartGroundDeviceSkid) stack.getItem()).partName); 
		tooltipLines.add(I18n.format("info.item.ground_device.diameter") + pack.skid.width);
		tooltipLines.add(I18n.format("info.item.ground_device.motivefriction") + 0);
		tooltipLines.add(I18n.format("info.item.ground_device.lateralfriction") + pack.skid.lateralFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.rotatesonshaft_false"));
		tooltipLines.add(I18n.format("info.item.ground_device.canfloat_false"));
	}
}
