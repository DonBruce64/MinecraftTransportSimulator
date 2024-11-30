package mcinterface1182;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.RegistryObject;

/**
 * Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityInventoryContainer extends BuilderTileEntity implements IItemHandler {
    protected static RegistryObject<BlockEntityType<BuilderTileEntityInventoryContainer>> TE_TYPE2;

    private EntityInventoryContainer inventory;

    public BuilderTileEntityInventoryContainer(BlockPos pos, BlockState state) {
        super(TE_TYPE2.get(), pos, state);
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
    public ItemStack getStackInSlot(int index) {
        return ((WrapperItemStack) inventory.getStack(index)).stack;
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
            inventory.setStack(new WrapperItemStack(stack), slot);
        }
        return extracted;
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
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
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
