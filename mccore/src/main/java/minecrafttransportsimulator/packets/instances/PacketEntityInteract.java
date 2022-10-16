package minecrafttransportsimulator.packets.instances;

import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable.PlayerOwnerState;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemEntityInteractable;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet used to interact with entities.  Initially sent from clients to the server
 * to handle players clicking on the entity.  Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the entity if the
 * action requires updates on clients.  This can be driven by the logic in this packet, or
 * the logic in {@link IItemEntityInteractable#doEntityInteraction(AEntityE_Interactable, APart, IWrapperPlayer, PlayerOwnerState, boolean)}
 *
 * @author don_bruce
 */
public class PacketEntityInteract extends APacketEntityInteract<AEntityE_Interactable<?>, IWrapperPlayer> {
    private final Point3D hitBoxLocalCenter;
    private final boolean leftClick;
    private final boolean rightClick;

    public PacketEntityInteract(AEntityE_Interactable<?> entity, IWrapperPlayer player, BoundingBox hitBox, boolean leftClick, boolean rightClick) {
        super(entity, player);
        this.hitBoxLocalCenter = hitBox.localCenter;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
    }

    public PacketEntityInteract(ByteBuf buf) {
        super(buf);
        this.hitBoxLocalCenter = readPoint3dFromBuffer(buf);
        this.leftClick = buf.readBoolean();
        this.rightClick = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(hitBoxLocalCenter, buf);
        buf.writeBoolean(leftClick);
        buf.writeBoolean(rightClick);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, IWrapperPlayer player) {
        PlayerOwnerState ownerState = entity.getOwnerState(player);
        IWrapperItemStack heldStack = player.getHeldStack();
        AItemBase heldItem = heldStack.getItem();

        //Get the bounding box hit for future operations.
        BoundingBox hitBox = null;

        //First check part slots.
        //This takes priority as part placement should always be checked before part interaction.
        if (rightClick && entity instanceof AEntityF_Multipart) {
            AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) entity;
            for (Entry<BoundingBox, JSONPartDefinition> slotEntry : multipart.partSlotBoxes.entrySet()) {
                if (slotEntry.getKey().localCenter.equals(hitBoxLocalCenter)) {
                    //Only owners can add parts.
                    if (ownerState.equals(PlayerOwnerState.USER)) {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_OWNED));
                    } else {
                        //Attempt to add a part.  Entity is responsible for callback packet here.
                        if (heldItem instanceof AItemPart && !player.isSneaking()) {
                            if (multipart.addPartFromItem((AItemPart) heldItem, player, heldStack.getData(), multipart.definition.parts.indexOf(slotEntry.getValue())) != null && !player.isCreative()) {
                                player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
                            }
                        }
                    }
                    return false;
                }
            }
        }

        //If we didn't get the box from the part slot, get it from the main list.
        if (hitBox == null) {
            for (BoundingBox box : entity.interactionBoxes) {
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

        //If we clicked with with an item that can interact with a entity, perform that interaction.
        //If the item doesn't or couldn't interact with the entity, check for other interactions.
        if (heldItem instanceof IItemEntityInteractable && (rightClick || leftClick)) {
            switch (((IItemEntityInteractable) heldItem).doEntityInteraction(entity, hitBox, player, ownerState, rightClick)) {
                case ALL:
                    return true;
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
            //Can't touch locked entities.
            if (rightClick && entity.locked) {
                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_LOCKED));
            } else {
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

        //Not holding an item that can interact with a entity, nor clicked a box.  Try to interact with the entity itself.
        if (rightClick) {
            entity.interact(player);
        } else if (leftClick) {
            entity.attack(new Damage(1.0F, entity.boundingBox, null, player, null).setHand());
        }
        return false;
    }
}
