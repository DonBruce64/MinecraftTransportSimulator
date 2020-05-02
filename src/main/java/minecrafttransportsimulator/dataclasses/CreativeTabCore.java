package minecrafttransportsimulator.dataclasses;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**Core mod creative tab class.  This is responsible for holding MTS core items/blocks
 * such as the crafting blocks, instruments, manual, wrench, etc.  While it would be
 * nice to make the core mod just another pack, that's not really possible due to the fact
 * some core mod items aren't pack-based, so they won't go into the pack lists.
 * 
 * @author don_bruce
 */
public final class CreativeTabCore extends CreativeTabs{
	
	public CreativeTabCore(){
		super("tabMTSCore");
	}
	
	@Override
	public ItemStack getTabIconItem(){
		return new ItemStack(MTSRegistry.wrench);
	}

	@Override
    public void displayAllRelevantItems(NonNullList<ItemStack> givenList){
		givenList.clear();
		for(Item item : MTSRegistry.coreItems){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(this.equals(tab)){
					item.getSubItems(tab, givenList);
				}
			}
		}
    }
}
