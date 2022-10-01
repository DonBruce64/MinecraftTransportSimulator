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

/**
 * Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityInventoryContainer extends BuilderTileEntity implements IInventory {
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
    public boolean isEmpty() {
        return inventory.getCount() == 0;
    }

    @Override
    public ItemStack getItem(int index) {
        return ((WrapperItemStack) inventory.getStack(index)).stack;
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
    public void setItem(int index, ItemStack stack) {
        inventory.setStack(new WrapperItemStack(stack), index);
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
