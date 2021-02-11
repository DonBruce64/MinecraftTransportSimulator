package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.AEntityA_Base;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet sent to pumps on clients to change what vehicle they are connected to.
 * 
 * @author don_bruce
 */
public class PacketTileEntityFuelPumpConnection extends APacketEntity<TileEntityFuelPump>{
	private final int linkedID;
	private final boolean connect;
	
	public PacketTileEntityFuelPumpConnection(TileEntityFuelPump pump, boolean connect){
		super(pump);
		this.linkedID = pump.connectedVehicle.lookupID;
		this.connect = connect;
	}
	
	public PacketTileEntityFuelPumpConnection(ByteBuf buf){
		super(buf);
		this.linkedID = buf.readInt();
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(linkedID);
		buf.writeBoolean(connect);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityFuelPump pump){
		EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) AEntityA_Base.getEntity(world, linkedID);
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
