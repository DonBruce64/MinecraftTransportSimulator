package minecrafttransportsimulator.items.parts;

import java.util.List;

import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPartBarrel extends AItemPart{
	
	public ItemPartBarrel(String partName){
		super(partName);
	}
	
	@Override
	public boolean isPartValidForPackDef(PackPart packPart){
		int capacity = PackParserSystem.getPartPack(partName).barrel.capacity/1000;
		return packPart.minValue <= capacity && packPart.maxValue >= capacity ? super.isPartValidForPackDef(packPart) : false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		PackPartObject pack = PackParserSystem.getPartPack(((ItemPartBarrel) stack.getItem()).partName); 
		tooltipLines.add(I18n.format("info.item.barrel.capacity") + pack.barrel.capacity + "mb");
	}
}
