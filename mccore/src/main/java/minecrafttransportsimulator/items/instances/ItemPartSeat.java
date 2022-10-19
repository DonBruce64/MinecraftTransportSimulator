package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class ItemPartSeat extends AItemPart {

    public ItemPartSeat(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public PartSeat createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
        return new PartSeat(entity, placingPlayer, packVehicleDef, partData);
    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("seat");
        }

        @Override
        public ItemPartSeat createItem(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
            return new ItemPartSeat(definition, subDefinition, sourcePackID);
        }
    };
}
