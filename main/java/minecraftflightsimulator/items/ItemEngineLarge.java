package minecraftflightsimulator.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class ItemEngineLarge extends ItemEngine{

	public ItemEngineLarge(){
		super();
		this.setUnlocalizedName("LargeEngine");
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
        itemList.add(new ItemStack(item, 1, 2907));
        itemList.add(new ItemStack(item, 1, 3210));
    }
}
