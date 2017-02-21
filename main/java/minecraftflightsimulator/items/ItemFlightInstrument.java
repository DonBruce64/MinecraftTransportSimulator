package minecraftflightsimulator.items;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.rendering.AircraftInstruments;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

public class ItemFlightInstrument extends Item{
	private IIcon[] icons = new IIcon[AircraftInstruments.AircraftGauges.values().length];
	
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
	public void addInformation(ItemStack item, EntityPlayer player, List list, boolean p_77624_4_){
		list.add(StatCollector.translateToLocal("item.flightinstrument" + item.getItemDamage() + ".description"));
	}
	//DEL180START
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register){
    	for(int i=0; i<AircraftInstruments.AircraftGauges.values().length; ++i){
    		icons[0] = register.registerIcon("mfs:flightinstrument" + i);
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
    	return this.icons[damage > AircraftInstruments.AircraftGauges.values().length ? 0 : damage];
    }
    //DEL180END
}
