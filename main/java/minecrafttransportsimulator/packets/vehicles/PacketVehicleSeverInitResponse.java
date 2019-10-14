package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to clients when the server gets a request to
 * send pack data to them.  We send over the NBT data to the
 * client, which will allow them to look-up the pack data on
 * their side.  It also allows the client to get extra saved
 * data like fuel level and parts that are present on the vehicle.
 * 
 * @author don_bruce
 */
public class PacketVehicleSeverInitResponse extends APacketVehicle{
	private NBTTagCompound vehicleNBT;

	public PacketVehicleSeverInitResponse(){}
	
	public PacketVehicleSeverInitResponse(EntityVehicleA_Base vehicle, NBTTagCompound tag){
		super(vehicle);
		this.vehicleNBT=tag;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		vehicleNBT = tag.getCompoundTag("vehicleNBT");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setTag("vehicleNBT", vehicleNBT);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		getVehicle(world).handleLoad(vehicleNBT);
	}
}
