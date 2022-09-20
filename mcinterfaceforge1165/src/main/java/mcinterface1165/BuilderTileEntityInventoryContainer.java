package mcinterface1165;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;

/**
 * Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityInventoryContainer<InventoryTileEntity extends ATileEntityBase<?> & ITileEntityInventoryProvider> extends BuilderTileEntity<InventoryTileEntity> implements IInventory {
    protected static TileEntityType<BuilderTileEntityInventoryContainer> TE_TYPE2;

    public BuilderTileEntityInventoryContainer() {
        super(TE_TYPE2);
    }

    @Override
    public int getContainerSize() {
        return tileEntity.getInventory().getSize();
    }

    @Override
    public boolean isEmpty() {
        return tileEntity.getInventory().getCount() == 0;
    }

    @Override
    public ItemStack getItem(int index) {
        return ((WrapperItemStack) tileEntity.getInventory().getStack(index)).stack;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        tileEntity.getInventory().removeFromSlot(index, count);
        return getItem(index);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        tileEntity.getInventory().removeFromSlot(index, tileEntity.getInventory().getStack(index).getSize());
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        tileEntity.getInventory().setStack(new WrapperItemStack(stack), index);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
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
