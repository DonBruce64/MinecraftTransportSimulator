package minecraftflightsimulator.entities.parts;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.packets.control.EnginePacket;
import minecraftflightsimulator.sounds.EngineSound;
import minecraftflightsimulator.utilities.ConfigSystem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public abstract class EntityEngine extends EntityChild{
	protected EntityVehicle vehicle;
	
	public EngineTypes type;
	public boolean engineOn;
	public int internalFuel;
	public double engineRPM;
	public double engineTemp = 20;
	public double hours;
	
	protected boolean engineEngaged;
	protected boolean electricStarterEngaged;
	protected byte starterLevel;
	protected int maxEngineRPM;
	protected float fuelConsumption;
	private EngineSound engineSound;

	public EntityEngine(World world){
		super(world);
	}

	public EntityEngine(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode, EngineTypes type){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, type.size, type.size, propertyCode);
		this.maxEngineRPM = (propertyCode/((int) 100))*100;
		this.fuelConsumption = (propertyCode%100)/10F;
		this.type = type;
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
		vehicle.electricUsage -= 0.01*engineRPM/maxEngineRPM;
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

			vehicle.fuel -= this.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor")*engineRPM/maxEngineRPM;
			if(vehicle.fuel <= 0 || engineRPM < 300 || isLiquidAt(posX, posY, posZ)){
				MFS.proxy.playSound(this, "mfs:engine_starting", 1, 1);
				vehicle.fuel = 0;
				stopEngine(false);
			}
		}else{
			if(engineRPM > 500){
				if(vehicle.fuel > 0 && vehicle.throttle > 5 && engineEngaged && !isLiquidAt(posX, posY + 0.25, posZ)){
					MFS.proxy.playSound(this, "mfs:engine_starting", 1, 1);
					engineOn=true;
					if(!worldObj.isRemote){
						MFS.MFSNet.sendToAll(new EnginePacket(parent.getEntityId(), (byte) 4, this.getEntityId()));
					}
				}
			}
			if(internalFuel > 0){
				if(internalFuel == 100){
					MFS.proxy.playSound(this, "mfs:engine_starting", 1, 1);
				}
				--internalFuel;
				if(engineRPM < 1000){
					internalFuel = 0;
				}
			}
		}
		
		if(electricStarterEngaged && starterLevel == 0){
			if(vehicle.electricPower > 2){
				starterLevel += type.starterIncrement;
			}
		}
		if(starterLevel > 0){
			if(starterLevel == type.starterIncrement){
				if(vehicle.electricPower > 6 || !electricStarterEngaged){
					MFS.proxy.playSound(this, "mfs:" + type.engineCrankingSoundName, 1, 1);
				}else{
					MFS.proxy.playSound(this, "mfs:" + type.engineCrankingSoundName, 1, (float) (vehicle.electricPower/8F));
				}
			}
			--starterLevel;
			if(electricStarterEngaged){
				vehicle.electricUsage += 0.01F;
				vehicle.fuel -= this.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor");
			}
			if(engineRPM < 600){
				engineRPM = Math.min(engineRPM+type.starterPower, 600);
			}else{
				engineRPM = Math.max(engineRPM-type.starterPower, 600);
			}
		}
		engineSound = MFS.proxy.updateEngineSoundAndSmoke(engineSound, this);
	}

	@Override
	public void setDead(){
		super.setDead();
		engineOn=false;
		internalFuel = 0;
		engineSound = MFS.proxy.updateEngineSoundAndSmoke(engineSound, this);
	}
	
	public void setElectricStarterState(boolean state){
		if(state){engineEngaged = true;}
		electricStarterEngaged = state;
	}
	
	public boolean handStartEngine(){
		if(starterLevel==0){
			this.starterLevel += type.starterIncrement;
			engineEngaged = true;
			return true;
		}else{
			return false;
		}
	}
	
	public void stopEngine(boolean switchedOff){
		engineEngaged = !switchedOff;
		if(engineOn){
			engineOn = false;
			internalFuel = 100;
		}
	}
	
	protected void explodeEngine(){
		worldObj.newExplosion(this, posX, posY, posZ, 1F, true, true);
		this.parent.removeChild(this.UUID, false);
	}
	
	public EngineSound getEngineSound(){
		if(worldObj.isRemote){
			return new EngineSound(new ResourceLocation("mfs:" + type.engineRunningSoundName), this, 2000F);
		}else{
			return null;
		}
	}
	
	public double[] getEngineProperties(){
		return new double[] {this.engineTemp, this.engineRPM, this.maxEngineRPM};
	}
	
	public enum EngineTypes{
		PLANE_SMALL((byte) 4, (byte) 50, 1.0F, "small_engine_running", "small_engine_cranking", (short) 2805, (short) 3007), 
		PLANE_LARGE((byte) 22, (byte) 25, 1.2F, "large_engine_running", "large_engine_cranking", (short) 2907, (short) 3210),
		HELICOPTER((byte) 100, (byte) 100, 1.2F, "helicopter_engine_running", "helicopter_engine_cranking", (short) 3500, (short) 3700),
		VEHICLE((byte) 100, (byte) 100, 1.2F, "vehicle_engine_running", "vehicle_engine_cranking", (short) 3500, (short) 3700);
		//TODO find other sounds
		
		public final byte starterIncrement;
		public final byte starterPower;
		public final float size;
		public final String engineRunningSoundName;
		public final String engineCrankingSoundName;
		private short[] subtypes;
		private EngineTypes(byte starterIncrement, byte starterPower, float size, String engineRunningSoundName, String engineCrankingSoundName, short... subtypes){
			this.starterIncrement = starterIncrement;
			this.starterPower = starterPower;
			this.size = size;
			this.engineRunningSoundName = engineRunningSoundName;
			this.engineCrankingSoundName = engineCrankingSoundName;
			this.subtypes = subtypes;
		}
		
		public short[] getSubtypes(){
			return subtypes;
		}
	}
		
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.type=EngineTypes.values()[tagCompound.getByte("type")];
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
		tagCompound.setByte("type", (byte) this.type.ordinal());
		tagCompound.setBoolean("engineOn", this.engineOn);
		tagCompound.setInteger("maxEngineRPM", this.maxEngineRPM);
		tagCompound.setFloat("fuelConsumption", this.fuelConsumption);
		tagCompound.setDouble("engineRPM", this.engineRPM);
		tagCompound.setDouble("engineTemp", this.engineTemp);
		tagCompound.setDouble("hours", this.hours);
	}
}
