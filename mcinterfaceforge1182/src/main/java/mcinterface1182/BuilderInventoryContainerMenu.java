package mcinterface1182;

import java.util.UUID;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/**
 * Minecraft-native menu facade for IV inventories.  Only the visible six rows are represented
 * by slots at one time, but those slots are real vanilla slots: Minecraft owns the carried stack
 * and all click/drag/keyboard behavior.  Changing pages remaps the facade to another six rows of
 * the same IV inventory without changing its saved representation.
 */
public class BuilderInventoryContainerMenu extends AbstractContainerMenu {
    public static RegistryObject<MenuType<BuilderInventoryContainerMenu>> TYPE;

    public static final int COLUMNS = 9;
    public static final int VISIBLE_ROWS = 6;
    public static final int MAX_VISIBLE_SLOTS = COLUMNS * VISIBLE_ROWS;

    private final EntityInventoryContainer inventory;
    private final PagedContainer pagedInventory;
    private final Inventory playerInventory;
    private final IWrapperItemStack inventoryItem;
    private final int heldPlayerSlot;
    private final int visibleSlots;
    private final int inventoryYOffset;
    private final String texture;
    private int rowOffset;

    /** Client-side factory invoked with the extra data written by {@link WrapperPlayer}. */
    public BuilderInventoryContainerMenu(int containerID, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerID, playerInventory, getClientInventory(playerInventory, data.readUUID(), data.readInt(), data.readInt()), data.readUtf(), null, data.readInt());
    }

    public BuilderInventoryContainerMenu(int containerID, Inventory playerInventory, EntityInventoryContainer inventory, String texture, IWrapperItemStack inventoryItem, int heldPlayerSlot) {
        super(TYPE.get(), containerID);
        this.inventory = inventory;
        this.playerInventory = playerInventory;
        this.inventoryItem = inventoryItem;
        this.heldPlayerSlot = heldPlayerSlot;
        this.visibleSlots = Math.min(inventory.getSize(), MAX_VISIBLE_SLOTS);
        this.inventoryYOffset = (MAX_VISIBLE_SLOTS - visibleSlots) * 18 / COLUMNS / 2;
        this.texture = texture == null || texture.isEmpty() ? "mts:textures/guis/inventory.png" : texture;
        this.pagedInventory = new PagedContainer();

        //IV container slots.  The facade remaps these when the scrollbar changes rows.
        for (int slot = 0; slot < visibleSlots; ++slot) {
            addSlot(new IVSlot(pagedInventory, slot, 9 + 18 * (slot % COLUMNS), 13 + inventoryYOffset + 18 * (slot / COLUMNS)));
        }

        //Player main inventory, then hotbar.  Coordinates match the existing IV inventory atlas.
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < COLUMNS; ++column) {
                int playerSlot = column + row * COLUMNS + COLUMNS;
                addSlot(new PlayerSlot(playerInventory, playerSlot, 8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; ++column) {
            addSlot(new PlayerSlot(playerInventory, column, 8 + column * 18, 198));
        }
    }

    private static EntityInventoryContainer getClientInventory(Inventory playerInventory, UUID inventoryID, int size, int stackSize) {
        WrapperWorld world = WrapperWorld.getWrapperFor(playerInventory.player.level);
        AEntityA_Base entity = world.getEntity(inventoryID);
        if (entity instanceof EntityInventoryContainer) {
            return (EntityInventoryContainer) entity;
        }
        //Held crates do not have a persistent client-side IV entity.  Vanilla's menu sync fills
        //this temporary backing inventory immediately after the screen is opened.
        return new EntityInventoryContainer(world, null, size, stackSize);
    }

    public String getTexture() {
        return texture;
    }

    public int getVisibleSlots() {
        return visibleSlots;
    }

    public int getInventoryYOffset() {
        return inventoryYOffset;
    }

    public int getRowOffset() {
        return rowOffset;
    }

    public int getMaxRowOffset() {
        return inventory.getSize() > MAX_VISIBLE_SLOTS ? (inventory.getSize() - MAX_VISIBLE_SLOTS + COLUMNS - 1) / COLUMNS : 0;
    }

    public void setRowOffset(int requestedOffset) {
        int clampedOffset = Math.max(0, Math.min(getMaxRowOffset(), requestedOffset));
        if (rowOffset != clampedOffset) {
            rowOffset = clampedOffset;
            //Every facade slot may now expose a different backing slot.  A full broadcast prevents
            //equal-looking stacks on adjacent pages from being skipped by the normal diff sync.
            broadcastFullState();
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonID) {
        if (buttonID >= 0 && buttonID <= getMaxRowOffset()) {
            setRowOffset(buttonID);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int menuSlotIndex) {
        Slot slot = slots.get(menuSlotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();
        if (menuSlotIndex < visibleSlots) {
            if (!moveItemStackTo(slotStack, visibleSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (isAllowedInIVInventory(slotStack)) {
            if (!moveItemStackTo(slotStack, 0, visibleSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, slotStack);
        return originalStack;
    }

    @Override
    public void clicked(int slotID, int dragType, ClickType clickType, Player player) {
        if (heldPlayerSlot >= 0 && clickType == ClickType.SWAP && dragType == heldPlayerSlot) {
            return;
        }
        super.clicked(slotID, dragType, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.isValid;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (inventoryItem != null && !player.level.isClientSide) {
            saveHeldInventory();
            inventory.remove();
        }
    }

    private void saveHeldInventory() {
        IWrapperNBT newData = InterfaceManager.coreInterface.getNewNBTWrapper();
        newData.setData("inventory", inventory.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        inventoryItem.setData(newData);
    }

    private static boolean isAllowedInIVInventory(ItemStack stack) {
        if (!(stack.getItem() instanceof IBuilderItemInterface)) {
            return true;
        }
        AItemBase item = ((IBuilderItemInterface) stack.getItem()).getWrappedItem();
        return !(item instanceof ItemPartInteractable && ((ItemPartInteractable) item).definition.interactable.inventoryUnits != 0)
            && !(item instanceof ItemDecor && ((ItemDecor) item).definition.decor.inventoryUnits != 0);
    }

    private class PagedContainer implements Container {
        private int getBackingIndex(int visibleIndex) {
            return rowOffset * COLUMNS + visibleIndex;
        }

        private boolean isMapped(int visibleIndex) {
            return getBackingIndex(visibleIndex) < inventory.getSize();
        }

        @Override
        public int getContainerSize() {
            return visibleSlots;
        }

        @Override
        public boolean isEmpty() {
            for (int index = 0; index < visibleSlots; ++index) {
                if (!getItem(index).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int index) {
            return isMapped(index) ? ((WrapperItemStack) inventory.getStack(getBackingIndex(index))).stack : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            if (!isMapped(index)) {
                return ItemStack.EMPTY;
            }
            ItemStack current = getItem(index);
            ItemStack removed = current.split(count);
            inventory.setStack(new WrapperItemStack(current), getBackingIndex(index));
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            if (!isMapped(index)) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = getItem(index);
            inventory.setStack(new WrapperItemStack(ItemStack.EMPTY), getBackingIndex(index));
            setChanged();
            return removed;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (isMapped(index)) {
                inventory.setStack(new WrapperItemStack(stack), getBackingIndex(index));
                setChanged();
            }
        }

        @Override
        public int getMaxStackSize() {
            return inventory.getStackSize();
        }

        @Override
        public void setChanged() {
            if (inventoryItem != null && !playerInventory.player.level.isClientSide) {
                saveHeldInventory();
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return BuilderInventoryContainerMenu.this.stillValid(player);
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return isMapped(index) && isAllowedInIVInventory(stack) && inventory.isStackValid(new WrapperItemStack(stack), getBackingIndex(index));
        }

        @Override
        public void clearContent() {
            for (int index = 0; index < visibleSlots; ++index) {
                setItem(index, ItemStack.EMPTY);
            }
        }
    }

    private class IVSlot extends Slot {
        private IVSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return inventory.getStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return inventory.getStackSize();
        }

        @Override
        public boolean isActive() {
            return pagedInventory.isMapped(index);
        }
    }

    private class PlayerSlot extends Slot {
        private PlayerSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return heldPlayerSlot < 0 || getContainerSlot() != heldPlayerSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return (heldPlayerSlot < 0 || getContainerSlot() != heldPlayerSlot) && super.mayPlace(stack);
        }
    }
}



