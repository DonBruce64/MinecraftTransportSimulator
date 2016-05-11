package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.core.EntityPlane;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class FuelPacket implements IMessage{
	private int id;
	private double fuelAmount;
	private byte buckets;

	public FuelPacket() { }
	
	public FuelPacket(int id, double fuelAmount, byte buckets){
		this.id=id;
		this.fuelAmount=fuelAmount;
		this.buckets=buckets;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.fuelAmount=buf.readDouble();
		this.buckets=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeDouble(this.fuelAmount);
		buf.writeByte(this.buckets);
	}

	public static class FuelPacketHandler implements IMessageHandler<FuelPacket, IMessage> {
		public IMessage onMessage(FuelPacket message, MessageContext ctx){
			if(ctx.side==Side.CLIENT){
				EntityParent thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
				if(thisEntity!=null){
					thisEntity.fuel = message.fuelAmount;
					if(message.buckets != 0){
						thisEntity.emptyBuckets = message.buckets;
					}
				}
			}
			return null;
		}
	}

}
