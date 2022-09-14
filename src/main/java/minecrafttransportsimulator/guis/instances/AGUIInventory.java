package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Abstract GUI class used as the base for GUIs with inventory slots.  By default, it sets up slots
 * for all the player's items and populates them.
 *
 * @author don_bruce
 */
public abstract class AGUIInventory extends AGUIBase {

    private final String texture;
    protected final IWrapperPlayer player;
    protected final IWrapperInventory playerInventory;
    private final List<GUIComponentButton> playerSlotButtons = new ArrayList<>();
    private final List<GUIComponentItem> playerSlotIcons = new ArrayList<>();
    protected final List<GUIComponentButton> interactableSlotButtons = new ArrayList<>();
    protected final List<GUIComponentItem> interactableSlotIcons = new ArrayList<>();

    public AGUIInventory(String texture) {
        super();
        this.texture = texture != null ? texture : "mts:textures/guis/inventory.png";
        this.player = InterfaceManager.clientInterface.getClientPlayer();
        this.playerInventory = player.getInventory();
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        //Create the player item buttons and icons.  This is a static list of all 36 slots.
        //Rendering will occur if the player has an item in that slot.
        playerSlotButtons.clear();
        playerSlotIcons.clear();
        int yOffset = getPlayerInventoryOffset();
        for (byte i = 0; i < 36; ++i) {
            GUIComponentButton itemButton = new GUIComponentButton(guiLeft + 7 + GUIComponentButton.ITEM_BUTTON_SIZE * (i % 9), guiTop + yOffset, false) {
                @Override
                public void onClicked(boolean leftSide) {
                    handlePlayerItemClick(playerSlotButtons.indexOf(this));
                }
            };
            addComponent(itemButton);
            playerSlotButtons.add(itemButton);

            GUIComponentItem itemIcon = new GUIComponentItem(itemButton);
            addComponent(itemIcon);
            playerSlotIcons.add(itemIcon);

            //Move offset to next row if required.
            if (i == 8) {
                yOffset -= (3 * GUIComponentButton.ITEM_BUTTON_SIZE + 4);
            } else if (i == 17 || i == 26) {
                yOffset += GUIComponentButton.ITEM_BUTTON_SIZE;
            }
        }

        //Clear intractable slots.
        interactableSlotButtons.clear();
        interactableSlotIcons.clear();
    }

    @Override
    public void setStates() {
        super.setStates();
        //Set player item icons to player inventory.
        for (int i = 0; i < playerSlotButtons.size(); ++i) {
            IWrapperItemStack stack = playerInventory.getStack(i);
            playerSlotButtons.get(i).enabled = !stack.isEmpty();
            playerSlotIcons.get(i).stack = stack;
        }
    }

    protected abstract void handlePlayerItemClick(int slotClicked);

    protected int getPlayerInventoryOffset() {
        return 197;
    }

    @Override
    public int getWidth() {
        return 194;
    }

    @Override
    public int getHeight() {
        return 221;
    }

    @Override
    protected String getTexture() {
        return texture;
    }
}
