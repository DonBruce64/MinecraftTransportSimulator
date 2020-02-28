package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;

/**This is a special GUI that is called in devMode to allow vehicle editing.
 * The idea is that pack authors can use it to modify their vehicle's JSON
 * in-game and see the effects without hotloading.
 * 
 * @author don_bruce
 */
public class GUIVehicleEditor extends AGUIBase{
	
	//Screen selctor buttons.
	private Map<EditScreen, GUIComponentButton> screenSelectorButtons = new HashMap<EditScreen, GUIComponentButton>();
	
	//Global components used to enter information on all screens.
	private List<GUIComponentTextBox> dataEntryBoxes = new ArrayList<GUIComponentTextBox>();
	private List<GUIComponentLabel> dataEntryLabels = new ArrayList<GUIComponentLabel>();
	private GUIComponentButton componentLoadButton;
	private GUIComponentButton componentSaveButton;
	private GUIComponentButton componentBackButton;
	private GUIComponentTextBox debugBox;
	private int numberComponents = 0;
	private boolean needToLoadState = false;
	
	//Static savers for GUI state.
	private static EntityVehicleE_Powered vehicle;
	private static EditScreen currentScreen = EditScreen.NONE;
	private static int lastComponentIndexModified = -1; 
	
	
	public GUIVehicleEditor(EntityVehicleE_Powered clickedVehicle){
		//If we are modifying the same vehicle as the last time we opened this GUI, re-load the state.
		if(clickedVehicle.equals(vehicle)){
			needToLoadState = true;
		}else{
			vehicle = clickedVehicle;
			currentScreen = EditScreen.NONE;
			lastComponentIndexModified = -1;
		}
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		screenSelectorButtons.clear();
		dataEntryBoxes.clear();
		dataEntryLabels.clear();
		
		//Add main screen buttons.
		//This also inits the lists in the map.
		for(EditScreen screen : EditScreen.values()){
			if(!screen.equals(EditScreen.NONE)){
				GUIComponentButton screenSelectorButton = new GUIComponentButton(guiLeft + 25, guiTop + 15 + screenSelectorButtons.size()*20, 200, "Edit " + screen.name().toLowerCase()){
					public void onClicked(){
						currentScreen = screen;
						for(byte i=0; i<15; ++i){
							dataEntryBoxes.get(i).setText("");
							dataEntryLabels.get(i).text = "";
						}
						if(currentScreen.equals(EditScreen.INSTRUMENTS)){
							debugBox.setText("Enter an instrument number to load and edit, or make a new one.  When done, press SAVE.");
							int labelBoxIndex = 0;
							dataEntryLabels.get(labelBoxIndex++).text = "Instrument# (0 is first instrumet):";
							dataEntryLabels.get(labelBoxIndex++).text = "X-Pos (blocks):";
							dataEntryLabels.get(labelBoxIndex++).text = "Y-Pos (blocks):";
							dataEntryLabels.get(labelBoxIndex++).text = "Z-Pos (blocks):";
							dataEntryLabels.get(labelBoxIndex++).text = "X-Rot (degrees):";
							dataEntryLabels.get(labelBoxIndex++).text = "Y-Rot (degrees):";
							dataEntryLabels.get(labelBoxIndex++).text = "Z-Rot (degrees):";
							dataEntryLabels.get(labelBoxIndex++).text = "Scale (normally 128x128 blocks):";
							dataEntryLabels.get(labelBoxIndex++).text = "HUD X-Pos (in texture px)";
							dataEntryLabels.get(labelBoxIndex++).text = "HUD Y-Pos (in texture px)";
							dataEntryLabels.get(labelBoxIndex++).text = "HUD Scale (normally 128x128 px):";
							dataEntryLabels.get(labelBoxIndex++).text = "Engine# (puts on panel too):";
							numberComponents = labelBoxIndex;
						}
					}
				};
				addButton(screenSelectorButton);
				screenSelectorButtons.put(screen, screenSelectorButton);
			}
		}
		
		//Add data entry boxes and labels.
		for(byte i=0; i<15; ++i){
			GUIComponentTextBox dataEntryBox = new GUIComponentTextBox(guiLeft + 190, guiTop + 15 + 11*i, 50, "", 10, Color.WHITE, Color.BLACK, 8);
			GUIComponentLabel dataEntryLabel = new GUIComponentLabel(guiLeft + 15, dataEntryBox.y, Color.WHITE, "").setBox(dataEntryBox);
			dataEntryBoxes.add(dataEntryBox);
			dataEntryLabels.add(dataEntryLabel);
			addTextBox(dataEntryBox);
			addLabel(dataEntryLabel);
		}
		
		//Add editing screen components.
		addTextBox(debugBox = new GUIComponentTextBox(guiLeft - 80, guiTop + 10, 70, "", 180, Color.WHITE, Color.BLACK, 400));
		
		addButton(componentLoadButton = new GUIComponentButton(guiLeft + 256, guiTop + 30, 60, "LOAD", 40, true){
			@Override
			public void onClicked(){
				if(currentScreen.equals(EditScreen.INSTRUMENTS)){
					try{
						//Load the information from the selected instrument into the boxes.
						int dataEntryBoxIndex = 0;
						int instrumentNumber = Integer.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						PackInstrument loadingInstrument = vehicle.definition.motorized.instruments.get(instrumentNumber);
						try{
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.pos[0]));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.pos[1]));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.pos[2]));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.rot[0]));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.rot[1]));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.rot[2]));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.scale));
							
							//We have this here so old instruments convert themselves correctly.
							if(loadingInstrument.hudX == 0 && loadingInstrument.hudY == 0){
								loadingInstrument.hudX = (int) (400D*(loadingInstrument.hudpos[0]/100D));
								loadingInstrument.hudY = (int) (2D*(loadingInstrument.hudpos[1]/100D -0.5D)*140);
								loadingInstrument.hudScale /= 2;
							}
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.hudX));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.hudY));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.hudScale));
							dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loadingInstrument.optionalEngineNumber));
							debugBox.setText("Loaded instrument definition: " + dataEntryBoxes.get(0).getText());
							lastComponentIndexModified = instrumentNumber;
						}catch(Exception e){
							debugBox.setText("ERROR: Could not load instrument from JSON.  Either instrument section is corrupt, or loader has faulted.");
						}
					}catch(Exception e){
						debugBox.setText("ERROR:\nInvalid instrument specified for loading.  Did you not save your edits last time?  Or did you just forget to enter an instrument in the first box to load?");
					}
				}
			}
		});
		
		addButton(componentSaveButton = new GUIComponentButton(guiLeft + 256, guiTop + 70, 60, "SAVE", 40, true){
			@Override
			public void onClicked(){
				if(currentScreen.equals(EditScreen.INSTRUMENTS)){
					//Validate the entered information.
					//If there is an error, note it in the error text box.
					int dataEntryBoxIndex = 0;
					try{
						int instrumentNumber = Integer.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						PackInstrument packInstrument = vehicle.definition.new PackInstrument();
						packInstrument.pos = new float[3];
						packInstrument.pos[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.pos[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.pos[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.rot = new float[3];
						packInstrument.rot[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.rot[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.rot[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.scale = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.hudX = Integer.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.hudY = Integer.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.hudScale = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						packInstrument.optionalEngineNumber = Byte.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
						
						if(instrumentNumber >= vehicle.definition.motorized.instruments.size()){
							instrumentNumber = vehicle.definition.motorized.instruments.size();
							dataEntryBoxes.get(0).setText(String.valueOf(instrumentNumber));
							vehicle.definition.motorized.instruments.add(packInstrument);
							debugBox.setText("Created new instrument definition #" + instrumentNumber);
						}else{
							vehicle.definition.motorized.instruments.set(instrumentNumber, packInstrument);
							debugBox.setText("Saved and replaced instrument definition #" + instrumentNumber);
						}
						lastComponentIndexModified = instrumentNumber;
						
						try{
							File outputFile = new File(MTS.minecraftDir, "DevModeOutput.json");
							FileWriter writer = new FileWriter(outputFile);
							new GsonBuilder().setPrettyPrinting().create().toJson(vehicle.definition, JSONVehicle.class, writer);
							writer.flush();
							writer.close();
							debugBox.setText(debugBox.getText() + "\n\nAlso saved JSON output to: " + outputFile.getAbsolutePath());
						}catch(Exception e){
							debugBox.setText(debugBox.getText() + "\n\nBut encountered an error when saving JSON.");
							e.printStackTrace();
						}
					}catch(Exception e){
						dataEntryBoxes.get(--dataEntryBoxIndex).setText("ERROR");
						debugBox.setText("ERROR: Invalid value detected.  This may be due to text being entered rather than a number or a decimal being entered where only a whole number is allowed.");
					}
				}
			}
		});
		
		addButton(componentBackButton = new GUIComponentButton(guiLeft + 256, guiTop + 110, 60, "BACK", 40, true){
			@Override
			public void onClicked(){
				currentScreen = EditScreen.NONE;
				numberComponents = -1;
			}
		});
	}
	
	@Override
	public void setStates(){
		//If we need to load our state, do so now.
		if(needToLoadState){
			if(!currentScreen.equals(EditScreen.NONE)){
				screenSelectorButtons.get(currentScreen).onClicked();
				if(lastComponentIndexModified != -1){
					dataEntryBoxes.get(0).setText(String.valueOf(lastComponentIndexModified));
					componentLoadButton.onClicked();
				}
			}
			needToLoadState = false;
		}
		
		//Set main screen button visibility.
		for(GUIComponentButton button : screenSelectorButtons.values()){
			button.visible = currentScreen.equals(EditScreen.NONE);
		}
		
		//Make input boxes and buttons visible depending on how many we need.
		for(byte i=0; i<15; ++i){
			dataEntryBoxes.get(i).visible = i < numberComponents; 
		}
		componentLoadButton.visible = !currentScreen.equals(EditScreen.NONE);
		componentSaveButton.visible = !currentScreen.equals(EditScreen.NONE);
		componentBackButton.visible = !currentScreen.equals(EditScreen.NONE);
		debugBox.visible = !currentScreen.equals(EditScreen.NONE);
	}
	
	private static enum EditScreen{
		NONE,
		INSTRUMENTS;
	}
}
