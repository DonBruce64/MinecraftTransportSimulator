package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.components.APacketVehiclePart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet used to add/remove parts from a vehicle.  This packet only appears on clients after the
 * server has added or removed a part from the vehicle.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartChange extends APacketVehiclePart{
	private final String partPackID;
	private final String partSystemName;
	private final WrapperNBT partData;
	
	public PacketVehiclePartChange(EntityVehicleF_Physics vehicle, Point3d offset){
		super(vehicle, offset);
		this.partPackID = "";
		this.partSystemName = "";
		this.partData = null;
	}
	
	public PacketVehiclePartChange(EntityVehicleF_Physics vehicle, Point3d offset, String partPackID, String partSystemName, WrapperNBT partData){
		super(vehicle, offset);
		this.partPackID = partPackID;
		this.partSystemName = partSystemName;
		this.partData = partData;
	}
	
	public PacketVehiclePartChange(ByteBuf buf){
		super(buf);
		this.partPackID = readStringFromBuffer(buf);
		if(!partPackID.isEmpty()){
			this.partSystemName = readStringFromBuffer(buf);
			this.partData = new WrapperNBT(buf);
		}else{
			this.partSystemName = "";
			this.partData = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(partPackID, buf);
		if(!partPackID.isEmpty()){
			writeStringToBuffer(partSystemName, buf);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		if(partPackID.isEmpty()){
			vehicle.removePart(vehicle.getPartAtLocation(offset), null, false);
		}else{
			VehiclePart packPart = vehicle.getPackDefForLocation(offset);
			vehicle.addPart(PackParserSystem.createPart(vehicle, packPart, (JSONPart) MTSRegistry.packItemMap.get(partPackID).get(partSystemName).definition, partData), false);
		}
		return true;
	}
}
