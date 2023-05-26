package minecrafttransportsimulator.jsondefs;

import java.util.Map;

/**
 * Custom class designed to hold damage factors for external damage.  Split from the main config file
 * to prevent nuking the file if the config loading fails.  We can do without damage factors
 * overrides just fine.  Not so for configs.
 *
 * @author don_bruce
 */
public class JSONConfigExternalDamageOverrides {
    public String comment1 = "The following section is used for overriding the default 1:1 damage values applied to vehicles in packs from external sources.";
    public String comment2 = "This is useful for modpack creators that wish to include MTS in packs with other weapons that don't know about the various hitbox and armor systems.";
    public String comment3 = "The entries in here are first indexed by the pack, then the vehicle ID.";
    public String comment4 = "If you need the full listing of items, set dumpDamageConfig to true in the general config section.";
    public String comment5 = "This will overwrite this file with all vehicles in all packs.";
    public Map<String, Map<String, Double>> overrides;
}
