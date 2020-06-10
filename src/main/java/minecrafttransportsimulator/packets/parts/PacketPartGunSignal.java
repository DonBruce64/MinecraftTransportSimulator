package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartGunSignal extends APacketPart{
	private int playerControllerID;
	private boolean firing;

	public PacketPartGunSignal(){}
	
	public PacketPartGunSignal(PartGun gun, int playerControllerID, boolean firing){
		super(gun);
		this.playerControllerID = playerControllerID;
		this.firing = firing;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.playerControllerID = buf.readInt();
		this.firing = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.playerControllerID);
		buf.writeBoolean(this.firing);
	}

	public static class Handler implements IMessageHandler<PacketPartGunSignal, IMessage>{
		public IMessage onMessage(final PacketPartGunSignal message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					PartGun gun = (PartGun) getVehiclePartFromMessage(message, ctx);
					if(gun != null){
						gun.playerControllerID = message.playerControllerID;
						gun.firing = message.firing;
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
