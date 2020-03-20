package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartEngineLinked extends APacketPart{
	private int linkedId;
	private double linkedX;
	private double linkedY;
	private double linkedZ;

	public PacketPartEngineLinked(){}
	
	public PacketPartEngineLinked(APartEngine engine, APartEngine engineLinked){
		super(engine);
		this.linkedId = engineLinked.vehicle.getEntityId();
		this.linkedX = engineLinked.offset.x;
		this.linkedY = engineLinked.offset.y;
		this.linkedZ = engineLinked.offset.z;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.linkedId = buf.readInt();
		this.linkedX = buf.readDouble();
		this.linkedY = buf.readDouble();
		this.linkedZ = buf.readDouble();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.linkedId);
		buf.writeDouble(this.linkedX);
		buf.writeDouble(this.linkedY);
		buf.writeDouble(this.linkedZ);
	}

	public static class Handler implements IMessageHandler<PacketPartEngineLinked, IMessage>{
		public IMessage onMessage(final PacketPartEngineLinked message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					APartEngine engine = (APartEngine) getVehiclePartFromMessage(message, ctx);
					
					EntityVehicleE_Powered linkedVehicle = (EntityVehicleE_Powered) Minecraft.getMinecraft().world.getEntityByID(message.linkedId);
					APartEngine linkedEngine = null;
					if(linkedVehicle != null){
						for(APart part : linkedVehicle.getVehicleParts()){
							if(part.offset.x == message.linkedX && part.offset.y == message.linkedY && part.offset.z == message.linkedZ){
								linkedEngine = (APartEngine) part;
							}
						}
					}
					
					if(engine != null && linkedEngine != null){
						engine.linkedEngine = linkedEngine;
						linkedEngine.linkedEngine = engine;
					}
				}
			});
			return null;
		}
	}
}
