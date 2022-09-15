package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet sent to roads when their collision data changes.  This can either be the blocks that make up the
 * road's collision, blocks blocking the collision, or the active state in general.  This is sent from servers
 * to all clients when collision checks are requested for roads.
 *
 * @author don_bruce
 */
public class PacketTileEntityRoadCollisionUpdate extends APacketEntity<TileEntityRoad> {
    private final List<Point3D> collisionBlockOffsets;
    private final List<Point3D> collidingBlockOffsets;

    public PacketTileEntityRoadCollisionUpdate(TileEntityRoad road) {
        super(road);
        collisionBlockOffsets = road.collisionBlockOffsets;
        collidingBlockOffsets = road.collidingBlockOffsets;
    }

    public PacketTileEntityRoadCollisionUpdate(ByteBuf buf) {
        super(buf);
        this.collisionBlockOffsets = new ArrayList<>();
        int collisionBlockOffsetCount = buf.readInt();
        for (int i = 0; i < collisionBlockOffsetCount; ++i) {
            collisionBlockOffsets.add(readPoint3dCompactFromBuffer(buf));
        }

        this.collidingBlockOffsets = new ArrayList<>();
        int collidingBlockOffsetCount = buf.readInt();
        for (int i = 0; i < collidingBlockOffsetCount; ++i) {
            collidingBlockOffsets.add(readPoint3dCompactFromBuffer(buf));
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(collisionBlockOffsets.size());
        for (Point3D point : collisionBlockOffsets) {
            writePoint3dCompactToBuffer(point, buf);
        }

        buf.writeInt(collidingBlockOffsets.size());
        for (Point3D point : collidingBlockOffsets) {
            writePoint3dCompactToBuffer(point, buf);
        }
    }

    @Override
    protected boolean handle(AWrapperWorld world, TileEntityRoad road) {
        road.collisionBlockOffsets.clear();
        road.collisionBlockOffsets.addAll(collisionBlockOffsets);
        road.collidingBlockOffsets.clear();
        road.collidingBlockOffsets.addAll(collidingBlockOffsets);
        road.blockingBoundingBoxes.clear();
        if (!road.isActive() && road.collidingBlockOffsets.isEmpty()) {
            road.setActive(true);
        }
        return false;
    }
}
