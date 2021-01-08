package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.components.APacketVehiclePart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Packet used to add/remove parts from a vehicle.  This packet only appears on clients after the
 * server has added or removed a part from the vehicle.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartChange extends APacketVehiclePart{
	private final ItemPart partItem;
	private final WrapperNBT partData;
	private boolean clickedPart;
	private Point3d partClickedOffset;
	
	public PacketVehiclePartChange(EntityVehicleF_Physics vehicle, Point3d offset){
		super(vehicle, offset);
		this.partItem = null;
		this.partData = null;
		this.partClickedOffset = null;
	}
	
	public PacketVehiclePartChange(EntityVehicleF_Physics vehicle, Point3d offset, ItemPart partItem, WrapperNBT partData, APart partClicked){
		super(vehicle, offset);
		this.partItem = partItem;
		this.partData = partData;
		this.clickedPart = partClicked != null;
		this.partClickedOffset = clickedPart ? partClicked.placementOffset : null;
	}
	
	public PacketVehiclePartChange(ByteBuf buf){
		super(buf);
		if(buf.readBoolean()){
			this.partItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
			this.partData = readDataFromBuffer(buf);
			this.clickedPart = buf.readBoolean();
			if(clickedPart){
				this.partClickedOffset = readPoint3dFromBuffer(buf);
			}else{
				this.partClickedOffset = null;
			}
		}else{
			this.partItem = null;
			this.partData = null;
			this.partClickedOffset = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		if(partItem != null){
			buf.writeBoolean(true);
			writeStringToBuffer(partItem.definition.packID, buf);
			writeStringToBuffer(partItem.definition.systemName, buf);
			writeStringToBuffer(partItem.subName, buf);
			writeDataToBuffer(partData, buf);
			buf.writeBoolean(clickedPart);
			if(clickedPart){
				writePoint3dToBuffer(partClickedOffset, buf);
			}
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		if(partItem == null){
			vehicle.removePart(vehicle.getPartAtLocation(offset), null);
		}else{
			VehiclePart packVehicleDef = vehicle.getPackDefForLocation(offset);
			vehicle.addPart(partItem.createPart(vehicle, packVehicleDef, partData, vehicle.getPartAtLocation(partClickedOffset)));
		}
		return true;
	}
}
