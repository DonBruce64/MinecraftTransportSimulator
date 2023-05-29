package minecrafttransportsimulator.packets.components;

import java.util.UUID;

import io.netty.buffer.ByteBuf;

/**
 * Abstract packet for bullets that saves a reference to the gun lookup and bullet number.
 *
 * @author don_bruce
 */
public abstract class APacketBullet extends APacketBase {
    protected final UUID gunID;
    protected final int bulletNumber;

    public APacketBullet(UUID gunID, int bulletNumber) {
        super(null);
        this.gunID = gunID;
        this.bulletNumber = bulletNumber;
    }

    public APacketBullet(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.bulletNumber = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeInt(bulletNumber);
    }
}
