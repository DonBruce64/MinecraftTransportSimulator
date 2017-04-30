package minecrafttransportsimulator.items;

import java.util.List;

import minecrafttransportsimulator.MTS;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSeat extends Item{
	private static final byte numberSeats = 102;
	private IIcon[] icons = new IIcon[numberSeats];

	public ItemSeat(){
		this.hasSubtypes=true;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(int i=0; i<numberSeats; ++i){
			itemList.add(new ItemStack(item, 1, i));
		}
    }
	//DEL180START
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register){
    	for(int i=0; i<numberSeats; ++i){
    		icons[i] = register.registerIcon(MTS.MODID + ":seat" + i);
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
        return this.icons[damage >= numberSeats ? 0 : damage];
    }
    //DEL180END
}
