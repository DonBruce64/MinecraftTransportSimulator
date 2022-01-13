package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to send signals to engines.  This can be a state change or damage from an attack.
 * Constructors are present for each of these situations, though the side this packet is present
 * on differ between packet types.  For example engine signal data is sent both from clients to
 * the server, and from the server to clients, while damage information is only sent from
 * servers to clients.
 * 
 * @author don_bruce
 */
public class PacketPartEngine extends APacketEntity<PartEngine>{
	private final Signal packetType;
	private final double hours;
	private final boolean oilLeak;
	private final boolean fuelLeak;
	private final boolean brokenStarter;
	private final UUID linkedID;
	private final Point3d linkedPos;
	
	public PacketPartEngine(PartEngine engine, Signal packetType){
		super(engine);
		this.packetType = packetType;
		this.hours = 0;
		this.oilLeak = false;
		this.fuelLeak = false;
		this.brokenStarter = false;
		this.linkedID = null;
		this.linkedPos = null;
	}
	
	public PacketPartEngine(PartEngine engine, double hours, boolean oilLeak, boolean fuelLeak, boolean brokenStarter){
		super(engine);
		this.packetType = Signal.DAMAGE;
		this.hours = hours;
		this.oilLeak = oilLeak;
		this.fuelLeak = fuelLeak;
		this.brokenStarter = brokenStarter;
		this.linkedID = null;
		this.linkedPos = null;
	}
	
	public PacketPartEngine(PartEngine engine, PartEngine linkedEngine){
		super(engine);
		this.packetType = Signal.LINK;
		this.hours = 0;
		this.oilLeak = false;
		this.fuelLeak = false;
		this.brokenStarter = false;
		this.linkedID = linkedEngine.entityOn.uniqueUUID;
		this.linkedPos = linkedEngine.placementOffset;
	}
	
	public PacketPartEngine(ByteBuf buf){
		super(buf);
		this.packetType = Signal.values()[buf.readByte()];
		if(packetType.equals(Signal.DAMAGE)){
			this.hours = buf.readDouble();
			this.oilLeak = buf.readBoolean();
			this.fuelLeak = buf.readBoolean();
			this.brokenStarter = buf.readBoolean();
		}else{
			this.hours = 0;
			this.oilLeak = false;
			this.fuelLeak = false;
			this.brokenStarter = false;
		}
		if(packetType.equals(Signal.LINK)){
			this.linkedID = readUUIDFromBuffer(buf);
			this.linkedPos = readPoint3dFromBuffer(buf);
		}else{
			this.linkedID = null;
			this.linkedPos = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(packetType.ordinal());
		if(packetType.equals(Signal.DAMAGE)){
			buf.writeDouble(hours);
			buf.writeBoolean(oilLeak);
			buf.writeBoolean(fuelLeak);
			buf.writeBoolean(brokenStarter);
		}else if(packetType.equals(Signal.LINK)){
			writeUUIDToBuffer(linkedID, buf);
			writePoint3dToBuffer(linkedPos, buf);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, PartEngine engine){
		switch(packetType){
			case HS_ON: engine.handStartEngine(); break;
			case AS_ON: engine.autoStartEngine(); break;
			case BACKFIRE: engine.backfireEngine(); break;
			case BAD_SHIFT: engine.badShiftEngine(); break;
			case START: engine.startEngine(); break;
			case FUEL_OUT: engine.stallEngine(packetType); break;
			case TOO_SLOW: engine.stallEngine(packetType); break;
			case DEAD_VEHICLE: engine.stallEngine(packetType); break;
			case DROWN: engine.stallEngine(packetType); break;
			case DAMAGE: {
				engine.hours += hours;
				if(fuelLeak){
					engine.fuelLeak = true;
				}
				if(oilLeak){
					engine.oilLeak = true;
				}
				if(brokenStarter){
					engine.brokenStarter = true;
				}
				break;
			}case LINK: {
				AEntityF_Multipart<?> otherEntity = world.getEntity(linkedID);
				if(otherEntity != null){
					for(APart part : otherEntity.parts){
						if(part.placementOffset.equals(linkedPos)){
							((PartEngine) part).linkedEngine = engine;
							engine.linkedEngine = (PartEngine) part;
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	public enum Signal{
		HS_ON,
		AS_ON,
		BACKFIRE,
		BAD_SHIFT,
		START,
		FUEL_OUT,
		DEAD_VEHICLE,
		TOO_SLOW,
		DROWN,
		DAMAGE,
		LINK;
	}
}
