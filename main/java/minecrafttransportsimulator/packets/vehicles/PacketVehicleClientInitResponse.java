package minecrafttransportsimulator.packets.vehicles;

import java.io.IOException;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

/**This packet is sent to clients when the server gets a request to
 * send pack data to them.  We send over the NBT data to the
 * client, which will allow them to look-up the pack data on
 * their side.  It also allows the client to get extra saved
 * data like fuel level and parts that are present on the vehicle.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientInitResponse extends APacketVehicle{
	private NBTTagCompound tag;

	public PacketVehicleClientInitResponse(){}
	
	public PacketVehicleClientInitResponse(EntityVehicleA_Base vehicle, NBTTagCompound tag){
		super(vehicle);
		this.tag=tag;
	}
	
	@Override
	public void populateFromBytes(PacketBuffer buf){
		super.populateFromBytes(buf);
		try{
			this.tag = buf.readCompoundTag();
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public void convertToBytes(PacketBuffer buf){
		super.convertToBytes(buf);
		buf.writeCompoundTag(tag);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, MTSPlayerInterface player, boolean onServer){
		EntityVehicleA_Base vehicle = getVehicle(this, world);
		if(vehicle != null){
			vehicle.handleLoad(tag);
		}
	}
}
