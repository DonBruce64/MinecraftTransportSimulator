package minecraftflightsimulator.entities.parts;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.minecrafthelpers.ItemStackHelper;
import minecraftflightsimulator.sounds.EngineSound;
import minecraftflightsimulator.systems.ConfigSystem;
import minecraftflightsimulator.systems.SFXSystem;
import minecraftflightsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public abstract class EntityEngine extends EntityChild implements SFXEntity{
	protected EntityVehicle vehicle;
	
	//NBT data
	public EngineTypes type;
	public boolean oilLeak;
	public boolean fuelLeak;
	public boolean brokenStarter;
	public int maxRPM;
	public int maxSafeRPM;
	public float fuelConsumption;
	public double hours;
	
	//Runtime data
	public EngineStates state;
	public byte starterLevel;
	public int internalFuel;
	public double fuelFlow;
	public double RPM;
	public double temp = 20;
	public double oilPressure = 90;
	private double ambientTemp;
	private double engineHeat;
	private double coolingFactor;
	private EngineSound engineSound;
	
	//Constants
	public static final float engineStallRPM = 300;
	public static final float engineStartRPM = 500;
	public static final float engineColdTemp = 30F;
	public static final float engineOverheatTemp1 = 115.556F;
	public static final float engineOverheatTemp2 = 121.111F;
	public static final float engineFailureTemp = 132.222F;
	public static final float engineOilDanger = 40F;
	public static final float engineOilCritical = 10F;

	public EntityEngine(World world){
		super(world);
	}

	public EntityEngine(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, EngineTypes.values()[propertyCode].size, EngineTypes.values()[propertyCode].size, propertyCode);
		this.type = EngineTypes.values()[propertyCode];
		this.state = EngineStates.ENGINE_OFF;
	}
	
	@Override
	public void setNBTFromStack(ItemStack stack){
		NBTTagCompound stackNBT = ItemStackHelper.getStackNBT(stack);
		oilLeak=stackNBT.getBoolean("oilLeak");
		fuelLeak=stackNBT.getBoolean("fuelLeak");
		brokenStarter=stackNBT.getBoolean("brokenStarter");
		hours = stackNBT.getDouble("hours");
		maxRPM = stackNBT.getInteger("maxRPM");
		maxSafeRPM = stackNBT.getInteger("maxSafeRPM");
		fuelConsumption = stackNBT.getFloat("fuelConsumption");
	}
	
	@Override
	public ItemStack getItemStack(){
		ItemStack engineStack = new ItemStack(MFSRegistry.engine, 1, type.ordinal());
		NBTTagCompound tag = new NBTTagCompound();
		tag.setBoolean("oilLeak", this.oilLeak);
		tag.setBoolean("fuelLeak", this.fuelLeak);
		tag.setBoolean("brokenStarter", this.brokenStarter);
		tag.setInteger("maxRPM", maxRPM);
		tag.setInteger("maxSafeRPM", maxSafeRPM);
		tag.setFloat("fuelConsumption", fuelConsumption);
		tag.setDouble("hours", hours);
		ItemStackHelper.setStackNBT(engineStack, tag);
		return engineStack;
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(isDamageWrench(source)){
				return true;
			}
			if(source.isExplosion()){
				hours += damage*10;
				if(!oilLeak)oilLeak = Math.random() < ConfigSystem.getDoubleConfig("EngineLeakProbability")*10;
				if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.getDoubleConfig("EngineLeakProbability")*10;
				if(!brokenStarter)brokenStarter = Math.random() < 0.05;
			}else{
				hours += damage;
				if(source.isProjectile()){
					if(!oilLeak)oilLeak = Math.random() < ConfigSystem.getDoubleConfig("EngineLeakProbability");
					if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.getDoubleConfig("EngineLeakProbability");
				}
			}
			this.sendDataToClient();
		}
		return true;
    }
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		vehicle = (EntityVehicle) this.parent;
		fuelFlow = 0;
		
		if(isBurning()){
			hours += 0.1;
			if(BlockHelper.isPositionInLiquid(worldObj, posX, posY + 0.25, posZ)){
				temp -= 0.3;
			}else{
				temp += 0.3;
			}
			if(temp > engineFailureTemp){
				explodeEngine();
				return;
			}
		}
		
		//Check to see if electric or hand starter can keep running.
		if(state.esOn){
			if(starterLevel == 0){
				if(vehicle.electricPower > 2){
					starterLevel += type.starterIncrement;
					if(vehicle.electricPower > 6){
						MFS.proxy.playSound(this, "mfs:" + type.engineCrankingSoundName, 1, 1);
					}else{
						MFS.proxy.playSound(this, "mfs:" + type.engineCrankingSoundName, 1, (float) (vehicle.electricPower/8F));
					}
				}
			}
			if(starterLevel > 0){
				vehicle.electricUsage += 0.01F;
				if(vehicle.fuel > this.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor")){
					vehicle.fuel -= this.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor");
					fuelFlow += this.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor");
				}
			}
		}else if(state.hsOn){
			if(starterLevel == 0){
				state = state.magnetoOn ? EngineStates.MAGNETO_ON_STARTERS_OFF : EngineStates.ENGINE_OFF;
			}
		}
		
		if(starterLevel > 0){
			--starterLevel;
			if(RPM < 600){
				RPM = Math.min(RPM+type.starterPower, 600);
			}else{
				RPM = Math.max(RPM-type.starterPower, 600);
			}
		}
		//Idle 100-110
		//Use 140-150
		
		ambientTemp = 25*worldObj.getBiomeGenForCoords((int) this.posX, (int) this.posZ).temperature - 5*(Math.pow(2, posY/400) - 1);
		coolingFactor = 0.001 + vehicle.velocity/500F;
		temp -= (temp - ambientTemp)*coolingFactor;
		vehicle.electricUsage -= 0.01*RPM/maxRPM;
		if(state.running){
			//First part is temp affect on oil, second is engine oil pump.
			oilPressure = Math.min(90 - temp/10, oilPressure + RPM/500 - 0.5*(oilLeak ? 5F : 1F)*(oilPressure/engineOilDanger));
			if(oilPressure < engineOilDanger){
				temp += Math.max(0, (RPM/250)/20);
				hours += 0.01;
			}else{
				temp += Math.max(0, (RPM/400 - temp/(engineColdTemp*2))/20);
				hours += 0.001;	
			}
			if(RPM > engineStartRPM*1.5 && temp < engineColdTemp){//Not warmed up
				hours += 0.001*(RPM/engineStartRPM - 1);
			}
			if(RPM > getMaxSafeRPM(maxRPM)){//Too fast
				hours += 0.001*(RPM - getMaxSafeRPM(maxRPM))/10F;
				temp += (RPM - getMaxSafeRPM(maxRPM))/1000;
			}
			if(temp > engineOverheatTemp1){//Too hot
				hours += 0.001*(temp - engineOverheatTemp1);
				if(temp > engineFailureTemp){
					explodeEngine();
				}
			}
			if(fuelLeak && !worldObj.isRemote && temp > engineOverheatTemp1 && RPM > maxSafeRPM * 0.8){
				setFire(10);
			}
			
			if(hours > 200){
				if(Math.random() < hours/(200*1200)){
					RPM -= 100;
					//TODO get sputter sound.
					MFS.proxy.playSound(this, "mfs:engine_sputter", 1, 1);
				}
			}

			fuelFlow = Math.min(this.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor")*RPM*(fuelLeak ? 1.5F : 1.0F)/maxRPM, vehicle.fuel);
			vehicle.fuel -= fuelFlow;
			if(vehicle.fuel == 0 && fuelConsumption != 0){
				internalFuel = 100;
				stallEngine();
			}else if(RPM < engineStallRPM){
				internalFuel = 100;
				stallEngine();
			}else if(BlockHelper.isPositionInLiquid(worldObj, posX, posY, posZ)){
				MFS.proxy.playSound(this, "mfs:engine_starting", 1, 1);
				stallEngine();
			}
		}else{
			oilPressure = 0;
			if(RPM > engineStartRPM){
				if(vehicle.fuel > 0 || fuelConsumption == 0){
					if(!BlockHelper.isPositionInLiquid(worldObj, posX, posY + 0.25, posZ)){
						if(state.magnetoOn){
							startEngine();
						}
					}
				}
			}
			
			//Internal fuel is used for engine sound wind down.  NOT used for power.
			if(internalFuel > 0){
				--internalFuel;
				if(RPM < 1000){
					internalFuel = 0;
				}
			}
		}
		MFS.proxy.updateSFXEntity(this, worldObj);
	}
	
	public void setMagnetoStatus(boolean on){
		if(on){
			if(state.equals(EngineStates.MAGNETO_OFF_ES_ON)){
				state = EngineStates.MAGNETO_ON_ES_ON;
			}else if(state.equals(EngineStates.MAGNETO_OFF_HS_ON)){
				state = EngineStates.MAGNETO_ON_HS_ON;
			}else if(state.equals(EngineStates.ENGINE_OFF)){
				state = EngineStates.MAGNETO_ON_STARTERS_OFF;
			}
		}else{
			if(state.equals(EngineStates.MAGNETO_ON_ES_ON)){
				state = EngineStates.MAGNETO_OFF_ES_ON;
			}else if(state.equals(EngineStates.MAGNETO_ON_HS_ON)){
				state = EngineStates.MAGNETO_OFF_HS_ON;
			}else if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
				state = EngineStates.ENGINE_OFF;
			}else if(state.equals(EngineStates.RUNNING)){
				state = EngineStates.ENGINE_OFF;
				internalFuel = 100;
				MFS.proxy.playSound(this, "mfs:engine_starting", 1, 1);
			}
		}
	}
	
	public void setElectricStarterStatus(boolean engaged){
		if(!brokenStarter){
			if(engaged){
				if(state.equals(EngineStates.ENGINE_OFF)){
					state = EngineStates.MAGNETO_OFF_ES_ON;
				}else if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
					state = EngineStates.MAGNETO_ON_ES_ON;
				}else if(state.equals(EngineStates.RUNNING)){
					state =  EngineStates.RUNNING_ES_ON;
				}
			}else{
				if(state.equals(EngineStates.MAGNETO_OFF_ES_ON)){
					state = EngineStates.ENGINE_OFF;
				}else if(state.equals(EngineStates.MAGNETO_ON_ES_ON)){
					state = EngineStates.MAGNETO_ON_STARTERS_OFF;
				}else if(state.equals(EngineStates.RUNNING_ES_ON)){
					state = EngineStates.RUNNING;
				}
			}
		}
	}
	
	public void handStartEngine(){
		if(state.equals(EngineStates.ENGINE_OFF)){
			state = EngineStates.MAGNETO_OFF_HS_ON;
		}else if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
			state = EngineStates.MAGNETO_ON_HS_ON;
		}else if(state.equals(EngineStates.RUNNING)){
			state = EngineStates.RUNNING_HS_ON;
		}else{
			return;
		}
		starterLevel += type.starterIncrement;
		MFS.proxy.playSound(this, "mfs:" + type.engineCrankingSoundName, 1, 1);
	}
	
	private void startEngine(){
		MFS.proxy.playSound(this, "mfs:engine_starting", 1, 1);
		if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
			state = EngineStates.RUNNING;
		}else if(state.equals(EngineStates.MAGNETO_ON_ES_ON)){
			state = EngineStates.RUNNING_ES_ON;
		}else if(state.equals(EngineStates.MAGNETO_ON_HS_ON)){
			state = EngineStates.RUNNING_HS_ON;
		}
		starterLevel = 0;
		oilPressure = 60;
		if(!worldObj.isRemote){
			this.sendDataToClient();
		}
	}
	
	private void stallEngine(){
		MFS.proxy.playSound(this, "mfs:engine_starting", 1, 1);
		if(state.equals(EngineStates.RUNNING)){
			state = EngineStates.MAGNETO_ON_STARTERS_OFF;
		}else if(state.equals(EngineStates.RUNNING_ES_ON)){
			state = EngineStates.MAGNETO_ON_ES_ON;
		}else if(state.equals(EngineStates.RUNNING_HS_ON)){
			state = EngineStates.MAGNETO_ON_HS_ON;
		}
		if(!worldObj.isRemote){
			this.sendDataToClient();
		}
	}
	
	protected void explodeEngine(){
		worldObj.newExplosion(this, posX, posY, posZ, 1F, true, true);
		this.parent.removeChild(this.UUID, false);
	}
	
	public static int getMaxSafeRPM(int maxRPM){
		return (maxRPM - (maxRPM - 2500)/2);
	}
	

	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getNewSound(){
		return type != null ? new EngineSound(new ResourceLocation("mfs:" + type.engineRunningSoundName), this, 2000F) : null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getCurrentSound(){
		return engineSound;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setCurrentSound(MovingSound sound){
		engineSound = (EngineSound) sound;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSoundBePlaying(){
		return (state.running || internalFuel > 0) && !isDead;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(Minecraft.getMinecraft().effectRenderer != null){
			if(temp > engineOverheatTemp1){
				Minecraft.getMinecraft().theWorld.spawnParticle("smoke", posX, posY + 0.5, posZ, 0, 0.15, 0);
				if(temp > engineOverheatTemp2){
					Minecraft.getMinecraft().theWorld.spawnParticle("largesmoke", posX, posY + 0.5, posZ, 0, 0.15, 0);
				}
			}
			if(parent != null){
				if(oilLeak){
					if(ticksExisted%20 == 0){
						Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.OilDropParticleFX(worldObj, posX - 0.25*Math.sin(Math.toRadians(parent.rotationYaw)), posY, posZ + 0.25*Math.cos(Math.toRadians(parent.rotationYaw))));
					}
				}
				if(fuelLeak){
					if((ticksExisted + 5)%20 == 0){
						Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.FuelDropParticleFX(worldObj, posX, posY, posZ));
					}
				}
			}
		}
	}
	
	public enum EngineTypes{
		PLANE_SMALL((byte) 4, (byte) 50, 1.0F, "small_engine_running", "small_engine_cranking", new EngineProperties[]{new EngineProperties(2700, 0.3F), new EngineProperties(2900, 0.4F)}), 
		PLANE_LARGE((byte) 22, (byte) 25, 1.2F, "large_engine_running", "large_engine_cranking", new EngineProperties[]{new EngineProperties(2000, 0.5F), new EngineProperties(2400, 0.7F)}),
		HELICOPTER((byte) 100, (byte) 100, 1.2F, "helicopter_engine_running", "helicopter_engine_cranking", new EngineProperties[]{new EngineProperties(500, 0.1F), new EngineProperties(600, 0.15F)}),
		VEHICLE((byte) 100, (byte) 100, 1.2F, "vehicle_engine_running", "vehicle_engine_cranking", new EngineProperties[]{new EngineProperties(5500, 0.2F), new EngineProperties(6500, 0.4F)});
		//TODO find other sounds
		
		public final byte starterIncrement;
		public final byte starterPower;
		public final float size;
		public final String engineRunningSoundName;
		public final String engineCrankingSoundName;
		public final EngineProperties[] defaultSubtypes;
		private EngineTypes(byte starterIncrement, byte starterPower, float size, String engineRunningSoundName, String engineCrankingSoundName, EngineProperties[] defaultSubtypes){
			this.starterIncrement = starterIncrement;
			this.starterPower = starterPower;
			this.size = size;
			this.engineRunningSoundName = engineRunningSoundName;
			this.engineCrankingSoundName = engineCrankingSoundName;
			this.defaultSubtypes = defaultSubtypes;
		}
		
		public static class EngineProperties{
			public final int maxRPM;
			public final float fuelConsumption;
			
			public EngineProperties(int maxRPM, float fuelConsumption){
				this.maxRPM = maxRPM;
				this.fuelConsumption = fuelConsumption;
			}
		}
	}
	
	/**
	 * Engine states have 5 components.  Magneto, electric starter, hand starter, power status, and drowned out.
	 */
	public enum EngineStates{
		ENGINE_OFF(false, false, false, false),
		MAGNETO_ON_STARTERS_OFF(true, false, false, false),
		MAGNETO_OFF_ES_ON(false, true, false, false),
		MAGNETO_OFF_HS_ON(false, false, true, false),
		MAGNETO_ON_ES_ON(true, true, false, false),
		MAGNETO_ON_HS_ON(true, false, true, false),
		RUNNING(true, false, false, true),
		RUNNING_ES_ON(true, true, false, true),
		RUNNING_HS_ON(true, false, true, true);
		
		public final boolean magnetoOn;
		public final boolean esOn;
		public final boolean hsOn;
		public final boolean running;
		
		private EngineStates(boolean magnetoOn, boolean esOn, boolean hsOn, boolean running){
			this.magnetoOn = magnetoOn;
			this.esOn = esOn;
			this.hsOn = hsOn;
			this.running = running;
		}
	}
		
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.type=EngineTypes.values()[tagCompound.getByte("type")];
		this.state=EngineStates.values()[tagCompound.getByte("state")];
		this.oilLeak=tagCompound.getBoolean("oilLeak");
		this.fuelLeak=tagCompound.getBoolean("fuelLeak");
		this.brokenStarter=tagCompound.getBoolean("brokenStarter");
		this.maxRPM=tagCompound.getInteger("maxRPM");
		this.maxSafeRPM=tagCompound.getInteger("maxSafeRPM");
		this.fuelConsumption=tagCompound.getFloat("fuelConsumption");
		this.hours=tagCompound.getDouble("hours");
		this.RPM=tagCompound.getDouble("RPM");
		this.temp=tagCompound.getDouble("temp");
		this.oilPressure=tagCompound.getDouble("oilPressure");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setByte("type", (byte) this.type.ordinal());
		tagCompound.setByte("state", (byte) this.state.ordinal());
		tagCompound.setBoolean("oilLeak", this.oilLeak);
		tagCompound.setBoolean("fuelLeak", this.fuelLeak);
		tagCompound.setBoolean("brokenStarter", this.brokenStarter);
		tagCompound.setInteger("maxRPM", this.maxRPM);
		tagCompound.setInteger("maxSafeRPM", this.maxSafeRPM);
		tagCompound.setFloat("fuelConsumption", this.fuelConsumption);
		tagCompound.setDouble("hours", this.hours);
		tagCompound.setDouble("RPM", this.RPM);
		tagCompound.setDouble("temp", this.temp);
		tagCompound.setDouble("oilPressure", this.oilPressure);
	}
}
