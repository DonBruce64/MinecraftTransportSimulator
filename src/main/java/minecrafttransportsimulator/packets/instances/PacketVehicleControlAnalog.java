package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;

/**Packet used for controlling vehicles.  Responsible for handing inputs for control points that
 * have a wide span of values, such as throttles and steering angles.  If cooldown is equal to the
 * max value of a byte then it is assumed that this packet is from a joystick and the value of the 
 * control should be set to whatever value is passed-in. If cooldown is anything else, then it is 
 * assumed the value is to be added-on to the current value as this packet is from a keyboard or 
 * button input that can only be fired once a tick.
 * 
 * @author don_bruce
 */
public class PacketVehicleControlAnalog extends APacketVehicle{
	private final Controls controlType;
	private final short value;
	private final byte cooldown;
	
	public PacketVehicleControlAnalog(EntityVehicleE_Powered vehicle, Controls controlType, short value, byte cooldown){
		super(vehicle);
		this.controlType = controlType;
		this.value = value;
		this.cooldown = cooldown;
	}
	
	public PacketVehicleControlAnalog(ByteBuf buf){
		super(buf);
		this.controlType = Controls.values()[buf.readByte()];
		this.value = buf.readShort();
		this.cooldown = buf.readByte();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(controlType.ordinal());
		buf.writeShort(value);
		buf.writeByte(cooldown);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleE_Powered vehicle){
		switch(controlType){
			case THROTTLE : {
				vehicle.throttle = (byte) clampAngle(0, 100, cooldown == Byte.MAX_VALUE ? value : vehicle.throttle + value);
				break;
			}	
			case STEERING : {
				EntityVehicleF_Ground powered = (EntityVehicleF_Ground) vehicle;
				powered.steeringAngle = (short) clampAngle(-powered.MAX_STEERING_ANGLE, powered.MAX_STEERING_ANGLE, cooldown == Byte.MAX_VALUE ? value : powered.steeringAngle + value);
				powered.steeringCooldown = cooldown; 
				break;
			}
			case AILERON : {
				EntityVehicleF_Air aircraft = (EntityVehicleF_Air) vehicle;
				aircraft.aileronAngle = (short) clampAngle(-aircraft.MAX_AILERON_ANGLE, aircraft.MAX_AILERON_ANGLE, cooldown == Byte.MAX_VALUE ? value : aircraft.aileronAngle + value);
				aircraft.aileronCooldown = cooldown; 
				break;
			}
			case ELEVATOR : {
				EntityVehicleF_Air aircraft = (EntityVehicleF_Air) vehicle;
				aircraft.elevatorAngle = (short) clampAngle(-aircraft.MAX_ELEVATOR_ANGLE, aircraft.MAX_ELEVATOR_ANGLE, cooldown == Byte.MAX_VALUE ? value : aircraft.elevatorAngle + value);
				aircraft.elevatorCooldown = cooldown;
				break;
			}
			case RUDDER : {
				EntityVehicleF_Air aircraft = (EntityVehicleF_Air) vehicle;
				aircraft.rudderAngle = (short) clampAngle(-aircraft.MAX_RUDDER_ANGLE, aircraft.MAX_RUDDER_ANGLE, cooldown == Byte.MAX_VALUE ? value : aircraft.rudderAngle + value);
				aircraft.rudderCooldown = cooldown;
				break;
			}
		}
		return true;
	}
	
	public enum Controls{
		THROTTLE,
		STEERING,
		AILERON,
		ELEVATOR,
		RUDDER;
	}
}
