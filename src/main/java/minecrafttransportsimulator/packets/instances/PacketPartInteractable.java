package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIInteractableCrate;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet used to send signals to interactable parts.  This is either used used to link the interactable with
 * a vehicle or part tank for fluid-pumping operations, trigger a GUI to appear on the interactable, or transfer
 * an item from the interactable's inventory from to or from the player's inventory.
 * Sent to servers by the fuel hose item when it does linking in the first case, when a player clicks the
 * interactable in the second, and when a player clicks an item in the interactable GUI (spawned by the second case)
 * in the third.
 * 
 * @author don_bruce
 */
public class PacketPartInteractable extends APacketEntityInteract<PartInteractable, WrapperPlayer>{
	private final int linkedID;
	private final Point3d linkedOffset;
	private final int interactableSlot;
	private final int playerSlot;
	
	public PacketPartInteractable(PartInteractable interactable, WrapperPlayer player){
		super(interactable, player);
		if(interactable.linkedVehicle != null){
			this.linkedID = interactable.linkedVehicle.lookupID;
			this.linkedOffset = null;
		}else if(interactable.linkedPart != null){
			this.linkedID = interactable.linkedPart.entityOn.lookupID;
			this.linkedOffset = interactable.linkedPart.placementOffset;
		}else{
			this.linkedID = -1;
			this.linkedOffset = null;
		}
		this.interactableSlot = -1;
		this.playerSlot = -1;
	}
	
	public PacketPartInteractable(PartInteractable interactable, WrapperPlayer player, int interactableSlot, int playerSlot){
		super(interactable, player);
		if(interactable.linkedVehicle != null){
			this.linkedID = interactable.linkedVehicle.lookupID;
			this.linkedOffset = null;
		}else if(interactable.linkedPart != null){
			this.linkedID = interactable.linkedPart.entityOn.lookupID;
			this.linkedOffset = interactable.linkedPart.placementOffset;
		}else{
			this.linkedID = -1;
			this.linkedOffset = null;
		}
		this.interactableSlot = interactableSlot;
		this.playerSlot = playerSlot;
	}
	
	public PacketPartInteractable(ByteBuf buf){
		super(buf);
		this.linkedID = buf.readInt();
		this.linkedOffset = buf.readBoolean() ? readPoint3dFromBuffer(buf) : null;
		this.interactableSlot = buf.readInt();
		this.playerSlot = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(linkedID);
		if(linkedOffset != null){
			buf.writeBoolean(true);
			writePoint3dToBuffer(linkedOffset, buf);
		}else{
			buf.writeBoolean(false);
		}
		buf.writeInt(interactableSlot);
		buf.writeInt(playerSlot);
	}
	
	@Override
	public boolean handle(WrapperWorld world, PartInteractable interactable, WrapperPlayer player){
		if(linkedID != -1){
			AEntityA_Base linkedEntity = AEntityA_Base.getEntity(world, linkedID);
			if(linkedEntity != null){
				if(linkedOffset == null){
					interactable.linkedVehicle = (EntityVehicleF_Physics) linkedEntity;
				}else{
					interactable.linkedPart = (PartInteractable) ((AEntityE_Multipart<?>) linkedEntity).getPartAtLocation(linkedOffset);
				}
			}
		}else if(interactableSlot != -1){
			player.getInventory().addStack(interactable.inventory.get(interactableSlot));
		}else if(playerSlot != -1){
			WrapperInventory playerInventory = player.getInventory();
			playerInventory.decrementSlot(playerSlot, interactable.addStackToInventory(playerInventory.getStackInSlot(playerSlot)));
		}else{
			if(interactable.definition.interactable.interactionType.equals(InteractableComponentType.CRAFTING_BENCH)){
				InterfaceGUI.openGUI(new GUIPartBench(interactable.definition.interactable.crafting));
			}else if(interactable.definition.interactable.interactionType.equals(InteractableComponentType.CRATE)){
				InterfaceGUI.openGUI(new GUIInteractableCrate(interactable));
			}
		}
		return true;
	}
}
