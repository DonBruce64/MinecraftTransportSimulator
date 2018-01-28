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
	private List<ItemMultipartMoving> multipartList;
	private ItemStack defaultStack;
	
	public MTSCreativeTabs(String tabName){
		super(tabName);
	}

	@Override
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(List givenList){
		givenList.clear();
		for(Item item : MTSRegistry.itemList){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(tab.equals(this)){
					item.getSubItems(item, tab, givenList);
				}
			}
		}
    }
	
	@Override
	@SideOnly(Side.CLIENT)
    public ItemStack getIconItemStack(){
		if(multipartList == null){
			multipartList = new ArrayList<ItemMultipartMoving>();
			for(ItemMultipartMoving movingItem : MTSRegistry.multipartItemMap.values()){
				multipartList.add(movingItem);
			}
		}
		if(multipartList.isEmpty()){
			if(defaultStack == null){
				defaultStack = new ItemStack(this.getTabIconItem());
			}
			return this.defaultStack;
		}else{
			return new ItemStack(multipartList.get((int) (Minecraft.getMinecraft().theWorld.getTotalWorldTime()/20%multipartList.size())));
		}
    }
		
	public static final CreativeTabs tabMTS = new MTSCreativeTabs("tabMTS"){
		@Override
		public Item getTabIconItem(){
			return MTSRegistry.engineAircraftLarge;
		}
	};
}
