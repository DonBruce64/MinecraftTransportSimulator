package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.LanguageSystem;

public class ItemPartPropeller extends AItemPart {

    public ItemPartPropeller(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, JSONSubDefinition subDefinition, boolean checkMinMax) {
        return super.isPartValidForPackDef(placementDefinition, subDefinition, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.propeller.diameter && placementDefinition.maxValue >= definition.propeller.diameter));
    }

    @Override
    public PartPropeller createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
        return new PartPropeller(entity, placingPlayer, packVehicleDef, partData);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        tooltipLines.add(definition.propeller.isDynamicPitch ? LanguageSystem.ITEMINFO_PROPELLER_DYNAMICPITCH.getCurrentValue() : LanguageSystem.ITEMINFO_PROPELLER_STATICPITCH.getCurrentValue());
        tooltipLines.add(LanguageSystem.ITEMINFO_PROPELLER_PITCH.getCurrentValue() + definition.propeller.pitch);
        tooltipLines.add(LanguageSystem.ITEMINFO_PROPELLER_DIAMETER.getCurrentValue() + definition.propeller.diameter);
    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("propeller");
        }

        @Override
        public ItemPartPropeller createItem(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
            return new ItemPartPropeller(definition, subDefinition, sourcePackID);
        }
    };
}
