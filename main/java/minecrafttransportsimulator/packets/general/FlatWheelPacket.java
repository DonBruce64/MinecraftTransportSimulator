package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FlatWheelPacket implements IMessage{
	private int id;

	public FlatWheelPacket() {}
	
	public FlatWheelPacket(int id){
		this.id = id;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
	}

	public static class Handler implements IMessageHandler<FlatWheelPacket, IMessage>{
		@Override
		public IMessage onMessage(final FlatWheelPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityWheel wheel = (EntityWheel) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					if(wheel != null){
						if(!wheel.isFlat()){
							//Replace regular wheel with flat wheel.
							EntityWheel flatWheel = wheel.getFlatVersion();
							wheel.parent.removeChild(wheel.UUID, false);
							wheel.parent.addChild(flatWheel.UUID, flatWheel, true);
						}
					}
				}
			});
			return null;
		}
	}

}
