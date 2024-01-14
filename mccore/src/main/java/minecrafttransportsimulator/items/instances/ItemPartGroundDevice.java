package minecrafttransportsimulator.items.instances;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class ItemPartGroundDevice extends AItemPart {

    public ItemPartGroundDevice(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, JSONSubDefinition subDefinition, boolean checkMinMax) {
        return super.isPartValidForPackDef(placementDefinition, subDefinition, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.ground.height && placementDefinition.maxValue >= definition.ground.height));
    }

    @Override
    public PartGroundDevice createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
        return new PartGroundDevice(entity, placingPlayer, packVehicleDef, partData);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_DIAMETER.value + definition.ground.height);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_MOTIVEFRICTION.value + definition.ground.motiveFriction);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_LATERALFRICTION.value + definition.ground.lateralFriction);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_WETFRICTION.value + definition.ground.wetFrictionPenalty);
        Map<Float, StringBuilder> frictionValues = new TreeMap<>();
        for (Entry<BlockMaterial, Float> modifier : definition.ground.frictionModifiers.entrySet()) {
            Float value = modifier.getValue();
            if(!frictionValues.containsKey(value)) {
                frictionValues.put(value, new StringBuilder().append(value).append(": ").append(modifier.getKey().name().toLowerCase(Locale.ROOT)));
            }else {
                frictionValues.get(value).append(", ").append(modifier.getKey().name().toLowerCase(Locale.ROOT)); 
            }
        }
        
        StringBuilder netString = new StringBuilder();
        frictionValues.forEach((key, value) -> netString.append("\n  ").append(value));
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_FRICTIONMODIFIERS.value + netString);
        tooltipLines.add(definition.ground.isWheel ? JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_ROTATESONSHAFT_TRUE.value : JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_ROTATESONSHAFT_FALSE.value);
        tooltipLines.add(definition.ground.canFloat ? JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_CANFLOAT_TRUE.value : JSONConfigLanguage.ITEMINFO_GROUND_DEVICE_CANFLOAT_FALSE.value);
    }

    public boolean needsRepair(IWrapperNBT data) {
        return super.needsRepair(data) || data.getBoolean(PartGroundDevice.FLAT_VARIABLE);
    }

    @Override
    public void repair(IWrapperNBT data) {
        super.repair(data);
        data.deleteData(PartGroundDevice.FLAT_VARIABLE);
    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("ground");
        }

        @Override
        public ItemPartGroundDevice createItem(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
            return new ItemPartGroundDevice(definition, subDefinition, sourcePackID);
        }
    };
}
