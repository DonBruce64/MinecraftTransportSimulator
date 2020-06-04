package minecrafttransportsimulator.vehicles.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourceJet;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.PartEngine.EngineSound;
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
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Blimp;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Boat;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Car;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PartEngine extends APart implements FXPart{
	
	//State data.
	public boolean isCreative;
	public boolean oilLeak;
	public boolean fuelLeak;
	public boolean brokenStarter;
	public byte currentGear;
	public double hours;
	public double RPM;
	public double temp = 20;
	public double pressure;
	public EngineStates state = EngineStates.ENGINE_OFF;
	
	//Runtime calculated values.
	public double fuelFlow;
	public PartEngine linkedEngine;
	
	//Internal variables.
	private boolean startSounds;
	private boolean backfired;
	private boolean isPropellerInLiquid;
	private byte starterLevel;
	private byte shiftCooldown;
	private int internalFuel;
	private long lastTimeParticleSpawned;
	private float currentGearRatio;
	private float propellerGearboxRatio;
	private float lowestWheelVelocity;
	private float desiredWheelVelocity;
	private float wheelFriction;
	private double ambientTemp;
	private double coolingFactor;
	private double engineTargetRPM;
	private double engineRotation;
	private double prevEngineRotation;
	private double driveshaftRotation;
	private double prevDriveshaftRotation;
	private PartPropeller propeller;
	
	//Constants and static variables.
	private final int startRPM;
	private final int stallRPM;
	private static final float COLD_TEMP = 30F;
	private static final float OVERHEAT_TEMP_1 = 115.556F;
	private static final float OVERHEAT_TEMP_2 = 121.111F;
	private static final float FAILURE_TEMP = 132.222F;
	private static final float LOW_OIL_PRESSURE = 40F;
	
	
	public PartEngine(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		this.isCreative = dataTag.getBoolean("isCreative");
		this.oilLeak = dataTag.getBoolean("oilLeak");
		this.fuelLeak = dataTag.getBoolean("fuelLeak");
		this.brokenStarter = dataTag.getBoolean("brokenStarter");
		this.currentGear = dataTag.getByte("currentGear");
		this.hours = dataTag.getDouble("hours");
		this.RPM = dataTag.getDouble("rpm");
		this.temp = dataTag.getDouble("temp");
		this.pressure = dataTag.getDouble("pressure");
		if(dataTag.hasKey("state")){
			this.state = EngineStates.values()[dataTag.getByte("state")];
		}else{
			this.state = EngineStates.ENGINE_OFF;
		}
		this.startSounds = vehicle.world.isRemote;
		this.startRPM = definition.engine.maxRPM < 15000 ? 500 : 2000;
		this.stallRPM = definition.engine.maxRPM < 15000 ? 300 : 1500;
		
		//If we are an engine_jet part, and our jetPowerFactor is 0, we are a legacy jet engine.
		if(definition.general.type.equals("engine_jet") && definition.engine.jetPowerFactor == 0){
			definition.engine.jetPowerFactor = 1.0F;
			definition.engine.bypassRatio = definition.engine.gearRatios[0];
			definition.engine.gearRatios[0] = 1.0F;
		}
		
		//If we only have one gearRatio, add two more gears as we're a legacy propeller-based engine.
		if(definition.engine.gearRatios.length == 1){
			definition.engine.propellerRatio = 1/definition.engine.gearRatios[0];
			definition.engine.gearRatios = new float[]{-1, 0, 1};
		}
		
		//If our shiftSpeed is 0, we are a legacy engine that didn't set a shift speed.
		if(definition.engine.shiftSpeed == 0){
			definition.engine.shiftSpeed = 20;
		}
		
		//If we aren't connected to a car or boat, set our gear to 1.
		//This is needed for correct speed setting.
		if(!(vehicle instanceof EntityVehicleG_Car || vehicle instanceof EntityVehicleG_Boat)){
			currentGear = 1;
		}
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(source.isExplosion()){
			hours += damage*20*ConfigSystem.configObject.general.engineHoursFactor.value;
			if(!definition.engine.isSteamPowered){
				if(!oilLeak)oilLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value*10;
				if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value*10;
				if(!brokenStarter)brokenStarter = Math.random() < 0.05;
			}
			MTS.MTSNet.sendToAll(new PacketPartEngineDamage(this, (float) (damage*10*ConfigSystem.configObject.general.engineHoursFactor.value)));
		}else{
			hours += damage*2*ConfigSystem.configObject.general.engineHoursFactor.value;
			if(!definition.engine.isSteamPowered){
				if(!oilLeak)oilLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value;
				if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value;
			}
			MTS.MTSNet.sendToAll(new PacketPartEngineDamage(this, (float) (damage*ConfigSystem.configObject.general.engineHoursFactor.value)));
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		//Set current gear ratio based on current gear.
		currentGearRatio = definition.engine.gearRatios[currentGear + 1];
		
		//Start up sounds if we haven't already.  We don't do this during construction as other mods are
		//PITA and will construct new vehicles every tick to get data.  I'm looking a YOU The One Probe!
		if(startSounds && state.running && vehicle.world.isRemote){
			if(definition.engine.customSoundset != null){
				for(EngineSound soundDefinition : definition.engine.customSoundset){
					WrapperAudio.playQuickSound(new SoundInstance(this, soundDefinition.soundName, true));
				}
			}else{
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_running", true));
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_supercharger", true));
			}
			startSounds = false;
		}
				
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
		
		//Check if we have a propeller attached as a child part.
		if(propeller == null){
			for(APart part : childParts){
				if(part instanceof PartPropeller){
					propeller = (PartPropeller) part;
					break;
				}
			}
		}else if(!childParts.contains(propeller)){
			propeller = null;
		}
		
		//Add cooling for ambient temp.
		ambientTemp = 25*vehicle.world.getBiome(vehicle.getPosition()).getTemperature(vehicle.getPosition()) - 5*(Math.pow(2, vehicle.posY/400) - 1);
		coolingFactor = 0.001 - ((definition.engine.superchargerEfficiency/1000F)*(RPM/2000F)) + Math.abs(vehicle.velocity)/500F;
		temp -= (temp - ambientTemp)*coolingFactor;
		
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
		
		//If the starter is running, adjust RPM.
		if(starterLevel > 0){
			--starterLevel;
			if(RPM < startRPM*1.2){
				RPM = Math.min(RPM + definition.engine.starterPower, startRPM*1.2);
			}else{
				RPM = Math.max(RPM - definition.engine.starterPower, startRPM*1.2);
			}
		}
		
		//Do running logic.
		if(state.running){
			//Provide electric power to the vehicle we're in.
			vehicle.electricUsage -= 0.05*RPM/definition.engine.maxRPM;
			
			//Add hours to the engine.
			if(!isCreative){
				hours += 0.001*getTotalWearFactor();
				
				//Add extra hours if we are running the engine too fast.
				if(RPM > getSafeRPMFromMax(definition.engine.maxRPM)){
					hours += 0.001*(RPM - getSafeRPMFromMax(definition.engine.maxRPM))/10F*getTotalWearFactor();
				}
			}
			
			//Do engine-type specific update logic.
			if(definition.engine.isSteamPowered){
				//FIXME do steam engine logic.
			}else{
				//Try to get fuel from the vehicle and calculate fuel flow.
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
				
				//Add temp based on engine speed.
				temp += Math.max(0, (7*RPM/definition.engine.maxRPM - temp/(COLD_TEMP*2))/20);
				
				//Adjust oil pressure based on RPM and leak status.
				pressure = Math.min(90 - temp/10, pressure + RPM/startRPM - 0.5*(oilLeak ? 5F : 1F)*(pressure/LOW_OIL_PRESSURE));
							
				//Add extra hours and temp if we have low oil.
				if(pressure < LOW_OIL_PRESSURE){
					temp += Math.max(0, (20*RPM/definition.engine.maxRPM)/20);
					hours += 0.01*getTotalWearFactor();
				}
				
				//Add extra hours if we tried to run the engine fast without it being warmed up.
				if(RPM > startRPM*1.5 && temp < COLD_TEMP){
					hours += 0.001*(RPM/startRPM - 1)*getTotalWearFactor();
				}
				
				//Add extra hours, and possibly explode the engine, if its too hot.
				if(temp > OVERHEAT_TEMP_1){
					hours += 0.001*(temp - OVERHEAT_TEMP_1)*getTotalWearFactor();
					if(temp > FAILURE_TEMP && !vehicle.world.isRemote && !isCreative){
						explodeEngine();
					}
				}
				
				//If the engine has high hours, give a chance for a backfire.
				if(hours > 200 && !vehicle.world.isRemote){
					if(Math.random() < hours/10000*(getSafeRPMFromMax(this.definition.engine.maxRPM)/(RPM+getSafeRPMFromMax(this.definition.engine.maxRPM)/2))){
						backfireEngine();
					}
				}
				
				//Check if we need to stall the engine for various conditions.
				if(!vehicle.world.isRemote){
					if(!vehicle.world.isRemote && isInLiquid()){
						stallEngine(PacketEngineTypes.DROWN);
					}else if(vehicle.fuel == 0 && !isCreative){
						stallEngine(PacketEngineTypes.FUEL_OUT);
					}else if(RPM < stallRPM){
						stallEngine(PacketEngineTypes.TOO_SLOW);
					}
				}
			}
			
			//Do automatic transmission functions if needed.
			if(definition.engine.isAutomatic){
				if(currentGear > 0){
					if(shiftCooldown == 0){
						if(definition.engine.upShiftRPM != null && definition.engine.downShiftRPM != null){
							if(RPM > definition.engine.upShiftRPM[currentGear - 1]*0.5*(1.0F + vehicle.throttle/100F)) {
								shiftUp(false);
								shiftCooldown = definition.engine.shiftSpeed;
							}else if(RPM < definition.engine.downShiftRPM[currentGear - 1]*0.5*(1.0F + vehicle.throttle/100F) && currentGear > 1){
								shiftDown(false);
								shiftCooldown = definition.engine.shiftSpeed;
							}
						}else{
							if(RPM > getSafeRPMFromMax(definition.engine.maxRPM)*0.5F*(1.0F + vehicle.throttle/100F)){
								shiftUp(false);
								shiftCooldown = definition.engine.shiftSpeed;
							}else if(RPM < getSafeRPMFromMax(definition.engine.maxRPM)*0.25*(1.0F + vehicle.throttle/100F) && currentGear > 1){
								shiftDown(false);
								shiftCooldown = definition.engine.shiftSpeed;
							}
						}
					}else{
						--shiftCooldown;
					}
				}
			}
		}else{
			//If we aren't a steam engine, set pressure and fuel flow to 0.
			if(!definition.engine.isSteamPowered){
				pressure = 0;
				fuelFlow = 0;
			}
			
			//Internal fuel is used for engine sound wind down.  NOT used for power.
			if(internalFuel > 0){
				--internalFuel;
				if(RPM < startRPM){
					internalFuel = 0;
				}
			}
			
			//Start engine if the RPM is high enough to cause it to start by itself.
			//Used for drowned engines that come out of the water, or engines that don't
			//have the ability to engage a starter.
			if(RPM > startRPM){
				if(vehicle.fuel > 0 || isCreative){
					if(!isInLiquid() && state.magnetoOn && !vehicle.world.isRemote){
						startEngine();
					}
				}
			}
		}
		
		//Update engine RPM.  This depends on what is connected.
		//First check to see if we need to check driven wheels.  Only for cars.
		//While doing this we also get the friction those wheels are providing.
		//This is used later in force calculations.
		if(vehicle.definition.car != null){
			lowestWheelVelocity = 999F;
			desiredWheelVelocity = -999F;
			wheelFriction = 0;
			engineTargetRPM = !state.esOn ? vehicle.throttle/100F*(definition.engine.maxRPM - startRPM/1.25 - hours) + startRPM/1.25 : startRPM*1.2;
			
			//Update wheel friction and velocity.
			for(APartGroundDevice wheel : vehicle.wheels){
				if((wheel.offset.z > 0 && vehicle.definition.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && vehicle.definition.car.isRearWheelDrive)){
					//If we have grounded wheels, and this wheel is not on the ground, don't take it into account.
					//This means the wheel is spinning in the air and can't provide force or feedback.
					if(wheel.isOnGround()){
						wheelFriction += wheel.getMotiveFriction() - wheel.getFrictionLoss();
						lowestWheelVelocity = Math.min(wheel.angularVelocity, lowestWheelVelocity);
						desiredWheelVelocity = (float) Math.max(vehicle.velocity/wheel.getHeight(), desiredWheelVelocity);
					}
				}
			}
			
			//Adjust RPM of the engine.
			//Don't adjust it down to stall the engine, that can only be done via backfire.
			if(wheelFriction > 0){
				double desiredRPM = lowestWheelVelocity*1200F*currentGearRatio*vehicle.definition.car.axleRatio;
				RPM += (desiredRPM - RPM)/10D;
				if(RPM < stallRPM && state.running){
					RPM = stallRPM;
				}
			}else{
				//No wheel force.  Adjust wheels to engine speed.
				for(APartGroundDevice wheel : vehicle.wheels){
					wheel.skipAngularCalcs = false;
					if((wheel.offset.z > 0 && vehicle.definition.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && vehicle.definition.car.isRearWheelDrive)){
						if(currentGearRatio != 0){
							wheel.angularVelocity = (float) RPM/currentGearRatio/vehicle.definition.car.axleRatio/60F/20F;
						}else if(wheel.angularVelocity > 0){
							wheel.angularVelocity = Math.max(0, wheel.angularVelocity - 0.01F);
						}else{
							wheel.angularVelocity = Math.min(0, wheel.angularVelocity + 0.01F);
						}
					}
				}
			}
		}
		
		//If wheel friction is 0, and we have a propeller, and arne't in neutral, get RPM contributions for that.
		if(wheelFriction == 0 && propeller != null && currentGearRatio != 0){
			isPropellerInLiquid = vehicle.world.getBlockState(new BlockPos(propeller.partPos)).getMaterial().isLiquid();
			propellerGearboxRatio = definition.engine.propellerRatio != 0 ? definition.engine.propellerRatio : currentGearRatio;
			double propellerForcePenalty = Math.max(0, (propeller.definition.propeller.diameter - 75)/(50*(definition.engine.fuelConsumption + (definition.engine.superchargerFuelConsumption*definition.engine.superchargerEfficiency)) - 15));
			double propellerDesiredSpeed = 0.0254*Math.abs(propeller.currentPitch)*RPM/propellerGearboxRatio/60D/20D;
			double propellerFeedback = Math.signum(currentGearRatio)*(propellerDesiredSpeed - vehicle.velocity)*(isPropellerInLiquid ? 150 : 50) + propellerForcePenalty*50;
			if(state.running){
				double engineTargetRPM = vehicle.throttle/100F*(definition.engine.maxRPM - startRPM*1.25 - hours) + startRPM*1.25;
				double engineRPMDifference = engineTargetRPM - RPM;
				
				//propellerFeedback can't make an engine stall, but hours can.
				if(RPM + engineRPMDifference/10 > stallRPM && RPM + engineRPMDifference/10 - propellerFeedback < stallRPM){
					RPM = stallRPM;
				}else{
					RPM += engineRPMDifference/10 - propellerFeedback;
				}
			}else{
				RPM = Math.max(RPM - (propellerFeedback - propellerForcePenalty*50), 0);
			}
		}
		
		//If wheel friction is 0, and we don't have a propeller, or we're in neutral, adjust RPM to throttle position.
		//Or, if we are not on, just slowly spin the engine down.
		if((wheelFriction == 0 && propeller == null) || currentGearRatio == 0){
			if(state.running){
				double engineTargetRPM = vehicle.throttle/100F*(definition.engine.maxRPM - startRPM*1.25 - hours*10) + startRPM*1.25;
				RPM += (engineTargetRPM - RPM)/10;
				if(RPM > getSafeRPMFromMax(definition.engine.maxRPM) && definition.engine.jetPowerFactor == 0){
					RPM -= Math.abs(engineTargetRPM - RPM)/5;
				}
			}else if(!state.esOn && !state.hsOn){
				RPM = Math.max(RPM - 10, 0);
			}
		}
		
		//If we provide jet thrust, check for entities forward and aft of the engine and damage them.
		if(definition.engine.jetPowerFactor > 0 && vehicle.world.isRemote && RPM >= 5000){
			List<EntityLivingBase> collidedEntites = vehicle.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getAABBWithOffset(vehicle.headingVec).expand(-0.25F, -0.25F, -0.25F));
			if(!collidedEntites.isEmpty()){
				Entity attacker = null;
				for(Entity passenger : vehicle.getPassengers()){
					if(vehicle.getSeatForRider(passenger).isController){
						attacker = passenger;
						break;
					}
				}
				for(int i=0; i < collidedEntites.size(); ++i){
					if(!vehicle.equals(collidedEntites.get(i).getRidingEntity())){
						collidedEntites.get(i).attackEntityFrom(new DamageSourceJet(attacker, true), (float) (definition.engine.jetPowerFactor*ConfigSystem.configObject.damage.jetDamageFactor.value*RPM/1000F));
					}
				}
			}
			
			collidedEntites = vehicle.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getAABBWithOffset(vehicle.headingVec.scale(-1.0D)).expand(0.25F, 0.25F, 0.25F));
			if(!collidedEntites.isEmpty()){
				Entity attacker = null;
				for(Entity passenger : vehicle.getPassengers()){
					if(vehicle.getSeatForRider(passenger).isController){
						attacker = passenger;
						break;
					}
				}
				for(int i=0; i < collidedEntites.size(); ++i){
					if(!vehicle.equals(collidedEntites.get(i).getRidingEntity())){
						collidedEntites.get(i).attackEntityFrom(new DamageSourceJet(attacker, false), (float) (definition.engine.jetPowerFactor*ConfigSystem.configObject.damage.jetDamageFactor.value*RPM/2000F));
						collidedEntites.get(i).setFire(5);
					}
				}
			}
		}
		
		//Update engine and driveshaft rotation.
		//If we are on a car, the driveshaft needs to follow the wheel rotation, not our own.
		prevEngineRotation = engineRotation;
		engineRotation += 360D*RPM/1200D;
		prevDriveshaftRotation = driveshaftRotation;
		if(vehicle.definition.car != null){
			float driveShaftDesiredSpeed = -999F;
			for(APartGroundDevice wheel : vehicle.wheels){
				if((wheel.offset.z > 0 && vehicle.definition.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && vehicle.definition.car.isRearWheelDrive)){
					driveShaftDesiredSpeed = Math.max(Math.abs(wheel.angularVelocity), driveShaftDesiredSpeed);
				}
			}
			driveshaftRotation += (float) (vehicle.SPEED_FACTOR*driveShaftDesiredSpeed*Math.signum(vehicle.velocity)*360D);
		}else{
			driveshaftRotation += 360D*RPM/1200D*definition.engine.gearRatios[currentGear + 1];
		}
	}
	
	@Override
	public void removePart(){
		super.removePart();
		//Set state to off and tell wheels to stop skipping calcs from being controlled by the engine.
		this.state = EngineStates.ENGINE_OFF;
		for(APartGroundDevice wheel : vehicle.wheels){
			if(!wheel.isOnGround() && ((wheel.offset.z > 0 && vehicle.definition.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && vehicle.definition.car.isRearWheelDrive))){
				wheel.skipAngularCalcs = false;
			}
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound partData = new NBTTagCompound();
		partData.setBoolean("isCreative", this.isCreative);
		partData.setBoolean("oilLeak", this.oilLeak);
		partData.setBoolean("fuelLeak", this.fuelLeak);
		partData.setBoolean("brokenStarter", this.brokenStarter);
		partData.setByte("currentGear", this.currentGear);
		partData.setDouble("hours", hours);
		partData.setDouble("rpm", this.RPM);
		partData.setDouble("temp", this.temp);
		partData.setDouble("pressure", this.pressure);
		partData.setByte("state", (byte) this.state.ordinal());
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
	
	
	
	//--------------------START OF ENGINE STATE CHANGE METHODS--------------------
	
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
	
	public void startEngine(){
		if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
			state = EngineStates.RUNNING;
		}else if(state.equals(EngineStates.MAGNETO_ON_ES_ON)){
			state = EngineStates.RUNNING_ES_ON;
		}else if(state.equals(EngineStates.MAGNETO_ON_HS_ON)){
			state = EngineStates.RUNNING;
		}
		
		//Turn starter off.
		starterLevel = 0;
		
		//If we are not a steam engine, set oil pressure.
		if(!definition.engine.isSteamPowered){
			pressure = 60;
		}
		
		//Send off packet and start sounds.
		if(!vehicle.world.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, PacketEngineTypes.START));
		}else{
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_starting"));
			if(definition.engine.customSoundset != null){
				for(EngineSound soundDefinition : definition.engine.customSoundset){
					WrapperAudio.playQuickSound(new SoundInstance(this, soundDefinition.soundName, true));
				}
			}else{
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_running", true));
				WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_supercharger", true));
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
		
		//Add a small amount to the starter level from the player's hand, and play cranking sound.
		starterLevel += 4;
		if(vehicle.world.isRemote){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_cranking", true));
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
		
		//If we stalled due to not drowning, set internal fuel to play wind-down sounds.
		if(vehicle.world.isRemote){
			if(!packetType.equals(PacketEngineTypes.DROWN)){
				internalFuel = 100;
			}
		}
		
		//Send off packet and play stopping sound.
		if(!vehicle.world.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, packetType));
		}else{
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_stopping"));
		}
	}
	
	public void backfireEngine(){
		//Decrease RPM and send off packet to have clients do the same.
		//This also causes particles to spawn and sounds to play.
		RPM -= definition.engine.maxRPM < 15000 ? 100 : 500;
		if(!vehicle.world.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, PacketEngineTypes.BACKFIRE));
		}else{
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_sputter"));
			backfired = true;
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
	
	
	
	//--------------------START OF ENGINE GEAR METHODS--------------------
	
	public float getGearshiftRotation(){
		return definition.engine.isAutomatic ? Math.min(1, currentGear)*15F : currentGear*5;
	}
	
	public float getGearshiftPosition_Vertical(){
		if(currentGear == -1){
			return definition.engine.gearRatios.length%2 == 0 ? 15 : -15; 
		}else if(currentGear == 0){
			return 0;
		}else{
			return currentGear%2 == 0 ? -15 : 15;
		}
	}
	
	public float getGearshiftPosition_Horizontal(){
		int columns = (definition.engine.gearRatios.length)/2;
		int firstColumnAngle = columns/2*-5;
		float columnAngleDelta = columns != 1 ? -firstColumnAngle*2/(columns - 1) : 0; 
		if(currentGear == -1){
			return -firstColumnAngle;
		}else if(currentGear == 0){
			return 0;
		}else{
			//Divide the currentGear-1 by two to get our column (0 for column 1, 1 for 2).
			//Then add multiply that by columnAngleDelta to get our delta for this column.
			//Return that value, plus the initial angle.
			return firstColumnAngle + (currentGear - 1)/2*columnAngleDelta;
		}
	}
	
	public void shiftUp(boolean packet){
		if(currentGear == -1){
			currentGear = 0;
		}else if(currentGear == 0){
			if(vehicle.velocity > -0.25 || wheelFriction == 0){
				currentGear = 1;
			}else if(vehicle.world.isRemote){
				WrapperAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":engine_shifting_grinding"));
			}
		}else if(currentGear < definition.engine.gearRatios.length - 2){
			if(definition.engine.isAutomatic && packet){
				currentGear = 1;
			}else{
				++currentGear;
			}
		}
	}
	
	public void shiftDown(boolean packet){
		if(currentGear > 0){
			if(definition.engine.isAutomatic && packet){
				currentGear = 0;
			}else{
				--currentGear;
			}
		}else if(currentGear == 0){
			if(vehicle.velocity < 0.25 || wheelFriction == 0){
				currentGear = -1;
				//If the engine is running, and we are a big truck, turn on the backup beeper.
				if(state.running && vehicle.definition.car != null && vehicle.definition.car.isBigTruck && vehicle.world.isRemote){
					WrapperAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":backup_beeper", true));
				}
			}else if(vehicle.world.isRemote){
				WrapperAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":engine_shifting_grinding"));
			}
		}
	}
	
	
	
	//--------------------START OF ENGINE PROPERTY METHODS--------------------
	
	public static int getSafeRPMFromMax(int maxRPM){
		return maxRPM < 15000 ? maxRPM - (maxRPM - 2500)/2 : (int) (maxRPM/1.1);
	}
	
	public float getTotalFuelConsumption(){
		return definition.engine.fuelConsumption + definition.engine.superchargerFuelConsumption;
	}
	
	public double getTotalWearFactor(){
		if(definition.engine.superchargerEfficiency > 1.0F){
			return definition.engine.superchargerEfficiency*ConfigSystem.configObject.general.engineHoursFactor.value;
		}else{
			return ConfigSystem.configObject.general.engineHoursFactor.value;
		}
	}
	
	public boolean isInLiquid(){
		return vehicle.world.getBlockState(new BlockPos(partPos.addVector(0, packVehicleDef.intakeOffset, 0))).getMaterial().isLiquid();
	}
	
	public double getEngineRotation(float partialTicks){
		return engineRotation + (engineRotation = prevEngineRotation)*partialTicks;
	}
	
	public double getDriveshaftRotation(float partialTicks){
		return driveshaftRotation + (driveshaftRotation - prevDriveshaftRotation)*partialTicks;
	}
	
	public double getForceOutput(){
		//Get all the forces this part can output.
		double engineForce = 0;
		
		//First get wheel forces, if we have friction to do so.
		if(wheelFriction != 0){
			//If running, use the friction of the wheels to determine the new speed.
			if(state.running || state.esOn){
				engineForce = (engineTargetRPM - RPM)/definition.engine.maxRPM*currentGearRatio*vehicle.definition.car.axleRatio*(definition.engine.fuelConsumption + (definition.engine.superchargerFuelConsumption*definition.engine.superchargerEfficiency))*0.6F*30F;
				//Check to see if the wheels need to spin out.
				//If they do, we'll need to provide less force.
				if(Math.abs(engineForce/300F) > wheelFriction || (Math.abs(lowestWheelVelocity) - Math.abs(desiredWheelVelocity) > 0.1 && Math.abs(lowestWheelVelocity) - Math.abs(desiredWheelVelocity) < Math.abs(engineForce/300F))){
					engineForce *= vehicle.currentMass/100000F*wheelFriction/Math.abs(engineForce/300F);					
					for(APartGroundDevice wheel : vehicle.wheels){
						if((wheel.offset.z > 0 && vehicle.definition.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && vehicle.definition.car.isRearWheelDrive)){
							if(currentGearRatio > 0){
								if(engineForce >= 0){
									wheel.angularVelocity = (float) Math.min(engineTargetRPM/1200F/currentGearRatio/vehicle.definition.car.axleRatio, wheel.angularVelocity + 0.01);
								}else{
									wheel.angularVelocity = (float) Math.min(engineTargetRPM/1200F/currentGearRatio/vehicle.definition.car.axleRatio, wheel.angularVelocity - 0.01);
								}
							}else{
								if(engineForce >= 0){
									wheel.angularVelocity = (float) Math.max(engineTargetRPM/1200F/currentGearRatio/vehicle.definition.car.axleRatio, wheel.angularVelocity - 0.01);
								}else{
									
									wheel.angularVelocity = (float) Math.max(engineTargetRPM/1200F/currentGearRatio/vehicle.definition.car.axleRatio, wheel.angularVelocity + 0.01);
								}
							}
							wheel.skipAngularCalcs = true;
						}
					}
				}else{
					//If we have wheels not on the ground and we drive them, adjust their velocity now.
					for(APartGroundDevice wheel : vehicle.wheels){
						wheel.skipAngularCalcs = false;
						if(!wheel.isOnGround() && ((wheel.offset.z > 0 && vehicle.definition.car.isFrontWheelDrive) || (wheel.offset.z <= 0 && vehicle.definition.car.isRearWheelDrive))){
							wheel.angularVelocity = lowestWheelVelocity;
						}
					}
				}
				
				//Don't let us have negative engine force at low speeds.
				//This causes odd reversing behavior when the engine tries to maintain speed.
				if((engineForce < 0 && currentGear > 0 && vehicle.velocity < 0.25) || (engineForce > 0 && currentGear < 0 && vehicle.velocity > -0.25)){
					engineForce = 0;
				}
			}else{
				//Not running, do engine braking.
				engineForce = -RPM/definition.engine.maxRPM*Math.signum(currentGear)*30;
			}
		}else{
			//No wheel force.  Check for propellers to provide force.
			if(propeller != null && Math.abs(propeller.currentPitch) > 5 && state.running){
				//Get what the pitch velocity of the propeller would be at the current velocity.
				double currentPitchVelocity = vehicle.velocity*20D;
				//Get the effective pitch velocity of the propeller at the current RPM.
				double effectivePitchVelocity = 0.0254D*propeller.currentPitch*20D*propeller.angularVelocity;
				//Multiply by a factor to get the true effective pitch velocity.  This is slightly higher than ideal.
				effectivePitchVelocity *= (1D*propeller.currentPitch/propeller.definition.propeller.diameter + 0.2D)/(1D*propeller.currentPitch/propeller.definition.propeller.diameter);
				if(effectivePitchVelocity != 0){
					//Get the angle of attack of the propeller.
					//Note that because the effective pitch velocity is in meters per second, 
					//it needs to be converted to meters per revolution before we can move on.
					//This gets the angle as a ratio of forward pitch to propeller circumference.
					double angleOfAttack = Math.abs(((effectivePitchVelocity - currentPitchVelocity)/(20D*RPM/propellerGearboxRatio))/((propeller.definition.propeller.diameter*Math.PI)*0.0254D));
					double thrust = vehicle.airDensity*Math.PI*Math.pow(0.0254*propeller.definition.propeller.diameter/2D, 2)*
							(effectivePitchVelocity*effectivePitchVelocity - effectivePitchVelocity*currentPitchVelocity)*
							Math.pow(propeller.definition.propeller.diameter/2D/Math.abs(propeller.currentPitch) + propeller.definition.propeller.numberBlades/1000D, 1.5)/400D;
					//If the angle of attack is greater than 25 degrees (or a ratio of 0.4663), sap power off the propeller for stalling.
					if(angleOfAttack > 0.4663D){
						thrust *= 0.4663D/angleOfAttack;
					}
					//Get the correct sign of the force, taking engine systems into account.
					if(vehicle instanceof EntityVehicleG_Blimp && vehicle.reverseThrust){
						thrust *= -Math.signum(effectivePitchVelocity);
					}else{
						thrust *= Math.signum(effectivePitchVelocity);
					}
					
					//Add thrust as a force to engine force.
					//Note that propellers in water are more effective than those in air due to fluid dynamics.
					engineForce += isPropellerInLiquid ? thrust*50 : thrust;
				}
			}
		}
		
		//If we provide jet power, add it now.  This may be done with any parts or wheels on the ground.
		//Propellers max out at about 25 force, so use that to determine this force.
		if(definition.engine.jetPowerFactor > 0 && state.running){
			//First we need the air density (sea level 1.225) so we know how much air we are moving.
			//We then multiply that by the RPM and the fuel consumption to get the raw power produced
			//by the core of the engine.  This is speed-independent as the core will ALWAYS accelerate air.
			//Note that due to a lack of jet physics formulas available, this is "hacky math".
			double safeRPMFactor = RPM/getSafeRPMFromMax(definition.engine.maxRPM);
			double coreContribution = Math.max(10*vehicle.airDensity*definition.engine.fuelConsumption*safeRPMFactor - definition.engine.bypassRatio, 0);
			//The fan portion is calculated similarly to how propellers are calculated.
			//This takes into account the air density, and relative speed of the engine versus the fan's desired speed.
			//Again, this is "hacky math", as for some reason there's no data on fan pitches.
			//In this case, however, we don't care about the fuelConsumption as that's only used by the core.
			double fanVelocityFactor = (0.0254*250*RPM/60/20 - vehicle.velocity)/200D;
			double fanContribution = 10*vehicle.airDensity*safeRPMFactor*fanVelocityFactor*definition.engine.bypassRatio;
			engineForce += (vehicle.reverseThrust ? -(coreContribution + fanContribution) : coreContribution + fanContribution)*definition.engine.jetPowerFactor;
		}
		
		//Finally, return the force we calculated.
		return engineForce;
	}

	
	
	//--------------------START OF ENGINE SOUND METHODS--------------------
	
	@Override
	public void updateProviderSound(SoundInstance sound){
		super.updateProviderSound(sound);
		//Adjust cranking sound pitch to match RPM and stop looping if we are done cranking.
		//Adjust running sound to have pitch based on engine RPM.
		if(sound.soundName.endsWith("_cranking")){
			if(!state.esOn && !state.hsOn){
				sound.stop();
			}else{
				if(definition.engine.isCrankingNotPitched){
					sound.pitch = (float) Math.min(1.0F, vehicle.electricPower/10);
				}else{
					sound.pitch = (float) (RPM/startRPM);
				}
			}
		}else if(sound.soundName.endsWith("backup_beeper")){
			//Turn off backup beeper if we are no longer in reverse.
			if(currentGear != -1){
				sound.stop();
			}
		}else{
			//If we are using a custom soundset, do that logic. Otherwise, do default sound logic.
			if(definition.engine.customSoundset != null){
				for(EngineSound soundDefinition : definition.engine.customSoundset){
					if(sound.soundName.equals(soundDefinition.soundName)){
						if(!state.running && internalFuel == 0){
							sound.stop();
						}else{
							//Interpolate in the form of Y=A*X + B.
							//In this case, B is the idle offset, A is the slope, X is the RPM, and Y is the output.
							double rpmPercentOfMax = Math.max(0, (RPM - startRPM)/definition.engine.maxRPM);
							sound.pitch = (float) ((soundDefinition.pitchMax - soundDefinition.pitchIdle)*rpmPercentOfMax + soundDefinition.pitchIdle);
							sound.volume = (float) ((soundDefinition.volumeMax - soundDefinition.volumeIdle)*rpmPercentOfMax + soundDefinition.volumeIdle);
						}
					}
				}
			}else{
				//Update running and supercharger sounds.
				if(sound.soundName.endsWith("_running")){
					if(!state.running && internalFuel == 0){
						sound.stop();
					}else{
						//Pitch should be 0.35 at idle, with a 0.35 increase for every 2500 RPM, or every 25000 RPM for jet (high-revving) engines by default.
						//For steam engines, pitch is just 1 as it's meant to be the sound of a firebox.
						if(definition.engine.isSteamPowered){
							sound.pitch = 1.0F;
						}else{
							sound.pitch = (float) (0.35*(1 + Math.max(0, (RPM - startRPM))/(definition.engine.maxRPM < 15000 ? 500 : 5000)));
						}
					}
				}else if(sound.soundName.endsWith("_supercharger")){
					if(!state.running && internalFuel == 0){
						sound.stop();
					}else{
						sound.volume = (float) RPM/definition.engine.maxRPM;
						if(definition.engine.isSteamPowered){
							sound.pitch = 1.0F;
						}else{
							sound.pitch = (float) (0.35*(1 + Math.max(0, (RPM - startRPM))/(definition.engine.maxRPM < 15000 ? 500 : 5000)));
						}
					}
				}else if(sound.soundName.endsWith("_piston")){
					sound.pitch = (float) (0.35*(1 + Math.max(0, (RPM - startRPM))/500D)); 
				}
			}
		}
	}
	
	@Override
	public void restartSound(SoundInstance sound){
		if(definition.engine.customSoundset != null){
			for(EngineSound soundDefinition : definition.engine.customSoundset){
				if(sound.soundName.equals(soundDefinition.soundName)){
					WrapperAudio.playQuickSound(new SoundInstance(this, sound.soundName, true));
				}
			}
		}else if(sound.soundName.endsWith("_cranking") || sound.soundName.endsWith("_running") || sound.soundName.endsWith("_supercharger")){
			WrapperAudio.playQuickSound(new SoundInstance(this, sound.soundName, true));
		}else if(sound.soundName.endsWith("backup_beeper")){
			WrapperAudio.playQuickSound(new SoundInstance(this, MTS.MODID + ":backup_beeper", true));
		}
	}

	
	
	//--------------------START OF ENGINE PARTICLE METHODS--------------------
	
	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(Minecraft.getMinecraft().effectRenderer != null){
			//Render exhaust smoke if we have any exhausts and are running.
			//If we are starting and have flames set, render those instead.
			if(packVehicleDef.exhaustPos != null && (state.running || (definition.engine.flamesOnStartup && state.esOn)) && RPM != 0){
				//Render a smoke for every cycle the exhaust makes.
				//Depending on the number of positions we have, render an exhaust for every one.
				//So for 1 position, we render 1 every 2 engine cycles (4 stroke), and for 4, we render 4.
				//Note that the rendering is offset for multi-position points to simulate the cylinders firing
				//in their aligned order.
				
				//Get timing information and particle information.
				long engineCycleTimeMills = (long) (2D*(1D/(RPM/60D/1000D)));
				long currentTime = System.currentTimeMillis();
				long camTime = currentTime%engineCycleTimeMills;
				
				float particleColor = definition.engine.isSteamPowered ? 0.0F : (float) Math.max(1 - temp/COLD_TEMP, 0);
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
						//Also play steam chuff sound if we are a steam engine.
						if(definition.engine.isSteamPowered){
							WrapperAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_piston"));
						}
					}
					if(definition.engine.flamesOnStartup && state.esOn){
						Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.EngineFlameParticleFX(vehicle.world, exhaustOffset.x, exhaustOffset.y, exhaustOffset.z, velocityOffset.x/10D + 0.02 - Math.random()*0.04, velocityOffset.y/10D, velocityOffset.z/10D + 0.02 - Math.random()*0.04));
					}
					lastTimeParticleSpawned = singleExhaust ? currentTime : camTime;
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
			
			//Render engine smoke if we're overheating.  Only for non-steam engines.
			if(!definition.engine.isSteamPowered && temp > OVERHEAT_TEMP_1){
				Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, partPos.x, partPos.y + 0.5, partPos.z, 0, 0.15, 0, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F));
				if(temp > OVERHEAT_TEMP_2){
					Minecraft.getMinecraft().effectRenderer.addEffect(new VehicleEffectsSystem.ColoredSmokeFX(vehicle.world, partPos.x, partPos.y + 0.5, partPos.z, 0, 0.15, 0, 0.0F, 0.0F, 0.0F, 2.5F, 1.0F));
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
