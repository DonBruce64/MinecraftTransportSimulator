package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketVehicle implements IMessage{
	private int id;

	public APacketVehicle(){}
	
	public APacketVehicle(EntityVehicleA_Base vehicle){
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
	
	protected static EntityVehicleA_Base getVehicle(APacketVehicle message, MessageContext ctx){
		if(ctx.side.isServer()){
			return (EntityVehicleA_Base) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
		}else{
			return (EntityVehicleA_Base) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
		}
	}
}
