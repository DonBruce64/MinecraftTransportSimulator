package minecraftflightsimulator.dataclasses;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class MTSCreativeTabs extends CreativeTabs{
	
	public MTSCreativeTabs(String tabName){
		super(tabName);
	}

	@Override
    @SideOnly(Side.CLIENT)
    public void displayAllReleventItems(List givenList){
    	super.displayAllReleventItems(givenList);
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
	    	return MTSRegistry.planeMC172;
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
