package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.AEntityE_Multipart;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Packet used to add/remove parts from an entity.  This packet only appears on clients after the
 * server has added or removed a part from the entity.
 * 
 * @author don_bruce
 */
public class PacketPartChange extends APacketEntity<AEntityE_Multipart<?>>{
	private final Point3d partOffset;
	private final ItemPart partItem;
	private final WrapperNBT partData;
	private Point3d parentPartOffset;
	
	public PacketPartChange(AEntityE_Multipart<?> entity, Point3d partOffset){
		super(entity);
		this.partOffset = partOffset;
		this.partItem = null;
		this.partData = null;
		this.parentPartOffset = null;
	}
	
	public PacketPartChange(AEntityE_Multipart<?> entity, APart partAdded){
		super(entity);
		this.partOffset = partAdded.placementOffset;
		this.partItem = partAdded.getItem();
		this.partData = new WrapperNBT();
		partAdded.save(partData);
		this.parentPartOffset = partAdded.parentPart != null ? partAdded.parentPart.placementOffset : null;
	}
	
	public PacketPartChange(ByteBuf buf){
		super(buf);
		this.partOffset = readPoint3dFromBuffer(buf);
		if(buf.readBoolean()){
			this.partItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
			this.partData = readDataFromBuffer(buf);
			if(buf.readBoolean()){
				this.parentPartOffset = readPoint3dFromBuffer(buf);
			}else{
				this.parentPartOffset = null;
			}
		}else{
			this.partItem = null;
			this.partData = null;
			this.parentPartOffset = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(partOffset, buf);
		if(partItem != null){
			buf.writeBoolean(true);
			writeStringToBuffer(partItem.definition.packID, buf);
			writeStringToBuffer(partItem.definition.systemName, buf);
			writeStringToBuffer(partItem.subName, buf);
			writeDataToBuffer(partData, buf);
			if(parentPartOffset != null){
				buf.writeBoolean(true);
				writePoint3dToBuffer(parentPartOffset, buf);
			}else{
				buf.writeBoolean(false);
			}
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, AEntityE_Multipart<?> entity){
		if(partItem == null){
			entity.removePart(entity.getPartAtLocation(partOffset), null);
		}else{
			JSONPartDefinition packVehicleDef = entity.getPackDefForLocation(partOffset);
			entity.addPart(partItem.createPart(entity, packVehicleDef, partData, entity.getPartAtLocation(parentPartOffset)), false);
		}
		return true;
	}
}
