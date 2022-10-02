package mcinterface1122;

import javax.annotation.Nullable;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Builder for tile entities that transform MC energy into power for other entities.
 *
 * @author don_bruce
 */
public class BuilderTileEntityEnergyCharger<EnergyTileEntity extends ATileEntityBase<?> & ITileEntityEnergyCharger> extends BuilderTileEntity<EnergyTileEntity> implements IEnergyStorage {

    private static final int MAX_BUFFER = 1000;
    private int buffer;

    public BuilderTileEntityEnergyCharger() {
        super();
    }

    @Override
    public void update() {
        super.update();
        if (!world.isRemote && tileEntity != null) {
            //Try and charge the internal TE.
            if (buffer > 0) {
                int amountToCharge = tileEntity.getChargeAmount();
                if (amountToCharge != 0) {
                    if (amountToCharge > buffer) {
                        amountToCharge = buffer;
                    }
                    tileEntity.chargeEnergy(amountToCharge);
                    buffer -= amountToCharge;
                }
            }
        }
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (buffer == MAX_BUFFER) {
            return 0;
        } else {
            int amountToStore = MAX_BUFFER - buffer;
            if (amountToStore > maxReceive) {
                amountToStore = maxReceive;
            }
            if (!simulate) {
                buffer += amountToStore;
            }
            return amountToStore;
        }
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return buffer;
    }

    @Override
    public int getMaxEnergyStored() {
        return MAX_BUFFER;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && facing != null) {
            return true;
        } else {
            return super.hasCapability(capability, facing);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && facing != null) {
            return (T) this;
        } else {
            return super.getCapability(capability, facing);
        }
    }
}
