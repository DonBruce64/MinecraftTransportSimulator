package minecrafttransportsimulator.blocks.tileentities.components;

/**
 * Interface that allows an object to charge other entities with energy.
 * No energy should be stored in the thing implementing this interface.  But it should
 * always be able to accept energy if it says it is connected and the connected
 * entity is not full.
 *
 * @author don_bruce
 */
public interface ITileEntityEnergyCharger {
    /**
     * Returns the amount of energy this interface can charge this tick.
     */
    int getChargeAmount();

    /**
     * Charges the specified energy amount.  Should be less than or equal to the last call to {@link #getChargeAmount()}.
     * Only call this on the server.  Clients will sync via packets.
     */
    void chargeEnergy(int amount);
}
