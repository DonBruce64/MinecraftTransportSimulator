package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPartEngineSignal extends APacketPart{
	private byte packetType;

	public PacketPartEngineSignal(){}
	
	public PacketPartEngineSignal(APartEngine engine, PacketEngineTypes packetType){
		super(engine);
		this.packetType = (byte) packetType.ordinal();
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.packetType = buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeByte(this.packetType);
	}

	public static class Handler implements IMessageHandler<PacketPartEngineSignal, IMessage>{
		public IMessage onMessage(final PacketPartEngineSignal message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					APartEngine engine = (APartEngine) getVehiclePartFromMessage(message, ctx);
					if(engine != null){
						PacketEngineTypes packetType = PacketEngineTypes.values()[message.packetType];
						switch(packetType){
							case MAGNETO_OFF: engine.setMagnetoStatus(false); break;
							case MAGNETO_ON: engine.setMagnetoStatus(true); break;
							case ES_OFF: engine.setElectricStarterStatus(false); break;
							case ES_ON: engine.setElectricStarterStatus(true); break;
							case HS_ON: engine.handStartEngine(); break;
							case BACKFIRE: engine.backfireEngine(); break;
							case START: engine.startEngine(); break;
							default: engine.stallEngine(packetType); break;
						}
						if(ctx.side.isServer()){
							MTS.MTSNet.sendToAll(message);
						}
					}
				}
			});
			return null;
		}
	}
	
	public enum PacketEngineTypes{
		MAGNETO_OFF,
		MAGNETO_ON,
		ES_OFF,
		ES_ON,
		HS_ON,
		BACKFIRE,
		START,
		FUEL_OUT,
		TOO_SLOW,
		DROWN;
	}
}
