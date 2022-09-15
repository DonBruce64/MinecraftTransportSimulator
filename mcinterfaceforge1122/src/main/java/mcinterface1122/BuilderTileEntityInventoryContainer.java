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

/**
 * Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityInventoryContainer<InventoryTileEntity extends ATileEntityBase<?> & ITileEntityInventoryProvider> extends BuilderTileEntity<InventoryTileEntity> implements IInventory {

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
        return tileEntity.getInventory().getSize();
    }

    @Override
    public boolean isEmpty() {
        return tileEntity.getInventory().getCount() == 0;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return ((WrapperItemStack) tileEntity.getInventory().getStack(index)).stack;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        tileEntity.getInventory().removeFromSlot(index, count);
        return getStackInSlot(index);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        tileEntity.getInventory().removeFromSlot(index, tileEntity.getInventory().getStack(index).getSize());
        return ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        tileEntity.getInventory().setStack(new WrapperItemStack(stack), index);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
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
