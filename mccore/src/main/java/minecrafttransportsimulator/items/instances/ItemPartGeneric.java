package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartGeneric;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class ItemPartGeneric extends AItemPart {

    public ItemPartGeneric(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, JSONSubDefinition subDefinition, boolean checkMinMax) {
        return super.isPartValidForPackDef(placementDefinition, subDefinition, checkMinMax) && (!checkMinMax || ((placementDefinition.minValue <= definition.generic.height && placementDefinition.maxValue >= definition.generic.height) || (placementDefinition.minValue == 0 && placementDefinition.maxValue == 0)));
    }

    @Override
    public PartGeneric createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
        return new PartGeneric(entity, placingPlayer, packVehicleDef, partData);
    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("generic");
        }

        @Override
        public ItemPartGeneric createItem(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
            return new ItemPartGeneric(definition, subDefinition, sourcePackID);
        }
    };
}
