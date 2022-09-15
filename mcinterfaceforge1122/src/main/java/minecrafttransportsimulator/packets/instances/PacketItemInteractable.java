package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
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
    private final String texture;

    public PacketItemInteractable(IWrapperPlayer player, EntityInventoryContainer inventory, String texture) {
        super(player);
        this.uniqueUUID = inventory.uniqueUUID;
        this.data = inventory.save(InterfaceManager.coreInterface.getNewNBTWrapper());
        this.units = inventory.getSize();
        this.texture = texture;
    }

    private PacketItemInteractable(IWrapperPlayer player, UUID uniqueUUID) {
        super(player);
        this.uniqueUUID = uniqueUUID;
        this.data = null;
        this.units = 0;
        this.texture = null;
    }

    public PacketItemInteractable(ByteBuf buf) {
        super(buf);
        this.uniqueUUID = readUUIDFromBuffer(buf);
        if (buf.readBoolean()) {
            this.data = readDataFromBuffer(buf);
            this.units = buf.readInt();
            if (buf.readBoolean()) {
                this.texture = readStringFromBuffer(buf);
            } else {
                this.texture = null;
            }
        } else {
            this.data = null;
            this.units = 0;
            this.texture = null;
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
            if (texture != null) {
                buf.writeBoolean(true);
                writeStringToBuffer(texture, buf);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        if (world.isClient()) {
            //Create new inventory container ad-hoc to match server's data.
            //We then delete this container, and the one on the server, when the GUI is closed.
            EntityInventoryContainer inventory = new EntityInventoryContainer(world, data, units);
            new GUIInventoryContainer(inventory, texture, true) {
                @Override
                public void close() {
                    super.close();
                    InterfaceManager.packetInterface.sendToServer(new PacketItemInteractable(player, uniqueUUID));
                    inventory.remove();
                }
            };
            world.addEntity(inventory);
        } else {
            world.getEntity(uniqueUUID).remove();
        }
    }
}
