package mcinterface1122;

import java.io.IOException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfacePacket;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

class InterfacePacket implements IInterfacePacket {
    private static final SimpleNetworkWrapper network = NetworkRegistry.INSTANCE.newSimpleChannel(InterfaceLoader.MODID);
    private static final BiMap<Byte, Class<? extends APacketBase>> packetMappings = HashBiMap.create();

    /**
     * Called to init this network.  Needs to be done after networking is ready.
     * Packets should be registered at this point in this constructor.
     */
    public static void init() {
        //Register the main wrapper packet.
        network.registerMessage(WrapperHandler.class, WrapperPacket.class, 0, Side.CLIENT);
        network.registerMessage(WrapperHandler.class, WrapperPacket.class, 1, Side.SERVER);

        //Register internal packets, then external.
        byte packetIndex = 0;
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityCSHandshakeClient.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityCSHandshakeServer.class);
        APacketBase.initPackets(packetIndex);
    }

    @Override
    public void registerPacket(byte packetIndex, Class<? extends APacketBase> packetClass) {
        packetMappings.put(packetIndex, packetClass);
    }

    @Override
    public byte getPacketIndex(APacketBase packet) {
        return packetMappings.inverse().get(packet.getClass());
    }

    @Override
    public void sendToServer(APacketBase packet) {
        network.sendToServer(new WrapperPacket(packet));
    }

    @Override
    public void sendToAllClients(APacketBase packet) {
        network.sendToAll(new WrapperPacket(packet));
    }

    @Override
    public void sendToPlayer(APacketBase packet, IWrapperPlayer player) {
        network.sendTo(new WrapperPacket(packet), (EntityPlayerMP) ((WrapperPlayer) player).player);
    }

    /**
     * Gets the world this packet was sent from based on its context.
     * Used for handling packets arriving on the server.
     */
    private static AWrapperWorld getServerWorld(MessageContext ctx) {
        return WrapperWorld.getWrapperFor(ctx.getServerHandler().player.world);
    }

    @Override
    public void writeDataToBuffer(IWrapperNBT data, ByteBuf buf) {
        PacketBuffer pb = new PacketBuffer(buf);
        pb.writeCompoundTag(((WrapperNBT) data).tag);
    }

    @Override
    public WrapperNBT readDataFromBuffer(ByteBuf buf) {
        PacketBuffer pb = new PacketBuffer(buf);
        try {
            return new WrapperNBT(pb.readCompoundTag());
        } catch (IOException e) {
            // Unpossible? --- Says Forge comments, so who knows?
            throw new RuntimeException(e);
        }
    }

    /**
     * Custom class for packets.  Allows for a common packet to be used for all MC versions,
     * as well as less boilerplate code due to thread operations.  Note that when this packet
     * arrives on the other side of the pipeline, MC won't know what class to construct.
     * That's up to us to handle via the packet's first byte.  Also note that this class
     * must be public, as if it is private MC won't be able to construct it due to access violations.
     */
    public static class WrapperPacket implements IMessage {
        private APacketBase packet;

        /**
         * Do NOT call!  Required to keep Forge from crashing.
         **/
        public WrapperPacket() {
        }

        public WrapperPacket(APacketBase packet) {
            this.packet = packet;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            byte packetIndex = buf.readByte();
            try {
                Class<? extends APacketBase> packetClass = packetMappings.get(packetIndex);
                packet = packetClass.getConstructor(ByteBuf.class).newInstance(buf);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            packet.writeToBuffer(buf);
        }
    }

    /**
     * Custom class for handling packets.  This handler will have an instance of the packet
     * class passed-in with all fields populated by {@link WrapperPacket#fromBytes}.
     */
    public static class WrapperHandler implements IMessageHandler<WrapperPacket, IMessage> {
        @Override
        public IMessage onMessage(WrapperPacket message, MessageContext ctx) {
            if (message.packet.runOnMainThread()) {
                //Need to put this in a runnable to not run it on the network thread and get a CME.
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    //We need to use side-specific getters here to avoid side-specific classes from trying to be loaded
                    //by the JVM when this method is created.  Failure to do this will result in network faults.
                    //For this, we use abstract methods that are extended in our sub-classes.
                    AWrapperWorld world;
                    if (ctx.side.isServer()) {
                        world = getServerWorld(ctx);
                    } else {
                        world = InterfaceManager.clientInterface.getClientWorld();
                    }
                    if (world != null) {
                        message.packet.handle(world);
                    }
                });
            } else {
                if (ctx.side.isServer()) {
                    message.packet.handle(getServerWorld(ctx));
                } else {
                    message.packet.handle(InterfaceManager.clientInterface.getClientWorld());
                }
            }
            return null;
        }
    }

}
