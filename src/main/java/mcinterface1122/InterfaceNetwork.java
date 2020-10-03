package mcinterface1122;

import java.io.IOException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IInterfaceNetwork;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
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

class InterfaceNetwork implements IInterfaceNetwork{
	private static final SimpleNetworkWrapper network = NetworkRegistry.INSTANCE.newSimpleChannel(MasterInterface.MODID);
	private static final BiMap<Byte, Class<? extends APacketBase>> packetMappings = HashBiMap.create();
	
	/**
	 *  Called to init this network.  Needs to be done after networking is ready.
	 */
	public void init(){
		//Register the main wrapper packet.
		network.registerMessage(WrapperHandler.class, WrapperPacket.class, 0, Side.CLIENT);
		network.registerMessage(WrapperHandler.class, WrapperPacket.class, 1, Side.SERVER);
	}
	
	@Override
	public void registerPacket(byte packetIndex, Class<? extends APacketBase> packetClass){
		packetMappings.put(packetIndex, packetClass);
	}
	
	@Override
	public byte getPacketIndex(APacketBase packet){
		return packetMappings.inverse().get(packet.getClass());
	}
	
	@Override
	public void sendToServer(APacketBase packet){
		network.sendToServer(new WrapperPacket(packet));
	}
	
	@Override
	public void sendToAllClients(APacketBase packet){
		network.sendToAll(new WrapperPacket(packet));
	}
	
	@Override
	public IWrapperNBT createDataFromBuffer(ByteBuf buf){
		PacketBuffer pb = new PacketBuffer(buf);
        try{
        	return new WrapperNBT(pb.readCompoundTag());
        }catch (IOException e){
            // Unpossible? --- Says Forge comments, so who knows?
            throw new RuntimeException(e);
        }
	}
	
	/**
	 *  Sends the passed-in packet to the specified player.
	 *  Note that this may ONLY be called on the server, as
	 *  clients don't know about other player's network pipelines.
	 *  This is package-private as this gets fired from {@link IWrapperPlayer},
	 *  since we need an actual player instance here rather than a wrapper, so we
	 *  shouldn't be able to call this from non-wrapper code.
	 */
	void sendToPlayer(APacketBase packet, EntityPlayerMP player){
		network.sendTo(new WrapperPacket(packet), player);
	}
	
	/**
	 *  Gets the world this packet was sent from based on its context.
	 *  Used for handling packets arriving on the server.
	 */
	private static WrapperWorld getServerWorld(MessageContext ctx){
		return WrapperWorld.getWrapperFor(ctx.getServerHandler().player.world);
	}
	
	/**
	 *  Gets the player this packet was sent by based on its context.
	 *  Used for handling packets arriving on the server.
	 */
	private static WrapperPlayer getServerPlayer(MessageContext ctx){
		return getServerWorld(ctx).getWrapperFor(ctx.getServerHandler().player);
	}
	
	
	/**
	 *  Custom class for packets.  Allows for a common packet to be used for all MC versions, 
	 *  as well as less boilerplate code due to thread operations.  Note that when this packet 
	 *  arrives on the other side of the pipeline, MC won't know what class to construct.
	 *  That's up to us to handle via the packet's first byte.
	 */
	public static class WrapperPacket implements IMessage{
		private APacketBase packet;
		
		/**Do NOT call!  Required to keep Forge from crashing.**/
		public WrapperPacket(){}
		
		public WrapperPacket(APacketBase packet){
			this.packet = packet;
		}
				
		@Override
		public void fromBytes(ByteBuf buf){
			byte packetIndex = buf.readByte();
			try{
				Class<? extends APacketBase> packetClass = packetMappings.get(packetIndex);
				packet = packetClass.getConstructor(ByteBuf.class).newInstance(buf);
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		@Override
		public void toBytes(ByteBuf buf){
			packet.writeToBuffer(buf);
		}
	};
	
	/**
	 *  Custom class for handling packets.  This handler will have an instance of the packet
	 *  class passed-in with all fields populated by {@link WrapperPacket#fromBytes}.
	 */
	public static class WrapperHandler implements IMessageHandler<WrapperPacket, IMessage>{
		@Override
		public IMessage onMessage(WrapperPacket message, MessageContext ctx){
			//Need to put this in a runnable to not run it on the network thread and get a CME.
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					//We need to use side-specific getters here to avoid side-specific classes from trying to be loaded
					//by the JVM when this method is created.  Failure to do this will result in network faults.
					//For this, we use abstract methods that are extended in our sub-classes.
					if(ctx.side.isServer()){
						message.packet.handle(getServerWorld(ctx), getServerPlayer(ctx));
					}else{
						message.packet.handle(MasterInterface.gameInterface.getClientWorld(), MasterInterface.gameInterface.getClientPlayer());
					}
				}
			});
			return null;
		}
	};
}
