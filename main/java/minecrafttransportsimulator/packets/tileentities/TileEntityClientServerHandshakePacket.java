package minecrafttransportsimulator.packets.tileentities;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityClientServerHandshakePacket extends APacketTileEntity{
	private NBTTagCompound tag = new NBTTagCompound();

	public TileEntityClientServerHandshakePacket() {}
	
	public TileEntityClientServerHandshakePacket(TileEntity tile, NBTTagCompound tag){
		super(tile);
		this.tag = tag;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.tag=ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeTag(buf, tag);
	}

	public static class Handler implements IMessageHandler<TileEntityClientServerHandshakePacket, IMessage>{
		public IMessage onMessage(final TileEntityClientServerHandshakePacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntity tile = getTileEntity(message, ctx);
					if(tile != null){
						if(ctx.side.isServer()){
							MTS.MTSNet.sendTo(new TileEntityClientServerHandshakePacket(tile, tile.writeToNBT(new NBTTagCompound())), ctx.getServerHandler().playerEntity);
						}else{
							tile.readFromNBT(message.tag);
						}
					}
				}
			});
			return null;
		}
	}	
}
