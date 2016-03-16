package minecraftflightsimulator.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class ItemEngineSmall extends ItemEngine{

	public ItemEngineSmall(){
		super();
		this.setUnlocalizedName("SmallEngine");
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
        itemList.add(new ItemStack(item, 1, 2805));
        itemList.add(new ItemStack(item, 1, 3007));
    }
}
