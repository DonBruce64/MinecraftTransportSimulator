package minecrafttransportsimulator.mcinterface;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**Networking class.  Responsible for running as the network system and sending out packets.
 * Takes in and spits out MTSPacket classes, so use those when dealing with MTSNet systems.
 * Make sure to call the init() method somewhere in the code when the network is able to be
 * set up, otherwise it won't work. 
 * 
 * @author don_bruce
 */
public class MTSNetwork{
	private static final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel("MTSNet");
	
	/**Sends a packet to the server for processing.*/
	public static void sendPacketToServer(MTSPacket packet){
		channel.sendToServer(packet);
	}
	
	/**Sends a packet to all clients.*/
	public static void sendPacketToClients(MTSPacket packet){
		channel.sendToAll(packet);
	}
	
	/**Sends a packet to the specific player.*/
	public static void sendPacketToClient(MTSPacket packet, MTSPlayerInterface player){
		channel.sendTo(packet, player.getMultiplayer());
	}
	
	/**Sends a packet to whatever players are tracking (have loaded) the passed-in Entity.*/
	public static void sendPacketToPlayersTracking(MTSPacket packet, Entity entity){
		channel.sendToAllTracking(packet, entity);
	}
	
	
	
	//---------------START OF CUSTOM CLASSES---------------//
	/**Base class for all packets.  All packets send via the MTSNetwork are
	 * required to extend this class and implement the required methods.
	 * 
	 * @author don_bruce
	 */
	public static abstract class MTSPacket implements IMessage{	
		//---------------START OF FORWARDED METHODS---------------//
		@Override
		public void fromBytes(ByteBuf buf){
			populateFromBytes(new PacketBuffer(buf));
		}
		/**Parses out the variables from the ByteBuf.*/
		public abstract void populateFromBytes(PacketBuffer buf);
		

		@Override
		public void toBytes(ByteBuf buf){
			convertToBytes(new PacketBuffer(buf));
		}
		/**Writes the variables in this class to the ByteBuf.*/
		public abstract void convertToBytes(PacketBuffer buf);
		
		public static class MTSPacketHandler implements IMessageHandler<MTSPacket, IMessage>{
			@Override
			public IMessage onMessage(final MTSPacket message, final MessageContext ctx){
				FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
					@Override
					public void run(){
						if(ctx.side.isServer()){
							message.handlePacket(new MTSWorldInterface(ctx.getServerHandler().player.world), new MTSPlayerInterface(ctx.getServerHandler().player), true);
						}else{
							message.handlePacket(new MTSWorldInterface(Minecraft.getMinecraft().world), new MTSPlayerInterface(Minecraft.getMinecraft().player), false);
						}
					}
				});
				return null;
			}
		}
		/**Handles the logic once the packet has been received.*/
		public abstract void handlePacket(MTSWorldInterface world, MTSPlayerInterface sendingPlayer, boolean onServer);
	}
}
