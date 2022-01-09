package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPackMissing;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Builder for a MC creative tabs.  This class interfaces with the MC creative tab system,
 * allowing for items to be stored in it for rendering via the tab.  The item rendered as
 * the tab icon normally cycles between all items in the tab, but can be set to a member
 * item of the tab if desired.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class BuilderCreativeTab extends CreativeTabs{
	/**Map of created tabs names linked to their builder instances.  Used for interface operations.**/
	protected static final Map<String, BuilderCreativeTab> createdTabs = new HashMap<String, BuilderCreativeTab>();
	
	private Item itemIcon;
	private final List<Item> items = new ArrayList<Item>();
	
	BuilderCreativeTab(String name, BuilderItem mcItem){
		super(name);
		this.itemIcon = mcItem;
	}
	
	/**
     * Adds the passed-in item to this tab.
     */
	public void addItem(AItemBase item, BuilderItem mcItem){
		items.add(mcItem);
		mcItem.setCreativeTab(this);
    }
	
	@Override
	public String getTranslationKey(){
		return getTabLabel();
    }
	
	@Override
	public ItemStack createIcon(){
		return itemIcon != null ? new ItemStack(itemIcon) : null;
	}
	
	@Override
	public ItemStack getIcon(){
		if(itemIcon != null){
			return super.getIcon();
		}else{
			return new ItemStack(items.get((int) (System.currentTimeMillis()/1000%items.size())));
		}
	}

	@Override
    public void displayAllRelevantItems(NonNullList<ItemStack> givenList){
		//This is needed to re-sort the items here to get them in the correct order.
		//MC will re-order these by ID if we let it.  To prevent this, we swap MC's
		//internal list with our own, which ensures that the order is the order
		//we did registration in.
		givenList.clear();
		for(Item item : items){
			for(CreativeTabs tab : item.getCreativeTabs()){
				if(this.equals(tab)){
					item.getSubItems(tab, givenList);
				}
			}
		}
    }
	
	/**
     * Renders a warning on our tabs if there is no pack data.
     */
    @SubscribeEvent
    public static void on(DrawScreenEvent.Post event){
    	if(!PackParserSystem.arePacksPresent() && AGUIBase.activeInputGUI == null){
	    	if(event.getGui() instanceof GuiContainerCreative){
	    		GuiContainerCreative creativeScreen = (GuiContainerCreative) event.getGui();
	    		if(createdTabs.values().contains(CreativeTabs.CREATIVE_TAB_ARRAY[creativeScreen.getSelectedTabIndex()])){
	    			new GUIPackMissing();
	    		}
	    	}
    	}
    }
}
