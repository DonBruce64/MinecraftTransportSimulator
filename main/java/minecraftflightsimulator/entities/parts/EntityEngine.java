package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.sounds.EngineSound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public abstract class EntityEngine extends EntityChild{
	public boolean fueled;
	public int internalFuel;
	public double engineRPM;
	
	//unique to each class of engine
	protected byte starterIncrement;
	protected byte starterPower;
	protected String engineRunningSoundName;
	protected String engineCrankingSoundName;
	protected String engineStartingSoundName;
	
	//unique to each individual engine
	private boolean engineEngaged;
	private boolean engineOn;
	private byte starterState;
	private int maxEngineRPM;
	private float fuelConsumption;
	private EngineSound engineSound;
	protected EntityPropeller propeller;

	public EntityEngine(World world) {
		super(world);
	}

	public EntityEngine(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
		this.maxEngineRPM = (propertyCode/((int) 100))*100;
		this.fuelConsumption = (propertyCode%100)/10F;
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		//TODO make engine drownable
		if(engineOn){
			parent.fuel -= this.fuelConsumption*MFS.fuelUsageFactor*engineRPM/maxEngineRPM;
			if(parent.fuel <= 0 || engineRPM < 300){
				stopEngine(false);
			}else{
				fueled = true;
			}
		}else{
			if(engineRPM > 500 && parent.fuel > 0 && parent.throttle > 5 && engineEngaged){
				MFS.proxy.playSound(this, "mfs:" + engineStartingSoundName, 1, 1);
				engineOn=true;
			}
		}
		
		propeller = parent.getPropellerForEngine(this.UUID);
		if(starterState > 0){
			--starterState;
			parent.fuel -= this.fuelConsumption*MFS.fuelUsageFactor;
			if(engineRPM < 600){
				engineRPM = Math.min(engineRPM+starterPower, 600);
			}else{
				engineRPM = Math.max(engineRPM-starterPower, 600);
			}
		}
				
		if(fueled){
			if(propeller != null){
				engineRPM += (parent.throttle/100F*maxEngineRPM-engineRPM)/10 + (parent.velocity - 0.0254*propeller.pitch * engineRPM/60/20 - this.getPropellerForcePenalty())*15;
				if(propeller.diameter > 80 && engineRPM < 300 && parent.throttle >= 15){
					engineRPM = 300;
				}
			}else{
				engineRPM += (parent.throttle/100F*maxEngineRPM-engineRPM)/10;
			}
		}else{
			if(internalFuel > 0){
				if(engineRPM < 100){
					internalFuel = 0;
				}else{
					--internalFuel;
				}
			}
			if(propeller != null){
				engineRPM = Math.max(engineRPM + (parent.velocity - 0.0254*propeller.pitch * engineRPM/60/20)*15 - 10, 0);
			}else{
				engineRPM = Math.max(engineRPM - 10, 0);
			}
		}
		if(propeller != null){
			propeller.engineRPM = this.engineRPM;
		}
		engineSound = MFS.proxy.updateEngineSound(engineSound, this);
	}
	
	@Override
	public void setDead(){
		super.setDead();
		engineOn=false;
		fueled=false;
		internalFuel=0;
		engineSound = MFS.proxy.updateEngineSound(engineSound, this);
	}
	
	public void stopEngine(boolean switchedOff){
		engineEngaged = !switchedOff;
		if(engineOn){
			internalFuel = 100;
			engineOn = false;
			fueled = false;
			MFS.proxy.playSound(this, "mfs:" + engineStartingSoundName, 1, 1);
		}
	}
	
	public void startEngine(){
		engineEngaged = true;
		if(starterState==0){
			MFS.proxy.playSound(this, "mfs:" + engineCrankingSoundName, 1, 1);
			this.starterState += starterIncrement;
		}
	}
	
	public EngineSound getEngineSound(){
		if(worldObj.isRemote){
			return new EngineSound(new ResourceLocation("mfs:" + engineRunningSoundName), this, 0.5F, 2000F);
		}else{
			return null;
		}
	}
	
	protected abstract double getPropellerForcePenalty();
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.engineOn=tagCompound.getBoolean("engineOn");
		this.maxEngineRPM=tagCompound.getInteger("maxEngineRPM");
		this.fuelConsumption=tagCompound.getFloat("fuelConsumption");
		this.engineRPM=tagCompound.getDouble("engineRPM");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("engineOn", this.engineOn);
		tagCompound.setInteger("maxEngineRPM", this.maxEngineRPM);
		tagCompound.setFloat("fuelConsumption", this.fuelConsumption);
		tagCompound.setDouble("engineRPM", this.engineRPM);
	}
}
