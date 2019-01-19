package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartInteraction extends APacketPart{
	private int player;

	public PacketPartInteraction() {}
	
	public PacketPartInteraction(APart part, int player){
		super(part);
		this.player = player;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.player=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.player);
	}

	public static class Handler implements IMessageHandler<PacketPartInteraction, IMessage>{
		@Override
		public IMessage onMessage(final PacketPartInteraction message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					APart part = getVehiclePartFromMessage(message, ctx);
					EntityPlayer player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
					if(part != null && player != null){
						part.interactPart(player);
					}
				}
			});
			return null;
		}
	}
}
