package minecrafttransportsimulator.packets.multipart;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartInstruments extends APacketMultipartPlayer{
	private byte instrumentToChange;
	private byte instrumentToChangeTo;

	public PacketMultipartInstruments(){}
	
	public PacketMultipartInstruments(EntityMultipartE_Vehicle multipart, EntityPlayer player, byte instrumentToChange, byte instrumentToChangeTo){
		super(multipart, player);
		this.instrumentToChange=instrumentToChange;
		this.instrumentToChangeTo=instrumentToChangeTo;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.instrumentToChange=buf.readByte();
		this.instrumentToChangeTo=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeByte(instrumentToChange);
		buf.writeByte(instrumentToChangeTo);
	}

	public static class Handler implements IMessageHandler<PacketMultipartInstruments, IMessage> {
		public IMessage onMessage(final PacketMultipartInstruments message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartE_Vehicle multipart = (EntityMultipartE_Vehicle) getMultipart(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					if(multipart != null && player != null){
						byte blankInstrumentNumber = (byte) multipart.getBlankInstrument().ordinal();
						//Check to make sure the instrument can fit in survival player's inventories.
						if(!player.capabilities.isCreativeMode && ctx.side.isServer() && !multipart.getInstrumentNumber(message.instrumentToChange).equals(Instruments.values()[blankInstrumentNumber])){
							if(!player.inventory.addItemStackToInventory(new ItemStack(MTSRegistry.instrument, 1, multipart.getInstrumentNumber(message.instrumentToChange).ordinal()))){
								return;
							}
						}
						
						//Check to make sure player has the instrument they are trying to put in.
						if(!player.capabilities.isCreativeMode && ctx.side.isServer() && message.instrumentToChangeTo != blankInstrumentNumber){
							if(player.inventory.hasItemStack(new ItemStack(MTSRegistry.instrument, 1, message.instrumentToChangeTo))){
								player.inventory.clearMatchingItems(MTSRegistry.instrument, message.instrumentToChangeTo, 1, null);
							}else{
								return;
							}
						}
						
						multipart.setInstrumentNumber(message.instrumentToChange, Instruments.values()[message.instrumentToChangeTo]);
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
