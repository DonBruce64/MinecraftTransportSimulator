package mcinterface1165;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderBlockEntityInventoryContainer extends BuilderBlockEntity implements IItemHandler {
    protected static RegistryObject<BlockEntityType<BuilderBlockEntityInventoryContainer>> TE_INVENTORY;

    private EntityInventoryContainer inventory;

    public BuilderBlockEntityInventoryContainer() {
        super(TE_INVENTORY.get());
    }

    @Override
    protected void setTileEntity(ATileEntityBase<?> tile) {
        super.setTileEntity(tile);
        this.inventory = ((ITileEntityInventoryProvider) tile).getInventory();
    }

    @Override
    public int getSlots() {
        return inventory.getSize();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int index) {
        return ((WrapperItemStack) inventory.getStack(index)).stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = getStackInSlot(slot);
        if (stack.getCount() < amount) {
            amount = stack.getCount();
        }
        ItemStack extracted = stack.copy();
        extracted.setCount(amount);
        if (!simulate) {
            stack.setCount(stack.getCount() - amount);
            inventory.setStack(new WrapperItemStack(stack), slot);
        }
        return extracted;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        ItemStack existingStack = getStackInSlot(slot);
        if (ItemHandlerHelper.canItemStacksStack(stack, existingStack)) {
            int amount = existingStack.getMaxCount() - existingStack.getCount();
            if (amount != 0) {
                if (amount > stack.getCount()) {
                    amount = stack.getCount();
                }
                ItemStack remainderStack = stack.copy();
                remainderStack.setCount(remainderStack.getCount() - amount);
                if (!simulate) {
                    existingStack.setCount(existingStack.getCount() + amount);
                    inventory.setStack(new WrapperItemStack(existingStack), slot);
                }
                return remainderStack;
            }
        }
        return stack;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, Direction facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && (facing == Direction.UP || facing == Direction.DOWN)) {
            return LazyOptional.of(() -> (T) this);
        } else {
            return super.getCapability(capability, facing);
        }
    }
}
