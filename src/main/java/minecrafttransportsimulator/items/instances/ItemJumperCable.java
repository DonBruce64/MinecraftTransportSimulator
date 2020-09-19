package minecrafttransportsimulator.items.instances;

import java.util.List;

import mcinterface.InterfaceCore;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
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
		if(rightClick){
			if(part instanceof PartEngine){
				PartEngine engine = (PartEngine) part;
				if(engine.linkedEngine == null){
					if(lastEngineClicked == null){
						lastEngineClicked = engine;
						if(vehicle.world.isClient()){
							player.displayChatMessage("interact.jumpercable.firstlink");
						}else{
							return CallbackType.ALL;
						}
					}else if(!lastEngineClicked.equals(engine)){
						if(lastEngineClicked.vehicle.equals(engine.vehicle)){
							lastEngineClicked = null;
							if(vehicle.world.isClient()){
								player.displayChatMessage("interact.jumpercable.samevehicle");
							}else{
								return CallbackType.ALL;
							}
						}else if(engine.worldPos.distanceTo(lastEngineClicked.worldPos) < 15){
							engine.linkedEngine = lastEngineClicked;
							lastEngineClicked.linkedEngine = engine;
							lastEngineClicked = null;
							if(vehicle.world.isClient()){
								player.displayChatMessage("interact.jumpercable.secondlink");
							}else{
								return CallbackType.ALL;
							}
						}else{
							lastEngineClicked = null;
							if(vehicle.world.isClient()){
								player.displayChatMessage("interact.jumpercable.toofar");
							}else{
								return CallbackType.ALL;
							}
						}
					}
				}else{
					if(vehicle.world.isClient()){
						player.displayChatMessage("interact.jumpercable.alreadylinked");
					}else{
						return CallbackType.ALL;
					}
				}
			}
		}
		return CallbackType.NONE;
	}
}
