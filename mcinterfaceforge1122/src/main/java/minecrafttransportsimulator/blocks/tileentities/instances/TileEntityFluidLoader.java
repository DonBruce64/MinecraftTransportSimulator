package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityLoader;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.jsondefs.JSONDecor.DecorComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

public class TileEntityFluidLoader extends ATileEntityLoader implements ITileEntityFluidTankProvider {
    private final EntityFluidTank tank;

    public TileEntityFluidLoader(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);
        this.tank = new EntityFluidTank(world, data.getDataOrNew("tank"), definition.decor.fuelCapacity);
        world.addEntity(tank);
    }

    @Override
    public void connectToPart(PartInteractable part) {
        super.connectToPart(part);
        if (part != null) {
            tank.resetAmountDispensed();
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
            return isUnloader() ? part.tank.drain(tank.getFluid(), 1, false) > 0 : part.tank.fill(tank.getFluid(), 1, false) > 0;
        } else {
            return false;
        }
    }

    @Override
    protected void doLoading() {
        String fluidToLoad = tank.getFluid();
        double amountToLoad = connectedPart.tank.fill(fluidToLoad, definition.decor.pumpRate, false);
        if (amountToLoad > 0) {
            amountToLoad = tank.drain(fluidToLoad, amountToLoad, true);
            connectedPart.tank.fill(fluidToLoad, amountToLoad, true);
        } else {
            updateNearestPart();
        }
    }

    @Override
    protected void doUnloading() {
        String fluidToUnload = connectedPart.tank.getFluid();
        double amountToUnload = connectedPart.tank.drain(fluidToUnload, definition.decor.pumpRate, false);
        if (amountToUnload > 0) {
            amountToUnload = tank.fill(fluidToUnload, amountToUnload, true);
            connectedPart.tank.drain(fluidToUnload, amountToUnload, true);
        } else {
            updateNearestPart();
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
