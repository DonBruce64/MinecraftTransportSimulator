package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSNetwork;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to servers when a vehicle is spawned on a client.
 * It is responsible for telling the server that the vehicle has gotten
 * the spawn packet and is ready for the server to send pack data.  We
 * don't send this when the vehicle is spawned as it is possible for
 * the packet to get to the client before the spawn packet and be
 * discarded due to the vehicle not yet being present.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientInit extends APacketVehicle{

	public PacketVehicleClientInit(){}
	
	public PacketVehicleClientInit(EntityVehicleA_Base vehicle){
		super(vehicle);
	}

	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		EntityVehicleA_Base vehicle = getVehicle(world);
		MTSNetwork.sendPacketToClient(new PacketVehicleSeverInitResponse(vehicle, vehicle.handleSave(new NBTTagCompound())), player);
	}
}
