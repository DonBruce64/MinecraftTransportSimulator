package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to send signals to interactable parts.  Currently, this is only used to link the interactable with
 * a vehicle or part tank for fluid-pumping operations.  Sent to servers by the fuel hose item when it does linking.
 * 
 * @author don_bruce
 */
public class PacketPartInteractable extends APacketEntity<PartInteractable>{
	private final int linkedID;
	private final Point3d linkedOffset;
	
	public PacketPartInteractable(PartInteractable interactable){
		super(interactable);
		if(interactable.linkedVehicle != null){
			this.linkedID = interactable.linkedVehicle.lookupID;
			this.linkedOffset = null;
		}else{
			this.linkedID = interactable.linkedPart.entityOn.lookupID;
			this.linkedOffset = interactable.linkedPart.placementOffset;
		}
	}
	
	public PacketPartInteractable(ByteBuf buf){
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
	public boolean handle(WrapperWorld world, WrapperPlayer player, PartInteractable interactable){
		AEntityA_Base linkedEntity = AEntityA_Base.getEntity(world, linkedID);
		if(linkedEntity != null){
			if(linkedOffset == null){
				interactable.linkedVehicle = (EntityVehicleF_Physics) linkedEntity;
			}else{
				interactable.linkedPart = (PartInteractable) ((AEntityE_Multipart<?>) linkedEntity).getPartAtLocation(linkedOffset);
			}
		}
		return true;
	}
}
