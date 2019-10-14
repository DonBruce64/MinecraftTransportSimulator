package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.packets.vehicles.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.nbt.NBTTagCompound;

/**This packet is sent to clients when two engines
 * are linked via jumper cables.  Allows vehicles to
 * update their electrical flow in tandem with the server.
 * We have to check for nulls here, as it's possible that
 * only the first vehicle is being tracked on a client while
 * the second vehicle is not.
 * 
 * @author don_bruce
 */
public class PacketPartClientEngineLinked extends APacketVehiclePart{
	private int linkedId;
	private double linkedX;
	private double linkedY;
	private double linkedZ;

	public PacketPartClientEngineLinked(){}
	
	public PacketPartClientEngineLinked(APartEngine engine, APartEngine engineLinked){
		super(engine);
		this.linkedId = engineLinked.vehicle.getEntityId();
		this.linkedX = engineLinked.offset.x;
		this.linkedY = engineLinked.offset.y;
		this.linkedZ = engineLinked.offset.z;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		linkedId = tag.getInteger("linkedId");
		linkedX = tag.getDouble("linkedX");
		linkedY = tag.getDouble("linkedY");
		linkedZ = tag.getDouble("linkedZ");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setInteger("linkedId", linkedId);
		tag.setDouble("linkedX", linkedX);
		tag.setDouble("linkedY", linkedY);
		tag.setDouble("linkedZ", linkedZ);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		APartEngine engine = (APartEngine) getPart(world);
		EntityVehicleA_Base linkedVehicle = (EntityVehicleA_Base) world.getEntity(linkedId);
		APartEngine linkedEngine = null;
		if(linkedVehicle != null){
			for(APart part : linkedVehicle.getVehicleParts()){
				if(part.offset.x == linkedX && part.offset.y == linkedY && part.offset.z == linkedZ){
					linkedEngine = (APartEngine) part;
				}
			}
		}
		
		if(engine != null && linkedEngine != null){
			engine.linkedEngine = linkedEngine;
			linkedEngine.linkedEngine = engine;
		}
	}
}
