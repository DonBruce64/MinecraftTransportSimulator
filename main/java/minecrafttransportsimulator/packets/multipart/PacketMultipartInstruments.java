package minecrafttransportsimulator.packets.multipart;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartInstruments extends APacketMultipartPlayer{
	private byte slotToChange;
	private String instrumentToChangeTo;

	public PacketMultipartInstruments(){}
	
	public PacketMultipartInstruments(EntityMultipartE_Vehicle multipart, EntityPlayer player, byte slotToChange, String instrumentToChangeTo){
		super(multipart, player);
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

	public static class Handler implements IMessageHandler<PacketMultipartInstruments, IMessage> {
		public IMessage onMessage(final PacketMultipartInstruments message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartE_Vehicle multipart = (EntityMultipartE_Vehicle) getMultipart(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					if(multipart != null && player != null){
						//Check to make sure the instrument can fit in survival player's inventories.
						if(!player.capabilities.isCreativeMode && ctx.side.isServer() && multipart.getInstrumentInfoInSlot(message.slotToChange) != null){
							if(!player.inventory.addItemStackToInventory(new ItemStack(MTSRegistry.instrumentItemMap.get(multipart.getInstrumentInfoInSlot(message.slotToChange).name)))){
								return;
							}
						}
						
						//Check to make sure player has the instrument they are trying to put in.
						if(!player.capabilities.isCreativeMode && ctx.side.isServer() && !message.instrumentToChangeTo.isEmpty()){
							if(player.inventory.hasItemStack(new ItemStack(MTSRegistry.instrumentItemMap.get(message.instrumentToChangeTo)))){
								player.inventory.clearMatchingItems(MTSRegistry.instrumentItemMap.get(message.instrumentToChangeTo), -1, 1, null);
							}else{
								return;
							}
						}
						
						multipart.setInstrumentInSlot(message.slotToChange, message.instrumentToChangeTo);
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
