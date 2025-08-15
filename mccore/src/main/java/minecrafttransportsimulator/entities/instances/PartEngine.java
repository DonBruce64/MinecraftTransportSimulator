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
    public static final String ELECTRICITY_FUEL = "electricity";

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

    private final ComputedVariable maxRPMVar;
    private final ComputedVariable maxSafeRPMVar;
    private final ComputedVariable revLimitRPMVar;
    private final ComputedVariable revLimitBounceVar;
    private final ComputedVariable revResistanceVar;
    private final ComputedVariable idleRPMVar;
    private final ComputedVariable startRPMVar;
    private final ComputedVariable stallRPMVar;
    private final ComputedVariable starterPowerVar;
    private final ComputedVariable fuelConsumptionVar;
    private final ComputedVariable heatingCoefficientVar;
    private final ComputedVariable coolingCoefficientVar;
    private final ComputedVariable superchargerFuelConsumptionVar;
    private final ComputedVariable superchargerEfficiencyVar;
    private final ComputedVariable gearRatioVar;
    private final ComputedVariable forceShiftVar;
    public final ComputedVariable isAutomaticVar;
    private final ComputedVariable wearFactorVar;
    private final ComputedVariable winddownRateVar;
    private final ComputedVariable jetPowerFactorVar;
    private final ComputedVariable bypassRatioVar;

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
            this.hours = data.getDouble("hours");
        }
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

        addVariable(this.maxRPMVar = new ComputedVariable(this, "maxRPM"));
        addVariable(this.maxSafeRPMVar = new ComputedVariable(this, "maxSafeRPM"));
        addVariable(this.revLimitRPMVar = new ComputedVariable(this, "revlimitRPM"));
        addVariable(this.revLimitBounceVar = new ComputedVariable(this, "revlimitBounce"));
        addVariable(this.revResistanceVar = new ComputedVariable(this, "revResistance"));
        addVariable(this.idleRPMVar = new ComputedVariable(this, "idleRPM"));
        addVariable(this.startRPMVar = new ComputedVariable(this, "startRPM"));
        addVariable(this.stallRPMVar = new ComputedVariable(this, "stallRPM"));
        addVariable(this.starterPowerVar = new ComputedVariable(this, "starterPower"));
        addVariable(this.fuelConsumptionVar = new ComputedVariable(this, "fuelConsumption"));
        addVariable(this.heatingCoefficientVar = new ComputedVariable(this, "heatingCoefficient"));
        addVariable(this.coolingCoefficientVar = new ComputedVariable(this, "coolingCoefficient"));
        addVariable(this.superchargerFuelConsumptionVar = new ComputedVariable(this, "superchargerFuelConsumption"));
        addVariable(this.superchargerEfficiencyVar = new ComputedVariable(this, "superchargerEfficiency"));
        addVariable(this.gearRatioVar = new ComputedVariable(this, "gearRatio"));
        addVariable(this.forceShiftVar = new ComputedVariable(this, "forceShift"));
        addVariable(this.isAutomaticVar = new ComputedVariable(this, "isAutomatic"));
        addVariable(this.wearFactorVar = new ComputedVariable(this, "engineWearFactor"));
        addVariable(this.winddownRateVar = new ComputedVariable(this, "engineWinddownRate"));
        addVariable(this.jetPowerFactorVar = new ComputedVariable(this, "jetPowerFactor"));
        addVariable(this.bypassRatioVar = new ComputedVariable(this, "bypassRatio"));

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
                        vehicleOn.fuelTank.clear();
                    }
                    break;
                }
                default: {
                    //Check for matching fuel from configs.
                    if (!ConfigSystem.settings.fuel.fuels.containsKey(definition.engine.fuelType)) {
                        throw new IllegalArgumentException("Engine:" + definition.packID + ":" + definition.systemName + " wanted fuel configs for fuel of type:" + definition.engine.fuelType + ", but these do not exist in the config file.  Fuels currently in the file are:" + ConfigSystem.settings.fuel.fuels.keySet() + "If you are on a server, this means the server and client configs are not the same.  If this is a modpack, TELL THE AUTHOR IT IS BROKEN!");
                    } else if (!ConfigSystem.settings.fuel.fuels.get(definition.engine.fuelType).containsKey(vehicleOn.fuelTank.getFluid())) {
                        vehicleOn.fuelTank.clear();
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
                coolingFactor = 0.001 * coolingCoefficientVar.currentValue - (superchargerEfficiencyVar.currentValue / 1000F) * (rpm / 2000F) + (vehicleOn.velocity / 1000F) * coolingCoefficientVar.currentValue;
            } else {
                coolingFactor = 0.001 * coolingCoefficientVar.currentValue + (vehicleOn.velocity / 1000F) * coolingCoefficientVar.currentValue;
            }
            temp -= (temp - ambientTemp) * coolingFactor;

            //Check to see if electric or hand starter can keep running.
            if (electricStarterVar.isActive) {
                if ((outOfHealth || vehicleOn.outOfHealth) && !world.isClient()) {
                    electricStarterVar.toggle(true);
                    starterLevel = 0;
                    autoStarterEngaged = false;
                } else {
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
                            fuelFlow += vehicleOn.fuelTank.drain(getTotalFuelConsumption() * ConfigSystem.settings.general.fuelUsageFactor.value, !world.isClient());
                        }
                    }
                    if (autoStarterEngaged) {
                        if (!world.isClient() && running) {
                            electricStarterVar.setTo(0, true);
                        }
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
                if (rpm < startRPMVar.currentValue * 2) {
                    rpm = Math.min(rpm + starterPowerVar.currentValue, startRPMVar.currentValue * 2);
                } else {
                    rpm = Math.max(rpm - starterPowerVar.currentValue, startRPMVar.currentValue * 2);
                }
            }

            //Add extra hours if we are running the engine too fast.
            if (!vehicleOn.isCreative && rpm > maxSafeRPMVar.currentValue) {
                hours += (rpm - maxSafeRPMVar.currentValue) / maxSafeRPMVar.currentValue * getTotalWearFactor();
            }

            //Check for any shifting requests.
            if (shiftNeutralVar.isActive) {
                shiftNeutralVar.toggle(false);
                if (!world.isClient()) {
                    shiftNeutral();
                }
            } else if (shiftUpVar.isActive) {
                shiftUpVar.toggle(false);
                if (!world.isClient()) {
                    shiftUp();
                }
            } else if (shiftDownVar.isActive) {
                shiftDownVar.toggle(false);
                if (!world.isClient()) {
                    shiftDown();
                }
            } else if (shiftSelectionVar.isActive) {
                if (!world.isClient()) {
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
                    } else if (outOfHealth || vehicleOn.outOfHealth) {
                        stallEngine(Signal.OUT_OF_HEALTH);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.OUT_OF_HEALTH));
                    } else if (isInvalidDimension()) {
                        stallEngine(Signal.INVALID_DIMENSION);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.INVALID_DIMENSION));
                    }
                }

                //Do automatic transmission functions if needed.
                if (isAutomaticVar.isActive && !world.isClient() && currentGearVar.currentValue != 0) {
                    if (shiftCooldown == 0) {
                        if (currentGearVar.currentValue > 0 ? currentGearVar.currentValue < forwardsGears : -currentGearVar.currentValue < reverseGears) {
                            //Can shift up, try to do so.
                            if (rpm > (definition.engine.upShiftRPM != null ? definition.engine.upShiftRPM.get((int) (currentGearVar.currentValue + reverseGears)) : (maxSafeRPMVar.currentValue * 0.9)) * 0.5F * (1.0F + vehicleOn.throttleVar.currentValue)) {
                                if (currentGearVar.currentValue > 0) {
                                    shiftUp();
                                } else {
                                    shiftDown();
                                }
                            }
                        }
                        if (currentGearVar.currentValue > 1 || currentGearVar.currentValue < -1) {
                            //Can shift down, try to do so.
                            if (rpm < (definition.engine.downShiftRPM != null ? definition.engine.downShiftRPM.get((int) (currentGearVar.currentValue + reverseGears)) * 0.5 * (1.0F + vehicleOn.throttleVar.currentValue) : (maxSafeRPMVar.currentValue * 0.9) * 0.25 * (1.0F + vehicleOn.throttleVar.currentValue))) {
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
                        vehicleOn.electricUsage -= 0.05 * rpm / maxRPMVar.currentValue;

                        //Try to get fuel from the vehicle and calculate fuel flow.
                        if (!vehicleOn.isCreative && !vehicleOn.fuelTank.getFluid().isEmpty()) {
                            fuelFlow += vehicleOn.fuelTank.drain(getTotalFuelConsumption() * ConfigSystem.settings.general.fuelUsageFactor.value / ConfigSystem.settings.fuel.fuels.get(definition.engine.fuelType).get(vehicleOn.fuelTank.getFluid()) * rpm / maxRPMVar.currentValue, !world.isClient());
                        }

                        //Add temp based on engine speed.
                        temp += Math.max(0, (7 * rpm / maxRPMVar.currentValue - temp / (COLD_TEMP * 2)) / 20) * heatingCoefficientVar.currentValue * ConfigSystem.settings.general.engineSpeedTempFactor.value;

                        //Adjust oil pressure based on RPM and leak status.
                        pressure = Math.min(90 - temp / 10, pressure + rpm / idleRPMVar.currentValue - 0.5 * (pressure / LOW_OIL_PRESSURE));

                        //Add extra hours and temp if we have low oil.
                        if (pressure < LOW_OIL_PRESSURE && !vehicleOn.isCreative) {
                            temp += Math.max(0, (20 * rpm / maxRPMVar.currentValue) / 20);
                            hours += 0.01 * getTotalWearFactor();
                        }

                        //If the engine has high hours, give a chance for a backfire.
                        if (hours >= 500 && !world.isClient()) {
                            if (Math.random() < (hours / 3) / (500 + (10000 - hours)) * (maxSafeRPMVar.currentValue / (rpm + maxSafeRPMVar.currentValue / 1.5))) {
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
                            } else if (rpm < stallRPMVar.currentValue) {
                                stallEngine(Signal.TOO_SLOW);
                                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.TOO_SLOW));
                            } else if (!isActive) {
                                stallEngine(Signal.INACTIVE);
                                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(this, Signal.INACTIVE));
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
                        if (rpm >= startRPMVar.currentValue && !world.isClient() && !outOfHealth && !vehicleOn.outOfHealth && !isInvalidDimension()) {
                            if (vehicleOn.isCreative || ConfigSystem.settings.general.fuelUsageFactor.value == 0 || vehicleOn.fuelTank.getFluidLevel() > 0) {
                                if (isActive && !isInLiquid() && magnetoVar.isActive) {
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
                        if (!isActive || rocketFuelUsed >= definition.engine.rocketFuel) {
                            running = false;
                        }
                    } else {
                        //If the magneto comes on, and we have fuel, ignite.
                        if (isActive && !outOfHealth && magnetoVar.isActive && rocketFuelUsed < definition.engine.rocketFuel) {
                            running = true;
                        }
                    }
                    break;
                }

                case ELECTRIC: {
                    if (running) {
                        //Provide electric power to the vehicle we're in.
                        vehicleOn.electricUsage -= 0.05 * rpm / maxRPMVar.currentValue;

                        //Try to get fuel from the vehicle and calculate fuel flow.
                        if (!vehicleOn.isCreative && !vehicleOn.fuelTank.getFluid().isEmpty()) {
                            fuelFlow += vehicleOn.fuelTank.drain(getTotalFuelConsumption() * ConfigSystem.settings.general.fuelUsageFactor.value * rpm / maxRPMVar.currentValue, !world.isClient());
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
                        if (!world.isClient() && !outOfHealth && !vehicleOn.outOfHealth) {
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
                        vehicleOn.electricUsage -= 0.05 * rpm / maxRPMVar.currentValue;
                    } else {
                        //Turn on engine if the magneto is onl.
                        if (!world.isClient() && !outOfHealth && !vehicleOn.outOfHealth) {
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
            wheelFriction = 0;
            if (definition.engine.jetPowerFactor == 0 && !drivenWheels.isEmpty()) {
                lowestWheelVelocity = 999F;
                desiredWheelVelocity = -999F;
                engineTargetRPM = !electricStarterVar.isActive ? vehicleOn.throttleVar.currentValue * (maxRPMVar.currentValue - idleRPMVar.currentValue) / (1 + hours / 1500) + idleRPMVar.currentValue : startRPMVar.currentValue;

                //Update wheel friction and velocity.
                for (PartGroundDevice wheel : drivenWheels) {
                    //If we have grounded wheels, and this wheel is not on the ground, don't take it into account.
                    //This means the wheel is spinning in the air and can't provide force or feedback.
                    if (vehicleOn.groundDeviceCollective.groundedGroundDevices.contains(wheel)) {
                        wheelFriction += wheel.motiveFrictionVar.currentValue;
                        lowestWheelVelocity = Math.min(wheel.angularVelocity, lowestWheelVelocity);
                        desiredWheelVelocity = Math.max(wheel.getDesiredAngularVelocity(), desiredWheelVelocity);
                    }
                }

                //Adjust RPM of the engine to wheels.
                if (gearRatioVar.currentValue != 0 && starterLevel == 0) {
                    //Don't adjust it down to stall the engine, that can only be done via backfire.
                    if (wheelFriction > 0) {
                        double desiredRPM = lowestWheelVelocity * 1200F * gearRatioVar.currentValue * vehicleOn.axleRatioVar.currentValue;
                        rpm += (desiredRPM - rpm) / revResistanceVar.currentValue;
                        if (rpm < (idleRPMVar.currentValue - ((idleRPMVar.currentValue - stallRPMVar.currentValue) * 0.5)) && running) {
                            rpm = idleRPMVar.currentValue - ((idleRPMVar.currentValue - stallRPMVar.currentValue) * 0.5);
                        }
                    } else {
                        //No wheel force.  Adjust wheels to engine speed.
                        for (PartGroundDevice wheel : drivenWheels) {
                            wheel.angularVelocity = rpm / gearRatioVar.currentValue / vehicleOn.axleRatioVar.currentValue / 1200D;
                        }
                    }
                }
            }

            //Do logic for those propellers now.
            propellerGearboxRatio = Math.signum(gearRatioVar.currentValue) * (definition.engine.propellerRatio != 0 ? definition.engine.propellerRatio : Math.abs(gearRatioVar.currentValue));
            for (PartPropeller attachedPropeller : linkedPropellers) {
                //Don't try and do logic for the propeller on their first tick.
                //They need to update once to init their properties.
                //Also don't let the propeller affect the engine speed if we are powering wheels.
                //Those take priority over air resistance.
                if (attachedPropeller.ticksExisted != 0 && wheelFriction == 0 && gearRatioVar.currentValue != 0) {
                    boolean isPropellerInLiquid = attachedPropeller.isInLiquid();
                    double propellerForcePenalty = Math.max(0, (attachedPropeller.definition.propeller.diameter - 75) / (50 * (fuelConsumptionVar.currentValue + (superchargerFuelConsumptionVar.currentValue * superchargerEfficiencyVar.currentValue)) - 15));
                    double propellerFeedback = -Math.abs(attachedPropeller.airstreamLinearVelocity - attachedPropeller.desiredLinearVelocity) * (isPropellerInLiquid ? 6.5 : 2);
                    if (running) {
                        propellerFeedback -= propellerForcePenalty * 50;
                        engineTargetRPM = vehicleOn.throttleVar.currentValue * (maxRPMVar.currentValue - idleRPMVar.currentValue) / (1 + hours / 1500) + idleRPMVar.currentValue;
                        double engineRPMDifference = engineTargetRPM - rpm;

                        //propellerFeedback can't make an engine stall, but hours can.
                        if (rpm + engineRPMDifference / revResistanceVar.currentValue > stallRPMVar.currentValue && rpm + engineRPMDifference / revResistanceVar.currentValue + propellerFeedback < stallRPMVar.currentValue) {
                            rpm = stallRPMVar.currentValue;
                        } else {
                            rpm += engineRPMDifference / revResistanceVar.currentValue + propellerFeedback;
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
            if ((wheelFriction == 0 && linkedPropellers.isEmpty()) || gearRatioVar.currentValue == 0) {
                if (running) {
                    if (rocketFuelUsed < definition.engine.rocketFuel) {
                        engineTargetRPM = maxRPMVar.currentValue;
                    } else {
                        engineTargetRPM = vehicleOn.throttleVar.currentValue * (maxRPMVar.currentValue - idleRPMVar.currentValue) / (1 + hours / 1500) + idleRPMVar.currentValue;
                    }
                    rpm += (engineTargetRPM - rpm) / (revResistanceVar.currentValue * 3);
                    if (revLimitRPMVar.currentValue == -1) {
                        if (rpm > maxSafeRPMVar.currentValue) {
                            rpm -= Math.abs(engineTargetRPM - rpm) / 60;
                        }
                    } else {
                        if (rpm > revLimitRPMVar.currentValue) {
                            rpm -= Math.abs(engineTargetRPM - rpm) / revLimitBounceVar.currentValue;
                        }
                    }
                } else if (!electricStarterVar.isActive && !handStarterVar.isActive) {
                    rpm = Math.max(rpm - winddownRateVar.currentValue, 0); //engineWinddownRate tells us how quickly to slow down the engine, 10 rpm a tick by default
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

    public boolean isInvalidDimension() {
        return ConfigSystem.settings.general.engineDimensionWhitelist.value.isEmpty() ? ConfigSystem.settings.general.engineDimensionBlacklist.value.contains(world.getName()) : !ConfigSystem.settings.general.engineDimensionWhitelist.value.contains(world.getName());
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
    public void setVariableDefaults() {
        super.setVariableDefaults();
        maxRPMVar.setTo(definition.engine.maxRPM, false);
        maxSafeRPMVar.setTo(definition.engine.maxSafeRPM, false);
        revLimitRPMVar.setTo(definition.engine.revlimitRPM, false);
        revLimitBounceVar.setTo(definition.engine.revlimitBounce, false);
        revResistanceVar.setTo(definition.engine.revResistance, false);
        idleRPMVar.setTo(definition.engine.idleRPM, false);
        startRPMVar.setTo(definition.engine.startRPM, false);
        stallRPMVar.setTo(definition.engine.stallRPM, false);
        starterPowerVar.setTo(definition.engine.starterPower, false);
        fuelConsumptionVar.setTo(definition.engine.fuelConsumption, false);
        heatingCoefficientVar.setTo(definition.engine.heatingCoefficient, false);
        coolingCoefficientVar.setTo(definition.engine.coolingCoefficient, false);
        superchargerFuelConsumptionVar.setTo(definition.engine.superchargerFuelConsumption, false);
        superchargerEfficiencyVar.setTo(definition.engine.superchargerEfficiency, false);
        gearRatioVar.setTo(definition.engine.gearRatios.get((int) currentGearVar.currentValue + reverseGears), false);
        forceShiftVar.setActive(definition.engine.forceShift, false);
        isAutomaticVar.setActive(definition.engine.isAutomatic, false);
        wearFactorVar.setTo(definition.engine.engineWearFactor, false);
        winddownRateVar.setTo(definition.engine.engineWinddownRate, false);
        jetPowerFactorVar.setTo(definition.engine.jetPowerFactor, false);
        bypassRatioVar.setTo(definition.engine.bypassRatio, false);
    }

    @Override
    public boolean isInLiquid() {
        return world.isBlockLiquid(position.copy().add(0, placementDefinition.intakeOffset, 0));
    }

    @Override
    public void remove() {
        super.remove();
        //Turn off and tell wheels to stop skipping calcs from being controlled by the engine.
        if (vehicleOn != null) {
            for (PartGroundDevice wheel : drivenWheels) {
                wheel.skipAngularCalcs = false;
            }
        }
    }


    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
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
            case ("engine_rpm_percent"):
                return new ComputedVariable(this, variable, partialTicks -> rpm / maxRPMVar.currentValue, false);
            case ("engine_rpm_percent_safe"):
                return new ComputedVariable(this, variable, partialTicks -> rpm / maxSafeRPMVar.currentValue, false);
            case ("engine_rpm_percent_revlimit"):
                return new ComputedVariable(this, variable, partialTicks -> revLimitRPMVar.currentValue != -1 ? rpm / revLimitRPMVar.currentValue : rpm / maxSafeRPMVar.currentValue, false);
            case ("engine_rpm_target"):
                return new ComputedVariable(this, variable, partialTicks -> engineTargetRPM, false);
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
                    //Note that some packs omit the third offset number, so we need to account for that here.

                    //Extract the values we need.  Need these to be final for lambdas.
                    String[] parsedVariable = variable.split("_");
                    final int pistonNumber;
                    final int totalPistons;

                    //Safety to ensure the value always fluctuates and we don't have more sectors than are possible
                    int parsedPistonNumber = Integer.parseInt(parsedVariable[2]);
                    int parsedTotalPistons = Integer.parseInt(parsedVariable[3]);
                    if (parsedPistonNumber > parsedTotalPistons || parsedTotalPistons == 1) {
                        pistonNumber = 1;
                        totalPistons = 2;
                    } else {
                        pistonNumber = parsedPistonNumber;
                        totalPistons = parsedTotalPistons;
                    }

                    final int camMultiplier;
                    switch (parsedVariable[parsedVariable.length - 1]) {
                        case "crank": {
                            camMultiplier = 1;
                            break;
                        }
                        case "cam": {
                            camMultiplier = 2;
                            break;
                        }
                        default: {
                            //Invaild variable.
                            return new ComputedVariable(false);
                        }
                    }
                    final int offset = parsedVariable.length == 6 ? camMultiplier * Integer.parseInt(parsedVariable[4]) : 0;
                    final double sector = (360D * camMultiplier) / totalPistons;
                    return new ComputedVariable(this, variable, partialTicks -> {
                        //Map the shaft rotation to a value between 0 and 359.99...
                        double shaftRotation = Math.floorMod(Math.round(10 * (offset + getEngineRotation(partialTicks))), Math.round(3600D * camMultiplier)) / 10;

                        //If the crank is in the requested sector, return 1, otherwise return 0.
                        return ((sector * (pistonNumber - 1)) <= shaftRotation) && (shaftRotation < sector + (sector * (pistonNumber - 1))) ? 1 : 0;
                    }, true);
                } else {
                    return super.createComputedVariable(variable, createDefaultIfNotPresent);
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
    	handStarterVar.setTo(1, false);

        //Add a small amount to the starter level from the player's hand.
        starterLevel += 4;
    }

    public void autoStartEngine() {
        //Only engage auto-starter if we aren't running, have health, and we have the right fuel.
        if (!running && !outOfHealth && !vehicleOn.outOfHealth && (vehicleOn.isCreative || ConfigSystem.settings.general.fuelUsageFactor.value == 0 || vehicleOn.fuelTank.getFluidLevel() > 0)) {
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
        rpm -= maxRPMVar.currentValue < 15000 ? Math.round((0.05 * rpm) + ((hours * 0.05) - 25)) : Math.round((0.1 * rpm) + ((hours * 0.1) - 50));
    }

    public void badShiftEngine() {
        //Just set bad shifting variable here.
        badShift = true;
    }

    protected void explodeEngine() {
        world.spawnExplosion(position, 0F, false, false);
        entityOn.removePart(this, false, true);
    }

    //--------------------START OF ENGINE GEAR METHODS--------------------

    public double getGearshiftRotation() {
        return isAutomaticVar.isActive ? Math.min(1, currentGearVar.currentValue) * 15F : currentGearVar.currentValue * 5;
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
                doShift = world.isClient() || vehicleOn.axialVelocity < MAX_SHIFT_SPEED || wheelFriction == 0 || !vehicleOn.goingInReverse || forceShiftVar.isActive;
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
                doShift = world.isClient() || vehicleOn.axialVelocity < MAX_SHIFT_SPEED || wheelFriction == 0 || vehicleOn.goingInReverse || forceShiftVar.isActive;
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
        return fuelConsumptionVar.currentValue + superchargerFuelConsumptionVar.currentValue;
    }

    public double getTotalWearFactor() {
        if (superchargerEfficiencyVar.currentValue > 1.0F) {
            return wearFactorVar.currentValue * superchargerEfficiencyVar.currentValue * ConfigSystem.settings.general.engineHoursFactor.value;
        } else {
            return wearFactorVar.currentValue * ConfigSystem.settings.general.engineHoursFactor.value;
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
        if (!jetPowerFactorVar.isActive && wheelFriction != 0) {
            double wheelForce;
            //If running, use the friction of the wheels to determine the new speed.
            if (running || electricStarterVar.isActive) {
                if (rpm > revLimitRPMVar.currentValue && revLimitRPMVar.currentValue != -1) {
                    wheelForce = -rpm / maxRPMVar.currentValue * Math.signum(currentGearVar.currentValue) * 60;
                } else {
                    wheelForce = (engineTargetRPM - rpm) / maxRPMVar.currentValue * gearRatioVar.currentValue * vehicleOn.axleRatioVar.currentValue * (fuelConsumptionVar.currentValue + (superchargerFuelConsumptionVar.currentValue * superchargerEfficiencyVar.currentValue)) * 0.6F * 30F;
                }

                if (wheelForce != 0) {
                    //Check to see if the wheels need to spin out.
                    //If they do, we'll need to provide less force.
                    if (Math.abs(wheelForce / 300D) > wheelFriction || (Math.abs(lowestWheelVelocity) - Math.abs(desiredWheelVelocity) > 0.1 && Math.abs(lowestWheelVelocity) - Math.abs(desiredWheelVelocity) < Math.abs(wheelForce / 300D))) {
                        wheelForce *= vehicleOn.currentMass / 100000D * wheelFriction / Math.abs(wheelForce / 300F);
                        for (PartGroundDevice wheel : drivenWheels) {
                            if (wheelForce >= 0) {
                                wheel.angularVelocity = Math.min(engineTargetRPM / 1200F / gearRatioVar.currentValue / vehicleOn.axleRatioVar.currentValue, wheel.angularVelocity + 0.01D);
                            } else {
                                wheel.angularVelocity = Math.max(engineTargetRPM / 1200F / gearRatioVar.currentValue / vehicleOn.axleRatioVar.currentValue, wheel.angularVelocity - 0.01D);
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
                } else if (gearRatioVar.currentValue == 0) {
                    //Tell the wheels to not skid if they are already doing so.
                    for (PartGroundDevice wheel : drivenWheels) {
                        wheel.skipAngularCalcs = false;
                    }
                }

                //Don't let us have negative engine force at low speeds.
                //This causes odd reversing behavior when the engine tries to maintain speed.
                if (((wheelForce < 0 && gearRatioVar.currentValue > 0) || (wheelForce > 0 && gearRatioVar.currentValue < 0)) && vehicleOn.velocity < 0.25) {
                    wheelForce = 0;
                }
            } else {
                //Not running, do engine braking.
                wheelForce = -rpm / maxRPMVar.currentValue * Math.signum(currentGearVar.currentValue) * 30;
            }
            engineForceValue += wheelForce;
            engineForce.set(0, 0, wheelForce).rotate(vehicleOn.orientation);
            force.add(engineForce);
        }

        //If we provide jet power, add it now.  This may be done with any parts or wheels on the ground.
        //Propellers max out at about 25 force, so use that to determine this force.
        if (jetPowerFactorVar.isActive && running) {
            //First we need the air density (sea level 1.225) so we know how much air we are moving.
            //We then multiply that by the RPM and the fuel consumption to get the raw power produced
            //by the core of the engine.  This is speed-independent as the core will ALWAYS accelerate air.
            //Note that due to a lack of jet physics formulas available, this is "hacky math".
            double safeRPMFactor = rpm / maxSafeRPMVar.currentValue;
            double coreContribution = Math.max(10 * vehicleOn.airDensity * fuelConsumptionVar.currentValue * safeRPMFactor - bypassRatioVar.currentValue, 0);

            //The fan portion is calculated similarly to how propellers are calculated.
            //This takes into account the air density, and relative speed of the engine versus the fan's desired speed.
            //Again, this is "hacky math", as for some reason there's no data on fan pitches.
            //In this case, however, we don't care about the fuelConsumption as that's only used by the core.
            double fanVelocityFactor = (0.0254 * 250 * rpm / 60 / 20 - engineAxialVelocity) / 200D;
            double fanContribution = 10 * vehicleOn.airDensity * safeRPMFactor * fanVelocityFactor * bypassRatioVar.currentValue;
            double thrust = (vehicleOn.reverseThrustVar.isActive ? -(coreContribution + fanContribution) : coreContribution + fanContribution) * jetPowerFactorVar.currentValue;

            //Add the jet force to the engine.  Use the engine rotation to define the power vector.
            engineForceValue += thrust;
            engineForce.set(engineAxisVector).scale(thrust);
            force.add(engineForce);
            engineForce.reOrigin(vehicleOn.orientation);
            if (definition.engine.allowThrustVector) {
                torque.add(localOffset.crossProduct(engineForce));
            } else {
                torque.y -= engineForce.z * localOffset.x + engineForce.x * localOffset.z;
                torque.z += engineForce.y * localOffset.x - engineForce.x * localOffset.y;
            }
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
        data.setDouble("hours", hours);
        return data;
    }
}
