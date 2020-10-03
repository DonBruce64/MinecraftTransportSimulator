package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet class that includes a default implementation for transmitting a vehicle
 * to allow vehicle-specific interactions on the other side of the network.
 *
 * @author don_bruce
 */
public abstract class APacketVehicle extends APacketEntity{
	
	public APacketVehicle(EntityVehicleF_Physics vehicle){
		super(vehicle);
	}
	
	public APacketVehicle(ByteBuf buf){
		super(buf);
	};
	
	@Override
	protected boolean handle(IWrapperWorld world, IWrapperPlayer player, AEntityBase entity){
		return handle(world, player, (EntityVehicleF_Physics) entity);
	}
	
	/**
	 *  Helper method for handling clamped values.  Mainly comes from
	 *  control packets where we could go outside our desired bounds if we
	 *  don't check clamping.
	 */
	protected static int clampAngle(int min, int max, int value){
		return value < min ? min : (value > max ? max : value);
	}
	
	/**
	 *  Fall-down handler implementation of {@link #handle(IWrapperWorld, IWrapperPlayer, AEntityBase)}
	 */
	protected abstract boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle);
}
