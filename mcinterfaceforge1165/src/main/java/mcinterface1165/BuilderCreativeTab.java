package mcinterface1165;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Builder for a MC creative tabs.  This class interfaces with the MC creative tab system,
 * allowing for items to be stored in it for rendering via the tab.  The item rendered as
 * the tab icon normally cycles between all items in the tab, but can be set to a member
 * item of the tab if desired.
 *
 * @author don_bruce
 */
public class BuilderCreativeTab extends ItemGroup {
    /**
     * Map of created tabs names linked to their builder instances.  Used for interface operations.
     **/
    protected static final Map<String, BuilderCreativeTab> createdTabs = new HashMap<>();

    private final String label;
    private final Item itemIcon;
    private final List<Item> items = new ArrayList<>();

    BuilderCreativeTab(String name, BuilderItem mcItem) {
        super(name);
        this.label = name;
        this.itemIcon = mcItem;
    }

    /**
     * Adds the passed-in item to this tab.
     */
    public void addItem(AItemBase item, BuilderItem mcItem) {
        items.add(mcItem);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ITextComponent getDisplayName() {
        return new StringTextComponent(label);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack makeIcon() {
        return itemIcon != null ? new ItemStack(itemIcon) : null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack getIconItem() {
        if (itemIcon != null) {
            return super.getIconItem();
        } else {
            return new ItemStack(items.get((int) (System.currentTimeMillis() / 1000 % items.size())));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
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
