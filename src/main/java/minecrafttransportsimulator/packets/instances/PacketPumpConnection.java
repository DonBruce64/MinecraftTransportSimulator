package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Packet sent to pumps on clients to change what vehicle they are connected to.
 * 
 * @author don_bruce
 */
public class PacketPumpConnection extends APacketTileEntity<TileEntityFuelPump>{
	private final int vehicleID;
	
	public PacketPumpConnection(TileEntityFuelPump pump){
		super(pump);
		this.vehicleID = pump.connectedVehicle != null ? pump.connectedVehicle.getEntityId() : -1;
	}
	
	public PacketPumpConnection(ByteBuf buf){
		super(buf);
		this.vehicleID = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(vehicleID);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityFuelPump pump){
		if(vehicleID != -1){
			EntityVehicleE_Powered vehicle = world.getVehicle(vehicleID); 
			if(vehicle != null && vehicle.definition != null){
				pump.connectedVehicle = vehicle;
				pump.totalTransfered = 0;
			}
		}else{
			pump.connectedVehicle = null;
		}
		return true;
	}
}
