package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketInventoryContainerChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpDispense;

/**
 * A GUI that is used to set up fuel pumps as a pay-to-use system.  Allows for setting various items
 * for various fluid amounts.  Opened when the pump is clicked by a wrench from an OP player.
 * Will also open when a non-OP clicks the pump to let them select which item to spend on the fuel.
 *
 * @author don_bruce
 */
public class GUIFuelPump extends AGUIInventory {

    private final TileEntityFuelPump pump;
    private final boolean configuring;
    private final List<GUIComponentTextBox> interactableSlotBoxes = new ArrayList<>();
    private final List<AGUIComponent> fuelComponents = new ArrayList<>();
    private final List<AGUIComponent> paymentComponents = new ArrayList<>();
    private final List<GUIComponentItem> fuelItems = new ArrayList<>();
    private final List<GUIComponentItem> paymentItems = new ArrayList<>();

    public GUIFuelPump(TileEntityFuelPump pump, boolean configuring) {
        super(null);
        this.pump = pump;
        this.configuring = configuring;
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        interactableSlotBoxes.clear();
        fuelComponents.clear();
        paymentComponents.clear();
        fuelItems.clear();
        paymentItems.clear();

        //Create all currency slots.
        int xOffset = 8;
        for (int i = 0; i < pump.fuelItems.getSize(); ++i) {
            IWrapperItemStack stack = pump.fuelItems.getStack(i);
            GUIComponentButton itemButton = new GUIComponentButton(guiLeft + xOffset, guiTop + 12 + 22 * (i % (pump.fuelItems.getSize() / 2)), true) {
                @Override
                public void onClicked(boolean leftSide) {
                    if (configuring) {
                        //Remove stack from slot as we don't want this item available.
                        IWrapperItemStack changedStack = pump.fuelItems.getStack(interactableSlotButtons.indexOf(this));
                        changedStack.add(-changedStack.getSize());
                        InterfaceManager.packetInterface.sendToServer(new PacketInventoryContainerChange(pump.fuelItems, interactableSlotButtons.indexOf(this), changedStack));
                    } else {
                        //Send off packet to see if we need to remove stack count from player to pay for fuel.
                        InterfaceManager.packetInterface.sendToServer(new PacketTileEntityFuelPumpDispense(pump, player, interactableSlotButtons.indexOf(this)));
                    }
                }
            };
            itemButton.visible = !stack.isEmpty() || configuring;
            addComponent(itemButton);
            interactableSlotButtons.add(itemButton);
            fuelComponents.add(itemButton);

            GUIComponentItem itemIcon = new GUIComponentItem(itemButton);
            itemIcon.stack = stack;
            addComponent(itemIcon);
            interactableSlotIcons.add(itemIcon);
            fuelComponents.add(itemIcon);
            fuelItems.add(itemIcon);

            GUIComponentTextBox fuelAmount = new GUIComponentTextBox(itemButton.constructedX + itemButton.width + 4, itemButton.constructedY, 50, String.valueOf(pump.fuelAmounts.get(i))) {
                @Override
                public boolean isTextValid(String newText) {
                    //Only allow whole numbers.
                    return newText.matches("\\d+");
                }

                @Override
                public void handleTextChange() {
                    //Set new values on the pump.
                    InterfaceManager.packetInterface.sendToServer(new PacketTileEntityFuelPumpDispense(pump, player, interactableSlotBoxes.indexOf(this), Integer.parseInt(this.getText())));
                }
            };
            fuelAmount.visible = !stack.isEmpty() || configuring;
            fuelAmount.enabled = configuring;
            addComponent(fuelAmount);
            interactableSlotBoxes.add(fuelAmount);
            fuelComponents.add(fuelAmount);

            if (i == (pump.fuelItems.getSize() / 2 - 1)) {
                xOffset += 100;
            }
        }

        //Create all payment slots.
        int buttonOffset = interactableSlotButtons.size();
        int inventoryRowOffset = (MAX_ITEMS_PER_SCREEN - pump.paymentItems.getSize()) * GUIComponentButton.ITEM_BUTTON_SIZE / 9 / 2;
        for (byte i = 0; i < pump.paymentItems.getSize(); ++i) {
            GUIComponentButton itemButton = new GUIComponentButton(guiLeft + 8 + GUIComponentButton.ITEM_BUTTON_SIZE * (i % 9), guiTop + 52 + inventoryRowOffset + GUIComponentButton.ITEM_BUTTON_SIZE * (i / 9), true) {
                @Override
                public void onClicked(boolean leftSide) {
                    int index = interactableSlotButtons.indexOf(this) - buttonOffset;
                    if (!pump.paymentItems.getStack(index).isEmpty()) {
                        InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(pump.paymentItems, player, index, -1, false));
                    }
                }
            };
            addComponent(itemButton);
            interactableSlotButtons.add(itemButton);
            paymentComponents.add(itemButton);

            GUIComponentItem itemIcon = new GUIComponentItem(itemButton);
            addComponent(itemIcon);
            interactableSlotIcons.add(itemIcon);
            paymentComponents.add(itemIcon);
            paymentItems.add(itemIcon);
        }
    }

    @Override
    public void setStates() {
        super.setStates();
        //If we are configuring, change item states.
        if (configuring) {
            for (int i = 0; i < fuelItems.size(); ++i) {
                fuelItems.get(i).stack = pump.fuelItems.getStack(i);
            }
            for (int i = 0; i < paymentItems.size(); ++i) {
                paymentItems.get(i).stack = pump.paymentItems.getStack(i);
            }
        }
        paymentComponents.forEach(component -> component.visible = configuring);
    }

    @Override
    protected void handlePlayerItemClick(int slotClicked) {
        if (configuring) {
            //player clicked on item during config.  Set stack in next free slot.
            for (int i = 0; i < pump.fuelItems.getSize(); ++i) {
                IWrapperItemStack stack = pump.fuelItems.getStack(i);
                if (stack.isEmpty()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketInventoryContainerChange(pump.fuelItems, i, playerInventory.getStack(slotClicked)));
                    return;
                }
            }
        }
    }

    @Override
    protected boolean canStayOpen() {
        return pump.isValid;
    }
}
