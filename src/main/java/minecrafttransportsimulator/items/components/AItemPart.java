package minecrafttransportsimulator.items.components;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**Base class for part items.  Contains methods on what part to spawn from
 * the item if the part is valid for the defined pack-def, etc.
 * 
 * @author don_bruce
 */
public abstract class AItemPart extends AItemSubTyped<JSONPart> {

    public AItemPart(JSONPart definition, String subName, String sourcePackID) {
        super(definition, subName, sourcePackID);
    }

    /**
     * Checks if the part item is valid for the defined part slot.  Checked for all placement, and slot-based operations.
     * min/max may be ignored in some cases (such as holo-boxes where you want them to be red of the part is the right
     * type, but the wrong size).
     */
    public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax) {
        //First make sure we are the right type.
        if (placementDefinition.types.contains(definition.generic.type) && (placementDefinition.validSubNames == null || placementDefinition.validSubNames.contains(subNameToPlaceOn))) {
            //Check if our custom type matches, or if we aren't a custom type and the definition doesn't care.
            if (placementDefinition.customTypes == null) {
                return definition.generic.customType == null;
            } else if (definition.generic.customType == null) {
                return placementDefinition.customTypes == null || placementDefinition.customTypes.contains("");
            } else {
                return placementDefinition.customTypes.contains(definition.generic.customType);
            }
        } else {
            return false;
        }
    }

    /**
     * Creates a new part from the saved data.  This is used both in the construction of new parts, and loading
     * of saved parts from data.  In both cases, the passed-in data will be whatever is present, so ensure you
     * perform data-validation on said data so it has all properties required to create the part in the
     * part's constructor.
     */
    public abstract APart createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData);

    /**Class used to create instances of part items.  One of these should be registered
     * for every item before packs are parsed so the parser knows which to create.
     * 
     * @author don_bruce
     */
    public static abstract class AItemPartCreator {

        /**
         * Returns true if this creator can create the passed-in part item.  Creators are checked in the opposite order
         * they were added; a creator added later is checked first.  The reason for this is to allow other mods to
         * override the default creators if needed with part-specific creators.
         */
        public abstract boolean isCreatorValid(JSONPart definition);

        /**
         * Creates a part item from data.
         */
        public abstract AItemPart createItem(JSONPart definition, String subName, String sourcePackID);
    }
}
