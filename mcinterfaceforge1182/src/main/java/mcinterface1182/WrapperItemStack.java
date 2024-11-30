package mcinterface1182;

import java.util.List;

import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
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
    public int getFurnaceFuelValue() {
        return ForgeHooks.getBurnTime(stack, null);
    }

    @Override
    public IWrapperItemStack getSmeltedItem(AWrapperWorld world) {
        Level mcWorld = ((WrapperWorld) world).world;
        List<SmeltingRecipe> results = mcWorld.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING);
        return new WrapperItemStack(results.isEmpty() ? ItemStack.EMPTY : results.get(0).getResultItem());
    }

    @Override
    public int getSmeltingTime(AWrapperWorld world) {
        Level mcWorld = ((WrapperWorld) world).world;
        return mcWorld.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).get(0).getCookingTime();
    }

    @Override
    public boolean isBrewingFuel() {
        return stack.getItem() == Items.BLAZE_POWDER;
    }

    @Override
    public boolean isBrewingVessel() {
        return BrewingRecipeRegistry.isValidInput(stack);
    }

    @Override
    public boolean isBrewingModifier() {
        return BrewingRecipeRegistry.isValidIngredient(stack);
    }

    @Override
    public IWrapperItemStack getBrewedItem(IWrapperItemStack modifierStack) {
        return new WrapperItemStack(BrewingRecipeRegistry.getOutput(stack, ((WrapperItemStack) modifierStack).stack).copy());
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
    public IWrapperItemStack copy() {
        return new WrapperItemStack(stack.copy());
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
                //Need to find the mod that registered this fluid, Forge is stupid and has them per-mod vs just all with a single name.
                for (ResourceLocation fluidKey : ForgeRegistries.FLUIDS.getKeys()) {
                    if (fluidKey.getPath().equals(tank.getFluid())) {
                        FluidStack containedStack = new FluidStack(ForgeRegistries.FLUIDS.getValue(fluidKey), (int) tank.getFluidLevel());
                        int amountFilled = handler.fill(containedStack, player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                        if (amountFilled > 0) {
                            //Were able to fill the item.  Apply state change to tank and item.
                            tank.drain(tank.getFluid(), amountFilled, true);
                            player.setHeldStack(new WrapperItemStack(handler.getContainer()));
                        }
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IWrapperNBT getData() {
        return stack.hasTag() ? new WrapperNBT(stack.getTag().copy()) : null;
    }

    @Override
    public void setData(IWrapperNBT data) {
        stack.setTag(data != null ? ((WrapperNBT) data).tag : null);
    }
}