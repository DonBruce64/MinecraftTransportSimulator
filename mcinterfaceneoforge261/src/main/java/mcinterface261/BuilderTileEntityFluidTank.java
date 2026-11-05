package mcinterface261;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Builder for tile entities that contain fluids.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityFluidTank extends BuilderTileEntity implements ResourceHandler<FluidResource> {
    protected static DeferredHolder<BlockEntityType<?>, BlockEntityType<BuilderTileEntityFluidTank>> TE_TYPE2;

    private EntityFluidTank tank;

    public BuilderTileEntityFluidTank(BlockPos pos, BlockState state) {
        super(TE_TYPE2.get(), pos, state);
    }

    @Override
    protected void setTileEntity(ATileEntityBase<?> tile) {
        super.setTileEntity(tile);
        this.tank = ((ITileEntityFluidTankProvider) tile).getTank();
    }

    @Override
    public void tick() {
        super.tick();
        if (tank != null) {
            if (tileEntity instanceof TileEntityFluidLoader && ((TileEntityFluidLoader) tileEntity).isUnloader()) {
                FluidResource resource = getResource(0);
                int available = getAmountAsInt(0);
                if (available > 0 && !resource.isEmpty()) {
                    //Pump out fluid to handler below, if we have one.
                    ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, getBlockPos().below(), Direction.UP);
                    if (fluidHandler != null) {
                        try (Transaction transaction = Transaction.openRoot()) {
                            int inserted = fluidHandler.insert(resource, available, transaction);
                            if (inserted > 0) {
                                Identifier fluidLocation = BuiltInRegistries.FLUID.getKey(resource.getFluid());
                                tank.drain(fluidLocation.getPath(), fluidLocation.getNamespace(), inserted, true);
                                transaction.commit();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        if (tank != null && !tank.getFluid().isEmpty()) {
            for (Identifier fluidKey : BuiltInRegistries.FLUID.keySet()) {
                if (fluidKey.getPath().equals(tank.getFluid())) {
                    net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidKey);
                    if (fluid != null) return FluidResource.of(fluid);
                }
            }
        }
        return FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        return tank != null ? (long) tank.getFluidLevel() : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return tank != null ? (long) tank.getMaxLevel() : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return true;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (tank == null || index != 0 || resource.isEmpty()) return 0;
        Identifier fluidLocation = BuiltInRegistries.FLUID.getKey(resource.getFluid());
        if (fluidLocation == null) return 0;
        return (int) tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), amount, true);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (tank == null || index != 0 || resource.isEmpty()) return 0;
        Identifier fluidLocation = BuiltInRegistries.FLUID.getKey(resource.getFluid());
        if (fluidLocation == null) return 0;
        return (int) tank.drain(fluidLocation.getPath(), fluidLocation.getNamespace(), amount, true);
    }

}
