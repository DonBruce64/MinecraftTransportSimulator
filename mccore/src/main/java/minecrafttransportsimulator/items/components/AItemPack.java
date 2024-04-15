package minecrafttransportsimulator.items.components;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONCraftingBench;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Base item class for all pack-created items.  Stores information such as the
 * pack the item belongs to and the class that extends {@link AJSONItem} that
 * is the instance of the item's pack.
 *
 * @author don_bruce
 */
public abstract class AItemPack<JSONDefinition extends AJSONItem> extends AItemBase {
    public final JSONDefinition definition;
    private final String sourcePackID;
    public LanguageEntry languageName;
    public LanguageEntry languageDescription;

    public AItemPack(JSONDefinition definition, String sourcePackID) {
        super();
        this.definition = definition;
        this.sourcePackID = sourcePackID;
    }

    @Override
    public String getRegistrationName() {
        return definition.packID + "." + definition.systemName;
    }

    @Override
    public String getItemName() {
        return languageName.getCurrentValue();
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        //Don't add any tooltips if we are just an empty
        String text = languageDescription.getCurrentValue();
        if (!text.isEmpty()) {
            Collections.addAll(tooltipLines, text.split("\n"));
        }
    }

    @Override
    public int getStackSize() {
        return definition.general.stackSize != 0 ? definition.general.stackSize : super.getStackSize();
    }

    @Override
    public String getCreativeTabID() {
        String owningPackID = definition.packID;
        String generatingPackID = sourcePackID != null ? sourcePackID : definition.packID;
        JSONPack owningConfiguration = PackParser.getPackConfiguration(definition.packID);
        JSONPack generatingConfiguration = PackParser.getPackConfiguration(generatingPackID);
        if (owningConfiguration.externalSkinsInOwnTab) {
            return generatingPackID;
        } else if (generatingConfiguration.internalSkinsInOwnTab) {
            return generatingPackID;
        } else {
            return owningPackID;
        }
    }

    /**
     * Returns true if this item can be crafted by the passed-in bench definition.
     */
    public boolean isBenchValid(JSONCraftingBench craftingDefinition) {
        //Benches can only be valid if the item has materials to craft it with.
        boolean hasMaterials = false;

        //Make sure we have main materials to craft us with.
        for (List<String> list : definition.general.materialLists) {
            if (!list.isEmpty()) {
                hasMaterials = true;
                break;
            }
        }

        //No main materials, but we might have sub materials if we are sub-typed.
        if (!hasMaterials && this instanceof AItemSubTyped) {
            for (List<String> list : ((AItemSubTyped<?>) this).subDefinition.extraMaterialLists) {
                if (!list.isEmpty()) {
                    hasMaterials = true;
                    break;
                }
            }
        }

        //If we have materials, do final bench checks.  Otherwise, we know we can't be crafted in any bench.
        if (hasMaterials) {
            if (craftingDefinition.items != null) {
                return craftingDefinition.items.contains(definition.packID + ":" + definition.systemName);
            } else if (craftingDefinition.itemTypes.contains(definition.classification.toString().toLowerCase(Locale.ROOT))) {
                if (definition instanceof JSONPart && craftingDefinition.partTypes != null) {
                    for (String partType : craftingDefinition.partTypes) {
                        if (((JSONPart) definition).generic.type.contains(partType)) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the item needs repair, false if not.
     */
    public boolean needsRepair(IWrapperNBT data) {
        return data != null && data.getDouble(AEntityE_Interactable.DAMAGE_VARIABLE) > 0;
    }

    /**
     * Repairs the item.  What happens during repair differs from item to item.
     */
    public void repair(IWrapperNBT data) {
        data.setDouble(AEntityE_Interactable.DAMAGE_VARIABLE, 0);
    }
}
