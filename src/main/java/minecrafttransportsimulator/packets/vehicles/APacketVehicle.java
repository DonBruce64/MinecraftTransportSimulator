package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketVehicle implements IMessage{
	private int id;

	public APacketVehicle(){}
	
	public APacketVehicle(EntityVehicleF_Physics vehicle){
		this.id = vehicle.getEntityId();
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
	}
	
	protected static EntityVehicleF_Physics getVehicle(APacketVehicle message, MessageContext ctx){
		if(ctx.side.isServer()){
			return (EntityVehicleF_Physics) ctx.getServerHandler().player.world.getEntityByID(message.id);
		}else{
			return (EntityVehicleF_Physics) Minecraft.getMinecraft().world.getEntityByID(message.id);
		}
	}
}
