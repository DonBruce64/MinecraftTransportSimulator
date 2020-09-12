package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PacketPartGroundDeviceWheelFlat extends APacketPart{
	public PacketPartGroundDeviceWheelFlat(){}
	
	private boolean isFlat;

	public PacketPartGroundDeviceWheelFlat(PartGroundDevice part){
		super(part);
		this.isFlat = true;
	}
	
	public PacketPartGroundDeviceWheelFlat(PartGroundDevice part, boolean isFlat){
		super(part);
		this.isFlat = isFlat;
	}

	@Override
	public void fromBytes(ByteBuf buf){
		super.readBytes(buf);
		this.isFlat = buf.readBoolean();
	}
	
	@Override
	public void toBytes(ByteBuf buf){
		super.writeBytes(buf);
		buf.writeBoolean(this.isFlat);
	}
	
	public static class Handler implements IMessageHandler<PacketPartGroundDeviceWheelFlat, IMessage>{
		@Override
		public IMessage onMessage(final PacketPartGroundDeviceWheelFlat message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					PartGroundDevice wheel = (PartGroundDevice) getVehiclePartFromMessage(message, ctx);
					if(wheel != null){
						if(message.isFlat){
							wheel.setFlat();							
						}else{
							wheel.repair();
						}
					}
				}
			});
			return null;
		}
	}
	
	public boolean isFlat(){
		return this.isFlat;
	}
}