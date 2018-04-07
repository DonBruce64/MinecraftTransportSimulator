package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.TileEntityFuelPump;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FuelPumpConnectDisconnectPacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private int id;
	private int amountPresent;
	private int amountTransferred;

	public FuelPumpConnectDisconnectPacket() {}
	
	public FuelPumpConnectDisconnectPacket(TileEntity tile, int id, int amountPresent, int amountTransferred){
		this.x = tile.getPos().getX();
		this.y = tile.getPos().getY();
		this.z = tile.getPos().getZ();
		this.id=id;
		this.amountPresent=amountPresent;
		this.amountTransferred=amountTransferred;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
		this.id=buf.readInt();
		this.amountPresent=buf.readInt();
		this.amountTransferred=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		buf.writeInt(this.id);
		buf.writeInt(this.amountPresent);
		buf.writeInt(this.amountTransferred);
	}

	public static class Handler implements IMessageHandler<FuelPumpConnectDisconnectPacket, IMessage>{
		public IMessage onMessage(final FuelPumpConnectDisconnectPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityFuelPump pump = (TileEntityFuelPump) Minecraft.getMinecraft().theWorld.getTileEntity(new BlockPos(message.x, message.y, message.z));
					if(pump != null){
						if(message.id != -1){
							pump.setConnectedVehicle((EntityMultipartVehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id));
							pump.getInfo().fluid.amount = message.amountPresent;
							pump.totalTransfered = message.amountTransferred;
						}else{
							pump.setConnectedVehicle(null);
							pump.getInfo().fluid.amount = message.amountPresent;
							pump.totalTransfered = message.amountTransferred;
						}
					}
				}
			});
			return null;
		}
	}	
}
