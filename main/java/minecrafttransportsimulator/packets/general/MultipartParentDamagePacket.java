package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultipartParentDamagePacket implements IMessage{
	private int id;
	private float damage;
	private byte windowBroken;

	public MultipartParentDamagePacket() {}
	
	public MultipartParentDamagePacket(int id, float damage, byte windowBroken){
		this.id = id;
		this.damage = damage;
		this.windowBroken = windowBroken;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.damage=buf.readFloat();
		this.windowBroken=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeFloat(this.damage);
		buf.writeByte(this.windowBroken);
	}

	public static class Handler implements IMessageHandler<MultipartParentDamagePacket, IMessage>{
		@Override
		public IMessage onMessage(final MultipartParentDamagePacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartMoving mover = (EntityMultipartMoving) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					if(mover != null){
						mover.damage += message.damage;
						if(message.windowBroken != -1){
							mover.brokenWindows.add(message.windowBroken);
						}
					}	
				}
			});
			return null;
		}
	}

}
