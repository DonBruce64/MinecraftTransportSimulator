package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.MTSPlayer;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketVehiclePlayer extends APacketVehicle{
	private int playerID;

	public APacketVehiclePlayer(){}
	
	public APacketVehiclePlayer(EntityVehicleA_Base vehicle, MTSPlayer player){
		super(vehicle);
		this.playerID = player.getID();
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.playerID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.playerID);
	}
	
	protected static MTSPlayer getPlayer(APacketVehiclePlayer message, MessageContext ctx){
		if(ctx.side.isServer()){
			return new MTSPlayer((EntityPlayer) ctx.getServerHandler().player.world.getEntityByID(message.playerID));
		}else{
			return new MTSPlayer((EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(message.playerID));
		}
	}
}
