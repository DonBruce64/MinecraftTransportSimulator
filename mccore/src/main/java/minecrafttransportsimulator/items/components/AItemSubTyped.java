package minecrafttransportsimulator.items.components;

import java.util.Collections;
import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;

public abstract class AItemSubTyped<JSONDefinition extends AJSONMultiModelProvider> extends AItemPack<JSONDefinition> {
    public JSONSubDefinition subDefinition;

    public AItemSubTyped(JSONDefinition definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, sourcePackID);
        this.subDefinition = subDefinition;
    }

    @Override
    public String getRegistrationName() {
        return super.getRegistrationName() + subDefinition.subName;
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        if (subDefinition.description != null) {
            Collections.addAll(tooltipLines, subDefinition.description.split("\n"));
        }
    }

    @Override
    public void populateDefaultData(IWrapperNBT data) {
        super.populateDefaultData(data);
        data.setString("subName", subDefinition.subName);
    }

    /**
     * Creates a new entity from the item's data, or null if no entity is to be created.
     */
    public AEntityD_Definable<JSONDefinition> createEntityFromData(AWrapperWorld world, IWrapperNBT data) {
        return null;
    }
}
