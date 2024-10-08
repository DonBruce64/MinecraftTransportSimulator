package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.AEntityCrafter;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.jsondefs.JSONPart.CrafterComponentType;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;

/**
 * A GUI that is used to interface with crafter entities.   Displays the player's items on 
 * the bottom, and the crafter information in the top.  Works a bit differently than the MC 
 * GUIs, as it doesn't support item dragging or movement.  Rather, crafter fuel is defined and
 * clicked to be put in the fuel slot, with other acceptable items defined for other slots.
 *
 * @author don_bruce
 */
public class AGUICrafter extends AGUIInventory {

    private GUIComponentCutout fuelIcon;
    private GUIComponentCutout craftingProgress;

    private final AEntityCrafter crafter;
    private final int[] itemSlotParams;
    private final int[] fuelLevelParams;
    private final int[] progressLevelParams;

    public AGUICrafter(AEntityCrafter crafter, String texture, int[] itemSlotParams, int[] fuelLevelParams, int[] progressLevelParams) {
        super(texture);
        this.crafter = crafter;
        this.itemSlotParams = itemSlotParams;
        this.fuelLevelParams = fuelLevelParams;
        this.progressLevelParams = progressLevelParams;
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        interactableSlotButtons.clear();
        interactableSlotIcons.clear();

        //Create item slots.
        for (int i = 0; i < itemSlotParams.length / 2; ++i) {
            GUIComponentButton itemButton = new GUIComponentButton(this, guiLeft + itemSlotParams[i * 2], guiTop + itemSlotParams[i * 2 + 1], false) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(crafter, player, interactableSlotButtons.indexOf(this), -1, false));
                }
            };
            addComponent(itemButton);
            interactableSlotButtons.add(itemButton);

            GUIComponentItem itemIcon = new GUIComponentItem(itemButton);
            addComponent(itemIcon);
            interactableSlotIcons.add(itemIcon);

            if (i == 0 && crafter.definition.crafterType != CrafterComponentType.STANDARD) {
                //Don't display fuel item slot if we don't use fuel items.
                itemButton.visible = false;
            }
        }

        //Add the section for the backplate that displays the current fuel type.
        int backplaneOffset = 36;
        switch (crafter.definition.crafterType) {
            case STANDARD:
                break;
            case FUEL:
                backplaneOffset += 18;
                break;
            case ELECTRIC:
                backplaneOffset += 36;
                break;
        }
        addComponent(new GUIComponentCutout(this, guiLeft + itemSlotParams[0], guiTop + itemSlotParams[1], 18, 18, 176, backplaneOffset));

        //Add the section that displays how much fuel the crafter has.
        addComponent(this.fuelIcon = new GUIComponentCutout(this, guiLeft + fuelLevelParams[0], guiTop + fuelLevelParams[1], fuelLevelParams[2], fuelLevelParams[3], fuelLevelParams[4], fuelLevelParams[5]));

        //Add the arrow section that displays how far along the smelting operation is.
        addComponent(this.craftingProgress = new GUIComponentCutout(this, guiLeft + progressLevelParams[0], guiTop + progressLevelParams[1], progressLevelParams[2], progressLevelParams[3], progressLevelParams[4], progressLevelParams[5]));
    }

    @Override
    public void setStates() {
        super.setStates();
        //Set other item icons to other inventory.
        for (int i = 0; i < interactableSlotButtons.size(); ++i) {
            IWrapperItemStack stack = crafter.getStack(i);
            interactableSlotButtons.get(i).enabled = !stack.isEmpty();
            interactableSlotIcons.get(i).stack = stack;
        }

        //Set fuel state.
        fuelIcon.visible = crafter.ticksLeftOfFuel > 0;
        if (fuelIcon.visible) {
            if (fuelLevelParams[6] == 0) {
                int pixelsWide = (int) (fuelLevelParams[2] * ((double) (crafter.ticksLeftOfFuel) / crafter.ticksFuelProvides));
                fuelIcon.width = pixelsWide;
                fuelIcon.textureSectionWidth = pixelsWide;
            } else {
                int pixelsHigh = (int) Math.ceil(fuelLevelParams[3] * ((double) (crafter.ticksLeftOfFuel) / crafter.ticksFuelProvides));
                fuelIcon.position.y = -(fuelIcon.constructedY + fuelLevelParams[3] - pixelsHigh);
                fuelIcon.height = pixelsHigh;
                fuelIcon.textureSectionHeight = pixelsHigh;
                fuelIcon.textureYOffset = fuelLevelParams[5] + fuelLevelParams[3] - pixelsHigh;
            }
        }

        //Set crafting state.
        if (crafter.ticksNeededToCraft > 0 && crafter.ticksLeftToCraft > 0) {
            if (progressLevelParams[6] == 0) {
                int pixelsWide = (int) (progressLevelParams[2] * ((double) (crafter.ticksNeededToCraft - crafter.ticksLeftToCraft) / crafter.ticksNeededToCraft));
                craftingProgress.width = pixelsWide;
                craftingProgress.textureSectionWidth = pixelsWide;
            } else {
                int pixelsHigh = (int) Math.ceil(progressLevelParams[3] * ((double) (crafter.ticksNeededToCraft - crafter.ticksLeftToCraft) / crafter.ticksNeededToCraft));
                craftingProgress.height = pixelsHigh;
                craftingProgress.textureSectionHeight = pixelsHigh;
            }
            craftingProgress.visible = true;
        } else {
            craftingProgress.visible = false;
        }
    }

    @Override
    protected void handlePlayerItemClick(int slotClicked) {
        InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(crafter, player, -1, slotClicked, false));
    }

    @Override
    protected int getPlayerInventoryOffset() {
        return 142;
    }

    @Override
    protected boolean canStayOpen() {
        return super.canStayOpen() && crafter.isValid;
    }

    @Override
    public int getWidth() {
        return 176;
    }

    @Override
    public int getHeight() {
        return 166;
    }
}
