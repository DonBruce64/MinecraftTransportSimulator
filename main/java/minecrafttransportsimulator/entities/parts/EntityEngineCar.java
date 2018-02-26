package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityCar;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class EntityEngineCar extends EntityEngine{
	private byte gearNumber = 1;
	private double engineTargetRPM;
	private double engineTorque;
	private double engineForce;
	private EntityCar car;

	public EntityEngineCar(World world){
		super(world);
	}

	public EntityEngineCar(World world, EntityCar car, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, car, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		car = (EntityCar) vehicle;

		
		//Set the speed of the engine to the speed of the driving wheels.
		float lowestSpeed = 999F;
		if(getCurrentGear() != 0){
			byte grounders = 0;
			for(EntityWheel wheel : car.wheels){
				++grounders;
				if((wheel.offsetZ > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offsetZ < 0 && car.pack.car.isRearWheelDrive)){
					//If we have grounded ground devices, and this wheel is not on the ground, don't take it into account.
					if(wheel.isOnGround() || (car.groundedGroundDevices.size() == 0)){
						lowestSpeed = Math.min(wheel.angularVelocity, lowestSpeed);
					}
				}
			}
			if(lowestSpeed != 999){
				//Don't let the engine stall while being stopped.
				if(lowestSpeed*1200F*getRatioForGear(gearNumber)*2.0F > engineStallRPM || (!state.running && !state.esOn)){
					RPM = lowestSpeed*1200F*getRatioForGear(gearNumber)*2.0F;
				}else{
					RPM -= (RPM - engineStallRPM)/10;
				}
			}
		}else{
			RPM = Math.max(RPM - 10, 0);
		}
		
		//Do automatic transmission functions if needed.
		if(state.running && isAutomatic){
			if(gearNumber > 0){
				if(RPM > maxSafeRPM*0.5F*(1.0F + car.throttle/100F)){
					shiftUp();
				}else if(RPM < maxSafeRPM*0.25*(1.0F + car.throttle/100F) && gearNumber > 1){
					shiftDown();
				}
			}
		}
		
		//Get friction of wheels.
		float wheelFriction = 0;
		for(EntityWheel wheel : car.wheels){
			if(wheel.isOnGround()){
				if((wheel.offsetZ > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offsetZ <= 0 && car.pack.car.isRearWheelDrive)){
					float frictionLoss = 0.6F - wheel.worldObj.getBlockState(wheel.getPosition().down()).getBlock().slipperiness;
					wheelFriction = wheel.motiveFriction - frictionLoss;
				}
			}
		}
		
		//If running, use the friction of the wheels to determine the new speed.
		if(state.running || state.esOn){
			engineTargetRPM = car.throttle/100F*(maxRPM - engineStartRPM/1.25 - hours) + engineStartRPM/1.25;
			if(getRatioForGear(gearNumber) != 0){
				engineTorque = RPM/maxSafeRPM*getRatioForGear(gearNumber);
				
				//Check to see if the wheels have enough friction to affect the engine.
				engineForce = (engineTargetRPM - RPM)/maxRPM*engineTorque;
				if(Math.abs(engineForce/10) > wheelFriction){
					engineForce /= 2F;
					for(EntityWheel wheel : car.wheels){
						if((wheel.offsetZ > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offsetZ < 0 && car.pack.car.isRearWheelDrive)){
							wheel.angularVelocity = (float) Math.min(engineTargetRPM/1200F/getRatioForGear(gearNumber)/2.0F, wheel.angularVelocity + 0.05);
						}
					}
				}else{
					//If we have wheels no on the ground and we drive them, adjust their velocity now.
					for(EntityWheel wheel : car.wheels){
						if(!wheel.isOnGround() && ((wheel.offsetZ > 0 && car.pack.car.isFrontWheelDrive) || (wheel.offsetZ < 0 && car.pack.car.isRearWheelDrive))){
							wheel.angularVelocity = lowestSpeed;
						}
					}
				}
			}else{
				RPM += (engineTargetRPM - RPM)/10;
				engineForce = 0;
			}
		}else{
			//Not running, so either inhibit motion if not in neutral or just don't do anything.
			if(getCurrentGear() != 0){
				engineForce = -RPM/maxRPM;
			}else{
				engineForce = 0;
			}
		}
	}
	
	public double getForceOutput(){
		return engineForce*30;
	}
	
	public void shiftUp(){
		if(gearNumber == -1){
			gearNumber = 0;
		}else if(gearNumber == 0){
			if(vehicle != null && vehicle.velocity > -0.1){
				gearNumber = 1;
			}else{
				hours += 100;
				MTS.proxy.playSound(this, MTS.MODID + ":engine_shifting_grinding", 1.0F, 1);
			}
		}else if(gearNumber < numberGears){
			++gearNumber;
		}
	}
	
	public void shiftDown(){
		if(gearNumber > 0){
			--gearNumber;
		}else{
			if(gearNumber == 1){
				gearNumber = 0;
			}else if(gearNumber == 0){
				if(vehicle != null && vehicle.velocity < 0.1){
					gearNumber = -1;
				}else{
					hours += 100;
					MTS.proxy.playSound(this, MTS.MODID + ":engine_shifting_grinding", 1.0F, 1);
				}
			}
		}
	}
	
	public byte getCurrentGear(){
		return gearNumber;
	}
	
	public abstract float getRatioForGear(byte gearNumber);
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.gearNumber=tagCompound.getByte("gearNumber");
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setByte("gearNumber", this.gearNumber);
		return tagCompound;
	}
}
