package minecrafttransportsimulator.jsondefs;

import java.util.List;
import java.util.Map;

/**
 * Custom class designed to hold crafting overrides.  Split from the main config file
 * to prevent nuking the file if the config loading fails.  We can do without crafting
 * overrides just fine.  Not so for configs.
 *
 * @author don_bruce
 */
public class JSONCraftingOverrides {
    public String comment1 = "The following section is used for overriding crafting recipes from packs for use in modpacks and servers.";
    public String comment2 = "Everything that is crafted on one of the benches can be modified here to include any item, including modded ones.";
    public String comment3 = "The format is one of [modID]:[ItemName]:[Metadata]:[Qty], with the name being the same as the in-game /give command. (Ignore metadata for 1.16.5+ versions)";
    public String comment4 = "If you need the full listing of items, set dumpCraftingConfig to true in the general config section.";
    public String comment5 = "This will overwrite this file with all craft-able items in all packs.";
    public String comment6 = "Note that their crafting recipes may vary depending on what packs are installed.";
    public Map<String, Map<String, List<String>>> overrides;
}
