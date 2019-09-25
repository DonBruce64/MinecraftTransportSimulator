package minecrafttransportsimulator.dataclasses;

import net.minecraft.util.NonNullList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Core mod creative tab class.  This is responsible for holding MTS core items/blocks
 * such as the crafting blocks, instruments, manual, wrench, etc.
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
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(NonNullList<ItemStack> givenList){
		givenList.clear();
		for(Item item : MTSRegistry.itemList){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(tab.equals(this)){
					item.getSubItems(tab, givenList);
				}
			}
		}
    }
}
