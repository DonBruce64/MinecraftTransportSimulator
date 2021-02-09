package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketMultipartPart;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;

/**Packet used to send signals to interactable parts.  Currently, this is only used to link the interactable with
 * a vehicle or part tank for fluid-pumping operations.  Sent to servers by the fuel hose item when it does linking.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartInteractable extends APacketMultipartPart{
	private final int linkedID;
	private final Point3d linkedOffset;
	
	public PacketVehiclePartInteractable(PartInteractable interactable){
		super(interactable.vehicle, interactable.placementOffset);
		if(interactable.linkedVehicle != null){
			this.linkedID = interactable.linkedVehicle.lookupID;
			this.linkedOffset = null;
		}else{
			this.linkedID = interactable.linkedPart.vehicle.lookupID;
			this.linkedOffset = interactable.linkedPart.placementOffset;
		}
	}
	
	public PacketVehiclePartInteractable(ByteBuf buf){
		super(buf);
		this.linkedID = buf.readInt();
		this.linkedOffset = buf.readBoolean() ? readPoint3dFromBuffer(buf) : null;
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(linkedID);
		if(linkedOffset != null){
			buf.writeBoolean(true);
			writePoint3dToBuffer(linkedOffset, buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		PartInteractable interactable = (PartInteractable) vehicle.getPartAtLocation(offset);
		for(AEntityBase entity : AEntityBase.createdClientEntities){
			if(entity.lookupID == linkedID){
				if(linkedOffset == null){
					interactable.linkedVehicle = (EntityVehicleF_Physics) entity;
				}else{
					interactable.linkedPart = (PartInteractable) ((EntityVehicleF_Physics) entity).getPartAtLocation(linkedOffset);
				}
				break;
			}
		}
		return true;
	}
}
