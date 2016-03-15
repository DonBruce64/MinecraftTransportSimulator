package minecraftflightsimulator.items;

import java.util.List;

import minecraftflightsimulator.MFS;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemWheel extends Item{

	public ItemWheel(){
		this.setUnlocalizedName("Wheel");
		this.setCreativeTab(MFS.tabMFS);
		this.hasSubtypes=true;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
	    return this.getUnlocalizedName() + "_" + stack.getItemDamage();
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
        itemList.add(new ItemStack(item, 1, 0));
        itemList.add(new ItemStack(item, 1, 1));
    }
	
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister p_94581_1_){}
}
