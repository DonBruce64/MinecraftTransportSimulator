package mcinterface;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packets.instances.PacketEntityCSHandshake;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketPlayerCraftItem;
import minecrafttransportsimulator.packets.instances.PacketFluidTankChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPumpConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerControlled;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketVehicleInstruments;
import minecrafttransportsimulator.packets.instances.PacketVehicleLightToggle;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartChange;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartGroundDevice;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartGun;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for the MC networking system.  This interface allows us to send packets
 * around without actually creating packet classes.  Instead, we simply pass-in an
 * object to send over, which contains a handler for how to handle said object.
 * Forge packets do something similar, but Forge can't be bothered to keep networking
 * code the same, so we roll our own here. 
 *
 * @author don_bruce
 */
public class InterfaceNetwork{
	private static final SimpleNetworkWrapper network = NetworkRegistry.INSTANCE.newSimpleChannel(MTS.MODID);
	private static final BiMap<Byte, Class<? extends APacketBase>> packetMappings = HashBiMap.create();
	
	/**
	 *  This method is responsible for registering all packets.
	 */
	public static void init(){
		//First register the main wrapper packet.
		network.registerMessage(WrapperHandler.class, WrapperPacket.class, 0, Side.CLIENT);
		network.registerMessage(WrapperHandler.class, WrapperPacket.class, 1, Side.SERVER);
		
		//Now register all classes in the minecrafttransportsimulator.packets.instances package.
		//Ideally this could be done via reflection, but it doesn't work too well so we don't do that.
		byte packetIndex = 0;
		packetMappings.put(packetIndex++, PacketEntityCSHandshake.class);
		packetMappings.put(packetIndex++, PacketEntityRiderChange.class);
		packetMappings.put(packetIndex++, PacketFluidTankChange.class);
		packetMappings.put(packetIndex++, PacketPlayerChatMessage.class);
		packetMappings.put(packetIndex++, PacketPlayerCraftItem.class);
		packetMappings.put(packetIndex++, PacketTileEntityPoleChange.class);
		packetMappings.put(packetIndex++, PacketTileEntityPumpConnection.class);
		packetMappings.put(packetIndex++, PacketTileEntitySignalControllerChange.class);
		packetMappings.put(packetIndex++, PacketTileEntitySignalControllerControlled.class);
		packetMappings.put(packetIndex++, PacketVehicleControlAnalog.class);
		packetMappings.put(packetIndex++, PacketVehicleControlDigital.class);
		packetMappings.put(packetIndex++, PacketVehicleInstruments.class);
		packetMappings.put(packetIndex++, PacketVehicleLightToggle.class);
		packetMappings.put(packetIndex++, PacketVehiclePartChange.class);
		packetMappings.put(packetIndex++, PacketVehiclePartGroundDevice.class);
		packetMappings.put(packetIndex++, PacketVehiclePartGun.class);
		packetMappings.put(packetIndex++, PacketVehicleServerMovement.class);
	}
	
	/**
	 *  Gets the index for the passed-in packet from the mapping.
	 */
	public static byte getPacketIndex(APacketBase packet){
		return packetMappings.inverse().get(packet.getClass());
	}
	
	/**
	 *  Sends the passed-in packet to the server.
	 */
	public static void sendToServer(APacketBase packet){
		network.sendToServer(new WrapperPacket(packet));
	}
	
	/**
	 *  Sends the passed-in packet to all clients.
	 */
	public static void sendToAllClients(APacketBase packet){
		network.sendToAll(new WrapperPacket(packet));
	}
	
	/**
	 *  Sends the passed-in packet to all clients tracking the
	 *  passed-in vehicle.  Useful for preventing packets going to
	 *  vehicles that don't actually exist on clients due to them
	 *  being far away.
	 */
	public static void sendToClientsTracking(APacketBase packet, AEntityBase trackingEntity){
		network.sendToAllTracking(new WrapperPacket(packet), trackingEntity.builder);
	}
	
	/**
	 *  Sends the passed-in packet to all clients near the passed-in
	 *  point.  Useful for clients that are near things that output information.
	 */
	public static void sendToClientsNear(APacketBase packet, int dimension, Point3i point, int distance){
		network.sendToAllTracking(new WrapperPacket(packet), new TargetPoint(dimension, point.x, point.y, point.z, distance));
	}
	
	/**
	 *  Sends the passed-in packet to the specified player.
	 *  Note that this may ONLY be called on the server, as
	 *  clients don't know about other player's network pipelines.
	 *  This is package-private as this gets fired from {@link WrapperPlayer},
	 *  since we need an actual player instance here rather than a wrapper, so we
	 *  shouldn't be able to call this from non-wrapper code.
	 */
	static void sendToPlayer(APacketBase packet, EntityPlayerMP player){
		network.sendTo(new WrapperPacket(packet), player);
	}
	
	/**
	 *  Gets the world this packet was sent from based on its context.
	 *  Used for handling packets arriving on the server.
	 */
	private static WrapperWorld getServerWorld(MessageContext ctx){
		return new WrapperWorld(ctx.getServerHandler().player.world);
	}
	
	/**
	 *  Gets the player this packet was sent by based on its context.
	 *  Used for handling packets arriving on the server.
	 */
	private static WrapperPlayer getServerPlayer(MessageContext ctx){
		return new WrapperPlayer(ctx.getServerHandler().player);
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
						message.packet.handle(InterfaceGame.getClientWorld(), InterfaceGame.getClientPlayer());
					}
				}
			});
			return null;
		}
	};
}
