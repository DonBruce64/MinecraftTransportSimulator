package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.sounds.EngineSound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public abstract class EntityEngine extends EntityChild{
	protected EntityVehicle vehicle;
	public boolean fueled;
	public int internalFuel;
	public double engineRPM;
	public double engineTemp = 20;
	public double hours;
	
	//unique to each class of engine
	protected byte starterIncrement;
	protected byte starterPower;
	protected String engineRunningSoundName;
	protected String engineCrankingSoundName;
	
	//unique to each individual engine
	protected boolean engineEngaged;
	protected boolean engineOn;
	protected byte starterState;
	protected int maxEngineRPM;
	protected float fuelConsumption;
	private EngineSound engineSound;

	public EntityEngine(World world) {
		super(world);
	}

	public EntityEngine(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
		this.maxEngineRPM = (propertyCode/((int) 100))*100;
		this.fuelConsumption = (propertyCode%100)/10F;
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		vehicle = (EntityVehicle) this.parent;
		
		if(isBurning()){
			hours += 0.1;
			if(isLiquidAt(posX, posY + 0.25, posZ)){
				engineTemp -= 0.3;
			}else{
				engineTemp += 0.3;
			}
			if(engineTemp > 150){
				explodeEngine();
				return;
			}
		}
		
		engineTemp -= (engineTemp - (20*(1 - posY/400)))*(0.25 + vehicle.velocity/2F)/100F/2F;
		if(engineOn){
			engineTemp += engineRPM/5000F/2F;
			hours += 0.001;
			if(engineRPM > 500 && engineTemp < 30){//Not warmed up
				hours += 0.001*(engineRPM/500 - 1);
			}
			if(engineRPM > maxEngineRPM - (maxEngineRPM - 2500)/2){//Too fast
				hours += 0.001*(engineRPM - (maxEngineRPM - (maxEngineRPM - 2500)/2))/10F;
				engineTemp += (engineRPM - (maxEngineRPM - (maxEngineRPM - 2500)/2))/1000;
			}
			if(engineTemp > 93.3333){//Too hot, 200 by gauge standard
				hours += 0.001*(engineTemp - 93.3333);
				if(engineTemp > 130){
					setFire(10);
				}
			}

			vehicle.fuel -= this.fuelConsumption*MFS.fuelUsageFactor*engineRPM/maxEngineRPM;
			if(vehicle.fuel <= 0 || engineRPM < 300 || isLiquidAt(posX, posY, posZ)){
				stopEngine(false);
			}else{
				fueled = true;
			}
		}else{
			if(engineRPM > 500 && vehicle.fuel > 0 && vehicle.throttle > 5 && engineEngaged && !isLiquidAt(posX, posY + 0.25, posZ)){
				MFS.proxy.playSound(this, "mfs:engine_starting", 1, 1);
				engineOn=true;
			}
		}
		
		if(starterState > 0){
			if(starterState==starterIncrement){
				MFS.proxy.playSound(this, "mfs:" + engineCrankingSoundName, 1, 1);
			}
			--starterState;
			vehicle.fuel -= this.fuelConsumption*MFS.fuelUsageFactor;
			if(engineRPM < 600){
				engineRPM = Math.min(engineRPM+starterPower, 600);
			}else{
				engineRPM = Math.max(engineRPM-starterPower, 600);
			}
		}				
		engineSound = MFS.proxy.updateEngineSoundAndSmoke(engineSound, this);
	}
	
	@Override
	public void setDead(){
		super.setDead();
		engineOn=false;
		fueled=false;
		internalFuel=0;
		engineSound = MFS.proxy.updateEngineSoundAndSmoke(engineSound, this);
	}
	
	public void stopEngine(boolean switchedOff){
		engineEngaged = !switchedOff;
		if(engineOn){
			internalFuel = 100;
			engineOn = false;
			fueled = false;
		}
	}
	
	public void startEngine(){
		engineEngaged = true;
		if(starterState==0){
			this.starterState += starterIncrement;
		}
	}
	
	protected void explodeEngine(){
		worldObj.newExplosion(this, posX, posY, posZ, 1F, true, true);
		this.parent.removeChild(this.UUID, false);
	}
	
	public EngineSound getEngineSound(){
		if(worldObj.isRemote){
			return new EngineSound(new ResourceLocation("mfs:" + engineRunningSoundName), this, 0.5F, 2000F);
		}else{
			return null;
		}
	}
	
	public double[] getEngineProperties(){
		return new double[] {this.engineTemp, this.engineRPM, this.maxEngineRPM};
	}
		
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.engineOn=tagCompound.getBoolean("engineOn");
		this.maxEngineRPM=tagCompound.getInteger("maxEngineRPM");
		this.fuelConsumption=tagCompound.getFloat("fuelConsumption");
		this.engineRPM=tagCompound.getDouble("engineRPM");
		this.engineTemp=tagCompound.getDouble("engineTemp");
		this.hours=tagCompound.getDouble("hours");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("engineOn", this.engineOn);
		tagCompound.setInteger("maxEngineRPM", this.maxEngineRPM);
		tagCompound.setFloat("fuelConsumption", this.fuelConsumption);
		tagCompound.setDouble("engineRPM", this.engineRPM);
		tagCompound.setDouble("engineTemp", this.engineTemp);
		tagCompound.setDouble("hours", this.hours);
	}
}
