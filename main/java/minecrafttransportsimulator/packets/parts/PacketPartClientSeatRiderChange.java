package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.packets.vehicles.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;


/**This packet is sent to clients to update the riders
 * in a vehicle when a player mounts or dismounts it.
 * We need to check for nulls here as the vehicle may
 * be tracked but the rider may not be if they weren't
 * close enough when they mounted the vehicle.
 * 
 * @author don_bruce
 */
public class PacketPartClientSeatRiderChange extends APacketVehiclePart{
	private int riderID;
	private boolean mounting;

	public PacketPartClientSeatRiderChange() {}
	
	public PacketPartClientSeatRiderChange(APart part, int riderID, boolean mounting){
		super(part);
		this.riderID = riderID;
		this.mounting = mounting;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		riderID = tag.getInteger("riderID");
		mounting = tag.getBoolean("mounting");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setInteger("riderID", riderID);
		tag.setBoolean("mounting", mounting);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		Entity rider = world.getEntity(riderID);
		PartSeat seat = (PartSeat) getPart(world);
		if(rider != null && seat != null){
			if(mounting){
				seat.vehicle.setRiderInSeat(rider, seat);
			}else{
				seat.vehicle.removeRiderFromSeat(rider, seat);
			}
		}
	}
}
