package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentOBJModel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.rendering.instances.RenderVehicle;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**This GUI is normally locked, and is only available in devMode.  It allows
 * pack importing and exporting to and from files.  The idea is that pack authors 
 * can use it to export the JSON that was loaded, edit it, and then import it again
 * and see the effects without rebooting the game.
 * 
 * @author don_bruce
 */
public class GUIPackExporter extends AGUIBase{
	
	//Main screen components.
	private GUIComponentButton modelRenderButton;
	private GUIComponentButton packExportButton;
	private GUIComponentButton packImportButton;
	private GUIComponentTextBox debug;
	
	//Model render screen components.
	private final EntityVehicleF_Physics vehicleClicked;
	private List<GUIComponentTextBox> dataEntryBoxes = new ArrayList<GUIComponentTextBox>();
	private List<GUIComponentLabel> dataEntryLabels = new ArrayList<GUIComponentLabel>();
	private GUIComponentButton backButton;
	private GUIComponentButton confirmButton;
	private GUIComponentOBJModel componentItemModel;
	
	public GUIPackExporter(EntityVehicleF_Physics vehicleClicked){
		this.vehicleClicked = vehicleClicked;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		int buttonWidth = getWidth()/3;
		addButton(packExportButton = new GUIComponentButton(guiLeft , guiTop + 0, buttonWidth, "EXPORT PACKS", 20, true){
			@Override
			public void onClicked(){
				File jsonDir = new File(MasterLoader.gameDirectory, "mts_dev");
				if(!jsonDir.exists()){
					if(!jsonDir.mkdir()){
						debug.setText("ERROR: Could not create dev folder: " + jsonDir.getAbsolutePath() + "\nIs this location write-protected?");
						return;
					}
				}
				
				long lastTimeModified = 0;
				debug.setText("Export dir is: " + jsonDir.getAbsolutePath());
				for(String packID : PackParserSystem.getAllPackIDs()){
					File packDir = new File(jsonDir, packID);
					if(!packDir.exists()){
						if(!packDir.mkdir()){
							debug.setText("ERROR: Could not create pack folder: " + packDir.getAbsolutePath() + "\nIs this location write-protected?");
							return;
						}	
					}
					for(AItemPack<?> packItem : PackParserSystem.getAllItemsForPack(packID)){
						try{
							File jsonFile;
							if(packItem.definition instanceof JSONVehicle){
								jsonFile = new File(packDir, "vehicle_" + packItem.definition.systemName + ".json");
							}else if(packItem.definition instanceof JSONPart){
								jsonFile = new File(packDir, "part_" + packItem.definition.systemName + ".json");
							}else if(packItem.definition instanceof JSONInstrument){
								jsonFile = new File(packDir, "instrument_" + packItem.definition.systemName + ".json");
							}else if(packItem.definition instanceof JSONPoleComponent){
								jsonFile = new File(packDir, "polesign_" + packItem.definition.systemName + ".json");
							}else{
								continue;
							}
							FileWriter writer = new FileWriter(jsonFile);
							JSONParser.exportStream(packItem.definition, writer);
							lastTimeModified = jsonFile.lastModified();
							writer.flush();
							writer.close();
						}catch (IOException e){
							debug.setText("ERROR: Could not save pack definition to disk.  Error is:\n" + e.getMessage());
							return;
						}
					}
					debug.setText(debug.getText() + "\nExported pack: " + packID);
				}
				try{
					File lastModifiedFile = new File(jsonDir, "lastexported.txt");
					FileWriter writer = new FileWriter(lastModifiedFile);
					writer.write(String.valueOf(lastTimeModified));
					writer.flush();
					writer.close();
					debug.setText(debug.getText() + "\nExporting finished.");
				}catch(IOException e){
					debug.setText("\nERROR: Could not save last modified timestamp to disk.  Error is:\n" + e.getMessage());
					return;
				}
			}
		});
		addButton(packImportButton = new GUIComponentButton(guiLeft + buttonWidth, guiTop + 0, buttonWidth, "IMPORT PACKS", 20, true){
			@Override
			public void onClicked(){
				File jsonDir = new File(MasterLoader.gameDirectory, "mts_dev");
				if(jsonDir.exists()){
					debug.setText("Import dir is: " + jsonDir.getAbsolutePath());
					File lastModifiedFile = new File(jsonDir, "lastexported.txt");
					if(lastModifiedFile.exists()){
						long lastTimeModified;
						try{
							FileReader reader = new FileReader(lastModifiedFile);
							BufferedReader buffer = new BufferedReader(reader);
							lastTimeModified = Long.valueOf(buffer.readLine());
							buffer.close();
						}catch(Exception e){
							debug.setText("\nERROR: Could not read last modified timestamp from disk.  Error is:\n" + e.getMessage());
							return;
						}
						
						for(String packID : PackParserSystem.getAllPackIDs()){
							File packDir = new File(jsonDir, packID);
							if(packDir.exists()){
								debug.setText(debug.getText() + "\nChecking pack: " + packID);
								for(AItemPack<?> packItem : PackParserSystem.getAllItemsForPack(packID)){
									File jsonFile;
									if(packItem.definition instanceof JSONVehicle){
										jsonFile = new File(packDir, "vehicle_" + packItem.definition.systemName + ".json");
									}else if(packItem.definition instanceof JSONPart){
										jsonFile = new File(packDir, "part_" + packItem.definition.systemName + ".json");
									}else if(packItem.definition instanceof JSONInstrument){
										jsonFile = new File(packDir, "instrument_" + packItem.definition.systemName + ".json");
									}else if(packItem.definition instanceof JSONPoleComponent){
										jsonFile = new File(packDir, "polesign_" + packItem.definition.systemName + ".json");
									}else{
										continue;
									}
									if(jsonFile.lastModified() > lastTimeModified){
										debug.setText(debug.getText() + "\nImporting file: " + jsonFile.getName());
										try{
											if(packItem.definition instanceof JSONVehicle){
												JSONVehicle definition = (JSONVehicle) packItem.definition;
												JSONVehicle loadedDefinition = JSONParser.parseStream(new FileReader(jsonFile), JSONVehicle.class);
												definition.general = loadedDefinition.general;
												definition.definitions = loadedDefinition.definitions;
												definition.motorized = loadedDefinition.motorized;
												definition.parts = loadedDefinition.parts;
												definition.collision = loadedDefinition.collision;
												definition.doors = loadedDefinition.doors;
												definition.connections = loadedDefinition.connections;
												definition.rendering = loadedDefinition.rendering;
												RenderVehicle.clearVehicleCaches(definition);
												
											}else if(packItem.definition instanceof JSONPart){
												JSONPart definition = (JSONPart) packItem.definition;
												JSONPart loadedDefinition = JSONParser.parseStream(new FileReader(jsonFile), JSONPart.class);
												definition.general = loadedDefinition.general;
												definition.engine = loadedDefinition.engine;
												definition.ground = loadedDefinition.ground;
												definition.propeller = loadedDefinition.propeller;
												definition.seat = loadedDefinition.seat;
												definition.gun = loadedDefinition.gun;
												definition.bullet = loadedDefinition.bullet;
												definition.interactable = loadedDefinition.interactable;
												definition.effector = loadedDefinition.effector;
												definition.generic = loadedDefinition.generic;
												definition.subParts = loadedDefinition.subParts;
												definition.collision = loadedDefinition.collision;
												definition.doors = loadedDefinition.doors;
												definition.connections = loadedDefinition.connections;
												definition.rendering = loadedDefinition.rendering;
												RenderVehicle.clearPartCaches(definition);
												
											}else if(packItem.definition instanceof JSONInstrument){
												JSONInstrument definition = (JSONInstrument) packItem.definition;
												JSONInstrument loadedDefinition = JSONParser.parseStream(new FileReader(jsonFile), JSONInstrument.class);
												definition.general = loadedDefinition.general;
												definition.components = loadedDefinition.components;
												
											}else if(packItem.definition instanceof JSONPoleComponent){
												JSONPoleComponent definition = (JSONPoleComponent) packItem.definition;
												JSONPoleComponent loadedDefinition = JSONParser.parseStream(new FileReader(jsonFile), JSONPoleComponent.class);
												definition.general = loadedDefinition.general;
												
											}else{
												continue;
											}
										}catch(Exception e){
											//e.printStackTrace();
											debug.setText(debug.getText() + "\nERROR: " + e.getMessage());
										}
									}
								}
							}
						}
						debug.setText(debug.getText() + "\nImporting finished.");
					}else{
						debug.setText("ERROR: No last modified timestamp file found at location: " + lastModifiedFile.getAbsolutePath() + "\nPlease re-export your pack data.");
					}
				}else{
					debug.setText("ERROR: Could not find dev folder: " + jsonDir.getAbsolutePath());
				}
			}
		});
		//Add control buttons.
		addButton(modelRenderButton = new GUIComponentButton(guiLeft + 2*buttonWidth, guiTop + 0, buttonWidth, "MODEL RENDER", 20, true){
			@Override
			public void onClicked(){
				modelRenderButton.visible = false;
				packExportButton.visible = false;
				packImportButton.visible = false;
				debug.visible = false;
				
				componentItemModel.visible = true;
				backButton.visible = true;
				confirmButton.visible = true;
				for(byte i=0; i<dataEntryBoxes.size(); ++i){
					dataEntryBoxes.get(i).visible = true; 
				}
			}
		});
		addButton(backButton = new GUIComponentButton(guiLeft + 20, guiTop + 140, 60, "BACK", 20, true){
			@Override
			public void onClicked(){
				modelRenderButton.visible = true;
				packExportButton.visible = true;
				packImportButton.visible = true;
				debug.visible = true;
				
				componentItemModel.visible = false;
				backButton.visible = false;
				confirmButton.visible = false;
				for(byte i=0; i<dataEntryBoxes.size(); ++i){
					dataEntryBoxes.get(i).visible = false; 
				}
			}
		});
		addButton(confirmButton = new GUIComponentButton(guiLeft + 100, guiTop + 140, 60, "CONFIRM", 20, true){
			@Override
			public void onClicked(){
				try{
					int dataEntryBoxIndex = 0;
					componentItemModel.modelLocation = String.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
					componentItemModel.textureLocation = String.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
					componentItemModel.x = Integer.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
					componentItemModel.y = Integer.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
					componentItemModel.scale = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				}catch(Exception e){}
			}
		});
		
		//Create debug output box.
		addTextBox(debug = new GUIComponentTextBox(guiLeft, guiTop + 20, getWidth(), "", getHeight() - 20, Color.WHITE, Color.BLACK, 1200));
		
		//Create data entry boxes and labels.
		dataEntryBoxes.clear();
		dataEntryLabels.clear();
		int currentRow = 15;
		for(byte i=0; i<5; ++i){
			int height = i < 2 ? 40 : 10;
			GUIComponentTextBox dataEntryBox = new GUIComponentTextBox(guiLeft + 100, guiTop + currentRow, 140, "", height, Color.WHITE, Color.BLACK, 100);
			GUIComponentLabel dataEntryLabel = new GUIComponentLabel(guiLeft + 15, dataEntryBox.y, Color.WHITE, "").setBox(dataEntryBox);
			dataEntryBoxes.add(dataEntryBox);
			dataEntryLabels.add(dataEntryLabel);
			addTextBox(dataEntryBox);
			addLabel(dataEntryLabel);
			currentRow += height + 1;
		}
		
		//Add item icon model component.
		componentItemModel = new GUIComponentOBJModel(guiLeft + 208, guiTop + 205, 1.0F, true, false, true);
		componentItemModel.scale = 6.0F;
		addOBJModel(componentItemModel);
		
		//Set label text and default entries.
		int labelBoxIndex = 0;
		dataEntryLabels.get(labelBoxIndex).text = "Model:";
		dataEntryBoxes.get(labelBoxIndex++).setText(vehicleClicked.definition.getModelLocation());
		dataEntryLabels.get(labelBoxIndex).text = "Texture:";
		dataEntryBoxes.get(labelBoxIndex++).setText(vehicleClicked.definition.getTextureLocation(vehicleClicked.currentSubName));
		dataEntryLabels.get(labelBoxIndex).text = "X-Pos (px):";
		dataEntryBoxes.get(labelBoxIndex++).setText(String.valueOf(componentItemModel.x));
		dataEntryLabels.get(labelBoxIndex).text = "Y-Pos (px):";
		dataEntryBoxes.get(labelBoxIndex++).setText(String.valueOf(componentItemModel.y));
		dataEntryLabels.get(labelBoxIndex).text = "Scale (1blk=1px):";
		dataEntryBoxes.get(labelBoxIndex++).setText(String.valueOf(componentItemModel.scale));
		
		//Click back button to set initial states.
		backButton.onClicked();
	}

	@Override
	public void setStates(){
		try{
			componentItemModel.x = Integer.valueOf(dataEntryBoxes.get(2).getText());
			componentItemModel.y = Integer.valueOf(dataEntryBoxes.get(3).getText());
			componentItemModel.scale = Float.valueOf(dataEntryBoxes.get(4).getText());
		}catch(Exception e){
			
		}
	}
	
	@Override
	public boolean renderBackground(){
		return componentItemModel != null && componentItemModel.visible;
	}
	
	@Override
	public int getWidth(){
		return componentItemModel != null && componentItemModel.visible ? super.getWidth() : 350;
	}
}
