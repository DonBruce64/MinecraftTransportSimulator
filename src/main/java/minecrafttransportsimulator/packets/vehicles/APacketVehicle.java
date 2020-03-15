package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketVehicle implements IMessage{
	private int id;

	public APacketVehicle(){}
	
	public APacketVehicle(EntityVehicleE_Powered vehicle){
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
	
	protected static EntityVehicleE_Powered getVehicle(APacketVehicle message, MessageContext ctx){
		if(ctx.side.isServer()){
			return (EntityVehicleE_Powered) ctx.getServerHandler().player.world.getEntityByID(message.id);
		}else{
			return (EntityVehicleE_Powered) Minecraft.getMinecraft().world.getEntityByID(message.id);
		}
	}
}
