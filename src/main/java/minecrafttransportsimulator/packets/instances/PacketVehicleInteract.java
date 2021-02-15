package minecrafttransportsimulator.packets.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable.PlayerOwnerState;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONDoor;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.item.ItemStack;

/**Packet used to interact with vehicles.  Initially sent from clients to the server
 * to handle players clicking on the vehicle.  Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the vehicle if the
 * action requires updates on clients.  This can be driven by the logic in this packet, or
 * the logic in {@link IItemVehicleInteractable#doVehicleInteraction(ItemStack, EntityVehicleF_Physics, APart, WrapperPlayer, PlayerOwnerState, boolean)}
 * 
 * @author don_bruce
 */
public class PacketVehicleInteract extends APacketEntity<EntityVehicleF_Physics>{
	private final Point3d hitPosition;
	private boolean rightClick;
		
	public PacketVehicleInteract(EntityVehicleF_Physics vehicle, Point3d hitPosition, boolean rightClick){
		super(vehicle);
		this.hitPosition = hitPosition;
		this.rightClick = rightClick;
	}
	
	public PacketVehicleInteract(ByteBuf buf){
		super(buf);
		this.hitPosition = readPoint3dFromBuffer(buf);
		this.rightClick = buf.readBoolean();
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(hitPosition, buf);
		buf.writeBoolean(rightClick);
	}

	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		boolean canPlayerEditVehicle = player.isOP() || vehicle.ownerUUID.isEmpty() || player.getUUID().equals(vehicle.ownerUUID);
		PlayerOwnerState ownerState = player.isOP() ? PlayerOwnerState.ADMIN : (canPlayerEditVehicle ? PlayerOwnerState.OWNER : PlayerOwnerState.USER);
		ItemStack heldStack = player.getHeldStack();
		AItemBase heldItem = player.getHeldItem();
		
		//Check if we clicked a part slot box.  This takes priority as part placement
		//should always be checked before part interaction.
		for(BoundingBox slotBox : vehicle.partSlotBoxes.keySet()){
			if(slotBox.localCenter.equals(hitPosition)){
				//Only owners can add vehicle parts.
				if(!canPlayerEditVehicle){
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehicleowned"));
				}else{
					//Attempt to add a part.  Vehicle is responsible for callback packet here.
					if(heldItem instanceof ItemPart){
						if(vehicle.addPartFromItem((ItemPart) heldItem, new WrapperNBT(heldStack), hitPosition, false) && !player.isCreative()){				
							player.getInventory().removeStack(heldStack, 1);
						}
					}
				}
				return false;
			}
		}
		
		//Didn't click a slot.  Check to see if we clicked a part directly.
		//If our part is null, see if we clicked a part's collision box instead.
		APart part = vehicle.getPartAtLocation(hitPosition);
		if(part == null){
			for(Entry<APart, List<BoundingBox>> partCollisionEntry : vehicle.partCollisionBoxes.entrySet()){
				for(BoundingBox box : partCollisionEntry.getValue()){
					if(box.localCenter.equals(hitPosition)){
						part = partCollisionEntry.getKey();
						break;
					}
				}
				if(part != null){
					break;
				}
			}
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
		
		//Not holding an item that can interact with a vehicle.  Try to interact with the vehicle itself.
		if(part != null){
			if(rightClick){
				part.interact(player);
			}else{
				part.attack(new Damage("player", 1.0F, part.boundingBox, null, player));
			}
		}else{
			//Check if we clicked a door.
			Map<BoundingBox, JSONDoor> allDoors = new HashMap<BoundingBox, JSONDoor>();
			allDoors.putAll(vehicle.vehicleDoorBoxes);
			for(Map<BoundingBox, JSONDoor> doorMap : vehicle.partDoorBoxes.values()){
				allDoors.putAll(doorMap);
			}
			for(Entry<BoundingBox, JSONDoor> doorEntry : allDoors.entrySet()){
				if(doorEntry.getKey().localCenter.equals(hitPosition)){
					if(!doorEntry.getValue().ignoresClicks){
						//Can't open locked vehicles.
						if(vehicle.locked){
							player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehiclelocked"));
						}else{
							//Open or close the clicked door.
							if(vehicle.variablesOn.contains(doorEntry.getValue().name)){
								vehicle.variablesOn.remove(doorEntry.getValue().name);
							}else{
								vehicle.variablesOn.add(doorEntry.getValue().name);
							}
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}
}
