package minecraftflightsimulator.items;

import java.util.List;

import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemWrench extends Item{
	public ItemWrench(){
		super();
		setFull3D();
	}
	
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean p_77624_4_){
		list.add(PlayerHelper.getTranslatedText("info.item.wrench.use"));
		list.add(PlayerHelper.getTranslatedText("info.item.wrench.attack"));
		list.add(PlayerHelper.getTranslatedText("info.item.wrench.sneakattack"));
	}
}
