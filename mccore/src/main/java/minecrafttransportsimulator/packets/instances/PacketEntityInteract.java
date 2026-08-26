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
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

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
    private static final double PART_INTERACTION_REACH = 3.5D;
    private static final double PART_INTERACTION_REACH_SQUARED = PART_INTERACTION_REACH * PART_INTERACTION_REACH;
    private static final long MIN_PENDING_PART_ACTION_TIMEOUT_MILLIS = 30000L;
    private static final long PENDING_PART_ACTION_TIMEOUT_PER_TICK_MILLIS = 250L;
    private static final Map<UUID, PendingPartInstallation> pendingPartInstallations = new HashMap<>();
    private static final Map<UUID, PendingPartRemoval> pendingPartRemovals = new HashMap<>();
    private final Point3D hitBoxLocalCenter;
    private final boolean leftClick;
    private final boolean rightClick;
    private final boolean partOperationComplete;

    public PacketEntityInteract(AEntityE_Interactable<?> entity, IWrapperPlayer player, BoundingBox hitBox, boolean leftClick, boolean rightClick) {
        this(entity, player, hitBox, leftClick, rightClick, false);
    }

    public PacketEntityInteract(AEntityE_Interactable<?> entity, IWrapperPlayer player, BoundingBox hitBox, boolean leftClick, boolean rightClick, boolean partOperationComplete) {
        super(entity, player);
        this.hitBoxLocalCenter = hitBox.localCenter;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
        this.partOperationComplete = partOperationComplete;
    }

    public PacketEntityInteract(ByteBuf buf) {
        super(buf);
        this.hitBoxLocalCenter = readPoint3dFromBuffer(buf);
        this.leftClick = buf.readBoolean();
        this.rightClick = buf.readBoolean();
        this.partOperationComplete = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(hitBoxLocalCenter, buf);
        buf.writeBoolean(leftClick);
        buf.writeBoolean(rightClick);
        buf.writeBoolean(partOperationComplete);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, IWrapperPlayer player) {
        removeExpiredPartActions(System.currentTimeMillis());
        EntityVehicleF_Physics vehicle = entity instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) entity : (entity instanceof APart ? ((APart) entity).vehicleOn : null);
        IWrapperItemStack heldStack = player.getHeldStack();
        AItemBase heldItem = heldStack.getItem();

        if (!leftClick && !rightClick && partOperationComplete) {
            pendingPartInstallations.remove(player.getID());
            pendingPartRemovals.remove(player.getID());
            return false;
        }

        if (leftClick
                && !rightClick
                && !player.isSneaking()
                && entity instanceof APart
                && ((APart) entity).definition.generic.removeTime > 0
                && (player.isHoldingItemType(ItemComponentType.WRENCH) || player.isHoldingItemType(ItemComponentType.SCREWDRIVER))) {
            pendingPartInstallations.remove(player.getID());
            APart part = (APart) entity;
            BoundingBox removalBox = getPartRemovalBox(part, hitBoxLocalCenter);
            if (removalBox == null || !isWithinPartInteractionReach(player, removalBox)) {
                pendingPartRemovals.remove(player.getID());
                return false;
            }

            if (partOperationComplete) {
                PendingPartRemoval pendingRemoval = pendingPartRemovals.get(player.getID());
                if (pendingRemoval != null
                        && pendingRemoval.partID.equals(part.uniqueUUID)
                        && pendingRemoval.removeTime == part.definition.generic.removeTime
                        && heldStack.isCompleteMatch(pendingRemoval.toolStack)
                        && isPartRemovalValid(part, vehicle, player)) {
                    if (part.ticksExisted - pendingRemoval.startTick + 1 >= pendingRemoval.removeTime) {
                        pendingPartRemovals.remove(player.getID());
                        part.entityOn.world.spawnItemStack(part.getStack(), part.position, null);
                        part.entityOn.removePart(part, true, true);
                    }
                } else {
                    pendingPartRemovals.remove(player.getID());
                }
            } else {
                pendingPartRemovals.remove(player.getID());
                if (isPartRemovalValid(part, vehicle, player)) {
                    pendingPartRemovals.put(player.getID(), new PendingPartRemoval(part, heldStack.copy(), part.ticksExisted, part.definition.generic.removeTime));
                } else if (vehicle != null && vehicle.lockedVar.isActive) {
                    player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
                } else {
                    LanguageEntry removalResult = part.checkForRemoval(player);
                    if (removalResult != null) {
                        player.sendPacket(new PacketPlayerChatMessage(player, removalResult));
                    }
                }
            }
            return false;
        }

        if (partOperationComplete && leftClick) {
            pendingPartRemovals.remove(player.getID());
            return false;
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
                    if (partOperationComplete) {
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
                        pendingPartRemovals.remove(player.getID());
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

        if (partOperationComplete) {
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

    private static boolean isPartRemovalValid(APart part, EntityVehicleF_Physics vehicle, IWrapperPlayer player) {
        return part.definition.generic.removeTime > 0
                && part.isValid
                && !part.isFake()
                && !part.isPermanent
                && part.canBeClicked()
                && !player.isSneaking()
                && (vehicle == null || !vehicle.lockedVar.isActive)
                && (player.isHoldingItemType(ItemComponentType.WRENCH) || player.isHoldingItemType(ItemComponentType.SCREWDRIVER))
                && part.checkForRemoval(player) == null;
    }

    private static BoundingBox getPartRemovalBox(APart part, Point3D hitBoxLocalCenter) {
        for (BoundingBox box : part.collisionBoxes) {
            if (box.localCenter.equals(hitBoxLocalCenter)) {
                return box;
            }
        }
        return null;
    }

    private static boolean isWithinPartInteractionReach(IWrapperPlayer player, BoundingBox box) {
        Point3D eyePosition = player.getEyePosition();
        double xDistance = Math.max(Math.abs(eyePosition.x - box.globalCenter.x) - box.widthRadius, 0.0D);
        double yDistance = Math.max(Math.abs(eyePosition.y - box.globalCenter.y) - box.heightRadius, 0.0D);
        double zDistance = Math.max(Math.abs(eyePosition.z - box.globalCenter.z) - box.depthRadius, 0.0D);
        return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance <= PART_INTERACTION_REACH_SQUARED;
    }

    private static void removeExpiredPartActions(long currentTimeMillis) {
        pendingPartInstallations.values().removeIf(pendingInstallation -> pendingInstallation.expirationTimeMillis <= currentTimeMillis);
        pendingPartRemovals.values().removeIf(pendingRemoval -> pendingRemoval.expirationTimeMillis <= currentTimeMillis);
    }

    private static long getPartActionExpirationTime(int actionTime) {
        return System.currentTimeMillis() + Math.max(MIN_PENDING_PART_ACTION_TIMEOUT_MILLIS, actionTime * PENDING_PART_ACTION_TIMEOUT_PER_TICK_MILLIS);
    }

    private static class PendingPartInstallation {
        private final UUID multipartID;
        private final int slotIndex;
        private final IWrapperItemStack stack;
        private final long startTick;
        private final int installTime;
        private final long expirationTimeMillis;

        private PendingPartInstallation(AEntityF_Multipart<?> multipart, int slotIndex, IWrapperItemStack stack, long startTick, int installTime) {
            this.multipartID = multipart.uniqueUUID;
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.startTick = startTick;
            this.installTime = installTime;
            this.expirationTimeMillis = getPartActionExpirationTime(installTime);
        }
    }

    private static class PendingPartRemoval {
        private final UUID partID;
        private final IWrapperItemStack toolStack;
        private final long startTick;
        private final int removeTime;
        private final long expirationTimeMillis;

        private PendingPartRemoval(APart part, IWrapperItemStack toolStack, long startTick, int removeTime) {
            this.partID = part.uniqueUUID;
            this.toolStack = toolStack;
            this.startTick = startTick;
            this.removeTime = removeTime;
            this.expirationTimeMillis = getPartActionExpirationTime(removeTime);
        }
    }
}
