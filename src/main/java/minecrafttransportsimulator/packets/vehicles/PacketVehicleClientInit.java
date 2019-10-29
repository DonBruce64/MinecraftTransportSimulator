package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleClientInit extends APacketVehicle{

	public PacketVehicleClientInit(){}
	
	public PacketVehicleClientInit(EntityVehicleA_Base vehicle){
		super(vehicle);
	}

	public static class Handler implements IMessageHandler<PacketVehicleClientInit, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleClientInit message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleA_Base vehicle = getVehicle(message, ctx);
					if(vehicle!=null){
						NBTTagCompound tagCompound = new NBTTagCompound();
						vehicle.writeToNBT(tagCompound);
						MTS.MTSNet.sendTo(new PacketVehicleClientInitResponse(vehicle, tagCompound), ctx.getServerHandler().player);
					}
				}
			});
			return null;
		}
	}
}
