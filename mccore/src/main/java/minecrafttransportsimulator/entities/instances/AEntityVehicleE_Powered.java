package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.EngineType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * This class adds engine components for vehicles, such as fuel, throttle,
 * and electricity.  Contains numerous methods for gauges, HUDs, and fuel systems.
 * This is added on-top of the D level to keep the crazy movement calculations
 * separate from the vehicle power overhead bits.  This is the first level of
 * class that can be used for references in systems as it's the last common class for
 * vehicles.  All other sub-levels are simply functional building-blocks to keep this
 * class from having 1000+ lines of code and to better segment things out.
 *
 * @author don_bruce
 */
public abstract class AEntityVehicleE_Powered extends AEntityVehicleD_Moving {
    //Variables
	public final ComputedVariable runningLightVar;
	public final ComputedVariable headLightVar;
	public final ComputedVariable navigationLightVar;
	public final ComputedVariable strobeLightVar;
	public final ComputedVariable taxiLightVar;
	public final ComputedVariable landingLightVar;
	public final ComputedVariable hornVar;
	public final ComputedVariable retractGearVar;
	public final ComputedVariable throttleVar;
	public final ComputedVariable reverseThrustVar;
    public static final double MAX_THROTTLE = 1.0D;

    //Internal states.
    public boolean beingFueled;
    public boolean enginesOn;
    public boolean enginesStarting;
    public boolean enginesRunning;
    public boolean isCreative;
    public boolean hasReverseThrust;
    public int gearMovementTime;
    public int ticksOutOfHealth;
    public double electricPower;
    public double electricUsage;
    public double electricFlow;
    public String selectedBeaconName;
    public NavBeacon selectedBeacon;
    public final EntityFluidTank fuelTank;

    //Engines.
    public final List<PartEngine> engines = new ArrayList<>();

    //Map containing incoming missiles and radar info, sorted by distance.
    public final List<EntityBullet> missilesIncoming = new ArrayList<>();
    public final List<AEntityD_Definable<?>> radarsTracking = new ArrayList<>();

    public AEntityVehicleE_Powered(AWrapperWorld world, IWrapperPlayer placingPlayer, ItemVehicle item, IWrapperNBT data) {
        super(world, placingPlayer, item, data);

        if (data != null) {
            //Load simple variables.
            this.electricPower = data.getDouble("electricPower");
            this.selectedBeaconName = data.getString("selectedBeaconName");
            this.selectedBeacon = NavBeacon.getByNameFromWorld(world, selectedBeaconName);
            this.fuelTank = new EntityFluidTank(world, data.getData("fuelTank"), definition.motorized.fuelCapacity);
        } else {
            this.electricPower = 12;
            this.selectedBeaconName = "";
            this.fuelTank = new EntityFluidTank(world, null, definition.motorized.fuelCapacity);
        }
        world.addEntity(fuelTank);
        
        addVariable(this.runningLightVar = new ComputedVariable(this, "running_light", data));
        addVariable(this.headLightVar = new ComputedVariable(this, "headlight", data));
    	addVariable(this.navigationLightVar = new ComputedVariable(this, "navigation_light", data));
    	addVariable(this.strobeLightVar = new ComputedVariable(this, "strobe_light", data));
    	addVariable(this.taxiLightVar = new ComputedVariable(this, "taxi_light", data));
    	addVariable(this.landingLightVar = new ComputedVariable(this, "landing_light", data));
    	addVariable(this.hornVar = new ComputedVariable(this, "horn", data));
    	addVariable(this.retractGearVar = new ComputedVariable(this, "gear_setpoint", data));
    	addVariable(this.throttleVar = new ComputedVariable(this, "throttle", data));
    	addVariable(this.reverseThrustVar = new ComputedVariable(this, "reverser", data));
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("VehicleE_Level", true);

        //If we have space for fuel, and we have tanks with it, transfer it.
        if (!world.isClient() && fuelTank.getFluidLevel() < definition.motorized.fuelCapacity - 100) {
            for (APart part : allParts) {
                if (part instanceof PartInteractable && part.isActive && part.definition.interactable.feedsVehicles) {
                    EntityFluidTank tank = ((PartInteractable) part).tank;
                    if (tank != null) {
                        double amountFilled = tank.drain(fuelTank.getFluid(), 1, true);
                        if (amountFilled > 0) {
                            fuelTank.fill(fuelTank.getFluid(), amountFilled, true);
                        }
                    }
                }
            }
        }

        //Check to make sure the selected beacon is still correct.
        //It might not be valid if it has been removed from the world,
        //or one might have been placed that matches our selection.
        if (definition.motorized.isAircraft && ticksExisted % 20 == 0) {
            if (!selectedBeaconName.isEmpty()) {
                selectedBeacon = NavBeacon.getByNameFromWorld(world, selectedBeaconName);
            } else {
                selectedBeacon = null;
            }
        }

        //Do trailer-specific logic, if we are one and towed.
        //Otherwise, do normal update logic for DRLs.
        if (definition.motorized.isTrailer) {
            //If we are being towed set the brake state to the same as the towing vehicle.
            //If we aren't being towed, set the parking brake.
            if (towedByConnection != null) {
                if (parkingBrakeVar.isActive) {
                	parkingBrakeVar.setTo(0, false);
                }
                brakeVar.setTo(towedByConnection.towingVehicle.brakeVar.currentValue, false);
            } else {
                if (!parkingBrakeVar.isActive) {
                    parkingBrakeVar.setTo(1, false);
                }
                if (brakeVar.isActive) {
                	brakeVar.setTo(0, false);
                }
            }
        } else {
            //Set engine state mapping variables.
            enginesOn = false;
            enginesStarting = false;
            enginesRunning = false;
            for (PartEngine engine : engines) {
                if (engine.magnetoVar.isActive) {
                    enginesOn = true;
                    if (engine.electricStarterVar.isActive) {
                        enginesStarting = true;
                    }
                    if (engine.running) {
                        enginesRunning = true;
                        break;
                    }
                }
            }
        }

        //If we are a trailer, get the towing vehicle's electric power.
        //If we are too damaged, don't hold any charge.
        if (definition.motorized.isTrailer) {
            if (towedByConnection != null) {
                electricPower = towedByConnection.towingVehicle.electricPower;
            }
        } else if (!outOfHealth) {
            electricPower = Math.max(0, Math.min(13, electricPower -= electricUsage));
            electricFlow = electricUsage;
            electricUsage = 0;
        } else {
            electricPower = 0;
            electricFlow = 0;
            electricUsage = 0;
        }

        //Adjust gear variables.
        if (retractGearVar.isActive) {
            if (gearMovementTime < definition.motorized.gearSequenceDuration) {
                ++gearMovementTime;
            }
        } else {
            if (gearMovementTime > 0) {
                --gearMovementTime;
            }
        }

        //Update missile list to sort by distance.
        missilesIncoming.sort((missle1, missile2) -> missle1.targetDistance < missile2.targetDistance ? -1 : 1);

        //Check to make sure we are still being tracked.
        radarsTracking.removeIf(tracker -> !tracker.isValid || (!tracker.aircraftOnRadar.contains(this) && !tracker.groundersOnRadar.contains(this)));

        //If we are supposed to de-spawn, do so.
        if (outOfHealth && ConfigSystem.settings.general.vehicleDeathDespawnTime.value > 0) {
            if (++ticksOutOfHealth > ConfigSystem.settings.general.vehicleDeathDespawnTime.value * 20) {
                remove();
            }
        }
        world.endProfiling();
    }

    @Override
    public void destroy(BoundingBox box) {
        //Spawn instruments in the world.
        for (ItemInstrument instrument : instruments) {
            if (instrument != null) {
                world.spawnItemStack(instrument.getNewStack(null), box.globalCenter, null);
            }
        }

        //Oh, and add explosions.  Because those are always fun.
        //Note that this is done after spawning all parts here and in the super call,
        //so although all parts are DROPPED, not all parts may actually survive the explosion.
        if (ConfigSystem.settings.damage.vehicleExplosions.value) {
            double explosivePower = 0;
            for (APart part : allParts) {
                if (part instanceof PartInteractable) {
                    explosivePower += ((PartInteractable) part).getExplosiveContribution();
                }
            }
            world.spawnExplosion(box.globalCenter, explosivePower + fuelTank.getExplosiveness() + 1D, true);
        }

        //Now call super, since super might modify parts.
        super.destroy(box);
    }

    @Override
    public double getMass() {
        return super.getMass() + fuelTank.getMass();
    }

    @Override
    public void addPartsPostAddition(IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super.addPartsPostAddition(placingPlayer, data);
        //If we have a default fuel, add it now as we SHOULD have an engine to tell
        //us what fuel type we will need to add.
        if (data == null && definition.motorized.defaultFuelQty > 0) {
            for (APart part : allParts) {
                if (part instanceof PartEngine) {
                    String mostPotentFluid = "";
                    //If the engine is electric, just use the electric fuel type.
                    if (part.definition.engine.type == EngineType.ELECTRIC) {
                        mostPotentFluid = PartEngine.ELECTRICITY_FUEL;
                    } else {
                        //Get the most potent fuel for the vehicle from the fuel configs.
                        for (String fluidName : ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).keySet()) {
                            if (InterfaceManager.coreInterface.isFluidValid(fluidName)) {
                                if (mostPotentFluid.isEmpty() || ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).get(mostPotentFluid) < ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).get(fluidName)) {
                                    mostPotentFluid = fluidName;
                                }
                            }
                        }
                    }
                    fuelTank.manuallySet(mostPotentFluid, definition.motorized.defaultFuelQty);
                    break;
                }
            }
            if (fuelTank.getFluid().isEmpty() && placingPlayer != null) {
                placingPlayer.sendPacket(new PacketPlayerChatMessage(placingPlayer, LanguageSystem.SYSTEM_DEBUG, "A defaultFuelQty was specified for: " + definition.packID + ":" + definition.systemName + ", but no engine was noted as a defaultPart, so we don't know what fuel to put in the vehicle.  Vehicle will be spawned without fuel and engine."));
            }
        }
    }

    @Override
    public void updatePartList() {
        super.updatePartList();

        //Add engines to the list of engines.
        engines.clear();
        allParts.forEach(part -> {
            if (part instanceof PartEngine) {
                engines.add((PartEngine) part);
            }
        });

        //Set reverse thrust.
        hasReverseThrust = false;
        if (definition.motorized.isBlimp) {
            hasReverseThrust = true;
        } else {
            for (APart part : allParts) {
                if (part instanceof PartPropeller) {
                    if (part.definition.propeller.isDynamicPitch) {
                        hasReverseThrust = true;
                        break;
                    }
                } else if (part instanceof PartEngine && part.definition.engine.jetPowerFactor > 0) {
                    hasReverseThrust = true;
                    break;
                }
            }
        }
    }

    public FuelTankResult checkFuelTankCompatibility(String fluid) {
        //Check tank first to make sure there's not a mis-match.
        if (!fuelTank.getFluid().isEmpty()) {
            if (!fluid.equals(fuelTank.getFluid())) {
                return FuelTankResult.MISMATCH;
            }
        }

        //Fuel type can be taken by vehicle, check to make sure engines can take it.
        boolean foundEngine = false;
        for (APart part : allParts) {
            if (part instanceof PartEngine) {
                foundEngine = true;
                if (ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).containsKey(fluid)) {
                    return FuelTankResult.VALID;
                }
            }
        }
        return foundEngine ? FuelTankResult.INVALID : FuelTankResult.NOENGINE;
    }

    public boolean canPlayerStartEngines(IWrapperPlayer player) {
        if (!ConfigSystem.settings.general.keyRequiredToStartVehicles.value) {
            return true;
        } else {
            if (player.isHoldingItemType(ItemComponentType.KEY)) {
                IWrapperNBT data = player.getHeldStack().getData();
                if (data != null) {
                    UUID itemKeyUUID = data.getUUID(ItemItem.KEY_UUID_TAG);
                    if (itemKeyUUID != null && itemKeyUUID.equals(keyUUID)) {
                        return true;
                    }
                }
            }
            if (world.isClient()) {
                player.displayChatMessage(LanguageSystem.INTERACT_VEHICLE_NEEDKEY);
            }
            return false;
        }
    }

    //-----START OF SOUND AND ANIMATION CODE-----
    @Override
    public boolean hasRadio() {
        return true;
    }

    @Override
    public boolean renderTextLit() {
        if (super.renderTextLit() && electricPower > 3) {
            return getOrCreateVariable(definition.motorized.litVariable).isActive;
        } else {
            return false;
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setDouble("electricPower", electricPower);
        data.setString("selectedBeaconName", selectedBeaconName);
        data.setData("fuelTank", fuelTank.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        return data;
    }

    public enum FuelTankResult {
        NOENGINE,
        VALID,
        INVALID,
        MISMATCH;
    }
}
