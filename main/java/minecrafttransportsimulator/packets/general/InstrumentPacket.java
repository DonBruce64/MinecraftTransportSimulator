package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class InstrumentPacket implements IMessage{
	private int id;
	private int player;
	private byte instrumentToChange;
	private byte instrumentToChangeTo;

	public InstrumentPacket() {}
	
	public InstrumentPacket(int id, int player, byte instrumentToChange, byte instrumentToChangeTo){
		this.id=id;
		this.player=player;
		this.instrumentToChange=instrumentToChange;
		this.instrumentToChangeTo=instrumentToChangeTo;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.player=buf.readInt();
		this.instrumentToChange=buf.readByte();
		this.instrumentToChangeTo=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeInt(this.player);
		buf.writeByte(instrumentToChange);
		buf.writeByte(instrumentToChangeTo);
	}

	public static class Handler implements IMessageHandler<InstrumentPacket, IMessage> {
		public IMessage onMessage(final InstrumentPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartVehicle vehicle;
					EntityPlayer player;
					if(ctx.side.isServer()){
						vehicle = (EntityMultipartVehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
						player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
					}else{
						vehicle = (EntityMultipartVehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
						player = (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.player);
					}
					if(vehicle != null && player != null){
						byte blankInstrumentNumber = (byte) vehicle.getBlankInstrument().ordinal();
						//Check to make sure the instrument can fit in survival player's inventories.
						if(!player.capabilities.isCreativeMode && ctx.side.isServer() && !vehicle.getInstrumentNumber(message.instrumentToChange).equals(Instruments.values()[blankInstrumentNumber])){
							if(!player.inventory.addItemStackToInventory(new ItemStack(MTSRegistry.instrument, 1, vehicle.getInstrumentNumber(message.instrumentToChange).ordinal()))){
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
						
						vehicle.setInstrumentNumber(message.instrumentToChange, Instruments.values()[message.instrumentToChangeTo]);
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
