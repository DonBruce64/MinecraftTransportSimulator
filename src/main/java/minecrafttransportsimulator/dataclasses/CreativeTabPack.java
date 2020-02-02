package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**Pack-specific creative tab class.  One of each will be made for every pack
 * that loads into MTS.  These are held in the {@link MTSRegistry} along with the
 * core creative tab class.
 * 
 * @author don_bruce
 */
public final class CreativeTabPack extends CreativeTabs{
	
	public CreativeTabPack(String packID){
		super(packID);
	}
	
	@Override
	public ItemStack getTabIconItem(){
		//We won't ever use this, but it keeps the compiler happy.
		return new ItemStack(MTSRegistry.wrench);
	}
	
	@Override
	public ItemStack getIconItemStack(){
		//Render cycling items.
		List<AItemPack<? extends AJSONItem<?>>> packItems = new ArrayList<AItemPack<? extends AJSONItem<?>>>(MTSRegistry.packItemMap.get(getTabLabel()).values());
		return new ItemStack(packItems.get((int) (Minecraft.getMinecraft().world.getTotalWorldTime()/20%packItems.size())));
	}

	@Override
    public void displayAllRelevantItems(NonNullList<ItemStack> givenList){
		//This is needed to re-sort the items here to get them in the correct order.
		//MC will re-order these by ID if we let it.  To prevent this, we swap MC's
		//internal list with our own, which ensures that the order is the order
		//we did registration in.
		givenList.clear();
		for(Item item : MTSRegistry.packItemMap.get(getTabLabel()).values()){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(tab.equals(this)){
					item.getSubItems(tab, givenList);
				}
			}
		}
    }
}
