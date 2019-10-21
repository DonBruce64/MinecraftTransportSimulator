package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemJerrycan extends Item{
	public ItemJerrycan(){
		super();
		setFull3D();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		tooltipLines.add(I18n.format("info.item.jerrycan.fill"));
		tooltipLines.add(I18n.format("info.item.jerrycan.drain"));
		if(stack.hasTagCompound() && stack.getTagCompound().getBoolean("isFull")){
			tooltipLines.add(I18n.format("info.item.jerrycan.contains") + stack.getTagCompound().getString("fluidName"));
		}else{
			tooltipLines.add(I18n.format("info.item.jerrycan.empty"));
		}
	}
}
