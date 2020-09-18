package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
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
	protected boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		return handle(world, player, vehicle, offset);
	}
	
	/**
	 *  Handler method with an extra parameter for the offset of this part.
	 *  Supplements {@link #handle(WrapperWorld, WrapperPlayer, EntityVehicleF_Physics)}
	 */
	protected abstract boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset);
}
