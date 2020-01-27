package minecrafttransportsimulator.packets.tileentities;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.pole.TileEntityPoleSign;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONSign;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSignChange extends APacketTileEntity{
	private String packID;
	private String systemName;
	private byte textLines;
	private List<String> text = new ArrayList<String>();
	private int playerID;

	public PacketSignChange(){}
	
	public PacketSignChange(TileEntityPoleSign tile, JSONSign definition, List<String> text, int playerID){
		super(tile);
		this.packID = definition.packID;
		this.systemName = definition.systemName;
		this.textLines = (byte) text.size();
		this.text = text;
		this.playerID = playerID;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.packID = ByteBufUtils.readUTF8String(buf);
		this.systemName = ByteBufUtils.readUTF8String(buf);
		this.textLines = buf.readByte();
		for(byte i=0; i<textLines; ++i){
			this.text.add(ByteBufUtils.readUTF8String(buf));
		}
		this.playerID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, this.packID);
		ByteBufUtils.writeUTF8String(buf, this.systemName);
		buf.writeByte(this.textLines);
		for(byte i=0; i<this.textLines; ++i){
			ByteBufUtils.writeUTF8String(buf, text.get(i));
		}
		buf.writeInt(this.playerID);
	}

	public static class Handler implements IMessageHandler<PacketSignChange, IMessage>{
		public IMessage onMessage(final PacketSignChange message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityPoleSign decor = (TileEntityPoleSign) getTileEntity(message, ctx);
					if(ConfigSystem.configObject.general.opSignEditingOnly.value){
						boolean isPlayerOP = false;
						EntityPlayer player = (EntityPlayer) ctx.getServerHandler().player.world.getEntityByID(message.playerID);
						if(player != null){
							isPlayerOP = player.getServer() == null || player.getServer().isSinglePlayer() || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
						}
						if(!isPlayerOP){
							return;
						}
					}
					if(decor != null){
						decor.definition = MTSRegistry.packSignMap.get(message.packID).get(message.systemName);
						decor.text = message.text;
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
