package minecrafttransportsimulator.items;

import java.util.List;

import minecrafttransportsimulator.dataclasses.MTSCreativeTabs;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSeat extends ItemPart{
	private static final byte numberSeats = 102;
	
	public ItemSeat(){
		super(EntitySeat.class);
		this.hasSubtypes=true;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack){
	    return stack.getItemDamage() < numberSeats ? this.getUnlocalizedName() : this.getUnlocalizedName() + "_invalid";
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems){
		if(MTSCreativeTabs.tabMTSParts.equals(tab)){
			for(int i=0; i<numberSeats; ++i){
				subItems.add(new ItemStack(this, 1, i));
			}
		}
    }
}
