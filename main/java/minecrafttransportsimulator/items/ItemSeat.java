package minecrafttransportsimulator.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSeat extends Item{
	private static final byte numberSeats = 102;
	
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
}
