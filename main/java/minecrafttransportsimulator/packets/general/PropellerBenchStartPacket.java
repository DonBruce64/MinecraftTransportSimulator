package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PropellerBenchStartPacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private byte propellerType;
	private byte numberBlades;
	private byte pitch;
	private byte diameter;
	private long timeOperationFinished;

	public PropellerBenchStartPacket() {}
	
	public PropellerBenchStartPacket(TileEntityPropellerBench tile){
		this.x = tile.getPos().getX();
		this.y = tile.getPos().getY();
		this.z = tile.getPos().getZ();
		this.propellerType = tile.propellerType;
		this.numberBlades = tile.numberBlades;
		this.pitch = tile.pitch;
		this.diameter = tile.diameter;
		this.timeOperationFinished = tile.timeOperationFinished;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
		this.propellerType=buf.readByte();
		this.numberBlades=buf.readByte();
		this.pitch=buf.readByte();
		this.diameter=buf.readByte();
		this.timeOperationFinished=buf.readLong();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		buf.writeByte(this.propellerType);
		buf.writeByte(this.numberBlades);
		buf.writeByte(this.pitch);
		buf.writeByte(this.diameter);
		buf.writeLong(this.timeOperationFinished);
	}

	public static class Handler implements IMessageHandler<PropellerBenchStartPacket, IMessage>{
		public IMessage onMessage(final PropellerBenchStartPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityPropellerBench bench;
					if(ctx.side.isServer()){
						bench = (TileEntityPropellerBench) ctx.getServerHandler().playerEntity.worldObj.getTileEntity(new BlockPos(message.x, message.y, message.z));
					}else{
						bench = (TileEntityPropellerBench) Minecraft.getMinecraft().theWorld.getTileEntity(new BlockPos(message.x, message.y, message.z));
					}
					if(bench != null){
						bench.propellerType = message.propellerType;
						bench.numberBlades = message.numberBlades;
						bench.pitch = message.pitch;
						bench.diameter = message.diameter;
						bench.timeOperationFinished = message.timeOperationFinished;
					}
					if(ctx.side.isServer()){
						MTS.MTSNet.sendToAll(message);
					}
				}
			});
			return null;
		}
	}	
}
