package mcinterface1211;

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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

class InterfacePacket implements IInterfacePacket {
    private static final BiMap<Byte, Class<? extends APacketBase>> packetMappings = HashBiMap.create();

    /**
     * Called to init this network.  Needs to be done after networking is ready.
     * Packets should be registered at this point in this constructor.
     */
    public static void init() {
        //Register internal packets, then external.
        byte packetIndex = 0;
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityCSHandshakeClient.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityCSHandshakeServer.class);
        APacketBase.initPackets(packetIndex);
    }

    /**
     * Called during RegisterPayloadHandlersEvent to register the WrapperPayload type
     * with NeoForge's networking system.
     */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(InterfaceLoader.MODID).versioned("1");
        registrar.playBidirectional(
            WrapperPayload.TYPE,
            WrapperPayload.CODEC,
            WrapperPayload::handle
        );
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
        PacketDistributor.sendToServer(new WrapperPayload(packet));
    }

    @Override
    public void sendToAllClients(APacketBase packet) {
        PacketDistributor.sendToAllPlayers(new WrapperPayload(packet));
    }

    @Override
    public void sendToPlayer(APacketBase packet, IWrapperPlayer player) {
        ServerPlayer serverPlayer = (ServerPlayer) ((WrapperPlayer) player).player;
        PacketDistributor.sendToPlayer(serverPlayer, new WrapperPayload(packet));
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
     * NeoForge 1.21.1 payload record wrapping MTS's APacketBase for the CustomPacketPayload system.
     * Replaces the old WrapperPacket/SimpleChannel approach from Forge.
     */
    public record WrapperPayload(APacketBase packet) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WrapperPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mts", "main"));

        public static final StreamCodec<RegistryFriendlyByteBuf, WrapperPayload> CODEC =
            StreamCodec.of(WrapperPayload::encode, WrapperPayload::decode);

        private static void encode(RegistryFriendlyByteBuf buf, WrapperPayload payload) {
            payload.packet.writeToBuffer(buf);
        }

        private static WrapperPayload decode(RegistryFriendlyByteBuf buf) {
            byte packetIndex = buf.readByte();
            Class<? extends APacketBase> packetClass = packetMappings.get(packetIndex);
            if (packetClass == null) {
                throw new IndexOutOfBoundsException("Was asked to create packet of index " + packetIndex + " but we haven't registered that one yet!");
            }
            try {
                return new WrapperPayload(packetClass.getConstructor(ByteBuf.class).newInstance(buf));
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Was asked to create packet of " + packetClass.getSimpleName() + " but couldn't due to an error.  Something went VERY wrong here.  Check the log for more information.");
            }
        }

        public static void handle(WrapperPayload payload, IPayloadContext context) {
            if (payload.packet.runOnMainThread()) {
                //Need to put this in a runnable to not run it on the network thread and get a CME.
                context.enqueueWork(() -> {
                    AWrapperWorld world;
                    if (context.flow().isServerbound()) {
                        world = WrapperWorld.getWrapperFor(((ServerPlayer) context.player()).serverLevel());
                    } else {
                        world = InterfaceManager.clientInterface.getClientWorld();
                    }
                    if (world != null) {
                        payload.packet.handle(world);
                    }
                });
            } else {
                AWrapperWorld world;
                if (context.flow().isServerbound()) {
                    world = WrapperWorld.getWrapperFor(((ServerPlayer) context.player()).serverLevel());
                } else {
                    world = InterfaceManager.clientInterface.getClientWorld();
                }
                if (world != null) {
                    payload.packet.handle(world);
                }
            }
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
