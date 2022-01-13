package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet sent to pumps on clients to change what vehicle they are connected to.
 * 
 * @author don_bruce
 */
public class PacketTileEntityFuelPumpConnection extends APacketEntity<TileEntityFuelPump>{
	private final UUID linkedID;
	private final boolean connect;
	
	public PacketTileEntityFuelPumpConnection(TileEntityFuelPump pump, boolean connect){
		super(pump);
		this.linkedID = pump.connectedVehicle.uniqueUUID;
		this.connect = connect;
	}
	
	public PacketTileEntityFuelPumpConnection(ByteBuf buf){
		super(buf);
		this.linkedID = readUUIDFromBuffer(buf);
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeUUIDToBuffer(linkedID, buf);
		buf.writeBoolean(connect);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, TileEntityFuelPump pump){
		EntityVehicleF_Physics vehicle = world.getEntity(linkedID);
		if(vehicle != null){
			if(connect){
				pump.connectedVehicle = vehicle;
				vehicle.beingFueled = true;
				pump.getTank().resetAmountDispensed();
			}else{
				vehicle.beingFueled = false;
				pump.connectedVehicle = null;
			}
		}
		return true;
	}
}
