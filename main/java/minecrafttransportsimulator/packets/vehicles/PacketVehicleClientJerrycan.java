package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;


/**This packet is sent to clients when a jerrycan is used
 * to fuel a vehicle.  Adds 1000 fuel from the jerrycan
 * and sets the fluidName to that of the one in the
 * jerrycan.  No need to check if fluids match, as
 * the server will have done that and the server 
 * knows best.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientJerrycan extends APacketVehicle{
	private String fluidName;

	public PacketVehicleClientJerrycan(){}
	
	public PacketVehicleClientJerrycan(EntityVehicleB_Existing vehicle, String fluidName){
		super(vehicle);
		this.fluidName = fluidName;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		fluidName = tag.getString("fluidName");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setString("fluidName", fluidName);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) getVehicle(world);
		vehicle.fluidName = fluidName;
		vehicle.fuel += 1000;
	}
}
