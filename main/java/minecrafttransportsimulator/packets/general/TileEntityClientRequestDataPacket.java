package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityClientRequestDataPacket implements IMessage{
	private int x;
	private int y;
	private int z;

	public TileEntityClientRequestDataPacket() {}
	
	public TileEntityClientRequestDataPacket(TileEntity tile){
		this.x = tile.getPos().getX();
		this.y = tile.getPos().getY();
		this.z = tile.getPos().getZ();
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
				TileEntity tile = ctx.getServerHandler().playerEntity.worldObj.getTileEntity(new BlockPos(message.x, message.y, message.z));
				if(tile != null){
					return new TileEntitySyncPacket(tile);
				}
			}
			return null;
		}
	}	
}
