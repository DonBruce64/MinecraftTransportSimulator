package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONDoor;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import net.minecraft.item.ItemStack;

/**Packet used to interact with vehicles.  Initially sent from clients to the server
 * to handle players clicking on the vehicle.  Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the vehicle if the
 * action requires updates on clients.  This can be driven by the logic in this packet, or
 * the logic in {@link IItemVehicleInteractable#doVehicleInteraction(ItemStack, EntityVehicleF_Physics, APart, WrapperPlayer, PlayerOwnerState, boolean)}
 * 
 * @author don_bruce
 */
public class PacketVehicleInteract extends APacketEntityInteract<EntityVehicleF_Physics, WrapperPlayer>{
	private final int hitPartLookupID;
	private final Point3d hitBoxLocalCenter;
	private final boolean rightClick;
		
	public PacketVehicleInteract(EntityVehicleF_Physics vehicle, WrapperPlayer player, BoundingBox hitBox, boolean rightClick){
		super(vehicle, player);
		APart hitPart = vehicle.getPartWithBox(hitBox);
		this.hitPartLookupID = hitPart != null ? hitPart.lookupID : -1;
		this.hitBoxLocalCenter = hitBox.localCenter;
		this.rightClick = rightClick;
	}
	
	public PacketVehicleInteract(ByteBuf buf){
		super(buf);
		this.hitPartLookupID = buf.readInt();
		this.hitBoxLocalCenter = readPoint3dFromBuffer(buf);
		this.rightClick = buf.readBoolean();
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(hitPartLookupID);
		writePoint3dToBuffer(hitBoxLocalCenter, buf);
		buf.writeBoolean(rightClick);
	}

	@Override
	public boolean handle(WrapperWorld world, EntityVehicleF_Physics vehicle, WrapperPlayer player){
		PlayerOwnerState ownerState = vehicle.getOwnerState(player);
		ItemStack heldStack = player.getHeldStack();
		AItemBase heldItem = player.getHeldItem();
		
		//Get the part we hit, if one was specified.
		APart part = hitPartLookupID != -1 ? AEntityA_Base.getEntity(world, hitPartLookupID) : null;
		
		//Next, get the bounding box.  This is either from the part or the main entity.
		BoundingBox hitBox = null;
		for(BoundingBox box : (part != null ? part.interactionBoxes : vehicle.interactionBoxes)){
			if(box.localCenter.equals(hitBoxLocalCenter)){
				hitBox = box;
				break;
			}
		}
		if(hitBox == null){
			//Try part interaction boxes, as they're not in the normal list.
			if(part == null){
				for(BoundingBox box : vehicle.allPartSlotBoxes.keySet()){
					if(box.localCenter.equals(hitBoxLocalCenter)){
						hitBox = box;
						break;
					}
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
					if(vehicle.addPartFromItem((AItemPart) heldItem, new WrapperNBT(heldStack), hitBoxLocalCenter, false) != null && !player.isCreative()){				
						player.getInventory().removeStack(heldStack, 1);
					}
				}
			}
			return false;
		}
		
		//If we clicked with with an item that can interact with a part or vehicle, perform that interaction.
		//If the item doesn't or couldn't interact with the vehicle, check for other interactions.
		if(heldItem instanceof IItemVehicleInteractable){
			switch(((IItemVehicleInteractable) heldItem).doVehicleInteraction(vehicle, part, player, ownerState, rightClick)){
				case ALL: return true;
				case PLAYER: player.sendPacket(this); return false;
				case NONE: return false;
				case SKIP: //Don't return anything, continue processing.
			}
		}
		
		//Check if we clicked a door.
		JSONDoor hitDoor = vehicle.allDoorBoxes.get(hitBox); 
		if(hitDoor != null){
			if(!hitDoor.ignoresClicks){
				//Can't open locked vehicles.
				if(vehicle.locked){
					player.sendPacket(new PacketPlayerChatMessage(player, "interact.failure.vehiclelocked"));
				}else{
					//Open or close the clicked door.
					if(vehicle.variablesOn.contains(hitDoor.name)){
						vehicle.variablesOn.remove(hitDoor.name);
					}else{
						vehicle.variablesOn.add(hitDoor.name);
					}
					return true;
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
		return false;
	}
}
