package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Plane;
import net.minecraft.nbt.NBTTagCompound;

public class PartEngineAircraft extends APartEngine{
	public PartPropeller propeller;
	private final EntityVehicleF_Plane plane;

	public PartEngineAircraft(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		this.plane = (EntityVehicleF_Plane) vehicle;
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(propeller == null){
			if(!childParts.isEmpty()){
				propeller = (PartPropeller) childParts.get(0);
			}
		}else{
			if(childParts.isEmpty()){
				propeller = null;
			}
		}
		if(state.running){
			double engineTargetRPM = plane.throttle/100F*(pack.engine.maxRPM - engineStartRPM*1.25 - hours) + engineStartRPM*1.25;
			double engineRPMDifference = engineTargetRPM - RPM;
			if(propeller != null){
				double propellerForcePenalty = (propeller.pack.propeller.diameter - 75)/(50*this.pack.engine.fuelConsumption - 15);
				double propellerFeedback = -(plane.velocity - 0.0254*Math.abs(propeller.currentPitch)*RPM*pack.engine.gearRatios[0]/60/20 - propellerForcePenalty)*25;
				RPM += engineRPMDifference/10 - propellerFeedback;
			}else{
				RPM += engineRPMDifference/10;
			}
		}else{
			if(propeller != null){
				RPM = Math.max(RPM + (plane.velocity - 0.0254*Math.abs(propeller.currentPitch)*RPM*pack.engine.gearRatios[0]/60/20)*15 - 10, 0);
			}else{
				RPM = Math.max(RPM - 10, 0);
			}
		}
		
		engineRotationLast = engineRotation;
		engineRotation += 360D*RPM/1200D;
		engineDriveshaftRotationLast = engineDriveshaftRotation;
		engineDriveshaftRotation += 360D*RPM/1200D*pack.engine.gearRatios[0];
	}
	
	@Override
	public double getForceOutput(){
		if(propeller != null && propeller.currentPitch != 0 && state.running){
			//Get what the pitch velocity of the propeller would be at the current velocity.
			double currentPitchVelocity = vehicle.velocity*20D;
			//Get the effective pitch velocity of the propeller at the current RPM.
			double effectivePitchVelocity = 0.0254D*propeller.currentPitch*RPM*pack.engine.gearRatios[0]/60D;
			//Multiply by a factor to get the true effective pitch velocity.  This is slightly higher than ideal.
			effectivePitchVelocity *= (1D*propeller.currentPitch/propeller.pack.propeller.diameter + 0.2D)/(1D*propeller.currentPitch/propeller.pack.propeller.diameter);
			if(effectivePitchVelocity != 0){
				//Get the angle of attack of the propeller.
				double angleOfAttack = Math.abs(effectivePitchVelocity - currentPitchVelocity);
				//Now return the thrust equation.  If the angle of attack is greater than 35, sap power off the propeller for stalling.
				return vehicle.airDensity*Math.PI*Math.pow(0.0254*propeller.pack.propeller.diameter/2D, 2)*
						(effectivePitchVelocity*effectivePitchVelocity - effectivePitchVelocity*currentPitchVelocity)*
						Math.pow(propeller.pack.propeller.diameter/2D/Math.abs(propeller.currentPitch) + propeller.pack.propeller.numberBlades/1000D, 1.5)/400D
						*(angleOfAttack > 35 ? 35/angleOfAttack : 1.0D)*Math.signum(effectivePitchVelocity);
			}
		}
		return 0;
	}
}
