package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;

/**
 * Packet used to interact with entities, with a hitbox reference associated.
 *
 * @author don_bruce
 */
public abstract class APacketEntityInteractHitbox extends APacketEntity<AEntityE_Interactable<?>> {
    private final Point3D hitBoxLocalCenter;

    public APacketEntityInteractHitbox(AEntityE_Interactable<?> entity, BoundingBox hitBox) {
        super(entity);
        this.hitBoxLocalCenter = hitBox.localCenter;
    }

    public APacketEntityInteractHitbox(ByteBuf buf) {
        super(buf);
        this.hitBoxLocalCenter = readPoint3dFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(hitBoxLocalCenter, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity) {
        for (BoundingBox box : entity.interactionBoxes) {
            if (box.localCenter.equals(hitBoxLocalCenter)) {
                return handle(world, entity, box);
            }
        }
        //Not sure how the heck this happened, but it did.
        return false;
    }

    /**
     * Handler method for the entity, plus the box.
     * Essentially overrides {@link #handle(AWrapperWorld, AEntityE_Interactable)}
     */
    protected abstract boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, BoundingBox box);
}
