package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packets.components.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartEngine;

/**Packet used to send signals to engines.  This can be a state change or damage from an attack.
 * Constructors are present for each of these situations, though the side this packet is present
 *  on differ between packet types.  For example engine signal data is sent both from clients to
 *   the server, and from the server to clients, while damage information is only sent from
 * servers to clients.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartEngine extends APacketVehiclePart{
	private final Signal packetType;
	private final double hours;
	private final boolean oilLeak;
	private final boolean fuelLeak;
	private final boolean brokenStarter;
	
	public PacketVehiclePartEngine(PartEngine engine, Signal packetType){
		super(engine.vehicle, engine.placementOffset);
		this.packetType = packetType;
		this.hours = 0;
		this.oilLeak = false;
		this.fuelLeak = false;
		this.brokenStarter = false;
	}
	
	public PacketVehiclePartEngine(PartEngine engine, double hours, boolean oilLeak, boolean fuelLeak, boolean brokenStarter){
		super(engine.vehicle, engine.placementOffset);
		this.packetType = Signal.DAMAGE;
		this.hours = hours;
		this.oilLeak = oilLeak;
		this.fuelLeak = fuelLeak;
		this.brokenStarter = brokenStarter;
	}
	
	public PacketVehiclePartEngine(PartEngine engine, int linkedID, Point3d linkedPos){
		super(engine.vehicle, engine.placementOffset);
		this.packetType = Signal.DAMAGE;
		this.hours = 0;
		this.oilLeak = false;
		this.fuelLeak = false;
		this.brokenStarter = false;
	}
	
	public PacketVehiclePartEngine(ByteBuf buf){
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
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		PartEngine engine = (PartEngine) vehicle.getPartAtLocation(offset);
		switch(packetType){
			case MAGNETO_OFF: engine.setMagnetoStatus(false); break;
			case MAGNETO_ON: engine.setMagnetoStatus(true); break;
			case ES_OFF: engine.setElectricStarterStatus(false); break;
			case ES_ON: engine.setElectricStarterStatus(true); break;
			case HS_ON: engine.handStartEngine(); break;
			case BACKFIRE: engine.backfireEngine(); break;
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
		BACKFIRE,
		START,
		FUEL_OUT,
		TOO_SLOW,
		DROWN,
		DAMAGE;
	}
}
