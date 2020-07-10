package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperEntityPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityFluidTank;
import minecrafttransportsimulator.packets.components.APacketTileEntity;

/**Packet sent to fluid tank TEs on clients to update the fluid they have in their tank.
 * 
 * @author don_bruce
 */
public class PacketTileEntityFluidTankChange extends APacketTileEntity<ATileEntityFluidTank<?>>{
	private final String fluidName;
	private final int fluidDelta;
	
	public PacketTileEntityFluidTankChange(ATileEntityFluidTank<?> tank, int fluidDelta){
		super(tank);
		this.fluidName = tank.getFluid();
		this.fluidDelta = fluidDelta;
	}
	
	public PacketTileEntityFluidTankChange(ByteBuf buf){
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
	protected boolean handle(WrapperWorld world, WrapperEntityPlayer player, ATileEntityFluidTank<?> tank){
		if(fluidDelta < 0){
			tank.drain(fluidName, -fluidDelta, true);
		}else{
			tank.fill(fluidName, fluidDelta, true);
		}
		return true;
	}
}
