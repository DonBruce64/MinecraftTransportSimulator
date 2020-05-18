package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartEngineDamage extends APacketPart{
	private float hours;
	private boolean oilLeak;
	private boolean fuelLeak;
	private boolean brokenStarter;

	public PacketPartEngineDamage(){}
	
	public PacketPartEngineDamage(PartEngine engine, float hours){
		super(engine);
		this.hours = hours;
		this.oilLeak = engine.oilLeak;
		this.fuelLeak = engine.fuelLeak;
		this.brokenStarter = engine.brokenStarter;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.hours = buf.readFloat();
		this.oilLeak = buf.readBoolean();
		this.fuelLeak = buf.readBoolean();
		this.brokenStarter = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeFloat(this.hours);
		buf.writeBoolean(this.oilLeak);
		buf.writeBoolean(this.fuelLeak);
		buf.writeBoolean(this.brokenStarter);
	}

	public static class Handler implements IMessageHandler<PacketPartEngineDamage, IMessage>{
		public IMessage onMessage(final PacketPartEngineDamage message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					PartEngine engine = (PartEngine) getVehiclePartFromMessage(message, ctx);
					if(engine != null){
						engine.hours += message.hours;
						if(!engine.fuelLeak){
							engine.fuelLeak = message.fuelLeak;
						}
						if(!engine.oilLeak){
							engine.oilLeak = message.oilLeak;
						}
						if(!engine.brokenStarter){
							engine.brokenStarter = message.brokenStarter;
						}
					}
				}
			});
			return null;
		}
	}
}
