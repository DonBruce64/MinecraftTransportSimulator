package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public final class PartGeneric extends APart {

    public PartGeneric(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, data);
    }
}
