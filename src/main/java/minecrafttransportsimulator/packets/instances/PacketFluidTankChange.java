package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Packet sent to fluid tanks on clients to update the fluid they have in their tank.
 * Uses the tank's ID for syncing operations.
 * 
 * @author don_bruce
 */
public class PacketFluidTankChange extends APacketBase{
	private final int tankID;
	private final String fluidName;
	private final int fluidDelta;
	
	public PacketFluidTankChange(FluidTank tank, int fluidDelta){
		super(null);
		this.tankID = tank.tankID;
		this.fluidName = tank.getFluid();
		this.fluidDelta = fluidDelta;
	}
	
	public PacketFluidTankChange(ByteBuf buf){
		super(buf);
		this.tankID = buf.readInt();
		this.fluidName = readStringFromBuffer(buf);
		this.fluidDelta = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(tankID);
		writeStringToBuffer(fluidName, buf);
		buf.writeInt(fluidDelta);
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		FluidTank tank = FluidTank.createdTanks.get(tankID);
		if(fluidDelta < 0){
			tank.drain(fluidName, -fluidDelta, true);
		}else{
			tank.fill(fluidName, fluidDelta, true);
		}
	}
}
