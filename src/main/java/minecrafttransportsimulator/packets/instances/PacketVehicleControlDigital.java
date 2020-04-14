package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Plane;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.vehicles.parts.APartEngineGeared;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**Packet used for controlling vehicles.  Responsible for handing singular button presses.
 * 
 * @author don_bruce
 */
public class PacketVehicleControlDigital extends APacketVehicle{
	private final Controls controlType;
	private final boolean controlState;
	
	public PacketVehicleControlDigital(EntityVehicleE_Powered vehicle, Controls controlType, boolean controlState){
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
	protected boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleE_Powered vehicle){
		switch(controlType){
			case BRAKE : vehicle.brakeOn = controlState; break;
			case P_BRAKE : {
				//If we are a big truck on a client that just set the brake, play the brake sound.
				if(world.isClient() && !vehicle.parkingBrakeOn && controlState && vehicle instanceof EntityVehicleG_Car && vehicle.definition.car.isBigTruck){
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
				//TODO change this out when we make aircraft tow-able.
				EntityVehicleF_Ground mainVehicle = (EntityVehicleF_Ground) vehicle;
				if(mainVehicle.towedVehicle != null){
					mainVehicle.towedVehicle.towedByVehicle = null;
					mainVehicle.towedVehicle = null;
					if(!world.isClient()){
						player.sendPacket(new PacketPlayerChatMessage("interact.trailer.disconnect"));
						return true;
					}
				}else if(vehicle.definition.motorized.hitchPos != null){
					for(Entity entity : vehicle.world.loadedEntityList){
						if(entity instanceof EntityVehicleF_Ground){
							EntityVehicleF_Ground testVehicle = (EntityVehicleF_Ground) entity;
							if(testVehicle.definition.motorized.hookupPos != null){
								//Make sure clients hitch vehicles that the server sees.  Little more lenient here.
								Vec3d hitchOffset = new Vec3d(vehicle.definition.motorized.hitchPos[0], vehicle.definition.motorized.hitchPos[1], vehicle.definition.motorized.hitchPos[2]);
								Vec3d hitchPos = RotationSystem.getRotatedPoint(hitchOffset, vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.getPositionVector());
								Vec3d hookupOffset = new Vec3d(testVehicle.definition.motorized.hookupPos[0], testVehicle.definition.motorized.hookupPos[1], testVehicle.definition.motorized.hookupPos[2]);
								Vec3d hookupPos = RotationSystem.getRotatedPoint(hookupOffset, testVehicle.rotationPitch, testVehicle.rotationYaw, testVehicle.rotationRoll).add(testVehicle.getPositionVector());
								if(hitchPos.distanceTo(hookupPos) < (world.isClient() ? 3 : 2)){
									for(String hitchType : vehicle.definition.motorized.hitchTypes){
										if(hitchType.equals(testVehicle.definition.motorized.hookupType)){
											testVehicle.towedByVehicle = mainVehicle;
											mainVehicle.towedVehicle = testVehicle;
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
				for(APartEngine engine : vehicle.engines.values()){
					if(engine instanceof APartEngineGeared){
						if(controlState){
							((APartEngineGeared) engine).shiftUp(true);
						}else{
							((APartEngineGeared) engine).shiftDown(true);
						}
					}
				}
				break;
			}
			case REVERSE : vehicle.reverseThrust = controlState; break;
			case AUTOPILOT : ((EntityVehicleF_Air) vehicle).autopilot = controlState; break;
			case FLAPS : ((EntityVehicleG_Plane) vehicle).flapDesiredAngle = (short) clampAngle(0, 350, ((EntityVehicleG_Plane) vehicle).flapDesiredAngle + (controlState ? 50 : -50)); break;
			case TRIM_ROLL : ((EntityVehicleF_Air) vehicle).aileronTrim = (short) clampAngle(-100, 100, ((EntityVehicleF_Air) vehicle).aileronTrim + (controlState ? 1 : -1)); break;
			case TRIM_PITCH : ((EntityVehicleF_Air) vehicle).elevatorTrim = (short) clampAngle(-100, 100, ((EntityVehicleF_Air) vehicle).elevatorTrim + (controlState ? 1 : -1)); break;
			case TRIM_YAW : ((EntityVehicleF_Air) vehicle).rudderTrim = (short) clampAngle(-100, 100, ((EntityVehicleF_Air) vehicle).rudderTrim + (controlState ? 1 : -1)); break;
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
		AUTOPILOT,
		FLAPS,
		TRIM_ROLL,
		TRIM_PITCH,
		TRIM_YAW;
	}
}
