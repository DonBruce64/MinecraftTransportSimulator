package minecrafttransportsimulator.items.components;

import java.util.Collections;
import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

public abstract class AItemSubTyped<JSONDefinition extends AJSONMultiModelProvider> extends AItemPack<JSONDefinition> {
    public JSONSubDefinition subDefinition;
    public LanguageEntry languageSubDescription;

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
        String text = languageSubDescription.getCurrentValue();
        if (!text.isEmpty()) {
            Collections.addAll(tooltipLines, text.split("\n"));
        }
    }

    @Override
    public void populateDefaultData(IWrapperNBT data) {
        super.populateDefaultData(data);
        data.setString("subName", subDefinition.subName);
    }
}
