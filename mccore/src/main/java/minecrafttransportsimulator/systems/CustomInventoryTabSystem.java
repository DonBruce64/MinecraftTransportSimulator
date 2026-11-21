package minecrafttransportsimulator.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.jsondefs.JSONPack.JSONCustomInventoryTab;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Version-independent rules for pack-authored creative-inventory side tabs.
 * Rendering and input live in the version adapters, but all adapters use these
 * methods so malformed assignments have identical fallback behavior.
 */
public final class CustomInventoryTabSystem {
    /** -1 denotes the normal/default pack tab. */
    public static final int DEFAULT_TAB = -1;

    private CustomInventoryTabSystem() {}

    /** Returns true when the client-side feature is available and enabled. */
    public static boolean isEnabled() {
        return ConfigSystem.client != null && ConfigSystem.client.renderingSettings != null && ConfigSystem.client.renderingSettings.customInventoryTabs != null && ConfigSystem.client.renderingSettings.customInventoryTabs.value;
    }

    /**
     * Returns valid, ordered definitions for a pack.  Null entries and entries
     * without a usable name cannot be selected, so they are ignored without
     * shifting item assignments to another named group.
     */
    public static List<JSONCustomInventoryTab> getTabs(String packID) {
        JSONPack pack = packID != null ? PackParser.getPackConfiguration(packID) : null;
        if (pack == null || pack.customInventoryTabs == null || pack.customInventoryTabs.isEmpty()) {
            return Collections.emptyList();
        }
        List<JSONCustomInventoryTab> validTabs = new ArrayList<>();
        for (JSONCustomInventoryTab tab : pack.customInventoryTabs) {
            if (tab != null && tab.name != null && !tab.name.trim().isEmpty()) {
                validTabs.add(tab);
            }
        }
        return validTabs;
    }

    /** Returns whether an item belongs in the selected side tab. */
    public static boolean isItemVisible(AItemPack<?> item, String displayedPackID, int selectedTab) {
        if (item == null || item.definition == null || item.definition.general == null) {
            return selectedTab == DEFAULT_TAB;
        }
        Object configuredGroups = item instanceof AItemSubTyped && ((AItemSubTyped<?>) item).subDefinition.customTabGroups != null ? ((AItemSubTyped<?>) item).subDefinition.customTabGroups : item.definition.general.customTabGroups;
        List<String> groupNames = getGroupNames(configuredGroups);
        if (groupNames.isEmpty()) {
            return selectedTab == DEFAULT_TAB;
        }

        boolean includeDefault = false;
        boolean foundValidTab = false;
        boolean foundSelectedTab = false;
        List<JSONCustomInventoryTab> tabs = getTabs(displayedPackID);
        for (String groupName : groupNames) {
            if ("default".equals(groupName)) {
                includeDefault = true;
                continue;
            }
            int matchingIndex = DEFAULT_TAB;
            for (int i = 0; i < tabs.size(); ++i) {
                if (groupName.equals(tabs.get(i).name)) {
                    if (matchingIndex != DEFAULT_TAB) {
                        matchingIndex = DEFAULT_TAB;
                        break;
                    }
                    matchingIndex = i;
                }
            }
            if (matchingIndex != DEFAULT_TAB) {
                foundValidTab = true;
                foundSelectedTab |= matchingIndex == selectedTab;
            }
        }

        if (selectedTab == DEFAULT_TAB) {
            return includeDefault || !foundValidTab;
        }
        return foundSelectedTab;
    }

    /** Resolves an optional item-model icon, or null when the declaration is absent or invalid. */
    public static AItemPack<?> getIconItem(String displayedPackID, JSONCustomInventoryTab tab) {
        if (tab == null || tab.itemIcon == null || tab.itemIcon.trim().isEmpty()) {
            return null;
        }
        String iconPackID = displayedPackID;
        String iconName = tab.itemIcon.trim();
        int namespaceSeparator = iconName.indexOf(':');
        if (namespaceSeparator >= 0) {
            iconPackID = iconName.substring(0, namespaceSeparator);
            iconName = iconName.substring(namespaceSeparator + 1);
        }
        return iconPackID.isEmpty() || iconName.isEmpty() ? null : PackParser.getItem(iconPackID, iconName);
    }

    private static List<String> getGroupNames(Object configuredGroups) {
        if (configuredGroups instanceof String) {
            String group = (String) configuredGroups;
            return group.isEmpty() ? Collections.emptyList() : Collections.singletonList(group);
        }
        if (configuredGroups instanceof List) {
            List<String> groupNames = new ArrayList<>();
            for (Object group : (List<?>) configuredGroups) {
                if (group instanceof String && !((String) group).isEmpty()) {
                    groupNames.add((String) group);
                }
            }
            return groupNames;
        }
        return Collections.emptyList();
    }

    /**
     * Converts the flexible JSON icon syntax to a namespaced texture path.
     * The returned pair is namespace then path and avoids referencing any
     * Minecraft ResourceLocation class from common code.
     */
    public static String[] getIconResource(String displayedPackID, JSONCustomInventoryTab tab) {
        String namespace = displayedPackID;
        String path = tab != null && tab.icon != null ? tab.icon.trim().replace('\\', '/') : "";
        int namespaceSeparator = path.indexOf(':');
        if (namespaceSeparator >= 0) {
            namespace = path.substring(0, namespaceSeparator);
            path = path.substring(namespaceSeparator + 1);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String assetPrefix = "assets/" + namespace + "/";
        if (path.startsWith(assetPrefix)) {
            path = path.substring(assetPrefix.length());
        }
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path += ".png";
        }
        return new String[] {namespace, path};
    }
}
