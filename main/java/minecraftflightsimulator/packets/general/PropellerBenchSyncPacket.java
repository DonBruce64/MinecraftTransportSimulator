package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PropellerBenchSyncPacket implements IMessage{
	private int x;
	private int y;
	private int z;

	public PropellerBenchSyncPacket() {}
	
	public PropellerBenchSyncPacket(TileEntityPropellerBench tile){
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

	public static class Handler implements IMessageHandler<PropellerBenchSyncPacket, PropellerBenchSyncPacket> {
		public PropellerBenchSyncPacket onMessage(PropellerBenchSyncPacket message, MessageContext ctx){
			if(ctx.side.isServer()){
				TileEntityPropellerBench tile = (TileEntityPropellerBench) ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.x, message.y, message.z);
				ctx.getServerHandler().playerEntity.playerNetServerHandler.sendPacket(tile.getDescriptionPacket());
			}
			return null;
		}
	}	
}
