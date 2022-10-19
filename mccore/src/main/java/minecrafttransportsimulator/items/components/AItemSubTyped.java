package minecrafttransportsimulator.items.components;

import java.util.Collections;
import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
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
}
