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
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.LanguageSystem;

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
        tooltipLines.add(LanguageSystem.ITEMINFO_GROUND_DEVICE_DIAMETER.getCurrentValue() + definition.ground.height);
        tooltipLines.add(LanguageSystem.ITEMINFO_GROUND_DEVICE_MOTIVEFRICTION.getCurrentValue() + definition.ground.motiveFriction);
        tooltipLines.add(LanguageSystem.ITEMINFO_GROUND_DEVICE_LATERALFRICTION.getCurrentValue() + definition.ground.lateralFriction);
        tooltipLines.add(LanguageSystem.ITEMINFO_GROUND_DEVICE_WETFRICTION.getCurrentValue() + definition.ground.wetFrictionPenalty);
        Map<Float, String> frictionValues = new TreeMap<>();
        for (Entry<BlockMaterial, Float> modifier : definition.ground.frictionModifiers.entrySet()) {
            Float value = modifier.getValue();
            if(!frictionValues.containsKey(value)) {
                frictionValues.put(value, value + ": " + modifier.getKey().name().toLowerCase(Locale.ROOT));
            }else {
                frictionValues.put(value, frictionValues.get(value) + ", " + modifier.getKey().name().toLowerCase(Locale.ROOT));
            }
        }
        
        tooltipLines.add(LanguageSystem.ITEMINFO_GROUND_DEVICE_FRICTIONMODIFIERS.getCurrentValue());
        frictionValues.forEach((key, value) -> tooltipLines.add(value));
        tooltipLines.add(definition.ground.isWheel ? LanguageSystem.ITEMINFO_GROUND_DEVICE_ROTATESONSHAFT_TRUE.getCurrentValue() : LanguageSystem.ITEMINFO_GROUND_DEVICE_ROTATESONSHAFT_FALSE.getCurrentValue());
        tooltipLines.add(definition.ground.canFloat ? LanguageSystem.ITEMINFO_GROUND_DEVICE_CANFLOAT_TRUE.getCurrentValue() : LanguageSystem.ITEMINFO_GROUND_DEVICE_CANFLOAT_FALSE.getCurrentValue());
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
