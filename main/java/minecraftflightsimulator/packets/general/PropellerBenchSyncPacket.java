package minecraftflightsimulator.packets.general;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;

public class PropellerBenchSyncPacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private NBTTagCompound tag = new NBTTagCompound();

	public PropellerBenchSyncPacket() {}
	
	public PropellerBenchSyncPacket(TileEntityPropellerBench tile){
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

	public static class Handler implements IMessageHandler<PropellerBenchSyncPacket, IMessage> {
		public IMessage onMessage(PropellerBenchSyncPacket message, MessageContext ctx){
			if(ctx.side==Side.CLIENT){
				TileEntityPropellerBench bench = (TileEntityPropellerBench) BlockHelper.getTileEntityFromCoords(Minecraft.getMinecraft().theWorld, message.x, message.y, message.z);
				if(bench != null){
					bench.readFromNBT(message.tag);
				}
			}
			return null;
		}
	}	
}
