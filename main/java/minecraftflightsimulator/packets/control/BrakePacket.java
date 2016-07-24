package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class BrakePacket implements IMessage{
	private int id;
	private byte brakeCode;

	public BrakePacket() { }
	
	public BrakePacket(int id, byte brakeCode){
		this.id=id;
		this.brakeCode=brakeCode;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.brakeCode=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.brakeCode);
	}

	public static class BrakePacketHandler implements IMessageHandler<BrakePacket, IMessage> {
		public IMessage onMessage(BrakePacket message, MessageContext ctx){
			EntityVehicle thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityVehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityVehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if((message.brakeCode & 2) == 2){
					thisEntity.brakeOn = (message.brakeCode & 1) == 1 ? true : false;
				}
				if((message.brakeCode & 8) == 8){
					thisEntity.parkingBrakeOn = (message.brakeCode & 4) == 4 ? true : false;
				}
				if(ctx.side==Side.SERVER){
					MFS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
