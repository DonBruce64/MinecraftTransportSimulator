package minecrafttransportsimulator.packets.tileentities;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketTileEntity implements IMessage{
	private BlockPos tileEntityPos;

	public APacketTileEntity(){}
	
	public APacketTileEntity(TileEntity tile){
		this.tileEntityPos = tile.getPos();
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.tileEntityPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.tileEntityPos.getX());
		buf.writeInt(this.tileEntityPos.getY());
		buf.writeInt(this.tileEntityPos.getZ());
	}

	protected static TileEntity getTileEntity(APacketTileEntity message, MessageContext ctx){
		if(ctx.side.isServer()){
			return ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.tileEntityPos);
		}else{
			return Minecraft.getMinecraft().theWorld.getTileEntity(message.tileEntityPos);
		}
	}
}
