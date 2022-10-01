package mcinterface1122;

import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class WrapperItemStack implements IWrapperItemStack {
    private static final TileEntityFurnace VANILLA_FAKE_FURNACE = new TileEntityFurnace();

    protected final ItemStack stack;

    protected WrapperItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean isCompleteMatch(IWrapperItemStack other) {
        ItemStack otherStack = ((WrapperItemStack) other).stack;
        return otherStack.isItemEqual(stack) && (otherStack.hasTagCompound() ? otherStack.getTagCompound().equals(stack.getTagCompound()) : !stack.hasTagCompound());
    }

    @Override
    public int getFuelValue() {
        return TileEntityFurnace.getItemBurnTime(stack);
    }

    @Override
    public IWrapperItemStack getSmeltedItem(AWrapperWorld world) {
        return new WrapperItemStack(FurnaceRecipes.instance().getSmeltingResult(stack).copy());
    }

    @Override
    public int getSmeltingTime(AWrapperWorld world) {
        return VANILLA_FAKE_FURNACE.getCookTime(stack);
    }

    @Override
    public AItemBase getItem() {
        Item item = stack.getItem();
        return item instanceof IBuilderItemInterface ? ((IBuilderItemInterface) item).getItem() : null;
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public int getSize() {
        return stack.getCount();
    }

    @Override
    public int getMaxSize() {
        return stack.getMaxStackSize();
    }

    @Override
    public int add(int qty) {
        if (qty < 0) {
            int amountToRemove = -qty;
            if (amountToRemove > getSize()) {
                amountToRemove = getSize();
            }
            stack.setCount(stack.getCount() - amountToRemove);
            return qty + amountToRemove;
        } else {
            int amountToAdd = qty;
            if (amountToAdd + getSize() > getMaxSize()) {
                amountToAdd = getMaxSize() - getSize();
            }
            stack.setCount(stack.getCount() + amountToAdd);
            return qty - amountToAdd;
        }
    }

    @Override
    public IWrapperItemStack split(int qty) {
        return new WrapperItemStack(stack.splitStack(qty));
    }

    @Override
    public boolean interactWith(EntityFluidTank tank, IWrapperPlayer player) {
        IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
        if (handler != null) {
            if (!player.isSneaking()) {
                //Item can provide fluid.  Check if the tank can accept it.
                FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, false);
                if (drainedStack != null) {
                    //Able to take fluid from item, attempt to do so.
                    int amountToDrain = (int) tank.fill(drainedStack.getFluid().getName(), drainedStack.amount, false);
                    drainedStack = handler.drain(amountToDrain, !player.isCreative());
                    if (drainedStack != null) {
                        //Was able to provide liquid from item.  Fill the tank.
                        tank.fill(drainedStack.getFluid().getName(), drainedStack.amount, true);
                        player.setHeldStack(new WrapperItemStack(handler.getContainer()));
                    }
                }
            } else {
                //Item can hold fluid.  Check if we can fill it.
                FluidStack containedStack = FluidRegistry.getFluidStack(tank.getFluid(), (int) tank.getFluidLevel());
                int amountFilled = handler.fill(containedStack, !player.isCreative());
                if (amountFilled > 0) {
                    //Were able to fill the item.  Apply state change to tank and item.
                    tank.drain(tank.getFluid(), amountFilled, true);
                    player.setHeldStack(new WrapperItemStack(handler.getContainer()));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IWrapperNBT getData() {
        return stack.hasTagCompound() ? new WrapperNBT(stack.getTagCompound().copy()) : InterfaceManager.coreInterface.getNewNBTWrapper();
    }

    @Override
    public void setData(IWrapperNBT data) {
        stack.setTagCompound(data != null ? ((WrapperNBT) data).tag : null);
    }
}