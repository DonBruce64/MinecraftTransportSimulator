package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.ConfigSystem;

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
abstract class AEntityVehicleE_Powered extends AEntityVehicleD_Moving {
    //Static variables used in logic that are kept in the global map.
    public static final String RUNNINGLIGHT_VARIABLE = "running_light";
    public static final String HEADLIGHT_VARIABLE = "headlight";
    public static final String NAVIGATIONLIGHT_VARIABLE = "navigation_light";
    public static final String STROBELIGHT_VARIABLE = "strobe_light";
    public static final String TAXILIGHT_VARIABLE = "taxi_light";
    public static final String LANDINGLIGHT_VARIABLE = "landing_light";
    public static final String HORN_VARIABLE = "horn";
    public static final String GEAR_VARIABLE = "gear_setpoint";
    public static final String THROTTLE_VARIABLE = "throttle";
    public static final String REVERSE_THRUST_VARIABLE = "reverser";

    //External state control.
    @DerivedValue
    public boolean reverseThrust;
    public boolean beingFueled;
    public boolean enginesOn;
    public boolean enginesRunning;
    public boolean isCreative;
    @DerivedValue
    public double throttle;
    public static final double MAX_THROTTLE = 1.0D;

    //Internal states.
    public int gearMovementTime;
    public double electricPower;
    public double electricUsage;
    public double electricFlow;
    public String selectedBeaconName;
    public NavBeacon selectedBeacon;
    public EntityFluidTank fuelTank;

    //Engines.
    public final List<PartEngine> engines = new ArrayList<>();

    //Map containing incoming missiles, sorted by distance, which is the value for this map.
    public final List<EntityBullet> missilesIncoming = new ArrayList<>();

    public AEntityVehicleE_Powered(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, data);

        //Load simple variables.
        this.electricPower = data.getDouble("electricPower");
        this.selectedBeaconName = data.getString("selectedBeaconName");
        this.selectedBeacon = NavBeacon.getByNameFromWorld(world, selectedBeaconName);
        this.fuelTank = new EntityFluidTank(world, data.getDataOrNew("fuelTank"), definition.motorized.fuelCapacity);
        world.addEntity(fuelTank);

        if (newlyCreated) {
            //Set initial electrical power.
            electricPower = 12;
        }
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("VehicleE_Level", true);
        //Get throttle and reverse state.
        throttle = getVariable(THROTTLE_VARIABLE);
        reverseThrust = isVariableActive(REVERSE_THRUST_VARIABLE);

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
                if (parkingBrakeOn) {
                    toggleVariable(PARKINGBRAKE_VARIABLE);
                }
                setVariable(BRAKE_VARIABLE, towedByConnection.towingVehicle.brake);
            } else {
                if (!parkingBrakeOn) {
                    toggleVariable(PARKINGBRAKE_VARIABLE);
                }
                if (brake != 0) {
                    setVariable(BRAKE_VARIABLE, 0);
                }
            }
        } else {
            //Set engine state mapping variables.
            enginesOn = false;
            enginesRunning = false;
            for (PartEngine engine : engines) {
                if (engine.magnetoOn) {
                    enginesOn = true;
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
        } else if (damageAmount < definition.general.health) {
            electricPower = Math.max(0, Math.min(13, electricPower -= electricUsage));
            electricFlow = electricUsage;
            electricUsage = 0;
        } else {
            electricPower = 0;
            electricFlow = 0;
            electricUsage = 0;
        }

        //Adjust gear variables.
        if (isVariableActive(EntityVehicleF_Physics.GEAR_VARIABLE)) {
            if (gearMovementTime < definition.motorized.gearSequenceDuration) {
                ++gearMovementTime;
            }
        } else {
            if (gearMovementTime > 0) {
                --gearMovementTime;
            }
        }

        //Check that missiles are still valid.
        //If they are, update their distances. Otherwise, remove them.
        missilesIncoming.removeIf(entityBullet -> !entityBullet.isValid);
        missilesIncoming.sort((missle1, missile2) -> missle1.targetDistance < missile2.targetDistance ? -1 : 1);
        world.endProfiling();
    }

    @Override
    public void destroy(BoundingBox box) {
        //Spawn instruments in the world.
        for (ItemInstrument instrument : instruments) {
            if (instrument != null) {
                world.spawnItem(instrument, null, box.globalCenter);
            }
        }

        //Oh, and add explosions.  Because those are always fun.
        //Note that this is done after spawning all parts here and in the super call,
        //so although all parts are DROPPED, not all parts may actually survive the explosion.
        if (ConfigSystem.settings.damage.explosions.value) {
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
    public void updatePartList() {
        super.updatePartList();

        //Add engines to the list of engines.
        engines.clear();
        allParts.forEach(part -> {
            if (part instanceof PartEngine) {
                engines.add((PartEngine) part);
            }
        });
    }

    public void acquireMissile(EntityBullet missile) {
        //Add this missile with its current distance
        if (!missilesIncoming.contains(missile)) {
            missilesIncoming.add(missile);
        }
    }

    public boolean canPlayerStartEngines(IWrapperPlayer player) {
        if (!ConfigSystem.settings.general.keyRequiredToStartVehicles.value) {
            return true;
        } else {
            if (player.isHoldingItemType(ItemComponentType.KEY)) {
                String uniqueUUIDString = player.getHeldStack().getData().getString("vehicle");
                if (!uniqueUUIDString.isEmpty() && uniqueUUID.equals(UUID.fromString(uniqueUUIDString))) {
                    return true;
                }
            }
            if (world.isClient()) {
                player.displayChatMessage(JSONConfigLanguage.INTERACT_VEHICLE_NEEDKEY);
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
        if (definition.motorized.hasRunningLights && isVariableActive(RUNNINGLIGHT_VARIABLE))
            return electricPower > 3 && super.renderTextLit();
        if (definition.motorized.hasHeadlights && isVariableActive(HEADLIGHT_VARIABLE))
            return electricPower > 3 && super.renderTextLit();
        if (definition.motorized.hasNavLights && isVariableActive(NAVIGATIONLIGHT_VARIABLE))
            return electricPower > 3 && super.renderTextLit();
        if (definition.motorized.hasStrobeLights && isVariableActive(STROBELIGHT_VARIABLE))
            return electricPower > 3 && super.renderTextLit();
        if (definition.motorized.hasTaxiLights && isVariableActive(TAXILIGHT_VARIABLE))
            return electricPower > 3 && super.renderTextLit();
        if (definition.motorized.hasLandingLights && isVariableActive(LANDINGLIGHT_VARIABLE))
            return electricPower > 3 && super.renderTextLit();
        return false;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setDouble("electricPower", electricPower);
        data.setString("selectedBeaconName", selectedBeaconName);
        data.setData("fuelTank", fuelTank.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        return data;
    }
}
