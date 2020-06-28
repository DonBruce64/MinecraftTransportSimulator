package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.entity.Entity;

/**Packet used for controlling vehicles.  Responsible for handing singular button presses.
 * 
 * @author don_bruce
 */
public class PacketVehicleControlDigital extends APacketVehicle{
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
	protected boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		switch(controlType){
			case BRAKE : vehicle.brakeOn = controlState; break;
			case P_BRAKE : {
				//If we are a big truck on a client that just set the brake, play the brake sound.
				if(world.isClient() && !vehicle.parkingBrakeOn && controlState && vehicle.definition.car != null && vehicle.definition.car.isBigTruck){
					WrapperAudio.playQuickSound(new SoundInstance(vehicle, MTS.MODID + ":air_brake_activating"));
				}
				vehicle.parkingBrakeOn = controlState;
				break;
			}
			case HORN : {
				if(world.isClient() && !vehicle.hornOn && controlState){
					WrapperAudio.playQuickSound(new SoundInstance(vehicle, vehicle.definition.motorized.hornSound, true));
				}
				vehicle.hornOn = controlState;
				break;
			}
			case SIREN : {
				if(world.isClient() && !vehicle.sirenOn && controlState){
					WrapperAudio.playQuickSound(new SoundInstance(vehicle, vehicle.definition.motorized.sirenSound, true));
				}
				vehicle.sirenOn = controlState;
				break;
			}
			case TRAILER : {
				if(vehicle.towedVehicle != null){
					vehicle.towedVehicle.towedByVehicle = null;
					vehicle.towedVehicle = null;
					if(!world.isClient()){
						player.sendPacket(new PacketPlayerChatMessage("interact.trailer.disconnect"));
						return true;
					}
				}else if(vehicle.definition.motorized.hitchPos != null){
					for(Entity entity : vehicle.world.loadedEntityList){
						if(entity instanceof EntityVehicleF_Physics){
							EntityVehicleF_Physics testVehicle = (EntityVehicleF_Physics) entity;
							if(testVehicle.definition.motorized.hookupPos != null){
								//Make sure clients hitch vehicles that the server sees.  Little more lenient here.
								Point3d hitchOffset = new Point3d(vehicle.definition.motorized.hitchPos[0], vehicle.definition.motorized.hitchPos[1], vehicle.definition.motorized.hitchPos[2]);
								Point3d hitchPos = RotationSystem.getRotatedPoint(hitchOffset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.positionVector);
								Point3d hookupOffset = new Point3d(testVehicle.definition.motorized.hookupPos[0], testVehicle.definition.motorized.hookupPos[1], testVehicle.definition.motorized.hookupPos[2]);
								Point3d hookupPos = RotationSystem.getRotatedPoint(hookupOffset, testVehicle.rotationPitch, testVehicle.rotationYaw, testVehicle.rotationRoll).add(testVehicle.positionVector);
								if(hitchPos.distanceTo(hookupPos) < (world.isClient() ? 3 : 2)){
									for(String hitchType : vehicle.definition.motorized.hitchTypes){
										if(hitchType.equals(testVehicle.definition.motorized.hookupType)){
											testVehicle.towedByVehicle = vehicle;
											vehicle.towedVehicle = testVehicle;
											if(!world.isClient()){
												player.sendPacket(new PacketPlayerChatMessage("interact.trailer.connect"));
											}
											return true;
										}
									}
									if(!world.isClient()){
										player.sendPacket(new PacketPlayerChatMessage("interact.trailer.wronghitch"));
									}
									break;
								}
							}
						}
					}
					player.sendPacket(new PacketPlayerChatMessage("interact.trailer.notfound"));
				}else{
					if(!world.isClient()){
						player.sendPacket(new PacketPlayerChatMessage("interact.trailer.nohitch"));
					}
				}
				return false;
			}
			case SHIFT : {
				for(PartEngine engine : vehicle.engines.values()){
					if(controlState){
						engine.shiftUp(true);
					}else{
						engine.shiftDown(true);
					}
				}
				break;
			}
			case REVERSE : {
				if(vehicle.definition.blimp != null){
					for(PartEngine engine : vehicle.engines.values()){
						if(controlState){
							engine.shiftDown(true);
							engine.shiftDown(true);
						}else{
							engine.shiftUp(true);
							engine.shiftUp(true);
						}
					}
				}else{
					vehicle.reverseThrust = controlState;
				}
				break;
			}
			case GEAR : vehicle.gearUpCommand = controlState; break;
			case AUTOPILOT : vehicle.autopilot = controlState; break;
			case CRUISECONTROL : {
				if(controlState){
					vehicle.cruiseControl = true;
					vehicle.cruiseControlSpeed = vehicle.velocity;
				}else{
					vehicle.cruiseControl = false;
				}
				break;
			}
			case FLAPS : vehicle.flapDesiredAngle = (short) clampAngle(0, EntityVehicleF_Physics.MAX_FLAP_ANGLE, vehicle.flapDesiredAngle + (controlState ? 50 : -50)); break;
			case TRIM_ROLL : vehicle.aileronTrim = (short) clampAngle(-100, 100, vehicle.aileronTrim + (controlState ? 1 : -1)); break;
			case TRIM_PITCH : vehicle.elevatorTrim = (short) clampAngle(-100, 100, vehicle.elevatorTrim + (controlState ? 1 : -1)); break;
			case TRIM_YAW : vehicle.rudderTrim = (short) clampAngle(-100, 100, vehicle.rudderTrim + (controlState ? 1 : -1)); break;
		}
		return true;
	}
	
	public enum Controls{
		BRAKE,
		P_BRAKE,
		HORN,
		SIREN,
		SHIFT,
		TRAILER,
		REVERSE,
		GEAR,
		AUTOPILOT,
		CRUISECONTROL,
		FLAPS,
		TRIM_ROLL,
		TRIM_PITCH,
		TRIM_YAW;
	}
}
