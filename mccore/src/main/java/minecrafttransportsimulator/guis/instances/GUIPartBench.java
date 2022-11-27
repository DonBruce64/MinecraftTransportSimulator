package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponent3DModel;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONCraftingBench;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerCraftItem;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;

/**
 * A GUI that is used to craft vehicle parts and other pack components.  This GUI displays
 * the items required to craft a vehicle, the item that will be crafted, and some properties
 * of that item.  Allows for scrolling via a scroll wheel, and remembers the last item that
 * was selected to allow for faster lookup next time the GUI is opened.
 *
 * @author don_bruce
 */
public class GUIPartBench extends AGUIBase {
    /*Last item this GUI was on when closed.  Keyed by definition instance to keep all benches in-sync.*/
    private static final Map<JSONCraftingBench, AItemPack<? extends AJSONItem>> lastOpenedItem = new HashMap<>();

    //Init variables.
    private final JSONCraftingBench definition;
    private final IWrapperPlayer player;

    //Buttons and labels.
    private GUIComponentButton prevPackButton;
    private GUIComponentButton nextPackButton;
    private GUIComponentLabel packName;

    private GUIComponentButton prevPartButton;
    private GUIComponentButton nextPartButton;
    private GUIComponentLabel partName;

    private GUIComponentButton prevColorButton;
    private GUIComponentButton nextColorButton;
    private GUIComponentButton nextRecipeButton;

    private GUIComponentLabel partInfo;
    private GUIComponentLabel vehicleInfo;
    private GUIComponentButton vehicleInfoButton;
    private GUIComponentButton vehicleDescriptionButton;
    private GUIComponentButton repairCraftingButton;
    private GUIComponentButton normalCraftingButton;
    private GUIComponentButton confirmButton;

    //Crafting components.
    private final List<GUIComponentItem> craftingItemIcons = new ArrayList<>();
    private final List<GUIComponentCutout> craftingItemBackgrounds = new ArrayList<>();
    private List<PackMaterialComponent> materials;

    //Renders for the item.
    private GUIComponentItem itemRender;
    private GUIComponent3DModel modelRender;

    //Runtime variables.
    private String prevPack;
    private String currentPack;
    private String nextPack;
    private boolean viewingRepair;

    private AItemPack<? extends AJSONItem> prevItem;
    private AItemPack<? extends AJSONItem> currentItem;
    private AItemPack<? extends AJSONItem> nextItem;
    private int recipeIndex;

    //Only used for vehicles.
    private AItemPack<? extends AJSONItem> prevSubItem;
    private AItemPack<? extends AJSONItem> nextSubItem;
    boolean displayVehicleInfo = false;

    public GUIPartBench(JSONCraftingBench definition) {
        super();
        this.definition = definition;
        this.player = InterfaceManager.clientInterface.getClientPlayer();
        if (lastOpenedItem.containsKey(definition)) {
            currentItem = lastOpenedItem.get(definition);
            currentPack = currentItem.definition.packID;
        } else {
            //Find a pack that has the item we are supposed to craft and set it.
            for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                if (packItem.isBenchValid(definition)) {
                    currentItem = packItem;
                    currentPack = packItem.definition.packID;
                    return;
                }
            }
        }
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        //Create pack navigation section.
        addComponent(prevPackButton = new GUIComponentButton(guiLeft + 17, guiTop + 11, 20, 20, 40, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                currentPack = prevPack;
                viewingRepair = false;
                currentItem = null;
                updateNames();
            }
        });
        addComponent(nextPackButton = new GUIComponentButton(guiLeft + 243, guiTop + 11, 20, 20, 60, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                currentPack = nextPack;
                viewingRepair = false;
                currentItem = null;
                updateNames();
            }
        });
        int centerBetweenButtons = prevPackButton.constructedX + prevPackButton.width + (nextPackButton.constructedX - (prevPackButton.constructedX + prevPackButton.width)) / 2;
        addComponent(packName = new GUIComponentLabel(centerBetweenButtons, guiTop + 16, ColorRGB.WHITE, "", TextAlignment.CENTERED, 1.0F));

        //Create part navigation section.
        addComponent(prevPartButton = new GUIComponentButton(prevPackButton.constructedX, prevPackButton.constructedY + prevPackButton.height, 20, 20, 40, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                currentItem = prevItem;
                viewingRepair = false;
                updateNames();
            }
        });
        addComponent(nextPartButton = new GUIComponentButton(nextPackButton.constructedX, nextPackButton.constructedY + nextPackButton.height, 20, 20, 60, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                currentItem = nextItem;
                viewingRepair = false;
                updateNames();
            }
        });
        addComponent(partName = new GUIComponentLabel(packName.constructedX, packName.constructedY + prevPackButton.height, ColorRGB.WHITE, "", TextAlignment.CENTERED, 0.75F));
        addComponent(partInfo = new GUIComponentLabel(guiLeft + 17, guiTop + 60, ColorRGB.WHITE, "", TextAlignment.LEFT_ALIGNED, 0.75F, 150));
        addComponent(vehicleInfo = new GUIComponentLabel(guiLeft + 17, guiTop + 60, ColorRGB.WHITE, "", TextAlignment.LEFT_ALIGNED, 1.0F, 150));

        //Create color navigation section.
        addComponent(prevColorButton = new GUIComponentButton(guiLeft + 175, guiTop + 131, 20, 15, 40, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                currentItem = prevSubItem;
                updateNames();
            }
        });
        addComponent(nextColorButton = new GUIComponentButton(guiLeft + 245, guiTop + 131, 20, 15, 60, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                currentItem = nextSubItem;
                updateNames();
            }
        });
        addComponent(new GUIComponentLabel(prevColorButton.constructedX + prevColorButton.width + (nextColorButton.constructedX - (prevColorButton.constructedX + prevColorButton.width)) / 2, guiTop + 136, ColorRGB.WHITE, JSONConfigLanguage.GUI_PART_BENCH_COLOR.value, TextAlignment.CENTERED, 1.0F).setButton(nextColorButton));

        //Create recipe selection button.
        addComponent(nextRecipeButton = new GUIComponentButton(guiLeft + 295, guiTop + 148, 20, 20, 180, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                if (++recipeIndex == currentItem.definition.general.materialLists.size()) {
                    recipeIndex = 0;
                }
                updateNames();
            }
        });

        //Create the crafting item slots.  14 16X16 slots (7X2) need to be made here.
        craftingItemIcons.clear();
        craftingItemBackgrounds.clear();
        for (byte i = 0; i < 7 * 2; ++i) {
            GUIComponentItem craftingItem = new GUIComponentItem(guiLeft + 276 + GUIComponentButton.ITEM_BUTTON_SIZE * (i / 7), guiTop + 20 + GUIComponentButton.ITEM_BUTTON_SIZE * (i % 7), 1.0F);
            GUIComponentCutout itemBackground = new GUIComponentCutout(craftingItem.constructedX, craftingItem.constructedY, craftingItem.width, craftingItem.height, 160, 236, 20, 20);
            itemBackground.visible = false;
            addComponent(craftingItem);
            addComponent(itemBackground);
            craftingItemIcons.add(craftingItem);
            craftingItemBackgrounds.add(itemBackground);
        }

        //Create both the item and OBJ renders.  We choose which to display later.
        addComponent(itemRender = new GUIComponentItem(guiLeft + 175, guiTop + 56, 5.625F));
        addComponent(modelRender = new GUIComponent3DModel(guiLeft + 220, guiTop + 101, 32.0F, true, true, false));

        //Create the info switching button.
        addComponent(vehicleInfoButton = new GUIComponentButton(guiLeft + 147, guiTop + 159, 20, 20, 100, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                displayVehicleInfo = true;
            }
        });
        addComponent(vehicleDescriptionButton = new GUIComponentButton(guiLeft + 147, guiTop + 159, 20, 20, 80, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                displayVehicleInfo = false;
            }
        });

        //Create the crafting switching button.
        addComponent(repairCraftingButton = new GUIComponentButton(guiLeft + 127, guiTop + 159, 20, 20, 120, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                viewingRepair = true;
                recipeIndex = 0;
                updateNames();
            }
        });
        addComponent(normalCraftingButton = new GUIComponentButton(guiLeft + 127, guiTop + 159, 20, 20, 140, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                viewingRepair = false;
                recipeIndex = 0;
                updateNames();
            }
        });

        //Create the confirm button.
        addComponent(confirmButton = new GUIComponentButton(guiLeft + 211, guiTop + 156, 20, 20, 20, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketPlayerCraftItem(player, currentItem, recipeIndex, viewingRepair));
            }
        });

        //Update the names now that we have everything put together.
        updateNames();
    }

    @Override
    public void setStates() {
        super.setStates();
        //Set buttons based on if we have prev or next items.
        prevPackButton.enabled = prevPack != null;
        nextPackButton.enabled = nextPack != null;
        prevPartButton.enabled = prevItem != null;
        nextPartButton.enabled = nextItem != null;
        prevColorButton.visible = currentItem instanceof AItemSubTyped;
        prevColorButton.enabled = prevSubItem != null;
        nextColorButton.visible = currentItem instanceof AItemSubTyped;
        nextColorButton.enabled = nextSubItem != null;

        //Enable next recipe button if we have multiple valid recipes.
        nextRecipeButton.enabled = currentItem != null && (viewingRepair ? currentItem.definition.general.repairMaterialLists.size() > 1 : currentItem.definition.general.materialLists.size() > 1);

        vehicleInfoButton.visible = currentItem instanceof ItemVehicle && !displayVehicleInfo;
        vehicleDescriptionButton.visible = currentItem instanceof ItemVehicle && displayVehicleInfo;
        repairCraftingButton.visible = !viewingRepair && currentItem != null && currentItem.definition.general.repairMaterialLists != null && !currentItem.definition.general.repairMaterialLists.isEmpty();
        normalCraftingButton.visible = viewingRepair;
        partInfo.visible = !displayVehicleInfo;
        vehicleInfo.visible = displayVehicleInfo;

        //Set materials.
        //Get the offset index based on the clock-time and the number of materials.
        if (materials != null) {
            int materialOffset = 1 + (materials.size() - 1) / craftingItemIcons.size();
            materialOffset = (int) (System.currentTimeMillis() % (materialOffset * 5000) / 5000);
            materialOffset *= craftingItemIcons.size();
            for (byte i = 0; i < craftingItemIcons.size(); ++i) {
                int materialIndex = i + materialOffset;
                if (materialIndex < materials.size()) {
                    craftingItemIcons.get(i).stacks = materials.get(materialIndex).possibleItems;
                    craftingItemBackgrounds.get(i).visible = !player.isCreative() && inClockPeriod(20, 10) && player.getInventory().hasSpecificMaterial(currentItem, recipeIndex, i, true, true, viewingRepair);
                } else {
                    craftingItemIcons.get(i).stacks = null;
                    craftingItemBackgrounds.get(i).visible = false;
                }
            }
        } else {
            craftingItemIcons.forEach(icon -> icon.stacks = null);
        }

        //Set confirm button based on if player has materials.
        confirmButton.enabled = currentItem != null && (player.isCreative() || (materials != null && player.getInventory().hasMaterials(materials)));

        //Check the mouse to see if it updated and we need to change items.
        int wheelMovement = InterfaceManager.inputInterface.getTrackedMouseWheel();
        if (wheelMovement < 0 && nextPartButton.enabled) {
            nextPartButton.onClicked(false);
        } else if (wheelMovement > 0 && prevPartButton.enabled) {
            prevPartButton.onClicked(false);
        }
    }

    @Override
    public int getWidth() {
        return 327;
    }

    @Override
    public int getHeight() {
        return 196;
    }

    @Override
    protected String getTexture() {
        return "mts:textures/guis/crafting.png";
    }

    /**
     * Loop responsible for updating pack/part names whenever an action occurs.
     * Looks through all items in the list that was passed-in on GUI construction time and
     * uses the order to determine which pack/item to scroll to when a button is clicked.
     * Sets the variables to be used on a button action, so once an action is performed this
     * logic MUST be called to update the button action states!
     */
    @SuppressWarnings("RedundantCast")
    private void updateNames() {
        //Get all pack indexes.
        List<String> packIDs = new ArrayList<>(PackParser.getAllPackIDs());
        int currentPackIndex = packIDs.indexOf(currentPack);

        //Loop forwards to find a pack that has the items we need and set that as the next pack.
        //Only set the pack if it has items in it that match our bench's parameters.
        nextPack = null;
        if (currentPackIndex < packIDs.size()) {
            for (int i = currentPackIndex + 1; i < packIDs.size() && nextPack == null; ++i) {
                for (AItemPack<?> packItem : PackParser.getAllItemsForPack(packIDs.get(i), true)) {
                    if (packItem.isBenchValid(definition)) {
                        nextPack = packIDs.get(i);
                        break;
                    }
                }
            }
        }

        //Loop backwards to find a pack that has the items we need and set that as the prev pack.
        //Only set the pack if it has items in it that match our bench's parameters.
        prevPack = null;
        if (currentPackIndex > 0) {
            for (int i = currentPackIndex - 1; i >= 0 && prevPack == null; --i) {
                for (AItemPack<?> packItem : PackParser.getAllItemsForPack(packIDs.get(i), true)) {
                    if (packItem.isBenchValid(definition)) {
                        prevPack = packIDs.get(i);
                        break;
                    }
                }
            }
        }

        //Set item indexes.
        //If we don't have a pack, it means we don't have any items that are for this bench, so we shouldn't do anything else.
        if (currentPack == null) {
            return;
        }
        List<AItemPack<?>> packItems = PackParser.getAllItemsForPack(currentPack, true);
        int currentItemIndex = packItems.indexOf(currentItem);
        //If currentItem is null, it means we switched packs and need to re-set it to the first item of the new pack.
        //Do so now before we do looping to prevent crashes.
        //Find a pack that has the item we are supposed to craft and set it.
        //If we are for a subTyped item, make sure to set the next subItem if we can.
        if (currentItem == null) {
            for (AItemPack<?> packItem : packItems) {
                if (currentItem == null || (currentItem.definition instanceof AJSONMultiModelProvider && nextSubItem == null)) {
                    if (packItem.isBenchValid(definition)) {
                        if (currentItem == null) {
                            currentItem = packItem;
                            currentItemIndex = packItems.indexOf(currentItem);
                        } else if (currentItem.definition instanceof AJSONMultiModelProvider && nextSubItem == null) {
                            if (packItem.definition.systemName.equals(currentItem.definition.systemName)) {
                                nextSubItem = packItem;
                            }
                        }
                    }
                }
            }
        }

        //Loop forwards in our pack to find the next item in that pack.
        //Only set the pack if it has items in it that match our bench's parameters.
        nextItem = null;
        nextSubItem = null;
        if (currentItemIndex < packItems.size()) {
            for (int i = currentItemIndex + 1; i < packItems.size() && nextItem == null; ++i) {
                if (packItems.get(i).isBenchValid(definition)) {
                    //If we are for subTyped item, and this item is the same sub-item classification, 
                    //set nextSubItem and continue on.
                    if (currentItem.definition instanceof AJSONMultiModelProvider) {
                        if (packItems.get(i).definition.systemName.equals(currentItem.definition.systemName)) {
                            if (nextSubItem == null) {
                                nextSubItem = packItems.get(i);
                            }
                            continue;
                        }
                    }
                    nextItem = packItems.get(i);
                    break;
                }
            }
        }

        //Loop backwards in our pack to find the prev item in that pack.
        //Only set the pack if it has items in it that match our bench's parameters.
        prevItem = null;
        prevSubItem = null;
        if (currentItemIndex > 0) {
            for (int i = currentItemIndex - 1; i >= 0 && (prevItem == null || currentItem.definition instanceof AJSONMultiModelProvider); --i) {
                if (packItems.get(i).isBenchValid(definition)) {
                    //If we are for a subTyped item, and we didn't switch items, and this item
                    //is the same sub-item classification, set prevSubItem and continue on.
                    //If we did switch, we want the first subItem in the set of items to
                    //be the prevItem we pick.  This ensures when we switch we'll be on the 
                    //same subItem each time we switch items.
                    if (currentItem.definition instanceof AJSONMultiModelProvider) {
                        if (packItems.get(i).definition.systemName.equals(currentItem.definition.systemName)) {
                            if (prevSubItem == null) {
                                prevSubItem = packItems.get(i);
                            }
                        } else {
                            if (prevItem == null) {
                                prevItem = packItems.get(i);
                            } else if (packItems.get(i).definition.systemName.equals(prevItem.definition.systemName)) {
                                prevItem = packItems.get(i);
                            }
                        }
                    } else {
                        prevItem = packItems.get(i);
                        break;
                    }
                }
            }
        }

        //All pack and part bits are now set and updated.  Update info labels and item icons.
        packName.text = PackParser.getPackConfiguration(currentPack).packName;
        partName.text = currentItem.getItemName();

        //Create part description text.
        List<String> descriptiveLines = new ArrayList<>();
        currentItem.addTooltipLines(descriptiveLines, InterfaceManager.coreInterface.getNewNBTWrapper());
        partInfo.text = "";
        for (String line : descriptiveLines) {
            partInfo.text += line + "\n";
        }
        //Create vehicle information text, if we are a vehicle item.
        if (currentItem instanceof ItemVehicle) {
            vehicleInfo.text = getVehicleInfoText();
        }

        //Parse crafting items and set icon items.
        int requestedRecipe = recipeIndex;
        String errorMessage = "";
        do {
            materials = PackMaterialComponent.parseFromJSON(currentItem, recipeIndex, true, true, viewingRepair);
            if (materials != null) {
                for (byte i = 0; i < craftingItemIcons.size(); ++i) {
                    if (i < materials.size()) {
                        craftingItemIcons.get(i).stacks = materials.get(i).possibleItems;
                    }
                }
            } else {
                craftingItemIcons.forEach(icon -> icon.stacks = null);
                if (++recipeIndex == (viewingRepair ? currentItem.definition.general.repairMaterialLists.size() : currentItem.definition.general.materialLists.size())) {
                    recipeIndex = 0;
                }
                errorMessage += PackMaterialComponent.lastErrorMessage + "\n";
                if (recipeIndex == requestedRecipe) {
                    partInfo.text = errorMessage;
                    break;
                }
            }
        } while (materials == null);

        //Enable render based on what component we have.
        boolean isPartWithVehicleTexture = currentItem instanceof AItemPart && ((AItemPart) currentItem).definition.generic.useVehicleTexture;
        boolean isPartWithBuiltinTexture = currentItem instanceof AItemPart && ((AItemPart) currentItem).definition.generic.benchTexture != null;
        if (currentItem instanceof AItemSubTyped && (!isPartWithVehicleTexture || isPartWithBuiltinTexture)) {
            modelRender.modelLocation = ((AItemSubTyped<?>) currentItem).definition.getModelLocation(((AItemSubTyped<?>) currentItem).subDefinition);
            modelRender.textureLocation = isPartWithBuiltinTexture ? PackResourceLoader.getPackResource(currentItem.definition, ResourceType.PNG, ((AItemPart) currentItem).definition.generic.benchTexture) : ((AItemSubTyped<?>) currentItem).definition.getTextureLocation(((AItemSubTyped<?>) currentItem).subDefinition);
            itemRender.stack = null;
            //Don't spin signs.  That gets annoying.
            modelRender.spin = !(currentItem.definition instanceof JSONPoleComponent && ((JSONPoleComponent) currentItem.definition).pole.type.equals(PoleComponentType.SIGN));
        } else {
            itemRender.stack = currentItem.getNewStack(null);
            modelRender.modelLocation = null;
        }

        //Now update the last saved item.
        lastOpenedItem.put(definition, currentItem);
    }

    private String getVehicleInfoText() {
        JSONVehicle vehicleDefinition = (JSONVehicle) currentItem.definition;
        int controllers = 0;
        int passengers = 0;
        int cargo = 0;
        int mixed = 0;
        float minFuelConsumption = 99;
        float maxFuelConsumption = 0;
        float minWheelSize = 99;
        float maxWheelSize = 0;

        //Get how many passengers and cargo this vehicle can hold.
        for (JSONPartDefinition part : vehicleDefinition.parts) {
            if (part.isController) {
                ++controllers;
            } else {
                boolean canAcceptSeat = false;
                boolean canAcceptCargo = false;
                if (part.types.contains("seat")) {
                    canAcceptSeat = true;
                }
                if (part.types.contains("crate") || part.types.contains("barrel")) {
                    canAcceptCargo = true;
                }
                if (canAcceptSeat && !canAcceptCargo) {
                    ++passengers;
                } else if (canAcceptCargo && !canAcceptSeat) {
                    ++cargo;
                } else if (canAcceptCargo && canAcceptSeat) {
                    ++mixed;
                }

                for (String partNameEntry : part.types) {
                    if (partNameEntry.startsWith("engine")) {
                        minFuelConsumption = Math.min(part.minValue, minFuelConsumption);
                        maxFuelConsumption = Math.max(part.maxValue, maxFuelConsumption);
                        break;
                    }
                }

                if (part.types.contains("wheel")) {
                    minWheelSize = Math.min(part.minValue, minWheelSize);
                    maxWheelSize = Math.max(part.maxValue, maxWheelSize);
                }
            }
        }

        //Combine translated header and info text together into a single string and return.
        String totalInformation = "";
        totalInformation += JSONConfigLanguage.GUI_PART_BENCH_WEIGHT.value + vehicleDefinition.motorized.emptyMass + "\n";
        totalInformation += JSONConfigLanguage.GUI_PART_BENCH_FUEL.value + vehicleDefinition.motorized.fuelCapacity + "\n";
        totalInformation += JSONConfigLanguage.GUI_PART_BENCH_CONTROLLERS.value + controllers + "\n";
        totalInformation += JSONConfigLanguage.GUI_PART_BENCH_PASSENGERS.value + passengers + "\n";
        totalInformation += JSONConfigLanguage.GUI_PART_BENCH_CARGO.value + cargo + "\n";
        totalInformation += JSONConfigLanguage.GUI_PART_BENCH_MIXED.value + mixed + "\n";
        if (minFuelConsumption != 99) {
            totalInformation += JSONConfigLanguage.GUI_PART_BENCH_ENGINE.value + minFuelConsumption + "-" + maxFuelConsumption + "\n";
        }
        if (minWheelSize != 99) {
            totalInformation += JSONConfigLanguage.GUI_PART_BENCH_WHEEL.value + minWheelSize + "-" + maxWheelSize + "\n";
        }
        return totalInformation;
    }
}
