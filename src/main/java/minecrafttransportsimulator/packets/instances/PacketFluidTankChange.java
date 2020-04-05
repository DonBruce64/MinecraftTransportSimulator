package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityFluidTank;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Packet sent to fluid tank TEs on clients to update the fluid they have in their tank.
 * 
 * @author don_bruce
 */
public class PacketFluidTankChange extends APacketTileEntity<ATileEntityFluidTank>{
	private final String fluidName;
	private final int fluidDelta;
	
	public PacketFluidTankChange(ATileEntityFluidTank tank, int fluidDelta){
		super(tank);
		this.fluidName = tank.getFluid();
		this.fluidDelta = fluidDelta;
	}
	
	public PacketFluidTankChange(ByteBuf buf){
		super(buf);
		this.fluidName = readStringFromBuffer(buf);
		this.fluidDelta = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(fluidName, buf);
		buf.writeInt(fluidDelta);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, ATileEntityFluidTank tank){
		if(fluidDelta < 0){
			tank.drain(fluidName, -fluidDelta, true);
		}else{
			tank.fill(fluidName, fluidDelta, true);
		}
		return true;
	}
}
