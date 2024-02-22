package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.instances.ItemPartEngine;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.EngineType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

public class PartEngine extends APart {
    public static String ELECTRICITY_FUEL = "electricity";

    //State data.
    public boolean backfired;
    public boolean badShift;
    public boolean running;
    public byte forwardsGears;
    public byte reverseGears;
    public int upshiftCountdown;
    public int downshiftCountdown;
    public int internalFuel;
    public double hours;
    public double rpm;
    public double temp;
    public double pressure;
    public double propellerGearboxRatio;

    //Runtime calculated values.
    public double fuelFlow;
    public double rocketFuelUsed;
    public PartEngine linkedEngine;

    //Internal properties
    @ModifiedValue
    private double currentMaxRPM;
    @ModifiedValue
    private double currentMaxSafeRPM;
    @ModifiedValue
    private double currentRevlimitRPM;
    @ModifiedValue
    private double currentRevlimitBounce;
    @ModifiedValue
    private double currentRevResistance;
    @ModifiedValue
    private double currentIdleRPM;
    @ModifiedValue
    private double currentStartRPM;
    @ModifiedValue
    private double currentStallRPM;
    @ModifiedValue
    private double currentStarterPower;
    @ModifiedValue
    private double currentFuelConsumption;
    @ModifiedValue
    private double currentHeatingCoefficient;
    @ModifiedValue
    private double currentCoolingCoefficient;
    @ModifiedValue
    private double currentSuperchargerFuelConsumption;
    @ModifiedValue
    private double currentSuperchargerEfficiency;
    @ModifiedValue
    private double currentGearRatio;
    @ModifiedValue
    private double currentForceShift;
    @ModifiedValue
    public double currentIsAutomatic;
    @ModifiedValue
    private double currentWearFactor;
    @ModifiedValue
    private double currentWinddownRate;

    //Internal variables.
    private boolean autoStarterEngaged;
    private int starterLevel;
    private int shiftCooldown;
    private double lowestWheelVelocity;
    private double desiredWheelVelocity;
    private double engineAxialVelocity;
    private float wheelFriction;
    private double ambientTemp;
    private double coolingFactor;
    private double engineTargetRPM;
    private double engineRotation;
    private double prevEngineRotation;
    private double driveshaftRotation;
    private double prevDriveshaftRotation;
    private double currentJetPowerFactor;
    private double currentBypassRatio;
    private final List<PartGroundDevice> linkedWheels = new ArrayList<>();
    private final List<PartGroundDevice> drivenWheels = new ArrayList<>();
    private final List<PartPropeller> linkedPropellers = new ArrayList<>();
    private final Point3D engineAxisVector = new Point3D();
    private final Point3D engineForce = new Point3D();
    private double engineForceValue;

    //Constants and variables.
    public static final String HOURS_VARIABLE = "hours";
    public final ComputedVariable magnetoVar;
    public final ComputedVariable electricStarterVar;
    public final ComputedVariable handStarterVar;
    public final ComputedVariable currentGearVar;
    public final ComputedVariable shiftUpVar;
    public final ComputedVariable shiftDownVar;
    public final ComputedVariable shiftNeutralVar;
    public final ComputedVariable shiftSelectionVar;
    public final ComputedVariable hoursVar;
    public static final float COLD_TEMP = 30F;
    public static final float OVERHEAT_TEMP_1 = 115.556F;
    public static final float OVERHEAT_TEMP_2 = 121.111F;
    public static final float FAILURE_TEMP = 132.222F;
    public static final float LOW_OIL_PRESSURE = 40F;
    public static final float MAX_SHIFT_SPEED = 0.35F;

    public PartEngine(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartEngine item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        if (data != null) {
            this.running = data.getBoolean("running");
            this.rpm = data.getDouble("rpm");
            this.temp = data.getDouble("temp");
            this.pressure = data.getDouble("pressure");
            this.rocketFuelUsed = data.getDouble("rocketFuelUsed");
        }
        this.running = data.getBoolean("running");
        this.rpm = data.getDouble("rpm");
        this.temp = data.getDouble("temp");
        this.pressure = data.getDouble("pressure");
        for (float gear : definition.engine.gearRatios) {
            if (gear < 0) {
                ++reverseGears;
            } else if (gear > 0) {
                ++forwardsGears;
            }
        }
        addVariable(this.magnetoVar = new ComputedVariable(this, "engine_magneto", data));
        addVariable(this.electricStarterVar = new ComputedVariable(this, "engine_starter", data));
        addVariable(this.handStarterVar = new ComputedVariable(this, "engine_starter_hand", data));
        addVariable(this.currentGearVar = new ComputedVariable(this, "engine_gear", data));
        addVariable(this.shiftUpVar = new ComputedVariable(this, "engine_shift_up", data));
        addVariable(this.shiftDownVar = new ComputedVariable(this, "engine_shift_down", data));
        addVariable(this.shiftNeutralVar = new ComputedVariable(this, "engine_shift_neutral", data));
        addVariable(this.shiftSelectionVar = new ComputedVariable(this, "engine_shift_request", data));
        addVariable(this.hoursVar = new ComputedVariable(this, HOURS_VARIABLE, data));

        //Verify gears aren't out of range.  This can happen if a pack updates to lower number of gears.
        if (definition.engine.gearRatios.size() <= currentGearVar.currentValue + reverseGears) {
        	currentGearVar.setTo(forwardsGears + reverseGears - 1, false);
        }

        //If we are on an aircraft, set our gear to 1 as aircraft don't have shifters.
        //Well, except blimps, but that's a special case.
        if (vehicleOn != null && vehicleOn.definition.motorized.isAircraft) {
        	currentGearVar.setTo(1, false);
        }

        //Verify the vehicle has the right fuel for us.  If not, clear it out.
        //This allows us to swap in an engine with a different fuel type than the last one.
        if (vehicleOn != null && !vehicleOn.fuelTank.getFluid().isEmpty()) {
            switch (definition.engine.type) {
                case MAGIC: {
                    //Do nothing, magic engines don't need fuel.
                    break;
                }
                case ELECTRIC: {
                    //Check for electricity.
                    if (!vehicleOn.fuelTank.getFluid().equals(ELECTRICITY_FUEL)) {
                        vehicleOn.fuelTank.drain(vehicleOn.fuelTank.getFluid(), vehicleOn.fuelTank.getFluidLevel(), true);
                    }
                    break;
                }
                default: {
                    //Check for matching fuel from configs.
                    if (!ConfigSystem.settings.fuel.fuels.containsKey(definition.engine.fuelType)) {
                        throw new IllegalArgumentException("Engine:" + definition.packID + ":" + definition.systemName + " wanted fuel configs for fuel of type:" + definition.engine.fuelType + ", but these do not exist in the config file.  Fuels currently in the file are:" + ConfigSystem.settings.fuel.fuels.keySet() + "If you are on a server, this means the server and client configs are not the same.  If this is a modpack, TELL THE AUTHOR IT IS BROKEN!");
                    } else if (!ConfigSystem.settings.fuel.fuels.get(definition.engine.fuelType).containsKey(vehicleOn.fuelTank.getFluid())) {
                        vehicleOn.fuelTank.drain(vehicleOn.fuelTank.getFluid(), vehicleOn.fuelTank.getFluidLevel(), true);
                    }
                }
            }
        }
    }

    @Override
    public void attack(Damage damage) {
        super.attack(damage);
        if (!damage.isWater) {
            if (definition.engine.disableAutomaticStarter) {
                //Check if this is a hand-start command.
                if (damage.entityResponsible instanceof IWrapperPlayer && ((IWrapperPlayer) damage.entityResponsible).getHeldStack().isEmpty()) {
                    //Don't hand-start engines from seated players.  Lazy bums...
                    if (!masterEntity.allParts.contains(damage.entityResponsible.getEntityRiding())) {
                        handStartEngine();
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.HS_ON));
                        return;
                    }
                }
            }
            if (vehicleOn == null || !vehicleOn.isCreative) {
                double hoursApplied = damage.amount * ConfigSystem.settings.general.engineHoursFactor.value;
                if (damage.isExplosion) {
                    hoursApplied *= 10;
                }
                hours += hoursApplied;
                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, hoursApplied));
            }
        } else if (definition.engine.type == JSONPart.EngineType.NORMAL) {
            stallEngine(Signal.DROWN);
            InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.DROWN));
        }
    }

    @Override
    public void update() {
        super.update();
        //Reset states.
        backfired = false;
        badShift = false;

        //If the engine is running, but the magneto is off, turn the engine off.
        if (running && !magnetoVar.isActive) {
            running = false;
            if (definition.engine.type == JSONPart.EngineType.NORMAL) {
                internalFuel = 200;
            }
        }

        //Set fuel flow to 0 for the start of this cycle.
        fuelFlow = 0;

        //Remove values from shifting times if applicable.
        if (upshiftCountdown > 0) {
            --upshiftCountdown;
        }
        if (downshiftCountdown > 0) {
            --downshiftCountdown;
        }

        if (vehicleOn != null) {
            //Check to see if we are linked and need to equalize power between us and another engine.
            if (linkedEngine != null) {
                if (!linkedEngine.position.isDistanceToCloserThan(position, 16)) {
                    linkedEngine.linkedEngine = null;
                    linkedEngine = null;
                    if (world.isClient()) {
                        for (IWrapperPlayer player : world.getPlayersWithin(new BoundingBox(position, 16, 16, 16))) {
                            player.displayChatMessage(LanguageSystem.INTERACT_JUMPERCABLE_LINKDROPPED);
                        }
                    }
                } else if (vehicleOn.electricPower + 0.5 < linkedEngine.vehicleOn.electricPower) {
                    linkedEngine.vehicleOn.electricPower -= 0.005F;
                    vehicleOn.electricPower += 0.005F;
                } else if (vehicleOn.electricPower > linkedEngine.vehicleOn.electricPower + 0.5) {
                    vehicleOn.electricPower -= 0.005F;
                    linkedEngine.vehicleOn.electricPower += 0.005F;
                } else {
                    linkedEngine.linkedEngine = null;
                    linkedEngine = null;
                    if (world.isClient()) {
                        for (IWrapperPlayer player : world.getPlayersWithin(new BoundingBox(position, 16, 16, 16))) {
                            player.displayChatMessage(LanguageSystem.INTERACT_JUMPERCABLE_POWEREQUAL);
                        }
                    }
                }
            }

            //Add cooling for ambient temp.
            ambientTemp = (25 * world.getTemperature(position) + 5) * ConfigSystem.settings.general.engineBiomeTempFactor.value;
            if (running) {
                coolingFactor = 0.001 * currentCoolingCoefficient - (currentSuperchargerEfficiency / 1000F) * (rpm / 2000F) + (vehicleOn.velocity / 1000F) * currentCoolingCoefficient;
            } else {
                coolingFactor = 0.001 * currentCoolingCoefficient + (vehicleOn.velocity / 1000F) * currentCoolingCoefficient;
            }
            temp -= (temp - ambientTemp) * coolingFactor;

            //Check to see if electric or hand starter can keep running.
            if (electricStarterVar.isActive) {
                if (starterLevel == 0) {
                    if (vehicleOn.electricPower > 1) {
                        starterLevel += 4;
                    } else if (!world.isClient()) {
                    	electricStarterVar.toggle(true);
                    }
                }
                if (starterLevel > 0) {
                    if (!vehicleOn.isCreative) {
                        vehicleOn.electricUsage += 0.05F;
                    }
                    if (!vehicleOn.isCreative) {
                        fuelFlow += vehicleOn.fuelTank.drain(vehicleOn.fuelTank.getFluid(), getTotalFuelConsumption() * ConfigSystem.settings.general.fuelUsageFactor.value, !world.isClient());
                    }
                }
                if (autoStarterEngaged) {
                    if (!world.isClient() && running) {
                    	electricStarterVar.setTo(0, true);
                    }
                }
            } else if (handStarterVar.isActive) {
                if (starterLevel == 0) {
                	handStarterVar.setTo(0, false);
                }
            } else {
                starterLevel = 0;
                autoStarterEngaged = false;
            }

            //If the starter is running, adjust RPM.
            if (starterLevel > 0) {
                --starterLevel;
                if (rpm < currentStartRPM * 2) {
                    rpm = Math.min(rpm + currentStarterPower, currentStartRPM * 2);
                } else {
                    rpm = Math.max(rpm - currentStarterPower, currentStartRPM * 2);
                }
            }

            //Add extra hours if we are running the engine too fast.
            if (!vehicleOn.isCreative && rpm > currentMaxSafeRPM) {
                hours += (rpm - currentMaxSafeRPM) / currentMaxSafeRPM * getTotalWearFactor();
            }

            //Check for any shifting requests.
            if (!world.isClient()) {
                if (shiftNeutralVar.isActive) {
                	shiftNeutralVar.toggle(false);
                    shiftNeutral();
                }
                if (shiftUpVar.isActive) {
                	shiftUpVar.toggle(false);
                    shiftUp();
                }else {
                	if (shiftDownVar.isActive) {
                		shiftDownVar.toggle(false);
                        shiftDown();
                    } else if (shiftSelectionVar.isActive) {
                    	if (shiftSelectionVar.currentValue < 10) {
                            while (currentGearVar.currentValue < shiftSelectionVar.currentValue && shiftUp())
                                ;
                        } else if (shiftSelectionVar.currentValue == 10) {
                            if (currentGearVar.currentValue == 0) {
                                shiftDown();
                            }
                        } else if (shiftSelectionVar.currentValue == 11) {
                            shiftNeutral();
                        }
                    }
                }
            }

            //Check for reversing if we are on a blimp with reversed thrust.
            if (vehicleOn.definition.motorized.isBlimp && !linkedPropellers.isEmpty()) {
                if (vehicleOn.reverseThrustVar.isActive && currentGearVar.currentValue > 0) {
                	currentGearVar.setTo(-1, false);
                } else if (!vehicleOn.reverseThrustVar.isActive && currentGearVar.currentValue < 0) {
                    currentGearVar.setTo(1, false);
                }
            }

            //Update driven wheels.  These are a subset of linked depending on the wheel state.
            drivenWheels.clear();
            for (PartGroundDevice wheel : linkedWheels) {
                if (!wheel.isSpare && wheel.isActive && (wheel.definition.ground.isWheel || wheel.definition.ground.isTread)) {
                    drivenWheels.add(wheel);
                    wheel.drivenLastTick = true;
                }
            }

            //Do common running tasks.
            if (running) {

                //If we aren't creative, add hours.
                if (!vehicleOn.isCreative) {
                    hours += 0.001 * getTotalWearFactor();
                }

                //Stall engine for conditions.
                if (!world.isClient()) {
                    if (!isActive) {
                        stallEngine(Signal.INACTIVE);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.INACTIVE));
                    } else if (vehicleOn.outOfHealth) {
                        stallEngine(Signal.DEAD_VEHICLE);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.DEAD_VEHICLE));
                    } else if (ConfigSystem.settings.general.engineDimensionWhitelist.value.isEmpty() ? ConfigSystem.settings.general.engineDimensionBlacklist.value.contains(world.getName()) : !ConfigSystem.settings.general.engineDimensionWhitelist.value.contains(world.getName())) {
                        stallEngine(Signal.INVALID_DIMENSION);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.INVALID_DIMENSION));
                    }
                }

                //Do automatic transmission functions if needed.
                if (currentIsAutomatic != 0 && !world.isClient() && currentGearVar.currentValue != 0) {
                    if (shiftCooldown == 0) {
                        if (currentGearVar.currentValue > 0 ? currentGearVar.currentValue < forwardsGears : -currentGearVar.currentValue < reverseGears) {
                            //Can shift up, try to do so.
                            if (rpm > (definition.engine.upShiftRPM != null ? definition.engine.upShiftRPM.get((int) (currentGearVar.currentValue + reverseGears)) : (currentMaxSafeRPM * 0.9)) * 0.5F * (1.0F + vehicleOn.throttleVar.currentValue)) {
                                if (currentGearVar.currentValue > 0) {
                                    shiftUp();
                                } else {
                                    shiftDown();
                                }
                            }
                        }
                        if (currentGearVar.currentValue > 1 || currentGearVar.currentValue < -1) {
                            //Can shift down, try to do so.
                            if (rpm < (definition.engine.downShiftRPM != null ? definition.engine.downShiftRPM.get((int) (currentGearVar.currentValue + reverseGears)) * 0.5 * (1.0F + vehicleOn.throttleVar.currentValue) : (currentMaxSafeRPM * 0.9) * 0.25 * (1.0F + vehicleOn.throttleVar.currentValue))) {
                                if (currentGearVar.currentValue > 0) {
                                    shiftDown();
                                } else {
                                    shiftUp();
                                }
                            }
                        }
                    } else {
                        --shiftCooldown;
                    }
                }

                //Add extra hours, and possibly explode the engine, if it's too hot.
                if (temp > OVERHEAT_TEMP_1 && !vehicleOn.isCreative) {
                    hours += 0.001 * (temp - OVERHEAT_TEMP_1) * getTotalWearFactor();
                    if (temp > FAILURE_TEMP && !world.isClient()) {
                        explodeEngine();
                    }
                }
            }

            //Do running logic.
            switch (definition.engine.type) {
                case NORMAL: {
                    if (running) {
                        //Provide electric power to the vehicle we're in.
                        vehicleOn.electricUsage -= 0.05 * rpm / currentMaxRPM;

                        //Try to get fuel from the vehicle and calculate fuel flow.
                        if (!vehicleOn.isCreative && !vehicleOn.fuelTank.getFluid().isEmpty()) {
                            fuelFlow += vehicleOn.fuelTank.drain(vehicleOn.fuelTank.getFluid(), getTotalFuelConsumption() * ConfigSystem.settings.general.fuelUsageFactor.value / ConfigSystem.settings.fuel.fuels.get(definition.engine.fuelType).get(vehicleOn.fuelTank.getFluid()) * rpm / currentMaxRPM, !world.isClient());
                        }

                        //Add temp based on engine speed.
                        temp += Math.max(0, (7 * rpm / currentMaxRPM - temp / (COLD_TEMP * 2)) / 20) * currentHeatingCoefficient * ConfigSystem.settings.general.engineSpeedTempFactor.value;

                        //Adjust oil pressure based on RPM and leak status.
                        pressure = Math.min(90 - temp / 10, pressure + rpm / currentIdleRPM - 0.5 * (pressure / LOW_OIL_PRESSURE));

                        //Add extra hours and temp if we have low oil.
                        if (pressure < LOW_OIL_PRESSURE && !vehicleOn.isCreative) {
                            temp += Math.max(0, (20 * rpm / currentMaxRPM) / 20);
                            hours += 0.01 * getTotalWearFactor();
                        }

                        //If the engine has high hours, give a chance for a backfire.
                        if (hours >= 500 && !world.isClient()) {
                            if (Math.random() < (hours / 3) / (500 + (10000 - hours)) * (currentMaxSafeRPM / (rpm + currentMaxSafeRPM / 1.5))) {
                                backfireEngine();
                                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.BACKFIRE));
                            }
                        }

                        //Check if we need to stall the engine for various conditions.
                        if (!world.isClient()) {
                            if (isInLiquid()) {
                                stallEngine(Signal.DROWN);
                                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.DROWN));
                            } else if (!vehicleOn.isCreative && ConfigSystem.settings.general.fuelUsageFactor.value != 0 && vehicleOn.fuelTank.getFluidLevel() == 0) {
                                stallEngine(Signal.FUEL_OUT);
                                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.FUEL_OUT));
                            } else if (rpm < currentStallRPM) {
                                stallEngine(Signal.TOO_SLOW);
                                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.TOO_SLOW));
                            }
                        }
                    } else {
                        //Internal fuel is used for engine sound wind down.  NOT used for power.
                        if (internalFuel > 0) {
                            --internalFuel;
                            if (rpm < 500) {
                                internalFuel = 0;
                            }
                        }

                        //Start engine if the RPM is high enough to cause it to start by itself.
                        //Used for drowned engines that come out of the water, or engines that don't
                        //have the ability to engage a starter.
                        if (rpm >= currentStartRPM && !world.isClient() && !vehicleOn.outOfHealth) {
                            if (vehicleOn.isCreative || ConfigSystem.settings.general.fuelUsageFactor.value == 0 || vehicleOn.fuelTank.getFluidLevel() > 0) {
                                if (!isInLiquid() && magnetoVar.isActive) {
                                    startEngine();
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.START));
                                }
                            }
                        }
                    }
                    break;
                }

                case ROCKET: {
                    if (running) {
                        //Remove fuel, and if we don't have any, turn ourselves off.
                        rocketFuelUsed += getTotalFuelConsumption();
                        if (rocketFuelUsed >= definition.engine.rocketFuel) {
                            running = false;
                        }
                    } else {
                        //If the magneto comes on, and we have fuel, ignite.
                        if (magnetoVar.isActive && rocketFuelUsed < definition.engine.rocketFuel) {
                            running = true;
                        }
                    }
                    break;
                }

                case ELECTRIC: {
                    if (running) {
                        //Provide electric power to the vehicle we're in.
                        vehicleOn.electricUsage -= 0.05 * rpm / currentMaxRPM;

                        //Try to get fuel from the vehicle and calculate fuel flow.
                        if (!vehicleOn.isCreative && !vehicleOn.fuelTank.getFluid().isEmpty()) {
                            fuelFlow += vehicleOn.fuelTank.drain(vehicleOn.fuelTank.getFluid(), getTotalFuelConsumption() * ConfigSystem.settings.general.fuelUsageFactor.value * rpm / currentMaxRPM, !world.isClient());
                        }

                        //Check if we need to stall the engine for various conditions.
                        if (!world.isClient()) {
                            if (!vehicleOn.isCreative && ConfigSystem.settings.general.fuelUsageFactor.value != 0 && vehicleOn.fuelTank.getFluidLevel() == 0) {
                                stallEngine(Signal.FUEL_OUT);
                                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.FUEL_OUT));
                            }
                        }
                    } else {
                        //Turn on engine if the magneto is on and we have fuel.
                        if (!world.isClient() && !vehicleOn.outOfHealth) {
                            if (isActive && (vehicleOn.isCreative || ConfigSystem.settings.general.fuelUsageFactor.value == 0 || vehicleOn.fuelTank.getFluidLevel() > 0)) {
                                if (magnetoVar.isActive) {
                                    startEngine();
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.START));
                                }
                            }
                        }
                    }
                    break;
                }

                case MAGIC: {
                    if (running) {
                        //Provide electric power to the vehicle we're in.
                        vehicleOn.electricUsage -= 0.05 * rpm / currentMaxRPM;
                    } else {
                        //Turn on engine if the magneto is onl.
                        if (!world.isClient() && !vehicleOn.outOfHealth) {
                            if (isActive) {
                                if (magnetoVar.isActive){
                                    startEngine();
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.START));
                                }
                            }
                        }
                    }
                    break;
                }
            }

            //Update engine RPM.  This depends on what is connected.
            //First check to see if we need to check driven wheels.
            //While doing this we also get the friction those wheels are providing.
            //This is used later in force calculations.
            if (definition.engine.jetPowerFactor == 0 && !drivenWheels.isEmpty()) {
                lowestWheelVelocity = 999F;
                desiredWheelVelocity = -999F;
                wheelFriction = 0;
                engineTargetRPM = !electricStarterVar.isActive ? vehicleOn.throttleVar.currentValue * (currentMaxRPM - currentIdleRPM) / (1 + hours / 1500) + currentIdleRPM : currentStartRPM;

                //Update wheel friction and velocity.
                for (PartGroundDevice wheel : drivenWheels) {
                    //If we have grounded wheels, and this wheel is not on the ground, don't take it into account.
                    //This means the wheel is spinning in the air and can't provide force or feedback.
                    if (vehicleOn.groundDeviceCollective.groundedGroundDevices.contains(wheel)) {
                        wheelFriction += wheel.currentMotiveFriction;
                        lowestWheelVelocity = Math.min(wheel.angularVelocity, lowestWheelVelocity);
                        desiredWheelVelocity = Math.max(wheel.getDesiredAngularVelocity(), desiredWheelVelocity);
                    }
                }

                //Adjust RPM of the engine to wheels.
                if (currentGearRatio != 0 && starterLevel == 0) {
                    //Don't adjust it down to stall the engine, that can only be done via backfire.
                    if (wheelFriction > 0) {
                        double desiredRPM = lowestWheelVelocity * 1200F * currentGearRatio * vehicleOn.currentAxleRatio;
                        rpm += (desiredRPM - rpm) / currentRevResistance;
                        if (rpm < (currentIdleRPM - ((currentIdleRPM - currentStallRPM) * 0.5)) && running) {
                            rpm = currentIdleRPM - ((currentIdleRPM - currentStallRPM) * 0.5);
                        }
                    } else {
                        //No wheel force.  Adjust wheels to engine speed.
                        for (PartGroundDevice wheel : drivenWheels) {
                            wheel.angularVelocity = rpm / currentGearRatio / vehicleOn.currentAxleRatio / 1200D;
                        }
                    }
                }
            }

            //Do logic for those propellers now.
            propellerGearboxRatio = Math.signum(currentGearRatio) * (definition.engine.propellerRatio != 0 ? definition.engine.propellerRatio : Math.abs(currentGearRatio));
            for (PartPropeller attachedPropeller : linkedPropellers) {
                //Don't try and do logic for the propeller on their first tick.
                //They need to update once to init their properties.
                //Also don't let the propeller affect the engine speed if we are powering wheels.
                //Those take priority over air resistance.
                if (attachedPropeller.ticksExisted != 0 && wheelFriction == 0 && currentGearRatio != 0) {
                    boolean isPropellerInLiquid = attachedPropeller.isInLiquid();
                    double propellerForcePenalty = Math.max(0, (attachedPropeller.definition.propeller.diameter - 75) / (50 * (currentFuelConsumption + (currentSuperchargerFuelConsumption * currentSuperchargerEfficiency)) - 15));
                    double propellerFeedback = -Math.abs(attachedPropeller.airstreamLinearVelocity - attachedPropeller.desiredLinearVelocity) * (isPropellerInLiquid ? 6.5 : 2);
                    if (running) {
                        propellerFeedback -= propellerForcePenalty * 50;
                        engineTargetRPM = vehicleOn.throttleVar.currentValue * (currentMaxRPM - currentIdleRPM) / (1 + hours / 1500) + currentIdleRPM;
                        double engineRPMDifference = engineTargetRPM - rpm;

                        //propellerFeedback can't make an engine stall, but hours can.
                        if (rpm + engineRPMDifference / currentRevResistance > currentStallRPM && rpm + engineRPMDifference / currentRevResistance + propellerFeedback < currentStallRPM) {
                            rpm = currentStallRPM;
                        } else {
                            rpm += engineRPMDifference / currentRevResistance + propellerFeedback;
                        }
                    } else if (!electricStarterVar.isActive && !handStarterVar.isActive) {
                        rpm += (propellerFeedback - 1) * Math.abs(propellerGearboxRatio);

                        //Don't let the engine RPM go negative.  This results in physics errors.
                        if (rpm < 0) {
                            rpm = 0;
                        }
                    }
                }
            }

            //If wheel friction is 0, and we don't have a propeller, or we're in neutral, adjust RPM to throttle position.
            //Or, if we are not on, just slowly spin the engine down.
            if ((wheelFriction == 0 && linkedPropellers.isEmpty()) || currentGearRatio == 0) {
                if (running) {
                    if (rocketFuelUsed < definition.engine.rocketFuel) {
                        engineTargetRPM = currentMaxRPM;
                    } else {
                        engineTargetRPM = vehicleOn.throttleVar.currentValue * (currentMaxRPM - currentIdleRPM) / (1 + hours / 1500) + currentIdleRPM;
                    }
                    rpm += (engineTargetRPM - rpm) / (currentRevResistance * 3);
                    if (currentRevlimitRPM == -1) {
                        if (rpm > currentMaxSafeRPM) {
                            rpm -= Math.abs(engineTargetRPM - rpm) / 60;
                        }
                    } else {
                        if (rpm > currentRevlimitRPM) {
                            rpm -= Math.abs(engineTargetRPM - rpm) / currentRevlimitBounce;
                        }
                    }
                } else if (!electricStarterVar.isActive && !handStarterVar.isActive) {
                    rpm = Math.max(rpm - currentWinddownRate, 0); //engineWinddownRate tells us how quickly to slow down the engine, 10 rpm a tick by default
                }
            }

            ///Update variables used for jet thrust.
            if (definition.engine.jetPowerFactor > 0) {
                engineAxisVector.set(0, 0, 1).rotate(orientation);
                engineAxialVelocity = vehicleOn.motion.dotProduct(engineAxisVector, false);

                //Check for entities forward and aft of the engine and damage them.
                if (!world.isClient() && rpm >= 5000) {
                    boundingBox.widthRadius += 0.25;
                    boundingBox.heightRadius += 0.25;
                    boundingBox.depthRadius += 0.25;
                    boundingBox.globalCenter.add(vehicleOn.headingVector);
                    IWrapperEntity controller = vehicleOn.getController();
                    LanguageEntry language = controller != null ? LanguageSystem.DEATH_JETINTAKE_PLAYER : LanguageSystem.DEATH_JETINTAKE_NULL;
                    Damage jetIntake = new Damage(definition.engine.jetPowerFactor * ConfigSystem.settings.damage.jetDamageFactor.value * rpm / 1000F, boundingBox, this, controller, language);
                    world.attackEntities(jetIntake, null, false);

                    boundingBox.globalCenter.subtract(vehicleOn.headingVector);
                    boundingBox.globalCenter.subtract(vehicleOn.headingVector);
                    language = controller != null ? LanguageSystem.DEATH_JETEXHAUST_PLAYER : LanguageSystem.DEATH_JETEXHAUST_NULL;
                    Damage jetExhaust = new Damage(definition.engine.jetPowerFactor * ConfigSystem.settings.damage.jetDamageFactor.value * rpm / 2000F, boundingBox, this, controller, language).setFire();
                    world.attackEntities(jetExhaust, null, false);

                    boundingBox.globalCenter.add(vehicleOn.headingVector);
                    boundingBox.widthRadius -= 0.25;
                    boundingBox.heightRadius -= 0.25;
                    boundingBox.depthRadius -= 0.25;
                }
            }

            //Update engine and driveshaft rotation.
            //If we are linked to wheels on the ground follow the wheel rotation, not our own.
            prevEngineRotation = engineRotation;
            engineRotation += 360D * rpm / 1200D;
            if (engineRotation > 3600000) {
                engineRotation -= 3600000;
                prevEngineRotation -= 3600000;
            } else if (engineRotation < -3600000) {
                engineRotation += 3600000;
                prevEngineRotation += 3600000;
            }

            prevDriveshaftRotation = driveshaftRotation;
            double driveshaftDesiredSpeed = -999;
            for (PartGroundDevice wheel : drivenWheels) {
                driveshaftDesiredSpeed = Math.max(wheel.angularVelocity, driveshaftDesiredSpeed);
            }
            if (driveshaftDesiredSpeed != -999) {
                driveshaftRotation += 360D * driveshaftDesiredSpeed * vehicleOn.speedFactor;
            } else if (propellerGearboxRatio != 0) {
                driveshaftRotation += 360D * rpm / 1200D / propellerGearboxRatio;
            }
            if (driveshaftRotation > 3600000) {
                driveshaftRotation -= 3600000;
                prevDriveshaftRotation -= 3600000;
            } else if (driveshaftRotation < -3600000) {
                driveshaftRotation += 3600000;
                prevDriveshaftRotation += 3600000;
            }
        }
    }

    @Override
    public void updatePartList() {
        super.updatePartList();

        //Update linked wheel list.
        linkedWheels.clear();
        addLinkedPartsToList(linkedWheels, PartGroundDevice.class);
        List<PartGroundDevice> fakeWheels = new ArrayList<>();
        linkedWheels.forEach(wheel -> {
            if (wheel.fakePart != null) {
                fakeWheels.add(wheel.fakePart);
            }
        });
        linkedWheels.addAll(fakeWheels);

        //Update propellers that are linked to us.
        linkedPropellers.clear();
        parts.forEach(part -> {
            if (part instanceof PartPropeller) {
                linkedPropellers.add((PartPropeller) part);
            }
        });
        addLinkedPartsToList(linkedPropellers, PartPropeller.class);
    }

    @Override
    public void updateVariableModifiers() {
        currentMaxRPM = definition.engine.maxRPM;
        currentMaxSafeRPM = definition.engine.maxSafeRPM;
        currentRevlimitRPM = definition.engine.revlimitRPM;
        currentRevlimitBounce = definition.engine.revlimitBounce;
        currentRevResistance = definition.engine.revResistance;
        currentIdleRPM = definition.engine.idleRPM;
        currentStartRPM = definition.engine.startRPM;
        currentStallRPM = definition.engine.stallRPM;
        currentStarterPower = definition.engine.starterPower;
        currentFuelConsumption = definition.engine.fuelConsumption;
        currentHeatingCoefficient = definition.engine.heatingCoefficient;
        currentCoolingCoefficient = definition.engine.coolingCoefficient;
        currentSuperchargerFuelConsumption = definition.engine.superchargerFuelConsumption;
        currentSuperchargerEfficiency = definition.engine.superchargerEfficiency;
        currentGearRatio = definition.engine.gearRatios.get((int)currentGearVar.currentValue + reverseGears);
        currentForceShift = definition.engine.forceShift ? 1 : 0;
        currentIsAutomatic = definition.engine.isAutomatic ? 1 : 0;
        currentWearFactor = definition.engine.engineWearFactor;
        currentWinddownRate = definition.engine.engineWinddownRate;
        currentJetPowerFactor = definition.engine.jetPowerFactor;
        currentBypassRatio = definition.engine.bypassRatio;


        //Adjust current variables to modifiers, if any exist.
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                switch (modifier.variable) {
                    case "maxRPM":
                        currentMaxRPM = adjustVariable(modifier, currentMaxRPM);
                        break;
                    case "maxSafeRPM":
                        currentMaxSafeRPM = adjustVariable(modifier, currentMaxSafeRPM);
                        break;
                    case "revlimitRPM":
                        currentRevlimitRPM = adjustVariable(modifier, currentRevlimitRPM);
                        break;
                    case "revlimitBounce":
                        currentRevlimitBounce = adjustVariable(modifier, currentRevlimitBounce);
                        break;
                    case "revResistance":
                        currentRevResistance = adjustVariable(modifier, currentRevResistance);
                        break;
                    case "idleRPM":
                        currentIdleRPM = adjustVariable(modifier, currentIdleRPM);
                        break;
                    case "startRPM":
                        currentStartRPM = adjustVariable(modifier, currentStartRPM);
                        break;
                    case "stallRPM":
                        currentStallRPM = adjustVariable(modifier, currentStallRPM);
                        break;
                    case "starterPower":
                        currentStarterPower = adjustVariable(modifier, currentStarterPower);
                        break;
                    case "fuelConsumption":
                        currentFuelConsumption = adjustVariable(modifier, currentFuelConsumption);
                        break;
                    case "heatingCoefficient":
                        currentHeatingCoefficient = adjustVariable(modifier, currentHeatingCoefficient);
                        break;
                    case "coolingCoefficient":
                        currentCoolingCoefficient = adjustVariable(modifier, currentCoolingCoefficient);
                        break;
                    case "superchargerFuelConsumption":
                        currentSuperchargerFuelConsumption = adjustVariable(modifier, currentSuperchargerFuelConsumption);
                        break;
                    case "superchargerEfficiency":
                        currentSuperchargerEfficiency = adjustVariable(modifier, currentSuperchargerEfficiency);
                        break;
                    case "currentGearRatio":
                        currentGearRatio = adjustVariable(modifier, currentGearRatio);
                        break;
                    case "forceShift":
                    	currentForceShift = adjustVariable(modifier, currentForceShift);
                        break;
                    case "isAutomatic":
                    	currentIsAutomatic = adjustVariable(modifier, currentIsAutomatic);
                        break;
                    case "engineWearFactor":
                        currentWearFactor = adjustVariable(modifier, currentWearFactor);
                        break;
                    case "engineWinddownRate":
                        currentWinddownRate = adjustVariable(modifier, currentWinddownRate);
                        break;
                    case "jetPowerFactor":
                        currentJetPowerFactor = adjustVariable(modifier,(float) currentJetPowerFactor);
                        break;
                    case "bypassRatio":
                        currentBypassRatio = adjustVariable(modifier,(float) currentBypassRatio);
                        break;
                    default:
                    	ComputedVariable variable = getVariable(modifier.variable);
                    	variable.setTo(adjustVariable(modifier, variable.currentValue), false);
                        break;
                }
            }
        }
    }

    @Override
    public boolean isInLiquid() {
        return world.isBlockLiquid(position.copy().add(0, placementDefinition.intakeOffset, 0));
    }

    @Override
    public void remove() {
        super.remove();
        //Turn off and tell wheels to stop skipping calcs from being controlled by the engine.
        running = false;
        if (vehicleOn != null) {
            for (PartGroundDevice wheel : drivenWheels) {
                wheel.skipAngularCalcs = false;
            }
        }
    }


    @Override
    public ComputedVariable createComputedVariable(String variable) {
        switch (variable) {
            case ("engine_isautomatic"):
                return new ComputedVariable(this, variable, partialTicks -> currentIsAutomatic != 0 ? 1 : 0, false);
            case ("engine_rotation"):
                return new ComputedVariable(this, variable, partialTicks -> getEngineRotation(partialTicks), true);
            case ("engine_sin"):
                return new ComputedVariable(this, variable, partialTicks -> Math.sin(Math.toRadians(getEngineRotation(partialTicks))), true);
            case ("engine_cos"):
                return new ComputedVariable(this, variable, partialTicks -> Math.cos(Math.toRadians(getEngineRotation(partialTicks))), true);
            case ("engine_driveshaft_rotation"):
                return new ComputedVariable(this, variable, partialTicks -> getDriveshaftRotation(partialTicks), true);
            case ("engine_driveshaft_sin"):
                return new ComputedVariable(this, variable, partialTicks -> Math.sin(Math.toRadians(getDriveshaftRotation(partialTicks))), true);
            case ("engine_driveshaft_cos"):
                return new ComputedVariable(this, variable, partialTicks -> Math.cos(Math.toRadians(getDriveshaftRotation(partialTicks))), true);
            case ("engine_rpm"):
                return new ComputedVariable(this, variable, partialTicks -> rpm, false);
            case ("engine_rpm_safe"):
                return new ComputedVariable(this, variable, partialTicks -> currentMaxSafeRPM, false);
            case ("engine_rpm_max"):
                return new ComputedVariable(this, variable, partialTicks -> currentMaxRPM, false);
            case ("engine_rpm_revlimit"):
                return new ComputedVariable(this, variable, partialTicks -> currentRevlimitRPM, false);
            case ("engine_rpm_percent"):
                return new ComputedVariable(this, variable, partialTicks -> rpm / currentMaxRPM, false);
            case ("engine_rpm_percent_safe"):
                return new ComputedVariable(this, variable, partialTicks -> rpm / currentMaxSafeRPM, false);
            case ("engine_rpm_percent_revlimit"):
                return new ComputedVariable(this, variable, partialTicks -> currentRevlimitRPM != -1 ? rpm / currentRevlimitRPM : rpm / currentMaxSafeRPM, false);
            case ("engine_rpm_target"):
                return new ComputedVariable(this, variable, partialTicks -> engineTargetRPM, false);
            case ("engine_rpm_idle"):
                return new ComputedVariable(this, variable, partialTicks -> currentIdleRPM, false);
            case ("engine_rpm_start"):
                return new ComputedVariable(this, variable, partialTicks -> currentStartRPM, false);
            case ("engine_rpm_stall"):
                return new ComputedVariable(this, variable, partialTicks -> currentStallRPM, false);
            case ("engine_starter_power"):
                return new ComputedVariable(this, variable, partialTicks -> currentStarterPower, false);
            case ("engine_fuel_consumption"):
                return new ComputedVariable(this, variable, partialTicks -> currentFuelConsumption, false);
            case ("engine_supercharger_fuel_consumption"):
                return new ComputedVariable(this, variable, partialTicks -> currentSuperchargerFuelConsumption, false);
            case ("engine_supercharger_efficiency"):
                return new ComputedVariable(this, variable, partialTicks -> currentSuperchargerEfficiency, false);
            case ("engine_fuel_flow"):
                return new ComputedVariable(this, variable, partialTicks -> fuelFlow * 20D * 60D / 1000D, false);
            case ("engine_fuel_remaining"):
                return new ComputedVariable(this, variable, partialTicks -> (definition.engine.rocketFuel - rocketFuelUsed) / definition.engine.rocketFuel, false);
            case ("engine_temp"):
                return new ComputedVariable(this, variable, partialTicks -> temp, false);
            case ("engine_temp_ambient"):
                return new ComputedVariable(this, variable, partialTicks -> ambientTemp, false);
            case ("engine_pressure"):
                return new ComputedVariable(this, variable, partialTicks -> pressure, false);
            case ("engine_gear"):
                return new ComputedVariable(this, variable, partialTicks -> currentGearVar.currentValue, false);
            case ("engine_gearshift"):
                return new ComputedVariable(this, variable, partialTicks -> getGearshiftRotation(), false);
            case ("engine_gearshift_hvertical"):
                return new ComputedVariable(this, variable, partialTicks -> getGearshiftPosition_Vertical(), false);
            case ("engine_gearshift_hhorizontal"):
                return new ComputedVariable(this, variable, partialTicks -> getGearshiftPosition_Horizontal(), false);
            case ("engine_clutch_upshift"):
                return new ComputedVariable(this, variable, partialTicks -> upshiftCountdown > 0 ? 1 : 0, false);
            case ("engine_clutch_downshift"):
                return new ComputedVariable(this, variable, partialTicks -> downshiftCountdown > 0 ? 1 : 0, false);
            case ("engine_badshift"):
                return new ComputedVariable(this, variable, partialTicks -> badShift ? 1 : 0, false);
            case ("engine_reversed"):
                return new ComputedVariable(this, variable, partialTicks -> currentGearVar.currentValue < 0 ? 1 : 0, false);
            case ("engine_running"):
                return new ComputedVariable(this, variable, partialTicks -> running ? 1 : 0, false);
            case ("engine_powered"):
                return new ComputedVariable(this, variable, partialTicks -> running || internalFuel > 0 ? 1 : 0, false);
            case ("engine_backfired"):
                return new ComputedVariable(this, variable, partialTicks -> backfired ? 1 : 0, false);
            case ("engine_jumper_cable"):
                return new ComputedVariable(this, variable, partialTicks -> linkedEngine != null ? 1 : 0, false);
            case ("engine_hours"):
                return new ComputedVariable(this, variable, partialTicks -> hours, false);
            case ("engine_bypass_ratio"):
                return new ComputedVariable(this, variable, partialTicks -> currentBypassRatio, false);
            case ("engine_jet_power_factor"):
                return new ComputedVariable(this, variable, partialTicks -> currentJetPowerFactor, false);
            default: {
                if (variable.startsWith("engine_sin_")) {
                    final int offset = Integer.parseInt(variable.substring("engine_sin_".length()));
                    return new ComputedVariable(this, variable, partialTicks -> Math.sin(Math.toRadians(getEngineRotation(partialTicks) + offset)), true);
                } else if (variable.startsWith("engine_cos_")) {
                    final int offset = Integer.parseInt(variable.substring("engine_cos_".length()));
                    return new ComputedVariable(this, variable, partialTicks -> Math.cos(Math.toRadians(getEngineRotation(partialTicks) + offset)), true);
                } else if (variable.startsWith("engine_driveshaft_sin_")) {
                    final int offset = Integer.parseInt(variable.substring("engine_driveshaft_sin_".length()));
                    return new ComputedVariable(this, variable, partialTicks -> Math.sin(Math.toRadians(getDriveshaftRotation(partialTicks) + offset)), true);
                } else if (variable.startsWith("engine_driveshaft_cos_")) {
                    final int offset = Integer.parseInt(variable.substring("engine_driveshaft_cos_".length()));
                    return new ComputedVariable(this, variable, partialTicks -> Math.cos(Math.toRadians(getDriveshaftRotation(partialTicks) + offset)), true);
                } else if (variable.startsWith("engine_piston_")) {
                    //Divide the crank shaft rotation into a number of sectors, and return 1 when the crank is in the defined sector.
                    //i.e. engine_piston_2_6_0_crank will return 1 when the crank is in the second of 6 sectors.
                    //When suffixed with _cam, it will instead return the sector the camshaft rotation.

                    //If this a camshaft, set the multiplier to 2 and chop off the end of the variable string
                    final int camMultiplier;
                    if (variable.endsWith("_crank")) {
                        camMultiplier = 1;
                        variable = variable.substring(0, variable.length() - "_crank".length());
                    } else if (variable.endsWith("_cam")) {
                        camMultiplier = 2;
                        variable = variable.substring(0, variable.length() - "_cam".length());
                    } else {
                        //Invaild variable.
                        return ZERO_VARIABLE;
                    }

                    //Extract the values we need
                    String[] parsedVariable = variable.substring("engine_piston_".length()).split("_");
                    final int pistonNumber = Integer.parseInt(parsedVariable[0]);
                    final int totalPistons = Integer.parseInt(parsedVariable[1]);

                    //Safety to ensure the value always fluctuates and we don't have more sectors than are possible
                    if (pistonNumber <= totalPistons && totalPistons > 1) {
                        final double sector = (360D * camMultiplier) / totalPistons;
                        final int offset = parsedVariable.length >= 3 ? camMultiplier * Integer.parseInt(parsedVariable[2]) : 0;
                        return new ComputedVariable(this, variable, partialTicks -> {
                            //Map the shaft rotation to a value between 0 and 359.99...
                            double shaftRotation = Math.floorMod(Math.round(10 * (offset + getEngineRotation(partialTicks))), Math.round(3600D * camMultiplier)) / 10;

                            //If the crank is in the requested sector, return 1, otherwise return 0.
                            return (0 + (sector * (pistonNumber - 1)) <= shaftRotation) && (shaftRotation < sector + (sector * (pistonNumber - 1))) ? 1 : 0;
                        }, true);
                    } else {
                        //Invalid piston arrangement.
                        return ZERO_VARIABLE;
                    }
                } else {
                    return super.createComputedVariable(variable);
                }
            }
        }
    }

    //--------------------START OF ENGINE STATE CHANGE METHODS--------------------
    public void startEngine() {
        running = true;

        //Set oil pressure for normal engines.
        //Not setting this means we will start at 0 and damage the engine.
        if (definition.engine.type == JSONPart.EngineType.NORMAL) {
            pressure = 60;
        }
    }

    public void handStartEngine() {
    	magnetoVar.setTo(1, false);
    	handStarterVar.setTo(1, false);

        //Add a small amount to the starter level from the player's hand.
        starterLevel += 4;
    }

    public void autoStartEngine() {
        //Only engage auto-starter if we aren't running and we have the right fuel.
        if (!running && (vehicleOn.isCreative || ConfigSystem.settings.general.fuelUsageFactor.value == 0 || vehicleOn.fuelTank.getFluidLevel() > 0)) {
        	magnetoVar.setTo(1, false);
            if (definition.engine.type == JSONPart.EngineType.NORMAL) {
                autoStarterEngaged = true;
                electricStarterVar.setTo(1, false);
            }
        }
    }

    public void stallEngine(Signal signal) {
        running = false;

        //If we stalled due to not drowning, set internal fuel to play wind-down sounds.
        //Don't do this for electrics, as they just die.
        if (world.isClient()) {
            if (signal != Signal.DROWN && signal != Signal.INVALID_DIMENSION && definition.engine.type != EngineType.ELECTRIC) {
                internalFuel = 100;
            }
        }
    }

    public void backfireEngine() {
        //Decrease RPM and send off packet to have clients do the same. Also tells lug rpm to lug harder.
        backfired = true;
        rpm -= currentMaxRPM < 15000 ? Math.round((0.05*rpm)+((hours*0.05)-25)) : Math.round((0.1*rpm)+((hours*0.1)-50));
    }

    public void badShiftEngine() {
        //Just set bad shifting variable here.
        badShift = true;
    }

    protected void explodeEngine() {
        if (ConfigSystem.settings.damage.vehicleExplosions.value) {
            world.spawnExplosion(position, 1F, true);
        } else {
            world.spawnExplosion(position, 0F, false);
        }
        remove();
    }

    //--------------------START OF ENGINE GEAR METHODS--------------------

    public double getGearshiftRotation() {
        return currentIsAutomatic != 0 ? Math.min(1, currentGearVar.currentValue) * 15F : currentGearVar.currentValue * 5;
    }

    public float getGearshiftPosition_Vertical() {
        if (currentGearVar.currentValue < 0) {
            return definition.engine.gearRatios.size() % 2 == 0 ? 15 : -15;
        } else if (currentGearVar.currentValue == 0) {
            return 0;
        } else {
            return currentGearVar.currentValue % 2 == 0 ? -15 : 15;
        }
    }

    public double getGearshiftPosition_Horizontal() {
        int columns = (definition.engine.gearRatios.size()) / 2;
        int firstColumnAngle = columns / 2 * -5;
        float columnAngleDelta = columns != 1 ? -firstColumnAngle * 2 / (columns - 1) : 0;
        if (currentGearVar.currentValue < 0) {
            return -firstColumnAngle;
        } else if (currentGearVar.currentValue == 0) {
            return 0;
        } else {
            //Divide the currentGear-1 by two to get our column (0 for column 1, 1 for 2).
            //Then add multiply that by columnAngleDelta to get our delta for this column.
            //Return that value, plus the initial angle.
            return firstColumnAngle + (currentGearVar.currentValue - 1) / 2 * columnAngleDelta;
        }
    }

    public boolean shiftUp() {
        byte nextGear;
        boolean doShift = false;
        if (definition.engine.jetPowerFactor == 0) {
            //Check to make sure we can shift.
            if (currentGearVar.currentValue == forwardsGears) {
                //Already at highest gear, don't process things.
                return false;
            } else if (currentGearVar.currentValue == 0) {
                //Neutral to 1st.
                nextGear = 1;
                doShift = world.isClient() || vehicleOn.axialVelocity < MAX_SHIFT_SPEED || wheelFriction == 0 || !vehicleOn.goingInReverse || currentForceShift != 0;
            } else {//Gear to next gear.
                nextGear = (byte) (currentGearVar.currentValue + 1);
                doShift = true;
            }

            if (doShift) {
                currentGearVar.setTo(nextGear, false);
                shiftCooldown = definition.engine.shiftSpeed;
                upshiftCountdown = definition.engine.clutchTime;
                if (!world.isClient()) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.SHIFT_UP));
                }
            } else {
                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.BAD_SHIFT));
            }
        }
        return doShift;
    }

    public boolean shiftDown() {
        byte nextGear;
        boolean doShift = false;
        if (definition.engine.jetPowerFactor == 0) {
            //Check to make sure we can shift.
            if (currentGearVar.currentValue < 0 && -currentGearVar.currentValue == reverseGears) {
                //Already at lowest gear.
                return false;
            } else if (currentGearVar.currentValue == 0) {
                //Neutral to 1st reverse.
                nextGear = -1;
                doShift = world.isClient() || vehicleOn.axialVelocity < MAX_SHIFT_SPEED || wheelFriction == 0 || vehicleOn.goingInReverse || currentForceShift != 0;
            } else {//Gear to next gear.
                nextGear = (byte) (currentGearVar.currentValue - 1);
                doShift = true;
            }
            
            if (doShift) {
                currentGearVar.setTo(nextGear, false);
                shiftCooldown = definition.engine.shiftSpeed;
                downshiftCountdown = definition.engine.clutchTime;
                if (!world.isClient()) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.SHIFT_DOWN));
                }
            } else {
                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.BAD_SHIFT));
            }
        }
        return doShift;
    }

    public void shiftNeutral() {
        if (definition.engine.jetPowerFactor == 0) {
            if (currentGearVar.currentValue != 0) {//Any gear to neutral.
                if (currentGearVar.currentValue > 0) {
                    downshiftCountdown = definition.engine.clutchTime;
                } else {
                    upshiftCountdown = definition.engine.clutchTime;
                }
                shiftCooldown = definition.engine.shiftSpeed;
                currentGearVar.setTo(0, false);
                if (!world.isClient()) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.SHIFT_NEUTRAL));
                }
            }
        }
    }

    //--------------------START OF ENGINE PROPERTY METHODS--------------------
    public double getTotalFuelConsumption() {
        return currentFuelConsumption + currentSuperchargerFuelConsumption;
    }

    public double getTotalWearFactor() {
        if (currentSuperchargerEfficiency > 1.0F) {
            return currentWearFactor * currentSuperchargerEfficiency * ConfigSystem.settings.general.engineHoursFactor.value;
        } else {
            return currentWearFactor * ConfigSystem.settings.general.engineHoursFactor.value;
        }
    }

    public double getEngineRotation(float partialTicks) {
        return partialTicks != 0 ? prevEngineRotation + (engineRotation - prevEngineRotation) * partialTicks : engineRotation;
    }

    public double getDriveshaftRotation(float partialTicks) {
        return partialTicks != 0 ? prevDriveshaftRotation + (driveshaftRotation - prevDriveshaftRotation) * partialTicks : driveshaftRotation;
    }

    public double addToForceOutput(Point3D force, Point3D torque) {
        engineForce.set(0D, 0D, 0D);
        engineForceValue = 0;
        //First get wheel forces, if we have friction to do so.
        if (currentJetPowerFactor == 0 && wheelFriction != 0) {
            double wheelForce;
            //If running, use the friction of the wheels to determine the new speed.
            if (running || electricStarterVar.isActive) {
                if (rpm > currentRevlimitRPM && currentRevlimitRPM != -1) {
                    wheelForce = -rpm / currentMaxRPM * Math.signum(currentGearVar.currentValue) * 60;
                } else {
                    wheelForce = (engineTargetRPM - rpm) / currentMaxRPM * currentGearRatio * vehicleOn.currentAxleRatio * (currentFuelConsumption + (currentSuperchargerFuelConsumption * currentSuperchargerEfficiency)) * 0.6F * 30F;
                }

                if (wheelForce != 0) {
                    //Check to see if the wheels need to spin out.
                    //If they do, we'll need to provide less force.
                    if (Math.abs(wheelForce / 300D) > wheelFriction || (Math.abs(lowestWheelVelocity) - Math.abs(desiredWheelVelocity) > 0.1 && Math.abs(lowestWheelVelocity) - Math.abs(desiredWheelVelocity) < Math.abs(wheelForce / 300D))) {
                        wheelForce *= vehicleOn.currentMass / 100000D * wheelFriction / Math.abs(wheelForce / 300F);
                        for (PartGroundDevice wheel : drivenWheels) {
                            if (wheelForce >= 0) {
                                wheel.angularVelocity = Math.min(engineTargetRPM / 1200F / currentGearRatio / vehicleOn.currentAxleRatio, wheel.angularVelocity + 0.01D);
                            } else {
                                wheel.angularVelocity = Math.max(engineTargetRPM / 1200F / currentGearRatio / vehicleOn.currentAxleRatio, wheel.angularVelocity - 0.01D);
                            }
                            wheel.skipAngularCalcs = true;
                        }
                    } else {
                        //If we have wheels not on the ground and we drive them, adjust their velocity now.
                        for (PartGroundDevice wheel : drivenWheels) {
                            wheel.skipAngularCalcs = false;
                            if (!vehicleOn.groundDeviceCollective.groundedGroundDevices.contains(wheel)) {
                                wheel.angularVelocity = lowestWheelVelocity;
                            }
                        }
                    }
                } else if (currentGearRatio == 0) {
                    //Tell the wheels to not skid if they are already doing so.
                    for (PartGroundDevice wheel : drivenWheels) {
                        wheel.skipAngularCalcs = false;
                    }
                }

                //Don't let us have negative engine force at low speeds.
                //This causes odd reversing behavior when the engine tries to maintain speed.
                if (((wheelForce < 0 && currentGearRatio > 0) || (wheelForce > 0 && currentGearRatio < 0)) && vehicleOn.velocity < 0.25) {
                    wheelForce = 0;
                }
            } else {
                //Not running, do engine braking.
                wheelForce = -rpm / currentMaxRPM * Math.signum(currentGearVar.currentValue) * 30;
            }
            engineForceValue += wheelForce;
            engineForce.set(0, 0, wheelForce).rotate(vehicleOn.orientation);
            force.add(engineForce);
        }

        //If we provide jet power, add it now.  This may be done with any parts or wheels on the ground.
        //Propellers max out at about 25 force, so use that to determine this force.
        if (currentJetPowerFactor > 0 && running) {
            //First we need the air density (sea level 1.225) so we know how much air we are moving.
            //We then multiply that by the RPM and the fuel consumption to get the raw power produced
            //by the core of the engine.  This is speed-independent as the core will ALWAYS accelerate air.
            //Note that due to a lack of jet physics formulas available, this is "hacky math".
            double safeRPMFactor = rpm / currentMaxSafeRPM;
            double coreContribution = Math.max(10 * vehicleOn.airDensity * currentFuelConsumption * safeRPMFactor - currentBypassRatio, 0);

            //The fan portion is calculated similarly to how propellers are calculated.
            //This takes into account the air density, and relative speed of the engine versus the fan's desired speed.
            //Again, this is "hacky math", as for some reason there's no data on fan pitches.
            //In this case, however, we don't care about the fuelConsumption as that's only used by the core.
            double fanVelocityFactor = (0.0254 * 250 * rpm / 60 / 20 - engineAxialVelocity) / 200D;
            double fanContribution = 10 * vehicleOn.airDensity * safeRPMFactor * fanVelocityFactor * currentBypassRatio;
            double thrust = (vehicleOn.reverseThrustVar.isActive ? -(coreContribution + fanContribution) : coreContribution + fanContribution) * currentJetPowerFactor;

            //Add the jet force to the engine.  Use the engine rotation to define the power vector.
            engineForceValue += thrust;
            engineForce.set(engineAxisVector).scale(thrust);
            force.add(engineForce);
            engineForce.reOrigin(vehicleOn.orientation);
            torque.add(localOffset.crossProduct(engineForce));
        }
        return engineForceValue;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setBoolean("running", running);
        data.setDouble("rpm", rpm);
        data.setDouble("temp", temp);
        data.setDouble("pressure", pressure);
        data.setDouble("rocketFuelUsed", rocketFuelUsed);
        return data;
    }
}
