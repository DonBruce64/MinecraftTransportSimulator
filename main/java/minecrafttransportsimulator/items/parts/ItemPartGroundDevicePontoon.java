package minecrafttransportsimulator.items.parts;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartGroundDevicePontoon extends AItemPart{
	
	public ItemPartGroundDevicePontoon(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(PackPart packPart){
		float width = PackParserSystem.getPartPack(partName).pontoon.width;
		return packPart.minValue <= width && packPart.maxValue >= width ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartGroundDevicePontoon) stack.getItem()).partName); 
		tooltipLines.add(I18n.format("info.item.ground_device.diameter") + pack.pontoon.width);
		tooltipLines.add(I18n.format("info.item.ground_device.motivefriction") + 0);
		tooltipLines.add(I18n.format("info.item.ground_device.lateralfriction") + pack.pontoon.lateralFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.rotatesonshaft_false"));
		tooltipLines.add(I18n.format("info.item.ground_device.canfloat_true"));
	}
}
