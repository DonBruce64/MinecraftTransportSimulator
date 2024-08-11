package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteractHitbox;

/**
 * Packet sent when a bullet hits an entity collision box.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitCollision extends APacketEntityInteractHitbox {
    private final double amount;

    public PacketEntityBulletHitCollision(AEntityE_Interactable<?> entity, BoundingBox hitBox, double amount) {
        super(entity, hitBox);
        this.amount = amount;
    }

    public PacketEntityBulletHitCollision(ByteBuf buf) {
        super(buf);
        this.amount = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeDouble(amount);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, BoundingBox hitBox) {
        entity.damageCollisionBox(hitBox, amount);
        //Don't send this back to clients.  The method does this for us.
        return false;
    }
}
