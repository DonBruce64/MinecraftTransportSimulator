package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityInstrumentChange;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;

/**
 * A GUI that is used to put instruments into vehicles.  This GUI is essentially an overlay
 * to {@link GUIHUD} and {@link AGUIPanel} that uses the textures from those GUIs, but does
 * custom rendering over them rather than the usual rendering routines.  This prevents players
 * from messing with the panel while adding instruments, as well as easier tracking of the
 * spots where blank instruments are located (normally those aren't saved in variables).
 *
 * @author don_bruce
 */
public class GUIInstruments extends AGUIBase {

    //GUIs components created at opening.
    private final EntityVehicleF_Physics vehicle;
    private final IWrapperPlayer player;
    private final TreeMap<String, List<ItemInstrument>> playerInstruments = new TreeMap<>();

    //Runtime variables.
    private GUIComponentButton prevPackButton;
    private GUIComponentButton nextPackButton;
    private GUIComponentButton clearButton;
    private static String currentPack;
    private GUIComponentLabel packName;

    private boolean hudSelected = true;
    private GUIComponentButton hudButton;
    private GUIComponentButton panelButton;
    private GUIComponentLabel infoLabel;
    private AEntityE_Interactable<?> selectedEntity;
    private JSONInstrumentDefinition selectedInstrumentDefinition;

    private final List<GUIComponentButton> instrumentSlots = new ArrayList<>();
    private final List<GUIComponentItem> instrumentSlotIcons = new ArrayList<>();
    private final Map<AEntityE_Interactable<?>, List<InstrumentSlotBlock>> entityInstrumentBlocks = new HashMap<>();

    public GUIInstruments(EntityVehicleF_Physics vehicle) {
        super();
        this.vehicle = vehicle;
        this.player = InterfaceManager.clientInterface.getClientPlayer();
    }

    @Override
    public void setupComponents() {
        super.setupComponents();

        //Create the prior and next pack buttons.
        addComponent(prevPackButton = new GUIComponentButton(guiLeft, guiTop - 74, 20, 20, "<", true, ColorRGB.WHITE, false) {
            @Override
            public void onClicked(boolean leftSide) {
                currentPack = playerInstruments.lowerKey(currentPack);
            }
        });
        addComponent(nextPackButton = new GUIComponentButton(guiLeft, guiTop - 52, 20, 20, ">", true, ColorRGB.WHITE, false) {
            @Override
            public void onClicked(boolean leftSide) {
                currentPack = playerInstruments.higherKey(currentPack);
            }
        });

        //Create the player instrument buttons and icons.  This is a static list of 20 slots, though not all may be rendered.
        //That depends if there are that many instruments present for the currentPack.
        instrumentSlots.clear();
        instrumentSlotIcons.clear();
        for (byte i = 0; i < 36; ++i) {
            GUIComponentButton instrumentButton = new GUIComponentButton(guiLeft + 23 + GUIComponentButton.ITEM_BUTTON_SIZE * (i / 2), guiTop - 75 + GUIComponentButton.ITEM_BUTTON_SIZE * (i % 2), false) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityInstrumentChange(selectedEntity, player, selectedEntity.definition.instruments.indexOf(selectedInstrumentDefinition), playerInstruments.get(currentPack).get(instrumentSlots.indexOf(this))));
                    selectedEntity = null;
                    selectedInstrumentDefinition = null;
                }
            };
            addComponent(instrumentButton);
            instrumentSlots.add(instrumentButton);

            GUIComponentItem instrumentItem = new GUIComponentItem(instrumentButton);
            addComponent(instrumentItem);
            instrumentSlotIcons.add(instrumentItem);
        }

        //Create the pack name label.
        addComponent(packName = new GUIComponentLabel(guiLeft + 40, guiTop - 85, ColorRGB.WHITE, ""));

        //Create the clear button and background.
        addComponent(clearButton = new GUIComponentButton(guiLeft + getWidth() - 2 * GUIComponentButton.ITEM_BUTTON_SIZE, guiTop - 75, 2 * GUIComponentButton.ITEM_BUTTON_SIZE, 2 * GUIComponentButton.ITEM_BUTTON_SIZE, JSONConfigLanguage.GUI_INSTRUMENTS_CLEAR.value, true, ColorRGB.WHITE, false) {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityInstrumentChange(selectedEntity, player, selectedEntity.definition.instruments.indexOf(selectedInstrumentDefinition), null));
                selectedEntity = null;
                selectedInstrumentDefinition = null;
            }
        });
        addComponent(new GUIComponentCutout(clearButton.constructedX, clearButton.constructedY, clearButton.width, clearButton.height, 448, 0, 64, 64));

        //Create the HUD selection button.
        addComponent(hudButton = new GUIComponentButton(guiLeft, guiTop - 20, 100, 20, JSONConfigLanguage.GUI_INSTRUMENTS_MAIN.value, true, ColorRGB.WHITE, false) {
            @Override
            public void onClicked(boolean leftSide) {
                hudSelected = true;
                selectedEntity = null;
                selectedInstrumentDefinition = null;
                setupComponents();
            }
        });

        //Create the panel selection button.
        addComponent(panelButton = new GUIComponentButton(guiLeft + getWidth() - 100, guiTop - 20, 100, 20, JSONConfigLanguage.GUI_INSTRUMENTS_PANEL.value, true, ColorRGB.WHITE, false) {
            @Override
            public void onClicked(boolean leftSide) {
                hudSelected = false;
                selectedEntity = null;
                selectedInstrumentDefinition = null;
                setupComponents();
            }
        });

        //Create the info label.
        addComponent(infoLabel = new GUIComponentLabel(guiLeft + getWidth() / 2, guiTop - 20, ColorRGB.WHITE, "", TextAlignment.CENTERED, 1.0F));

        //Get all entities with instruments and adds them to the list. definitions, and add them to a map-list.
        //These come from the vehicle and all parts.
        List<AEntityE_Interactable<?>> entitiesWithInstruments = new ArrayList<>();
        if (vehicle.definition.instruments != null) {
            entitiesWithInstruments.add(vehicle);
        }
        for (APart part : vehicle.parts) {
            if (part.definition.instruments != null) {
                entitiesWithInstruments.add(part);
            }
        }

        //Create the slots.
        //We need one for every instrument, present or not, as we can click on any instrument.
        //This allows us to render instruments as they are added or removed.
        entityInstrumentBlocks.clear();
        for (AEntityE_Interactable<?> entity : entitiesWithInstruments) {
            List<InstrumentSlotBlock> instrumentBlocks = new ArrayList<>();
            for (JSONInstrumentDefinition packInstrument : entity.definition.instruments) {
                if (hudSelected ^ packInstrument.placeOnPanel) {
                    instrumentBlocks.add(new InstrumentSlotBlock(guiLeft, guiTop, entity, packInstrument));
                }
            }
            entityInstrumentBlocks.put(entity, instrumentBlocks);
        }
    }

    @Override
    public void setStates() {
        super.setStates();

        //Set pack prior and pack next buttons depending if we have such packs.
        prevPackButton.visible = playerInstruments.lowerKey(currentPack) != null;
        nextPackButton.visible = playerInstruments.higherKey(currentPack) != null;

        //Add all packs that have instruments in them.
        //This depends on if the player has the instruments, or if they are in creative.
        playerInstruments.clear();
        String firstPackSeen = null;
        for (String packID : PackParser.getAllPackIDs()) {
            for (AItemPack<?> packItem : PackParser.getAllItemsForPack(packID, true)) {
                if (packItem instanceof ItemInstrument) {
                    if (player.isCreative() || player.getInventory().getSlotForStack(packItem.getNewStack(null)) != -1) {
                        //Add the instrument to the list of instruments the player has.
                        if (!playerInstruments.containsKey(packID)) {
                            playerInstruments.put(packID, new ArrayList<>());
                            if (firstPackSeen == null) {
                                firstPackSeen = packID;
                            }
                        }
                        playerInstruments.get(packID).add((ItemInstrument) packItem);
                    }
                }
            }
        }

        //Set current pack if we don't have it or its invalid.
        if (currentPack == null || playerInstruments.get(currentPack) == null) {
            currentPack = firstPackSeen;
        }

        //Set instrument icon and button states depending on which instruments the player has.
        if (currentPack != null) {
            packName.text = PackParser.getPackConfiguration(currentPack).packName;
        }
        for (int i = 0; i < instrumentSlots.size(); ++i) {
            if (currentPack != null && playerInstruments.get(currentPack).size() > i) {
                instrumentSlots.get(i).visible = true;
                instrumentSlots.get(i).enabled = selectedInstrumentDefinition != null;
                instrumentSlotIcons.get(i).stack = playerInstruments.get(currentPack).get(i).getNewStack(null);
            } else {
                instrumentSlots.get(i).visible = false;
                instrumentSlotIcons.get(i).stack = null;
            }
        }

        //Set entity instrument states.
        for (AEntityE_Interactable<?> entity : entityInstrumentBlocks.keySet()) {
            for (InstrumentSlotBlock block : entityInstrumentBlocks.get(entity)) {
                block.selectorOverlay.visible = entity.equals(selectedEntity) && block.definition.equals(selectedInstrumentDefinition) && inClockPeriod(40, 20);
                block.instrument.visible = !block.selectorOverlay.visible && entity.instruments.get(block.instrument.slot) != null;
                block.blank.visible = !block.selectorOverlay.visible && !block.instrument.visible;
            }
        }

        //Set buttons depending on which vehicle section is selected.
        hudButton.enabled = !hudSelected;
        panelButton.enabled = hudSelected;

        //Set info and clear state based on if we've clicked an instrument.
        infoLabel.text = selectedInstrumentDefinition == null ? "\\/  " + JSONConfigLanguage.GUI_INSTRUMENTS_IDLE.value + "  \\/" : "/\\  " + JSONConfigLanguage.GUI_INSTRUMENTS_DECIDE.value + "  /\\";
        clearButton.enabled = selectedInstrumentDefinition != null && selectedEntity.instruments.get(selectedEntity.definition.instruments.indexOf(selectedInstrumentDefinition)) != null;
    }

    @Override
    protected EntityVehicleF_Physics getGUILightSource() {
        return vehicle;
    }

    @Override
    public int getWidth() {
        return AGUIPanel.PANEL_WIDTH;
    }

    @Override
    public int getHeight() {
        return AGUIPanel.PANEL_HEIGHT;
    }

    @Override
    public boolean renderFlushBottom() {
        return true;
    }

    @Override
    protected String getTexture() {
        if (hudSelected) {
            return vehicle.definition.motorized.hudTexture != null ? vehicle.definition.motorized.hudTexture : "mts:textures/guis/hud.png";
        } else {
            return vehicle.definition.motorized.panelTexture != null ? vehicle.definition.motorized.panelTexture : "mts:textures/guis/panel.png";
        }
    }

    private class InstrumentSlotBlock {
        private final JSONInstrumentDefinition definition;
        private final GUIComponentInstrument instrument;
        @SuppressWarnings("unused") //We use this, the complier is just too dumb to realize it.
        private final GUIComponentButton button;
        private final GUIComponentCutout blank;
        private final GUIComponentCutout selectorOverlay;

        private InstrumentSlotBlock(int guiLeft, int guiTop, AEntityE_Interactable<?> entity, JSONInstrumentDefinition definition) {
            this.definition = definition;
            int instrumentRadius = (int) (64F * definition.hudScale);
            addComponent(this.instrument = new GUIComponentInstrument(guiLeft, guiTop, entity, entity.definition.instruments.indexOf(definition)));
            addComponent(this.button = new GUIComponentButton(guiLeft + definition.hudX - instrumentRadius, guiTop + definition.hudY - instrumentRadius, 2 * instrumentRadius, 2 * instrumentRadius) {
                @Override
                public void onClicked(boolean leftSide) {
                    selectedEntity = entity;
                    selectedInstrumentDefinition = definition;
                }
            });
            addComponent(this.blank = new GUIComponentCutout(guiLeft + definition.hudX - instrumentRadius, guiTop + definition.hudY - instrumentRadius, 2 * instrumentRadius, 2 * instrumentRadius, 448, 0, 64, 64));
            addComponent(this.selectorOverlay = new GUIComponentCutout(guiLeft + definition.hudX - instrumentRadius, guiTop + definition.hudY - instrumentRadius, 2 * instrumentRadius, 2 * instrumentRadius, 448, 64, 64, 64));
        }
    }
}
