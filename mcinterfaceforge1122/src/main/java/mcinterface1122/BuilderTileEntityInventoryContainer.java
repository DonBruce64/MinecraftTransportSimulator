package mcinterface1122;

import javax.annotation.Nullable;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
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
public class BuilderTileEntityInventoryContainer<InventoryTileEntity extends ATileEntityBase<?> & ITileEntityInventoryProvider> extends BuilderTileEntity<InventoryTileEntity> implements IInventory, IItemHandler {

    public BuilderTileEntityInventoryContainer() {
        super();
    }

    @Override
    public String getName() {
        return "item_loader";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getSizeInventory() {
        return tileEntity != null ? tileEntity.getInventory().getSize() : 0;
    }

    @Override
    public int getSlots() {
        return getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        return tileEntity != null ? tileEntity.getInventory().getCount() == 0 : false;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return tileEntity != null ? ((WrapperItemStack) tileEntity.getInventory().getStack(index)).stack : ItemStack.EMPTY;
    }


    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (tileEntity != null) {
            tileEntity.getInventory().removeFromSlot(index, count);
            return getStackInSlot(index);
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (tileEntity != null) {
            tileEntity.getInventory().removeFromSlot(index, tileEntity.getInventory().getStack(index).getSize());
        }
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
            setInventorySlotContents(slot, stack);
        }
        return extracted;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (tileEntity != null) {
            tileEntity.getInventory().setStack(new WrapperItemStack(stack), index);
        }
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
                    setInventorySlotContents(slot, existingStack);
                }
                return remainderStack;
            }
        }
        return stack;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public int getSlotLimit(int slot) {
        return getInventoryStackLimit();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
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
