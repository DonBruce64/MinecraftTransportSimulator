package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleD_Moving;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to clients whenever a vehicle moves.
 * It is responsible for adding values to the delta variables
 * to tell the client how far the vehicle has moved on the server.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientDeltas extends APacketVehicle{
	private double deltaX;
	private double deltaY;
	private double deltaZ;
	private float deltaYaw;
	private float deltaPitch;
	private float deltaRoll;

	public PacketVehicleClientDeltas(){}
	
	public PacketVehicleClientDeltas(EntityVehicleD_Moving vehicle, double deltaX, double deltaY, double deltaZ, float deltaYaw, float deltaPitch, float deltaRoll){
		super(vehicle);
		this.deltaX=deltaX;
		this.deltaY=deltaY;
		this.deltaZ=deltaZ;
		this.deltaYaw=deltaYaw;
		this.deltaPitch=deltaPitch;
		this.deltaRoll=deltaRoll;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		deltaX = tag.getDouble("deltaX");
		deltaY = tag.getDouble("deltaY");
		deltaZ = tag.getDouble("deltaZ");
		deltaYaw = tag.getFloat("deltaYaw");
		deltaPitch = tag.getFloat("deltaPitch");
		deltaRoll = tag.getFloat("deltaRoll");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setDouble("deltaX", deltaX);
		tag.setDouble("deltaY", deltaY);
		tag.setDouble("deltaZ", deltaZ);
		tag.setFloat("deltaYaw", deltaYaw);
		tag.setFloat("deltaPitch", deltaPitch);
		tag.setFloat("deltaRoll", deltaRoll);
	}

	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		((EntityVehicleD_Moving) getVehicle(world)).addToServerDeltas(deltaX, deltaY, deltaZ, deltaYaw, deltaPitch, deltaRoll);
	}
}
