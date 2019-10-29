package minecrafttransportsimulator.items.parts;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.List;

import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackPartObject.PartBulletConfig;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartBullet extends AItemPart{
	public final PartBulletConfig bulletPackData;
	
	public ItemPartBullet(String partName){
		super(partName);
		this.bulletPackData = PackParserSystem.getPartPack(partName).bullet;
	}
	
	@Override
	public boolean isPartValidForPackDef(PackPart packPart){
		float bulletDiameter = PackParserSystem.getPartPack(partName).bullet.diameter;
		return packPart.minValue <= bulletDiameter && packPart.maxValue >= bulletDiameter ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartBullet) stack.getItem()).partName); 
		tooltipLines.add(I18n.format("info.item.bullet.type." + bulletPackData.type));
		tooltipLines.add(I18n.format("info.item.bullet.diameter") + pack.bullet.diameter);
		tooltipLines.add(I18n.format("info.item.bullet.quantity") + pack.bullet.quantity);
	}
}
