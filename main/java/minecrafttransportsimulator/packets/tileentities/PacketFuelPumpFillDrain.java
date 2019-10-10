package minecrafttransportsimulator.packets.tileentities;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.core.TileEntityFuelPump;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketFuelPumpFillDrain extends APacketTileEntity{
	private boolean drain;
	private FluidStack stack;

	public PacketFuelPumpFillDrain() {}
	
	public PacketFuelPumpFillDrain(TileEntityFuelPump tile, FluidStack stack, boolean drain){
		super(tile);
		this.stack=stack;
		this.drain=drain;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.stack=FluidStack.loadFluidStackFromNBT(ByteBufUtils.readTag(buf));
		this.drain=buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeTag(buf, this.stack.writeToNBT(new NBTTagCompound()));
		buf.writeBoolean(this.drain);
	}

	public static class Handler implements IMessageHandler<PacketFuelPumpFillDrain, IMessage>{
		public IMessage onMessage(final PacketFuelPumpFillDrain message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityFuelPump pump = (TileEntityFuelPump) getTileEntity(message, ctx);
					if(pump != null){
						if(pump.getFluid() == null){
							pump.setFluid(message.stack.getFluid());
						}
						if(message.drain){
							pump.getFluid().amount -= message.stack.amount;
						}else{
							pump.getFluid().amount += message.stack.amount;
						}
					}
				}
			});
			return null;
		}
	}	
}
