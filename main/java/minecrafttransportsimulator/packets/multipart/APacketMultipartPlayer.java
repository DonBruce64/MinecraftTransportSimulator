package minecrafttransportsimulator.packets.multipart;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.multipart.main.EntityMultipartB_Existing;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketMultipartPlayer extends APacketMultipart{
	private int player;

	public APacketMultipartPlayer(){}
	
	public APacketMultipartPlayer(EntityMultipartB_Existing multipart, EntityPlayer player){
		super(multipart);
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
	
	protected static EntityPlayer getPlayerFromMessage(APacketMultipartPlayer message, MessageContext ctx){
		if(ctx.side.isServer()){
			return (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
		}else{
			return (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.player);
		}
	}
}
