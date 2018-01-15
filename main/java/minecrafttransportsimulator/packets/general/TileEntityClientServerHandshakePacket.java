package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TileEntityClientServerHandshakePacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private NBTTagCompound tag = new NBTTagCompound();

	public TileEntityClientServerHandshakePacket() {}
	
	public TileEntityClientServerHandshakePacket(TileEntity tile, NBTTagCompound tag){
		this.x = tile.getPos().getX();
		this.y = tile.getPos().getY();
		this.z = tile.getPos().getZ();
		this.tag = tag;
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

	public static class Handler implements IMessageHandler<TileEntityClientServerHandshakePacket, IMessage>{
		public IMessage onMessage(final TileEntityClientServerHandshakePacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					if(ctx.side.isServer()){
						TileEntity tile = ctx.getServerHandler().playerEntity.worldObj.getTileEntity(new BlockPos(message.x, message.y, message.z));
						if(tile != null){
							MTS.MTSNet.sendTo(new TileEntityClientServerHandshakePacket(tile, tile.writeToNBT(new NBTTagCompound())), ctx.getServerHandler().playerEntity);
						}
					}else{
						TileEntity tile = Minecraft.getMinecraft().theWorld.getTileEntity(new BlockPos(message.x, message.y, message.z));
						if(tile != null){
							tile.readFromNBT(message.tag);
						}
					}
				}
			});
			return null;
		}
	}	
}
