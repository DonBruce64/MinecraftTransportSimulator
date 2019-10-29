package minecrafttransportsimulator.packets.tileentities;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.core.TileEntityFuelPump;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketFuelPumpConnection extends APacketTileEntity{
	private int connectedVehicleID;
	private int amountPresent;
	private int amountTransferred;

	public PacketFuelPumpConnection(){}
	
	public PacketFuelPumpConnection(TileEntityFuelPump tile, int id, int amountPresent, int amountTransferred){
		super(tile);
		this.connectedVehicleID=id;
		this.amountPresent=amountPresent;
		this.amountTransferred=amountTransferred;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.connectedVehicleID=buf.readInt();
		this.amountPresent=buf.readInt();
		this.amountTransferred=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeInt(this.connectedVehicleID);
		buf.writeInt(this.amountPresent);
		buf.writeInt(this.amountTransferred);
	}

	public static class Handler implements IMessageHandler<PacketFuelPumpConnection, IMessage>{
		public IMessage onMessage(final PacketFuelPumpConnection message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityFuelPump pump = (TileEntityFuelPump) getTileEntity(message, ctx);
					if(pump != null){
						if(message.connectedVehicleID != -1){
							pump.setConnectedVehicle((EntityVehicleE_Powered) Minecraft.getMinecraft().world.getEntityByID(message.connectedVehicleID));
						}else{
							pump.setConnectedVehicle(null);
						}
						if(pump.getInfo().fluid != null){
							pump.getInfo().fluid.amount = message.amountPresent;
							if(message.amountPresent == 0){
								pump.clearFluid();
							}
						}
						pump.totalTransfered = message.amountTransferred;
					}
				}
			});
			return null;
		}
	}	
}
