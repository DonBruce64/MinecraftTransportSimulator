package mcinterface1165;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityInventoryContainer extends BuilderTileEntity implements IInventory, IItemHandler {
    protected static TileEntityType<BuilderTileEntityInventoryContainer> TE_TYPE2;

    private EntityInventoryContainer inventory;

    public BuilderTileEntityInventoryContainer() {
        super(TE_TYPE2);
    }

    @Override
    protected void setTileEntity(ATileEntityBase<?> tile) {
        super.setTileEntity(tile);
        this.inventory = ((ITileEntityInventoryProvider) tile).getInventory();
    }

    @Override
    public int getContainerSize() {
        return inventory.getSize();
    }

    @Override
    public int getSlots() {
        return getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.getCount() == 0;
    }

    @Override
    public ItemStack getItem(int index) {
        return ((WrapperItemStack) inventory.getStack(index)).stack;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        inventory.removeFromSlot(index, count);
        return getItem(index);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        inventory.removeFromSlot(index, inventory.getStack(index).getSize());
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = getStackInSlot(slot);
        if (stack.getCount() < amount) {
            amount = stack.getCount();
        }
        ItemStack extracted = stack.copy();
        extracted.setCount(amount);
        if (!simulate) {
            stack.setCount(stack.getCount() - amount);
            setItem(slot, stack);
        }
        return extracted;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        inventory.setStack(new WrapperItemStack(stack), index);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack existingStack = getStackInSlot(slot);
        if (ItemHandlerHelper.canItemStacksStack(stack, existingStack)) {
            int amount = existingStack.getMaxStackSize() - existingStack.getCount();
            if (amount != 0) {
                if (amount > stack.getCount()) {
                    amount = stack.getCount();
                }
                ItemStack remainderStack = stack.copy();
                remainderStack.setCount(remainderStack.getCount() - amount);
                if (!simulate) {
                    existingStack.setCount(existingStack.getCount() + amount);
                    setItem(slot, existingStack);
                }
                return remainderStack;
            }
        }
        return stack;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getSlotLimit(int slot) {
        return getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }

    @Override
    public void clearContent() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && (facing == Direction.UP || facing == Direction.DOWN)) {
            return LazyOptional.of(() -> (T) this);
        } else {
            return super.getCapability(capability, facing);
        }
    }
}
