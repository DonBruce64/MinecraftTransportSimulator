package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet used to add/remove riders from an entity.  This packet only appears on clients after the
 * server has added or removed a rider from the entity.  If a position is given, then this rider should
 * be added to that position.  If no position is given, then the rider should be removed.
 *
 * @author don_bruce
 */
public class PacketEntityRiderChange extends APacketEntityInteract<AEntityB_Existing, IWrapperEntity> {
    private final boolean mount;
    private final boolean facesForwards;

    /**
     * Constructor for setting the rider.
     **/
    public PacketEntityRiderChange(AEntityB_Existing entity, IWrapperEntity rider, boolean facesForwards) {
        super(entity, rider);
        this.mount = true;
        this.facesForwards = facesForwards;
    }

    /**
     * Constructor for removing the rider.
     **/
    public PacketEntityRiderChange(AEntityB_Existing entity, IWrapperEntity rider) {
        super(entity, rider);
        this.mount = false;
        this.facesForwards = false;
    }

    public PacketEntityRiderChange(ByteBuf buf) {
        super(buf);
        this.mount = buf.readBoolean();
        this.facesForwards = buf.readBoolean();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeBoolean(mount);
        buf.writeBoolean(facesForwards);
    }

    @Override
    protected boolean handle(AWrapperWorld world, AEntityB_Existing entity, IWrapperEntity rider) {
        if (mount) {
            entity.setRider(rider, facesForwards);
        } else {
            entity.removeRider();
        }
        return true;
    }
}
