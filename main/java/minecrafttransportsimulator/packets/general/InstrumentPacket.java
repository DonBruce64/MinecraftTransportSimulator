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
						//If the instrument selected is a blank, remove the current instrument.
						if(message.instrumentToChangeTo != blankInstrumentNumber){
							//If there's an instrument present, give it back.
							if(!vehicle.getInstrumentNumber(message.instrumentToChange).equals(Instruments.values()[blankInstrumentNumber])){
								if(!player.capabilities.isCreativeMode){
									//If the player can't fit the old instrument into their inventory, don't make the change.
									if(!player.inventory.addItemStackToInventory(new ItemStack(MTSRegistry.instrument, 1, vehicle.getInstrumentNumber(message.instrumentToChange).ordinal()))){
										return;
									}
									player.inventory.clearMatchingItems(MTSRegistry.instrument, message.instrumentToChangeTo, 1, null);
								}
								vehicle.setInstrumentNumber(message.instrumentToChange, Instruments.values()[message.instrumentToChangeTo]);
								if(ctx.side.isServer()){
									MTS.MTSNet.sendToAll(message);
								}
							}
						}else{
							//This time we are adding a new instrument.
							//Check if we are in creative mode and go from there like before.
							if(!player.capabilities.isCreativeMode){
								if(player.inventory.hasItemStack(new ItemStack(MTSRegistry.instrument, 1, message.instrumentToChangeTo))){
									player.inventory.clearMatchingItems(MTSRegistry.instrument, message.instrumentToChangeTo, 1, null);
									vehicle.setInstrumentNumber(message.instrumentToChange, Instruments.values()[message.instrumentToChangeTo]);
									if(ctx.side.isServer()){
										MTS.MTSNet.sendToAll(message);
									}
								}
							}else{
								vehicle.setInstrumentNumber(message.instrumentToChange, Instruments.values()[message.instrumentToChangeTo]);
								if(ctx.side.isServer()){
									MTS.MTSNet.sendToAll(message);
								}
							}
						}
					}
				}
			});
			return null;
		}
	}
}
