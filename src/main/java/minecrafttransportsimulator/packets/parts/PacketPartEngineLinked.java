package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
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
	
	public PacketPartEngineLinked(PartEngine engine, PartEngine engineLinked){
		super(engine);
		this.linkedId = engineLinked.vehicle.getEntityId();
		this.linkedX = engineLinked.placementOffset.x;
		this.linkedY = engineLinked.placementOffset.y;
		this.linkedZ = engineLinked.placementOffset.z;
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
					PartEngine engine = (PartEngine) getVehiclePartFromMessage(message, ctx);
					
					EntityVehicleF_Physics linkedVehicle = (EntityVehicleF_Physics) Minecraft.getMinecraft().world.getEntityByID(message.linkedId);
					PartEngine linkedEngine = null;
					if(linkedVehicle != null){
						for(APart part : linkedVehicle.getVehicleParts()){
							if(part.placementOffset.x == message.linkedX && part.placementOffset.y == message.linkedY && part.placementOffset.z == message.linkedZ){
								linkedEngine = (PartEngine) part;
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
