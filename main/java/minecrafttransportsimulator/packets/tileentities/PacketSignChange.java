package minecrafttransportsimulator.packets.tileentities;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.decor.TileEntityDecor6AxisSign;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSignChange extends APacketTileEntity{
	private String definition;
	private List<String> text = new ArrayList<String>();

	public PacketSignChange(){}
	
	public PacketSignChange(TileEntityDecor6AxisSign tile, String definition, List<String> text){
		super(tile);
		this.definition = definition;
		this.text = text;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.definition = ByteBufUtils.readUTF8String(buf);
		for(byte i=0; i<PackParserSystem.getSign(definition).general.textLines.length; ++i){
			this.text.add(ByteBufUtils.readUTF8String(buf));
		}
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, this.definition);
		for(byte i=0; i<PackParserSystem.getSign(definition).general.textLines.length; ++i){
			ByteBufUtils.writeUTF8String(buf, text.get(i));
		}
	}

	public static class Handler implements IMessageHandler<PacketSignChange, IMessage>{
		public IMessage onMessage(final PacketSignChange message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityDecor6AxisSign decor = (TileEntityDecor6AxisSign) getTileEntity(message, ctx);
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
