package minecrafttransportsimulator.items.instances;

import java.util.List;

import mcinterface.InterfaceCore;
import mcinterface.InterfaceNetwork;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;

public class ItemJumperCable extends AItemBase implements IItemVehicleInteractable{
	private static PartEngine lastEngineClicked;
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(InterfaceCore.translate("info.item.jumpercable.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(!vehicle.world.isClient()){
			if(rightClick){
				if(part instanceof PartEngine){
					PartEngine engine = (PartEngine) part;
					if(engine.linkedEngine == null){
						if(lastEngineClicked == null){
							lastEngineClicked = engine;
							player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.firstlink"));
						}else if(!lastEngineClicked.equals(engine)){
							if(lastEngineClicked.vehicle.equals(engine.vehicle)){
								lastEngineClicked = null;
								player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.samevehicle"));
							}else if(engine.worldPos.distanceTo(lastEngineClicked.worldPos) < 15){
								engine.linkedEngine = lastEngineClicked;
								lastEngineClicked.linkedEngine = engine;
								InterfaceNetwork.sendToClientsTracking(new PacketVehiclePartEngine(engine, lastEngineClicked), vehicle);
								InterfaceNetwork.sendToClientsTracking(new PacketVehiclePartEngine(lastEngineClicked, engine), lastEngineClicked.vehicle);
								lastEngineClicked = null;
								player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.secondlink"));
							}else{
								lastEngineClicked = null;
								player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.toofar"));
							}
						}
					}else{
						player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.alreadylinked"));
					}
				}
			}
		}
		return CallbackType.NONE;
	}
}
