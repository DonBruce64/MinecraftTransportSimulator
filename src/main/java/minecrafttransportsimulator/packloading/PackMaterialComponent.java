package minecrafttransportsimulator.packloading;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

import java.util.ArrayList;
import java.util.List;

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
        if (itemText.startsWith("oredict:")) {
            qty = Integer.parseInt(itemText.substring(itemText.lastIndexOf(':') + 1));
            itemText = itemText.substring(0, itemText.lastIndexOf(':'));
            meta = 0;
            String oreName = itemText.substring("oredict:".length());
            List<IWrapperItemStack> oreDictMaterials = InterfaceManager.coreInterface.getOredictMaterials(oreName);

            //Set stack qty.
            for (IWrapperItemStack stack : oreDictMaterials) {
                stack.add(qty - 1);
            }
            possibleItems.addAll(oreDictMaterials);
        } else {
            qty = Integer.parseInt(itemText.substring(itemText.lastIndexOf(':') + 1));
            itemText = itemText.substring(0, itemText.lastIndexOf(':'));
            meta = Integer.parseInt(itemText.substring(itemText.lastIndexOf(':') + 1));
            itemText = itemText.substring(0, itemText.lastIndexOf(':'));
            IWrapperItemStack stack = InterfaceManager.coreInterface.getStackForProperties(itemText, meta, qty);
            if (!stack.isEmpty()) {
                possibleItems.add(stack);
            }
        }
    }

    /**
     * Returns the Material Components require to craft the passed-in item.  The return value is a list of components.
     * Each component corresponds to a single ingredient input, with each list in the component corresponding to the
     * possible items that are valid for that ingredient.  The idea being that OreDict allows for
     * multiple items to be used.  If this component is not for crafting checks, set forCraftingCheck to false.
     * This prevents the returned stacks from having the wildcard value in their metadata and not being actual items.
     * If an error occured during parsing, then null is returned and {@link #lastErrorMessage} is set to the error.
     */
    public static List<PackMaterialComponent> parseFromJSON(AItemPack<?> item, boolean includeMain, boolean includeSub, boolean forCraftingCheck, boolean forRepair) {
        //Get all the strings that represent the items.
        List<String> itemTexts = new ArrayList<>();
        String currentSubName = "";
        if (forRepair) {
            if (item.definition.general.repairMaterials != null) {
                //Get repair materials. Make sure to add actual item into the list too.
                itemTexts.add(InterfaceManager.coreModID + ":" + item.getRegistrationName() + ":0:1");
                itemTexts.addAll(item.definition.general.repairMaterials);
            }
        } else {
            //Get main materials.
            if (includeMain) {
                itemTexts.addAll(item.definition.general.materials);
            }

            //Get subType materials, if required.
            if (includeSub && item instanceof AItemSubTyped) {
                itemTexts.addAll(((AItemSubTyped<?>) item).getExtraMaterials());
                currentSubName = ((AItemSubTyped<?>) item).subName;
            }
        }

        //Parse the items.
        List<PackMaterialComponent> components = new ArrayList<>();
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
