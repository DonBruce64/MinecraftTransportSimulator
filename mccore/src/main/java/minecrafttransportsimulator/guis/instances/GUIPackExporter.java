package minecrafttransportsimulator.guis.instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponent3DModel;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.PackParser;

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
                File jsonDir = new File(InterfaceManager.gameDirectory, "mts_dev");
                if (!jsonDir.exists()) {
                    if (!jsonDir.mkdir()) {
                        debug.setText("ERROR: Could not create dev folder: " + jsonDir.getAbsolutePath() + "\nIs this location write-protected?");
                        return;
                    }
                }

                File lastModifiedFile = new File(jsonDir, "lastexported.txt");
                if (lastModifiedFile.exists()) {
                    debug.setText("\nWARNING: Existing export detected!  Exporting will not continue.  Either delete the mts_dev folder, or the lastexported.txt file and try again.");
                    return;
                }

                long lastTimeModified = 0;
                debug.setText("Export dir is: " + jsonDir.getAbsolutePath());
                for (String packID : PackParser.getAllPackIDs()) {
                    File packDir = new File(jsonDir, packID);
                    if (!packDir.exists()) {
                        if (!packDir.mkdir()) {
                            debug.setText("ERROR: Could not create pack folder: " + packDir.getAbsolutePath() + "\nIs this location write-protected?");
                            return;
                        }
                    }
                    for (AItemPack<?> packItem : PackParser.getAllItemsForPack(packID, false)) {
                        try {
                            File jsonFile = new File(packDir, packItem.definition.classification.toDirectory() + packItem.definition.prefixFolders);
                            jsonFile.mkdirs();
                            jsonFile = new File(jsonFile, packItem.definition.systemName + ".json");
                            JSONParser.exportStream(packItem.definition, Files.newOutputStream(jsonFile.toPath()));
                            lastTimeModified = jsonFile.lastModified();
                        } catch (IOException e) {
                            e.printStackTrace();
                            debug.setText("ERROR: Could not save pack definition to disk.  Error is:\n" + e.getMessage());
                            return;
                        }
                    }
                    debug.setText(debug.getText() + "\nExported pack: " + packID);
                }

                try {
                    FileWriter writer = new FileWriter(lastModifiedFile);
                    writer.write(String.valueOf(lastTimeModified));
                    writer.flush();
                    writer.close();
                    debug.setText(debug.getText() + "\nExporting finished.");
                } catch (IOException e) {
                    debug.setText("\nERROR: Could not save last modified timestamp to disk.  Error is:\n" + e.getMessage());
                    return;
                }
            }
        });
        addComponent(packImportButton = new GUIComponentButton(guiLeft + buttonWidth + buttonOffset, guiTop, buttonWidth, 20, "IMPORT PACKS") {
            @Override
            public void onClicked(boolean leftSide) {
                File jsonDir = new File(InterfaceManager.gameDirectory, "mts_dev");
                if (jsonDir.exists()) {
                    debug.setText("Import dir is: " + jsonDir.getAbsolutePath());
                    File lastModifiedFile = new File(jsonDir, "lastexported.txt");
                    if (lastModifiedFile.exists()) {
                        long lastTimeModified;
                        try {
                            FileReader reader = new FileReader(lastModifiedFile);
                            BufferedReader buffer = new BufferedReader(reader);
                            lastTimeModified = Long.parseLong(buffer.readLine());
                            buffer.close();
                        } catch (Exception e) {
                            debug.setText("\nERROR: Could not read last modified timestamp from disk.  Error is:\n" + e.getMessage());
                            return;
                        }

                        Set<File> parsedFiles = new HashSet<>();
                        for (String packID : PackParser.getAllPackIDs()) {
                            File packDir = new File(jsonDir, packID);
                            if (packDir.exists()) {
                                for (AItemPack<?> packItem : PackParser.getAllItemsForPack(packID, false)) {
                                    File jsonFile = new File(packDir, packItem.definition.classification.toDirectory() + packItem.definition.prefixFolders + packItem.definition.systemName + ".json");
                                    if (!parsedFiles.contains(jsonFile)) {
                                        if (jsonFile.lastModified() > lastTimeModified) {
                                            debug.setText(debug.getText() + JSONParser.hotloadJSON(jsonFile, packItem.definition));
                                        }
                                        parsedFiles.add(jsonFile);
                                    }
                                }
                            }
                        }
                        debug.setText(debug.getText() + "\nImporting finished.");
                    } else {
                        debug.setText("ERROR: No last modified timestamp file found at location: " + lastModifiedFile.getAbsolutePath() + "\nPlease re-export your pack data.");
                    }
                } else {
                    debug.setText("ERROR: Could not find dev folder: " + jsonDir.getAbsolutePath());
                }
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
            GUIComponentLabel dataEntryLabel = new GUIComponentLabel(guiLeft + 15, dataEntryBox.constructedY, ColorRGB.WHITE, "").setBox(dataEntryBox);
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
