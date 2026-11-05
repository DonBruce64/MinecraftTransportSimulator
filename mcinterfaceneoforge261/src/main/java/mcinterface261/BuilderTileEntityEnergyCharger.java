package mcinterface261;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityEnergyCharger;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Builder for tile entities that transform MC energy into power for other entities.
 *
 * @author don_bruce
 */
public class BuilderTileEntityEnergyCharger extends BuilderTileEntity implements EnergyHandler {
    protected static DeferredHolder<BlockEntityType<?>, BlockEntityType<BuilderTileEntityEnergyCharger>> TE_TYPE2;

    private ITileEntityEnergyCharger charger;
    private static final int MAX_BUFFER = 1000;
    private int buffer;

    public BuilderTileEntityEnergyCharger(BlockPos pos, BlockState state) {
        super(TE_TYPE2.get(), pos, state);
    }

    @Override
    protected void setTileEntity(ATileEntityBase<?> tile) {
        super.setTileEntity(tile);
        this.charger = (ITileEntityEnergyCharger) tile;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide() && charger != null) {
            //Try and charge the internal TE.
            if (buffer > 0) {
                double amountToCharge = charger.getChargeAmount();
                if (amountToCharge != 0) {
                    int amountToRemoveFromBuffer = (int) (amountToCharge / ConfigSystem.settings.general.rfToElectricityFactor.value);
                    if (amountToRemoveFromBuffer > buffer) {
                        amountToRemoveFromBuffer = buffer;
                        amountToCharge = amountToRemoveFromBuffer * ConfigSystem.settings.general.rfToElectricityFactor.value;
                    }
                    charger.chargeEnergy(amountToCharge);
                    buffer -= amountToRemoveFromBuffer;
                }
            }
        }
    }

    @Override
    public long getAmountAsLong() {
        return buffer;
    }

    @Override
    public long getCapacityAsLong() {
        return MAX_BUFFER;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        int amountToStore = MAX_BUFFER - buffer;
        if (amountToStore > amount) amountToStore = amount;
        buffer += amountToStore;
        return amountToStore;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }

}
