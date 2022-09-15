package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityLoader;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.jsondefs.JSONDecor.DecorComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

public class TileEntityItemLoader extends ATileEntityLoader implements ITileEntityInventoryProvider {
    private final EntityInventoryContainer inventory;
    private static final int LOADING_RATE = 10;

    public TileEntityItemLoader(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);
        this.inventory = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), (int) (definition.decor.inventoryUnits * 9));
        world.addEntity(inventory);
    }

    @Override
    public void update() {
        super.update();
        if (isUnloader()) {
            //Push stack to inventory below to ready for unload.
            //Need to advance stack-grabbing by 1 tick from rate to ensure that slot is free during next unloading cycle.
            if ((ticksExisted + 1) % LOADING_RATE == 0) {
                for (int i = 0; i < inventory.getSize(); ++i) {
                    IWrapperItemStack stack = inventory.getStack(i);
                    if (!stack.isEmpty()) {
                        if (world.insertStack(position, Axis.DOWN, stack)) {
                            inventory.setStack(stack, i);
                            break;
                        }
                    }
                }

            }
        } else {
            //Pull stack from inventory above to ready for load.
            //Need to retard stack-grabbing by 1 tick from rate to ensure that it's ready during next loading cycle.
            if ((ticksExisted - 1) % LOADING_RATE == 0) {
                for (int i = 0; i < inventory.getSize(); ++i) {
                    IWrapperItemStack stack = inventory.getStack(i);
                    if (stack.isEmpty()) {
                        IWrapperItemStack extractedStack = world.extractStack(position, Axis.UP);
                        if (extractedStack != null) {
                            stack = extractedStack;
                            inventory.setStack(stack, i);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isUnloader() {
        return definition.decor.type.equals(DecorComponentType.ITEM_UNLOADER);
    }

    @Override
    protected boolean canOperate() {
        return isUnloader() ? inventory.getCount() < inventory.getSize() : inventory.getCount() > 0;
    }

    @Override
    protected boolean canLoadPart(PartInteractable part) {
        if (part.inventory != null) {
            if (isUnloader()) {
                //We can always unload.
                return true;
            } else {
                for (int i = 0; i < inventory.getSize(); ++i) {
                    IWrapperItemStack stack = inventory.getStack(i);
                    if (!stack.isEmpty() && part.inventory.addStack(stack, 1, false)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void doLoading() {
        if (ticksExisted % LOADING_RATE == 0) {
            boolean hadStacksToAddThisCheck = false;
            boolean addedStacksThisCheck = false;
            for (int i = 0; i < inventory.getSize(); ++i) {
                IWrapperItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty() && !addedStacksThisCheck) {
                    hadStacksToAddThisCheck = true;
                    if (connectedPart.inventory.addStack(stack, 1, true)) {
                        inventory.setStack(stack, i);
                        addedStacksThisCheck = true;
                        break;
                    }
                }
            }
            if (hadStacksToAddThisCheck && !addedStacksThisCheck) {
                //Try to add to another part.
                updateNearestPart();
            }
        }
    }

    @Override
    protected void doUnloading() {
        if (ticksExisted % LOADING_RATE == 0) {
            boolean hadStacksToRemoveThisCheck = false;
            for (int i = 0; i < connectedPart.inventory.getSize(); ++i) {
                IWrapperItemStack stack = connectedPart.inventory.getStack(i);
                if (!stack.isEmpty()) {
                    hadStacksToRemoveThisCheck = true;
                    if (inventory.addStack(stack, 1, true)) {
                        connectedPart.inventory.setStack(stack, i);
                        break;
                    }
                }
            }
            if (!hadStacksToRemoveThisCheck) {
                //Part is empty, go grab another.
                updateNearestPart();
            }
        }
    }

    @Override
    public EntityInventoryContainer getInventory() {
        return inventory;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setData("inventory", inventory.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        return data;
    }
}
