package mcinterface262;

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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
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
                int currentFluidAmount = getFluidAmount();
                FluidResource resource = getResource(0);
                if (currentFluidAmount > 0 && !resource.isEmpty()) {
                    //Pump out fluid to handler below, if we have one.
                    ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, getBlockPos().below(), Direction.UP);
                    if (fluidHandler != null) {
                        int amountDrained = fluidHandler.insert(resource, currentFluidAmount, null);
                        if (amountDrained > 0 && currentFluidAmount == getFluidAmount()) {
                            //Need to drain from our tank as the system didn't do this.
                            tank.drain(amountDrained, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the fluid currently in this tank as a stack, for internal use.
     */
    public FluidStack getFluid() {
        if (tank != null && !tank.getFluid().isEmpty()) {
            //Need to find the mod that registered this fluid, Forge is stupid and has them per-mod vs just all with a single name.
            for (Identifier fluidKey : BuiltInRegistries.FLUID.keySet()) {
                if (fluidKey.getPath().equals(tank.getFluid())) {
                    return new FluidStack(BuiltInRegistries.FLUID.getValue(fluidKey), (int) tank.getFluidLevel());
                }
            }
        }
        return FluidStack.EMPTY;
    }

    public int getFluidAmount() {
        return (int) (tank != null ? tank.getFluidLevel() : 0);
    }

    public int getCapacity() {
        return tank != null ? tank.getMaxLevel() : 0;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        FluidStack stack = getFluid();
        return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        return getFluidAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return getCapacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return true;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (tank != null && !resource.isEmpty()) {
            Identifier fluidLocation = BuiltInRegistries.FLUID.getKey(resource.getFluid());
            return (int) tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), amount, true);
        } else {
            return 0;
        }
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (tank != null && !resource.isEmpty()) {
            Identifier fluidLocation = BuiltInRegistries.FLUID.getKey(resource.getFluid());
            return (int) tank.drain(fluidLocation.getPath(), fluidLocation.getNamespace(), amount, true);
        } else {
            return 0;
        }
    }
}
