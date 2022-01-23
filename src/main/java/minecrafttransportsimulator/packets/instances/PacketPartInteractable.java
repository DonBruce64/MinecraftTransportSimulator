package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.guis.instances.GUIFurnace;
import minecrafttransportsimulator.guis.instances.GUIInventoryContainer;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet used to send signals to interactable parts.  This is either used used to link the interactable with
 * a vehicle or part tank for fluid-pumping operations, or trigger a GUI to appear on the interactable.
 * Sent to servers by the fuel hose item when it does linking in the first case, and when a player clicks the
 * interactable in the second.
 * in the third.
 * 
 * @author don_bruce
 */
public class PacketPartInteractable extends APacketEntityInteract<PartInteractable, WrapperPlayer>{
	private final UUID linkedID;
	private final Point3d linkedOffset;
	
	public PacketPartInteractable(PartInteractable interactable, WrapperPlayer player){
		super(interactable, player);
		if(interactable.linkedVehicle != null){
			this.linkedID = interactable.linkedVehicle.uniqueUUID;
			this.linkedOffset = null;
		}else if(interactable.linkedPart != null){
			this.linkedID = interactable.linkedPart.entityOn.uniqueUUID;
			this.linkedOffset = interactable.linkedPart.placementOffset;
		}else{
			this.linkedID = null;
			this.linkedOffset = null;
		}
	}
	
	public PacketPartInteractable(ByteBuf buf){
		super(buf);
		if(buf.readBoolean()){
			this.linkedID = readUUIDFromBuffer(buf);
		}else{
			this.linkedID = null;
		}
		this.linkedOffset = buf.readBoolean() ? readPoint3dFromBuffer(buf) : null;
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		if(linkedID != null){
			buf.writeBoolean(true);
			writeUUIDToBuffer(linkedID, buf);
		}else{
			buf.writeBoolean(false);
		}
		if(linkedOffset != null){
			buf.writeBoolean(true);
			writePoint3dToBuffer(linkedOffset, buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, PartInteractable interactable, WrapperPlayer player){
		if(linkedID != null){
			AEntityA_Base linkedEntity = world.getEntity(linkedID);
			if(linkedEntity != null){
				if(linkedOffset == null){
					interactable.linkedVehicle = (EntityVehicleF_Physics) linkedEntity;
				}else{
					interactable.linkedPart = (PartInteractable) ((AEntityF_Multipart<?>) linkedEntity).getPartAtLocation(linkedOffset);
				}
			}
		}else{
			if(interactable.definition.interactable.interactionType.equals(InteractableComponentType.CRAFTING_BENCH)){
				new GUIPartBench(interactable.definition.interactable.crafting);
			}else if(interactable.definition.interactable.interactionType.equals(InteractableComponentType.CRATE)){
				new GUIInventoryContainer(interactable.inventory, interactable.definition.interactable.inventoryTexture, false);
			}else if(interactable.definition.interactable.interactionType.equals(InteractableComponentType.FURNACE)){
				new GUIFurnace(interactable.furnace, interactable.definition.interactable.inventoryTexture);
			}
		}
		return true;
	}
}
