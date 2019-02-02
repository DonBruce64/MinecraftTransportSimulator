package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Plane;
import net.minecraft.nbt.NBTTagCompound;

public class PartEngineJet extends APartEngine{
	private final EntityVehicleF_Plane plane;

	public PartEngineJet(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		this.plane = (EntityVehicleF_Plane) vehicle;
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(state.running){
			double engineTargetRPM = plane.throttle/100F*(pack.engine.maxRPM - engineStartRPM*1.25 - hours*10) + engineStartRPM*1.25;
			double engineRPMDifference = engineTargetRPM - RPM;
			//This is governed by the core, so use the bypass ratio and air density to calculate how fast this thing spools up.
			//Smaller cores and higher altitudes will cause spool times to increase due to lack of airflow to push.
			RPM += pack.engine.fuelConsumption*engineRPMDifference/(10 + pack.engine.gearRatios[0])/vehicle.airDensity;
		}else if(!state.esOn){
			RPM = Math.max(RPM + (plane.velocity - 0.0254*250*RPM/60/20)*15 - 10, 0);
		}
		
		engineRotationLast = engineRotation;
		engineRotation += RPM*1200D/360D;
		engineDriveshaftRotationLast = engineDriveshaftRotation;
		engineDriveshaftRotation += RPM*1200D/360D;
	}
	
	@Override
	public double getForceOutput(){
		//Propellers max out at about 25 force, so use that to determine this force.
		if(state.running){
			//First we need the air density (sea level 1.225) so we know how much air we are moving.
			//We then multiply that by the RPM and the fuel consumption to get the raw power produced
			//by the core of the engine.  This is speed-independent as the core will ALWAYS accelerate air.
			//Note that due to a lack of jet physics formulas available, this is "hacky math".
			double safeRPMFactor = RPM/getSafeRPMFromMax(pack.engine.maxRPM);
			double coreContribution = Math.max(10*vehicle.airDensity*pack.engine.fuelConsumption*safeRPMFactor - pack.engine.gearRatios[0], 0);
			//The fan portion is calculated similarly to how propellers are calculated.
			//This takes into account the air density, and relative speed of the engine versus the fan's desired speed.
			//Again, this is "hacky math", as for some reason there's no data on fan pitches.
			//In this case, however, we don't care about the fuelConsumption as that's only used by the core.
			double fanVelocityFactor = (0.0254*250*RPM/60/20 - plane.velocity)/200D;
			double fanContribution = 10*vehicle.airDensity*safeRPMFactor*fanVelocityFactor*pack.engine.gearRatios[0];
			return plane.reverseThrust ? -(coreContribution + fanContribution) : coreContribution + fanContribution;
		}else{
			return 0;
		}
	}
}
