package mcinterface1211;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.minecraft.core.registries.BuiltInRegistries;

public class WrapperItemStack implements IWrapperItemStack {

    protected final ItemStack stack;

    protected WrapperItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean isCompleteMatch(IWrapperItemStack other) {
        ItemStack otherStack = ((WrapperItemStack) other).stack;
        return !stack.isEmpty() && otherStack.is(stack.getItem()) && ItemStack.isSameItemSameComponents(otherStack, stack);
    }

    @Override
    public int getFurnaceFuelValue() {
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    public IWrapperItemStack getSmeltedItem(AWrapperWorld world) {
        Level mcWorld = ((WrapperWorld) world).world;
        SingleRecipeInput input = new SingleRecipeInput(stack);
        for (RecipeHolder<SmeltingRecipe> holder : mcWorld.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
            if (holder.value().matches(input, mcWorld)) {
                return new WrapperItemStack(holder.value().getResultItem(mcWorld.registryAccess()));
            }
        }
        return new WrapperItemStack(ItemStack.EMPTY);
    }

    @Override
    public int getSmeltingTime(AWrapperWorld world) {
        Level mcWorld = ((WrapperWorld) world).world;
        SingleRecipeInput input = new SingleRecipeInput(stack);
        for (RecipeHolder<SmeltingRecipe> holder : mcWorld.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
            if (holder.value().matches(input, mcWorld)) {
                return holder.value().getCookingTime();
            }
        }
        return 0;
    }

    @Override
    public boolean isBrewingFuel() {
        return stack.getItem() == Items.BLAZE_POWDER;
    }

    @Override
    public boolean isBrewingVessel() {
        return stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION || stack.getItem() == Items.LINGERING_POTION || stack.getItem() == Items.GLASS_BOTTLE;
    }

    @Override
    public boolean isBrewingModifier() {
        Item item = stack.getItem();
        return item == Items.NETHER_WART || item == Items.REDSTONE || item == Items.GLOWSTONE_DUST
                || item == Items.FERMENTED_SPIDER_EYE || item == Items.GUNPOWDER || item == Items.DRAGON_BREATH
                || item == Items.PHANTOM_MEMBRANE || item == Items.RABBIT_FOOT || item == Items.GLISTERING_MELON_SLICE
                || item == Items.SPIDER_EYE || item == Items.PUFFERFISH || item == Items.MAGMA_CREAM
                || item == Items.GOLDEN_CARROT || item == Items.BLAZE_POWDER || item == Items.GHAST_TEAR
                || item == Items.TURTLE_HELMET || item == Items.SUGAR;
    }

    @Override
    public IWrapperItemStack getBrewedItem(IWrapperItemStack modifierStack) {
        //Brewing registry is no longer static in NeoForge 1.21.1, return empty for now.
        //Actual brewing is handled by vanilla's PotionBrewing which requires a level instance.
        return new WrapperItemStack(ItemStack.EMPTY);
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
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler != null) {
            if (!player.isSneaking()) {
                //Item can provide fluid.  Check if the tank can accept it.
                FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
                if (drainedStack != null) {
                    //Able to take fluid from item, attempt to do so.
                    ResourceLocation fluidLocation = BuiltInRegistries.FLUID.getKey(drainedStack.getFluid());
                    int amountToDrain = (int) tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), drainedStack.getAmount(), false);
                    drainedStack = handler.drain(amountToDrain, player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                    if (drainedStack != null) {
                        //Was able to provide liquid from item.  Fill the tank.
                        tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), drainedStack.getAmount(), true);
                        player.setHeldStack(new WrapperItemStack(handler.getContainer()));
                    }
                }
            } else {
                //Item can hold fluid.  Check if we can fill it.
                //Need to find the mod that registered this fluid, Forge is stupid and has them per-mod vs just all with a single name.
                for (ResourceLocation fluidKey : BuiltInRegistries.FLUID.keySet()) {
                    if ((tank.getFluidMod().equals(EntityFluidTank.WILDCARD_FLUID_MOD) || tank.getFluidMod().equals(fluidKey.getNamespace())) && fluidKey.getPath().equals(tank.getFluid())) {
                        FluidStack containedStack = new FluidStack(BuiltInRegistries.FLUID.get(fluidKey), (int) tank.getFluidLevel());
                        int amountFilled = handler.fill(containedStack, player.isCreative() ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                        if (amountFilled > 0) {
                            //Were able to fill the item.  Apply state change to tank and item.
                            tank.drain(amountFilled, true);
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
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? new WrapperNBT(customData.copyTag()) : null;
    }

    @Override
    public void setData(IWrapperNBT data) {
        if (data != null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(((WrapperNBT) data).tag));
        } else {
            stack.remove(DataComponents.CUSTOM_DATA);
        }
    }
}