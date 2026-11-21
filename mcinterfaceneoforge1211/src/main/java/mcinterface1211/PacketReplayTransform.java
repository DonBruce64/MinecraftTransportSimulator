package mcinterface1211;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

import java.util.UUID;

public class PacketReplayTransform extends APacketBase {
    private final UUID entityUUID;
    private final Point3D position;
    private final Point3D angles;
    private final double velocity;
    private final Point3D motion;

    public PacketReplayTransform(UUID entityUUID, Point3D position, Point3D angles, double velocity, Point3D motion) {
        super(null);
        this.entityUUID = entityUUID;
        this.position = position;
        this.angles = angles;
        this.velocity = velocity;
        this.motion = motion;
    }

    public PacketReplayTransform(ByteBuf buf) {
        super(buf);
        this.entityUUID = readUUIDFromBuffer(buf);
        this.position = readPoint3dFromBuffer(buf);
        this.angles = readPoint3dFromBuffer(buf);
        this.velocity = buf.readDouble();
        this.motion = buf.readableBytes() >= 24 ? readPoint3dFromBuffer(buf) : null;
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(entityUUID, buf);
        writePoint3dToBuffer(position, buf);
        writePoint3dToBuffer(angles, buf);
        buf.writeDouble(velocity);
        writePoint3dToBuffer(motion, buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        if (!FlashbackCompat.isReplayClient()) {
            return;
        }
        FlashbackCompat.TRANSFORMS.put(entityUUID, new Point3D[]{
                position,
                angles,
                new Point3D(velocity, 0, 0)
        });
        AEntityB_Existing entity = world.getEntity(entityUUID);
        if (entity instanceof EntityVehicleF_Physics) {
            ((EntityVehicleF_Physics) entity).setReplayTransform(position, angles, velocity, motion, FlashbackCompat.isFrozen());
        }
    }
}
