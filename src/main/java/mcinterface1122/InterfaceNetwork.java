package mcinterface1122;

import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packets.components.NetworkSystem;
import minecrafttransportsimulator.packets.components.NetworkSystem.WrapperPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class InterfaceNetwork{
	
	/**
	 *  Sends the passed-in packet to the specified player.
	 *  Note that this may ONLY be called on the server, as
	 *  clients don't know about other player's network pipelines.
	 *  This is package-private as this gets fired from {@link IWrapperPlayer},
	 *  since we need an actual player instance here rather than a wrapper, so we
	 *  shouldn't be able to call this from non-wrapper code.
	 */
	static void sendToPlayer(APacketBase packet, EntityPlayerMP player){
		NetworkSystem.network.sendTo(new WrapperPacket(packet), player);
	}
	
	/**
	 *  Gets the world this packet was sent from based on its context.
	 *  Used for handling packets arriving on the server.
	 */
	public static WrapperWorld getServerWorld(MessageContext ctx){
		return WrapperWorld.getWrapperFor(ctx.getServerHandler().player.world);
	}
	
	/**
	 *  Gets the player this packet was sent by based on its context.
	 *  Used for handling packets arriving on the server.
	 */
	public static WrapperPlayer getServerPlayer(MessageContext ctx){
		return getServerWorld(ctx).getWrapperFor(ctx.getServerHandler().player);
	}
}
