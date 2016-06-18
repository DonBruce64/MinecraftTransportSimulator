package minecraftflightsimulator.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemEngineLarge extends Item{
	
	public ItemEngineLarge(){
		this.hasSubtypes=true;
	}
	
	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List list, boolean p_77624_4_){
		list.add("Model# " + item.getItemDamage());
		list.add("Max RPM: " + (item.getItemDamage()/((int) 100))*100);
		list.add("Fuel consumption: " + (item.getItemDamage()%100)/10F);
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
        itemList.add(new ItemStack(item, 1, 2907));
        itemList.add(new ItemStack(item, 1, 3210));
    }
}
