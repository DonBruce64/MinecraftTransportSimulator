package minecrafttransportsimulator.dataclasses;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public abstract class MTSCreativeTabs extends CreativeTabs{
	
	public MTSCreativeTabs(String tabName){
		super(tabName);
	}

	@Override
    @SideOnly(Side.CLIENT)
    public void displayAllRelevantItems(List givenList){
    	super.displayAllRelevantItems(givenList);
    	ItemStack[] itemArray = (ItemStack[]) givenList.toArray(new ItemStack[givenList.size()]); 
    	int currentIndex = 0;
    	for(int i=0; i<MTSRegistry.itemList.size(); ++i){
    		for(int j=0; j<givenList.size(); ++j){
    			if(MTSRegistry.itemList.get(i).equals(itemArray[j].getItem())){
    				if(MTSRegistry.itemList.get(i).getCreativeTab().getTabLabel().equals(this.getTabLabel())){
    					givenList.set(currentIndex++, itemArray[j]);
    				}
    			}
    		}
    	}
    }

	
	public static final CreativeTabs tabMTSPlanes = new MTSCreativeTabs("tabMTSPlanes"){
	    @Override
		@SideOnly(Side.CLIENT)
	    public Item getTabIconItem(){
	    	return MTSRegistry.engineAircraftLarge;
	    }
	};
	
	public static final CreativeTabs tabMTSTrains = new MTSCreativeTabs("tabMTSTrains"){
	    @Override
		@SideOnly(Side.CLIENT)
	    public Item getTabIconItem(){
	    	return MTSRegistry.track;
	    }
	};
}
