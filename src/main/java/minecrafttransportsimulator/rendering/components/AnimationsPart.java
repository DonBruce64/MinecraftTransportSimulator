package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;
import minecrafttransportsimulator.vehicles.parts.PartPropeller;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

/**This class contains methods for part animations.
 * These are used to animate parts on vehicles.  The
 * vehicle itself is animated by {@link AnimationsVehicle}
 *
 * @author don_bruce
 */
public final class AnimationsPart extends AAnimationsBase<APart>{
	
	@Override
	public double getRawVariableValue(APart part, String variable, float partialTicks){
		//First check if we are a base variable.
		double value = getBaseVariableValue(part, variable, partialTicks);
		if(Double.isNaN(value)){
			return 0;
		}
				
		if(part instanceof PartEngine){
			PartEngine engine = (PartEngine) part;
			switch(variable){
				case("engine_isautomatic"): return engine.definition.engine.isAutomatic ? 1 : 0;	
				case("engine_rotation"): return engine.getEngineRotation(partialTicks);
				case("engine_sin"): return Math.sin(Math.toRadians(engine.getEngineRotation(partialTicks)));
				case("engine_cos"): return Math.cos(Math.toRadians(engine.getEngineRotation(partialTicks)));
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
				case("engine_running"): return engine.state.running ? 1 : 0;
				case("engine_jumper_cable"): return engine.linkedEngine != null ? 1 : 0;
				case("engine_hours"): return engine.hours;
			}
		}else if(part instanceof PartGun){
			PartGun gun = (PartGun) part;
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
		}else if(part instanceof PartInteractable){
			PartInteractable interactable = (PartInteractable) part;
			switch(variable){
				case("interactable_count"): return interactable.getInventoryCount();
				case("interactable_percent"): return interactable.getInventoryPercent();
				case("interactable_capacity"): return interactable.getInventoryCapacity();
			}
		}else if(part instanceof PartPropeller){
			PartPropeller propeller = (PartPropeller) part;
			switch(variable){
				case("propeller_pitch_deg"): return Math.toDegrees(Math.atan(propeller.currentPitch / (propeller.definition.propeller.diameter*0.75D*Math.PI)));
				case("propeller_pitch_in"): return propeller.currentPitch;
				case("propeller_pitch_percent"): return 1D*(propeller.currentPitch - PartPropeller.MIN_DYNAMIC_PITCH)/(propeller.definition.propeller.pitch - PartPropeller.MIN_DYNAMIC_PITCH);
			}
		}else if(part instanceof PartGroundDevice){
			PartGroundDevice groundDevice = (PartGroundDevice) part;
			switch(variable){
				case("ground_rotation"): return groundDevice.getRenderingRotation(partialTicks).x;
				case("ground_onground"): return part.vehicle.groundDeviceCollective.groundedGroundDevices.contains(groundDevice) ? 1 : 0;
				case("ground_inliquid"): return groundDevice.isInLiquid() ? 1 : 0;
				case("ground_isflat"): return groundDevice.getFlatState() ? 1 : 0;
			}
		}else if(part instanceof PartSeat){
			PartSeat seat = (PartSeat) part;
			IWrapperEntity riderForSeat = part.vehicle.locationRiderMap.get(seat.placementOffset);
			boolean riderPresent = riderForSeat != null && riderForSeat.isValid();
			switch(variable){
				case("seat_occupied"): return riderPresent ? 1 : 0;
				case("seat_rider_yaw"): {
					if(riderPresent){
						double riderYaw = riderForSeat.getHeadYaw() - part.vehicle.angles.y;
						while(riderYaw < -180) riderYaw += 360;
						while(riderYaw > 180) riderYaw -= 360;
						return riderYaw;
					}else{
						return 0;
					}
				}
				case("seat_rider_pitch"): {
					if(riderPresent) {
						double pitch = part.vehicle.angles.x;
		            	double roll = part.vehicle.angles.z;
		            	double riderYaw = riderForSeat.getHeadYaw() - part.vehicle.angles.y;
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
		
		//Check for generic part variables.
		switch(variable){
			case("part_present"): return 1;
		}
		
		//We didn't find any part-specific animations.
		//We could, however, be wanting the animations of our parent part.
		//If we have a parent part, get it, and try this loop again.
		if(part.parentPart != null){
			return getRawVariableValue(part.parentPart, variable, partialTicks);
		}

		//If we are down here, we must have not found a part variable.
		//This means we might be requesting a vehicle variable on this part.
		//Try to get the vehicle variable, and return whatever we get.
		return part.vehicle.getAnimationSystem().getRawVariableValue(part.vehicle, variable, partialTicks);
	}
}
