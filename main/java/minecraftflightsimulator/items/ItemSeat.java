package minecraftflightsimulator.items;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemSeat extends Item{
	private IIcon[] icons = new IIcon[96];

	public ItemSeat(){
		this.hasSubtypes=true;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(int i=0; i<16; ++i){
			for(int j=0; j<6; ++j){
				itemList.add(new ItemStack(item, 1, (j << 4) + i));
			}
		}
    }
	
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register){
    	for(int i=0; i<96; ++i){
    		icons[i] = register.registerIcon("mfs:seat" + i);
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
        return this.icons[damage > 95 ? 0 : damage];
    }
}
