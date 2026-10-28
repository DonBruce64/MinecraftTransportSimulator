package mcinterface261;

import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import net.minecraft.resources.Identifier;
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
import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
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
        FuelValues fuelValues = null;
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.level != null) fuelValues = mc.level.fuelValues();
        } catch (Exception ignored) {}
        if (fuelValues == null) {
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) fuelValues = server.overworld().fuelValues();
        }
        return fuelValues != null ? stack.getBurnTime(RecipeType.SMELTING, fuelValues) : 0;
    }

    @Override
    public IWrapperItemStack getSmeltedItem(AWrapperWorld world) {
        Level mcWorld = ((WrapperWorld) world).world;
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return new WrapperItemStack(ItemStack.EMPTY);
        Optional<RecipeHolder<SmeltingRecipe>> holder = server.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), mcWorld);
        return new WrapperItemStack(holder.map(h -> h.value().assemble(new SingleRecipeInput(stack))).orElse(ItemStack.EMPTY));
    }

    @Override
    public int getSmeltingTime(AWrapperWorld world) {
        Level mcWorld = ((WrapperWorld) world).world;
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        Optional<RecipeHolder<SmeltingRecipe>> holder = server.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), mcWorld);
        return holder.map(h -> h.value().cookingTime()).orElse(0);
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
        Player mcPlayer = ((WrapperPlayer) player).player;
        ResourceHandler<FluidResource> handler = stack.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forPlayerInteraction(mcPlayer, InteractionHand.MAIN_HAND));
        if (handler != null) {
            if (!player.isSneaking()) {
                //Item can provide fluid.  Check if it has any.
                for (int i = 0; i < handler.size(); ++i) {
                    FluidResource resource = handler.getResource(i);
                    if (!resource.isEmpty()) {
                        Identifier fluidLocation = BuiltInRegistries.FLUID.getKey(resource.getFluid());
                        if (fluidLocation != null) {
                            int available = handler.getAmountAsInt(i);
                            int amountToDrain = (int) tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), available, false);
                            if (amountToDrain > 0) {
                                try (Transaction transaction = Transaction.openRoot()) {
                                    int extracted = player.isCreative() ? amountToDrain : handler.extract(i, resource, amountToDrain, transaction);
                                    if (extracted > 0) {
                                        tank.fill(fluidLocation.getPath(), fluidLocation.getNamespace(), extracted, true);
                                        transaction.commit();
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            } else {
                //Item can hold fluid.  Check if we can fill it from the tank.
                for (Identifier fluidKey : BuiltInRegistries.FLUID.keySet()) {
                    if ((tank.getFluidMod().equals(EntityFluidTank.WILDCARD_FLUID_MOD) || tank.getFluidMod().equals(fluidKey.getNamespace())) && fluidKey.getPath().equals(tank.getFluid())) {
                        FluidResource fluidResource = FluidResource.of(BuiltInRegistries.FLUID.getValue(fluidKey));
                        try (Transaction transaction = Transaction.openRoot()) {
                            int amountInserted = player.isCreative() ? (int) tank.getFluidLevel() : handler.insert(fluidResource, (int) tank.getFluidLevel(), transaction);
                            if (amountInserted > 0) {
                                tank.drain(amountInserted, !player.isCreative());
                                transaction.commit();
                            }
                        }
                        break;
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