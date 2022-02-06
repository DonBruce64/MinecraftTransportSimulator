package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet used to interact with vehicles.  Initially sent from clients to the server
 * to handle players clicking on the vehicle.  Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the vehicle if the
 * action requires updates on clients.  This can be driven by the logic in this packet, or
 * the logic in {@link IItemVehicleInteractable#doVehicleInteraction(WrapperItemStack, EntityVehicleF_Physics, APart, WrapperPlayer, PlayerOwnerState, boolean)}
 * 
 * @author don_bruce
 */
public class PacketVehicleInteract extends APacketEntityInteract<EntityVehicleF_Physics, WrapperPlayer>{
	private final UUID hitPartUniqueUUID;
	private final Point3d hitBoxLocalCenter;
	private final boolean rightClick;
		
	public PacketVehicleInteract(EntityVehicleF_Physics vehicle, WrapperPlayer player, BoundingBox hitBox, boolean rightClick){
		super(vehicle, player);
		APart hitPart = vehicle.getPartWithBox(hitBox);
		this.hitPartUniqueUUID = hitPart != null ? hitPart.uniqueUUID : null;
		this.hitBoxLocalCenter = hitBox.localCenter;
		this.rightClick = rightClick;
	}
	
	public PacketVehicleInteract(ByteBuf buf){
		super(buf);
		if(buf.readBoolean()){
			this.hitPartUniqueUUID = readUUIDFromBuffer(buf);
		}else{
			this.hitPartUniqueUUID = null;
		}
		this.hitBoxLocalCenter = readPoint3dFromBuffer(buf);
		this.rightClick = buf.readBoolean();
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		if(hitPartUniqueUUID != null){
			buf.writeBoolean(true);
			writeUUIDToBuffer(hitPartUniqueUUID, buf);
		}else{
			buf.writeBoolean(false);
		}
		writePoint3dToBuffer(hitBoxLocalCenter, buf);
		buf.writeBoolean(rightClick);
	}

	@Override
	public boolean handle(WrapperWorld world, EntityVehicleF_Physics vehicle, WrapperPlayer player){
		PlayerOwnerState ownerState = vehicle.getOwnerState(player);
		WrapperItemStack heldStack = player.getHeldStack();
		AItemBase heldItem = heldStack.getItem();
		
		//Get the part we hit, if one was specified.
		APart part = hitPartUniqueUUID != null ? world.getEntity(hitPartUniqueUUID) : null;
		
		//Get the bounding box hit for future operations.
		BoundingBox hitBox = null;
		
		//First check part slots.  But don't do that if we hit a part, as obviously we didn't hit a slot.
		if(part == null){
			for(BoundingBox box : vehicle.allPartSlotBoxes.keySet()){
				if(box.localCenter.equals(hitBoxLocalCenter)){
					hitBox = box;
					break;
				}
			}
		}
		
		//If we didn't get the box from the part slot, get it from the main list.
		if(hitBox == null){
			for(BoundingBox box : (part != null ? part.interactionBoxes : vehicle.interactionBoxes)){
				if(box.localCenter.equals(hitBoxLocalCenter)){
					hitBox = box;
					break;
				}
			}
			
			if(hitBox == null){
				//Not sure how the heck this happened, but it did.
				return false;
			}
		}
		
		//Check if we clicked a part slot box.  This takes priority as part placement
		//should always be checked before part interaction.
		if(vehicle.allPartSlotBoxes.containsKey(hitBox)){
			//Only owners can add vehicle parts.
			if(ownerState.equals(PlayerOwnerState.USER)){
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehicleowned"));
			}else{
				//Attempt to add a part.  Vehicle is responsible for callback packet here.
				if(heldItem instanceof AItemPart){
					if(vehicle.addPartFromItem((AItemPart) heldItem, player, heldStack.getData(), hitBoxLocalCenter, false) != null && !player.isCreative()){				
						player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
					}
				}
			}
			return false;
		}
		
		//If we clicked with with an item that can interact with a part or vehicle, perform that interaction.
		//If the item doesn't or couldn't interact with the vehicle, check for other interactions.
		boolean hadAllCondition = false;
		if(heldItem instanceof IItemVehicleInteractable){
			switch(((IItemVehicleInteractable) heldItem).doVehicleInteraction(vehicle, part, hitBox, player, ownerState, rightClick)){
				case ALL: return true;
				case ALL_AND_MORE: hadAllCondition = true; break;
				case PLAYER: player.sendPacket(this); return false;
				case NONE: return false;
				case SKIP: //Don't return anything, continue processing.
			}
		}
		
		//Check if we clicked a box with a variable attached.
		if(hitBox.definition != null && hitBox.definition.variableName != null){
			//Can't touch locked vehicles.
			if(vehicle.locked && !hadAllCondition){
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehiclelocked"));
			}else{
				AEntityD_Definable<?> entity = part != null ? part : vehicle;
				switch(hitBox.definition.variableType){
					case INCREMENT:
						entity.setVariable(hitBox.definition.variableName, entity.getVariable(hitBox.definition.variableName) + hitBox.definition.variableValue);
						InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(entity, hitBox.definition.variableName, hitBox.definition.variableValue));
						break;
					case SET:
						entity.setVariable(hitBox.definition.variableName, hitBox.definition.variableValue);
						InterfacePacket.sendToAllClients(new PacketEntityVariableSet(entity, hitBox.definition.variableName, hitBox.definition.variableValue));
						break;
					case TOGGLE:{
						entity.toggleVariable(hitBox.definition.variableName);
						InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(entity, hitBox.definition.variableName));	
						break;
					}
				}
				
			}
			return false;
		}
		
		//Not holding an item that can interact with a vehicle.  Try to interact with the vehicle itself.
		if(part != null){
			if(rightClick){
				part.interact(player);
			}else{
				part.attack(new Damage("player", 1.0F, part.boundingBox, null, player));
			}
		}
		return hadAllCondition;
	}
}
