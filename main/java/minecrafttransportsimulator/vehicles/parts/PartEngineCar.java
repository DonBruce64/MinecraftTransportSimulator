package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import net.minecraft.nbt.NBTTagCompound;

public class PartEngineCar extends APartEngineGeared{
	private double engineForce;
	private final EntityVehicleG_Car car;

	public PartEngineCar(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		this.car = (EntityVehicleG_Car) vehicle;
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		//Set the speed of the engine to the speed of the driving wheels.
		float lowestSpeed = 999F;
		float vehicleDesiredSpeed = -999F;
		if(currentGear != 0){
			for(APartGroundDevice wheel : car.wheels){
				if((wheel.offset.z > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && car.pack.car.isRearWheelDrive)){
					//If we have grounded wheels, and this wheel is not on the ground, don't take it into account.
					//If we don't have any grounded wheels, use them all to calculate the speeds.
					if(wheel.isOnGround() || (car.groundedWheels.size() == 0)){
						lowestSpeed = Math.min(wheel.angularVelocity, lowestSpeed);
						vehicleDesiredSpeed = (float) Math.max(car.velocity/wheel.getHeight(), vehicleDesiredSpeed);
					}
				}
			}
						
			if(lowestSpeed != 999){
				//Don't let the engine stall while being stopped.
				if(lowestSpeed*1200F*getRatioForCurrentGear()*car.pack.car.axleRatio > engineStallRPM || (!state.running && !state.esOn)){
					RPM = lowestSpeed*1200F*getRatioForCurrentGear()*car.pack.car.axleRatio;
				}else{
					RPM -= (RPM - engineStallRPM)/10;
				}
			}
		}
		
		//Get friction of wheels.
		float wheelFriction = 0;
		for(APartGroundDevice wheel : car.groundedWheels){
			if((wheel.offset.z > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && car.pack.car.isRearWheelDrive)){
				wheelFriction += wheel.getMotiveFriction() - wheel.getFrictionLoss();
			}
		}
		
		//If running, in reverse, and we are a big truck, fire the backup beepers.
		if(state.running && this.currentGear == -1 && car.pack != null && car.pack.car.isBigTruck && car.electricPower > 4 && car.world.getTotalWorldTime()%20==1 && vehicle.world.isRemote){
			MTS.proxy.playSound(vehicle.getPositionVector(), MTS.MODID + ":backup_beeper", 1.0F, 1);
		}
		
		//If running, use the friction of the wheels to determine the new speed.
		if(state.running || state.esOn){
			double engineTargetRPM = !state.esOn ? car.throttle/100F*(pack.engine.maxRPM - engineStartRPM/1.25 - hours) + engineStartRPM/1.25 : engineStartRPM*1.2;
			if(getRatioForCurrentGear() != 0 && car.wheels.size() > 0){
				engineForce = (engineTargetRPM - RPM)/pack.engine.maxRPM*getRatioForCurrentGear()*car.pack.car.axleRatio*pack.engine.fuelConsumption*0.6F;
				//Check to see if the wheels have enough friction to affect the engine.
				if(Math.abs(engineForce/10F) > wheelFriction || (Math.abs(lowestSpeed) - Math.abs(vehicleDesiredSpeed) > 0.1 && Math.abs(lowestSpeed) - Math.abs(vehicleDesiredSpeed) < Math.abs(engineForce/10F))){
					engineForce *= car.currentMass/100000F*wheelFriction/Math.abs(engineForce/10F);					
					for(APartGroundDevice wheel : car.wheels){
						if((wheel.offset.z > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && car.pack.car.isRearWheelDrive)){
							if(getRatioForCurrentGear() > 0){
								if(engineForce >= 0){
									wheel.angularVelocity = (float) Math.min(engineTargetRPM/1200F/getRatioForCurrentGear()/car.pack.car.axleRatio, wheel.angularVelocity + 0.01);
								}else{
									wheel.angularVelocity = (float) Math.min(engineTargetRPM/1200F/getRatioForCurrentGear()/car.pack.car.axleRatio, wheel.angularVelocity - 0.01);
								}
							}else{
								if(engineForce >= 0){
									wheel.angularVelocity = (float) Math.max(engineTargetRPM/1200F/getRatioForCurrentGear()/car.pack.car.axleRatio, wheel.angularVelocity - 0.01);
								}else{
									
									wheel.angularVelocity = (float) Math.max(engineTargetRPM/1200F/getRatioForCurrentGear()/car.pack.car.axleRatio, wheel.angularVelocity + 0.01);
								}
							}
							wheel.skipAngularCalcs = true;
						}
					}
				}else{
					//If we have wheels not on the ground and we drive them, adjust their velocity now.
					for(APartGroundDevice wheel : car.wheels){
						wheel.skipAngularCalcs = false;
						if(!wheel.isOnGround() && ((wheel.offset.z > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && car.pack.car.isRearWheelDrive))){
							wheel.angularVelocity = lowestSpeed;
						}
					}
				}
				//Don't let us have negative engine force at low speeds.
				//This causes odd reversing behavior when the engine tries to maintain speed.
				if((engineForce < 0 && currentGear > 0 && car.velocity < 0.25) || (engineForce > 0 && currentGear < 0 && car.velocity > -0.25)){
					engineForce = 0;
				}
			}else{
				for(APartGroundDevice wheel : car.wheels){
					wheel.skipAngularCalcs = false;
				}
				RPM += (engineTargetRPM - RPM)/10;
				if(RPM > getSafeRPMFromMax(this.pack.engine.maxRPM)){
					RPM -= Math.abs(engineTargetRPM - RPM)/5;
				}
				engineForce = 0;
			}
		}else{
			//Not running, so either inhibit motion if not in neutral or just don't do anything.
			if(currentGear != 0){
				engineForce = -RPM/pack.engine.maxRPM*Math.signum(currentGear);
			}else{
				engineForce = 0;
				RPM = Math.max(RPM - 10, 0);
			}
		}
		
		//Set engine and driveshaft rotations for rendering of parts of models.
		engineRotationLast = engineRotation;
		engineRotation += 360D*RPM/1200D;
		
		float driveShaftDesiredSpeed = -999F;
		for(APartGroundDevice wheel : car.wheels){
			if((wheel.offset.z > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && car.pack.car.isRearWheelDrive)){
				driveShaftDesiredSpeed = (float) Math.max(Math.abs(wheel.angularVelocity), driveShaftDesiredSpeed);
			}
		}
		driveShaftDesiredSpeed = (float) (vehicle.speedFactor*driveShaftDesiredSpeed*Math.signum(car.velocity)*360D);
		engineDriveshaftRotationLast = engineDriveshaftRotation;
		engineDriveshaftRotation += driveShaftDesiredSpeed;
	}
	
	@Override
	public void removePart(){
		super.removePart();
		for(APartGroundDevice wheel : car.wheels){
			if(!wheel.isOnGround() && ((wheel.offset.z > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && car.pack.car.isRearWheelDrive))){
				wheel.skipAngularCalcs = false;
			}
		}
	}
	
	public double getForceOutput(){
		return engineForce*30F;
	}
}
