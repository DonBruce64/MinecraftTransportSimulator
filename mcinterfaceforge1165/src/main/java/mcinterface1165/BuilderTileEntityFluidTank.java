package mcinterface1165;

import javax.annotation.Nonnull;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Builder for tile entities that contain fluids.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityFluidTank extends BuilderTileEntity implements IFluidTank, IFluidHandler {
    protected static TileEntityType<BuilderTileEntityFluidTank> TE_TYPE2;

    private EntityFluidTank tank;

    public BuilderTileEntityFluidTank() {
        super(TE_TYPE2);
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
                if (currentFluidAmount > 0) {
                    //Pump out fluid to handler below, if we have one.
                    TileEntity teBelow = level.getBlockEntity(getBlockPos().below());
                    IFluidHandler fluidHandler = teBelow.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.UP).orElse(null);
                    if (teBelow != null && fluidHandler != null) {
                        int amountDrained = fluidHandler.fill(getFluid(), FluidAction.EXECUTE);
                        if (amountDrained > 0 && currentFluidAmount == getFluidAmount()) {
                            //Need to drain from our tank as the system didn't do this.
                            drain(amountDrained, FluidAction.EXECUTE);
                        }
                    }
                }
            }
        }
    }

    @Override
    public FluidStack getFluid() {
        return tank != null && !tank.getFluid().isEmpty() ? new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(tank.getFluid())), (int) tank.getFluidLevel()) : null;
    }

    @Override
    public int getFluidAmount() {
        return (int) (tank != null ? tank.getFluidLevel() : 0);
    }

    @Override
    public int getCapacity() {
        return tank != null ? tank.getMaxLevel() : 0;
    }

    @Override
    public boolean isFluidValid(FluidStack fluid) {
        return true;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack stack, FluidAction doFill) {
        if (tank != null) {
            return (int) tank.fill(stack.getFluid().getRegistryName().getPath(), stack.getAmount(), doFill == FluidAction.EXECUTE);
        } else {
            return 0;
        }
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction doDrain) {
        if (getFluidAmount() > 0) {
            return this.drain(new FluidStack(getFluid().getFluid(), maxDrain), doDrain);
        }
        return null;
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction doDrain) {
        return new FluidStack(stack.getFluid(), (int) (tank != null ? tank.drain(stack.getFluid().getRegistryName().getPath(), stack.getAmount(), doDrain == FluidAction.EXECUTE) : 0));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && facing == Direction.DOWN) {
            return LazyOptional.of(() -> (T) this);
        } else {
            return super.getCapability(capability, facing);
        }
    }
}
