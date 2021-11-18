package minecrafttransportsimulator.packets.components;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketBeaconListingChange;
import minecrafttransportsimulator.packets.instances.PacketEntityCSHandshakeClient;
import minecrafttransportsimulator.packets.instances.PacketEntityCSHandshakeServer;
import minecrafttransportsimulator.packets.instances.PacketEntityColorChange;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityInstrumentChange;
import minecrafttransportsimulator.packets.instances.PacketEntityRiderChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTextChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTrailerChange;
import minecrafttransportsimulator.packets.instances.PacketEntityTrailerConnection;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketFluidTankChange;
import minecrafttransportsimulator.packets.instances.PacketGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketInventoryContainerChange;
import minecrafttransportsimulator.packets.instances.PacketItemInteractable;
import minecrafttransportsimulator.packets.instances.PacketPartChange;
import minecrafttransportsimulator.packets.instances.PacketPartEffector;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartGroundDevice;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packets.instances.PacketPartGunBulletHit;
import minecrafttransportsimulator.packets.instances.PacketPartInteractable;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketPlayerCraftItem;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;
import minecrafttransportsimulator.packets.instances.PacketRadioStateChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFluidLoaderConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpDispense;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadCollisionUpdate;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadConnectionUpdate;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketVehicleInteract;
import minecrafttransportsimulator.packets.instances.PacketVehicleServerMovement;
import minecrafttransportsimulator.packets.instances.PacketWorldSavedDataCSHandshake;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**Interface to the MC networking system.  This interface allows us to send packets
 * around without actually creating packet classes.  Instead, we simply pass-in an
 * object to send over, which contains a handler for how to handle said object.
 * Forge packets do something similar, but Forge can't be bothered to keep networking
 * code the same, so we roll our own here. 
 *
 * @author don_bruce
 */
public class InterfacePacket{
	private static final SimpleNetworkWrapper network = NetworkRegistry.INSTANCE.newSimpleChannel(MasterLoader.MODID);
	private static final BiMap<Byte, Class<? extends APacketBase>> packetMappings = HashBiMap.create();
	
	/**
	 *  Called to init this network.  Needs to be done after networking is ready.
	 *  Packets should be registered at this point in this constructor.
	 */
	public static void init(){
		//Register the main wrapper packet.
		network.registerMessage(WrapperHandler.class, WrapperPacket.class, 0, Side.CLIENT);
		network.registerMessage(WrapperHandler.class, WrapperPacket.class, 1, Side.SERVER);
		
		//Register all classes in the minecrafttransportsimulator.packets.instances package.
		//Ideally this could be done via reflection, but it doesn't work too well so we don't do that.
		byte packetIndex = 0;
		registerPacket(packetIndex++, PacketBeaconListingChange.class);
		
		//Entity packets.
		registerPacket(packetIndex++, PacketEntityCSHandshakeClient.class);
		registerPacket(packetIndex++, PacketEntityCSHandshakeServer.class);
		registerPacket(packetIndex++, PacketEntityColorChange.class);
		registerPacket(packetIndex++, PacketEntityInstrumentChange.class);
		registerPacket(packetIndex++, PacketEntityRiderChange.class);
		registerPacket(packetIndex++, PacketEntityTextChange.class);
		registerPacket(packetIndex++, PacketEntityTrailerChange.class);
		registerPacket(packetIndex++, PacketEntityTrailerConnection.class);
		registerPacket(packetIndex++, PacketEntityVariableIncrement.class);
		registerPacket(packetIndex++, PacketEntityVariableSet.class);
		registerPacket(packetIndex++, PacketEntityVariableToggle.class);
		
		//Fluid tank packets.
		registerPacket(packetIndex++, PacketFluidTankChange.class);
		
		//Inventory container packets.
		registerPacket(packetIndex++, PacketInventoryContainerChange.class);
		registerPacket(packetIndex++, PacketItemInteractable.class);
		
		//GUI packets.
		registerPacket(packetIndex++, PacketGUIRequest.class);
		registerPacket(packetIndex++, PacketEntityGUIRequest.class);
		
		//Part packets.
		registerPacket(packetIndex++, PacketPartChange.class);
		registerPacket(packetIndex++, PacketPartGunBulletHit.class);
		registerPacket(packetIndex++, PacketPartGun.class);
		registerPacket(packetIndex++, PacketPartEffector.class);
		registerPacket(packetIndex++, PacketPartEngine.class);
		registerPacket(packetIndex++, PacketPartGroundDevice.class);
		registerPacket(packetIndex++, PacketPartInteractable.class);
		registerPacket(packetIndex++, PacketPartSeat.class);
		
		//Player packets.
		registerPacket(packetIndex++, PacketPlayerChatMessage.class);
		registerPacket(packetIndex++, PacketPlayerCraftItem.class);
		registerPacket(packetIndex++, PacketPlayerItemTransfer.class);
		
		//Radio packets.
		registerPacket(packetIndex++, PacketRadioStateChange.class);
		
		//Tile entity packets.
		registerPacket(packetIndex++, PacketTileEntityFluidLoaderConnection.class);
		registerPacket(packetIndex++, PacketTileEntityFuelPumpConnection.class);
		registerPacket(packetIndex++, PacketTileEntityFuelPumpDispense.class);
		registerPacket(packetIndex++, PacketTileEntityRoadCollisionUpdate.class);
		registerPacket(packetIndex++, PacketTileEntityPoleChange.class);
		registerPacket(packetIndex++, PacketTileEntityRoadChange.class);
		registerPacket(packetIndex++, PacketTileEntityRoadConnectionUpdate.class);
		registerPacket(packetIndex++, PacketTileEntitySignalControllerChange.class);
		
		//Vehicle packets.
		registerPacket(packetIndex++, PacketVehicleBeaconChange.class);
		registerPacket(packetIndex++, PacketVehicleControlAnalog.class);
		registerPacket(packetIndex++, PacketVehicleControlDigital.class);
		registerPacket(packetIndex++, PacketVehicleInteract.class);
		registerPacket(packetIndex++, PacketVehicleServerMovement.class);
		
		//World packets.
		registerPacket(packetIndex++, PacketWorldSavedDataCSHandshake.class);
	}
	
	/**
	 *  Registers the passed-in packet with the interface.
	 */
	public static void registerPacket(byte packetIndex, Class<? extends APacketBase> packetClass){
		packetMappings.put(packetIndex, packetClass);
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
	 *  Sends the passed-in packet to the passed-in player.
	 *  Note that this may ONLY be called on the server, as
	 *  clients don't know about other player's network pipelines.
	 */
	public static void sendToPlayer(APacketBase packet, WrapperPlayer player){
		network.sendTo(new WrapperPacket(packet), (EntityPlayerMP) player.player);
	}
	
	/**
	 *  Gets the world this packet was sent from based on its context.
	 *  Used for handling packets arriving on the server.
	 */
	private static WrapperWorld getServerWorld(MessageContext ctx){
		return WrapperWorld.getWrapperFor(ctx.getServerHandler().player.world);
	}
	
	
	/**
	 *  Custom class for packets.  Allows for a common packet to be used for all MC versions, 
	 *  as well as less boilerplate code due to thread operations.  Note that when this packet 
	 *  arrives on the other side of the pipeline, MC won't know what class to construct.
	 *  That's up to us to handle via the packet's first byte.  Also note that this class
	 *  must be public, as if it is private MC won't be able to construct it due to access violations.
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
						message.packet.handle(getServerWorld(ctx));
					}else{
						message.packet.handle(InterfaceClient.getClientWorld());
					}
				}
			});
			return null;
		}
	};
}
