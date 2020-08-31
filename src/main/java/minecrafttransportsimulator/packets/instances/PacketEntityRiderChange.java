package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperEntity;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Packet used to add/remove riders from an entity.  This packet only appears on clients after the
 * server has added or removed a rider from the entity.  If a position is given, then this rider should
 * be added to that position.  If no position is given, then the rider should be removed.
 * 
 * @author don_bruce
 */
public class PacketEntityRiderChange extends APacketEntity{
	private final int riderID;
	private final Point3d position;
	
	public PacketEntityRiderChange(AEntityBase entity, WrapperEntity rider, Point3d position){
		super(entity);
		this.riderID = rider.getID();
		this.position = position;
	}
	
	public PacketEntityRiderChange(ByteBuf buf){
		super(buf);
		this.riderID = buf.readInt();
		position = buf.readBoolean() ? readPoint3dFromBuffer(buf) : null;
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(riderID);
		buf.writeBoolean(position != null);
		if(position != null){
			writePoint3dToBuffer(position, buf);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, AEntityBase entity){
		if(position != null){
			//Correlate the position object with the entity position objects.
			//Need to pass-in the actual object to ensure hash equality.
			for(Point3d ridablePosition : entity.locationRiderMap.keySet()){
				if(ridablePosition.equals(position)){
					entity.addRider(world.getEntity(riderID), ridablePosition);
					return true;
				}
			}
			entity.addRider(world.getEntity(riderID), position);
		}else{
			entity.removeRider(world.getEntity(riderID), null);
		}
		return true;
	}
}
