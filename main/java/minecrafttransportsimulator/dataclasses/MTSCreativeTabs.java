package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.ItemMultipartMoving;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class MTSCreativeTabs extends CreativeTabs{
	
	public MTSCreativeTabs(String tabName){
		super(tabName);
	}

	@Override
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(List<ItemStack> givenList){
		givenList.clear();
		for(Item item : MTSRegistry.itemList){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(tab.equals(this)){
					item.getSubItems(item, tab, givenList);
				}
			}
		}
    }
		
	public static final CreativeTabs tabMTSVehicles = new MTSCreativeTabs("tabMTSVehicles"){
		private final List<ItemMultipartMoving> multipartList = new ArrayList<ItemMultipartMoving>();
		
		@Override
		public Item getTabIconItem(){
			return MTSRegistry.wrench;
		}
		
		@Override
		@SideOnly(Side.CLIENT)
	    public ItemStack getIconItemStack(){
			if(multipartList.isEmpty()){
				if(!MTSRegistry.multipartItemMap.isEmpty()){
					for(ItemMultipartMoving movingItem : MTSRegistry.multipartItemMap.values()){
						multipartList.add(movingItem);
					}
				}else{
					return super.getIconItemStack();
				}
			}
			return new ItemStack(multipartList.get((int) (Minecraft.getMinecraft().theWorld.getTotalWorldTime()/20%multipartList.size())));
	    }
	};
	
	public static final CreativeTabs tabMTSParts = new MTSCreativeTabs("tabMTSParts"){
		@Override
		public Item getTabIconItem(){
			return MTSRegistry.wrench;
		}
	};
}
