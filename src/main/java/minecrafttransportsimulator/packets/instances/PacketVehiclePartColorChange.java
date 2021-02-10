package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketMultipartPart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Packet sent to parts to update their their subName (color).  This gets sent from
 * a client when they change the color in the paint gun GUI.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartColorChange extends APacketMultipartPart{
	private final ItemPart newPartItem;
	
	public PacketVehiclePartColorChange(APart part, ItemPart newPartItem){
		super(part.vehicle, part.placementOffset);
		this.newPartItem = newPartItem;
	}
	
	public PacketVehiclePartColorChange(ByteBuf buf){
		super(buf);
		this.newPartItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(newPartItem.definition.packID, buf);
		writeStringToBuffer(newPartItem.definition.systemName, buf);
		writeStringToBuffer(newPartItem.subName, buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		APart part = vehicle.getPartAtLocation(offset);
		WrapperInventory inventory = player.getInventory();
		if(player.isCreative() || inventory.hasMaterials(newPartItem, false, true)){
			//Remove livery materials (if required) and set new sugName.
			if(!player.isCreative()){
				inventory.removeMaterials(newPartItem, false, true);
			}
			part.subName = newPartItem.subName;
			return true;
		}
		return false;
	}
}
