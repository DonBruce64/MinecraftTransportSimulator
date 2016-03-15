package minecraftflightsimulator.items;

import java.util.List;

import minecraftflightsimulator.MFS;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemSeat extends Item {

	public ItemSeat(){
		this.setUnlocalizedName("Seat");
		this.setCreativeTab(MFS.tabMFS);
		this.hasSubtypes=true;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(int i=0; i<6; ++i){
			for(int j=0; j<16; ++j){
				itemList.add(new ItemStack(item, 1, i + (j << 3)));
			}
		}
    }
	
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister p_94581_1_){}
}
