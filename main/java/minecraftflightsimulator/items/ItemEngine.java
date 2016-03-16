package minecraftflightsimulator.items;

import java.util.List;

import minecraftflightsimulator.MFS;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class ItemEngine extends Item {

	public ItemEngine(){
		this.hasSubtypes=true;
		this.setCreativeTab(MFS.tabMFS);
	}
	
	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List list, boolean p_77624_4_){
		list.add("Model# " + item.getItemDamage());
		list.add("Max RPM: " + (item.getItemDamage()/((int) 100))*100);
		list.add("Fuel consumption: " + (item.getItemDamage()%100)/10F);
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister p_94581_1_){}
}
