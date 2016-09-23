package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class LightPacket implements IMessage{
	private int id;
	private boolean auxLights;

	public LightPacket() { }
	
	public LightPacket(int id, boolean auxLights){
		this.id=id;
		this.auxLights=auxLights;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.auxLights=buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeBoolean(this.auxLights);
	}

	public static class Handler implements IMessageHandler<LightPacket, IMessage> {
		public IMessage onMessage(LightPacket message, MessageContext ctx){
			EntityVehicle thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityVehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityVehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(message.auxLights){
					thisEntity.auxLightsOn = !thisEntity.auxLightsOn;
				}else{
					thisEntity.lightsOn = !thisEntity.lightsOn;
				}
				if(ctx.side==Side.SERVER){
					MFS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
