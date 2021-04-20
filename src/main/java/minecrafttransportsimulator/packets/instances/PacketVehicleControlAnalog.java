package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used for controlling vehicles.  Responsible for handing inputs for control points that
 * have a wide span of values, such as throttles and steering angles.  If cooldown is equal to the
 * max value of a byte then it is assumed that this packet is from a joystick and the value of the 
 * control should be set to whatever value is passed-in. If cooldown is anything else, then it is 
 * assumed the value is to be added-on to the current value as this packet is from a keyboard or 
 * button input that can only be fired once a tick.
 * 
 * @author don_bruce
 */
public class PacketVehicleControlAnalog extends APacketEntity<EntityVehicleF_Physics>{
	private final Controls controlType;
	private final short value;
	private final byte cooldown;
	
	public PacketVehicleControlAnalog(EntityVehicleF_Physics vehicle, Controls controlType, short value, byte cooldown){
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
	protected boolean handle(WrapperWorld world, EntityVehicleF_Physics vehicle){
		switch(controlType){
			case THROTTLE : {
				vehicle.throttle = (byte) clampAngle(0, EntityVehicleF_Physics.MAX_THROTTLE, cooldown == Byte.MAX_VALUE ? value : vehicle.throttle + value);
				break;
			}
			case BRAKE : {
				vehicle.brake = (byte) clampAngle(0, EntityVehicleF_Physics.MAX_BRAKE, cooldown == Byte.MAX_VALUE ? value : vehicle.brake + value);
				break;
			}
			case AILERON : {
				vehicle.aileronAngle = (short) clampAngle(-EntityVehicleF_Physics.MAX_AILERON_ANGLE, EntityVehicleF_Physics.MAX_AILERON_ANGLE, cooldown == Byte.MAX_VALUE ? value : vehicle.aileronAngle + value);
				vehicle.aileronCooldown = cooldown; 
				break;
			}
			case ELEVATOR : {
				vehicle.elevatorAngle = (short) clampAngle(-EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, cooldown == Byte.MAX_VALUE ? value : vehicle.elevatorAngle + value);
				vehicle.elevatorCooldown = cooldown;
				break;
			}
			case RUDDER : {
				vehicle.rudderAngle = (short) clampAngle(-EntityVehicleF_Physics.MAX_RUDDER_ANGLE, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, cooldown == Byte.MAX_VALUE ? value : vehicle.rudderAngle + value);
				vehicle.rudderCooldown = cooldown;
				break;
			}
		}
		return true;
	}
	
	public enum Controls{
		THROTTLE,
		BRAKE,
		AILERON,
		ELEVATOR,
		RUDDER;
	}
}
