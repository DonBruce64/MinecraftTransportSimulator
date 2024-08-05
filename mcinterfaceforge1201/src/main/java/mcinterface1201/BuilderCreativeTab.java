package mcinterface1201;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
        super(
                CreativeModeTab.builder()
                        .title(Component.literal(name))
                        .icon(() -> tabItem != null ? new ItemStack(BuilderItem.itemMap.get(tabItem)) : null)
        );
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
    @OnlyIn(Dist.CLIENT)
    public Component getDisplayName() {
        return Component.literal(label);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ItemStack getIconItem() {
        if (tabItem != null) {
            return super.getIconItem();
        } else {
            return new ItemStack(items.get((int) (System.currentTimeMillis() / 1000 % items.size())));
        }
    }

    public static void onFillCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if(event.getTab() instanceof BuilderCreativeTab tab) {
            for(Item item : tab.items) {
                event.accept(() -> item);
            }
        }
    }
}
