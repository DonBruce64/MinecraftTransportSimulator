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
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDisplayText;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRotatableModelObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleTranslatableModelObject;
import minecrafttransportsimulator.rendering.vehicles.RenderVehicle;
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
						debugBox.setText("Enter a " + screen.getStringName() + " number to load and edit, or make a new one.  When done, press SAVE.");
						numberComponents = screen.loader.setLabels(dataEntryLabels);
					}
				};
				addButton(screenSelectorButton);
				screenSelectorButtons.put(screen, screenSelectorButton);
			}
		}
		
		//Add data entry boxes and labels.
		for(byte i=0; i<15; ++i){
			GUIComponentTextBox dataEntryBox = new GUIComponentTextBox(guiLeft + 150, guiTop + 15 + 11*i, 90, "", 10, Color.WHITE, Color.BLACK, 25);
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
				//Get the number of the component to load.
				int componentIndex;
				try{
					componentIndex = Integer.valueOf(dataEntryBoxes.get(0).getText());
				}catch(Exception e){
					debugBox.setText("ERROR:\nInvalid entry for component index!  Entry must be a number.");
					return;
				}
				
				//Try to set the current index of the loader.
				if(!currentScreen.loader.setIndex(componentIndex)){
					debugBox.setText("ERROR:\nInvalid " + currentScreen.getStringName() + " specified for loading.  Did you not save your edits last time?  Or did you just forget to enter a " + currentScreen.getStringName() + " in the first box to load?");
					return;
				}
				
				//Loader index is set.  Try to load the data into the boxes.
				try{
					currentScreen.loader.loadObject(dataEntryBoxes);
					debugBox.setText("Loaded " + currentScreen.getStringName() + " definition: " + componentIndex);
					lastComponentIndexModified = componentIndex;
				}catch(Exception e){
					debugBox.setText("ERROR:\nCould not load " + currentScreen.getStringName() + " from JSON.  Either section is corrupt, or loader has faulted.");
				}
			}
		});
		
		addButton(componentSaveButton = new GUIComponentButton(guiLeft + 256, guiTop + 70, 60, "SAVE", 40, true){
			@Override
			public void onClicked(){
				//Try to have the loader save the entered data.
				int saveReturnCode = currentScreen.loader.saveObject(dataEntryBoxes);
				if(saveReturnCode >= 100){
					debugBox.setText("Created new " + currentScreen.getStringName() + " definition #" + (saveReturnCode - 100));
					lastComponentIndexModified = saveReturnCode - 100;
					dataEntryBoxes.get(0).setText(String.valueOf(lastComponentIndexModified));
				}else if(saveReturnCode >= 0){
					debugBox.setText("Saved and replaced " + currentScreen.getStringName() + " definition #" + saveReturnCode);
					lastComponentIndexModified = saveReturnCode;
				}else{
					dataEntryBoxes.get(-saveReturnCode).setText("ERROR");
					debugBox.setText("ERROR:\nInvalid value detected.  This may be due to text being entered rather than a number or a decimal being entered where only a whole number is allowed.");
					return;
				}
				
				//Loader save was successful.  Save JSON output to disk.
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
				
				//Reset the rendering system to update rendering caches.
				RenderVehicle.clearVehicleCaches(vehicle);
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
	
	
	/**Enum class to keep track of what screen we are on.
	 * 
	 * @author don_bruce
	 */
	private static enum EditScreen{
		NONE(null),
		MODELS(new ModelLoader()),
		INSTRUMENTS(new InstrumentLoader()),
		ROTATIONS(new RotationLoader()),
		TRANSLATIONS(new TranslationLoader()),
		TEXTS(new TextLoader());;
		
		private final LoaderHelper<?> loader;
		
		private EditScreen(LoaderHelper<?> loader){
			this.loader = loader;
		}
		
		public String getStringName(){
			return this.name().substring(0, this.name().length() - 1).toLowerCase();
		}
	}
	
	
	/**Helper class used for organizing save/load operations in this GUI.  Prevents massive if
	 * statements and redundant code by using generics and keeping all the logic in one place.
	 * Linked to the enums above.
	 * 
	 * @author don_bruce
	 */
	private static abstract class LoaderHelper<JSONObject>{
		protected int currentIndex;
		
		/**
		 *  Sets the internal index to the passed-in index.
		 *  This should return false if the index doesn't exist or is out of bounds.
		 */
		public abstract boolean setIndex(int index);
		
		/**
		 *  Sets the label text for the data labels.
		 *  Returns the number of label boxes that were set.
		 */
		public abstract int setLabels(List<GUIComponentLabel> dataEntryLabels);
		
		/**
		 *  Loads the object at the current index into the data boxes. 
		 */
		public abstract void loadObject(List<GUIComponentTextBox> dataEntryBoxes);
		
		/**
		 *  Saves the data from the data boxes into the object at the current index.
		 *  Returns a negative number if the save encountered an error.  The number
		 *  being the index of the box that had the error.  Returns a positive number
		 *  (or 0) if the save was successful, with the number being the index saved.
		 *  If a new index was made, it returns 100 plus the index.
		 *  Note that it is up to the calling function to save the resulting object to disk, if desired.
		 */
		public abstract int saveObject(List<GUIComponentTextBox> dataEntryBoxes);
	}
	
	private static class ModelLoader extends LoaderHelper<String>{

		@Override
		public boolean setIndex(int index){
			return true;
		}
		
		@Override
		public int setLabels(List<GUIComponentLabel> dataEntryLabels){
			int labelBoxIndex = 0;
			dataEntryLabels.get(labelBoxIndex++).text = "Just put a number here:";
			dataEntryLabels.get(labelBoxIndex++).text = "Model Folder (optional):";
			dataEntryLabels.get(labelBoxIndex++).text = "Model Name:";
			return labelBoxIndex;
		}

		@Override
		public void loadObject(List<GUIComponentTextBox> dataEntryBoxes){
			int dataEntryBoxIndex = 1;
			dataEntryBoxes.get(dataEntryBoxIndex++).setText("");
			dataEntryBoxes.get(dataEntryBoxIndex++).setText("");
		}

		@Override
		public int saveObject(List<GUIComponentTextBox> dataEntryBoxes){
			int dataEntryBoxIndex = 1;
			File modelFile;
			try{
				String folderName = dataEntryBoxes.get(dataEntryBoxIndex++).getText();
				if(!folderName.isEmpty() && !(new File(MTS.minecraftDir, folderName).exists())){
					return -1; 
				}
				
				String fileName = dataEntryBoxes.get(dataEntryBoxIndex++).getText();
				if(fileName.isEmpty()){
					return -2;
				}
				if(!folderName.isEmpty()){
					modelFile = new File(new File(MTS.minecraftDir, folderName), fileName);
				}else{
					modelFile = new File(MTS.minecraftDir, fileName);
				}
				if(!modelFile.exists()){
					return -2;
				}
			}catch(Exception e){
				return -(--dataEntryBoxIndex);
			}
			
			RenderVehicle.injectModel(vehicle, modelFile.getAbsolutePath());
			return 0;
		}
	}
	
	private static class InstrumentLoader extends LoaderHelper<PackInstrument>{
		@Override
		public boolean setIndex(int index){
			if(index < vehicle.definition.motorized.instruments.size()){
				currentIndex = index;
				return true;
			}else{
				return false;
			}
		}
		
		@Override
		public int setLabels(List<GUIComponentLabel> dataEntryLabels){
			int labelBoxIndex = 0;
			dataEntryLabels.get(labelBoxIndex++).text = "Instrument# (0 is first):";
			dataEntryLabels.get(labelBoxIndex++).text = "X-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "Y-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "Z-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "X-Rot (degrees):";
			dataEntryLabels.get(labelBoxIndex++).text = "Y-Rot (degrees):";
			dataEntryLabels.get(labelBoxIndex++).text = "Z-Rot (degrees):";
			dataEntryLabels.get(labelBoxIndex++).text = "Scale (1=128x128 blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "HUD X-Pos (in texture px)";
			dataEntryLabels.get(labelBoxIndex++).text = "HUD Y-Pos (in texture px)";
			dataEntryLabels.get(labelBoxIndex++).text = "HUD Scale (1=128x128 px):";
			dataEntryLabels.get(labelBoxIndex++).text = "Part# (puts on panel too):";
			return labelBoxIndex;
		}

		@Override
		public void loadObject(List<GUIComponentTextBox> dataEntryBoxes){
			PackInstrument loading = vehicle.definition.motorized.instruments.get(currentIndex);
			
			int dataEntryBoxIndex = 1;
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.pos[0]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.pos[1]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.pos[2]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rot[0]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rot[1]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rot[2]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.scale));
			
			//We have this here so old instruments convert themselves correctly.
			if(loading.hudX == 0 && loading.hudY == 0){
				loading.hudX = (int) (400D*(loading.hudpos[0]/100D));
				loading.hudY = (int) (2D*(loading.hudpos[1]/100D -0.5D)*140);
				loading.hudScale /= 2;
			}
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.hudX));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.hudY));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.hudScale));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.optionalPartNumber));
		}

		@Override
		public int saveObject(List<GUIComponentTextBox> dataEntryBoxes){
			PackInstrument saving = vehicle.definition.new PackInstrument();
			
			int dataEntryBoxIndex = 1;
			try{
				saving.pos = new float[3];
				saving.pos[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.pos[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.pos[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rot = new float[3];
				saving.rot[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rot[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rot[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.scale = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.hudX = Integer.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.hudY = Integer.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.hudScale = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.optionalPartNumber = Byte.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
			}catch(Exception e){
				return -(--dataEntryBoxIndex);
			}
			
			if(currentIndex >= vehicle.definition.motorized.instruments.size()){
				currentIndex = vehicle.definition.motorized.instruments.size();
				vehicle.definition.motorized.instruments.add(saving);
				return 100 + currentIndex;
			}else{
				vehicle.definition.motorized.instruments.set(currentIndex, saving);
				return currentIndex;
			}
		}
	}
	
	private static class RotationLoader extends LoaderHelper<VehicleRotatableModelObject>{
		@Override
		public boolean setIndex(int index){
			if(index < vehicle.definition.rendering.rotatableModelObjects.size()){
				currentIndex = index;
				return true;
			}else{
				return false;
			}
		}
		
		@Override
		public int setLabels(List<GUIComponentLabel> dataEntryLabels){
			int labelBoxIndex = 0;
			dataEntryLabels.get(labelBoxIndex++).text = "Rotation# (0 is first):";
			dataEntryLabels.get(labelBoxIndex++).text = "Part Name (on OBJ):";
			dataEntryLabels.get(labelBoxIndex++).text = "X-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "Y-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "Z-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "X-Axis (vector):";
			dataEntryLabels.get(labelBoxIndex++).text = "Y-Axis (vector):";
			dataEntryLabels.get(labelBoxIndex++).text = "Z-Axis (vector):";
			dataEntryLabels.get(labelBoxIndex++).text = "Variable (see handbook):";
			dataEntryLabels.get(labelBoxIndex++).text = "Min clamp (0 is no clamp):";
			dataEntryLabels.get(labelBoxIndex++).text = "Max clamp (0 is no clamp):";
			return labelBoxIndex;
		}

		@Override
		public void loadObject(List<GUIComponentTextBox> dataEntryBoxes){
			VehicleRotatableModelObject loading = vehicle.definition.rendering.rotatableModelObjects.get(currentIndex);
			
			int dataEntryBoxIndex = 1;
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.partName));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationPoint[0]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationPoint[1]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationPoint[2]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationAxis[0]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationAxis[1]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationAxis[2]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationVariable));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationClampMin));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rotationClampMax));
		}

		@Override
		public int saveObject(List<GUIComponentTextBox> dataEntryBoxes){
			VehicleRotatableModelObject saving = vehicle.definition.new VehicleRotatableModelObject();
			
			int dataEntryBoxIndex = 1;
			try{
				saving.partName = dataEntryBoxes.get(dataEntryBoxIndex++).getText();
				saving.rotationPoint = new float[3];
				saving.rotationPoint[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rotationPoint[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rotationPoint[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rotationAxis = new float[3];
				saving.rotationAxis[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rotationAxis[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rotationAxis[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rotationVariable = dataEntryBoxes.get(dataEntryBoxIndex++).getText();
				saving.rotationClampMin = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rotationClampMax = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
			}catch(Exception e){
				return -(--dataEntryBoxIndex);
			}
			
			if(currentIndex >= vehicle.definition.rendering.rotatableModelObjects.size()){
				currentIndex = vehicle.definition.rendering.rotatableModelObjects.size();
				vehicle.definition.rendering.rotatableModelObjects.add(saving);
				return 100 + currentIndex;
			}else{
				vehicle.definition.rendering.rotatableModelObjects.set(currentIndex, saving);
				return currentIndex;
			}
		}
	}
	
	private static class TranslationLoader extends LoaderHelper<VehicleTranslatableModelObject>{
		@Override
		public boolean setIndex(int index){
			if(index < vehicle.definition.rendering.translatableModelObjects.size()){
				currentIndex = index;
				return true;
			}else{
				return false;
			}
		}
		
		@Override
		public int setLabels(List<GUIComponentLabel> dataEntryLabels){
			int labelBoxIndex = 0;
			dataEntryLabels.get(labelBoxIndex++).text = "Translation# (0 is first):";
			dataEntryLabels.get(labelBoxIndex++).text = "Part Name (on OBJ):";
			dataEntryLabels.get(labelBoxIndex++).text = "X-Axis (vector):";
			dataEntryLabels.get(labelBoxIndex++).text = "Y-Axis (vector):";
			dataEntryLabels.get(labelBoxIndex++).text = "Z-Axis (vector):";
			dataEntryLabels.get(labelBoxIndex++).text = "Variable (see handbook):";
			dataEntryLabels.get(labelBoxIndex++).text = "Min clamp (0 is no clamp):";
			dataEntryLabels.get(labelBoxIndex++).text = "Max clamp (0 is no clamp):";
			return labelBoxIndex;
		}

		@Override
		public void loadObject(List<GUIComponentTextBox> dataEntryBoxes){
			VehicleTranslatableModelObject loading = vehicle.definition.rendering.translatableModelObjects.get(currentIndex);
			
			int dataEntryBoxIndex = 1;
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.partName));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.translationAxis[0]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.translationAxis[1]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.translationAxis[2]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.translationVariable));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.translationClampMin));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.translationClampMax));
		}

		@Override
		public int saveObject(List<GUIComponentTextBox> dataEntryBoxes){
			VehicleTranslatableModelObject saving = vehicle.definition.new VehicleTranslatableModelObject();
			
			int dataEntryBoxIndex = 1;
			try{
				saving.partName = dataEntryBoxes.get(dataEntryBoxIndex++).getText();
				saving.translationAxis = new float[3];
				saving.translationAxis[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.translationAxis[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.translationAxis[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.translationVariable = dataEntryBoxes.get(dataEntryBoxIndex++).getText();
				saving.translationClampMin = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.translationClampMax = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
			}catch(Exception e){
				return -(--dataEntryBoxIndex);
			}
			
			if(currentIndex >= vehicle.definition.rendering.translatableModelObjects.size()){
				currentIndex = vehicle.definition.rendering.translatableModelObjects.size();
				vehicle.definition.rendering.translatableModelObjects.add(saving);
				return 100 + currentIndex;
			}else{
				vehicle.definition.rendering.translatableModelObjects.set(currentIndex, saving);
				return currentIndex;
			}
		}
	}
	
	private static class TextLoader extends LoaderHelper<VehicleDisplayText>{
		@Override
		public boolean setIndex(int index){
			if(index < vehicle.definition.rendering.textMarkings.size()){
				currentIndex = index;
				return true;
			}else{
				return false;
			}
		}
		
		@Override
		public int setLabels(List<GUIComponentLabel> dataEntryLabels){
			int labelBoxIndex = 0;
			dataEntryLabels.get(labelBoxIndex++).text = "Text# (0 is first):";
			dataEntryLabels.get(labelBoxIndex++).text = "X-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "Y-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "Z-Pos (blocks):";
			dataEntryLabels.get(labelBoxIndex++).text = "X-Rot (deg):";
			dataEntryLabels.get(labelBoxIndex++).text = "Y-Rot (deg):";
			dataEntryLabels.get(labelBoxIndex++).text = "Z-Rot (deg):";
			dataEntryLabels.get(labelBoxIndex++).text = "Scale (1=1/2 block):";
			dataEntryLabels.get(labelBoxIndex++).text = "Color (hex, #FFFFFF):";
			return labelBoxIndex;
		}

		@Override
		public void loadObject(List<GUIComponentTextBox> dataEntryBoxes){
			VehicleDisplayText loading = vehicle.definition.rendering.textMarkings.get(currentIndex);
			
			int dataEntryBoxIndex = 1;
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.pos[0]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.pos[1]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.pos[2]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rot[0]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rot[1]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.rot[2]));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.scale));
			dataEntryBoxes.get(dataEntryBoxIndex++).setText(String.valueOf(loading.color));
		}

		@Override
		public int saveObject(List<GUIComponentTextBox> dataEntryBoxes){
			VehicleDisplayText saving = vehicle.definition.new VehicleDisplayText();
			
			int dataEntryBoxIndex = 1;
			try{
				saving.pos = new float[3];
				saving.pos[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.pos[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.pos[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rot = new float[3];
				saving.rot[0] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rot[1] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.rot[2] = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.scale = Float.valueOf(dataEntryBoxes.get(dataEntryBoxIndex++).getText());
				saving.color = dataEntryBoxes.get(dataEntryBoxIndex++).getText();
			}catch(Exception e){
				return -(--dataEntryBoxIndex);
			}
			
			if(currentIndex >= vehicle.definition.rendering.textMarkings.size()){
				currentIndex = vehicle.definition.rendering.textMarkings.size();
				vehicle.definition.rendering.textMarkings.add(saving);
				return 100 + currentIndex;
			}else{
				vehicle.definition.rendering.textMarkings.set(currentIndex, saving);
				return currentIndex;
			}
		}
	}
}
