package minecraftflightsimulator.items;

import java.util.ArrayList;
import java.util.List;

import minecraftflightsimulator.MFS;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemFlightInstrument extends Item{
	private static List<IIcon> iconList = new ArrayList<IIcon>();
	
	public ItemFlightInstrument(){
		super();
		this.setUnlocalizedName("FlightInstrument");
		this.setCreativeTab(MFS.tabMFS);
		this.hasSubtypes = true;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
	    return this.getUnlocalizedName() + stack.getItemDamage();
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(int i=0; i<15; ++i){
			if(i==8 || i==9 || i==13 || i==14){continue;}
			itemList.add(new ItemStack(item, 1, i));
		}
    }
	
	@Override
	public void addInformation(ItemStack item, EntityPlayer player, List list, boolean p_77624_4_){
		list.add(StatCollector.translateToLocal("item.FlightInstrument" + item.getItemDamage() + ".description"));
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register){
    	for(int i=0; i<15; ++i){
    		iconList.add(register.registerIcon("mfs:flightinstrument" + i));
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
    	return iconList.get(damage);
    }
}
