package minecrafttransportsimulator.packets.control;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import net.minecraft.client.Minecraft;

public class EnginePacket implements IMessage{
	private int id;
	private int engineID;
	private byte engineCode;

	public EnginePacket() { }
	
	public EnginePacket(int id, int engineID, byte engineCode){
		this.id=id;
		this.engineID=engineID;
		this.engineCode=engineCode;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.engineID=buf.readInt();
		this.engineCode=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeInt(this.engineID);
		buf.writeByte(this.engineCode);
	}

	public static class Handler implements IMessageHandler<EnginePacket, IMessage> {
		public IMessage onMessage(EnginePacket message, MessageContext ctx){
			EntityMultipartVehicle vehicle;
			EntityEngine engine;
			if(ctx.side==Side.SERVER){
				vehicle = (EntityMultipartVehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
				engine = (EntityEngine) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.engineID);
			}else{
				vehicle = (EntityMultipartVehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
				engine = (EntityEngine) Minecraft.getMinecraft().theWorld.getEntityByID(message.engineID);
			}	
			if(vehicle != null && engine != null){
				vehicle.handleEngineSignal(engine, message.engineCode);
				if(ctx.side==Side.SERVER){
					MTS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
