package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.guis.instances.GUIFurnace;
import minecrafttransportsimulator.guis.instances.GUIInventoryContainer;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet used to send signals to interactable parts.  This is either used used to link the interactable with
 * a vehicle or part tank for fluid-pumping operations, or trigger a GUI to appear on the interactable.
 * Sent to servers by the fuel hose item when it does linking in the first case, and when a player clicks the
 * interactable in the second.
 * in the third.
 *
 * @author don_bruce
 */
public class PacketPartInteractable extends APacketEntityInteract<PartInteractable, IWrapperPlayer> {
    private final UUID linkedID;

    public PacketPartInteractable(PartInteractable interactable, IWrapperPlayer player) {
        super(interactable, player);
        if (interactable.linkedVehicle != null) {
            this.linkedID = interactable.linkedVehicle.uniqueUUID;
        } else if (interactable.linkedPart != null) {
            this.linkedID = interactable.linkedPart.uniqueUUID;
        } else {
            this.linkedID = null;
        }
    }

    public PacketPartInteractable(ByteBuf buf) {
        super(buf);
        this.linkedID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        if (linkedID != null) {
            buf.writeBoolean(true);
            writeUUIDToBuffer(linkedID, buf);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartInteractable interactable, IWrapperPlayer player) {
        if (linkedID != null) {
            AEntityA_Base linkedEntity = world.getEntity(linkedID);
            if (linkedEntity != null) {
                if (linkedEntity instanceof EntityVehicleF_Physics) {
                    interactable.linkedVehicle = (EntityVehicleF_Physics) linkedEntity;
                } else {
                    interactable.linkedPart = (PartInteractable) linkedEntity;
                }
            }
        } else {
            switch (interactable.definition.interactable.interactionType) {
                case CRAFTING_BENCH: {
                    new GUIPartBench(interactable.definition.interactable.crafting);
                    break;
                }
                case CRATE: {
                    new GUIInventoryContainer(interactable.inventory, interactable.definition.interactable.inventoryTexture, false);
                    break;
                }
                case FURNACE: {
                    new GUIFurnace(interactable.furnace, interactable.definition.interactable.inventoryTexture);
                    break;
                }
                default:
                    return false;
            }
            InterfaceManager.packetInterface.sendToServer(new PacketPartInteractableInteract(interactable, player, true));
        }
        return true;
    }
}
