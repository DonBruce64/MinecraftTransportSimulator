package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Class that contains information about one set of materials for a pack item.
 * This is the number of materials, as well as the various items that can qualify for
 * these materials.  Used for crafting pack items.
 *
 * @author don_bruce
 */
public class PackMaterialComponent {
    public final int qty;
    public final int meta;
    public final List<IWrapperItemStack> possibleItems;
    public static String lastErrorMessage;

    private PackMaterialComponent(String itemText) {
        possibleItems = new ArrayList<>();
        String[] itemParameters = itemText.split(":");
        String domain = itemParameters[0];
        String name = itemParameters[1];
        meta = Integer.parseInt(itemParameters.length == 4 ? itemParameters[2] : "0");
        qty = Integer.parseInt(itemParameters.length == 4 ? itemParameters[3] : itemParameters[2]);

        if (domain.equals("oredict")) {
            possibleItems.addAll(InterfaceManager.coreInterface.getOredictMaterials(name, qty));
        } else {
            IWrapperItemStack stack = InterfaceManager.coreInterface.getStackForProperties(domain + ":" + name, meta, qty);
            if (!stack.isEmpty()) {
                possibleItems.add(stack);
            }
        }
    }

    /**
     * Returns the Material Components require to craft the passed-in item with the passed-in recipe.
     * Each component corresponds to a single ingredient input, with each list in the component corresponding to the
     * possible items that are valid for that ingredient.  The idea being that OreDict allows for
     * multiple items to be used.  If this component is not for crafting checks, set forCraftingCheck to false.
     * This prevents the returned stacks from having the wildcard value in their metadata and not being actual items.
     * If an error occured during parsing, then null is returned and {@link #lastErrorMessage} is set to the error.
     */
    public static List<PackMaterialComponent> parseFromJSON(AItemPack<?> item, int recipeIndex, boolean includeMain, boolean includeSub, boolean forRepair) {
        //Get all the strings that represent the items.
        List<String> itemTexts = new ArrayList<String>();
        String currentSubName = "";
        if (forRepair) {
            if (item.definition.general.repairMaterialLists != null) {
                //Get repair materials. Make sure to add actual item into the list too.
                itemTexts.add(InterfaceManager.coreModID + ":" + item.getRegistrationName() + ":0:1");
                itemTexts.addAll(item.definition.general.repairMaterialLists.get(recipeIndex));
            }
        } else {
            //Get main materials.
            if (includeMain) {
                itemTexts.addAll(item.definition.general.materialLists.get(recipeIndex));
            }

            //Get subType materials, if required.
            if (includeSub && item instanceof AItemSubTyped) {
                currentSubName = ((AItemSubTyped<?>) item).subDefinition.subName;
                if (((AItemSubTyped<?>) item).subDefinition.extraMaterialLists.size() != item.definition.general.materialLists.size()) {
                    lastErrorMessage = "This item has a mis-matched number of normal materialLists (" + item.definition.general.materialLists.size() + ") and extraMaterialLists (" + ((AItemSubTyped<?>) item).subDefinition.extraMaterialLists.size() + ") for " + item.definition.packID + ":" + item.definition.systemName + currentSubName + ".  Crafting will be disabled in survival mode.  Report this to the pack author!";
                    return null;
                }
                itemTexts.addAll(((AItemSubTyped<?>) item).subDefinition.extraMaterialLists.get(recipeIndex));
            }
        }

        //Parse the items.
        List<PackMaterialComponent> components = new ArrayList<PackMaterialComponent>();
        for (String itemText : itemTexts) {
            try {
                PackMaterialComponent component = new PackMaterialComponent(itemText);
                if (!component.possibleItems.isEmpty()) {
                    components.add(component);
                } else {
                    lastErrorMessage = "No valid items found for " + (forRepair ? "repair" : "normal") + " crafting ingredient entry " + itemText + " for " + item.definition.packID + ":" + item.definition.systemName + currentSubName + ".  Crafting will be disabled in survival mode.  Report this to the pack author!";
                    return null;
                }
            } catch (Exception e) {
                lastErrorMessage = "A fault was encountered when trying to parse " + (forRepair ? "repair" : "normal") + " crafting ingredient entry " + itemText + " for " + item.definition.packID + ":" + item.definition.systemName + currentSubName + ".  Crafting will be disabled in survival mode.  Report this to the pack author!";
                return null;
            }
        }

        //Return all materials.
        return components;
    }
}
