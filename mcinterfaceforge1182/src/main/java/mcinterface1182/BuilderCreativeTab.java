package mcinterface1182;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Builder for a MC creative tabs.  This class interfaces with the MC creative tab system,
 * allowing for items to be stored in it for rendering via the tab.  The item rendered as
 * the tab icon normally cycles between all items in the tab, but can be set to a member
 * item of the tab if desired.
 *
 * @author don_bruce
 */
public class BuilderCreativeTab extends CreativeModeTab {
    /**
     * Map of created tabs names linked to their builder instances.  Used for interface operations.
     **/
    protected static final Map<String, BuilderCreativeTab> createdTabs = new HashMap<>();

    private final String label;
    private final AItemPack<?> tabItem;
    private final List<Item> items = new ArrayList<>();

    BuilderCreativeTab(String name, AItemPack<?> tabItem) {
        super(name);
        this.label = name;
        //Need to delay turning this into a MC item since we may not yet have created a builder.
        this.tabItem = tabItem;
    }

    /**
     * Adds the passed-in item to this tab.
     */
    public void addItem(AItemBase item, BuilderItem mcItem) {
        items.add(mcItem);
    }

    @Override
    public Component getDisplayName() {
        return new TextComponent(label);
    }

    @Override
    public ItemStack makeIcon() {
        return tabItem != null ? new ItemStack(BuilderItem.itemMap.get(tabItem)) : null;
    }

    @Override
    public ItemStack getIconItem() {
        if (tabItem != null) {
            return super.getIconItem();
        } else {
            return new ItemStack(items.get((int) (System.currentTimeMillis() / 1000 % items.size())));
        }
    }

    @Override
    public void fillItemList(NonNullList<ItemStack> givenList) {
        //This is needed to re-sort the items here to get them in the correct order.
        //MC will re-order these by ID if we let it.  To prevent this, we swap MC's
        //internal list with our own, which ensures that the order is the order
        //we did registration in.
        givenList.clear();
        for (Item item : items) {
            item.fillItemCategory(this, givenList);
        }
    }
}
