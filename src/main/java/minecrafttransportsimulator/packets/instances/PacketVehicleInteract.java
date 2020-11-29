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
import minecrafttransportsimulator.items.components.IItemVehicleInteractable.CallbackType;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable.PlayerOwnerState;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDoor;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.item.ItemStack;

/**Packet used to interact with vehicles.  Initially sent from clients to the server
 * to handle players clicking on the vehicle.  Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the vehicle if the
 * action requires updates on clients.  This can be driven by the logic in this packet, or
 * the logic in {@link IItemVehicleInteractable#doVehicleInteraction(ItemStack, EntityVehicleF_Physics, APart, IWrapperPlayer, PlayerOwnerState, boolean)}
 * 
 * @author don_bruce
 */
public class PacketVehicleInteract extends APacketVehicle{
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
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle){
		boolean canPlayerEditVehicle = player.isOP() || vehicle.ownerUUID.isEmpty() || player.getUUID().equals(vehicle.ownerUUID);
		PlayerOwnerState ownerState = player.isOP() ? PlayerOwnerState.ADMIN : (canPlayerEditVehicle ? PlayerOwnerState.OWNER : PlayerOwnerState.USER);
		IWrapperItemStack heldStack = player.getHeldStack();
		AItemBase heldItem = heldStack.getItem();
		APart part = vehicle.getPartAtLocation(hitPosition);
		
		//If our part is null, see if we clicked a part's collision box instead.
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
		//Otherwise, try to do part-based interaction.
		if(heldItem instanceof IItemVehicleInteractable){
			CallbackType callback = ((IItemVehicleInteractable) heldItem).doVehicleInteraction(vehicle, part, player, ownerState, rightClick);
			if(callback.equals(CallbackType.ALL)){
				return true;
			}else if(callback.equals(CallbackType.PLAYER)){
				player.sendPacket(this);
			}
		}else{
			//Not holding an item that can interact with a vehicle.  Try to interact with the vehicle itself.
			if(part != null){
				if(rightClick){
					part.interact(player);
				}else{
					part.attack(new Damage("player", 1.0F, part.boundingBox, player));
				}
			}else{
				//Check if we clicked a part slot box.
				for(BoundingBox slotBox : vehicle.activePartSlotBoxes.keySet()){
					if(slotBox.localCenter.equals(hitPosition)){
						//Only owners can add vehicle parts.
						if(!canPlayerEditVehicle){
							player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehicleowned"));
						}else{
							//Attempt to add a part.  Vehicle is responsible for callback packet here.
							if(heldItem instanceof ItemPart){
								if(vehicle.addPartFromItem((ItemPart) heldItem, heldStack.getData(), hitPosition, false) && !player.isCreative()){				
									player.getInventory().removeStack(heldStack, 1);
								}
							}
						}
						return false;
					}
				}
				
				//Check if we clicked a door.
				Map<BoundingBox, VehicleDoor> allDoors = new HashMap<BoundingBox, VehicleDoor>();
				allDoors.putAll(vehicle.vehicleDoorBoxes);
				for(Map<BoundingBox, VehicleDoor> doorMap : vehicle.partDoorBoxes.values()){
					allDoors.putAll(doorMap);
				}
				for(Entry<BoundingBox, VehicleDoor> doorEntry : allDoors.entrySet()){
					if(doorEntry.getKey().localCenter.equals(hitPosition)){
						//Can't open locked vehicles.
						if(vehicle.locked){
							player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehiclelocked"));
						}else{
							//Open or close the clicked door.
							if(vehicle.doorsOpen.contains(doorEntry.getValue().name)){
								vehicle.doorsOpen.remove(doorEntry.getValue().name);
							}else{
								vehicle.doorsOpen.add(doorEntry.getValue().name);
							}
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
