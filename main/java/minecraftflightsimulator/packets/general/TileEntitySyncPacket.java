package minecraftflightsimulator.packets.general;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntitySyncPacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private NBTTagCompound tag = new NBTTagCompound();

	public TileEntitySyncPacket() {}
	
	public TileEntitySyncPacket(TileEntity tile){
		this.x = tile.xCoord;
		this.y = tile.yCoord;
		this.z = tile.zCoord;
		tile.writeToNBT(tag);
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
		this.tag=ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		ByteBufUtils.writeTag(buf, tag);
	}

	public static class Handler implements IMessageHandler<TileEntitySyncPacket, IMessage> {
		public IMessage onMessage(TileEntitySyncPacket message, MessageContext ctx){
			TileEntity tile;
			if(ctx.side.isServer()){
				tile = ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.x, message.y, message.z);
			}else{
				tile = Minecraft.getMinecraft().theWorld.getTileEntity(message.x, message.y, message.z);
			}
			if(tile != null){
				tile.readFromNBT(message.tag);
			}
			if(ctx.side.isServer()){
				MFS.MFSNet.sendToAll(message);
			}
			return null;
		}
	}	
}
