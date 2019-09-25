package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightTypes;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class LightPacket implements IMessage{
	private int id;
	private byte lightOrdinal;

	public LightPacket() { }
	
	public LightPacket(int id, LightTypes light){
		this.id=id;
		this.lightOrdinal=(byte) light.ordinal();
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.lightOrdinal=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.lightOrdinal);
	}

	public static class Handler implements IMessageHandler<LightPacket, IMessage>{
		public IMessage onMessage(final LightPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleE_Powered thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityVehicleE_Powered) ctx.getServerHandler().player.world.getEntityByID(message.id);
					}else{
						thisEntity = (EntityVehicleE_Powered) Minecraft.getMinecraft().world.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						thisEntity.changeLightStatus(LightTypes.values()[message.lightOrdinal], !thisEntity.isLightOn(LightTypes.values()[message.lightOrdinal]));
						if(ctx.side.isServer()){
							MTS.MTSNet.sendToAll(message);
						}
					}
				}
			});
			return null;
		}
	}

}
