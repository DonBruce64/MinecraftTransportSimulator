package minecrafttransportsimulator.items.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartGrapple;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.LanguageSystem;

public class ItemPartGrapple extends AItemPart {

    public ItemPartGrapple(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public PartGrapple createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
        return new PartGrapple(entity, placingPlayer, packVehicleDef, this, partData);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        tooltipLines.add(LanguageSystem.ITEMINFO_GRAPPLE_RANGE.getCurrentValue() + definition.grapple.range);
        tooltipLines.add(LanguageSystem.ITEMINFO_GRAPPLE_REELSPEED.getCurrentValue() + definition.grapple.reelSpeed);
        List<String> attachesTo = new ArrayList<>();
        if (!definition.grapple.disableBlockAttach) {
            attachesTo.add(LanguageSystem.ITEMINFO_GRAPPLE_BLOCKS.getCurrentValue());
        }
        if (!definition.grapple.disableEntityAttach) {
            attachesTo.add(LanguageSystem.ITEMINFO_GRAPPLE_ENTITIES.getCurrentValue());
        }
        if (!definition.grapple.disableVehicleAttach) {
            attachesTo.add(LanguageSystem.ITEMINFO_GRAPPLE_VEHICLES.getCurrentValue());
        }
        if (!attachesTo.isEmpty()) {
            tooltipLines.add(LanguageSystem.ITEMINFO_GRAPPLE_ATTACHESTO.getCurrentValue() + String.join(", ", attachesTo));
        }
    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("grapple");
        }

        @Override
        public ItemPartGrapple createItem(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
            return new ItemPartGrapple(definition, subDefinition, sourcePackID);
        }
    };
}
