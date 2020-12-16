package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleConnection;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartEngine;

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
	protected boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle){
		switch(controlType){
			case P_BRAKE : {
				//If we are a big truck on a client that just set the brake, play the brake sound.
				if(world.isClient() && !vehicle.parkingBrakeOn && controlState && vehicle.definition.motorized.isBigTruck){
					MasterLoader.audioInterface.playQuickSound(new SoundInstance(vehicle, MasterLoader.resourceDomain + ":air_brake_activating"));
				}
				vehicle.parkingBrakeOn = controlState;
				break;
			}
			case HORN : {
				if(world.isClient() && !vehicle.hornOn && controlState){
					MasterLoader.audioInterface.playQuickSound(new SoundInstance(vehicle, vehicle.definition.motorized.hornSound, true));
				}
				vehicle.hornOn = controlState;
				break;
			}
			case SIREN : {
				if(world.isClient() && !vehicle.sirenOn && controlState){
					MasterLoader.audioInterface.playQuickSound(new SoundInstance(vehicle, vehicle.definition.motorized.sirenSound, true));
				}
				vehicle.sirenOn = controlState;
				break;
			}
			case TRAILER : {
				if(vehicle.towedVehicle != null){
					vehicle.changeTrailer(null, 0, 0);
					player.sendPacket(new PacketPlayerChatMessage("interact.trailer.disconnect"));
				}else{
					boolean trailerInRange = false;
					boolean matchingConnection = false;
					for(AEntityBase entity : AEntityBase.createdServerEntities){
						if(!entity.equals(vehicle) && entity instanceof EntityVehicleF_Physics){
							EntityVehicleF_Physics testVehicle = (EntityVehicleF_Physics) entity;
							if(testVehicle.definition.motorized.hookups != null){
								//Vehicle has hookups.  See if any of them match our hitches.
								for(VehicleConnection hitch : vehicle.definition.motorized.hitches){
									for(VehicleConnection hookup : testVehicle.definition.motorized.hookups){
										Point3d hitchPos = hitch.pos.copy().rotateCoarse(vehicle.angles).add(vehicle.position);
										Point3d hookupPos = hookup.pos.copy().rotateCoarse(testVehicle.angles).add(testVehicle.position);
										if(hitchPos.distanceTo(hookupPos) < 10){
											//Potential connection.
											boolean validDistance = hitchPos.distanceTo(hookupPos) < 2;
											boolean validType = hitch.type.equals(hookup.type);
											if(validDistance && validType){
												vehicle.changeTrailer(testVehicle, vehicle.definition.motorized.hitches.indexOf(hitch), testVehicle.definition.motorized.hookups.indexOf(hookup));
												player.sendPacket(new PacketPlayerChatMessage("interact.trailer.connect"));
												return false;
											}else if(validDistance){
												trailerInRange = true;
											}else if(validType){
												matchingConnection = true;
											}
										}
									}
								}
							}
						}
					}
				
					//Send packet based on what we found.
					if(!trailerInRange && !matchingConnection){
						player.sendPacket(new PacketPlayerChatMessage("interact.trailer.notfound"));
					}else if(!trailerInRange && matchingConnection){
						player.sendPacket(new PacketPlayerChatMessage("interact.trailer.toofar"));
					}else if(trailerInRange && !matchingConnection){
						player.sendPacket(new PacketPlayerChatMessage("interact.trailer.wronghitch"));
					}else{
						player.sendPacket(new PacketPlayerChatMessage("interact.trailer.nohitch"));
					}
				}
				return false;
			}
			case SHIFT_UP : {
				boolean didShift = false;
				for(PartEngine engine : vehicle.engines.values()){
					didShift = engine.shiftUp(controlState);
				}
				return didShift;
			}
			case SHIFT_DN : {
				boolean didShift = false;
				for(PartEngine engine : vehicle.engines.values()){
					didShift = engine.shiftDown(controlState);
				}
				return didShift;
			}
			case REVERSE : {
				if(vehicle.definition.general.isBlimp){
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
			case GEAR : vehicle.gearUpCommand = controlState; break;
			case AUTOPILOT : {
				if(controlState){
					vehicle.autopilot = true;
					vehicle.altitudeSetting = vehicle.position.y;
				}else{
					vehicle.autopilot = false;
				}
				break;
			}
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
			case TRIM_ROLL : vehicle.aileronTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_AILERON_TRIM, EntityVehicleF_Physics.MAX_AILERON_TRIM, vehicle.aileronTrim + (controlState ? 1 : -1)); break;
			case TRIM_PITCH : vehicle.elevatorTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, vehicle.elevatorTrim + (controlState ? 1 : -1)); break;
			case TRIM_YAW : vehicle.rudderTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_RUDDER_TRIM, EntityVehicleF_Physics.MAX_RUDDER_TRIM, vehicle.rudderTrim + (controlState ? 1 : -1)); break;
		}
		return true;
	}
	
	public enum Controls{
		P_BRAKE,
		HORN,
		SIREN,
		SHIFT_UP,
		SHIFT_DN,
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
