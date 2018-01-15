package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.TileEntityFuelPump;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FuelPumpFillDrainPacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private FluidStack stack;

	public FuelPumpFillDrainPacket() {}
	
	public FuelPumpFillDrainPacket(TileEntity tile, FluidStack stack){
		this.x = tile.getPos().getX();
		this.y = tile.getPos().getY();
		this.z = tile.getPos().getZ();
		this.stack=stack;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
		this.stack=FluidStack.loadFluidStackFromNBT(ByteBufUtils.readTag(buf));
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		ByteBufUtils.writeTag(buf, this.stack.writeToNBT(new NBTTagCompound()));
	}

	public static class Handler implements IMessageHandler<FuelPumpFillDrainPacket, IMessage>{
		public IMessage onMessage(final FuelPumpFillDrainPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityFuelPump pump = (TileEntityFuelPump) Minecraft.getMinecraft().theWorld.getTileEntity(new BlockPos(message.x, message.y, message.z));
					if(pump != null){
						if(pump.getFluid() == null){
							pump.setFluid(message.stack.getFluid());
						}
						pump.getFluid().amount += message.stack.amount;
					}
				}
			});
			return null;
		}
	}	
}
