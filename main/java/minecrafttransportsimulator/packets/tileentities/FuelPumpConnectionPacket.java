package minecrafttransportsimulator.packets.tileentities;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.core.TileEntityFuelPump;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FuelPumpConnectionPacket extends APacketTileEntity{
	private int connectedMultipartID;
	private int amountPresent;
	private int amountTransferred;

	public FuelPumpConnectionPacket(){}
	
	public FuelPumpConnectionPacket(TileEntityFuelPump tile, int id, int amountPresent, int amountTransferred){
		super(tile);
		this.connectedMultipartID=id;
		this.amountPresent=amountPresent;
		this.amountTransferred=amountTransferred;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.connectedMultipartID=buf.readInt();
		this.amountPresent=buf.readInt();
		this.amountTransferred=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.connectedMultipartID);
		buf.writeInt(this.amountPresent);
		buf.writeInt(this.amountTransferred);
	}

	public static class Handler implements IMessageHandler<FuelPumpConnectionPacket, IMessage>{
		public IMessage onMessage(final FuelPumpConnectionPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityFuelPump pump = (TileEntityFuelPump) getTileEntity(message, ctx);
					if(pump != null){
						if(message.connectedMultipartID != -1){
							pump.setConnectedVehicle((EntityMultipartE_Vehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.connectedMultipartID));
						}else{
							pump.setConnectedVehicle(null);
						}
						if(pump.getInfo().fluid != null){
							pump.getInfo().fluid.amount = message.amountPresent;
						}
						pump.totalTransfered = message.amountTransferred;
					}
				}
			});
			return null;
		}
	}	
}
