package minecrafttransportsimulator.rendering.vehicles;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Plane;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;

/**This class contains static methods for vehicle animations.  These are used to animate
 * the vehicle and its parts, as well as instruments.  All methods are designed to be as
 * global as possible to keep all animations in this class.
 *
 * @author don_bruce
 */
public final class RenderAnimations{
	/**
	 *  Returns the current value for the passed-in variable on the passed-in vehicle.  A part may or
	 *  may not be passed in to allow for part-specific animations (such as a specific engine's RPM).
	 *  If a value other than 0 is passed-in, the variable returned will be clamped to that value.
	 *  This is in both the positive and negative direction.
	 */
	public static double getVariableValue(String variable, float scaling, float offset, float minClamp, float maxClamp, boolean absolute, float partialTicks, EntityVehicleE_Powered vehicle, APart optionalPart){
		double value = offset + scaling*(absolute ? Math.abs(getVariableValue(variable, partialTicks, vehicle, optionalPart)) : getVariableValue(variable, partialTicks, vehicle, optionalPart));
		if(minClamp != 0 && value < minClamp){
			return minClamp;
		}else if(maxClamp != 0 && value > maxClamp){
			return maxClamp;
		}else{
			return value;
		}
	}
	
	private static double getVariableValue(String variable, float partialTicks, EntityVehicleE_Powered vehicle, APart optionalPart){
		//If we have a variable with a suffix, we need to get that part first and pass
		//it into this method rather than trying to run through the code now.
		if(variable.substring(variable.length() - 1).matches("[0-9]+")){
			//Get the part number and the type from the variable.
			//Take off one because we are zero-indexed.
			int partNumber = Integer.parseInt(variable.substring(variable.length() - 1)) - 1;
			String partType = variable.substring(0, variable.indexOf('_'));
			final Class<?> partClass;
			switch(partType){
				case("engine"): partClass = PartEngine.class; break;
				case("propeller"): partClass = PartPropeller.class; break;
				case("gun"): partClass = PartGun.class; break;
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
							APart foundPart = vehicle.getPartAtLocation(vehiclePart.pos[0], vehiclePart.pos[1], vehiclePart.pos[2]);
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
					case("engine_rpm"): return engine.definition.engine.maxRPM < 15000 ? engine.RPM : engine.RPM/10D;
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
			}else if(optionalPart instanceof PartPropeller){
				PartPropeller propeller = (PartPropeller) optionalPart;
				switch(variable){
					case("propeller_pitch_deg"): return Math.toDegrees(Math.atan(propeller.currentPitch / (propeller.definition.propeller.diameter*0.75D*Math.PI)));
					case("propeller_pitch_in"): return propeller.currentPitch;
					case("propeller_pitch_percent"): return 1D*(propeller.currentPitch - PartPropeller.MIN_DYNAMIC_PITCH)/(propeller.definition.propeller.pitch - PartPropeller.MIN_DYNAMIC_PITCH);
				}
			}else if(optionalPart instanceof PartGun){
				PartGun gun = (PartGun) optionalPart;
				switch(variable){
					case("gun_pitch"): return gun.currentPitch;
					case("gun_yaw"): return gun.currentYaw;
					case("gun_ammo"): return gun.bulletsLeft;
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
			case("yaw"): return -vehicle.rotationYaw;
			case("pitch"): return vehicle.rotationPitch;
			case("roll"): return vehicle.rotationRoll;
			case("altitude"): return vehicle.posY;
			case("speed"): return vehicle.velocity*vehicle.SPEED_FACTOR*20;
			case("turn_coordinator"): return ((vehicle.rotationRoll - vehicle.prevRotationRoll)/10 + vehicle.rotationYaw - vehicle.prevRotationYaw)/0.15D*25;
			case("turn_indicator"): return (vehicle.rotationYaw - vehicle.prevRotationYaw)/0.15F*25F;
			
			//Inertia from accelerating and braking.
            case("acceleration"): return vehicle.acclInertia();
            case("braking"): return vehicle.brakeInertia();

			//Vehicle state cases.
			case("throttle"): return vehicle.throttle/100D;
			case("fuel"): return vehicle.fuel/vehicle.definition.motorized.fuelCapacity;
			case("electric_power"): return vehicle.electricPower;
			case("electric_usage"): return vehicle.electricFlow*20D;
			case("brake"): return vehicle.brakeOn ? 1 : 0;
			case("p_brake"): return vehicle.parkingBrakeOn ? 1 : 0;
			case("reverser"): return vehicle.reversePercent/20D;
			case("steering_wheel"): return vehicle.getSteerAngle();
			case("horn"): return vehicle.hornOn ? 1 : 0;
			case("siren"): return vehicle.sirenOn ? 1 : 0;
			case("hood"): return vehicle.engines.isEmpty() ? 1 : 0;
			case("rain"): return vehicle.world.getRainStrength(1.0F) == 1.0F ? (1.0D + Math.sin(((int)(vehicle.world.getRainStrength(1.0F) + vehicle.world.getThunderStrength(1.0F))*Math.toRadians(360*System.currentTimeMillis()/1000))))/2D : 0;
			case("door"): return (vehicle.prevParkingBrakeAngle + (vehicle.parkingBrakeAngle - vehicle.prevParkingBrakeAngle)*partialTicks)/30D;
		}
		
		//Check if this is a light variable.
		for(LightType light : LightType.values()){
			if(light.name().toLowerCase().equals(variable)){
				return vehicle.lightsOn.contains(light) ? 1 : 0;
			}
		}
		
		//Not a generic variable.  Check vehicle-class-specific variables.
		if(vehicle instanceof EntityVehicleF_Ground){
			EntityVehicleF_Ground ground = (EntityVehicleF_Ground) vehicle;
			switch(variable){
				case("trailer"): return ground.towingAngle/30D;
				case("hookup"): return ground.towedByVehicle != null ? ground.towedByVehicle.towingAngle/30D : 0;
			}
		}else if(vehicle instanceof EntityVehicleF_Air){
			EntityVehicleF_Air aircraft = (EntityVehicleF_Air) vehicle;
			switch(variable){
				case("aileron"): return aircraft.aileronAngle/10D;
				case("elevator"): return aircraft.elevatorAngle/10D;
				case("rudder"): return aircraft.rudderAngle/10D;
				case("trim_aileron"): return aircraft.aileronTrim/10D;
				case("trim_elevator"): return aircraft.elevatorTrim/10D;
				case("trim_rudder"): return aircraft.rudderTrim/10D;
				case("vertical_speed"): return vehicle.motionY*vehicle.SPEED_FACTOR*20;
				case("slip"): return 75*aircraft.sideVec.dotProduct(vehicle.velocityVec);
			}
			if(aircraft instanceof EntityVehicleG_Plane){
				EntityVehicleG_Plane plane = (EntityVehicleG_Plane) aircraft;
				switch(variable){
					case("lift_reserve"): return aircraft.trackAngle*3 + 20;
					case("flaps_setpoint"): return plane.flapDesiredAngle/10D;
					case("flaps_actual"): return plane.flapCurrentAngle/10D;
				}
			}
		}
		
		//No variable found for anything.  We could have an error, but likely we have an older pack.
		//Return 0 here to prevent pack crashes.
		return 0;
	}
}
