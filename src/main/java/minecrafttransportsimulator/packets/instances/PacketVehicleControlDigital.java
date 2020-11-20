package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
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
			case BRAKE : vehicle.brakeOn = controlState; break;
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
					vehicle.towedVehicle.towedByVehicle = null;
					vehicle.towedVehicle.parkingBrakeOn = true;
					vehicle.towedVehicle = null;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleTrailerChange(vehicle));
					player.sendPacket(new PacketPlayerChatMessage("interact.trailer.disconnect"));
				}else if(vehicle.definition.motorized.hitchPos != null){
					for(AEntityBase entity : AEntityBase.createdServerEntities){
						if(!entity.equals(vehicle) && entity instanceof EntityVehicleF_Physics){
							EntityVehicleF_Physics testVehicle = (EntityVehicleF_Physics) entity;
							if(testVehicle.definition.motorized.hookupPos != null){
								Point3d hitchPos = vehicle.definition.motorized.hitchPos.copy().rotateCoarse(vehicle.angles).add(vehicle.position);
								Point3d hookupPos = testVehicle.definition.motorized.hookupPos.copy().rotateCoarse(testVehicle.angles).add(testVehicle.position);
								if(hitchPos.distanceTo(hookupPos) < 2){
									for(String hitchType : vehicle.definition.motorized.hitchTypes){
										if(hitchType.equals(testVehicle.definition.motorized.hookupType)){
											testVehicle.towedByVehicle = vehicle;
											vehicle.towedVehicle = testVehicle;
											vehicle.towedVehicle.parkingBrakeOn = false;
											MasterLoader.networkInterface.sendToAllClients(new PacketVehicleTrailerChange(vehicle));
											player.sendPacket(new PacketPlayerChatMessage("interact.trailer.connect"));
											return false;
										}
									}
									player.sendPacket(new PacketPlayerChatMessage("interact.trailer.wronghitch"));
									break;
								}
							}
						}
					}
					player.sendPacket(new PacketPlayerChatMessage("interact.trailer.notfound"));
				}else{
					player.sendPacket(new PacketPlayerChatMessage("interact.trailer.nohitch"));
				}
				return false;
			}
			case SHIFT_UP : {
				for(PartEngine engine : vehicle.engines.values()){
					engine.shiftUp(controlState);
				}
				//Return here as we may not have shifted.
				return false;
			}
			case SHIFT_DN : {
				for(PartEngine engine : vehicle.engines.values()){
					engine.shiftDown(controlState);
				}
				//Return here as we may not have shifted.
				return false;
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
			case TRIM_ROLL : vehicle.aileronTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_AILERON_TRIM, EntityVehicleF_Physics.MAX_AILERON_TRIM, vehicle.aileronTrim + (controlState ? 1 : -1)); break;
			case TRIM_PITCH : vehicle.elevatorTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, vehicle.elevatorTrim + (controlState ? 1 : -1)); break;
			case TRIM_YAW : vehicle.rudderTrim = (short) clampAngle(-EntityVehicleF_Physics.MAX_RUDDER_TRIM, EntityVehicleF_Physics.MAX_RUDDER_TRIM, vehicle.rudderTrim + (controlState ? 1 : -1)); break;
			case CUSTOM_0 : if(controlState){vehicle.customsOn.add((byte)0);}else{vehicle.customsOn.remove((byte)0);}; break;
			case CUSTOM_1 : if(controlState){vehicle.customsOn.add((byte)1);}else{vehicle.customsOn.remove((byte)1);}; break;
			case CUSTOM_2 : if(controlState){vehicle.customsOn.add((byte)2);}else{vehicle.customsOn.remove((byte)2);}; break;
			case CUSTOM_3 : if(controlState){vehicle.customsOn.add((byte)3);}else{vehicle.customsOn.remove((byte)3);}; break;
		}
		return true;
	}
	
	public enum Controls{
		BRAKE,
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
		TRIM_YAW,
		CUSTOM_0,
		CUSTOM_1,
		CUSTOM_2,
		CUSTOM_3;
	}
}
