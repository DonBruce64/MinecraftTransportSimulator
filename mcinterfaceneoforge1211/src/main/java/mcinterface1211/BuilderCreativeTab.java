package mcinterface1211;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.world.item.CreativeModeTab;

/** Associates the registry-backed 1.21 creative tabs with their IV pack IDs. */
public final class BuilderCreativeTab {
    private static final Map<CreativeModeTab, String> PACK_IDS = new IdentityHashMap<>();

    private BuilderCreativeTab() {}

    public static CreativeModeTab register(String packID, CreativeModeTab tab) {
        PACK_IDS.put(tab, packID);
        return tab;
    }

    public static String getPackID(CreativeModeTab tab) {
        return PACK_IDS.get(tab);
    }
}
