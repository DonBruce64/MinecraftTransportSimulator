package mcinterface1122;

import javax.annotation.Nullable;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityInventoryContainer<InventoryTileEntity extends ATileEntityBase<?> & ITileEntityInventoryProvider> extends BuilderTileEntity<InventoryTileEntity> implements IItemHandler {

    public BuilderTileEntityInventoryContainer() {
        super();
    }

    @Override
    public int getSlots() {
        return tileEntity != null ? tileEntity.getInventory().getSize() : 0;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return tileEntity != null ? ((WrapperItemStack) tileEntity.getInventory().getStack(index)).stack : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (tileEntity != null) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.getCount() < amount) {
                amount = stack.getCount();
            }
            ItemStack extracted = stack.copy();
            extracted.setCount(amount);
            if (!simulate) {
                stack.setCount(stack.getCount() - amount);
                tileEntity.getInventory().setStack(new WrapperItemStack(stack), slot);
            }
            return extracted;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (tileEntity != null) {
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
                        tileEntity.getInventory().setStack(new WrapperItemStack(existingStack), slot);
                    }
                    return remainderStack;
                }
            }
        }
        return stack;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && (EnumFacing.UP.equals(facing) || EnumFacing.DOWN.equals(facing))) {
            return true;
        } else {
            return super.hasCapability(capability, facing);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && (EnumFacing.UP.equals(facing) || EnumFacing.DOWN.equals(facing))) {
            return (T) this;
        } else {
            return super.getCapability(capability, facing);
        }
    }
}
