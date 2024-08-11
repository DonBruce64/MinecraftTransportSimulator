package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityLoader;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to loaders  on clients to change what part they are connected to.
 *
 * @author don_bruce
 */
public class PacketTileEntityLoaderConnection extends APacketEntity<ATileEntityLoader> {
    private final UUID linkedID;
    private final boolean connect;

    public PacketTileEntityLoaderConnection(ATileEntityLoader loader, boolean connect) {
        super(loader);
        if (loader.connectedPart != null) {
            this.linkedID = loader.connectedPart.uniqueUUID;
        } else {
            this.linkedID = null;
        }
        this.connect = connect;
    }

    public PacketTileEntityLoaderConnection(ByteBuf buf) {
        super(buf);
        if (buf.readBoolean()) {
            this.linkedID = readUUIDFromBuffer(buf);
        } else {
            this.linkedID = null;
        }
        this.connect = buf.readBoolean();
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
        buf.writeBoolean(connect);
    }

    @Override
    protected boolean handle(AWrapperWorld world, ATileEntityLoader loader) {
        AEntityF_Multipart<?> entity = world.getEntity(linkedID);
        if (connect) {
            if (entity != null) {
                loader.connectToPart((PartInteractable) entity);
            }
        } else {
            loader.connectToPart(null);
        }
        return true;
    }
}
