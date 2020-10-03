package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Packet sent to fluid tanks on clients to update the fluid they have in their tank.
 * Uses the tank's ID for syncing operations.
 * 
 * @author don_bruce
 */
public class PacketFluidTankChange extends APacketBase{
	private final int tankID;
	private final String fluidName;
	private final double fluidDelta;
	
	public PacketFluidTankChange(FluidTank tank, double fluidDelta){
		super(null);
		this.tankID = tank.tankID;
		this.fluidName = tank.getFluid();
		this.fluidDelta = fluidDelta;
	}
	
	public PacketFluidTankChange(ByteBuf buf){
		super(buf);
		this.tankID = buf.readInt();
		this.fluidName = readStringFromBuffer(buf);
		this.fluidDelta = buf.readDouble();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(tankID);
		writeStringToBuffer(fluidName, buf);
		buf.writeDouble(fluidDelta);
	}
	
	@Override
	public void handle(IWrapperWorld world, IWrapperPlayer player){
		FluidTank tank = FluidTank.createdClientTanks.get(tankID);
		//Tank may be null if the client hasn't loaded this tank yet. 
		if(tank != null){
			if(fluidDelta < 0){
				tank.drain(fluidName, -fluidDelta, true);
			}else{
				tank.fill(fluidName, fluidDelta, true);
			}
		}
	}
}
