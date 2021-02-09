package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.guis.instances.GUIPackExporter;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

public class ItemWrench extends AItemBase implements IItemVehicleInteractable{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		tooltipLines.add(InterfaceCore.translate("info.item.wrench.use"));
		tooltipLines.add(InterfaceCore.translate("info.item.wrench.useblock"));
		tooltipLines.add(InterfaceCore.translate("info.item.wrench.attack"));
		tooltipLines.add(InterfaceCore.translate("info.item.wrench.sneakattack"));
		if(ConfigSystem.configObject.clientControls.devMode.value){
			tooltipLines.add("Use while riding a vehicle to open the devMode editor.");
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		//If the player isn't the owner of the vehicle, they can't interact with it.
		if(!ownerState.equals(PlayerOwnerState.USER)){
			if(rightClick){
				if(vehicle.world.isClient()){
					if(ConfigSystem.configObject.clientControls.devMode.value && vehicle.equals(player.getEntityRiding())){
						InterfaceGUI.openGUI(new GUIPackExporter(vehicle));
					}else if(player.isSneaking()){
						InterfaceGUI.openGUI(new GUITextEditor(vehicle));
					}else{
						InterfaceGUI.openGUI(new GUIInstruments(vehicle, player));
					}
				}else{
					return CallbackType.PLAYER;
				}
			}else if(!vehicle.world.isClient()){
				if(part != null && !player.isSneaking()){
					//Player can remove part.  Check that the part isn't permanent, or a part with subparts.
					//If not, spawn item in the world and remove part.
					//Make sure to remove the part before spawning the item.  Some parts
					//care about this order and won't spawn items unless they've been removed.
					if(!part.partDefinition.isPermanent && part.childParts.isEmpty()){
						vehicle.removePart(part, null);
						ItemPart droppedItem = part.getItem();
						if(droppedItem != null){
							vehicle.world.spawnItem(droppedItem, part.getData(), part.worldPos);
						}
					}
				}else if(player.isSneaking()){
					//Attacker is a sneaking player with a wrench.
					//Remove this vehicle if possible.
					if((!ConfigSystem.configObject.general.opPickupVehiclesOnly.value || ownerState.equals(PlayerOwnerState.ADMIN)) && (!ConfigSystem.configObject.general.creativePickupVehiclesOnly.value || player.isCreative())){
						//Make sure we disconnect any trailers linked to this vehicle.  We don't want to save those.
						if(vehicle.towedVehicle != null){
							vehicle.changeTrailer(null, null, null, null, null);
						}
						if(vehicle.towedByVehicle != null){
							vehicle.towedByVehicle.changeTrailer(null, null, null, null, null);
						}
						ItemVehicle vehicleItem = PackParserSystem.getItem(vehicle.definition.packID, vehicle.definition.systemName, vehicle.currentSubName);
						WrapperNBT vehicleData = new WrapperNBT();
						vehicle.save(vehicleData);
						vehicle.world.spawnItem(vehicleItem, vehicleData, vehicle.position);
						vehicle.isValid = false;
					}
				}
			}
		}else{
			player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehicleowned"));
		}
		return CallbackType.NONE;
	}
}
