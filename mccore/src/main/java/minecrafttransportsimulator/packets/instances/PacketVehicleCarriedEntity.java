package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

public class PacketVehicleCarriedEntity extends APacketEntityInteract<EntityVehicleF_Physics, IWrapperEntity> {

    private static final double MAX_DISTANCE = 4.0;
    private final Point3D localPostion;
    private final boolean onGround;

    public PacketVehicleCarriedEntity(EntityVehicleF_Physics entity, IWrapperEntity interactor, Point3D localPostion, boolean onGround) {
        super(entity, interactor);
        this.localPostion = localPostion;
        this.onGround = onGround;
    }

    public PacketVehicleCarriedEntity(ByteBuf buf) {
        super(buf);
        this.localPostion = readPoint3dFromBuffer(buf);
        this.onGround = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dToBuffer(localPostion, buf);
        buf.writeBoolean(onGround);
    }

    @Override
    protected boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle, IWrapperEntity entity) {
        if (world.isClient()) {
            if (entity != InterfaceManager.clientInterface.getClientPlayer()) {
                vehicle.entityCarrier.setRemotePosition(entity, localPostion, onGround);
            }
            return false;
        } else {
            double maxDistance = Math.max(
                    Math.max(vehicle.encompassingBox.widthRadius, vehicle.encompassingBox.heightRadius),
                    vehicle.encompassingBox.depthRadius
            ) + MAX_DISTANCE;
            return localPostion.length() <= maxDistance
                    && entity.getPosition().isDistanceToCloserThan(vehicle.position, maxDistance);
        }
    }
}
