package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GUIPacket implements IMessage{
	private int id;

	public GUIPacket() {}
	
	public GUIPacket(int id){
		this.id=id;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
	}

	public static class GUIPacketHandler implements IMessageHandler<GUIPacket, IMessage> {
		public IMessage onMessage(GUIPacket message, MessageContext ctx){
			if(ctx.side.isServer()){
				EntityParent parent = (EntityParent) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
				EntityPlayer player = ctx.getServerHandler().playerEntity;
				if(player.getDistanceToEntity(parent) < 5){
					player.openGui(MFS.instance, parent.getEntityId(), parent.worldObj, 0, 0, 0);
    			}
			}
			return null;
		}
	}

}
