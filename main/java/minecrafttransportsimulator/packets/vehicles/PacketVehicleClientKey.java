package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to clients when a vehicle is locked or
 * unlocked by a key.  The server will already have changed
 * state and ensured that the key interaction was a valid one.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientKey extends APacketVehicle{
	private boolean isLocked;
	
	public PacketVehicleClientKey(){}
	
	public PacketVehicleClientKey(EntityVehicleB_Existing vehicle){
		super(vehicle);
		this.isLocked = vehicle.locked;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		isLocked = tag.getBoolean("isLocked");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setBoolean("isLocked", isLocked);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		((EntityVehicleB_Existing) getVehicle(world)).locked = isLocked;
	}
}
