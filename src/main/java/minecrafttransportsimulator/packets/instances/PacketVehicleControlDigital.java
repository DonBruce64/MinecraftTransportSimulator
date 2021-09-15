package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;

/**Packet used for controlling vehicles.  Responsible for handing singular button presses.
 * 
 * @author don_bruce
 */
public class PacketVehicleControlDigital extends APacketEntity<EntityVehicleF_Physics>{
	private final Controls controlType;
	private final boolean controlState;
	
	public PacketVehicleControlDigital(EntityVehicleF_Physics vehicle, Controls controlType, boolean controlState){
		super(vehicle);
		this.controlType = controlType;
		this.controlState = controlState;
	}
	
	public PacketVehicleControlDigital(ByteBuf buf){
		super(buf);
		this.controlType = Controls.values()[buf.readByte()];
		this.controlState = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(controlType.ordinal());
		buf.writeBoolean(controlState);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, EntityVehicleF_Physics vehicle){
		switch(controlType){
			case SHIFT_UP : {
				for(PartEngine engine : vehicle.engines.values()){
					if(engine.shiftUp(controlState)){
						InterfacePacket.sendToAllClients(new PacketPartEngine(engine, Signal.SHIFT_UP_MANUAL));
					}
				}
				return false;
			}
			case SHIFT_DN : {
				for(PartEngine engine : vehicle.engines.values()){
					if(engine.shiftDown(controlState)){
						InterfacePacket.sendToAllClients(new PacketPartEngine(engine, Signal.SHIFT_DN_MANUAL));
					}
				}
				return false;
			}
			case SHIFT_NEUTRAL : {
				for(PartEngine engine : vehicle.engines.values()){
					engine.currentGear = 0;
				}
				return true;
			}
			case REVERSE : {
				if(vehicle.definition.motorized.isBlimp){
					for(PartEngine engine : vehicle.engines.values()){
						if(controlState){
							engine.shiftDown(false);
							engine.shiftDown(false);
						}else{
							engine.shiftUp(false);
							engine.shiftUp(false);
						}
					}
				}else{
					vehicle.reverseThrust = controlState;
				}
				break;
			}
			case AUTOPILOT : {
				if(controlState){
					vehicle.autopilot = true;
					vehicle.altitudeSetting = vehicle.position.y;
					vehicle.speedSetting = vehicle.velocity;
				}else{
					vehicle.autopilot = false;
				}
				break;
			}
			case FLAPS : {
				if(controlState){
					if(vehicle.flapNotchSelected < vehicle.definition.motorized.flapNotches.size() - 1){
						++vehicle.flapNotchSelected;
						return true;
					}
				}else{
					if(vehicle.flapNotchSelected > 0){
						--vehicle.flapNotchSelected;
						return true;
					}
				}
				return false;
			}
			case TRIM_ROLL : vehicle.aileronTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_AILERON_TRIM, EntityVehicleF_Physics.MAX_AILERON_TRIM, vehicle.aileronTrim + (controlState ? 1 : -1)); break;
			case TRIM_PITCH : vehicle.elevatorTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, vehicle.elevatorTrim + (controlState ? 1 : -1)); break;
			case TRIM_YAW : vehicle.rudderTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_RUDDER_TRIM, EntityVehicleF_Physics.MAX_RUDDER_TRIM, vehicle.rudderTrim + (controlState ? 1 : -1)); break;
		}
		return true;
	}
	
	public enum Controls{
		SHIFT_UP,
		SHIFT_DN,
		SHIFT_NEUTRAL,
		REVERSE,
		AUTOPILOT,
		FLAPS,
		TRIM_ROLL,
		TRIM_PITCH,
		TRIM_YAW;
	}
}
