package mcinterface262;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityInventoryProvider;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Builder for tile entities that contain inventories.  This builder ticks.
 *
 * @author don_bruce
 */
public class BuilderTileEntityInventoryContainer extends BuilderTileEntity implements ResourceHandler<ItemResource> {
    protected static DeferredHolder<BlockEntityType<?>, BlockEntityType<BuilderTileEntityInventoryContainer>> TE_TYPE2;

    private EntityInventoryContainer inventory;

    public BuilderTileEntityInventoryContainer(BlockPos pos, BlockState state) {
        super(TE_TYPE2.get(), pos, state);
    }

    @Override
    protected void setTileEntity(ATileEntityBase<?> tile) {
        super.setTileEntity(tile);
        this.inventory = ((ITileEntityInventoryProvider) tile).getInventory();
    }

    private ItemStack getStackInSlot(int index) {
        return inventory != null ? ((WrapperItemStack) inventory.getStack(index)).stack : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return inventory != null ? inventory.getSize() : 0;
    }

    @Override
    public ItemResource getResource(int index) {
        ItemStack stack = getStackInSlot(index);
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        return getStackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return resource.isEmpty() ? 0 : resource.toStack().getMaxStackSize();
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (inventory == null || resource.isEmpty()) {
            return 0;
        }
        ItemStack existingStack = getStackInSlot(index);
        if (existingStack.isEmpty()) {
            inventory.setStack(new WrapperItemStack(resource.toStack(amount)), index);
            return amount;
        }
        if (ItemResource.of(existingStack).equals(resource)) {
            int freeSpace = existingStack.getMaxStackSize() - existingStack.getCount();
            int accepted = Math.min(freeSpace, amount);
            existingStack.setCount(existingStack.getCount() + accepted);
            inventory.setStack(new WrapperItemStack(existingStack), index);
            return accepted;
        }
        return 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext context) {
        if (inventory == null || resource.isEmpty()) {
            return 0;
        }
        ItemStack existingStack = getStackInSlot(index);
        if (existingStack.isEmpty() || !ItemResource.of(existingStack).equals(resource)) {
            return 0;
        }
        int extracted = Math.min(amount, existingStack.getCount());
        existingStack.setCount(existingStack.getCount() - extracted);
        inventory.setStack(new WrapperItemStack(existingStack), index);
        return extracted;
    }
}
