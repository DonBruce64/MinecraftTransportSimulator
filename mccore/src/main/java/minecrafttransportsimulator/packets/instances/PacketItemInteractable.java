package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.guis.instances.GUIInventoryContainer;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**
 * Packet used to sync hand-held interactable crate inventory.  This is first sent by the server to the client
 * to open the crate GUI.  Once the client closes the GUI, this packet is sent back to the server to update
 * the item data with whatever changes the player made.  The server will ensure that the inventory exists before
 * sending this packet, but the client will have to create the inventory.  This is because this packet is designed
 * for inventories that are not normally created in the world.  Currently only item inventories on players fit
 * this description.
 *
 * @author don_bruce
 */
public class PacketItemInteractable extends APacketPlayer {
    private final UUID uniqueUUID;
    private final IWrapperNBT data;
    private final int units;
    private final int stackSize;
    private final String texture;
    private final boolean saveToHeldItem;

    public PacketItemInteractable(IWrapperPlayer player, EntityInventoryContainer inventory, String texture) {
        this(player, inventory, texture, true);
    }

    /**
     * Legacy-interface fallback for versions that do not expose IV inventories through a native
     * Minecraft menu.  Modern interfaces open the same inventory directly as a vanilla menu.
     */
    public PacketItemInteractable(IWrapperPlayer player, EntityInventoryContainer inventory, String texture, boolean saveToHeldItem) {
        super(player);
        this.uniqueUUID = inventory.uniqueUUID;
        this.data = inventory.save(InterfaceManager.coreInterface.getNewNBTWrapper());
        this.units = inventory.getSize();
        this.stackSize = inventory.getStackSize();
        this.texture = texture;
        this.saveToHeldItem = saveToHeldItem;
    }

    private PacketItemInteractable(IWrapperPlayer player, UUID uniqueUUID) {
        super(player);
        this.uniqueUUID = uniqueUUID;
        this.data = null;
        this.units = 0;
        this.stackSize = 0;
        this.texture = null;
        this.saveToHeldItem = true;
    }

    public PacketItemInteractable(ByteBuf buf) {
        super(buf);
        this.uniqueUUID = readUUIDFromBuffer(buf);
        if (buf.readBoolean()) {
            this.data = readDataFromBuffer(buf);
            this.units = buf.readInt();
            this.stackSize = buf.readInt();
            if (buf.readBoolean()) {
                this.texture = readStringFromBuffer(buf);
            } else {
                this.texture = null;
            }
            this.saveToHeldItem = buf.readBoolean();
        } else {
            this.data = null;
            this.units = 0;
            this.stackSize = 0;
            this.texture = null;
            this.saveToHeldItem = true;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(uniqueUUID, buf);
        if (data == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            writeDataToBuffer(data, buf);
            buf.writeInt(units);
            buf.writeInt(stackSize);
            if (texture != null) {
                buf.writeBoolean(true);
                writeStringToBuffer(texture, buf);
            } else {
                buf.writeBoolean(false);
            }
            buf.writeBoolean(saveToHeldItem);
        }
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        if (world.isClient()) {
            //Persistent world inventories normally already exist on the client.  Held crates need
            //an ad-hoc client container populated from this packet.
            AEntityA_Base existingEntity = world.getEntity(uniqueUUID);
            boolean temporaryInventory = !(existingEntity instanceof EntityInventoryContainer);
            EntityInventoryContainer inventory = temporaryInventory ? new EntityInventoryContainer(world, data, units, stackSize) : (EntityInventoryContainer) existingEntity;
            new GUIInventoryContainer(inventory, texture, saveToHeldItem) {
                @Override
                public void close() {
                    super.close();
                    if (saveToHeldItem) {
                        InterfaceManager.packetInterface.sendToServer(new PacketItemInteractable(player, uniqueUUID));
                    }
                    if (temporaryInventory) {
                        inventory.remove();
                    }
                }
            };
            if (temporaryInventory) {
                world.addEntity(inventory);
            }
        } else {
            world.getEntity(uniqueUUID).remove();
        }
    }
}
