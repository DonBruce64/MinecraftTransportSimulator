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

	public FuelPumpConnectDisconnectPacket() {}
	
	public FuelPumpConnectDisconnectPacket(TileEntity tile, int id){
		this.x = tile.getPos().getX();
		this.y = tile.getPos().getY();
		this.z = tile.getPos().getZ();
		this.id=id;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
		this.id=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		buf.writeInt(this.id);
	}

	public static class Handler implements IMessageHandler<FuelPumpConnectDisconnectPacket, IMessage>{
		public IMessage onMessage(final FuelPumpConnectDisconnectPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityFuelPump pump = (TileEntityFuelPump) Minecraft.getMinecraft().theWorld.getTileEntity(new BlockPos(message.x, message.y, message.z));
					if(pump != null){
						if(message.id != -1){
							pump.connectedVehicle = (EntityMultipartVehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
							pump.connectedVehicleUUID = pump.connectedVehicle.UUID;
						}else{
							pump.connectedVehicle = null;
							pump.connectedVehicleUUID = "";
						}
					}
				}
			});
			return null;
		}
	}	
}
