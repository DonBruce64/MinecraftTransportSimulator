package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;

public class PartEngine extends APart{
	
	//State data.
	public boolean isCreative;
	public boolean oilLeak;
	public boolean fuelLeak;
	public boolean brokenStarter;
	public boolean backfired;
	public boolean badShift;
	public boolean running;
	@DerivedValue
	public boolean magnetoOn;
	@DerivedValue
	public boolean electricStarterEngaged;
	@DerivedValue
	public boolean handStarterEngaged;
	public byte forwardsGears;
	public byte reverseGears;
	@DerivedValue
	public byte currentGear;
	public int upshiftCountdown;
	public int downshiftCountdown;
	public int internalFuel;
	public double hours;
	public double rpm;
	public double temp = 20;
	public double pressure;
	public float propellerGearboxRatio;
	
	//Runtime calculated values.
	public double fuelFlow;
	public PartEngine linkedEngine;
	
	//Internal properties
	@ModifiedValue
	private float currentMaxRPM;
	@ModifiedValue
	private float currentMaxSafeRPM;
	@ModifiedValue
	private float currentRevlimitRPM;
	@ModifiedValue
	private float currentIdleRPM;
	@ModifiedValue
	private float currentFuelConsumption;
	@ModifiedValue
	private float currentHeatingCoefficient;
	@ModifiedValue
	private float currentCoolingCoefficient;
	@ModifiedValue
	private float currentSuperchargerFuelConsumption;
	@ModifiedValue
	private float currentSuperchargerEfficiency;
	
	//Internal variables.
	private boolean isPropellerInLiquid;
	private boolean autoStarterEngaged;
	private int starterLevel;
	private int shiftCooldown;
	private int backfireCooldown;
	private float currentGearRatio;
	private double lowestWheelVelocity;
	private double desiredWheelVelocity;
	private double propellerAxialVelocity;
	private double engineAxialVelocity;
	private float wheelFriction;
	private double ambientTemp;
	private double coolingFactor;
	private double engineTargetRPM;
	private double engineRotation;
	private double prevEngineRotation;
	private double driveshaftRotation;
	private double prevDriveshaftRotation;
	private PartPropeller attachedPropeller;
	private final Point3d engineForce = new Point3d();
	
	//Constants and static variables.
	public static final String MAGNETO_VARIABLE = "engine_magneto";
	public static final String ELECTRIC_STARTER_VARIABLE = "engine_starter";
	public static final String HAND_STARTER_VARIABLE = "engine_starter_hand";
	public static final String UP_SHIFT_VARIABLE = "engine_shift_up";
	public static final String DOWN_SHIFT_VARIABLE = "engine_shift_down";
	public static final String NEUTRAL_SHIFT_VARIABLE = "engine_shift_neutral";
	public static final String GEAR_VARIABLE = "engine_gear";
	public static final float COLD_TEMP = 30F;
	public static final float OVERHEAT_TEMP_1 = 115.556F;
	public static final float OVERHEAT_TEMP_2 = 121.111F;
	public static final float FAILURE_TEMP = 132.222F;
	public static final float LOW_OIL_PRESSURE = 40F;
	public static final float MAX_SHIFT_SPEED = 0.35F;
	
	
	public PartEngine(AEntityF_Multipart<?> entityOn, WrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placingPlayer, placementDefinition, data, parentPart);
		this.isCreative = data.getBoolean("isCreative");
		this.oilLeak = data.getBoolean("oilLeak");
		this.fuelLeak = data.getBoolean("fuelLeak");
		this.brokenStarter = data.getBoolean("brokenStarter");
		this.running = data.getBoolean("running");
		this.hours = data.getDouble("hours");
		this.rpm = data.getDouble("rpm");
		this.temp = data.getDouble("temp");
		this.pressure = data.getDouble("pressure");
		for(float gear : definition.engine.gearRatios){
			if(gear < 0){
				++reverseGears;
			}else if(gear > 0){
				++forwardsGears;
			}
		}
		
		//If we are on an aircraft, set our gear to 1 as aircraft don't have shifters.
		//Well, except blimps, but that's a special case.
		if(vehicleOn != null && vehicleOn.definition.motorized.isAircraft){
			setVariable(GEAR_VARIABLE, 1);
		}
	}
	
	@Override
	public void attack(Damage damage){
		super.attack(damage);
		if(!damage.isWater){
			if(definition.engine.disableAutomaticStarter){
				//Check if this is a hand-start command.
				if(damage.entityResponsible instanceof WrapperPlayer && ((WrapperPlayer) damage.entityResponsible).getHeldStack().isEmpty()){
					if(!entityOn.equals(damage.entityResponsible.getEntityRiding())){
						if(!magnetoOn){
							setVariable(MAGNETO_VARIABLE, 1);
							InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(this, MAGNETO_VARIABLE));
						}
						handStartEngine();
						InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.HS_ON));
						return;
					}
				}
			}
			if(!isCreative){
				if(damage.isExplosion){
					hours += damage.amount*20*ConfigSystem.configObject.general.engineHoursFactor.value;
					if(!definition.engine.isSteamPowered){
						if(!oilLeak)oilLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value*10;
						if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value*10;
						if(!brokenStarter)brokenStarter = Math.random() < 0.05;
					}
					InterfacePacket.sendToAllClients(new PacketPartEngine(this, damage.amount*10*ConfigSystem.configObject.general.engineHoursFactor.value, oilLeak, fuelLeak, brokenStarter));
				}else{
					hours += damage.amount*2*ConfigSystem.configObject.general.engineHoursFactor.value;
					if(!definition.engine.isSteamPowered){
						if(!oilLeak)oilLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value;
						if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.configObject.damage.engineLeakProbability.value;
					}
					InterfacePacket.sendToAllClients(new PacketPartEngine(this, damage.amount*ConfigSystem.configObject.general.engineHoursFactor.value, oilLeak, fuelLeak, brokenStarter));
				}
			}
		}else{
			stallEngine(Signal.DROWN);
			InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.DROWN));
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Reset states.
			backfired = false;
			badShift = false;
			magnetoOn = isVariableActive(MAGNETO_VARIABLE);
			electricStarterEngaged = isVariableActive(ELECTRIC_STARTER_VARIABLE);
			handStarterEngaged = isVariableActive(HAND_STARTER_VARIABLE);
			currentGear = (byte) getVariable(GEAR_VARIABLE);
			
			//If the engine is running, but the magneto is off, turn the engine off.
			if(running && !magnetoOn){
				running = false;
				internalFuel = 200;
			}
			
			//Set fuel flow to 0 for the start of this cycle.
			fuelFlow = 0;
			
			//Remove values from shifting times if applicable.
			if(upshiftCountdown > 0){
				--upshiftCountdown;
			}
			if(downshiftCountdown > 0){
				--downshiftCountdown;
			}
			
			//Set current gear ratio based on current gear.
			currentGearRatio = definition.engine.gearRatios.get(currentGear + reverseGears);
					
			if(vehicleOn != null){
				//Check to see if we are linked and need to equalize power between us and another engine.
				if(linkedEngine != null){
					if(linkedEngine.position.distanceTo(this.position) > 16){
						linkedEngine.linkedEngine = null;
						linkedEngine = null;
						if(world.isClient()){
							for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
								if(entity instanceof WrapperPlayer){
									((WrapperPlayer) entity).displayChatMessage("interact.jumpercable.linkdropped");
								}
							}
						}
					}else if(vehicleOn.electricPower + 0.5 < linkedEngine.vehicleOn.electricPower){
						linkedEngine.vehicleOn.electricPower -= 0.005F;
						vehicleOn.electricPower += 0.005F;
					}else if(vehicleOn.electricPower > linkedEngine.vehicleOn.electricPower + 0.5){
						vehicleOn.electricPower -= 0.005F;
						linkedEngine.vehicleOn.electricPower += 0.005F;
					}else{
						linkedEngine.linkedEngine = null;
						linkedEngine = null;
						if(world.isClient()){
							for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
								if(entity instanceof WrapperPlayer){
									((WrapperPlayer) entity).displayChatMessage("interact.jumpercable.powerequal");
								}
							}
						}
					}
				}
				
				//Add cooling for ambient temp.
				ambientTemp = (25*world.getTemperature(position) + 5)*ConfigSystem.configObject.general.engineBiomeTempFactor.value;
				coolingFactor = 0.001*currentCoolingCoefficient - (currentSuperchargerEfficiency/1000F)*(rpm/2000F) + (vehicleOn.velocity/1000F)*currentCoolingCoefficient;
				temp -= (temp - ambientTemp)*coolingFactor;
				
				//Check to see if electric or hand starter can keep running.
				if(electricStarterEngaged){
					if(starterLevel == 0){
						if(vehicleOn.electricPower > 1){
							starterLevel += 4;
						}else{
							setVariable(ELECTRIC_STARTER_VARIABLE, 0);
							InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(this, ELECTRIC_STARTER_VARIABLE));
						}
					}
					if(starterLevel > 0){
						if(!isCreative){
							vehicleOn.electricUsage += 0.05F;
						}
						if(!isCreative){
							fuelFlow += vehicleOn.fuelTank.drain(vehicleOn.fuelTank.getFluid(), getTotalFuelConsumption()*ConfigSystem.configObject.general.fuelUsageFactor.value, !world.isClient());
						}
					}
					if(autoStarterEngaged){
						if(running){
							setVariable(ELECTRIC_STARTER_VARIABLE, 0);
							InterfacePacket.sendToAllClients(new PacketEntityVariableToggle(this, ELECTRIC_STARTER_VARIABLE));
						}
					}
				}else if(handStarterEngaged){
					if(starterLevel == 0){
						setVariable(HAND_STARTER_VARIABLE, 0);
					}
				}else{
					starterLevel = 0;
					autoStarterEngaged = false;
				}
				
				//If the starter is running, adjust RPM.
				if(starterLevel > 0){
					--starterLevel;
					if(rpm < definition.engine.startRPM*2){
						rpm = Math.min(rpm + definition.engine.starterPower, definition.engine.startRPM*2);
					}else{
						rpm = Math.max(rpm - definition.engine.starterPower, definition.engine.startRPM*2);
					}
				}
				
				//Add extra hours if we are running the engine too fast.
				if(!isCreative && rpm > currentMaxSafeRPM){
					hours += (rpm - currentMaxSafeRPM)/currentMaxSafeRPM*getTotalWearFactor();
				}
				
				//Check for any shifting requests.
				if(isVariableActive(UP_SHIFT_VARIABLE)){
					shiftUp(false);
					toggleVariable(UP_SHIFT_VARIABLE);
				}else if(isVariableActive(DOWN_SHIFT_VARIABLE)){
					shiftDown(false);
					toggleVariable(DOWN_SHIFT_VARIABLE);
				}else if(isVariableActive(NEUTRAL_SHIFT_VARIABLE)){
					shiftNeutral();
					toggleVariable(NEUTRAL_SHIFT_VARIABLE);
				}
				
				//Check for reversing if we are on a blimp with reversed thrust.
				if(vehicleOn != null && vehicleOn.definition.motorized.isBlimp && attachedPropeller != null){
					if(vehicleOn.reverseThrust && currentGear > 0){
						currentGear = -1;
					}else if(!vehicleOn.reverseThrust && currentGear < 0){
						currentGear = 1;
					}
				}
				
				//Do running logic.
				if(running){
					//Provide electric power to the vehicle we're in.
					vehicleOn.electricUsage -= 0.05*rpm/currentMaxRPM;
					
					//Add hours to the engine.
					if(!isCreative){
						hours += 0.001*getTotalWearFactor();
					}
					
					//Do engine-type specific update logic.
					if(definition.engine.isSteamPowered){
						//TODO do steam engine logic.
					}else{
						//Try to get fuel from the vehicle and calculate fuel flow.
						if(!isCreative && !vehicleOn.fuelTank.getFluid().isEmpty()){
							if(!ConfigSystem.configObject.fuel.fuels.containsKey(definition.engine.fuelType)){					
								throw new IllegalArgumentException("Engine:" + definition.packID + ":" + definition.systemName + " wanted fuel configs for fuel of type:" + definition.engine.fuelType + ", but these do not exist in the config file.  Fuels currently in the file are:" + ConfigSystem.configObject.fuel.fuels.keySet().toString() + "If you are on a server, this means the server and client configs are not the same.  If this is a modpack, TELL THE AUTHOR IT IS BORKEN!");
							}else if(!ConfigSystem.configObject.fuel.fuels.get(definition.engine.fuelType).containsKey(vehicleOn.fuelTank.getFluid())){
								//Clear out the fuel from this vehicle as it's the wrong type.
								vehicleOn.fuelTank.drain(vehicleOn.fuelTank.getFluid(), vehicleOn.fuelTank.getFluidLevel(), true);
							}else{
								fuelFlow += vehicleOn.fuelTank.drain(vehicleOn.fuelTank.getFluid(), getTotalFuelConsumption()*ConfigSystem.configObject.general.fuelUsageFactor.value/ConfigSystem.configObject.fuel.fuels.get(definition.engine.fuelType).get(vehicleOn.fuelTank.getFluid())*rpm*(fuelLeak ? 1.5F : 1.0F)/currentMaxRPM, !world.isClient());
							}
						}
						
						//Add temp based on engine speed.
						temp += Math.max(0, (7*rpm/currentMaxRPM - temp/(COLD_TEMP*2))/20)*currentHeatingCoefficient*ConfigSystem.configObject.general.engineSpeedTempFactor.value;
						
						//Adjust oil pressure based on RPM and leak status.
						//If this is a 0-idle RPM engine, assume it's electric and doesn't have oil.
						if(currentIdleRPM != 0){
							pressure = Math.min(90 - temp/10, pressure + rpm/currentIdleRPM - 0.5*(oilLeak ? 5F : 1F)*(pressure/LOW_OIL_PRESSURE));
							
							//Add extra hours and temp if we have low oil.
							if(pressure < LOW_OIL_PRESSURE && !isCreative){
								temp += Math.max(0, (20*rpm/currentMaxRPM)/20);
								hours += 0.01*getTotalWearFactor();
							}
						}
						
						//Add extra hours, and possibly explode the engine, if its too hot.
						if(temp > OVERHEAT_TEMP_1 && !isCreative){
							hours += 0.001*(temp - OVERHEAT_TEMP_1)*getTotalWearFactor();
							if(temp > FAILURE_TEMP && !world.isClient()){
								explodeEngine();
							}
						}
						
						//If the engine has high hours, give a chance for a backfire.
						if(hours > 250 && !world.isClient()){
							if(Math.random() < (hours/2)/(250+(10000-hours))*(currentMaxSafeRPM/(rpm+currentMaxSafeRPM/1.5))){
								backfireEngine();
								InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.BACKFIRE));
							}
						}
						
						//Check if we need to stall the engine for various conditions.
						if(!world.isClient()){
							if(!world.isClient() && isInLiquid()){
								stallEngine(Signal.DROWN);
								InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.DROWN));
							}else if(!isCreative && vehicleOn.fuelTank.getFluidLevel() == 0){
								stallEngine(Signal.FUEL_OUT);
								InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.FUEL_OUT));
							}else if(rpm < definition.engine.stallRPM){
								stallEngine(Signal.TOO_SLOW);
								InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.TOO_SLOW));
							}else if(!isActive){
								stallEngine(Signal.FUEL_OUT);
								InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.FUEL_OUT));
							}else if(vehicleOn.damageAmount == vehicleOn.definition.general.health){
								stallEngine(Signal.DEAD_VEHICLE);
								InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.DEAD_VEHICLE));
							}
							
						}
					}
					
					//Do automatic transmission functions if needed.
					if(definition.engine.isAutomatic && !world.isClient() && currentGear != 0){
						if(shiftCooldown == 0){
							if(currentGear > 0 ? currentGear < forwardsGears : -currentGear < reverseGears){
								//Can shift up, try to do so.
								if(rpm > (definition.engine.upShiftRPM != null ? definition.engine.upShiftRPM.get(currentGear + reverseGears) : (currentMaxSafeRPM*0.9))*0.5F*(1.0F + vehicleOn.throttle)){
									if(currentGear > 0){
										if(shiftUp(true)){
											InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, GEAR_VARIABLE, 1));
										}
									}else{
										if(shiftDown(true)){
											InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, GEAR_VARIABLE, -1));
										}
									}
								}
							}
							if(currentGear > 1 || currentGear < -1){
								//Can shift down, try to do so.
								if(rpm < (definition.engine.downShiftRPM != null ? definition.engine.downShiftRPM.get(currentGear + reverseGears)*0.5*(1.0F + vehicleOn.throttle) : (currentMaxSafeRPM*0.9)*0.25*(1.0F + vehicleOn.throttle))){
									if(currentGear > 0){
										if(shiftDown(true)){
											InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, GEAR_VARIABLE, -1));
										}
									}else{
										if(shiftUp(true)){
											InterfacePacket.sendToAllClients(new PacketEntityVariableIncrement(this, GEAR_VARIABLE, 1));
										}
									}
								}
							}
						}else{
							--shiftCooldown;
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
						if(rpm < 500){
							internalFuel = 0;
						}
					}
					
					//Start engine if the RPM is high enough to cause it to start by itself.
					//Used for drowned engines that come out of the water, or engines that don't
					//have the ability to engage a starter.
					if(rpm >= definition.engine.startRPM && !world.isClient() && vehicleOn.damageAmount < vehicleOn.definition.general.health){
						if(isCreative || vehicleOn.fuelTank.getFluidLevel() > 0){
							if(!isInLiquid() && magnetoOn){
								startEngine();
								InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.START));
							}
						}
					}
				}
				
				//Update engine RPM.  This depends on what is connected.
				//First check to see if we need to check driven wheels.
				//While doing this we also get the friction those wheels are providing.
				//This is used later in force calculations.
				if(definition.engine.jetPowerFactor == 0 && (vehicleOn.definition.motorized.isFrontWheelDrive || vehicleOn.definition.motorized.isRearWheelDrive)){
					lowestWheelVelocity = 999F;
					desiredWheelVelocity = -999F;
					wheelFriction = 0;
					engineTargetRPM = !electricStarterEngaged ? vehicleOn.throttle*(currentMaxRPM - currentIdleRPM)/(1 + hours/1250) + currentIdleRPM : definition.engine.startRPM;
					
					//Update wheel friction and velocity.
					for(PartGroundDevice wheel : vehicleOn.groundDeviceCollective.drivenWheels){
						//If we have grounded wheels, and this wheel is not on the ground, don't take it into account.
						//This means the wheel is spinning in the air and can't provide force or feedback.
						if(vehicleOn.groundDeviceCollective.groundedGroundDevices.contains(wheel)){
							wheelFriction += Math.max(wheel.getMotiveFriction() - wheel.getFrictionLoss(), 0);
							lowestWheelVelocity = Math.min(wheel.angularVelocity, lowestWheelVelocity);
							desiredWheelVelocity = Math.max(wheel.getDesiredAngularVelocity(), desiredWheelVelocity);
						}
					}
					
					//Adjust RPM of the engine to wheels.
					if(currentGearRatio != 0 && starterLevel == 0){
						//Don't adjust it down to stall the engine, that can only be done via backfire.
						if(wheelFriction > 0){
							double desiredRPM = lowestWheelVelocity*1200F*currentGearRatio*vehicleOn.currentAxleRatio;
							rpm += (desiredRPM - rpm)/definition.engine.revResistance;
							if(rpm < currentIdleRPM && running && backfireCooldown <= 0){//Checks if we're backfiring and sets lugging rpm to stall rpm, otherwise sets lug rpm to idle
								rpm = currentIdleRPM;
							}else if(rpm < definition.engine.stallRPM && running){
								rpm = definition.engine.stallRPM;
								backfireCooldown -= 1;
							}
						}else{
							//No wheel force.  Adjust wheels to engine speed.
							for(PartGroundDevice wheel : vehicleOn.groundDeviceCollective.drivenWheels){
								if(currentGearRatio != 0){
									wheel.angularVelocity = rpm/currentGearRatio/vehicleOn.currentAxleRatio/1200D;
								}else if(wheel.angularVelocity > 0){
									wheel.angularVelocity = Math.max(0, wheel.angularVelocity - 0.01D);
								}else{
									wheel.angularVelocity = Math.min(0, wheel.angularVelocity + 0.01D);
								}
							}
						}
					}
				}
				
				//Update propeller variables.
				attachedPropeller = null;
				for(APart part : childParts){
					if(part instanceof PartPropeller){
						attachedPropeller = (PartPropeller) part;
						Point3d propellerThrustAxis = new Point3d(0D, 0D, 1D).rotateFine(attachedPropeller.localAngles.copy().add(vehicleOn.angles));
						propellerAxialVelocity = vehicleOn.motion.dotProduct(propellerThrustAxis);
						propellerGearboxRatio = Math.signum(currentGearRatio)*(definition.engine.propellerRatio != 0 ? definition.engine.propellerRatio : Math.abs(currentGearRatio));
						
						//If wheel friction is 0, and we aren't in neutral, get RPM contributions for that.
						if(wheelFriction == 0 && currentGearRatio != 0){
							isPropellerInLiquid = attachedPropeller.isInLiquid();
							double propellerForcePenalty = Math.max(0, (attachedPropeller.definition.propeller.diameter - 75)/(50*(currentFuelConsumption + (currentSuperchargerFuelConsumption*currentSuperchargerEfficiency)) - 15));
							double propellerDesiredSpeed = 0.0254*attachedPropeller.currentPitch*rpm/propellerGearboxRatio/60D/20D;
							double propellerFeedback = (propellerDesiredSpeed - propellerAxialVelocity)*(isPropellerInLiquid ? 130 : 40);
							if(currentGear < 0 || attachedPropeller.currentPitch < 0){
								propellerFeedback *= -1;
							}
							
							if(running){
								propellerFeedback += propellerForcePenalty*50;
								engineTargetRPM = vehicleOn.throttle*(currentMaxRPM - currentIdleRPM)/(1 + hours/1250) + currentIdleRPM;
								double engineRPMDifference = engineTargetRPM - rpm;
								
								//propellerFeedback can't make an engine stall, but hours can.
								if(rpm + engineRPMDifference/definition.engine.revResistance > definition.engine.stallRPM && rpm + engineRPMDifference/definition.engine.revResistance - propellerFeedback < definition.engine.stallRPM){
									rpm = definition.engine.stallRPM;
								}else{
									rpm += engineRPMDifference/definition.engine.revResistance - propellerFeedback;
								}
							}else if(!electricStarterEngaged && !handStarterEngaged){
								rpm -= (1 + propellerFeedback)*Math.abs(propellerGearboxRatio);
								
								//Don't let the engine RPM go negative.  This results in physics errors.
								if(rpm < 0){
									rpm = 0;
								}
							}
						}
					}
				}
				
				//If wheel friction is 0, and we don't have a propeller, or we're in neutral, adjust RPM to throttle position.
				//Or, if we are not on, just slowly spin the engine down.
				if((wheelFriction == 0 && attachedPropeller == null) || currentGearRatio == 0){
					if(running){
						engineTargetRPM = vehicleOn.throttle*(currentMaxRPM - currentIdleRPM)/(1 + hours/1250) + currentIdleRPM;
						rpm += (engineTargetRPM - rpm)/(definition.engine.revResistance*3);
						if(currentRevlimitRPM == -1){
							if(rpm > currentMaxSafeRPM){
								rpm -= Math.abs(engineTargetRPM - rpm)/60;
							}
						}else{
							if(rpm > currentRevlimitRPM){
								rpm -= Math.abs(engineTargetRPM - rpm)/definition.engine.revlimitBounce;
							}
						}
					}else if(!electricStarterEngaged && !handStarterEngaged){
						rpm = Math.max(rpm - definition.engine.engineWinddownRate, 0); //engineWinddownRate tells us how quickly to slow down the engine, by default 10
					}
				}
				
				///Update variables used for jet thrust.
				if(definition.engine.jetPowerFactor > 0){
					Point3d engineThrustAxis = new Point3d(0D, 0D, 1D).rotateFine(localAngles.copy().add(vehicleOn.angles));
					engineAxialVelocity = vehicleOn.motion.dotProduct(engineThrustAxis);
					
					//Check for entities forward and aft of the engine and damage them.
					if(!world.isClient() && rpm >= 5000){
						boundingBox.widthRadius += 0.25;
						boundingBox.heightRadius += 0.25;
						boundingBox.depthRadius += 0.25;
						boundingBox.globalCenter.add(vehicleOn.headingVector);
						Damage jetIntake = new Damage("jet_intake", definition.engine.jetPowerFactor*ConfigSystem.configObject.damage.jetDamageFactor.value*rpm/1000F, boundingBox, this, vehicleOn.getController());
						world.attackEntities(jetIntake, null);
						
						boundingBox.globalCenter.subtract(vehicleOn.headingVector);
						boundingBox.globalCenter.subtract(vehicleOn.headingVector);
						Damage jetExhaust = new Damage("jet_exhaust", definition.engine.jetPowerFactor*ConfigSystem.configObject.damage.jetDamageFactor.value*rpm/2000F, boundingBox, this, jetIntake.entityResponsible).setFire();
						world.attackEntities(jetExhaust, null);
						
						boundingBox.globalCenter.add(vehicleOn.headingVector);
						boundingBox.widthRadius -= 0.25;
						boundingBox.heightRadius -= 0.25;
						boundingBox.depthRadius -= 0.25;
					}
				}
				
				//Update engine and driveshaft rotation.
				//If we are linked to wheels on the ground follow the wheel rotation, not our own.
				prevEngineRotation = engineRotation;
				engineRotation += 360D*rpm/1200D;
				if(engineRotation > 3600000){
					engineRotation -= 3600000;
					prevEngineRotation -= 3600000;
				}else if(engineRotation < -3600000){
					engineRotation += 3600000;
					prevEngineRotation += 3600000;
				}
				
				prevDriveshaftRotation = driveshaftRotation;
				double driveshaftDesiredSpeed = -999;
				for(PartGroundDevice wheel : vehicleOn.groundDeviceCollective.drivenWheels){
					driveshaftDesiredSpeed = Math.max(wheel.angularVelocity, driveshaftDesiredSpeed);
				}
				if(driveshaftDesiredSpeed != -999){
					driveshaftRotation += 360D*driveshaftDesiredSpeed*EntityVehicleF_Physics.SPEED_FACTOR;
				}else{
					driveshaftRotation += 360D*rpm/1200D/currentGearRatio;
				}
				if(driveshaftRotation > 3600000){
					driveshaftRotation -= 3600000;
					prevDriveshaftRotation -= 3600000;
				}else if(driveshaftRotation < -3600000){
					driveshaftRotation += 3600000;
					prevDriveshaftRotation += 3600000;
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	protected void updateVariableModifiers(){
		currentMaxRPM = definition.engine.maxRPM;
		currentMaxSafeRPM = definition.engine.maxSafeRPM;
		currentRevlimitRPM = definition.engine.revlimitRPM;
		currentIdleRPM = definition.engine.idleRPM;
		currentFuelConsumption = definition.engine.fuelConsumption;
		currentHeatingCoefficient = definition.engine.heatingCoefficient;
		currentCoolingCoefficient = definition.engine.coolingCoefficient;
		currentSuperchargerFuelConsumption = definition.engine.superchargerFuelConsumption;
		currentSuperchargerEfficiency = definition.engine.superchargerEfficiency;
		
		//Adjust current variables to modifiers, if any exist.
		if(definition.variableModifiers != null){
			for(JSONVariableModifier modifier : definition.variableModifiers){
				switch(modifier.variable){
					case "maxRPM" : currentMaxRPM = adjustVariable(modifier, currentMaxRPM); break;
					case "maxSafeRPM" : currentMaxSafeRPM = adjustVariable(modifier, currentMaxSafeRPM); break;
					case "revlimitRPM" : currentRevlimitRPM = adjustVariable(modifier, currentRevlimitRPM); break;
					case "idleRPM" : currentIdleRPM = adjustVariable(modifier, currentIdleRPM); break;
					case "fuelConsumption" : currentFuelConsumption = adjustVariable(modifier, currentFuelConsumption); break;
					case "heatingCoefficient" : currentHeatingCoefficient = adjustVariable(modifier, currentHeatingCoefficient); break;
					case "coolingCoefficient" : currentCoolingCoefficient = adjustVariable(modifier, currentCoolingCoefficient); break;
					case "superchargerFuelConsumption" : currentSuperchargerFuelConsumption = adjustVariable(modifier, currentSuperchargerFuelConsumption); break;
					case "superchargerEfficiency" : currentSuperchargerEfficiency = adjustVariable(modifier, currentSuperchargerEfficiency); break;
					default : setVariable(modifier.variable, adjustVariable(modifier, (float) getVariable(modifier.variable))); break;
				}
			}
		}
	}
	
	@Override
	public boolean isInLiquid(){
		return world.isBlockLiquid(position.copy().add(0, placementDefinition.intakeOffset, 0));
	}
	
	@Override
	public void remove(){
		super.remove();
		//Turn off and tell wheels to stop skipping calcs from being controlled by the engine.
		running = false;
		if(vehicleOn != null){
			for(PartGroundDevice wheel : vehicleOn.groundDeviceCollective.drivenWheels){
				wheel.skipAngularCalcs = false;
			}
		}
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("engine_isautomatic"): return definition.engine.isAutomatic ? 1 : 0;	
			case("engine_rotation"): return getEngineRotation(partialTicks);
			case("engine_sin"): return Math.sin(Math.toRadians(getEngineRotation(partialTicks)));
			case("engine_cos"): return Math.cos(Math.toRadians(getEngineRotation(partialTicks)));
			case("engine_driveshaft_rotation"): return getDriveshaftRotation(partialTicks);
			case("engine_driveshaft_sin"): return Math.sin(Math.toRadians(getDriveshaftRotation(partialTicks)));
			case("engine_driveshaft_cos"): return Math.cos(Math.toRadians(getDriveshaftRotation(partialTicks)));
			case("engine_rpm"): return rpm;
			case("engine_rpm_safe"): return currentMaxSafeRPM;
			case("engine_rpm_max"): return currentMaxRPM;
			case("engine_rpm_percent"): return rpm/currentMaxRPM;
			case("engine_rpm_percent_safe"): return rpm/currentMaxSafeRPM;
			case("engine_fuel_flow"): return fuelFlow*20D*60D/1000D;
			case("engine_temp"): return temp;
			case("engine_pressure"): return pressure;
			case("engine_gear"): return currentGear;
			case("engine_gearshift"): return getGearshiftRotation();
			case("engine_gearshift_hvertical"): return getGearshiftPosition_Vertical();
			case("engine_gearshift_hhorizontal"): return getGearshiftPosition_Horizontal();
			case("engine_clutch_upshift"): return upshiftCountdown > 0 ? 1 : 0;
			case("engine_clutch_downshift"): return downshiftCountdown > 0 ? 1 : 0;
			case("engine_badshift"): return badShift ? 1 : 0;
			case("engine_reversed"): return currentGear < 0 ? 1 : 0;
			case("engine_running"): return running ? 1 : 0;
			case("engine_powered"): return running || internalFuel > 0 ? 1 : 0;
			case("engine_backfired"): return backfired ? 1 : 0;
			case("engine_jumper_cable"): return linkedEngine != null ? 1 : 0;
			case("engine_hours"): return hours;
			case("engine_oilleak"): return oilLeak ? 1 : 0;
			case("engine_fuelleak"): return fuelLeak ? 1 : 0;
		}
		if(variable.startsWith("engine_piston_")){
			if(running){
				String pistonVariable = variable.substring("engine_piston_".length());
				int pistonNumber = Integer.parseInt(pistonVariable.substring(0, pistonVariable.indexOf("_")));
				pistonVariable.substring(pistonVariable.indexOf("_"));
				int totalPistons = Integer.parseInt(pistonVariable.substring(0, pistonVariable.indexOf("_")));
				long engineCycleTime = (long) (2D*(1D/(rpm/60D/1000D)));
				
				if(engineCycleTime != 0){
					long currentEngineTime = (long) ((ticksExisted + partialTicks)*50D);
					long engineTimeInCycle = currentEngineTime%engineCycleTime;
					
					long pistonCycleTime = totalPistons > 1 ? engineCycleTime/totalPistons : engineCycleTime/2;
					long camMin = (pistonNumber - 1)*pistonCycleTime;
					long camMax = camMin + pistonCycleTime;
					if(camMax > engineCycleTime){
						return engineTimeInCycle < camMin && engineTimeInCycle > camMax ? 1 : 0;
					}else{
						return engineTimeInCycle > camMin && engineTimeInCycle < camMax ? 1 : 0;	
					}
				}
			}
			return 0;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	
	//--------------------START OF ENGINE STATE CHANGE METHODS--------------------
	public void startEngine(){
		running = true;
		
		//If we are not a steam engine, set oil pressure.
		//Not setting this means we will start at 0 and damage the engine.
		if(!definition.engine.isSteamPowered){
			pressure = 60;
		}
	}
	
	public void handStartEngine(){
		setVariable(HAND_STARTER_VARIABLE, 1);
		
		//Add a small amount to the starter level from the player's hand.
		starterLevel += 4;
	}
	
	public void autoStartEngine(){
		//Only engage auto-starter if we aren't running and we have the right fuel.
		if(!running && (isCreative || vehicleOn.fuelTank.getFluidLevel() > 0)){
			autoStarterEngaged = true;
			setVariable(MAGNETO_VARIABLE, 1);
			setVariable(ELECTRIC_STARTER_VARIABLE, 1);
		}
	}
	
	public void stallEngine(Signal signal){
		running = false;
		
		//If we stalled due to not drowning, set internal fuel to play wind-down sounds.
		if(world.isClient()){
			if(!signal.equals(Signal.DROWN)){
				internalFuel = 100;
			}
		}
	}
	
	public void backfireEngine(){
		//Decrease RPM and send off packet to have clients do the same. Also tells lug rpm to lug harder.
		backfired = true;
		rpm -= currentMaxRPM < 15000 ? 100 : 500;
		backfireCooldown = 4;
	}
	
	public void badShiftEngine(){
		//Just set bad shifting variable here.
		badShift = true;
	}
	
	protected void explodeEngine(){
		if(ConfigSystem.configObject.damage.explosions.value){
			world.spawnExplosion(position, 1F, true);
		}else{
			world.spawnExplosion(position, 0F, false);
		}
		isValid = false;
	}
	
	
	
	//--------------------START OF ENGINE GEAR METHODS--------------------
	
	public float getGearshiftRotation(){
		return definition.engine.isAutomatic ? Math.min(1, currentGear)*15F : currentGear*5;
	}
	
	public float getGearshiftPosition_Vertical(){
		if(currentGear < 0){
			return definition.engine.gearRatios.size()%2 == 0 ? 15 : -15; 
		}else if(currentGear == 0){
			return 0;
		}else{
			return currentGear%2 == 0 ? -15 : 15;
		}
	}
	
	public float getGearshiftPosition_Horizontal(){
		int columns = (definition.engine.gearRatios.size())/2;
		int firstColumnAngle = columns/2*-5;
		float columnAngleDelta = columns != 1 ? -firstColumnAngle*2/(columns - 1) : 0; 
		if(currentGear < 0){
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
	
	private boolean shiftUp(boolean autoTransRequest){
		byte nextGear = 0;
		boolean doShift = false;
		if(definition.engine.jetPowerFactor == 0 ){
			//Check to make sure we can shift.
			if(currentGear == forwardsGears){
				//Already at highest gear, don't process things.
				return false;
			}else if(currentGear == 0){
				//Neutral to 1st.
				nextGear = 1;
				doShift = vehicleOn.axialVelocity < MAX_SHIFT_SPEED || wheelFriction == 0 || !vehicleOn.goingInReverse;
			}else if(!autoTransRequest && definition.engine.isAutomatic){
				//Automatic transmission with manual shift up requested.
				//Either go to neutral if in reverse, or ignore shift.
				if(currentGear < 0){
					nextGear = 0;
					doShift = true;
				}else{
					return false;
				}
			}else{//Gear to next gear.
				nextGear = (byte) (currentGear + 1);
				doShift = true;
			}
				
			if(doShift){
				currentGear = nextGear;
				setVariable(GEAR_VARIABLE, currentGear);
				shiftCooldown = definition.engine.shiftSpeed;
				upshiftCountdown = definition.engine.clutchTime;
			}else if(!world.isClient()){
				InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.BAD_SHIFT));
			}
		}
		return doShift;
	}
	
	private boolean shiftDown(boolean autoTransRequest){
		byte nextGear = 0;
		boolean doShift = false;
		if(definition.engine.jetPowerFactor == 0){
			//Check to make sure we can shift.
			if(currentGear < 0 && -currentGear == reverseGears){
				//Already at lowest gear.
				return false;
			}else if(currentGear == 0){
				//Neutral to 1st reverse.
				nextGear = -1;
				doShift = vehicleOn.axialVelocity < MAX_SHIFT_SPEED || wheelFriction == 0 || vehicleOn.goingInReverse;
			}else if(!autoTransRequest && definition.engine.isAutomatic){
				//Automatic transmission with manual shift down requested.
				//Either go to neutral if in forwards, or ignore shift.
				if(currentGear > 0){
					nextGear = 0;
					doShift = true;
				}else{
					return false;
				}
			}else{//Gear to next gear.
				nextGear = (byte) (currentGear - 1);
				doShift = true;
			}
				
			if(doShift){
				currentGear = nextGear;
				setVariable(GEAR_VARIABLE, currentGear);
				shiftCooldown = definition.engine.shiftSpeed;
				downshiftCountdown = definition.engine.clutchTime;
			}else if(!world.isClient()){
				InterfacePacket.sendToAllClients(new PacketPartEngine(this, Signal.BAD_SHIFT));
			}
		}
		return doShift;
	}
	
	private void shiftNeutral(){
		if(definition.engine.jetPowerFactor == 0){
			if(currentGear != 0){//Any gear to neutral.
				if(currentGear > 0){
					downshiftCountdown = definition.engine.clutchTime;
				}else{
					upshiftCountdown = definition.engine.clutchTime;
				}
				shiftCooldown = definition.engine.shiftSpeed;
				currentGear = 0;
				setVariable(GEAR_VARIABLE, currentGear);
			}
		}
	}
	
	
	
	//--------------------START OF ENGINE PROPERTY METHODS--------------------
	public float getTotalFuelConsumption(){
			return currentFuelConsumption + currentSuperchargerFuelConsumption;
	}
	
	public double getTotalWearFactor(){
		if(currentSuperchargerEfficiency > 1.0F){
			return definition.engine.engineWearFactor*currentSuperchargerEfficiency*ConfigSystem.configObject.general.engineHoursFactor.value;
		}else{
			return definition.engine.engineWearFactor*ConfigSystem.configObject.general.engineHoursFactor.value;
		}
	}
	
	public double getEngineRotation(float partialTicks){
		return engineRotation + (engineRotation - prevEngineRotation)*partialTicks;
	}
	
	public double getDriveshaftRotation(float partialTicks){
		return driveshaftRotation + (driveshaftRotation - prevDriveshaftRotation)*partialTicks;
	}
	
	public Point3d getForceOutput(){
		engineForce.set(0D, 0D, 0D);
		//First get wheel forces, if we have friction to do so.
		if(definition.engine.jetPowerFactor == 0 && wheelFriction != 0){
			double wheelForce = 0;
			//If running, use the friction of the wheels to determine the new speed.
			if(running || electricStarterEngaged){
				if(rpm > currentRevlimitRPM && currentRevlimitRPM != -1){
					wheelForce = -rpm/currentMaxRPM*Math.signum(currentGear)*60;
				}else{
					wheelForce = (engineTargetRPM - rpm)/currentMaxRPM*currentGearRatio*vehicleOn.currentAxleRatio*(currentFuelConsumption + (currentSuperchargerFuelConsumption*currentSuperchargerEfficiency))*0.6F*30F;
				}
				if(wheelForce != 0){
					//Check to see if the wheels need to spin out.
					//If they do, we'll need to provide less force.
					if(Math.abs(wheelForce/300D) > wheelFriction || (Math.abs(lowestWheelVelocity) - Math.abs(desiredWheelVelocity) > 0.1 && Math.abs(lowestWheelVelocity) - Math.abs(desiredWheelVelocity) < Math.abs(wheelForce/300D))){
						wheelForce *= vehicleOn.currentMass/100000D*wheelFriction/Math.abs(wheelForce/300F);
						for(PartGroundDevice wheel : vehicleOn.groundDeviceCollective.drivenWheels){
							if(currentGearRatio > 0){
								if(wheelForce >= 0){
									wheel.angularVelocity = Math.min(engineTargetRPM/1200F/currentGearRatio/vehicleOn.currentAxleRatio, wheel.angularVelocity + 0.01D);
								}else{
									wheel.angularVelocity = Math.max(engineTargetRPM/1200F/currentGearRatio/vehicleOn.currentAxleRatio, wheel.angularVelocity - 0.01D);
								}
							}else{
								if(wheelForce >= 0){
									wheel.angularVelocity = Math.min(engineTargetRPM/1200F/currentGearRatio/vehicleOn.currentAxleRatio, wheel.angularVelocity + 0.01D);
								}else{
									wheel.angularVelocity = Math.max(engineTargetRPM/1200F/currentGearRatio/vehicleOn.currentAxleRatio, wheel.angularVelocity - 0.01D);
								}
							}
							wheel.skipAngularCalcs = true;
						}
					}else{
						//If we have wheels not on the ground and we drive them, adjust their velocity now.
						for(PartGroundDevice wheel : vehicleOn.groundDeviceCollective.drivenWheels){
							wheel.skipAngularCalcs = false;
							if(!vehicleOn.groundDeviceCollective.groundedGroundDevices.contains(wheel)){
								wheel.angularVelocity = lowestWheelVelocity;
							}
						}
					}
				}else if(currentGearRatio == 0){
					//Tell the wheels to not skid if they are already doing so.
					for(PartGroundDevice wheel : vehicleOn.groundDeviceCollective.drivenWheels){
						wheel.skipAngularCalcs = false;
					}
				}
				
				//Don't let us have negative engine force at low speeds.
				//This causes odd reversing behavior when the engine tries to maintain speed.
				if(((wheelForce < 0 && currentGear > 0) || (wheelForce > 0 && currentGear < 0)) && vehicleOn.velocity < 0.25){
					wheelForce = 0;
				}
			}else{
				//Not running, do engine braking.
				wheelForce = -rpm/currentMaxRPM*Math.signum(currentGear)*30;
			}
			engineForce.z += wheelForce;
		}
		
		//If we provide jet power, add it now.  This may be done with any parts or wheels on the ground.
		//Propellers max out at about 25 force, so use that to determine this force.
		if(definition.engine.jetPowerFactor > 0 && running){
			//First we need the air density (sea level 1.225) so we know how much air we are moving.
			//We then multiply that by the RPM and the fuel consumption to get the raw power produced
			//by the core of the engine.  This is speed-independent as the core will ALWAYS accelerate air.
			//Note that due to a lack of jet physics formulas available, this is "hacky math".
			double safeRPMFactor = rpm/currentMaxSafeRPM;
			double coreContribution = Math.max(10*airDensity*currentFuelConsumption*safeRPMFactor - definition.engine.bypassRatio, 0);
			
			//The fan portion is calculated similarly to how propellers are calculated.
			//This takes into account the air density, and relative speed of the engine versus the fan's desired speed.
			//Again, this is "hacky math", as for some reason there's no data on fan pitches.
			//In this case, however, we don't care about the fuelConsumption as that's only used by the core.
			double fanVelocityFactor = (0.0254*250*rpm/60/20 - engineAxialVelocity)/200D;
			double fanContribution = 10*airDensity*safeRPMFactor*fanVelocityFactor*definition.engine.bypassRatio;
			double thrust = (vehicleOn.reverseThrust ? -(coreContribution + fanContribution) : coreContribution + fanContribution)*definition.engine.jetPowerFactor;
			
			//Add the jet force to the engine.  Use the engine rotation to define the power vector.
			engineForce.add(new Point3d(0D, 0D, thrust).rotateFine(localAngles));
		}
		
		//Finally, return the force we calculated.
		return engineForce;
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setBoolean("isCreative", isCreative);
		data.setBoolean("oilLeak", oilLeak);
		data.setBoolean("fuelLeak", fuelLeak);
		data.setBoolean("brokenStarter", brokenStarter);
		data.setBoolean("running", running);
		data.setDouble("hours", hours);
		data.setDouble("rpm", rpm);
		data.setDouble("temp", temp);
		data.setDouble("pressure", pressure);
		return data;
	}
}
