package minecrafttransportsimulator.multipart.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import net.minecraft.nbt.NBTTagCompound;

public class PartEngineCar extends APartEngine{
	public byte currentGear = 1;
	private boolean spinningOut;
	private double engineForce;
	private double engineDriveshaftRotation;
	private double engineDriveshaftRotationLast;
	private final EntityMultipartF_Car car;

	public PartEngineCar(EntityMultipartD_Moving multipart, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(multipart, packPart, partName, dataTag);
		this.car = (EntityMultipartF_Car) multipart;
		this.currentGear = dataTag.getByte("gearNumber");
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		//Set the speed of the engine to the speed of the driving wheels.
		float lowestSpeed = 999F;
		float vehicleDesiredSpeed = -999F;
		if(currentGear != 0){
			for(PartGroundDevice wheel : car.wheels){
				if((wheel.offset.zCoord > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.zCoord <= 0 && car.pack.car.isRearWheelDrive)){
					//If we have grounded ground devices, and this wheel is not on the ground, don't take it into account.
					if(wheel.isOnGround() || (car.groundedGroundDevices.size() == 0)){
						lowestSpeed = Math.min(wheel.angularVelocity, lowestSpeed);
						vehicleDesiredSpeed = (float) Math.max(car.velocity/wheel.getHeight(), vehicleDesiredSpeed);
					}
				}
			}
			
			if(lowestSpeed != 999){
				//Don't let the engine stall while being stopped.
				if(lowestSpeed*1200F*getRatioForCurrentGear() > engineStallRPM || (!state.running && !state.esOn)){
					RPM = lowestSpeed*1200F*getRatioForCurrentGear();
				}else{
					RPM -= (RPM - engineStallRPM)/10;
				}
			}
		}
		
		//Do automatic transmission functions if needed.
		if(state.running && pack.engine.isAutomatic){
			if(currentGear > 0){
				if(RPM > getSafeRPMFromMax(this.pack.engine.maxRPM)*0.5F*(1.0F + car.throttle/100F)){
					shiftUp();
				}else if(RPM < getSafeRPMFromMax(this.pack.engine.maxRPM)*0.25*(1.0F + car.throttle/100F) && currentGear > 1){
					shiftDown();
				}
			}
		}
		
		//Get friction of wheels.
		float wheelFriction = 0;
		for(PartGroundDevice wheel : car.wheels){
			if((wheel.offset.zCoord > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.zCoord <= 0 && car.pack.car.isRearWheelDrive)){
				wheelFriction += wheel.getMotiveFriction() - wheel.getFrictionLoss();
			}
		}
		
		//If running, in reverse, and we are a big truck, fire the backup beepers.
		if(state.running && this.currentGear == -1 && car.pack != null && car.pack.car.isBigTruck && car.electricPower > 4 && car.worldObj.getTotalWorldTime()%20==1 && multipart.worldObj.isRemote){
			MTS.proxy.playSound(multipart.getPositionVector(), MTS.MODID + ":backup_beeper", 1.0F, 1);
		}
		
		//If running, use the friction of the wheels to determine the new speed.
		if(state.running || state.esOn){
			double engineTargetRPM = car.throttle/100F*(pack.engine.maxRPM - engineStartRPM/1.25 - hours) + engineStartRPM/1.25;
			if(getRatioForCurrentGear() != 0 && car.wheels.size() > 0){
				engineForce = (engineTargetRPM - RPM)/pack.engine.maxRPM*getRatioForCurrentGear()*pack.engine.fuelConsumption*2.0F;
				//Check to see if the wheels have enough friction to affect the engine.
				if(Math.abs(engineForce/10F) > wheelFriction || (Math.abs(lowestSpeed) - Math.abs(vehicleDesiredSpeed) > 0.1 && Math.abs(lowestSpeed) - Math.abs(vehicleDesiredSpeed) < Math.abs(engineForce/10F))){
					engineForce *= car.currentMass/100000F;
					for(PartGroundDevice wheel : car.wheels){
						if((wheel.offset.zCoord > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.zCoord <= 0 && car.pack.car.isRearWheelDrive)){
							if(getRatioForCurrentGear() > 0){
								wheel.angularVelocity = (float) Math.min(engineTargetRPM/1200F/getRatioForCurrentGear(), wheel.angularVelocity + 0.01*Math.signum(engineForce));
							}else{
								wheel.angularVelocity = (float) Math.max(engineTargetRPM/1200F/getRatioForCurrentGear(), wheel.angularVelocity + 0.01*Math.signum(engineForce));
							}
							wheel.skipAngularCalcs = true;
						}
					}
				}else{
					//If we have wheels not on the ground and we drive them, adjust their velocity now.
					for(PartGroundDevice wheel : car.wheels){
						wheel.skipAngularCalcs = false;
						if(!wheel.isOnGround() && ((wheel.offset.zCoord > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.zCoord <= 0 && car.pack.car.isRearWheelDrive))){
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
				for(PartGroundDevice wheel : car.wheels){
					wheel.skipAngularCalcs = false;
				}
				RPM += (engineTargetRPM - RPM)/10;
				if(RPM > getSafeRPMFromMax(this.pack.engine.maxRPM)){
					RPM -= (engineTargetRPM - RPM)/5;
				}
				engineForce = 0;
			}
		}else{
			//Not running, so either inhibit motion if not in neutral or just don't do anything.
			if(currentGear != 0){
				engineForce = -RPM/pack.engine.maxRPM;
			}else{
				engineForce = 0;
				RPM = Math.max(RPM - 10, 0);
			}
		}
		
		//Set driveshaft rotations for rendering of parts of models.
		float driveShaftDesiredSpeed = -999F;
		for(PartGroundDevice wheel : car.wheels){
			if((wheel.offset.zCoord > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.zCoord <= 0 && car.pack.car.isRearWheelDrive)){
				driveShaftDesiredSpeed = (float) Math.max(Math.abs(wheel.angularVelocity), driveShaftDesiredSpeed);
			}
		}
		driveShaftDesiredSpeed = (float) Math.toDegrees(driveShaftDesiredSpeed*Math.signum(car.velocity));
		engineDriveshaftRotationLast = engineDriveshaftRotation;
		engineDriveshaftRotation += driveShaftDesiredSpeed;
	}
	
	@Override
	public void removePart(){
		super.removePart();
		for(PartGroundDevice wheel : car.wheels){
			if(!wheel.isOnGround() && ((wheel.offset.zCoord > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offset.zCoord <= 0 && car.pack.car.isRearWheelDrive))){
				wheel.skipAngularCalcs = false;
			}
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = super.getPartNBTTag();
		dataTag.setByte("gearNumber", this.currentGear);
		return dataTag;
	}
	
	private float getRatioForCurrentGear(){
		return currentGear == -1 ? pack.engine.gearRatios[0] : currentGear > 0 ? pack.engine.gearRatios[currentGear + 1] : 0;
	}
	
	public float getGearshiftRotation(){
		return pack.engine.isAutomatic ? Math.min(1, currentGear)*15F : currentGear*5;
	}
	
	public double getDriveshaftRotation(float partialTicks){
		return engineDriveshaftRotation + (engineDriveshaftRotation - engineDriveshaftRotationLast)*partialTicks;
	}
	
	public double getForceOutput(){
		return engineForce*30F;
	}
	
	public void shiftUp(){
		if(currentGear == -1){
			currentGear = 0;
		}else if(currentGear == 0){
			if(car.velocity > -0.1){
				currentGear = 1;
			}else if(multipart.worldObj.isRemote){
				MTS.proxy.playSound(partPos, MTS.MODID + ":engine_shifting_grinding", 1.0F, 1);
			}
		}else if(currentGear < pack.engine.gearRatios.length - 2){
			++currentGear;
		}
	}
	
	public void shiftDown(){
		if(currentGear > 0){
			--currentGear;
		}else{
			if(currentGear == 1){
				currentGear = 0;
			}else if(currentGear == 0){
				if(car.velocity < 0.1){
					currentGear = -1;
				}else if(multipart.worldObj.isRemote){
					MTS.proxy.playSound(partPos, MTS.MODID + ":engine_shifting_grinding", 1.0F, 1);
				}
			}
		}
	}
}
