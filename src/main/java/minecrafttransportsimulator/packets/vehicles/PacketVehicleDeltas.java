package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleDeltas extends APacketVehicle{
	private double deltaX;
	private double deltaY;
	private double deltaZ;
	private float deltaYaw;
	private float deltaPitch;
	private float deltaRoll;

	public PacketVehicleDeltas(){}
	
	public PacketVehicleDeltas(EntityVehicleF_Physics vehicle, double deltaX, double deltaY, double deltaZ, float deltaYaw, float deltaPitch, float deltaRoll){
		super(vehicle);
		this.deltaX=deltaX;
		this.deltaY=deltaY;
		this.deltaZ=deltaZ;
		this.deltaYaw=deltaYaw;
		this.deltaPitch=deltaPitch;
		this.deltaRoll=deltaRoll;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);;
		this.deltaX=buf.readDouble();
		this.deltaY=buf.readDouble();
		this.deltaZ=buf.readDouble();
		this.deltaYaw=buf.readFloat();
		this.deltaPitch=buf.readFloat();
		this.deltaRoll=buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeDouble(this.deltaX);
		buf.writeDouble(this.deltaY);
		buf.writeDouble(this.deltaZ);
		buf.writeFloat(this.deltaYaw);
		buf.writeFloat(this.deltaPitch);
		buf.writeFloat(this.deltaRoll);
	}

	public static class Handler implements IMessageHandler<PacketVehicleDeltas, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleDeltas message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Physics vehicle = getVehicle(message, ctx);
					if(vehicle != null){
						vehicle.addToServerDeltas(message.deltaX, message.deltaY, message.deltaZ, message.deltaYaw, message.deltaPitch, message.deltaRoll);
					}
				}
			});
			return null;
		}
	}
}
