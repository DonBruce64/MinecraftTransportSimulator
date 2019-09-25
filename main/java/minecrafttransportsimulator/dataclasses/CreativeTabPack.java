package minecrafttransportsimulator.dataclasses;

import net.minecraft.util.NonNullList;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.core.ItemVehicle;
import minecrafttransportsimulator.items.parts.AItemPart;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Pack-specific creative tab class.  One of each will be made for every pack
 * that loads into MTS.  These are held in the {@link MTSRegistry} along with the
 * core creative tab class.
 * 
 * @author don_bruce
 */
public final class CreativeTabPack extends CreativeTabs{
	
	public CreativeTabPack(String modID){
		super(modID);
	}
	
	@Override
	public ItemStack getTabIconItem(){
		return new ItemStack(MTSRegistry.wrench);
	}

	@Override
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(NonNullList<ItemStack> givenList){
		//This is needed to re-sort the items here to get them in the correct order.
		//MC will re-order these by ID if we let it.
		givenList.clear();
		for(Item item : MTSRegistry.vehicleItemMap.values()){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(tab.equals(this)){
					item.getSubItems(tab, givenList);
				}
			}
		}
		for(Item item : MTSRegistry.partItemMap.values()){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(tab.equals(this)){
					item.getSubItems(tab, givenList);
				}
			}
		}
		for(Item item : MTSRegistry.instrumentItemMap.values()){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(tab.equals(this)){
					item.getSubItems(tab, givenList);
				}
			}
		}
		for(Item item : MTSRegistry.decorItemMap.values()){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(tab.equals(this)){
					item.getSubItems(tab, givenList);
				}
			}
		}
    }
	
	@Override
	@SideOnly(Side.CLIENT)
    public ItemStack getIconItemStack(){
		List<ItemStack> tabStacks = new ArrayList<ItemStack>();
		for(ItemVehicle vehicleItem : MTSRegistry.vehicleItemMap.values()){
			if(vehicleItem.getRegistryName().getResourceDomain().equals(getTabLabel())){
				tabStacks.add(new ItemStack(vehicleItem));
			}
		}
		if(tabStacks.isEmpty()){
			for(AItemPart partItem : MTSRegistry.partItemMap.values()){
				if(partItem.getRegistryName().getResourceDomain().equals(getTabLabel())){
					tabStacks.add(new ItemStack(partItem));
				}
			}
		}
		return tabStacks.get((int) (Minecraft.getMinecraft().world.getTotalWorldTime()/20%tabStacks.size()));
    }
}
