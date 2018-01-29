package minecrafttransportsimulator.items;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemWrench extends Item{
	public ItemWrench(){
		super();
		setFull3D();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		tooltipLines.add(I18n.format("info.item.wrench.use"));
		tooltipLines.add(I18n.format("info.item.wrench.attack"));
		tooltipLines.add(I18n.format("info.item.wrench.sneakattack"));
	}
}
