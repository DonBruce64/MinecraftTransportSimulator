package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.entities.instances.EntityFurnace;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.jsondefs.JSONPart.FurnaceComponentType;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerItemTransfer;

/**
 * A GUI that is used to interface with furnaces.   Displays the player's items on the bottom,
 * and the furnace type/status in the top.  Works a bit differently than the MC GUIs, as it
 * doesn't support item dragging or movement.  Rather, furnace fuel is clicked to put it into
 * the furnace, and items are clicked to add them to the furnace and remove them when smelted.
 *
 * @author don_bruce
 */
public class GUIFurnace extends AGUIInventory {

    private GUIComponentCutout fuelIcon;
    private GUIComponentCutout smeltingProgress;

    private final EntityFurnace furnace;

    public GUIFurnace(EntityFurnace furnace, String texture) {
        super(texture != null ? texture : "mts:textures/guis/furnace.png");
        this.furnace = furnace;
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        //Create the two or three inventory slots.
        //The third is for fuel, which isn't present if we don't have that type of furnace.
        interactableSlotButtons.clear();
        interactableSlotIcons.clear();

        GUIComponentButton smeltingItemButton = new GUIComponentButton(guiLeft + 51, guiTop + 20, false) {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1, false));
            }
        };
        addComponent(smeltingItemButton);
        interactableSlotButtons.add(smeltingItemButton);

        GUIComponentItem smeltingItemIcon = new GUIComponentItem(smeltingItemButton);
        addComponent(smeltingItemIcon);
        interactableSlotIcons.add(smeltingItemIcon);

        GUIComponentButton smeltedItemButton = new GUIComponentButton(guiLeft + 110, guiTop + 21, false) {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1, false));
            }
        };
        addComponent(smeltedItemButton);
        interactableSlotButtons.add(smeltedItemButton);

        GUIComponentItem smeltedItemIcon = new GUIComponentItem(smeltedItemButton);
        addComponent(smeltedItemIcon);
        interactableSlotIcons.add(smeltedItemIcon);

        if (furnace.definition.furnaceType.equals(FurnaceComponentType.STANDARD)) {
            GUIComponentButton fuelItemButton = new GUIComponentButton(guiLeft + 79, guiTop + 53, false) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(furnace, player, interactableSlotButtons.indexOf(this), -1, false));
                }
            };
            addComponent(fuelItemButton);
            interactableSlotButtons.add(fuelItemButton);

            GUIComponentItem fuelItemIcon = new GUIComponentItem(fuelItemButton);
            addComponent(fuelItemIcon);
            interactableSlotIcons.add(fuelItemIcon);
        }

        //Add the section for the backplate that displays the current furnace type.
        int backplaneOffset = 0;
        switch (furnace.definition.furnaceType) {
            case STANDARD:
                backplaneOffset = 31;
                break;
            case FUEL:
                backplaneOffset = 49;
                break;
            case ELECTRIC:
                backplaneOffset = 67;
                break;
        }
        addComponent(new GUIComponentCutout(guiLeft + 61, guiTop + 53, 54, 18, 176, backplaneOffset));

        //Add the flame section that displays how much fuel the furnace has.
        addComponent(this.fuelIcon = new GUIComponentCutout(guiLeft + 81, guiTop + 38, 14, 14, 176, 0));

        //Add the arrow section that displays how far along the smelting operation is.
        addComponent(this.smeltingProgress = new GUIComponentCutout(guiLeft + 77, guiTop + 20, 24, 17, 176, 14));
    }

    @Override
    public void setStates() {
        super.setStates();
        //Set other item icons to other inventory.
        for (int i = 0; i < interactableSlotButtons.size(); ++i) {
            IWrapperItemStack stack = furnace.getStack(i);
            interactableSlotButtons.get(i).enabled = !stack.isEmpty();
            interactableSlotIcons.get(i).stack = stack;
        }

        //Set fuel state.
        fuelIcon.visible = furnace.ticksLeftOfFuel > 0;
        if (!furnace.definition.furnaceType.equals(FurnaceComponentType.ELECTRIC)) {
            int pixelsRemoved = (int) (fuelIcon.textureSectionHeight - fuelIcon.textureSectionHeight * ((double) furnace.ticksLeftOfFuel / furnace.ticksAddedOfFuel));
            //This could be over due to packet lag.
            if (pixelsRemoved < 0) {
                pixelsRemoved = 0;
            }
            fuelIcon.position.y = -fuelIcon.constructedY - pixelsRemoved;
            fuelIcon.height = 14 - pixelsRemoved;
            fuelIcon.textureYOffset = pixelsRemoved;
        }

        //Set smelting state.
        if (furnace.ticksNeededToSmelt > 0 && furnace.ticksLeftToSmelt > 0) {
            int pixelsRemoved = (int) (24 * ((double) furnace.ticksLeftToSmelt / furnace.ticksNeededToSmelt));
            smeltingProgress.width = 24 - pixelsRemoved;
            smeltingProgress.textureSectionWidth = 24 - pixelsRemoved;
            smeltingProgress.visible = true;
        } else {
            smeltingProgress.visible = false;
        }
    }

    @Override
    protected void handlePlayerItemClick(int slotClicked) {
        InterfaceManager.packetInterface.sendToServer(new PacketPlayerItemTransfer(furnace, player, -1, slotClicked, false));
    }

    @Override
    protected int getPlayerInventoryOffset() {
        return 142;
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
