package mcinterface1182;

import java.util.function.Supplier;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfacePacket;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

class InterfacePacket implements IInterfacePacket {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel network = NetworkRegistry.newSimpleChannel(new ResourceLocation(InterfaceLoader.MODID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static final BiMap<Byte, Class<? extends APacketBase>> packetMappings = HashBiMap.create();

    /**
     * Called to init this network.  Needs to be done after networking is ready.
     * Packets should be registered at this point in this constructor.
     */
    public static void init() {
        //Register the main wrapper packet.
        network.registerMessage(0, WrapperPacket.class, WrapperPacket::toBytes, WrapperPacket::fromBytes, WrapperPacket::handle);

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
        network.send(PacketDistributor.ALL.noArg(), new WrapperPacket(packet));
    }

    @Override
    public void sendToPlayer(APacketBase packet, IWrapperPlayer player) {
        network.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) ((WrapperPlayer) player).player), new WrapperPacket(packet));
    }

    /**
     * Gets the world this packet was sent from based on its context.
     * Used for handling packets arriving on the server.
     */
    private static AWrapperWorld getServerWorld(Supplier<NetworkEvent.Context> ctx) {
        return WrapperWorld.getWrapperFor(ctx.get().getSender().level);
    }

    @Override
    public void writeDataToBuffer(IWrapperNBT data, ByteBuf buf) {
        //We know this will be a PacketBuffer, so we can cast rather than wrap.
        ((FriendlyByteBuf) buf).writeNbt(((WrapperNBT) data).tag);
    }

    @Override
    public WrapperNBT readDataFromBuffer(ByteBuf buf) {
        return new WrapperNBT(((FriendlyByteBuf) buf).readNbt());
    }

    /**
     * Custom class for packets.  Allows for a common packet to be used for all MC versions,
     * as well as less boilerplate code due to thread operations.  Note that when this packet
     * arrives on the other side of the pipeline, MC won't know what class to construct.
     * That's up to us to handle via the packet's first byte.  Also note that this class
     * must be public, as if it is private MC won't be able to construct it due to access violations.
     */
    public static class WrapperPacket {
        private APacketBase packet;

        /**
         * Do NOT call!  Required to keep Forge from crashing.
         **/
        public WrapperPacket() {
        }

        public WrapperPacket(APacketBase packet) {
            this.packet = packet;
        }

        public static WrapperPacket fromBytes(FriendlyByteBuf buf) {
            byte packetIndex = buf.readByte();
            try {
                Class<? extends APacketBase> packetClass = packetMappings.get(packetIndex);
                return new WrapperPacket(packetClass.getConstructor(ByteBuf.class).newInstance(buf));
            } catch (Exception e) {
                e.printStackTrace();
                throw new IndexOutOfBoundsException("Was asked to create packet of index " + packetIndex + " but we haven't registered that one yet!");
            }
        }

        public static void toBytes(WrapperPacket message, FriendlyByteBuf buf) {
            message.packet.writeToBuffer(buf);
        }

        public static void handle(WrapperPacket message, Supplier<NetworkEvent.Context> ctx) {
            if (message.packet.runOnMainThread()) {
                //Need to put this in a runnable to not run it on the network thread and get a CME.
                ctx.get().enqueueWork(() -> {
                    //We need to use side-specific getters here to avoid side-specific classes from trying to be loaded
                    //by the JVM when this method is created.  Failure to do this will result in network faults.
                    //For this, we use abstract methods that are extended in our sub-classes.
                    AWrapperWorld world;
                    if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                        world = getServerWorld(ctx);
                    } else {
                        world = InterfaceManager.clientInterface.getClientWorld();
                    }
                    if (world != null) {
                        message.packet.handle(world);
                    }
                });
            } else {
                if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                    message.packet.handle(getServerWorld(ctx));
                } else {
                    message.packet.handle(InterfaceManager.clientInterface.getClientWorld());
                }
            }
            ctx.get().setPacketHandled(true);
        }
    }
}
