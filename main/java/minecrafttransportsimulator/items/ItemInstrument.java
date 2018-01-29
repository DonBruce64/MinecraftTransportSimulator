package minecrafttransportsimulator.items;

import java.util.List;

import minecrafttransportsimulator.dataclasses.MTSInstruments;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemInstrument extends Item{
	
	public ItemInstrument(){
		this.hasSubtypes = true;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack){
	    return stack.getItemDamage() < MTSInstruments.Instruments.values().length ? this.getUnlocalizedName() + "_" + MTSInstruments.Instruments.values()[stack.getItemDamage()].name().toLowerCase() : "_invalid";
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_){
		tooltipLines.add(I18n.format(this.getUnlocalizedName(stack) + ".description"));
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems){
		//Iterate though all the instruments and put them on this tab.
		for(Instruments instrument : MTSInstruments.Instruments.values()){
			subItems.add(new ItemStack(this, 1, instrument.ordinal()));
		}
    }
}
