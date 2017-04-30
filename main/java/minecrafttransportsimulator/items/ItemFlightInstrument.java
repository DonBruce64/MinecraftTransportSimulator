package minecrafttransportsimulator.items;

import java.util.List;

import minecrafttransportsimulator.rendering.AircraftInstruments;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemFlightInstrument extends Item{	
	
	public ItemFlightInstrument(){
		this.hasSubtypes = true;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
	    return this.getUnlocalizedName() + stack.getItemDamage();
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(int i=0; i<AircraftInstruments.AircraftGauges.values().length; ++i){
			itemList.add(new ItemStack(item, 1, i));
		}
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack item, EntityPlayer player, List list, boolean p_77624_4_){
		list.add(I18n.format("item.flightinstrument" + item.getItemDamage() + ".description"));
	}
}
