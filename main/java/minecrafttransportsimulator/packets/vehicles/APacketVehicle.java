package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSNetwork.MTSPacket;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.nbt.NBTTagCompound;


/**Base packet for vehicle interaction.  Contains the entity ID of the vehicle,
 * and a helper method for getting it from the world.
 * 
 * @author don_bruce
 */
public abstract class APacketVehicle extends MTSPacket{
	private int id;

	public APacketVehicle(){}
	
	public APacketVehicle(EntityVehicleA_Base vehicle){
		this.id = vehicle.getEntityId();
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		id = tag.getInteger("id");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		tag.setInteger("id", id);
	}
	
	protected EntityVehicleA_Base getVehicle(MTSWorldInterface world){
		return (EntityVehicleA_Base) world.getEntity(id);
	}
}
