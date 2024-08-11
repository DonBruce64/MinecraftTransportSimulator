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
    private final double amount;

    public PacketEntityBulletHitEntity(PartGun gun, AEntityE_Interactable<?> entity, Damage damage) {
        super(entity, damage.box);
        this.gunID = gun.uniqueUUID;
        this.amount = damage.amount;
    }

    public PacketEntityBulletHitEntity(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.amount = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeDouble(amount);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, BoundingBox hitBox) {
        EntityBullet.performEntityHitLogic(entity, new Damage(world.getBulletGun(gunID), hitBox, amount));
        //Don't send this back to clients.  The damage method in the call does this for us.
        return false;
    }
}
