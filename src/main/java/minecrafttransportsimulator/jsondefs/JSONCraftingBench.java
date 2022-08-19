package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public class JSONCraftingBench {

    @JSONDescription("A list of item types this component can craft.  This is the first layer of filtering, and restricts the items displayed to only crafting items that have the same type.  Valid types correspond to JSON definition types, those being the types that you make sub-folders for in your pack.")
    public List<String> itemTypes;

    @JSONDescription("An optional list of part types this component can craft.  Only used to filter 'part' itemTypes.  For example, adding 'engine' would allow the component to craft all engines, but adding 'engine_car' and 'engine_boat' would prevent it from crafting aircraft engines.")
    public List<String> partTypes;

    @JSONDescription("A list of items this component may craft.  This overrides all other filters, and may be used to specify exactly what this component may craft.  The format for this is [packID:systemName], where systemName is the name of the item with the subName appended.")
    public List<String> items;
}