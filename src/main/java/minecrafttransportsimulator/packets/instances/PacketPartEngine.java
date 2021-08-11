package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
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
	private final int linkedID;
	private final Point3d linkedPos;
	
	public PacketPartEngine(PartEngine engine, Signal packetType){
		super(engine);
		this.packetType = packetType;
		this.hours = 0;
		this.oilLeak = false;
		this.fuelLeak = false;
		this.brokenStarter = false;
		this.linkedID = 0;
		this.linkedPos = null;
	}
	
	public PacketPartEngine(PartEngine engine, double hours, boolean oilLeak, boolean fuelLeak, boolean brokenStarter){
		super(engine);
		this.packetType = Signal.DAMAGE;
		this.hours = hours;
		this.oilLeak = oilLeak;
		this.fuelLeak = fuelLeak;
		this.brokenStarter = brokenStarter;
		this.linkedID = 0;
		this.linkedPos = null;
	}
	
	public PacketPartEngine(PartEngine engine, PartEngine linkedEngine){
		super(engine);
		this.packetType = Signal.LINK;
		this.hours = 0;
		this.oilLeak = false;
		this.fuelLeak = false;
		this.brokenStarter = false;
		this.linkedID = linkedEngine.entityOn.lookupID;
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
			this.linkedID = buf.readInt();
			this.linkedPos = readPoint3dFromBuffer(buf);
		}else{
			this.linkedID = 0;
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
			buf.writeInt(linkedID);
			writePoint3dToBuffer(linkedPos, buf);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, PartEngine engine){
		switch(packetType){
			case MAGNETO_OFF: engine.setMagnetoStatus(false); break;
			case MAGNETO_ON: engine.setMagnetoStatus(true); break;
			case ES_OFF: engine.setElectricStarterStatus(false); break;
			case ES_ON: engine.setElectricStarterStatus(true); break;
			case HS_ON: engine.handStartEngine(); break;
			case AS_ON: engine.autoStartEngine(); break;
			case BACKFIRE: engine.backfireEngine(); break;
			case SHIFT_UP_AUTO: engine.shiftUp(true); break;
			case SHIFT_DN_AUTO: engine.shiftDown(true); break;
			case SHIFT_UP_MANUAL: engine.shiftUp(false); break;
			case SHIFT_DN_MANUAL: engine.shiftDown(false); break;
			case BAD_SHIFT: engine.badShiftEngine(); break;
			case START: engine.startEngine(); break;
			case FUEL_OUT: engine.stallEngine(packetType); break;
			case TOO_SLOW: engine.stallEngine(packetType); break;
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
				AEntityE_Multipart<?> otherEntity = (AEntityE_Multipart<?>) AEntityA_Base.getEntity(world, linkedID);
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
		MAGNETO_OFF,
		MAGNETO_ON,
		ES_OFF,
		ES_ON,
		HS_ON,
		AS_ON,
		BACKFIRE,
		SHIFT_UP_AUTO,
		SHIFT_DN_AUTO,
		SHIFT_UP_MANUAL,
		SHIFT_DN_MANUAL,
		BAD_SHIFT,
		START,
		FUEL_OUT,
		TOO_SLOW,
		DROWN,
		DAMAGE,
		LINK;
	}
}
