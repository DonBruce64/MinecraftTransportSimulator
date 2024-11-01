package mcinterface1182;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import net.minecraft.core.BlockPos;

/**
 * Packet used to request NBT data for entities from the server.  Used when an entity is first created
 * on a client, as MC is too dumb to let us simply set a flag to get the NBT data from the server in an
 * easy way.  Once on the server, it will send back a {@link PacketEntityCSHandshakeServer}.
 *
 * @author don_bruce
 */
public class PacketEntityCSHandshakeClient extends APacketPlayer {
    private final String builderID;

    public PacketEntityCSHandshakeClient(IWrapperPlayer player, ABuilderEntityBase builder) {
        super(player);
        this.builderID = builder.getStringUUID();
    }

    public PacketEntityCSHandshakeClient(IWrapperPlayer player, BuilderTileEntity builder) {
        super(player);
        this.builderID = builder.getBlockPos().getX() + "," + builder.getBlockPos().getY() + "," + builder.getBlockPos().getZ();
    }

    public PacketEntityCSHandshakeClient(ByteBuf buf) {
        super(buf);
        this.builderID = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(builderID, buf);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        //Need to do null checks here as some entities may not exist due to them being unloaded during packet transfer.
        //Queue up the builder to send the player data back next update.
        if (builderID.contains(",")) {
            String[] stringPos = builderID.split(",");
            BuilderTileEntity tile = (BuilderTileEntity) ((WrapperWorld) world).world.getBlockEntity(new BlockPos(Integer.parseInt(stringPos[0]), Integer.parseInt(stringPos[1]), Integer.parseInt(stringPos[2])));
            if (tile != null) {
                tile.playersRequestingData.add(player);
            }
        } else {
            WrapperEntity entity = ((WrapperWorld) world).getExternalEntity(UUID.fromString(builderID));
            if (entity != null) {
                //Queue up the builder to send the player data back next update.
                ((ABuilderEntityBase) entity.entity).playersRequestingData.add(player);
            }
        }
    }
}
