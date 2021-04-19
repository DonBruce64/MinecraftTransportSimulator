package minecrafttransportsimulator.rendering.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.rendering.components.AAnimationsBase;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This class contains methods for vehicle animations.  These are used to animate
 * the vehicle, as well as instruments.  All methods are designed to be as
 * global as possible to keep all animations in this class.  Parts are rendered
 * by {@link AnimationsParts}.
 *
 * @author don_bruce
 */
public final class AnimationsVehicle extends AAnimationsBase<EntityVehicleF_Physics>{
	
	/**
	 *  Returns the part number for the passed-in variable, or -1 if there is no part number for the variable.
	 *  Part number is 0-indexed to conform to the list indexes internal to vehicle parts.
	 */
	public static int getPartNumber(String variable){
		if(variable.matches("^.*_[0-9]+$")){
			return Integer.parseInt(variable.substring(variable.lastIndexOf('_') + 1)) - 1;
		}else{
			return -1;
		}
	}
	
	/**
	 *  Returns the part type prefix for the passed-in variable.
	 */
	public static String getPartType(String variable){
		return variable.substring(0, variable.indexOf('_'));
	}
	
	/**
	 *  Returns the part class for the variable based on the variable's name prefix, or null
	 *  if such a class does not exist.
	 */
	public static Class<? extends APart> getPartClass(String variable){
		//Get the part type from the variable.
		String partType = getPartType(variable);
		switch(partType){
			case("interactable"): return PartInteractable.class;	
			case("engine"): return PartEngine.class;
			case("gun"): return PartGun.class;
			case("part"): return APart.class;
			case("propeller"): return PartPropeller.class;
			case("ground"): return PartGroundDevice.class;
			case("seat"): return PartSeat.class;
			default: if(ConfigSystem.configObject.clientControls.devMode.value){
				throw new IllegalArgumentException("Was told to find part: " + partType + " for definition: " + variable + " but could not as the part isn't a valid part name.  Is your spelling correct?  Or are you trying to name a door with a suffix of a number?  Only part variables can have numbers at the end of their names!");
			}
		}
		return null;
	}
	
	@Override
	public double getRawVariableValue(EntityVehicleF_Physics vehicle, String variable, float partialTicks){
		//First check if we are a base variable.
		double value = getBaseVariableValue(vehicle, variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}
		
		//If we have a variable with a suffix, we need to get that part first and pass
		//it into this method rather than trying to run through the code now.
		int partNumber = getPartNumber(variable);
		if(partNumber != -1){
			//Get the part type from the variable.
			String partType = getPartType(variable);
			Class<? extends APart> partClass = getPartClass(variable);
			
			if(partClass != null){
				//Iterate through the definitions to find the index of the pack def for the part we want.
				JSONPartDefinition foundDef = null;
				for(JSONPartDefinition vehicleDef : vehicle.definition.parts){
					//If this part is the one we want, get it or add to our index.
					for(String defPartType : vehicleDef.types){
						if(partType.equals("part") || defPartType.startsWith(partType)){
							if(partNumber == 0){
								foundDef = vehicleDef;
							}else{
								--partNumber;
							}
							break;
						}
					}
					
					//Also check additional parts if we have them..
					if(foundDef == null && vehicleDef.additionalParts != null){
						for(JSONPartDefinition additionalDef : vehicleDef.additionalParts){
							for(String defPartType : additionalDef.types){
								if(partType.equals("part") || defPartType.startsWith(partType)){
									if(partNumber == 0){
										foundDef = additionalDef;
									}else{
										--partNumber;
									}
									break;
								}
							}
							if(foundDef != null){
								break;
							}
						}
					}
					
					//If we found our part, try to get it.
					if(foundDef != null){
						//Get the part at this location.  If it's of the same class as what we need, use it for animation.
						//If it's not, or it doesn't exist, return 0.
						APart foundPart = vehicle.getPartAtLocation(foundDef.pos);
						if(foundPart != null && partClass.isInstance(foundPart)){
							return foundPart.getAnimator().getRawVariableValue(foundPart, variable.substring(0, variable.lastIndexOf("_")), partialTicks);
						}else{
							return 0;
						}
					}
				}
			}
			
			//We couldn't find the part we were supposed to.  Likely because it hasn't been placed yet.
			return 0;
		}

		//Not a part variable that needs forwarding.  Try vehicle variables.
		switch(variable){
			//Vehicle world state cases.
			case("yaw"): return vehicle.angles.y;
			case("heading"): int heading = (int)-vehicle.angles.y; if(ConfigSystem.configObject.clientControls.north360.value) heading += 180; while (heading < 1) heading += 360; while (heading > 360) heading -= 360; return heading;
			case("pitch"): return vehicle.angles.x;
			case("roll"): return vehicle.angles.z;
			case("altitude"): return vehicle.position.y;
			case("speed"): return vehicle.axialVelocity*EntityVehicleF_Physics.SPEED_FACTOR*20;
			case("acceleration"): return vehicle.motion.length() - vehicle.prevMotion.length();

			//Vehicle state cases.
			case("throttle"): return vehicle.throttle/(double)EntityVehicleF_Physics.MAX_THROTTLE;
			case("brake"): return vehicle.brake/(double)EntityVehicleF_Physics.MAX_BRAKE;
			case("fuel"): return vehicle.fuelTank.getFluidLevel()/vehicle.fuelTank.getMaxLevel();
			case("electric_power"): return vehicle.electricPower;
			case("electric_usage"): return vehicle.electricFlow*20D;
			case("p_brake"): return vehicle.parkingBrakeOn ? 1 : 0;
			case("reverser"): return vehicle.reverseThrust ? 1 : 0;
			case("horn"): return vehicle.hornOn ? 1 : 0;
			case("locked"): return vehicle.locked ? 1 : 0;
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
			case("flaps_moving"): return vehicle.flapCurrentAngle != vehicle.flapDesiredAngle ? 1 : 0;
			case("trim_aileron"): return vehicle.aileronTrim/10D;
			case("trim_elevator"): return vehicle.elevatorTrim/10D;
			case("trim_rudder"): return vehicle.rudderTrim/10D;
			case("vertical_speed"): return vehicle.motion.y*EntityVehicleF_Physics.SPEED_FACTOR*20;
			case("lift_reserve"): return -vehicle.trackAngle;
			case("turn_coordinator"): return ((vehicle.angles.z - vehicle.prevAngles.z)/10 + vehicle.angles.y - vehicle.prevAngles.y)/0.15D*25;
			case("turn_indicator"): return (vehicle.angles.y - vehicle.prevAngles.y)/0.15F*25F;
			case("slip"): return 75*vehicle.sideVector.dotProduct(vehicle.normalizedVelocityVector);
			case("gear_setpoint"): return vehicle.gearUpCommand ? 1 : 0;
			case("gear_moving"): return (vehicle.gearUpCommand ? vehicle.gearMovementTime == vehicle.definition.motorized.gearSequenceDuration : vehicle.gearMovementTime == 0) ? 1 : 0;
			case("beacon_direction"): return vehicle.selectedBeacon != null ? vehicle.angles.getClampedYDelta(Math.toDegrees(Math.atan2(vehicle.selectedBeacon.position.x - vehicle.position.x, vehicle.selectedBeacon.position.z - vehicle.position.z))) : 0;
			case("beacon_bearing_setpoint"): return vehicle.selectedBeacon != null ? vehicle.selectedBeacon.bearing : 0;
			case("beacon_bearing_delta"): return vehicle.selectedBeacon != null ? vehicle.selectedBeacon.getBearingDelta(vehicle) : 0;
			case("beacon_glideslope_setpoint"): return vehicle.selectedBeacon != null ? vehicle.selectedBeacon.glideSlope : 0;
			case("beacon_glideslope_actual"): return vehicle.selectedBeacon != null ? Math.toDegrees(Math.asin((vehicle.position.y - vehicle.selectedBeacon.position.y)/vehicle.position.distanceTo(vehicle.selectedBeacon.position))) : 0;
			case("beacon_glideslope_delta"): return vehicle.selectedBeacon != null ? vehicle.selectedBeacon.glideSlope - Math.toDegrees(Math.asin((vehicle.position.y - vehicle.selectedBeacon.position.y)/vehicle.position.distanceTo(vehicle.selectedBeacon.position))) : 0;
			
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
								case("distance"): return vehicle.missilesIncoming.get(missileNumber).targetDistance;
								case("direction"): {
									Point3d missilePos = vehicle.missilesIncoming.get(missileNumber).position;
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
		
		//Not a vehicle variable or a part variable.  We could have an error, but likely we have an older pack,
		//a closed door, a missing part, a custom variable that's not on, or something else entirely.
		//In that case, we return 0 to prevent crashes.
		return 0;
	}
}
