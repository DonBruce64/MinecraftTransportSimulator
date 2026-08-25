package minecrafttransportsimulator.packets.instances;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.IItemEntityInteractable;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * Packet used to interact with entities.  Initially sent from clients to the server
 * to handle players clicking on the entity.  Actions (if any) are performed on the server.
 * A corresponding interaction packet may be sent to all players tracking the entity if the
 * action requires updates on clients.  This can be driven by the logic in this packet, or
 * the logic in {@link IItemEntityInteractable#doEntityInteraction(AEntityE_Interactable, APart, IWrapperPlayer, boolean)}
 *
 * @author don_bruce
 */
public class PacketEntityInteract extends APacketEntityInteract<AEntityE_Interactable<?>, IWrapperPlayer> {
    private static final Map<UUID, PendingPartInstallation> pendingPartInstallations = new HashMap<>();
    private final Point3D hitBoxLocalCenter;
    private final boolean leftClick;
    private final boolean rightClick;
    private final boolean partInstallComplete;

    public PacketEntityInteract(AEntityE_Interactable<?> entity, IWrapperPlayer player, BoundingBox hitBox, boolean leftClick, boolean rightClick) {
        this(entity, player, hitBox, leftClick, rightClick, false);
    }

    public PacketEntityInteract(AEntityE_Interactable<?> entity, IWrapperPlayer player, BoundingBox hitBox, boolean leftClick, boolean rightClick, boolean partInstallComplete) {
        super(entity, player);
        this.hitBoxLocalCenter = hitBox.localCenter;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
        this.partInstallComplete = partInstallComplete;
    }

    public PacketEntityInteract(ByteBuf buf) {
        super(buf);
        this.hitBoxLocalCenter = readPoint3dFromBuffer(buf);
        this.leftClick = buf.readBoolean();
        this.rightClick = buf.readBoolean();
        this.partInstallComplete = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(hitBoxLocalCenter, buf);
        buf.writeBoolean(leftClick);
        buf.writeBoolean(rightClick);
        buf.writeBoolean(partInstallComplete);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, IWrapperPlayer player) {
        EntityVehicleF_Physics vehicle = entity instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) entity : (entity instanceof APart ? ((APart) entity).vehicleOn : null);
        IWrapperItemStack heldStack = player.getHeldStack();
        AItemBase heldItem = heldStack.getItem();

        if (!leftClick && !rightClick) {
            pendingPartInstallations.remove(player.getID());
        }

        //Get the bounding box hit for future operations.
        BoundingBox hitBox = null;

        //First check part slots.
        //This takes priority as part placement should always be checked before part interaction.
        if (rightClick && entity instanceof AEntityF_Multipart) {
            AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) entity;
            for (Entry<BoundingBox, JSONPartDefinition> slotEntry : multipart.partSlotBoxes.entrySet()) {
                if (slotEntry.getKey().localCenter.equals(hitBoxLocalCenter)) {
                    //Only owners can add parts.
                    int slotIndex = multipart.definition.parts.indexOf(slotEntry.getValue());
                    if (partInstallComplete) {
                        PendingPartInstallation pendingInstallation = pendingPartInstallations.get(player.getID());
                        if (pendingInstallation != null && pendingInstallation.multipartID.equals(multipart.uniqueUUID) && pendingInstallation.slotIndex == slotIndex && heldItem instanceof AItemPart && heldStack.isCompleteMatch(pendingInstallation.stack) && isPartInstallationValid(multipart, vehicle, player, (AItemPart) heldItem, slotEntry.getValue(), slotIndex)) {
                            if (multipart.ticksExisted - pendingInstallation.startTick + 1 >= pendingInstallation.installTime) {
                                pendingPartInstallations.remove(player.getID());
                                if (multipart.addPartFromStack(heldStack, player, slotIndex, false, false) != null && !player.isCreative()) {
                                    player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
                                }
                            }
                        } else {
                            pendingPartInstallations.remove(player.getID());
                        }
                    } else if (heldItem instanceof AItemPart && ((AItemPart) heldItem).definition.generic.installTime > 0) {
                        pendingPartInstallations.remove(player.getID());
                        if (isPartInstallationValid(multipart, vehicle, player, (AItemPart) heldItem, slotEntry.getValue(), slotIndex)) {
                            pendingPartInstallations.put(player.getID(), new PendingPartInstallation(multipart, slotIndex, heldStack.copy(), multipart.ticksExisted, ((AItemPart) heldItem).definition.generic.installTime));
                        } else if (vehicle != null && vehicle.lockedVar.isActive) {
                            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
                        }
                    } else if (vehicle == null || !vehicle.lockedVar.isActive) {
                        //Attempt to add a part.  Entity is responsible for callback packet here.
                        if (heldItem instanceof AItemPart && !player.isSneaking()) {
                            if (multipart.addPartFromStack(heldStack, player, slotIndex, false, false) != null && !player.isCreative()) {
                                player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
                            }
                        }
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
                    }
                    return false;
                }
            }
        }

        if (partInstallComplete) {
            pendingPartInstallations.remove(player.getID());
            return false;
        }

        //If we didn't get the box from the part slot, get it from the main list.
        if (hitBox == null) {
            for (BoundingBox box : entity.collisionBoxes) {
                if (box.localCenter.equals(hitBoxLocalCenter)) {
                    hitBox = box;
                    break;
                }
            }

            if (hitBox == null) {
                //Flag error if we clicked something that no longer exists.
                //If this is an interact-release packet, don't worry about the error.
                if (rightClick || leftClick) {
                    InterfaceManager.coreInterface.logError("Got a packet for interacting with an entity, but don't have a hitbox for it, so we can't interact?  Interacting with: " + entity.toString());
                }
                return false;
            }
        }

        //If we clicked with with an item that can interact with a entity, perform that interaction.
        //If the item doesn't or couldn't interact with the entity, check for other interactions.
        if (heldItem instanceof IItemEntityInteractable && (rightClick || leftClick)) {
            switch (((IItemEntityInteractable) heldItem).doEntityInteraction(entity, hitBox, player, rightClick)) {
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

        //Check if we clicked a box with am action attached.
        if (!leftClick && hitBox.definition != null && hitBox.definition.action != null) {
            if (vehicle != null && vehicle.lockedVar.isActive) {
                //Can't touch locked vehicles.
                if (rightClick) {
                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
                }
            } else {
                if (hitBox.definition.action != null) {
                    entity.performAction(hitBox.definition.action, rightClick);
                }
            }
            return false;
        }

        //Not holding an item that can interact with a entity, nor right-clicked a box.  Try to interact with the entity itself.
        if (rightClick) {
            entity.interact(player);
        } else if (leftClick && (hitBox.groupDef == null || hitBox.groupDef.armorThickness == 0)) {
            entity.attack(new Damage(1.0F, entity.boundingBox, null, player, null).setHand());
        }
        return false;
    }

    private static boolean isPartInstallationValid(AEntityF_Multipart<?> multipart, EntityVehicleF_Physics vehicle, IWrapperPlayer player, AItemPart heldPart, JSONPartDefinition slotDefinition, int slotIndex) {
        return slotIndex >= 0 && slotIndex < multipart.partsInSlots.size() && multipart.partsInSlots.get(slotIndex) == null && !player.isSneaking() && (vehicle == null || !vehicle.lockedVar.isActive) && multipart.isVariableListTrue(slotDefinition.interactableVariables) && heldPart.isPartValidForPackDef(slotDefinition, multipart.subDefinition, !slotDefinition.bypassSlotMinMax);
    }

    private static class PendingPartInstallation {
        private final UUID multipartID;
        private final int slotIndex;
        private final IWrapperItemStack stack;
        private final long startTick;
        private final int installTime;

        private PendingPartInstallation(AEntityF_Multipart<?> multipart, int slotIndex, IWrapperItemStack stack, long startTick, int installTime) {
            this.multipartID = multipart.uniqueUUID;
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.startTick = startTick;
            this.installTime = installTime;
        }
    }
}
