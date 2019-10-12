package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.mcinterface.MTSPlayer;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleInstruments extends APacketVehiclePlayer{
	private byte slotToChange;
	private String instrumentToChangeTo;

	public PacketVehicleInstruments(){}
	
	public PacketVehicleInstruments(EntityVehicleE_Powered vehicle, MTSPlayer player, byte slotToChange, String instrumentToChangeTo){
		super(vehicle, player);
		this.slotToChange = slotToChange;
		this.instrumentToChangeTo = instrumentToChangeTo;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.slotToChange = buf.readByte();
		this.instrumentToChangeTo = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeByte(this.slotToChange);
		ByteBufUtils.writeUTF8String(buf, this.instrumentToChangeTo);
	}

	public static class Handler implements IMessageHandler<PacketVehicleInstruments, IMessage> {
		public IMessage onMessage(final PacketVehicleInstruments message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) getVehicle(message, ctx);
					MTSPlayer player = getPlayer(message, ctx);
					
					if(vehicle != null && player != null){
						//Check to make sure the instrument can fit in survival player's inventories.
						if(!player.creative() && ctx.side.isServer() && vehicle.getInstrumentInfoInSlot(message.slotToChange) != null){
							if(!player.addStack(new ItemStack(MTSRegistry.instrumentItemMap.get(vehicle.getInstrumentInfoInSlot(message.slotToChange).name)))){
								return;
							}
						}
						
						//Check to make sure player has the instrument they are trying to put in.
						if(!player.creative() && ctx.side.isServer() && !message.instrumentToChangeTo.isEmpty()){
							if(player.hasItems(MTSRegistry.instrumentItemMap.get(message.instrumentToChangeTo), 0, 0)){
								player.removeItems(MTSRegistry.instrumentItemMap.get(message.instrumentToChangeTo), 1, -1);
							}else{
								return;
							}
						}
						
						vehicle.setInstrumentInSlot(message.slotToChange, message.instrumentToChangeTo);
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
