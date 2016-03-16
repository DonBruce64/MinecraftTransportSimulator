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

public class ItemPropeller extends Item {

	public ItemPropeller(){
		this.setUnlocalizedName("Propeller");
		this.setCreativeTab(MFS.tabMFS);
		this.hasSubtypes=true;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack){
	    return this.getUnlocalizedName() + "_" + stack.getItemDamage()%10;
	}
	
	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List list, boolean p_77624_4_){
		list.add("Model# " + item.getItemDamage());
		list.add("Blades: " + (item.getItemDamage()%100/10));
		list.add("Pitch: " + (55+3*(item.getItemDamage()%1000/100)));
		list.add("Diameter: " + (70+5*(item.getItemDamage()/1000)));
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
        itemList.add(new ItemStack(item, 1, 1120));
        itemList.add(new ItemStack(item, 1, 1121));
        itemList.add(new ItemStack(item, 1, 1122));
    }
	
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister p_94581_1_){}
}
