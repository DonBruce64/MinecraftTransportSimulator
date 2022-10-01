package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

public class ItemPartGun extends AItemPart implements IItemEntityProvider<EntityPlayerGun> {

    public ItemPartGun(JSONPart definition, String subName, String sourcePackID) {
        super(definition, subName, sourcePackID);
    }

    @Override
    public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax) {
        return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.gun.diameter && placementDefinition.maxValue >= definition.gun.diameter));
    }

    @Override
    public PartGun createPart(AEntityF_Multipart<?> entity, IWrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, IWrapperNBT partData) {
        return new PartGun(entity, placingPlayer, packVehicleDef, partData);
    }

    @Override
    public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data) {
        super.addTooltipLines(tooltipLines, data);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GUN_DIAMETER.value + definition.gun.diameter);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GUN_CASERANGE.value + definition.gun.minCaseLength + "-" + definition.gun.maxCaseLength);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GUN_FIREDELAY.value + definition.gun.fireDelay);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GUN_MUZZLEVELOCITY.value + definition.gun.muzzleVelocity);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GUN_CAPACITY.value + definition.gun.capacity);
        if (definition.gun.autoReload) {
            tooltipLines.add(JSONConfigLanguage.ITEMINFO_GUN_AUTORELOAD.value);
        }
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GUN_YAWRANGE.value + definition.gun.minYaw + "-" + definition.gun.maxYaw);
        tooltipLines.add(JSONConfigLanguage.ITEMINFO_GUN_PITCHRANGE.value + definition.gun.minPitch + "-" + definition.gun.maxPitch);
    }

    @Override
    public boolean canBreakBlocks() {
        return !definition.gun.handHeld;
    }

    @Override
    public EntityPlayerGun createEntity(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        return new EntityPlayerGun(world, placingPlayer, data);
    }

    @Override
    public Class<EntityPlayerGun> getEntityClass() {
        return EntityPlayerGun.class;
    }

    public static final AItemPartCreator CREATOR = new AItemPartCreator() {
        @Override
        public boolean isCreatorValid(JSONPart definition) {
            return definition.generic.type.startsWith("gun");
        }

        @Override
        public ItemPartGun createItem(JSONPart definition, String subName, String sourcePackID) {
            return new ItemPartGun(definition, subName, sourcePackID);
        }
    };
}
