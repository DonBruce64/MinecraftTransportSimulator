package minecrafttransportsimulator.entities.instances;

import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketFluidTankChange;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Basic fluid tanks class.  Class contains methods for filling and draining, as well as automatic
 * syncing of fluid levels across clients and servers.  This allows the tank to be put on any object
 * without the need to worry about packets getting out of whack.
 *
 * @author don_bruce
 */
public class EntityFluidTank extends AEntityA_Base {
    public static final String WILDCARD_FLUID_MOD = "wildcard";

    private final int maxLevel;
    private String currentFluid;
    private String currentFluidMod;
    private double fluidLevel;

    public EntityFluidTank(AWrapperWorld world, IWrapperNBT data, int maxLevel) {
        super(world, data);
        this.maxLevel = maxLevel;
        if (data != null) {
            this.currentFluid = data.getString("currentFluid");
            this.currentFluidMod = data.getString("currentFluidMod");
            this.fluidLevel = data.getDouble("fluidLevel");
        } else {
            this.currentFluid = "";
        }
        //For older versions of saved data, ensure this isn't null.
        if (currentFluidMod == null) {
            this.currentFluidMod = WILDCARD_FLUID_MOD;
        }
    }

    @Override
    public EntityAutoUpdateTime getUpdateTime() {
        //Tanks don't need to tick.
        return EntityAutoUpdateTime.NEVER;
    }

    @Override
    public double getMass() {
        return fluidLevel / 50D;
    }

    /**
     * Gets the current fluid level.
     */
    public double getFluidLevel() {
        return fluidLevel;
    }

    /**
     * Gets the max fluid level.
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Gets the name of the fluid in the tank.
     * If no fluid is in the tank, an empty string should be returned.
     */
    public String getFluid() {
        return currentFluid;
    }

    /**
     * Gets the name of the mod that made the fluid in the tank.
     * If no fluid is in the tank, or if the fluid mod is undefined, then an empty string is returned.
     */
    public String getFluidMod() {
        return currentFluidMod;
    }

    /**
     * Manually sets the fluid and level of this tank.  Used for initial filling of the tank when
     * you don't want to sent packets or perform any validity checks.  Do NOT use for normal operations!
     */
    public void manuallySet(String fluidName, String fluidModName, double setLevel) {
        this.currentFluid = fluidName;
        this.currentFluidMod = fluidModName;
        this.fluidLevel = setLevel;
    }

    /**
     * Tries to fill fluid in the tank, returning the amount
     * filled, up to the passed-in maxAmount.  If doFill is false,
     * only the possible amount filled should be returned, and the
     * internal state should be left as-is.  Return value is the
     * amount filled.
     */
    public double fill(String fluid, String fluidMod, double maxAmount, boolean doFill) {
        if (currentFluid.isEmpty() || (currentFluid.equals(fluid) && (currentFluidMod.equals(fluidMod) || fluidMod.equals(WILDCARD_FLUID_MOD)))) {
            if (maxAmount >= getMaxLevel() - fluidLevel) {
                maxAmount = getMaxLevel() - fluidLevel;
            }
            if (doFill) {
                fluidLevel += maxAmount;
                if (currentFluid.isEmpty()) {
                    currentFluid = fluid;
                    currentFluidMod = fluidMod;
                }
                //Send off packet now that we know what fluid we will have on this tank.
                if (!world.isClient()) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketFluidTankChange(this, maxAmount));
                }
            }
            return maxAmount;
        } else {
            return 0;
        }
    }

    /**
     * Tries to drain the fluid from the tank, returning the amount
     * drained, up to the passed-in maxAmount.  If doDrain is false,
     * only the possible amount drained should be returned, and the
     * internal state should be left as-is.  Return value is the
     * amount drained.
     */
    public double drain(String fluid, String fluidMod, double maxAmount, boolean doDrain) {
        if (!currentFluid.isEmpty() && ((currentFluid.equals(fluid) && (currentFluidMod.equals(fluidMod) || fluidMod.equals(WILDCARD_FLUID_MOD))) || fluid.isEmpty())) {
            if (maxAmount >= fluidLevel) {
                maxAmount = fluidLevel;
            }
            if (doDrain) {
                //Need to send off packet before we remove fluid due to empty tank.
                if (!world.isClient()) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketFluidTankChange(this, -maxAmount));
                }
                fluidLevel -= maxAmount;
                if (fluidLevel == 0) {
                    currentFluid = "";
                    currentFluidMod = "";
                }
            }
            return maxAmount;
        } else {
            return 0;
        }
    }
    
    /**
     * Like {@link #drain(String, String, double, boolean)}, but doesn't check if the fluid matches.
     */
    public double drain(double maxAmount, boolean doDrain) {
        return drain(currentFluid, currentFluidMod, maxAmount, doDrain);
    }

    /**
     * Clears the tank of all fluid in it.  Sends no packets, so make sure to call this on both server and client.
     */
    public void clear() {
        currentFluid = "";
        currentFluidMod = "";
    }

    /**
     * Gets the explosive power of this fluid.  Used when this tank is blown up.
     * In general, 10000 units is one level of explosion.  Explosion is multiplied
     * by the fuel potency, so water won't blow up, but high-octane avgas will do nicely.
     */
    public double getExplosiveness() {
        for (Map<String, Double> fuelEntry : ConfigSystem.settings.fuel.fuels.values()) {
            if (fuelEntry.containsKey(currentFluid)) {
                return fluidLevel * fuelEntry.get(currentFluid) / 10000D;
            }
        }
        return 0;
    }

    /**
     * Saves tank data to the passed-in NBT.
     */
    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setString("currentFluid", currentFluid);
        data.setString("currentFluidMod", currentFluidMod);
        data.setDouble("fluidLevel", fluidLevel);
        return data;
    }
}
