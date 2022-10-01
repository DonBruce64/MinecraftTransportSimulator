package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;

/**
 * A GUI that is used to interface with inventory containers.   Displays the player's items on the bottom,
 * and the items in the container in the top.  Works a bit differently than the MC GUIs, as it
 * doesn't support item dragging or movement.  Just storage to the first available slot.
 *
 * @author don_bruce
 */
public class GUIInventoryContainer extends AGUIInventory {
    private static final int MAX_ITEMS_PER_SCREEN = 54;

    //GUIs components created at opening.
    private GUIComponentButton priorRowButton;
    private GUIComponentButton nextRowButton;
    private GUIComponentCutout sliderCutout;
    private final int maxRowIncrements;

    private final EntityInventoryContainer inventory;
    private final boolean isPlayerHolding;

    //Runtime variables.
    private int rowOffset;

    public GUIInventoryContainer(EntityInventoryContainer inventory, String texture, boolean isPlayerHolding) {
        super(texture);
        this.inventory = inventory;
        this.isPlayerHolding = isPlayerHolding;
        this.maxRowIncrements = inventory.getSize() > MAX_ITEMS_PER_SCREEN ? (inventory.getSize() - MAX_ITEMS_PER_SCREEN) / 9 + 1 : 0;
    }

    @Override
    public void setupComponents() {
        super.setupComponents();

        //Make a slider if we need to show extra rows.
        if (maxRowIncrements > 0) {
            //Create the prior and next row buttons.
            addComponent(priorRowButton = new GUIComponentButton(guiLeft + 174, guiTop + 11, 12, 7, 220, 0, 12, 7) {
                @Override
                public void onClicked(boolean leftSide) {
                    --rowOffset;
                }
            });
            addComponent(nextRowButton = new GUIComponentButton(guiLeft + 174, guiTop + 112, 12, 7, 232, 0, 12, 7) {
                @Override
                public void onClicked(boolean leftSide) {
                    ++rowOffset;
                }
            });

            //Add the slider box.  This is static and always rendered.
            addComponent(new GUIComponentCutout(guiLeft + 173, guiTop + 20, 14, 90, 242, 45));

            //Now add the slider.
            addComponent(sliderCutout = new GUIComponentCutout(guiLeft + 174, guiTop + 21, 12, 15, 244, 15));
        }

        //Create all inventory slots.  This is variable based on the size of the inventory, and can result in multiple pages.
        //However, one page can hold 6 rows, so we make all those slots and adjust as appropriate.
        int slotsToMake = Math.min(inventory.getSize(), MAX_ITEMS_PER_SCREEN);
        int inventoryRowOffset = (MAX_ITEMS_PER_SCREEN - slotsToMake) * GUIComponentButton.ITEM_BUTTON_SIZE / 9 / 2;
        for (byte i = 0; i < slotsToMake; ++i) {
            GUIComponentButton itemButton = new GUIComponentButton(guiLeft + 8 + GUIComponentButton.ITEM_BUTTON_SIZE * (i % 9), guiTop + 12 + inventoryRowOffset + GUIComponentButton.ITEM_BUTTON_SIZE * (i / 9), true) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(inventory, player, interactableSlotButtons.indexOf(this) + 9 * rowOffset, -1, isPlayerHolding));
                }
            };
            addComponent(itemButton);
            interactableSlotButtons.add(itemButton);

            GUIComponentItem itemIcon = new GUIComponentItem(itemButton);
            addComponent(itemIcon);
            interactableSlotIcons.add(itemIcon);
        }
    }

    @Override
    public void setStates() {
        super.setStates();
        //Set slider, next, and prior row button states, if we have scrolling.
        if (maxRowIncrements > 0) {
            priorRowButton.enabled = rowOffset > 0;
            nextRowButton.enabled = rowOffset < maxRowIncrements;
            sliderCutout.position.y = -sliderCutout.constructedY - 73 * rowOffset / maxRowIncrements;
        }

        //Set other item icons to other inventory.
        for (int i = 0; i < interactableSlotButtons.size(); ++i) {
            int index = i + 9 * rowOffset;
            if (inventory.getSize() > index) {
                IWrapperItemStack stack = inventory.getStack(index);
                interactableSlotButtons.get(i).visible = true;
                interactableSlotButtons.get(i).enabled = !stack.isEmpty();
                interactableSlotIcons.get(i).stack = stack;
            } else {
                interactableSlotButtons.get(i).visible = false;
                interactableSlotIcons.get(i).stack = null;
            }
        }
    }

    @Override
    protected void handlePlayerItemClick(int slotClicked) {
        InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(inventory, player, -1, slotClicked, isPlayerHolding));
    }
}
