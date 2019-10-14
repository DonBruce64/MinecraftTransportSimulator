package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.network.PacketBuffer;

/**Base packet used for vehicle-player interaction.  This packet
 * gets sent with the vehicle packet as a super to contain the vehicle
 * id.  If part interaction is needed, use APackerVehiclePartPlayer.
 * 
 * @author don_bruce
 */
public abstract class APacketVehiclePlayer extends APacketVehicle{
	private int playerID;

	public APacketVehiclePlayer(){}
	
	public APacketVehiclePlayer(EntityVehicleA_Base vehicle, int playerID){
		super(vehicle);
		this.playerID = playerID;
	}
	
	@Override
	public void populateFromBytes(PacketBuffer buf){
		super.populateFromBytes(buf);
		this.playerID = buf.readInt();
	}

	@Override
	public void convertToBytes(PacketBuffer buf){
		super.convertToBytes(buf);
		buf.writeInt(this.playerID);
	}
}
