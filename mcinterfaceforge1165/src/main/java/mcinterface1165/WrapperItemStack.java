package mcinterface1165;

import java.util.List;

import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;

public class WrapperItemStack implements IWrapperItemStack {

    protected final ItemStack stack;

    protected WrapperItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean isCompleteMatch(IWrapperItemStack other) {
        ItemStack otherStack = ((WrapperItemStack) other).stack;
        return otherStack.sameItem(stack) && (otherStack.hasTag() ? otherStack.getTag().equals(stack.getTag()) : !stack.hasTag());
    }

    @Override
    public int getFuelValue() {
        return ForgeHooks.getBurnTime(stack, null);
    }

    @Override
    public IWrapperItemStack getSmeltedItem(AWrapperWorld world) {
        World mcWorld = ((WrapperWorld) world).world;
        List<FurnaceRecipe> results = mcWorld.getRecipeManager().getAllRecipesFor(IRecipeType.SMELTING);
        return new WrapperItemStack(results.isEmpty() ? ItemStack.EMPTY : results.get(0).getResultItem());
    }

    @Override
    public int getSmeltingTime(AWrapperWorld world) {
        World mcWorld = ((WrapperWorld) world).world;
        return mcWorld.getRecipeManager().getAllRecipesFor(IRecipeType.SMELTING).get(0).getCookingTime();
    }

    @Override
    public AItemBase getItem() {
        Item item = stack.getItem();
        return item instanceof IBuilderItemInterface ? ((IBuilderItemInterface) item).getWrappedItem() : null;
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
        return new WrapperItemStack(stack.split(qty));
    }

    @Override
    public boolean interactWith(EntityFluidTank tank, IWrapperPlayer player) {
        IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null).orElse(null);
        if (handler != null) {
            if (!player.isSneaking()) {
                //Item can provide fluid.  Check if the tank can accept it.
                FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
                if (drainedStack != null) {
                    //Able to take fluid from item, attempt to do so.
                    int amountToDrain = (int) tank.fill(drainedStack.getFluid().getRegistryName().getPath(), drainedStack.getAmount(), false);
                    drainedStack = handler.drain(amountToDrain, player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                    if (drainedStack != null) {
                        //Was able to provide liquid from item.  Fill the tank.
                        tank.fill(drainedStack.getFluid().getRegistryName().getPath(), drainedStack.getAmount(), true);
                        player.setHeldStack(new WrapperItemStack(handler.getContainer()));
                    }
                }
            } else {
                //Item can hold fluid.  Check if we can fill it.
                FluidStack containedStack = new FluidStack(ForgeRegistries.FLUIDS.getValue(new ResourceLocation(tank.getFluid())), (int) tank.getFluidLevel());
                int amountFilled = handler.fill(containedStack, player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
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
        return stack.hasTag() ? new WrapperNBT(stack.getTag().copy()) : InterfaceManager.coreInterface.getNewNBTWrapper();
    }

    @Override
    public void setData(IWrapperNBT data) {
        stack.setTag(data != null ? ((WrapperNBT) data).tag : null);
    }
}