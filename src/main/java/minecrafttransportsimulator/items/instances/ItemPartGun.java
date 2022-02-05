package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class ItemPartGun extends AItemPart implements IItemEntityProvider<EntityPlayerGun>{
	
	public ItemPartGun(JSONPart definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}

	@Override
	public boolean isPartValidForPackDef(JSONPartDefinition placementDefinition, String subNameToPlaceOn, boolean checkMinMax){
		return super.isPartValidForPackDef(placementDefinition, subNameToPlaceOn, checkMinMax) && (!checkMinMax || (placementDefinition.minValue <= definition.gun.diameter && placementDefinition.maxValue >= definition.gun.diameter));
	}
	
	@Override
	public PartGun createPart(AEntityF_Multipart<?> entity, WrapperPlayer placingPlayer, JSONPartDefinition packVehicleDef, WrapperNBT partData, APart parentPart){
		return new PartGun(entity, placingPlayer, packVehicleDef, partData, parentPart);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		super.addTooltipLines(tooltipLines, data);
		tooltipLines.add(InterfaceCore.translate("info.item.gun.diameter") + definition.gun.diameter);
		tooltipLines.add(InterfaceCore.translate("info.item.gun.caseRange") + definition.gun.minCaseLength + "-" + definition.gun.maxCaseLength);
		tooltipLines.add(InterfaceCore.translate("info.item.gun.fireDelay") + definition.gun.fireDelay);
		tooltipLines.add(InterfaceCore.translate("info.item.gun.muzzleVelocity") + definition.gun.muzzleVelocity);
		tooltipLines.add(InterfaceCore.translate("info.item.gun.capacity") + definition.gun.capacity);
		if(definition.gun.autoReload){
			tooltipLines.add(InterfaceCore.translate("info.item.gun.autoReload"));
		}
		tooltipLines.add(InterfaceCore.translate("info.item.gun.yawRange") + definition.gun.minYaw + "-" + definition.gun.maxYaw);
		tooltipLines.add(InterfaceCore.translate("info.item.gun.pitchRange") + definition.gun.minPitch + "-" + definition.gun.maxPitch);
	}
	
	@Override
	public boolean canBreakBlocks(){
		return !definition.gun.handHeld;
	}
	
	@Override
	public EntityPlayerGun createEntity(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		return new EntityPlayerGun(world, placingPlayer, data);
	}

	@Override
	public Class<EntityPlayerGun> getEntityClass(){
		return EntityPlayerGun.class;
	}
	
	public static final AItemPartCreator CREATOR = new AItemPartCreator(){
		@Override
		public boolean isCreatorValid(JSONPart definition){
			return definition.generic.type.startsWith("gun");
		}
		@Override
		public ItemPartGun createItem(JSONPart definition, String subName, String sourcePackID){
			return new ItemPartGun(definition, subName, sourcePackID);
		}
	};
}
