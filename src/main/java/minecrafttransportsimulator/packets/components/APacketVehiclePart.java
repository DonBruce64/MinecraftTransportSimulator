package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet class that includes a default implementation for transmitting a locator
 * for a part on a vehicle.  Part may or may not exist; only the location is assured.
 *
 * @author don_bruce
 */
public abstract class APacketVehiclePart extends APacketVehicle{
	private final Point3d offset;
	
	public APacketVehiclePart(EntityVehicleF_Physics vehicle, Point3d offset){
		super(vehicle);
		this.offset = offset; 
	}
	
	public APacketVehiclePart(ByteBuf buf){
		super(buf);
		this.offset = readPoint3dFromBuffer(buf);
	};

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(offset, buf);
	}
	
	@Override
	protected boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle){
		return handle(world, player, vehicle, offset);
	}
	
	/**
	 *  Handler method with an extra parameter for the offset of this part.
	 *  Supplements {@link #handle(IWrapperWorld, IWrapperPlayer, EntityVehicleF_Physics)}
	 */
	protected abstract boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset);
}
