package minecrafttransportsimulator.packets.vehicles;

import java.lang.reflect.Constructor;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to clients when a part is added to vehicles
 * on the server.  Only clients that are tracking this vehicle
 * will get this packet.  This reduced network load.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientPartAddition extends APacketVehiclePart{
	private String partName;
	private NBTTagCompound partTag;

	public PacketVehicleClientPartAddition(){}
	
	public PacketVehicleClientPartAddition(EntityVehicleA_Base vehicle, double x, double y, double z, String partName, NBTTagCompound partTag){
		super(vehicle, x, y, z);
		this.partName = partName;
		this.partTag = partTag;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		partName = tag.getString("partName");
		partTag = tag.getCompoundTag("partTag");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setString("partName", partName);
		tag.setTag("partTag", partTag);
	}

	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		EntityVehicleA_Base vehicle = (EntityVehicleA_Base) getVehicle(world);
		Point partPoint = getPartPoint(this);
		PackPart packPart = vehicle.getPackDefForLocation(partPoint.x, partPoint.y, partPoint.z);
		try{
			Class<? extends APart> partClass = PackParserSystem.getPartPartClass(partName);
			Constructor<? extends APart> construct = partClass.getConstructor(EntityVehicleE_Powered.class, PackPart.class, String.class, NBTTagCompound.class);
			APart newPart = construct.newInstance((EntityVehicleE_Powered) vehicle, packPart, partName, partTag);
			vehicle.addPart(newPart, false);
		}catch(Exception e){
			MTS.MTSLog.error("ERROR SPAWING PART ON CLIENT!");
			MTS.MTSLog.error(e.getMessage());
		}
	}
}
