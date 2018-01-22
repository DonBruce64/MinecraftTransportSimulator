package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.ItemMultipartMoving;
import minecrafttransportsimulator.systems.PackParserSystem.MultipartTypes;
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
			multipartList = getAllRegisteredItemsForType();
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
	
	protected abstract MultipartTypes getTabType(); 
		
	private List<ItemMultipartMoving> getAllRegisteredItemsForType(){
		List<ItemMultipartMoving> movingList = new ArrayList<ItemMultipartMoving>();
		for(ItemMultipartMoving movingItem : MTSRegistry.multipartItemMap.values()){
			movingList.add(movingItem);
		}
		return movingList;
	}
		
	public static final CreativeTabs tabMTSPlanes = new MTSCreativeTabs("tabMTSPlanes"){
		@Override
		public Item getTabIconItem(){
			return MTSRegistry.engineAircraftLarge;
		}
		
		@Override
		protected MultipartTypes getTabType(){
			return MultipartTypes.PLANE;
		}
	};
	
	public static CreativeTabs[] getAllCreativeTabs(){
		List<CreativeTabs> tabList = new ArrayList<CreativeTabs>();
		for(Field field : MTSCreativeTabs.class.getFields()){
			if(field.getType().equals(CreativeTabs.class)){
				try{
					tabList.add((CreativeTabs) field.get(CreativeTabs.class));
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		return tabList.toArray(new CreativeTabs[tabList.size()]);
	}
}
