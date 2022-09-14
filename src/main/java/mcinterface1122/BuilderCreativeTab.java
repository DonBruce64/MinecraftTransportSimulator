package mcinterface1122;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * Builder for a MC creative tabs.  This class interfaces with the MC creative tab system,
 * allowing for items to be stored in it for rendering via the tab.  The item rendered as
 * the tab icon normally cycles between all items in the tab, but can be set to a member
 * item of the tab if desired.
 *
 * @author don_bruce
 */
public class BuilderCreativeTab extends CreativeTabs {
    /**
     * Map of created tabs names linked to their builder instances.  Used for interface operations.
     **/
    protected static final Map<String, BuilderCreativeTab> createdTabs = new HashMap<>();

    private final Item itemIcon;
    private final List<Item> items = new ArrayList<>();

    BuilderCreativeTab(String name, BuilderItem mcItem) {
        super(name);
        this.itemIcon = mcItem;
    }

    /**
     * Adds the passed-in item to this tab.
     */
    public void addItem(AItemBase item, BuilderItem mcItem) {
        items.add(mcItem);
        mcItem.setCreativeTab(this);
    }

    @Override
    public String getTranslationKey() {
        return getTabLabel();
    }

    @Override
    public ItemStack createIcon() {
        return itemIcon != null ? new ItemStack(itemIcon) : null;
    }

    @Override
    public ItemStack getIcon() {
        if (itemIcon != null) {
            return super.getIcon();
        } else {
            return new ItemStack(items.get((int) (System.currentTimeMillis() / 1000 % items.size())));
        }
    }

    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> givenList) {
        //This is needed to re-sort the items here to get them in the correct order.
        //MC will re-order these by ID if we let it.  To prevent this, we swap MC's
        //internal list with our own, which ensures that the order is the order
        //we did registration in.
        givenList.clear();
        for (Item item : items) {
            for (CreativeTabs tab : item.getCreativeTabs()) {
                if (this.equals(tab)) {
                    item.getSubItems(tab, givenList);
                }
            }
        }
    }
}
