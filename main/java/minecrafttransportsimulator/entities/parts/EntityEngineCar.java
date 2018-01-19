package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.entities.main.EntityCar;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class EntityEngineCar extends EntityEngine{
	private byte gearNumber = 0;
	private double engineTargetRPM;
	private double engineTorque;
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
		
		//Do automatic transmission functions if needed.
		if(state.running && isAutomatic()){
			if(gearNumber > 0){
				if(RPM > maxSafeRPM*0.5*car.throttle/50){
					shiftUp();
				}else if(RPM < engineStartRPM && gearNumber > 1){
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
		if(state.running){
			engineTargetRPM = car.throttle/100F*(maxRPM - engineStartRPM*1.25 - hours) + engineStartRPM*1.25;
			engineTorque = RPM/maxSafeRPM;
			
			
			//Check to see if the wheels have enough friction to affect the engine.
			if(engineTargetRPM*engineTorque/10 <= wheelFriction){
				//TODO make engine RPM based on current wheel speed, desired RPM, and torque;
			}else{
				RPM = engineTargetRPM*engineTorque/10;
				//TODO add wheel screeching noise here.
			}
		}else{
			//Not running, so just let the engine be the speed of the wheels.
			//TODO make code for non-running engine here soley based on wheel speed.
			RPM = Math.max(RPM - 10, 0);
		}
	}
	
	public float getForceOutput(){
		return 0;
	}
	
	public void shiftUp(){
		if(gearNumber == -1){
			gearNumber = 0;
		}else if(gearNumber == 0){
			if(vehicle != null && vehicle.velocity == 0){
				gearNumber = 1;
			}else{
				//TODO add gearbox grinding sound here.
			}
		}else if(gearNumber < getNumberGears()){
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
				if(vehicle != null && vehicle.velocity == 0){
					gearNumber = -1;
				}else{
					//TODO add gearbox grinding sound here.
				}
			}
		}
	}
	
	public byte getCurrentGear(){
		return gearNumber;
	}
	
	public abstract boolean isAutomatic();
	
	public abstract byte getNumberGears();
	
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
