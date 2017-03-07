package minecraftflightsimulator.packets.general;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;

public class TileEntityClientRequestDataPacket implements IMessage{
	private int x;
	private int y;
	private int z;

	public TileEntityClientRequestDataPacket() {}
	
	public TileEntityClientRequestDataPacket(TileEntity tile){
		this.x = tile.xCoord;
		this.y = tile.yCoord;
		this.z = tile.zCoord;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
	}

	public static class Handler implements IMessageHandler<TileEntityClientRequestDataPacket, TileEntitySyncPacket> {
		public TileEntitySyncPacket onMessage(TileEntityClientRequestDataPacket message, MessageContext ctx){
			if(ctx.side.isServer()){
				TileEntity tile = ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.x, message.y, message.z);
				if(tile != null){
					return new TileEntitySyncPacket(tile);
				}
			}
			return null;
		}
	}	
}
