package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponent3DModel;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPackImport;
import minecrafttransportsimulator.packloading.JSONParser;

/**
 * This GUI is normally locked, and is only available in devMode.  It allows
 * pack importing and exporting to and from files.  The idea is that pack authors
 * can use it to export the JSON that was loaded, edit it, and then import it again
 * and see the effects without rebooting the game.
 *
 * @author don_bruce
 */
public class GUIPackExporter extends AGUIBase {

    //Main screen components.
    private GUIComponentButton modelRenderButton;
    private GUIComponentButton packExportButton;
    private GUIComponentButton packImportButton;
    private GUIComponentButton packEditorButton;
    private GUIComponentTextBox debug;

    //Model render screen components.
    private final EntityVehicleF_Physics vehicleClicked;
    private final List<GUIComponentTextBox> dataEntryBoxes = new ArrayList<>();
    private final List<GUIComponentLabel> dataEntryLabels = new ArrayList<>();
    private GUIComponentButton backButton;
    private GUIComponentButton confirmButton;
    private GUIComponent3DModel componentItemModel;

    public GUIPackExporter(EntityVehicleF_Physics vehicleClicked) {
        super();
        this.vehicleClicked = vehicleClicked;
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        int buttonWidth = 350 / 4;
        int buttonOffset = -(350 - getWidth()) / 2;
        addComponent(packExportButton = new GUIComponentButton(guiLeft + buttonOffset, guiTop, buttonWidth, 20, "EXPORT PACKS") {
            @Override
            public void onClicked(boolean leftSide) {
                debug.setText(JSONParser.exportAllJSONs());
            }
        });
        addComponent(packImportButton = new GUIComponentButton(guiLeft + buttonWidth + buttonOffset, guiTop, buttonWidth, 20, "IMPORT PACKS") {
            @Override
            public void onClicked(boolean leftSide) {
                debug.setText(JSONParser.importAllJSONs());
                JSONParser.applyImports(vehicleClicked.world);
                InterfaceManager.packetInterface.sendToServer(new PacketPackImport());
            }
        });
        //Add control buttons.
        addComponent(modelRenderButton = new GUIComponentButton(guiLeft + 2 * buttonWidth + buttonOffset, guiTop, buttonWidth, 20, "MODEL RENDER") {
            @Override
            public void onClicked(boolean leftSide) {
                modelRenderButton.visible = false;
                packExportButton.visible = false;
                packImportButton.visible = false;
                packEditorButton.visible = false;
                debug.visible = false;

                componentItemModel.visible = true;
                backButton.visible = true;
                confirmButton.visible = true;
                for (GUIComponentTextBox dataEntryBox : dataEntryBoxes) {
                    dataEntryBox.visible = true;
                }
            }
        });
        addComponent(this.packEditorButton = new GUIComponentButton(guiLeft + 3 * buttonWidth + buttonOffset, guiTop, buttonWidth, 20, "PACK EDITOR") {
            @Override
            public void onClicked(boolean leftSide) {
                new GUIPackEditor();
            }
        });

        addComponent(backButton = new GUIComponentButton(guiLeft + 20, guiTop + 140, 60, 20, "BACK") {
            @Override
            public void onClicked(boolean leftSide) {
                modelRenderButton.visible = true;
                packExportButton.visible = true;
                packImportButton.visible = true;
                packEditorButton.visible = true;
                debug.visible = true;

                componentItemModel.visible = false;
                backButton.visible = false;
                confirmButton.visible = false;
                for (GUIComponentTextBox dataEntryBox : dataEntryBoxes) {
                    dataEntryBox.visible = false;
                }
            }
        });
        addComponent(confirmButton = new GUIComponentButton(guiLeft + 100, guiTop + 140, 60, 20, "CONFIRM") {
            @Override
            public void onClicked(boolean leftSide) {
                try {
                    int dataEntryBoxIndex = 0;
                    componentItemModel.modelLocation = String.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
                    componentItemModel.textureLocation = String.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
                    componentItemModel.position.x = componentItemModel.constructedX + Integer.parseInt(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
                    componentItemModel.position.y = -componentItemModel.constructedY - Integer.parseInt(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
                    componentItemModel.scale = Float.parseFloat(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
                } catch (Exception e) {
                }
            }
        });

        //Create debug output box.
        addComponent(debug = new GUIComponentTextBox(guiLeft + buttonOffset, guiTop + 20, 350, getHeight() - 20, "", ColorRGB.WHITE, 1200));

        //Create data entry boxes and labels.
        dataEntryBoxes.clear();
        dataEntryLabels.clear();
        int currentRow = 15;
        for (byte i = 0; i < 5; ++i) {
            int height = i < 2 ? 40 : 10;
            GUIComponentTextBox dataEntryBox = new GUIComponentTextBox(guiLeft + 100, guiTop + currentRow, 140, height, "", ColorRGB.WHITE, 100);
            GUIComponentLabel dataEntryLabel = new GUIComponentLabel(guiLeft + 15, dataEntryBox.constructedY, ColorRGB.WHITE, "").setComponent(dataEntryBox);
            dataEntryBoxes.add(dataEntryBox);
            dataEntryLabels.add(dataEntryLabel);
            addComponent(dataEntryBox);
            addComponent(dataEntryLabel);
            currentRow += height + 1;
        }

        //Add item icon model component.
        componentItemModel = new GUIComponent3DModel(guiLeft, guiTop, 1.0F, true, false, true);
        componentItemModel.position.add(208, -205, 0);
        componentItemModel.scale = 6.0F;
        addComponent(componentItemModel);

        //Set label text and default entries.
        int labelBoxIndex = 0;
        dataEntryLabels.get(labelBoxIndex).text = "Model:";
        dataEntryBoxes.get(labelBoxIndex++).setText(vehicleClicked.definition.getModelLocation(vehicleClicked.subDefinition));
        dataEntryLabels.get(labelBoxIndex).text = "Texture:";
        dataEntryBoxes.get(labelBoxIndex++).setText(vehicleClicked.definition.getTextureLocation(vehicleClicked.subDefinition));
        dataEntryLabels.get(labelBoxIndex).text = "X-Pos (px):";
        dataEntryBoxes.get(labelBoxIndex++).setText(String.valueOf((int) (componentItemModel.position.x) - componentItemModel.constructedX));
        dataEntryLabels.get(labelBoxIndex).text = "Y-Pos (px):";
        dataEntryBoxes.get(labelBoxIndex++).setText(String.valueOf((int) (-componentItemModel.position.y) + componentItemModel.constructedY));
        dataEntryLabels.get(labelBoxIndex).text = "Scale (1blk=1px):";
        dataEntryBoxes.get(labelBoxIndex++).setText(String.valueOf(componentItemModel.scale));

        //Click back button to set initial states.
        backButton.onClicked(false);
    }

    @Override
    public void setStates() {
        super.setStates();
        try {
            componentItemModel.position.x = componentItemModel.constructedX + Integer.parseInt(dataEntryBoxes.get(2).getText());
            componentItemModel.position.y = componentItemModel.constructedY - Integer.parseInt(dataEntryBoxes.get(3).getText());
            componentItemModel.scale = Float.parseFloat(dataEntryBoxes.get(4).getText());
        } catch (Exception e) {

        }
    }

    @Override
    protected boolean renderBackground() {
        return componentItemModel != null && componentItemModel.visible;
    }
}
