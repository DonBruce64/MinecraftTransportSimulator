package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.parts.APartGun;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartGunSignal extends APacketPart{
	private int playerControllerID;

	public PacketPartGunSignal(){}
	
	public PacketPartGunSignal(APartGun gun, int playerControllerID){
		super(gun);
		this.playerControllerID = playerControllerID;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.playerControllerID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.playerControllerID);
	}

	public static class Handler implements IMessageHandler<PacketPartGunSignal, IMessage>{
		public IMessage onMessage(final PacketPartGunSignal message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					APartGun gun = (APartGun) getVehiclePartFromMessage(message, ctx);
					if(gun != null){
						gun.playerControllerID = message.playerControllerID;
						if(ctx.side.isServer()){
							MTS.MTSNet.sendToAll(message);
						}
					}
				}
			});
			return null;
		}
	}
}
