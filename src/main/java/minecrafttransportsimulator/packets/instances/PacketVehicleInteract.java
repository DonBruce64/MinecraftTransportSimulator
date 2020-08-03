package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.core.IItemVehicleInteractable;
import minecrafttransportsimulator.items.core.IItemVehicleInteractable.CallbackType;
import minecrafttransportsimulator.items.core.IItemVehicleInteractable.PlayerOwnerState;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**Packet used to interact with vehicles.  Initially sent from clients to the server
 * to handle players clicking on the vehicle.  Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the vehicle if the
 * action requires updates on clients.  This can be driven by the logic in this packet, or
 * the logic in {@link IItemVehicleInteractable#doVehicleInteraction(ItemStack, EntityVehicleF_Physics, APart, WrapperPlayer, PlayerOwnerState, boolean)}
 * 
 * @author don_bruce
 */
public class PacketVehicleInteract extends APacketVehicle{
	private final Point3d hitPosition;
	private PacketVehicleInteractType type;
		
	public PacketVehicleInteract(EntityVehicleF_Physics vehicle, Point3d hitPosition, PacketVehicleInteractType type){
		super(vehicle);
		this.hitPosition = hitPosition;
		this.type = type;
	}
	
	public PacketVehicleInteract(ByteBuf buf){
		super(buf);
		this.hitPosition = readPoint3dFromBuffer(buf);
		this.type = PacketVehicleInteractType.values()[buf.readByte()];
	}

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(hitPosition, buf);
		buf.writeByte(type.ordinal());
	}

	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		boolean canPlayerEditVehicle = player.isOP() || vehicle.ownerUUID.isEmpty() || player.getUUID().equals(vehicle.ownerUUID);
		PlayerOwnerState ownerState = player.isOP() ? PlayerOwnerState.ADMIN : (canPlayerEditVehicle ? PlayerOwnerState.OWNER : PlayerOwnerState.USER);
		ItemStack heldStack = player.getHeldStack();
		APart part = vehicle.getPartAtLocation(hitPosition);
		
		//If we clicked with with an item that can interact with a part or vehicle, perform that interaction.
		//Otherwise, try to do part-based interaction.
		if(heldStack.getItem() instanceof IItemVehicleInteractable){
			CallbackType callback = ((IItemVehicleInteractable) heldStack.getItem()).doVehicleInteraction(heldStack, vehicle, part, player, ownerState, type.rightClick);
			if(callback.equals(CallbackType.ALL)){
				return true;
			}else if(callback.equals(CallbackType.PLAYER)){
				player.sendPacket(this);
			}
		}else if(heldStack.getItem().equals(Items.NAME_TAG) && type.rightClick){
			//Special case as this is a MC item.
			vehicle.displayText = heldStack.getDisplayName().length() > vehicle.definition.rendering.displayTextMaxLength ? heldStack.getDisplayName().substring(0, vehicle.definition.rendering.displayTextMaxLength - 1) : heldStack.getDisplayName();
			return true;
		}else{
			//Not holding an item that can interact with a vehicle.  Try to interact with parts or slots.
			if(type.equals(PacketVehicleInteractType.PART_RIGHTCLICK)){
				part.interact(player);
			}else if(type.equals(PacketVehicleInteractType.PART_LEFTCLICK)){
				part.attack(new Damage("player", 1.0F, part.boundingBox, player));
			}else if(type.equals(PacketVehicleInteractType.PART_SLOT_RIGHTCLICK)){
				//Only owners can add vehicle parts.
				if(!canPlayerEditVehicle){
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehicleowned"));
				}else{
					//Attempt to add the part.  Vehicle is responsible for callback packet here.
					if(heldStack.getItem() instanceof AItemPart){
						if(vehicle.addPartFromItem((AItemPart) heldStack.getItem(), new WrapperNBT(heldStack), hitPosition)){				
							player.removeItem(heldStack, 1);
						}
					}
				}
			}
		}
		return false;
	}
	
	public static enum PacketVehicleInteractType{
		COLLISION_RIGHTCLICK(true),
		COLLISION_LEFTCLICK(false),
		PART_RIGHTCLICK(true),
		PART_LEFTCLICK(false),
		PART_SLOT_RIGHTCLICK(true);
		
		private final boolean rightClick;
		
		private PacketVehicleInteractType(boolean rightClick){
			this.rightClick = rightClick;
		}
	}
}
