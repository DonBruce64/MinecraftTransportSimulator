package minecrafttransportsimulator.packets.tileentities;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityTrafficSignalController;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTrafficSignalControllerChange extends APacketTileEntity{
	private NBTTagCompound tag = new NBTTagCompound();

	public PacketTrafficSignalControllerChange(){}
	
	public PacketTrafficSignalControllerChange(TileEntityTrafficSignalController tile){
		super(tile);
		this.tag = tile.writeToNBT(new NBTTagCompound());
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

	public static class Handler implements IMessageHandler<PacketTrafficSignalControllerChange, IMessage>{
		public IMessage onMessage(final PacketTrafficSignalControllerChange message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntity tile = getTileEntity(message, ctx);
					if(tile != null){
						tile.readFromNBT(message.tag);
						BlockPos pos = tile.getPos();
						tile.getWorld().notifyBlockUpdate(pos, tile.getWorld().getBlockState(pos), tile.getWorld().getBlockState(pos).getActualState(tile.getWorld(), pos), 3);
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
