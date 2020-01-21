package minecrafttransportsimulator.items.parts;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

import minecrafttransportsimulator.jsondefs.PackPartObject;
import minecrafttransportsimulator.jsondefs.PackVehicleObject.PackPart;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartGroundDeviceTread extends AItemPart{
	
	public ItemPartGroundDeviceTread(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(PackPart packPart){
		float width = PackParserSystem.getPartPack(partName).tread.width;
		return packPart.minValue <= width && packPart.maxValue >= width ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartGroundDeviceTread) stack.getItem()).partName); 
		tooltipLines.add(I18n.format("info.item.ground_device.diameter") + pack.tread.width);
		tooltipLines.add(I18n.format("info.item.ground_device.motivefriction") + pack.tread.motiveFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.lateralfriction") + pack.tread.lateralFriction);
		tooltipLines.add(I18n.format("info.item.ground_device.rotatesonshaft_true"));
		tooltipLines.add(I18n.format("info.item.ground_device.canfloat_false"));
	}
}
