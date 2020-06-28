package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleClientInitResponse extends APacketVehicle{
	private NBTTagCompound tagCompound;

	public PacketVehicleClientInitResponse(){}
	
	public PacketVehicleClientInitResponse(EntityVehicleF_Physics vehicle, NBTTagCompound tagCompound){
		super(vehicle);
		this.tagCompound=tagCompound;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.tagCompound=ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeTag(buf, this.tagCompound);
	}

	public static class Handler implements IMessageHandler<PacketVehicleClientInitResponse, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleClientInitResponse message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Physics vehicle = getVehicle(message, ctx);
					if(vehicle != null){
						vehicle.readFromNBT(message.tagCompound);
					}
				}
			});
			return null;
		}
	}
}
