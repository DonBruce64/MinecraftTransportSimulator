package minecrafttransportsimulator.packets.general;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class InstrumentPlanePacket implements IMessage{
	private int id;
	private int player;
	private byte instrumentToChange;
	private byte instrumentToChangeTo;

	public InstrumentPlanePacket() { }
	
	public InstrumentPlanePacket(int id, int player, byte instrumentToChange, byte instrumentToChangeTo){
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

	public static class Handler implements IMessageHandler<InstrumentPlanePacket, IMessage> {
		public IMessage onMessage(InstrumentPlanePacket message, MessageContext ctx){
			EntityPlane plane;
			EntityPlayer player;
			if(ctx.side.isServer()){
				plane = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
				player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
			}else{
				plane = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
				player = (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.player);
			}
			if(plane != null && player != null){
				if(message.instrumentToChangeTo == 0){
					if(plane.instruments.containsKey(message.instrumentToChange)){
						if(!player.capabilities.isCreativeMode){
							if(!player.inventory.addItemStackToInventory(new ItemStack(MTSRegistry.flightInstrument, 1, plane.instruments.get(message.instrumentToChange)))){
								return null;
							}
						}
						plane.instruments.put(message.instrumentToChange, message.instrumentToChangeTo);
						if(ctx.side.isServer()){
							MTS.MFSNet.sendToAll(message);
						}
					}
				}else{
					if(!player.capabilities.isCreativeMode){
						if(PlayerHelper.getQtyOfItemInInventory(MTSRegistry.flightInstrument, message.instrumentToChangeTo, player) > 0){
							PlayerHelper.removeQtyOfItemInInventory(MTSRegistry.flightInstrument, 1, message.instrumentToChangeTo, player);
							plane.instruments.put(message.instrumentToChange, message.instrumentToChangeTo);
							if(ctx.side.isServer()){
								MTS.MFSNet.sendToAll(message);
							}
						}
					}else{
						plane.instruments.put(message.instrumentToChange, message.instrumentToChangeTo);
						if(ctx.side.isServer()){
							MTS.MFSNet.sendToAll(message);
						}
					}
				}
			}
			return null;
		}
	}
}
