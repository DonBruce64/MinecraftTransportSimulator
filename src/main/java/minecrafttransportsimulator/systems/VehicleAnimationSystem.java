package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;

/**This class contains static methods for vehicle animations.  These are used to animate
 * the vehicle and its parts, as well as instruments.  All methods are designed to be as
 * global as possible to keep all animations in this class.
 *
 * @author don_bruce
 */
public final class VehicleAnimationSystem{
	
	/**
	 *  Returns the clamped value for the passed-in variable on the passed-in vehicle.  A part may or
	 *  may not be passed in to allow for part-specific animations (such as a specific engine's RPM).
	 *  If a clamp value other than 0 is passed-in, the variable returned will be clamped to that value.
	 */
	public static double getVariableValue(String variable, double scaling, double offset, double minClamp, double maxClamp, boolean absolute, float partialTicks, EntityVehicleF_Physics vehicle, APart optionalPart){
		double value = getVariableValue(variable, partialTicks, vehicle, optionalPart);
		value = offset + scaling*(absolute ? Math.abs(value) : value);
		if(minClamp != 0 && value < minClamp){
			return minClamp;
		}else if(maxClamp != 0 && value > maxClamp){
			return maxClamp;
		}else{
			return value;
		}
	}
	
	/**
	 *  Returns the raw value for the passed-in variable on the passed-in vehicle.  A part may or
	 *  may not be passed in to allow for part-specific animations (such as a specific engine's RPM).
	 */
	public static double getVariableValue(String variable, float partialTicks, EntityVehicleF_Physics vehicle, APart optionalPart){
		//If we have a variable with a suffix, we need to get that part first and pass
		//it into this method rather than trying to run through the code now.
		if(variable.substring(variable.length() - 1).matches("[0-9]+")){
			//Get the part number and the type from the variable.
			//Take off one because we are zero-indexed.
			int partNumber = Integer.parseInt(variable.substring(variable.length() - 1)) - 1;
			String partType = variable.substring(0, variable.indexOf('_'));
			final Class<?> partClass;
			switch(partType){
				case("interactable"): partClass = PartInteractable.class; break;	
				case("engine"): partClass = PartEngine.class; break;
				case("gun"): partClass = PartGun.class; break;
				case("propeller"): partClass = PartPropeller.class; break;
				
				default: if(ConfigSystem.configObject.client.devMode.value){
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
					case("engine_rotation"): return engine.getEngineRotation(partialTicks);
					case("engine_driveshaft_rotation"): return engine.getDriveshaftRotation(partialTicks);
					case("engine_driveshaft_sin"): return Math.sin(Math.toRadians(engine.getDriveshaftRotation(partialTicks)));
					case("engine_driveshaft_cos"): return Math.cos(Math.toRadians(engine.getDriveshaftRotation(partialTicks)));
					case("engine_rpm"): return engine.definition.engine.maxRPM < 15000 ? engine.rpm : engine.rpm/10D;
					case("engine_rpm_safe"): return engine.definition.engine.maxRPM < 15000 ? PartEngine.getSafeRPMFromMax(engine.definition.engine.maxRPM) : PartEngine.getSafeRPMFromMax(engine.definition.engine.maxRPM)/10D;
					case("engine_rpm_max"): return engine.definition.engine.maxRPM < 15000 ? engine.definition.engine.maxRPM : engine.definition.engine.maxRPM/10D;
					case("engine_fuel_flow"): return engine.fuelFlow*20D*60D/1000D;
					case("engine_temp"): return engine.temp;
					case("engine_pressure"): return engine.pressure;
					case("engine_gear"): return engine.currentGear;
					case("engine_gearshift"): return engine.getGearshiftRotation();
					case("engine_gearshift_hvertical"): return engine.getGearshiftPosition_Vertical();
					case("engine_gearshift_hhorizontal"): return engine.getGearshiftPosition_Horizontal();
					case("engine_magneto"): return engine.state.magnetoOn ? 1 : 0;
					case("engine_starter"): return engine.state.esOn ? 1 : 0;
				}
			}else if(optionalPart instanceof PartGun){
				PartGun gun = (PartGun) optionalPart;
				switch(variable){
					case("gun_pitch"): return gun.prevOrientation.x + (gun.currentOrientation.x - gun.prevOrientation.x)*partialTicks;
					case("gun_yaw"): return gun.prevOrientation.y + (gun.currentOrientation.y - gun.prevOrientation.y)*partialTicks;
					case("gun_cooldown"): return gun.cooldownTimeRemaining/(double)gun.definition.gun.fireDelay;
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
			//Vehicle world position cases.	
			case("yaw"): return vehicle.angles.y;
			case("pitch"): return vehicle.angles.x;
			case("roll"): return vehicle.angles.z;
			case("altitude"): return vehicle.position.y;
			case("speed"): return vehicle.axialVelocity*vehicle.SPEED_FACTOR*20;

			//Vehicle state cases.
			case("throttle"): return vehicle.throttle/100D;
			case("fuel"): return vehicle.fuelTank.getFluidLevel()/vehicle.fuelTank.getMaxLevel();
			case("electric_power"): return vehicle.electricPower;
			case("electric_usage"): return vehicle.electricFlow*20D;
			case("brake"): return vehicle.brakeOn ? 1 : 0;
			case("p_brake"): return vehicle.parkingBrakeOn ? 1 : 0;
			case("reverser"): return vehicle.reverseThrust ? 1 : 0;
			case("horn"): return vehicle.hornOn ? 1 : 0;
			case("siren"): return vehicle.sirenOn ? 1 : 0;
			case("hood"): return vehicle.engines.isEmpty() ? 1 : 0;
			case("rain"): return 1.0D + Math.sin(vehicle.world.getRainStrength(new Point3i(vehicle.position)))*Math.toRadians(360*System.currentTimeMillis()/1000)/2D;
			case("door"): return vehicle.parkingBrakeOn && vehicle.velocity < 0.25 ? 1 : 0;
			case("trailer"): return vehicle.towedVehicle != null ? 1 : 0;
			case("hookup"): return vehicle.towedByVehicle != null ? 1 : 0;
			
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
			case("lift_reserve"): return vehicle.trackAngle*3 + 20;
			case("turn_coordinator"): return ((vehicle.angles.z - vehicle.prevAngles.z)/10 + vehicle.angles.y - vehicle.prevAngles.y)/0.15D*25;
			case("turn_indicator"): return (vehicle.angles.y - vehicle.prevAngles.y)/0.15F*25F;
			case("slip"): return 75*vehicle.sideVector.dotProduct(vehicle.normalizedVelocityVector);
			case("gear_setpoint"): return vehicle.gearUpCommand ? 1 : 0;
			case("gear_actual"): return vehicle.gearMovementTime/((double) vehicle.definition.motorized.gearSequenceDuration);
		}
		
		//Check if this is a light variable.
		for(LightType light : LightType.values()){
			if(light.name().toLowerCase().equals(variable)){
				return vehicle.lightsOn.contains(light) ? 1 : 0;
			}
		}
		
		//Check if this is a door variable.
		for(String doorName : vehicle.doorsOpen){
			if(variable.equals(doorName)){
				return 1;
			}
		}
		
		//Check if this is a custom variable.
		if(vehicle.definition.rendering.customVariables != null){
			return vehicle.customsOn.contains((byte)vehicle.definition.rendering.customVariables.indexOf(variable)) ? 1 : 0;
		}
		
		//No variable found for anything.  We could have an error, but likely we have an older pack or are a closed door.
		//Return 0 here to prevent pack crashes.
		return 0;
	}
}
