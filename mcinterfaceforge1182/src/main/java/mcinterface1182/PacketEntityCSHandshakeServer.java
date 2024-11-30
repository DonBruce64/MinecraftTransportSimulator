package mcinterface1182;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.core.BlockPos;

/**
 * Packet used to send NBT data to clients when requested for it.  Driven by the arrival of a
 * {@link PacketEntityCSHandshakeClient} on the server.  This is used for both normal and tile
 * entities, depending on the format of the string passed-in.
 *
 * @author don_bruce
 */
public class PacketEntityCSHandshakeServer extends APacketBase {
    private final String builderID;
    private final IWrapperNBT data;

    public PacketEntityCSHandshakeServer(ABuilderEntityBase builder, IWrapperNBT data) {
        super(null);
        this.builderID = builder.getStringUUID();
        this.data = data;
    }

    public PacketEntityCSHandshakeServer(BuilderTileEntity builder, IWrapperNBT data) {
        super(null);
        this.builderID = builder.getBlockPos().getX() + "," + builder.getBlockPos().getY() + "," + builder.getBlockPos().getZ();
        this.data = data;
    }

    public PacketEntityCSHandshakeServer(ByteBuf buf) {
        super(buf);
        this.builderID = readStringFromBuffer(buf);
        this.data = readDataFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeStringToBuffer(builderID, buf);
        writeDataToBuffer(data, buf);
    }

    @Override
    public void handle(AWrapperWorld world) {
        if (builderID.contains(",")) {
            String[] stringPos = builderID.split(",");
            BuilderTileEntity tile = (BuilderTileEntity) ((WrapperWorld) world).world.getBlockEntity(new BlockPos(Integer.parseInt(stringPos[0]), Integer.parseInt(stringPos[1]), Integer.parseInt(stringPos[2])));
            if (tile != null) {
                tile.lastLoadedNBT = ((WrapperNBT) data).tag;
                tile.loadFromSavedNBT = true;
            }
        } else {
            WrapperEntity mcWrapper = ((WrapperWorld) world).getExternalEntity(UUID.fromString(builderID));
            if (mcWrapper != null && mcWrapper.entity != null) {
                //Set last loaded NBT.
                ((ABuilderEntityBase) mcWrapper.entity).lastLoadedNBT = ((WrapperNBT) data).tag;
                ((ABuilderEntityBase) mcWrapper.entity).loadFromSavedNBT = true;
            }
        }
    }
}
