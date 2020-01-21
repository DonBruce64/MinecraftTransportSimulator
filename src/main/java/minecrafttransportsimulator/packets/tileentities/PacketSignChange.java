package minecrafttransportsimulator.packets.tileentities;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.pole.TileEntityPoleSign;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSignChange extends APacketTileEntity{
	private String definition;
	private List<String> text = new ArrayList<String>();
	private int playerID;

	public PacketSignChange(){}
	
	public PacketSignChange(TileEntityPoleSign tile, String definition, List<String> text, int playerID){
		super(tile);
		this.definition = definition;
		this.text = text;
		this.playerID = playerID;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.definition = ByteBufUtils.readUTF8String(buf);
		if(PackParserSystem.getSign(definition).general.textLines != null){
			for(byte i=0; i<PackParserSystem.getSign(definition).general.textLines.length; ++i){
				this.text.add(ByteBufUtils.readUTF8String(buf));
			}
		}
		this.playerID = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, this.definition);
		if(PackParserSystem.getSign(definition).general.textLines != null){
			for(byte i=0; i<PackParserSystem.getSign(definition).general.textLines.length; ++i){
				ByteBufUtils.writeUTF8String(buf, text.get(i));
			}
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
						decor.definition = message.definition;
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
