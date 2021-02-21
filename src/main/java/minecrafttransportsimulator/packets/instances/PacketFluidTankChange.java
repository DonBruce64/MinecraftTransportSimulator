package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet sent to fluid tanks on clients to update the fluid they have in their tank.
 * 
 * @author don_bruce
 */
public class PacketFluidTankChange extends APacketEntity<FluidTank>{
	private final String fluidName;
	private final double fluidDelta;
	
	public PacketFluidTankChange(FluidTank tank, double fluidDelta){
		super(tank);
		this.fluidName = tank.getFluid();
		this.fluidDelta = fluidDelta;
	}
	
	public PacketFluidTankChange(ByteBuf buf){
		super(buf);
		this.fluidName = readStringFromBuffer(buf);
		this.fluidDelta = buf.readDouble();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(fluidName, buf);
		buf.writeDouble(fluidDelta);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, FluidTank tank){
		if(fluidDelta < 0){
			tank.drain(fluidName, -fluidDelta, true);
		}else{
			tank.fill(fluidName, fluidDelta, true);
		}
		return true;
	}
}
