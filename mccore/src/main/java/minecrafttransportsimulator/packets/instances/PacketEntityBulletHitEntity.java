package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteractHitbox;

/**
 * Packet sent when a bullet hits an entity.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitEntity extends APacketEntityInteractHitbox {
    private final UUID gunID;
    private final int bulletNumber;
    private final double amount;

    public PacketEntityBulletHitEntity(EntityBullet bullet, AEntityE_Interactable<?> entity, BoundingBox hitBox, Damage damage) {
        super(entity, hitBox);
        this.gunID = bullet.gun.uniqueUUID;
        this.bulletNumber = bullet.bulletNumber;
        this.amount = damage.amount;
    }

    public PacketEntityBulletHitEntity(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.bulletNumber = buf.readInt();
        this.amount = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeInt(bulletNumber);
        buf.writeDouble(amount);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, BoundingBox hitBox) {
        PartGun gun = world.getEntity(gunID);
        if (gun != null) {
            EntityBullet bullet = gun.activeBullets.get(bulletNumber);
            if (bullet != null) {
                bullet.performEntityHitLogic(entity, new Damage(bullet, hitBox, amount));
                return true;
            }
        }
        return false;
    }
}
