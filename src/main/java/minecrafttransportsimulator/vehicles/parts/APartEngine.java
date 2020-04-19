package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.parts.PacketPartEngineDamage;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.systems.VehicleEffectsSystem;
import minecrafttransportsimulator.systems.VehicleEffectsSystem.FXPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class APartEngine extends APart implements FXPart{
	
	//NBT data
	public boolean isCreative;
	public boolean oilLeak;
	public boolean fuelLeak;
	public boolean brokenStarter;
	public double hours;
	
	//Runtime data
	public EngineStates state = EngineStates.ENGINE_OFF;
	public boolean backfired;
	public byte starterLevel;
	public int internalFuel;
	public double fuelFlow;
	public double RPM;
	public double temp = 20;
	public double oilPressure = 90;
	private double ambientTemp;
	private double coolingFactor;
	private Long lastTimeParticleSpawned = 0L;
	public APartEngine linkedEngine;
	
	//Rotation data.  Should be set by each engine type individually.
	protected double engineRotationLast;
	protected double engineRotation;
	protected double engineDriveshaftRotation;
	protected double engineDriveshaftRotationLast;
	
	//Constants
	public static final float engineColdTemp = 30F;
	public static final float engineOverheatTemp1 = 115.556F;
	public static final float engineOverheatTemp2 = 121.111F;
	public static final float engineFailureTemp = 132.222F;
	public static final float engineOilDanger = 40F;
	public final float engineStallRPM;
	public final float engineStartRPM;

	
	public APartEngine(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		engineStallRPM = definition.engine.maxRPM < 15000 ? 300 : 1500;
		engineStartRPM = definition.engine.maxRPM < 15000 ? 500 : 2000;
		if(dataTag.hasKey("engineState")){
			this.state = EngineStates.values()[dataTag.getByte("engineState")];
			if(state.running && vehicle.world.isRemote){
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_running", true));
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_supercharger", true));
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running1, true));
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running2, true));
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running3, true));
			}
		}else{
			this.state = EngineStates.ENGINE_OFF;
		}
		
		isCreative = dataTag.getBoolean("isCreative");
		oilLeak = dataTag.getBoolean("oilLeak");
		fuelLeak = dataTag.getBoolean("fuelLeak");
		brokenStarter = dataTag.getBoolean("brokenStarter");
		hours = dataTag.getDouble("hours");
		RPM = dataTag.getDouble("rpm");
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(source.isExplosion()){
			hours += damage*20*ConfigSystem.configObject.general.engineHoursFactor.value;
			if(!oilLeak)oilLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value*10;
			if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value*10;
			if(!brokenStarter)brokenStarter = Math.random() < 0.05;
			MTS.MTSNet.sendToAll(new PacketPartEngineDamage(this, (float) (damage*10*ConfigSystem.configObject.general.engineHoursFactor.value)));
		}else{
			hours += damage*2*ConfigSystem.configObject.general.engineHoursFactor.value;
			if(source.isProjectile()){
				if(!oilLeak)oilLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value;
				if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value;
			}
			MTS.MTSNet.sendToAll(new PacketPartEngineDamage(this, (float) (damage*ConfigSystem.configObject.general.engineHoursFactor.value)));
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		fuelFlow = 0;
		
		//Check to see if we are linked and need to equalize power between us and another engine.
		if(linkedEngine != null){
			if(linkedEngine.partPos.distanceTo(this.partPos) > 16){
				linkedEngine.linkedEngine = null;
				linkedEngine = null;
				if(vehicle.world.isRemote){
					MTS.MTSNet.sendToAllAround(new PacketChat("interact.jumpercable.linkdropped"), new TargetPoint(vehicle.world.provider.getDimension(), partPos.x, partPos.y, partPos.z, 16));
				}
			}else if(vehicle.electricPower + 0.5 < linkedEngine.vehicle.electricPower){
				linkedEngine.vehicle.electricPower -= 0.005F;
				vehicle.electricPower += 0.005F;
			}else if(vehicle.electricPower > linkedEngine.vehicle.electricPower + 0.5){
				vehicle.electricPower -= 0.005F;
				linkedEngine.vehicle.electricPower += 0.005F;
			}else{
				linkedEngine.linkedEngine = null;
				linkedEngine = null;
				if(vehicle.world.isRemote){
					MTS.MTSNet.sendToAllAround(new PacketChat("interact.jumpercable.powerequal"), new TargetPoint(vehicle.world.provider.getDimension(), partPos.x, partPos.y, partPos.z, 16));
				}
			}
		}
		
		//Check to see if electric or hand starter can keep running.
		if(state.esOn){
			if(starterLevel == 0){
				if(vehicle.electricPower > 2){
					starterLevel += 4;
				}else{
					setElectricStarterStatus(false);
				}
			}
			if(starterLevel > 0){
				if(!isCreative){
					vehicle.electricUsage += 0.05F;
				}
				if(vehicle.fuel > getTotalFuelConsumption()*ConfigSystem.configObject.general.fuelUsageFactor.value && !isCreative){
					vehicle.fuel -= getTotalFuelConsumption()*ConfigSystem.configObject.general.fuelUsageFactor.value;
					fuelFlow += getTotalFuelConsumption()*ConfigSystem.configObject.general.fuelUsageFactor.value;
				}
			}
		}else if(state.hsOn){
			if(starterLevel == 0){
				state = state.magnetoOn ? EngineStates.MAGNETO_ON_STARTERS_OFF : EngineStates.ENGINE_OFF;
			}
		}
		
		if(starterLevel > 0){
			--starterLevel;
			if(RPM < engineStartRPM*1.2){
				RPM = Math.min(RPM + definition.engine.starterPower, engineStartRPM*1.2);
			}else{
				RPM = Math.max(RPM - definition.engine.starterPower, engineStartRPM*1.2);
			}
		}
		
		ambientTemp = 25*vehicle.world.getBiome(vehicle.getPosition()).getTemperature(vehicle.getPosition()) - 5*(Math.pow(2, vehicle.posY/400) - 1);
		coolingFactor = 0.001 - ((definition.engine.superchargerEfficiency/1000F)*(RPM/2000F)) + Math.abs(vehicle.velocity)/500F;
		temp -= (temp - ambientTemp)*coolingFactor;
		vehicle.electricUsage -= state.running ? 0.05*RPM/definition.engine.maxRPM : 0;
		
		if(state.running){
			//First part is temp affect on oil, second is engine oil pump.
			oilPressure = Math.min(90 - temp/10, oilPressure + RPM/engineStartRPM - 0.5*(oilLeak ? 5F : 1F)*(oilPressure/engineOilDanger));
			if(oilPressure < engineOilDanger){
				temp += Math.max(0, (20*RPM/definition.engine.maxRPM)/20);
				hours += 0.01*getTotalWearFactor();
			}else{
				temp += Math.max(0, (7*RPM/definition.engine.maxRPM - temp/(engineColdTemp*2))/20);
				hours += 0.001*getTotalWearFactor();	
			}
			if(RPM > engineStartRPM*1.5 && temp < engineColdTemp){//Not warmed up
				hours += 0.001*(RPM/engineStartRPM - 1)*getTotalWearFactor();
			}
			if(RPM > getSafeRPMFromMax(this.definition.engine.maxRPM)){//Too fast
				hours += 0.001*(RPM - getSafeRPMFromMax(this.definition.engine.maxRPM))/10F*getTotalWearFactor();
			}
			if(temp > engineOverheatTemp1){//Too hot
				hours += 0.001*(temp - engineOverheatTemp1)*getTotalWearFactor();
				if(temp > engineFailureTemp && !vehicle.world.isRemote && !isCreative){
					explodeEngine();
				}
			}
			
			if(hours > 200 && !vehicle.world.isRemote){
				if(Math.random() < hours/10000*(getSafeRPMFromMax(this.definition.engine.maxRPM)/(RPM+getSafeRPMFromMax(this.definition.engine.maxRPM)/2))){
					backfireEngine();
				}
			}

			if(!isCreative && !vehicle.fluidName.isEmpty()){
				if(!ConfigSystem.configObject.fuel.fuels.containsKey(definition.engine.fuelType)){					
					throw new IllegalArgumentException("ERROR: Engine:" + definition.packID + ":" + definition.systemName + " wanted fuel configs for fuel of type:" + definition.engine.fuelType + ", but these do not exist in the config file.  Fuels currently in the file are:" + ConfigSystem.configObject.fuel.fuels.keySet().toString() + "If you are on a server, this means the server and client configs are not the same.  If this is a modpack, TELL THE AUTHOR IT IS BORKEN!");
				}else if(!ConfigSystem.configObject.fuel.fuels.get(definition.engine.fuelType).containsKey(vehicle.fluidName)){
					//Clear out the fuel from this vehicle as it's the wrong type.
					vehicle.fuel = 0;
					vehicle.fluidName = "";
				}else{
					fuelFlow = getTotalFuelConsumption()*ConfigSystem.configObject.general.fuelUsageFactor.value/ConfigSystem.configObject.fuel.fuels.get(definition.engine.fuelType).get(vehicle.fluidName)*RPM*(fuelLeak ? 1.5F : 1.0F)/definition.engine.maxRPM;
					vehicle.fuel -= fuelFlow;
				}
			}
			
			if(!vehicle.world.isRemote){
				if(vehicle.fuel == 0 && !isCreative){
					stallEngine(PacketEngineTypes.FUEL_OUT);
				}else if(RPM < engineStallRPM){
					stallEngine(PacketEngineTypes.TOO_SLOW);
				}else if(isInLiquid()){
					stallEngine(PacketEngineTypes.DROWN);
				}
			}
		}else{
			oilPressure = 0;
			if(RPM > engineStartRPM){
				if(vehicle.fuel > 0 || isCreative){
					if(!isInLiquid()){
						if(state.magnetoOn && !vehicle.world.isRemote){
							startEngine();
						}
					}
				}
			}
			
			//Internal fuel is used for engine sound wind down.  NOT used for power.
			if(internalFuel > 0){
				--internalFuel;
				if(RPM < engineStartRPM){
					internalFuel = 0;
				}
			}
		}
		//If we are creative, set all our hours to 0 as they don't apply.
		if(isCreative){
			hours = 0;
		}
	}
	
	@Override
	public void removePart(){
		super.removePart();
		this.state = EngineStates.ENGINE_OFF;
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound partData = new NBTTagCompound();
		partData.setBoolean("isCreative", this.isCreative);
		partData.setBoolean("oilLeak", this.oilLeak);
		partData.setBoolean("fuelLeak", this.fuelLeak);
		partData.setBoolean("brokenStarter", this.brokenStarter);
		partData.setDouble("hours", hours);
		partData.setByte("engineState", (byte) this.state.ordinal());
		partData.setDouble("rpm", this.RPM);
		return partData;
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
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
				if(vehicle.world.isRemote){
					WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_stopping"));
				}
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
					if(vehicle.world.isRemote){
						WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_cranking", true));
					}
				}else if(state.equals(EngineStates.RUNNING)){
					state =  EngineStates.RUNNING_ES_ON;
					if(vehicle.world.isRemote){
						WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_cranking", true));
					}
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
		starterLevel += 4;
		if(vehicle.world.isRemote){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_cranking", true));
		}
	}
	
	public void backfireEngine(){
		RPM -= definition.engine.maxRPM < 15000 ? 100 : 500;
		if(!vehicle.world.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, PacketEngineTypes.BACKFIRE));
		}else{
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_sputter"));
			backfired = true;
		}
	}
	
	public void startEngine(){
		if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
			state = EngineStates.RUNNING;
		}else if(state.equals(EngineStates.MAGNETO_ON_ES_ON)){
			state = EngineStates.RUNNING_ES_ON;
		}else if(state.equals(EngineStates.MAGNETO_ON_HS_ON)){
			state = EngineStates.RUNNING;
		}
		starterLevel = 0;
		oilPressure = 60;
		if(!vehicle.world.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, PacketEngineTypes.START));
		}else{
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_starting"));
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_running", true));
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_supercharger", true));
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running1, true));
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running2, true));
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running3, true));
		}
	}
	
	public void stallEngine(PacketEngineTypes packetType){
		if(state.equals(EngineStates.RUNNING)){
			state = EngineStates.MAGNETO_ON_STARTERS_OFF;
		}else if(state.equals(EngineStates.RUNNING_ES_ON)){
			state = EngineStates.MAGNETO_ON_ES_ON;
		}else if(state.equals(EngineStates.RUNNING_HS_ON)){
			state = EngineStates.MAGNETO_ON_HS_ON;
		}
		if(!vehicle.world.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, packetType));
		}else{
			if(!packetType.equals(PacketEngineTypes.DROWN)){
				internalFuel = 100;
			}
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_stopping"));
		}
	}
	
	protected void explodeEngine(){
		if(ConfigSystem.configObject.damage.explosions.value){
			vehicle.world.newExplosion(vehicle, partPos.x, partPos.y, partPos.z, 1F, true, true);
		}else{
			vehicle.world.newExplosion(vehicle, partPos.x, partPos.y, partPos.z, 0F, true, true);
		}
		vehicle.removePart(this, true);
	}
	
	public static int getSafeRPMFromMax(int maxRPM){
		return maxRPM < 15000 ? maxRPM - (maxRPM - 2500)/2 : (int) (maxRPM/1.1);
	}
	//Get the total fuel consumption of this engine, to account for supercharged engines.
	public float getTotalFuelConsumption(){
		return definition.engine.fuelConsumption + definition.engine.superchargerFuelConsumption;
	}
	//Get the total wear factor to be applied to this engine, to account for supercharged engines.
	public double getTotalWearFactor(){
		if (definition.engine.superchargerEfficiency > 1.0F){
			return definition.engine.superchargerEfficiency*ConfigSystem.configObject.general.engineHoursFactor.value;
		}else{
			return ConfigSystem.configObject.general.engineHoursFactor.value;
		}
	}
	
	protected boolean isInLiquid(){
		return vehicle.world.getBlockState(new BlockPos(partPos.addVector(0, packVehicleDef.intakeOffset, 0))).getMaterial().isLiquid();
	}
	
	public double getEngineRotation(float partialTicks){
		return engineRotation + (engineRotation - engineRotationLast)*partialTicks;
	}
	
	public double getDriveshaftRotation(float partialTicks){
		return engineDriveshaftRotation + (engineDriveshaftRotation - engineDriveshaftRotationLast)*partialTicks;
	}
	
	public abstract double getForceOutput();
	
	@Override
	public void updateProviderSound(SoundInstance sound){
		super.updateProviderSound(sound);
		//Adjust cranking sound pitch to match RPM and stop looping if we are done cranking.
		//Adjust running sound to have pitch based on engine RPM.
		if(sound.soundName.endsWith("_cranking")){
			if(!state.esOn && !state.hsOn){
				sound.stop();
			}else{
				if (definition.engine.isCrankingNotPitched){
					sound.pitch = (float) (vehicle.electricPower/10);
					if (sound.pitch > 1) {
						sound.pitch = 1;
					}
				}else{
				sound.pitch = (float) (RPM/engineStartRPM);
				}
			}
		}else if(sound.soundName.endsWith("_running")){
			if(!state.running && internalFuel == 0){
				sound.stop();
			}else{
				//Pitch should be 0.35 at idle, with a 0.35 increase for every 2500 RPM, or every 25000 RPM for jet (high-revving) engines by default.
				//Pitch and volume are set in the engine JSON
				if (definition.engine.pitchRev > 0){
						sound.pitch = (float) (((definition.engine.pitchRev*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.pitchRev)+ definition.engine.pitchIdle);
						sound.volume = (float) (((definition.engine.volumeRev*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.volumeRev)+ definition.engine.volumeIdle);
					}else{
						sound.pitch = (float) (0.35*(1 + Math.max(0, (RPM - engineStartRPM))/(definition.engine.maxRPM < 15000 ? 500 : 5000)));
				}
			}
		}else if(sound.soundName.endsWith("_supercharger")){
			if(!state.running && internalFuel == 0){
				sound.stop();
				}else{
					if (definition.engine.scPitchRev > 0){
						sound.pitch = (float) (((definition.engine.scPitchRev*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.scPitchRev)+ definition.engine.scPitchIdle);
						sound.volume = (float) (((definition.engine.scVolumeRev*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.scVolumeRev)+ definition.engine.scVolumeIdle);
					}else{
						sound.volume = (float) RPM/definition.engine.maxRPM;
						sound.pitch = (float) (0.35*(1 + Math.max(0, (RPM - engineStartRPM))/(definition.engine.maxRPM < 15000 ? 500 : 5000)));
					}
				}
		}else if(sound.soundName.equals(definition.engine.custom_Running1)){
				if(!state.running && internalFuel == 0){
					sound.stop();
				}else{
					sound.pitch = (float) (((definition.engine.pitchRev1*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.pitchRev1)+ definition.engine.pitchIdle1);
					sound.volume = (float) (((definition.engine.volumeRev1*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.volumeRev1)+ definition.engine.volumeIdle1);
				}
		}else if(sound.soundName.equals(definition.engine.custom_Running2)){
				if(!state.running && internalFuel == 0){
					sound.stop();
				}else{
					sound.pitch = (float) (((definition.engine.pitchRev2*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.pitchRev2)+ definition.engine.pitchIdle2);
					sound.volume = (float) (((definition.engine.volumeRev2*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.volumeRev2)+ definition.engine.volumeIdle2);
				}
		}else if(sound.soundName.equals(definition.engine.custom_Running3)){
				if(!state.running && internalFuel == 0){
					sound.stop();
				}else{
					sound.pitch = (float) (((definition.engine.pitchRev3*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.pitchRev3)+ definition.engine.pitchIdle3);
					sound.volume = (float) (((definition.engine.volumeRev3*((1 + Math.max(0, ((RPM - engineStartRPM))/definition.engine.maxRPM))))- definition.engine.volumeRev3)+ definition.engine.volumeIdle3);
				}
			}
		
	}
	
	@Override
	public void restartSound(SoundInstance sound){
		if(sound.soundName.endsWith("_cranking")){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_cranking", true));
		}else if(sound.soundName.endsWith("_running")){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_running", true));
		}else if(sound.soundName.endsWith("_supercharger")){
		WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_supercharger", true));
		}else if(sound.soundName.equals(definition.engine.custom_Running1)){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running1, true));
		}else if(sound.soundName.equals(definition.engine.custom_Running2)){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running2, true));
		}else if(sound.soundName.equals(definition.engine.custom_Running3)){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.engine.custom_Running3, true));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(Minecraft.getMinecraft().effectRenderer != null){
			//Render engine smoke if we're overheating.
			if(temp > engineOverheatTemp1){
				Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, partPos.x, partPos.y + 0.5, partPos.z, 0, 0.15, 0, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F));
				if(temp > engineOverheatTemp2){
					Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, partPos.x, partPos.y + 0.5, partPos.z, 0, 0.15, 0, 0.0F, 0.0F, 0.0F, 2.5F, 1.0F));
				}
			}
			
			//Render exhaust smoke if we have any exhausts and are running.
			//If we are starting and have flames set, render those instead.
			if(packVehicleDef.exhaustPos != null && (state.running || (definition.engine.flamesOnStartup && state.esOn))){
				//Render a smoke for every cycle the exhaust makes.
				//Depending on the number of positions we have, render an exhaust for every one.
				//So for 1 position, we render 1 every 2 engine cycles (4 stroke), and for 4, we render 4.
				//Note that the rendering is offset for multi-position points to simulate the cylinders firing
				//in their aligned order.
				
				//Get timing information and particle information.
				long engineCycleTimeMills = (long) (2D*(1D/(RPM/60D/1000D)));
				long currentTime = System.currentTimeMillis();
				long camTime = currentTime%engineCycleTimeMills;
				
				float particleColor = (float) Math.max(1 - temp/engineColdTemp, 0);
				boolean singleExhaust = packVehicleDef.exhaustPos.length == 3;
				
				//Iterate through all the exhaust positions and fire them if it is time to do so.
				//We need to offset the time we are supposed to spawn by the cycle time for multi-point exhausts.
				//For single-point exhausts, we only fire if we didn't fire this cycle.
				for(int i=0; i<packVehicleDef.exhaustPos.length; i+=3){
					if(singleExhaust){
						if(lastTimeParticleSpawned + camTime > currentTime){
							continue;
						}
					}else{
						long camOffset = engineCycleTimeMills*3/packVehicleDef.exhaustPos.length;
						long camMin = (i/3)*camOffset;
						long camMax = camMin + camOffset;
						if(camTime < camMin || camTime > camMax || (lastTimeParticleSpawned > camMin && lastTimeParticleSpawned < camMax)){
							continue;
						}
					}
					
					Vec3d exhaustOffset = RotationSystem.getRotatedPoint(new Vec3d(packVehicleDef.exhaustPos[i], packVehicleDef.exhaustPos[i+1], packVehicleDef.exhaustPos[i+2]), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.getPositionVector());
					Vec3d velocityOffset = RotationSystem.getRotatedPoint(new Vec3d(packVehicleDef.exhaustVelocity[i], packVehicleDef.exhaustVelocity[i+1], packVehicleDef.exhaustVelocity[i+2]), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll);
					if(state.running){
						Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, exhaustOffset.x, exhaustOffset.y, exhaustOffset.z, velocityOffset.x/10D + 0.02 - Math.random()*0.04, velocityOffset.y/10D, velocityOffset.z/10D + 0.02 - Math.random()*0.04, particleColor, particleColor, particleColor, 1.0F, (float) Math.min((50 + hours)/500, 1)));
					}
					if(definition.engine.flamesOnStartup && state.esOn){
						Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.EngineFlameParticleFX(vehicle.world, exhaustOffset.x, exhaustOffset.y, exhaustOffset.z, velocityOffset.x/10D + 0.02 - Math.random()*0.04, velocityOffset.y/10D, velocityOffset.z/10D + 0.02 - Math.random()*0.04));
					}
					lastTimeParticleSpawned = singleExhaust ? currentTime : camTime;
				}
			}
			
			//Render oil and fuel leak particles.
			if(oilLeak){
				if(vehicle.ticksExisted%20 == 0){
					Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.OilDropParticleFX(vehicle.world, partPos.x - 0.25*Math.sin(Math.toRadians(vehicle.rotationYaw)), partPos.y, partPos.z + 0.25*Math.cos(Math.toRadians(vehicle.rotationYaw))));
				}
			}
			if(fuelLeak){
				if((vehicle.ticksExisted + 5)%20 == 0){
					Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.FuelDropParticleFX(vehicle.world, partPos.y, partPos.y, partPos.z));
				}
			}
			
			//If we backfired, render a few puffs.
			//Will be from the engine or the exhaust if we have any.
			if(backfired){
				backfired = false;
				if(packVehicleDef.exhaustPos != null){
					for(int i=0; i<packVehicleDef.exhaustPos.length; i+=3){
						Vec3d exhaustOffset = RotationSystem.getRotatedPoint(new Vec3d(packVehicleDef.exhaustPos[i], packVehicleDef.exhaustPos[i+1], packVehicleDef.exhaustPos[i+2]), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll).add(vehicle.getPositionVector());
						Vec3d velocityOffset = RotationSystem.getRotatedPoint(new Vec3d(packVehicleDef.exhaustVelocity[i], packVehicleDef.exhaustVelocity[i+1], packVehicleDef.exhaustVelocity[i+2]), vehicle.rotationPitch, vehicle.rotationYaw, vehicle.rotationRoll);
						for(byte j=0; j<5; ++j){
							Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, exhaustOffset.x, exhaustOffset.y, exhaustOffset.z, velocityOffset.x/10D + 0.07 - Math.random()*0.14, velocityOffset.y/10D, velocityOffset.z/10D + 0.07 - Math.random()*0.14, 0.0F, 0.0F, 0.0F, 2.5F, 1.0F));
						}
					}
				}else{
					for(byte i=0; i<5; ++i){
						Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, partPos.x, partPos.y + 0.5, partPos.z, 0.07 - Math.random()*0.14, 0.15, 0.07 - Math.random()*0.14, 0.0F, 0.0F, 0.0F, 2.5F, 1.0F));
					}
				}
			}
		}
	}
	
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
}
