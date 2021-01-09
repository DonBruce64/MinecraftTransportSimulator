package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Packet sent to vehicles to update their their subName (color).  This gets sent from
 * a client when they change the color in the paint gun GUI.  Change is applied to the
 * vehicle and all parts (if applicable).
 * 
 * @author don_bruce
 */
public class PacketVehicleColorChange extends APacketVehicle{
	private final ItemVehicle newVehicleItem;
	
	public PacketVehicleColorChange(EntityVehicleF_Physics vehicle, ItemVehicle newVehicleItem){
		super(vehicle);
		this.newVehicleItem = newVehicleItem;
	}
	
	public PacketVehicleColorChange(ByteBuf buf){
		super(buf);
		this.newVehicleItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(newVehicleItem.definition.packID, buf);
		writeStringToBuffer(newVehicleItem.definition.systemName, buf);
		writeStringToBuffer(newVehicleItem.subName, buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		WrapperInventory inventory = player.getInventory();
		if(player.isCreative() || inventory.hasMaterials(newVehicleItem, false, true)){
			//Remove livery materials (if required) and set new sugName.
			if(!player.isCreative()){
				inventory.removeMaterials(newVehicleItem, false, true);
			}
			vehicle.currentSubName = newVehicleItem.subName;
			
			//If we have a secondTone, change parts to match if possible.
			String secondTone = null;
			for(JSONSubDefinition subDefinition : vehicle.definition.definitions){
				if(subDefinition.subName.equals(vehicle.currentSubName)){
					secondTone = subDefinition.secondTone;
				}
			}
			if(secondTone != null){
				for(APart part : vehicle.parts){
					for(JSONSubDefinition subDefinition : part.definition.definitions){
						if(subDefinition.subName.equals(secondTone)){
							part.currentSubName = secondTone;
							break;
						}
					}
				}
			}
			return true;
		}
		return false;
	}
}
