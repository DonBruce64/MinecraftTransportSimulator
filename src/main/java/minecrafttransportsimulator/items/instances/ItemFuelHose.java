package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartInteractable;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;

public class ItemFuelHose extends AItemBase implements IItemVehicleInteractable{
	private static PartInteractable firstPartClicked;
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(MasterLoader.coreInterface.translate("info.item.fuelhose.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, IWrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(!vehicle.world.isClient()){
			if(rightClick){
				if(firstPartClicked == null){
					if(part instanceof PartInteractable){
						PartInteractable interactable = (PartInteractable) part;
						if(interactable.tank != null){
							if(interactable.linkedPart == null && interactable.linkedVehicle == null){
								firstPartClicked = interactable;
								player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.firstlink"));
							}else{
								player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.alreadylinked"));
							}
						}
					}
				}else{
					if(part instanceof PartInteractable){
						PartInteractable interactable = (PartInteractable) part;
						if(interactable.tank != null && !interactable.equals(firstPartClicked)){
							if(interactable.linkedPart == null && interactable.linkedVehicle == null){
								if(part.worldPos.distanceTo(firstPartClicked.worldPos) < 15){
									if(interactable.tank.getFluid().isEmpty() || firstPartClicked.tank.getFluid().isEmpty() || interactable.tank.getFluid().equals(firstPartClicked.tank.getFluid())){
										firstPartClicked.linkedPart = interactable;
										MasterLoader.networkInterface.sendToAllClients(new PacketVehiclePartInteractable(firstPartClicked));
										player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.secondlink"));
										firstPartClicked = null;
									}else{
										firstPartClicked = null;
										player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.differentfluids"));
									}
								}else{
									firstPartClicked = null;
									player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.toofar"));
								}
							}else{
								firstPartClicked = null;
								player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.alreadylinked"));
							}
						}
					}else if(part == null){
						if(vehicle.position.distanceTo(firstPartClicked.worldPos) < 15){
							if(vehicle.fuelTank.getFluid().isEmpty() || firstPartClicked.tank.getFluid().isEmpty() || vehicle.fuelTank.getFluid().equals(firstPartClicked.tank.getFluid())){
								firstPartClicked.linkedVehicle = vehicle;
								MasterLoader.networkInterface.sendToAllClients(new PacketVehiclePartInteractable(firstPartClicked));
								player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.secondlink"));
								firstPartClicked = null;
							}else{
								firstPartClicked = null;
								player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.differentfluids"));
							}
						}else{
							firstPartClicked = null;
							player.sendPacket(new PacketPlayerChatMessage("interact.fuelhose.toofar"));
						}
					}
				}
			}
		}
		return CallbackType.NONE;
	}
}
