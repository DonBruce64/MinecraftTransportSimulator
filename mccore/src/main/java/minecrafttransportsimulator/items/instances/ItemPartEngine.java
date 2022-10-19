package minecrafttransportsimulator.items.instances;

import java.util.List;
import java.util.Map.Entry;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemPartEngine extends AItemPart {

    public ItemPartEngine(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, JSONSubDefinition subDefinition, boolean checkMinMax) {
        return super.isPartValidForPackDef(placementDefinition, subDefinition, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.engine.fuelConsumption && placementDefinition.maxValue >= definition.engine.fuelConsumption));
    }

    @Override
    public PartEngine createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
        return new PartEngine(entity, placingPlayer, packVehicleDef, partData);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_MAXRPM.value + definition.engine.maxRPM);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_MAXSAFERPM.value + definition.engine.maxSafeRPM);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_FUELCONSUMPTION.value + definition.engine.fuelConsumption);
        if (definition.engine.superchargerEfficiency != 0) {
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_SUPERCHARGERFUELCONSUMPTION.value + definition.engine.superchargerFuelConsumption);
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_SUPERCHARGEREFFICIENCY.value + definition.engine.superchargerEfficiency);
        }
        if (definition.engine.jetPowerFactor > 0) {
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_JETPOWERFACTOR.value + (int) (100 * definition.engine.jetPowerFactor) + "%");
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_BYPASSRATIO.value + definition.engine.bypassRatio);
        }
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_FUELTYPE.value + definition.engine.fuelType);
        if (ConfigSystem.settings.fuel.fuels.containsKey(definition.engine.fuelType)) {
            StringBuilder line = new StringBuilder(JSONConfigLanguage.ITEMINFO_ENGINE_FLUIDS.value);
            for (Entry<String, Double> fuelEntry : ConfigSystem.settings.fuel.fuels.get(definition.engine.fuelType).entrySet()) {
                if (InterfaceManager.coreInterface.isFluidValid(fuelEntry.getKey())) {
                    line.append(InterfaceManager.clientInterface.getFluidName(fuelEntry.getKey())).append("@").append(fuelEntry.getValue()).append(", ");
                }
            }
            tooltipLines.add(line.substring(0, line.length() - 2));
        }
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_HOURS.value + Math.round(data.getDouble("hours") * 100D) / 100D);

        if (definition.engine.gearRatios.size() > 3) {
            tooltipLines.add(definition.engine.isAutomatic ? JSONConfigLanguage.ITEMINFO_ENGINE_AUTOMATIC.value : JSONConfigLanguage.ITEMINFO_ENGINE_MANUAL.value);
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_GEARRATIOS.value);
            for (byte i = 0; i < definition.engine.gearRatios.size(); i += 5) {
                String gearRatios = "";
                for (byte j = i; j < i + 5 && j < definition.engine.gearRatios.size(); ++j) {
                    gearRatios += String.valueOf(definition.engine.gearRatios.get(j));
                    if (j < definition.engine.gearRatios.size() - 1) {
                        gearRatios += ",  ";
                    }
                }
                tooltipLines.add(gearRatios);
            }

        } else {
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_GEARRATIOS.value + definition.engine.gearRatios.get(definition.engine.gearRatios.size() - 1));
        }

        if (data.getBoolean("oilLeak")) {
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_OILLEAK.value);
        }
        if (data.getBoolean("fuelLeak")) {
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_ENGINE_FUELLEAK.value);
        }
    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("engine");
        }

        @Override
        public ItemPartEngine createItem(JSONPart definition, JSONSubDefinition subDefinition, String sourcePackID) {
            return new ItemPartEngine(definition, subDefinition, sourcePackID);
        }
    };
}
