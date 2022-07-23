package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

import java.util.UUID;

/**
 * Packet used to interact with vehicles. Initially sent from clients to the server
 * to handle players clicking on the vehicle. Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the vehicle if the
 * action requires updates on clients. This can be driven by the logic in this packet, or
 * the logic in {@link IItemVehicleInteractable#doVehicleInteraction(EntityVehicleF_Physics, APart, BoundingBox, IWrapperPlayer, PlayerOwnerState, boolean)}
 *
 * @author don_bruce
 */
public class PacketVehicleInteract extends APacketEntityInteract<EntityVehicleF_Physics, IWrapperPlayer> {
    private final UUID hitPartUniqueUUID;
    private final Point3D hitBoxLocalCenter;
    private final boolean leftClick;
    private final boolean rightClick;

    public PacketVehicleInteract(EntityVehicleF_Physics vehicle, IWrapperPlayer player, BoundingBox hitBox, boolean leftClick, boolean rightClick) {
        super(vehicle, player);
        APart hitPart = vehicle.getPartWithBox(hitBox);
        this.hitPartUniqueUUID = hitPart != null ? hitPart.uniqueUUID : null;
        this.hitBoxLocalCenter = hitBox.localCenter;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
    }

    public PacketVehicleInteract(ByteBuf buf) {
        super(buf);
        if (buf.readBoolean()) {
            this.hitPartUniqueUUID = readUUIDFromBuffer(buf);
        } else {
            this.hitPartUniqueUUID = null;
        }
        this.hitBoxLocalCenter = readPoint3dFromBuffer(buf);
        this.leftClick = buf.readBoolean();
        this.rightClick = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        if (hitPartUniqueUUID != null) {
            buf.writeBoolean(true);
            writeUUIDToBuffer(hitPartUniqueUUID, buf);
        } else {
            buf.writeBoolean(false);
        }
        writePoint3dToBuffer(hitBoxLocalCenter, buf);
        buf.writeBoolean(leftClick);
        buf.writeBoolean(rightClick);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle, IWrapperPlayer player) {
        PlayerOwnerState ownerState = vehicle.getOwnerState(player);
        IWrapperItemStack heldStack = player.getHeldStack();
        AItemBase heldItem = heldStack.getItem();

        //Get the part we hit, if one was specified.
        APart part = hitPartUniqueUUID != null ? world.getEntity(hitPartUniqueUUID) : null;

        //Get the bounding box hit for future operations.
        BoundingBox hitBox = null;

        //First check part slots. But don't do that if we hit a part, as obviously we didn't hit a slot.
        if (part == null) {
            for (BoundingBox box : vehicle.allPartSlotBoxes.keySet()) {
                if (box.localCenter.equals(hitBoxLocalCenter)) {
                    hitBox = box;
                    break;
                }
            }
        }

        //If we didn't get the box from the part slot, get it from the main list.
        if (hitBox == null) {
            for (BoundingBox box : (part != null ? part.interactionBoxes : vehicle.interactionBoxes)) {
                if (box.localCenter.equals(hitBoxLocalCenter)) {
                    hitBox = box;
                    break;
                }
            }

            if (hitBox == null) {
                //Not sure how the heck this happened, but it did.
                return false;
            }
        }

        //Check if we clicked a part slot box. This takes priority as part placement
        //should always be checked before part interaction.
        if (rightClick && vehicle.allPartSlotBoxes.containsKey(hitBox)) {
            //Only owners can add vehicle parts.
            if (ownerState.equals(PlayerOwnerState.USER)) {
                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_OWNED));
            } else {
                //Attempt to add a part. Vehicle is responsible for callback packet here.
                if (heldItem instanceof AItemPart) {
                    if (vehicle.addPartFromItem((AItemPart) heldItem, player, heldStack.getData(), hitBoxLocalCenter, false) != null && !player.isCreative()) {
                        player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
                    }
                }
            }
            return false;
        }

        //If we clicked with with an item that can interact with a part or vehicle, perform that interaction.
        //If the item doesn't or couldn't interact with the vehicle, check for other interactions.
        boolean hadAllCondition = false;
        if ((rightClick || leftClick) && heldItem instanceof IItemVehicleInteractable) {
            switch (((IItemVehicleInteractable) heldItem).doVehicleInteraction(vehicle, part, hitBox, player, ownerState, rightClick)) {
                case ALL:
                    return true;
                case ALL_AND_MORE:
                    hadAllCondition = true;
                    break;
                case PLAYER:
                    player.sendPacket(this);
                    return false;
                case NONE:
                    return false;
                case SKIP: //Don't return anything, continue processing.
            }
        }

        //Check if we clicked a box with a variable attached.
        if (!leftClick && hitBox.definition != null && hitBox.definition.variableName != null) {
            //Can't touch locked vehicles.
            if (vehicle.locked && !hadAllCondition) {
                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_LOCKED));
            } else {
                AEntityD_Definable<?> entity = part != null ? part : vehicle;
                switch (hitBox.definition.variableType) {
                    case BUTTON: {
                        if (rightClick) {
                            entity.setVariable(hitBox.definition.variableName, hitBox.definition.variableValue);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(entity, hitBox.definition.variableName, hitBox.definition.variableValue));
                        } else {
                            entity.setVariable(hitBox.definition.variableName, 0);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(entity, hitBox.definition.variableName, 0));
                        }
                        break;
                    }
                    case INCREMENT:
                        if (rightClick && entity.incrementVariable(hitBox.definition.variableName, hitBox.definition.variableValue, hitBox.definition.clampMin, hitBox.definition.clampMax)) {
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(entity, hitBox.definition.variableName, hitBox.definition.variableValue, hitBox.definition.clampMin, hitBox.definition.clampMax));
                        }
                        break;
                    case SET:
                        if (rightClick) {
                            entity.setVariable(hitBox.definition.variableName, hitBox.definition.variableValue);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(entity, hitBox.definition.variableName, hitBox.definition.variableValue));
                        }
                        break;
                    case TOGGLE: {
                        if (rightClick) {
                            entity.toggleVariable(hitBox.definition.variableName);
                            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(entity, hitBox.definition.variableName));
                        }
                        break;
                    }
                }

            }
            return false;
        }

        //Not holding an item that can interact with a vehicle. Try to interact with the vehicle itself.
        if (part != null) {
            if (rightClick) {
                part.interact(player);
            } else if (leftClick) {
                part.attack(new Damage(1.0F, part.boundingBox, null, player, null).setHand());
            }
        }
        return hadAllCondition;
    }
}
