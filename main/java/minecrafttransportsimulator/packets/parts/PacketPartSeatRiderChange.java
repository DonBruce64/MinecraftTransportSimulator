package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartSeatRiderChange extends APacketPart{
	private int rider;
	private boolean mount;

	public PacketPartSeatRiderChange() {}
	
	public PacketPartSeatRiderChange(APart part, Entity rider, boolean mount){
		super(part);
		this.rider = rider.getEntityId();
		this.mount = mount;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.rider=buf.readInt();
		this.mount=buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.rider);
		buf.writeBoolean(this.mount);
	}

	public static class Handler implements IMessageHandler<PacketPartSeatRiderChange, IMessage>{
		@Override
		public IMessage onMessage(final PacketPartSeatRiderChange message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					Entity rider = Minecraft.getMinecraft().world.getEntityByID(message.rider);
					PartSeat seat = (PartSeat) getPartFromMessage(message, ctx);
					if(rider != null && seat != null){
						if(message.mount){
							seat.vehicle.setRiderInSeat(rider, seat);
						}else{
							seat.vehicle.removeRiderFromSeat(rider, seat);
						}
					}
				}
			});
			return null;
		}
	}
}
