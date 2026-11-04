package mcinterface1122;

import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * Minecraft-native container facade for IV inventories on 1.12.  The visible six rows are
 * real vanilla slots, while scrolling remaps them onto later rows in the unchanged IV backing
 * inventory.  Consequently vanilla owns the cursor stack, splitting, dragging, hotbar swaps,
 * double-click collection, and shift-click behavior.
 */
public class BuilderInventoryContainerMenu extends Container {
    public static final int COLUMNS = 9;
    public static final int VISIBLE_ROWS = 6;
    public static final int MAX_VISIBLE_SLOTS = COLUMNS * VISIBLE_ROWS;

    private final EntityInventoryContainer inventory;
    private final PagedInventory pagedInventory;
    private final InventoryPlayer playerInventory;
    private final IWrapperItemStack inventoryItem;
    private final int heldPlayerSlot;
    private final int visibleSlots;
    private final int inventoryYOffset;
    private final String texture;
    private int rowOffset;

    public BuilderInventoryContainerMenu(InventoryPlayer playerInventory, EntityInventoryContainer inventory, String texture, IWrapperItemStack inventoryItem, int heldPlayerSlot) {
        this.inventory = inventory;
        this.playerInventory = playerInventory;
        this.inventoryItem = inventoryItem;
        this.heldPlayerSlot = heldPlayerSlot;
        this.visibleSlots = Math.min(inventory.getSize(), MAX_VISIBLE_SLOTS);
        this.inventoryYOffset = (MAX_VISIBLE_SLOTS - visibleSlots) * 18 / COLUMNS / 2;
        this.texture = texture == null || texture.isEmpty() ? "mts:textures/guis/inventory.png" : texture;
        this.pagedInventory = new PagedInventory();

        for (int slot = 0; slot < visibleSlots; ++slot) {
            addSlotToContainer(new IVSlot(pagedInventory, slot, 9 + 18 * (slot % COLUMNS), 13 + inventoryYOffset + 18 * (slot / COLUMNS)));
        }

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < COLUMNS; ++column) {
                int playerSlot = column + row * COLUMNS + COLUMNS;
                addSlotToContainer(new PlayerSlot(playerInventory, playerSlot, 8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < COLUMNS; ++column) {
            addSlotToContainer(new PlayerSlot(playerInventory, column, 8 + column * 18, 198));
        }
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
        rowOffset = Math.max(0, Math.min(getMaxRowOffset(), requestedOffset));
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int buttonID) {
        if (buttonID >= 0 && buttonID <= getMaxRowOffset()) {
            if (rowOffset != buttonID) {
                setRowOffset(buttonID);
                //All facade slots now expose different backing slots.  Send the complete window
                //so equal-looking stacks on adjacent pages are not skipped by differential sync.
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).sendContainerToPlayer(this);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int menuSlotIndex) {
        Slot slot = inventorySlots.get(menuSlotIndex);
        if (!slot.getHasStack() || !slot.canTakeStack(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getStack();
        ItemStack originalStack = slotStack.copy();
        if (menuSlotIndex < visibleSlots) {
            if (!mergeItemStack(slotStack, visibleSlots, inventorySlots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (isAllowedInIVInventory(slotStack)) {
            if (!mergeItemStack(slotStack, 0, visibleSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        slot.onTake(player, slotStack);
        return originalStack;
    }

    @Override
    public ItemStack slotClick(int slotID, int dragType, ClickType clickType, EntityPlayer player) {
        //Number-key swaps address the hotbar directly and bypass the PlayerSlot instance below.
        //Keep a held crate anchored while its contents are being edited so it cannot contain itself.
        if (heldPlayerSlot >= 0 && clickType == ClickType.SWAP && dragType == heldPlayerSlot) {
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotID, dragType, clickType, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return inventory.isValid;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (inventoryItem != null && !player.world.isRemote) {
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
        AItemBase item = ((IBuilderItemInterface) stack.getItem()).getItem();
        return !(item instanceof ItemPartInteractable && ((ItemPartInteractable) item).definition.interactable.inventoryUnits != 0)
            && !(item instanceof ItemDecor && ((ItemDecor) item).definition.decor.inventoryUnits != 0);
    }

    private class PagedInventory implements IInventory {
        private int getBackingIndex(int visibleIndex) {
            return rowOffset * COLUMNS + visibleIndex;
        }

        private boolean isMapped(int visibleIndex) {
            return getBackingIndex(visibleIndex) < inventory.getSize();
        }

        @Override
        public int getSizeInventory() {
            return visibleSlots;
        }

        @Override
        public boolean isEmpty() {
            for (int index = 0; index < visibleSlots; ++index) {
                if (!getStackInSlot(index).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return isMapped(index) ? ((WrapperItemStack) inventory.getStack(getBackingIndex(index))).stack : ItemStack.EMPTY;
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            if (!isMapped(index)) {
                return ItemStack.EMPTY;
            }
            ItemStack current = getStackInSlot(index);
            ItemStack removed = current.splitStack(count);
            inventory.setStack(new WrapperItemStack(current), getBackingIndex(index));
            markDirty();
            return removed;
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            if (!isMapped(index)) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = getStackInSlot(index);
            inventory.setStack(new WrapperItemStack(ItemStack.EMPTY), getBackingIndex(index));
            markDirty();
            return removed;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            if (isMapped(index)) {
                inventory.setStack(new WrapperItemStack(stack), getBackingIndex(index));
                markDirty();
            }
        }

        @Override
        public int getInventoryStackLimit() {
            return inventory.getStackSize();
        }

        @Override
        public void markDirty() {
            if (inventoryItem != null && !playerInventory.player.world.isRemote) {
                saveHeldInventory();
            }
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            return BuilderInventoryContainerMenu.this.canInteractWith(player);
        }

        @Override
        public void openInventory(EntityPlayer player) {
        }

        @Override
        public void closeInventory(EntityPlayer player) {
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return isMapped(index) && isAllowedInIVInventory(stack) && inventory.isStackValid(new WrapperItemStack(stack), getBackingIndex(index));
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
            for (int index = 0; index < visibleSlots; ++index) {
                setInventorySlotContents(index, ItemStack.EMPTY);
            }
        }

        @Override
        public String getName() {
            return "container.mts.inventory";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public ITextComponent getDisplayName() {
            return new TextComponentString("");
        }
    }

    private class IVSlot extends Slot {
        private IVSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public int getSlotStackLimit() {
            return BuilderInventoryContainerMenu.this.inventory.getStackSize();
        }

        @Override
        public int getItemStackLimit(ItemStack stack) {
            return BuilderInventoryContainerMenu.this.inventory.getStackSize();
        }

        @Override
        public boolean isEnabled() {
            return pagedInventory.isMapped(getSlotIndex());
        }
    }

    private class PlayerSlot extends Slot {
        private PlayerSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return heldPlayerSlot < 0 || getSlotIndex() != heldPlayerSlot;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return (heldPlayerSlot < 0 || getSlotIndex() != heldPlayerSlot) && super.isItemValid(stack);
        }
    }
}
