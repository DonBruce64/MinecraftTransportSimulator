package mcinterface261;

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

    @Override
    public int size() {
        return inventory != null ? inventory.getSize() : 0;
    }

    @Override
    public ItemResource getResource(int index) {
        if (inventory == null) return ItemResource.EMPTY;
        ItemStack stack = ((WrapperItemStack) inventory.getStack(index)).stack;
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        if (inventory == null) return 0;
        return ((WrapperItemStack) inventory.getStack(index)).stack.getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return 64;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (inventory == null || resource.isEmpty()) return 0;
        ItemStack existing = ((WrapperItemStack) inventory.getStack(index)).stack;
        ItemStack toInsert = resource.toStack(amount);
        if (existing.isEmpty()) {
            inventory.setStack(new WrapperItemStack(toInsert.copy()), index);
            return amount;
        } else if (ItemStack.isSameItemSameComponents(existing, toInsert)) {
            int canAdd = existing.getMaxStackSize() - existing.getCount();
            int added = Math.min(canAdd, amount);
            if (added > 0) {
                existing.setCount(existing.getCount() + added);
                inventory.setStack(new WrapperItemStack(existing), index);
            }
            return added;
        }
        return 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (inventory == null || resource.isEmpty()) return 0;
        ItemStack stack = ((WrapperItemStack) inventory.getStack(index)).stack;
        if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, resource.toStack(1))) return 0;
        int extracted = Math.min(stack.getCount(), amount);
        stack.setCount(stack.getCount() - extracted);
        inventory.setStack(new WrapperItemStack(stack), index);
        return extracted;
    }

}
