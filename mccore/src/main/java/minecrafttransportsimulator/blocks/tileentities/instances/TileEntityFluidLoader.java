package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityLoader;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor.DecorComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

public class TileEntityFluidLoader extends ATileEntityLoader implements ITileEntityFluidTankProvider {
    private final EntityFluidTank tank;
    private final ComputedVariable loadingActiveVar;
    private final ComputedVariable unloadingActiveVar;

    public TileEntityFluidLoader(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
        this.tank = new EntityFluidTank(world, data != null ? data.getData("tank") : null, definition.decor.fuelCapacity);
        world.addEntity(tank);
        addVariable(loadingActiveVar = new ComputedVariable(this, "tank_loading_active"));
        addVariable(unloadingActiveVar = new ComputedVariable(this, "tank_unloading_active"));
    }

    @Override
    public void remove() {
        super.remove();
        tank.remove();
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
            case ("tank_buffer_active"):
                return new ComputedVariable(this, variable, partialTicks -> tank.getFluidLevel() > 0 ? 1 : 0, false);
            default:
                return super.createComputedVariable(variable, createDefaultIfNotPresent);
        }
    }

    @Override
    public boolean isUnloader() {
        return definition.decor.type.equals(DecorComponentType.FLUID_UNLOADER);
    }

    @Override
    protected boolean canOperate() {
        return isUnloader() ? tank.getFluidLevel() < tank.getMaxLevel() : tank.getFluidLevel() > 0;
    }

    @Override
    protected boolean canLoadPart(PartInteractable part) {
        if (part.tank != null) {
            return isUnloader() ? part.tank.drain(tank.getFluid(), tank.getFluidMod(), 1, false) > 0 : part.tank.fill(tank.getFluid(), tank.getFluidMod(), 1, false) > 0;
        } else {
            return false;
        }
    }

    @Override
    protected void doLoading() {
        String fluidToLoad = tank.getFluid();
        String fluidModToLoad = tank.getFluidMod();
        double amountToLoad = connectedPart.tank.fill(fluidToLoad, fluidModToLoad, definition.decor.pumpRate, false);
        if (amountToLoad > 0) {
            amountToLoad = tank.drain(fluidToLoad, fluidModToLoad, amountToLoad, true);
            connectedPart.tank.fill(fluidToLoad, fluidModToLoad, amountToLoad, true);
            loadingActiveVar.setActive(amountToLoad > 0, true);
        } else {
            updateNearestPart();
            loadingActiveVar.setActive(false, true);
        }
    }

    @Override
    protected void doUnloading() {
        String fluidToUnload = connectedPart.tank.getFluid();
        String fluidModToUnload = connectedPart.tank.getFluidMod();
        double amountToUnload = connectedPart.tank.drain(fluidToUnload, fluidModToUnload, definition.decor.pumpRate, false);
        if (amountToUnload > 0) {
            amountToUnload = tank.fill(fluidToUnload, fluidModToUnload, amountToUnload, true);
            connectedPart.tank.drain(fluidToUnload, fluidModToUnload, amountToUnload, true);
            unloadingActiveVar.setActive(amountToUnload > 0, true);
        } else {
            updateNearestPart();
            unloadingActiveVar.setActive(false, true);
        }
    }

    @Override
    public EntityFluidTank getTank() {
        return tank;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setData("tank", tank.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        return data;
    }
}
