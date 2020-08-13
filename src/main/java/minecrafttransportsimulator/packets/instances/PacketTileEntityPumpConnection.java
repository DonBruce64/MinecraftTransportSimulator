package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet sent to pumps on clients to change what vehicle they are connected to.
 * 
 * @author don_bruce
 */
public class PacketTileEntityPumpConnection extends APacketTileEntity<TileEntityFuelPump>{
	private final int vehicleID;
	
	public PacketTileEntityPumpConnection(TileEntityFuelPump pump){
		super(pump);
		this.vehicleID = pump.connectedVehicle != null ? pump.connectedVehicle.lookupID : -1;
	}
	
	public PacketTileEntityPumpConnection(ByteBuf buf){
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
			EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) AEntityBase.createdEntities.get(vehicleID); 
			if(vehicle != null){
				pump.connectedVehicle = vehicle;
				pump.getTank().resetAmountDispensed();
			}
		}else{
			pump.connectedVehicle = null;
		}
		return true;
	}
}
