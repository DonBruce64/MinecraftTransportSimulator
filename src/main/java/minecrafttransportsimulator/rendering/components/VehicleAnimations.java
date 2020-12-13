package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

/**This class contains static methods for vehicle animations.  These are used to animate
 * the vehicle and its parts, as well as instruments.  All methods are designed to be as
 * global as possible to keep all animations in this class.
 *
 * @author don_bruce
 */
public final class VehicleAnimations{
	
	/**
	 *  Clamps and scales the passed-in variable value, returning it in the proper form.
	 */
	public static double clampAndScale(double value, double scaling, double offset, double clampMin, double clampMax, boolean absolute){
		value = scaling*(absolute ? Math.abs(value) : value) + offset;
		if(clampMin != 0 && value < clampMin){
			value = clampMin;
		}else if(clampMax != 0 && value > clampMax){
			value = clampMax;
		}
		return value;
	}
	
	/**
	 *  Returns the clamped value for the passed-in variable on the passed-in vehicle.  A part may or
	 *  may not be passed in to allow for part-specific animations (such as a specific engine's RPM).
	 *  If a clamp value other than 0 is passed-in, the variable returned will be clamped to that value.
	 */
	public static double getVariableValue(String variable, double scaling, double offset, double clampMin, double clampMax, boolean absolute, float partialTicks, EntityVehicleF_Physics vehicle, APart optionalPart){
		double value = getVariableValue(variable, partialTicks, vehicle, optionalPart);
		return clampAndScale(value, scaling, offset, clampMin, clampMax, absolute);
	}
	
	/**
	 *  Returns the part number for the passed-in variable, or -1 if there is no part number for the variable.
	 *  Part number is 0-indexed to conform to the list indexes internal to vehicle parts.
	 */
	public static int getPartNumber(String variable){
		if(variable.substring(variable.length() - 1).matches("[0-9]+")){
			return Integer.parseInt(variable.substring(variable.lastIndexOf('_') + 1)) - 1;
		}else{
			return -1;
		}
	}
	
	/**
	 *  Returns the raw value for the passed-in variable on the passed-in vehicle.  A part may or
	 *  may not be passed in to allow for part-specific animations (such as a specific engine's RPM).
	 *  No clamping or other operations are performed on this value, so keep this in mind.
	 */
	public static double getVariableValue(String variable, float partialTicks, EntityVehicleF_Physics vehicle, APart optionalPart){
		//If we have a variable with a suffix, we need to get that part first and pass
		//it into this method rather than trying to run through the code now.
		int partNumber = getPartNumber(variable);
		if(partNumber != -1){
			//Get the part type from the variable.
			String partType = variable.substring(0, variable.indexOf('_'));
			final Class<?> partClass;
			switch(partType){
				case("interactable"): partClass = PartInteractable.class; break;	
				case("engine"): partClass = PartEngine.class; break;
				case("gun"): partClass = PartGun.class; break;
				case("propeller"): partClass = PartPropeller.class; break;
				case("ground"): partClass = PartGroundDevice.class; break;
				case("seat"): partClass = PartSeat.class; break;
				
				default: if(ConfigSystem.configObject.clientControls.devMode.value){
					throw new IllegalArgumentException("ERROR: Was told to find part: " + variable.substring(0, variable.indexOf('_')) + " for rotation definition: " + variable + " but could not as the part isn't a valid part name.  Is your spelling correct?");
				}else{
					//Don't crash if we have a fault here.  It could be that we have an old pack that has a bad name.
					return 0;
				}
			}
			
			//Iterate through the pack defs to find the index of the pack def for the part we want.
			for(VehiclePart vehiclePart : vehicle.getAllPossiblePackParts().values()){
				for(String defPartType : vehiclePart.types){
					if(defPartType.startsWith(partType)){
						if(partNumber == 0){
							//Get the part at this location.  If it's of the same class as what we need, use it for animation.
							//If it's not, or it doesn't exist, return 0.
							APart foundPart = vehicle.getPartAtLocation(vehiclePart.pos);
							if(foundPart != null && partClass.isInstance(foundPart)){
								return getVariableValue(variable.substring(0, variable.length() - 2), partialTicks, vehicle, foundPart);
							}else{
								return 0;
							}
						}else{
							--partNumber;
							break;
						}
					}
				}
			}
			
			//We couldn't find the part we were supposed to.  Likely because it hasn't been placed yet.
			return 0;
		}else if(optionalPart != null){
			//If we passed-in a part, check for part-specific animations first.
			if(optionalPart instanceof PartEngine){
				PartEngine engine = (PartEngine) optionalPart;
				switch(variable){
					case("engine_present"): return 1;
					case("engine_isautomatic"): return engine.definition.engine.isAutomatic ? 1 : 0;	
					case("engine_rotation"): return engine.getEngineRotation(partialTicks);
					case("engine_driveshaft_rotation"): return engine.getDriveshaftRotation(partialTicks);
					case("engine_driveshaft_sin"): return Math.sin(Math.toRadians(engine.getDriveshaftRotation(partialTicks)));
					case("engine_driveshaft_cos"): return Math.cos(Math.toRadians(engine.getDriveshaftRotation(partialTicks)));
					case("engine_rpm"): return engine.definition.engine.maxRPM < 15000 ? engine.rpm : engine.rpm/10D;
					case("engine_rpm_safe"): return engine.definition.engine.maxRPM < 15000 ? PartEngine.getSafeRPMFromMax(engine.definition.engine.maxRPM) : PartEngine.getSafeRPMFromMax(engine.definition.engine.maxRPM)/10D;
					case("engine_rpm_max"): return engine.definition.engine.maxRPM < 15000 ? engine.definition.engine.maxRPM : engine.definition.engine.maxRPM/10D;
					case("engine_rpm_percent"): return engine.rpm/engine.definition.engine.maxRPM;
					case("engine_rpm_percent_safe"): return engine.rpm/PartEngine.getSafeRPMFromMax(engine.definition.engine.maxRPM);
					case("engine_fuel_flow"): return engine.fuelFlow*20D*60D/1000D;
					case("engine_temp"): return engine.temp;
					case("engine_pressure"): return engine.pressure;
					case("engine_gear"): return engine.currentGear;
					case("engine_gearshift"): return engine.getGearshiftRotation();
					case("engine_gearshift_hvertical"): return engine.getGearshiftPosition_Vertical();
					case("engine_gearshift_hhorizontal"): return engine.getGearshiftPosition_Horizontal();
					case("engine_magneto"): return engine.state.magnetoOn ? 1 : 0;
					case("engine_starter"): return engine.state.esOn ? 1 : 0;
					case("engine_jumper_cable"): return engine.linkedEngine != null ? 1 : 0;
					case("engine_hours"): return engine.hours;
				}
			}else if(optionalPart instanceof PartGun){
				PartGun gun = (PartGun) optionalPart;
				//Check for an instance of a gun_muzzle_# variable, since these requires additional parsing
				if (variable.startsWith("gun_muzzle_")){
					//Get the rest of the variable after gun_muzzle_
					String muzzleVariable = variable.substring("gun_muzzle_".length());
					//Parse one or more digits, then take off one because we are zero-indexed
					int muzzleNumber = Integer.parseInt(muzzleVariable.substring(0, muzzleVariable.indexOf('_'))) - 1;
					switch(muzzleVariable.substring(muzzleVariable.indexOf('_') + 1)) {
						case("firing"): return (muzzleNumber == gun.currentMuzzle ? 1 : 0) * gun.cooldownTimeRemaining/(double)gun.definition.gun.fireDelay;
					}
				}
				switch(variable){
					case("gun_active"): return gun.active ? 1 : 0;
					case("gun_firing"): return gun.firing ? 1 : 0;
					case("gun_pitch"): return gun.prevOrientation.x + (gun.currentOrientation.x - gun.prevOrientation.x)*partialTicks;
					case("gun_yaw"): return gun.prevOrientation.y + (gun.currentOrientation.y - gun.prevOrientation.y)*partialTicks;
					case("gun_cooldown"): return gun.cooldownTimeRemaining/(double)gun.definition.gun.fireDelay;
					case("gun_windup_time"): return gun.windupTimeCurrent;
					case("gun_windup_rotation"): return gun.windupRotation;
					case("gun_windup_complete"): return gun.windupTimeCurrent == gun.definition.gun.windupTime ? 1 : 0;
					case("gun_reload"): return gun.reloadTimeRemaining/(double)gun.definition.gun.reloadTime;
					case("gun_ammo_count"): return gun.bulletsLeft;
					case("gun_ammo_percent"): return gun.bulletsLeft/gun.definition.gun.capacity;
				}
			}else if(optionalPart instanceof PartInteractable){
				PartInteractable interactable = (PartInteractable) optionalPart;
				switch(variable){
					case("interactable_count"): return interactable.getInventoryCount();
					case("interactable_percent"): return interactable.getInventoryPercent();
					case("interactable_capacity"): return interactable.getInventoryCapacity();
				}
			}else if(optionalPart instanceof PartPropeller){
				PartPropeller propeller = (PartPropeller) optionalPart;
				switch(variable){
					case("propeller_pitch_deg"): return Math.toDegrees(Math.atan(propeller.currentPitch / (propeller.definition.propeller.diameter*0.75D*Math.PI)));
					case("propeller_pitch_in"): return propeller.currentPitch;
					case("propeller_pitch_percent"): return 1D*(propeller.currentPitch - PartPropeller.MIN_DYNAMIC_PITCH)/(propeller.definition.propeller.pitch - PartPropeller.MIN_DYNAMIC_PITCH);
				}
			}else if(optionalPart instanceof PartGroundDevice){
				PartGroundDevice groundDevice = (PartGroundDevice) optionalPart;
				switch(variable){
					case("ground_rotation"): return groundDevice.getActionRotation(partialTicks).x;
					case("ground_onground"): return vehicle.groundDeviceCollective.isDeviceOnGround(groundDevice) ? 1 : 0;
					case("ground_inliquid"): return groundDevice.isInLiquid() ? 1 : 0;
					case("ground_isflat"): return groundDevice.getFlatState() ? 1 : 0;
				}
			}else if(optionalPart instanceof PartSeat){
				PartSeat seat = (PartSeat) optionalPart;
				IWrapperEntity riderForSeat = vehicle.locationRiderMap.get(seat.placementOffset);
				boolean riderPresent = riderForSeat != null && riderForSeat.isValid();
				switch(variable){
					case("seat_occupied"): return riderPresent ? 1 : 0;
					case("seat_rider_yaw"): {
						if(riderPresent){
							double riderYaw = riderForSeat.getHeadYaw() - vehicle.angles.y;
							while(riderYaw < -180) riderYaw += 360;
							while(riderYaw > 180) riderYaw -= 360;
							return riderYaw;
						}else{
							return 0;
						}
					}
					case("seat_rider_pitch"): {
						if(riderPresent) {
							double pitch = vehicle.angles.x;
			            	double roll = vehicle.angles.z;
			            	double riderYaw = riderForSeat.getHeadYaw() - vehicle.angles.y;
			            	while(pitch > 180){pitch -= 360;}
			    			while(pitch < -180){pitch += 360;}
			    			while(roll > 180){roll -= 360;}
			    			while(roll < -180){roll += 360;}
	
			            	double rollRollComponent = -Math.sin(Math.toRadians(riderYaw))*roll;
			            	double pitchRollComponent = Math.cos(Math.toRadians(riderYaw))*pitch;
			            	return riderForSeat.getPitch() - (rollRollComponent + pitchRollComponent);
		            	}
						else {
							return 0;
						}
					}
				}
			}
			
			//We didn't find any part-specific animations.
			//We could, however, be wanting the animations of our parent part.
			//If we have a parent part, get it, and try this loop again.
			if(optionalPart.parentPart != null){
				return getVariableValue(variable, partialTicks, vehicle, optionalPart.parentPart);
			}
		}

		//Either we don't have a part, or we have a part and we don't want a part-specific variable.
		//Try vehicle variables now.
		switch(variable){
			//Vehicle world state cases.
			case("time"): return vehicle.world.getTime();
			case("yaw"): return vehicle.angles.y;
			case("heading"): int heading = (int)-vehicle.angles.y; if(ConfigSystem.configObject.clientControls.north360.value) heading += 180; while (heading < 1) heading += 360; while (heading > 360) heading -= 360; return heading;
			case("pitch"): return vehicle.angles.x;
			case("roll"): return vehicle.angles.z;
			case("altitude"): return vehicle.position.y;
			case("speed"): return vehicle.axialVelocity*vehicle.SPEED_FACTOR*20;

			//Vehicle state cases.
			case("throttle"): return vehicle.throttle/(double)EntityVehicleF_Physics.MAX_THROTTLE;
			case("brake"): return vehicle.brake/(double)EntityVehicleF_Physics.MAX_BRAKE;
			case("fuel"): return vehicle.fuelTank.getFluidLevel()/vehicle.fuelTank.getMaxLevel();
			case("electric_power"): return vehicle.electricPower;
			case("electric_usage"): return vehicle.electricFlow*20D;
			case("p_brake"): return vehicle.parkingBrakeOn ? 1 : 0;
			case("reverser"): return vehicle.reverseThrust ? 1 : 0;
			case("horn"): return vehicle.hornOn ? 1 : 0;
			case("siren"): return vehicle.sirenOn ? 1 : 0;
			case("locked"): return vehicle.locked ? 1 : 0;
			case("rain"): return vehicle.electricFlow < 0 ? (1.0 - Math.cos(((int)vehicle.world.getRainStrength(new Point3i(vehicle.position)))*Math.toRadians(360*System.currentTimeMillis()/1000)))/2D : 0;
			case("door"): return vehicle.parkingBrakeOn && vehicle.velocity < 0.25 ? 1 : 0;
			case("trailer"): return vehicle.towedVehicle != null ? 1 : 0;
			case("trailer_pitch"): return vehicle.towedVehicle != null ? vehicle.towedVehicle.angles.x - vehicle.angles.x : 0;
			case("trailer_yaw"): return vehicle.towedVehicle != null ?  vehicle.towedVehicle.angles.y - vehicle.angles.y : 0;
			case("trailer_roll"): return vehicle.towedVehicle != null ? vehicle.towedVehicle.angles.z - vehicle.angles.z : 0;
			case("hookup"): return vehicle.towedByVehicle != null ? 1 : 0;
			case("hookup_pitch"): return vehicle.towedByVehicle != null ? vehicle.towedByVehicle.angles.x - vehicle.angles.x : 0;
			case("hookup_yaw"): return vehicle.towedByVehicle != null ? vehicle.towedByVehicle.angles.y - vehicle.angles.y : 0;
			case("hookup_roll"): return vehicle.towedByVehicle != null ? vehicle.towedByVehicle.angles.z - vehicle.angles.z : 0;
			case("fueling"): return vehicle.beingFueled ? 1 : 0;
			
			//State cases generally used on aircraft.
			case("aileron"): return vehicle.aileronAngle/10D;
			case("elevator"): return vehicle.elevatorAngle/10D;
			case("rudder"): return vehicle.rudderAngle/10D;
			case("flaps_setpoint"): return vehicle.flapDesiredAngle/10D;
			case("flaps_actual"): return vehicle.flapCurrentAngle/10D;
			case("trim_aileron"): return vehicle.aileronTrim/10D;
			case("trim_elevator"): return vehicle.elevatorTrim/10D;
			case("trim_rudder"): return vehicle.rudderTrim/10D;
			case("vertical_speed"): return vehicle.motion.y*vehicle.SPEED_FACTOR*20;
			case("lift_reserve"): return -vehicle.trackAngle;
			case("turn_coordinator"): return ((vehicle.angles.z - vehicle.prevAngles.z)/10 + vehicle.angles.y - vehicle.prevAngles.y)/0.15D*25;
			case("turn_indicator"): return (vehicle.angles.y - vehicle.prevAngles.y)/0.15F*25F;
			case("slip"): return 75*vehicle.sideVector.dotProduct(vehicle.normalizedVelocityVector);
			case("gear_setpoint"): return vehicle.gearUpCommand ? 1 : 0;
			case("gear_actual"): return vehicle.gearMovementTime/((double) vehicle.definition.motorized.gearSequenceDuration);
			case("beacon_direction"): return vehicle.selectedBeacon != null ? vehicle.angles.getClampedYDelta(Math.toDegrees(Math.atan2(vehicle.selectedBeacon.location.x - vehicle.position.x, vehicle.selectedBeacon.location.z - vehicle.position.z))) : 0;
			case("beacon_bearing_setpoint"): return vehicle.selectedBeacon != null ? vehicle.selectedBeacon.bearing : 0;
			case("beacon_bearing_delta"): return vehicle.selectedBeacon != null ? vehicle.selectedBeacon.getBearingDelta(vehicle) : 0;
			case("beacon_glideslope_setpoint"): return vehicle.selectedBeacon != null ? vehicle.selectedBeacon.glideSlope : 0;
			case("beacon_glideslope_actual"): return vehicle.selectedBeacon != null ? Math.toDegrees(Math.asin((vehicle.position.y - vehicle.selectedBeacon.location.y)/vehicle.position.distanceTo(vehicle.selectedBeacon.location))) : 0;
			case("beacon_glideslope_delta"): return vehicle.selectedBeacon != null ? vehicle.selectedBeacon.glideSlope - Math.toDegrees(Math.asin((vehicle.position.y - vehicle.selectedBeacon.location.y)/vehicle.position.distanceTo(vehicle.selectedBeacon.location))) : 0;
			
			//Missile incoming variables.
			//Variable is in the form of missile_X_variablename.
			default: {
				if(variable.startsWith("missile_")){
					String missileVariable = variable.substring(variable.lastIndexOf("_") + 1);
					int missileNumber = getPartNumber(variable.substring(0, variable.lastIndexOf('_')));
					if(missileNumber != -1){
						if(vehicle.missilesIncoming.size() <= missileNumber){
							return 0;
						}else{
							switch(missileVariable){
								case("distance"): return (double)vehicle.missilesIncoming.keySet().toArray()[missileNumber];
								case("direction"): {
									double dist = (double)vehicle.missilesIncoming.keySet().toArray()[missileNumber];
									Point3d missilePos = vehicle.missilesIncoming.get(dist).position;
									return Math.toDegrees(Math.atan2(-missilePos.z + vehicle.position.z, -missilePos.x + vehicle.position.x)) + 90 + vehicle.angles.y;
								}
							}
						}
					}else if(missileVariable.equals("incoming")){
						return vehicle.missilesIncoming.isEmpty() ? 0 : 1;
					}
				}
			}
		}
		
		//Check if this is a light variable.
		for(LightType light : LightType.values()){
			if(light.name().toLowerCase().equals(variable)){
				return vehicle.lightsOn.contains(light) ? 1 : 0;
			}
		}
		
		//Check if this is a door variable.
		if(vehicle.doorsOpen.contains(variable)){
			return 1;
		}
		
		//Check if this is a custom variable.
		if(vehicle.customsOn.contains(variable)){
			return 1;
		}
		
		//No variable found for anything.  We could have an error, but likely we have an older pack,
		//a closed door, a missing part, a custom variable that's not on, or something else entirely.
		//Return 0 here as we don't want to animate.
		return 0;
	}
}
