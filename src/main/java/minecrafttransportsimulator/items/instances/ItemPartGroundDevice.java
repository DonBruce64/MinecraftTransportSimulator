package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

import java.util.List;
import java.util.Map.Entry;

public class ItemPartGroundDevice extends AItemPart {

    public ItemPartGroundDevice(JSONPart definition, String subName, String sourcePackID) {
        super(definition, subName, sourcePackID);
    }

    @Override
    public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax) {
        return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.ground.height && placementDefinition.maxValue >= definition.ground.height));
    }

    @Override
    public PartGroundDevice createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData, APart parentPart) {
        return new PartGroundDevice(entity, placingPlayer, packVehicleDef, partData, parentPart);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_DIAMETER.value + definition.ground.height);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_MOTIVEFRICTION.value + definition.ground.motiveFriction);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_LATERALFRICTION.value + definition.ground.lateralFriction);
        StringBuilder modifierString = null;
        int modifierCount = 0;
        for (Entry<BlockMaterial, Float> modifier : definition.ground.frictionModifiers.entrySet()) {
            if (modifierString == null) {
                modifierString = new StringBuilder("\n");
            } else {
                if (++modifierCount == 2) {
                    modifierCount = 0;
                    modifierString.append("\n");
                } else {
                    modifierString.append(", ");
                }
            }
            modifierString.append(modifier.getKey().name().toLowerCase()).append(": ").append(modifier.getValue());
        }
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_FRICTIONMODIFIERS.value + modifierString);
        tooltipLines.add(definition.ground.isWheel ? JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_ROTATESONSHAFT_TRUE.value : JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_ROTATESONSHAFT_FALSE.value);
        tooltipLines.add(definition.ground.canFloat ? JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_CANFLOAT_TRUE.value : JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_CANFLOAT_FALSE.value);
    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("ground");
        }

        @Override
        public ItemPartGroundDevice createItem(JSONPart definition, String subName, String sourcePackID) {
            return new ItemPartGroundDevice(definition, subName, sourcePackID);
        }
    };
}
