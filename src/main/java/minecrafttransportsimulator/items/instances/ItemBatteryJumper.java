package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;

public class ItemBatteryJumper extends AItemBase implements IItemVehicleInteractable{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		tooltipLines.add(InterfaceCore.translate("info.item.batteryjumper"));
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			//Use jumper on vehicle.
			vehicle.electricPower = 12;
			if(!vehicle.world.isClient()){
				InterfacePacket.sendToPlayer(new PacketPlayerChatMessage("interact.batteryjumper.success"), player);
				if(!player.isCreative()){
					player.getInventory().removeStack(player.getHeldStack(), 1);
				}
				return CallbackType.ALL;
			}
		}
		return CallbackType.NONE;
	}
}
