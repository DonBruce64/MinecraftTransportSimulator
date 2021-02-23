package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;

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
							if(lastEngineClicked.entityOn.equals(engine.entityOn)){
								lastEngineClicked = null;
								player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.samevehicle"));
							}else if(engine.position.distanceTo(lastEngineClicked.position) < 15){
								engine.linkedEngine = lastEngineClicked;
								lastEngineClicked.linkedEngine = engine;
								InterfacePacket.sendToAllClients(new PacketPartEngine(engine, lastEngineClicked));
								InterfacePacket.sendToAllClients(new PacketPartEngine(lastEngineClicked, engine));
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
