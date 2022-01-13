package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet used to add/remove riders from an entity.  This packet only appears on clients after the
 * server has added or removed a rider from the entity.  If a position is given, then this rider should
 * be added to that position.  If no position is given, then the rider should be removed.
 * 
 * @author don_bruce
 */
public class PacketEntityRiderChange extends APacketEntityInteract<AEntityE_Interactable<?>, WrapperEntity>{
	private final Point3d position;
	
	public PacketEntityRiderChange(AEntityE_Interactable<?> entity, WrapperEntity rider, Point3d position){
		super(entity, rider);
		this.position = position;
	}
	
	public PacketEntityRiderChange(ByteBuf buf){
		super(buf);
		position = buf.readBoolean() ? readPoint3dFromBuffer(buf) : null;
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeBoolean(position != null);
		if(position != null){
			writePoint3dToBuffer(position, buf);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, AEntityE_Interactable<?> entity, WrapperEntity rider){
		if(position != null){
			entity.addRider(rider, position);
		}else{
			entity.removeRider(rider, null);
		}
		return true;
	}
}
