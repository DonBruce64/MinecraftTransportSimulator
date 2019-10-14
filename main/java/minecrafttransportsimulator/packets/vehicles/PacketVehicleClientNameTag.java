package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to clients when a vehicle's text
 * has been changed.  The server will already have changed
 * the vehicle's text at this point.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientNameTag extends APacketVehicle{
	private String displayText; 

	public PacketVehicleClientNameTag(){}
	
	public PacketVehicleClientNameTag(EntityVehicleB_Existing vehicle){
		super(vehicle);
		this.displayText = vehicle.displayText;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		displayText = tag.getString("displayText");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setString("displayText", displayText);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		((EntityVehicleB_Existing) getVehicle(world)).displayText = displayText;
	}
}
