package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;

/**
 * Packet used to send over bullet commands.  Bullets don't sync via NBT.  Rather, they sync via
 * their gun and the index.
 *
 * @author don_bruce
 */
public abstract class APacketEntityBullet extends APacketEntity<PartGun> {
    private final int bulletNumber;

    public APacketEntityBullet(EntityBullet bullet) {
        super(bullet.gun);
        this.bulletNumber = bullet.bulletNumber;
    }

    public APacketEntityBullet(ByteBuf buf) {
        super(buf);
        this.bulletNumber = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(bulletNumber);
    }

    @Override
    public boolean handle(AWrapperWorld world, PartGun gun) {
        EntityBullet bullet = gun.activeBullets.get(bulletNumber);
        return bullet != null && handle(world, bullet);
    }

    /**
     * Handler method for the bullet, rather than the gun.
     * Essentially overrides {@link #handle(AWrapperWorld, PartGun)}
     */
    protected abstract boolean handle(AWrapperWorld world, EntityBullet bullet);
}
