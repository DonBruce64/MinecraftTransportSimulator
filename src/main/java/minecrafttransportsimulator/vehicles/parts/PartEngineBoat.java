package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.PackVehicleObject.PackPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class PartEngineBoat extends APartEngineGeared	{

	public PartEngineBoat(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		//Boat engines are similar to airplane engines, except the propellers are built-in and are set to 25 degree pitches.
		//This pitch is only used when the engine is turned off and not providing power, so it's not really critical.
		//Gear ratio is assumed to be 1, as it'll be a straight-shaft connection.
		//Check 1 block down for liquid.  If we are in liquid, then we should provide power.
		boolean inLiquid = vehicle.world.getBlockState(new BlockPos(partPos).down()).getMaterial().isLiquid();
		if(state.running){
			double engineTargetRPM = (vehicle.throttle/100F*((inLiquid ? getSafeRPMFromMax(pack.engine.maxRPM) : pack.engine.maxRPM) - engineStartRPM*1.25 - hours) + engineStartRPM*1.25);
			if(inLiquid){
				RPM += (engineTargetRPM - RPM - engineStartRPM*0.15F)/10;
			}else{
				RPM += (engineTargetRPM - RPM)/10;
			}
		}else{
			RPM = Math.max(RPM + (vehicle.velocity - 0.0254*25*RPM/60/20)*15 - 10, 0);
		}
		
		engineRotationLast = engineRotation;
		engineRotation += 360D*RPM/1200D;
		engineDriveshaftRotationLast = engineDriveshaftRotation;
		engineDriveshaftRotation += 360D*RPM/1200D;
	}
	
	@Override
	public double getForceOutput(){
		return state.running ? RPM/pack.engine.maxRPM*50*pack.engine.fuelConsumption*currentGear : 0;
	}
	
	@Override
	protected float getRatioForCurrentGear(){
		return currentGear;
	}
	
	@Override
	public void shiftUp(boolean packet){
		if(currentGear == -1){
			currentGear = 0;
		}else if(currentGear == 0){
			currentGear = 1;
		}
	}
	
	@Override
	public void shiftDown(boolean packet){
		if(currentGear > 0){
			currentGear = 0;
		}else if(currentGear == 0){
			currentGear = -1;
		}
	}
}
