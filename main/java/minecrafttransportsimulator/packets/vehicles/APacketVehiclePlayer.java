package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketVehiclePlayer extends APacketVehicle{
	private int player;

	public APacketVehiclePlayer(){}
	
	public APacketVehiclePlayer(EntityVehicleA_Base vehicle, EntityPlayer player){
		super(vehicle);
		this.player = player.getEntityId();
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.player = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.player);
	}
	
	protected static EntityPlayer getPlayer(APacketVehiclePlayer message, MessageContext ctx){
		if(ctx.side.isServer()){
			return (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
		}else{
			return (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.player);
		}
	}
}
