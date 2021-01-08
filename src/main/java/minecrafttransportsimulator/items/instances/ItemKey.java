package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

public class ItemKey extends AItemBase implements IItemVehicleInteractable{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(MasterLoader.coreInterface.translate("info.item.key.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, IWrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(!vehicle.world.isClient()){
			if(rightClick){
				if(player.isSneaking()){
					//Try to change ownership of the vehicle.
					if(vehicle.ownerUUID.isEmpty()){
						vehicle.ownerUUID = player.getUUID();
						player.sendPacket(new PacketPlayerChatMessage("interact.key.info.own"));
					}else{
						if(!ownerState.equals(PlayerOwnerState.USER)){
							vehicle.ownerUUID = "";
							player.sendPacket(new PacketPlayerChatMessage("interact.key.info.unown"));
						}else{
							player.sendPacket(new PacketPlayerChatMessage("interact.key.failure.alreadyowned"));
						}
					}
				}else{
					//Try to lock the vehicle.
					//First check to see if we need to set this key's vehicle.
					IWrapperItemStack stack = player.getHeldStack();
					WrapperNBT data = stack.getData();
					String keyVehicleUUID = data.getString("vehicle");
					if(keyVehicleUUID.isEmpty()){
						//Check if we are the owner before making this a valid key.
						if(!vehicle.ownerUUID.isEmpty() && ownerState.equals(PlayerOwnerState.USER)){
							player.sendPacket(new PacketPlayerChatMessage("interact.key.failure.notowner"));
							return CallbackType.NONE;
						}
						
						keyVehicleUUID = vehicle.uniqueUUID;
						data.setString("vehicle", keyVehicleUUID);
						stack.setData(data);
					}
					
					//Try to lock or unlock this vehicle.
					//If we succeed, send callback to clients to change locked state.
					if(!keyVehicleUUID.equals(vehicle.uniqueUUID)){
						player.sendPacket(new PacketPlayerChatMessage("interact.key.failure.wrongkey"));
					}else{
						if(vehicle.locked){
							vehicle.locked = false;
							player.sendPacket(new PacketPlayerChatMessage("interact.key.info.unlock"));
							//If we aren't in this vehicle, and we clicked a seat, start riding the vehicle.
							if(part instanceof PartSeat && player.getEntityRiding() == null){
								part.interact(player);
							}
						}else{
							vehicle.locked = true;
							player.sendPacket(new PacketPlayerChatMessage("interact.key.info.lock"));
						}
						return CallbackType.ALL;
					}
				}
			}
		}else{
			vehicle.locked = !vehicle.locked;
		}
		return CallbackType.NONE;
	}
	
	@Override
	public boolean canBeStacked(){
		return false;
	}
}
