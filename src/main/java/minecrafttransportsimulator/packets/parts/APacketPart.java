package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketPart implements IMessage{
	private int id;
	private double x;
	private double y;
	private double z;

	public APacketPart(){}
	
	public APacketPart(APart part){
		this.id = part.vehicle.getEntityId();
		this.x = part.placementOffset.x;
		this.y = part.placementOffset.y;
		this.z = part.placementOffset.z;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id = buf.readInt();
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeDouble(this.x);
		buf.writeDouble(this.y);
		buf.writeDouble(this.z);
	}
	
	protected static APart getVehiclePartFromMessage(APacketPart message, MessageContext ctx){
		EntityVehicleE_Powered vehicle;
		if(ctx.side.isServer()){
			vehicle = (EntityVehicleE_Powered) ctx.getServerHandler().player.world.getEntityByID(message.id);
		}else{
			vehicle = (EntityVehicleE_Powered) Minecraft.getMinecraft().world.getEntityByID(message.id);
		}
		if(vehicle != null){
			for(APart part : vehicle.getVehicleParts()){
				if(part.placementOffset.x == message.x && part.placementOffset.y == message.y && part.placementOffset.z == message.z){
					return part;
				}
			}
		}
		return null;
	}
}
