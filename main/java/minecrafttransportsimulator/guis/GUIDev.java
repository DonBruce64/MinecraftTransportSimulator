package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;

public class GUIDev extends GuiScreen{
	//Saved objects used every time we open this GUI.
	private static DevScreen currentScreen = DevScreen.MAIN;
	private static final Map<String, String> savedText = new HashMap<String, String>();
	private static final Map<String, Byte> savedIndexes = new HashMap<String, Byte>();

	//Maps containing objects.  Keyed by screen.  New objects are created for each screen.
	private final Map<DevScreen, List<DevButton>> buttons = new HashMap<DevScreen, List<DevButton>>();
	private final Map<DevScreen, List<DevTextBox>> textBoxes = new HashMap<DevScreen, List<DevTextBox>>();
	private final Map<DevScreen, List<DevText>> texts = new HashMap<DevScreen, List<DevText>>();

	//Public resources used for loading and rendering things.
	public static final Map<String, File> externalResources = new HashMap<String, File>();
	public static final Map<String, ResourceLocation> internalResources = new HashMap<String, ResourceLocation>();
	
	public GUIDev(){}
	
	@Override
	public void initGui(){
		//Populate all the maps with everything in them.
		//This is done for every screen.
		List<DevButton> buttonList = new ArrayList<DevButton>();
		List<DevTextBox> textBoxList = new ArrayList<DevTextBox>();
		List<DevText> textList = new ArrayList<DevText>();
		
		//MAIN
		textList.add(new DevText(5, 30, Color.WHITE, 
				"Welcome to the MTS in-game editor!", 
				"This system is designed to allow pack creators (you!) an easy way to make",
				"the JSONs for packs. It does this by letting you put your models in an external",
				"directory, and load them into the world.  From there, you may add parts,",
				"configure collision, adjust rotations, and even add instruments.  You've already",
				"made a model.  That's a great start.  So let's get this JSON knocked out together!"));
		textList.add(new DevText(5, 80, Color.WHITE, 
				"Note that for all file entries, you can specify a file in a pack.",
				"Do this by putting the pack name, followed by a colon.  For example, the",
				"OCP MC-172 model would be mtsofficalpack:mc172.obj"));
		if(MTS.MTSDevDir != null && !MTS.MTSDevDir.exists()){
			MTS.MTSDevDir.mkdir();
			if(!MTS.MTSDevDir.exists()){
				MTS.MTSDevDir = null;	
			}
		}
		if(MTS.MTSDevDir == null){
			textList.add(new DevText(5, 120, Color.RED, "An error occured when making the dev dir!  Contact the MTS author!"));
			return;
		}else{
			textList.add(new DevText(5, 120, Color.GREEN, "A dev dir was succefully created at:"));
			textList.add(new DevText(5, 130, Color.WHITE, MTS.MTSDevDir.getAbsolutePath()));
		}
		addNavigationButtons(buttonList);
		buttons.put(DevScreen.MAIN, buttonList);
		textBoxes.put(DevScreen.MAIN, textBoxList);
		texts.put(DevScreen.MAIN, textList);
		
		//MODEL
		buttonList = new ArrayList<DevButton>();
		textBoxList = new ArrayList<DevTextBox>();
		textList = new ArrayList<DevText>();
		textList.add(new DevText(5, 40, Color.WHITE, 
				"This screen allows you to specify the vehicle model to be loaded.", 
				"Enter the filename of the vehicle found in the dev dir, or use an existing",
				"pack model for a reference if you're designing parts.  It also allows for",
				"texture selection.  The same rules apply, except you can have multiple",
				"textures per model.  This will tie into your JSON when you create it, so",
				"make sure to specify all textures here.  Note that you don't have to have",
				"the textures in the dev dir for them to be in the JSON."));
		
		textList.add(new DevText(5, 90, Color.WHITE, "Model name:"));
		textBoxList.add(new DevTextBox(5, 110, 200, "VehicleModel"));
		buttonList.add(new DevButton(300, 110, 75, "CONFIRM", "ConfirmVehicleModel"));
		
		textList.add(new DevText(5, 130, Color.WHITE, "Texture#:"));
		textList.add(new DevText(75, 130, Color.WHITE, "Texture name:"));
		buttonList.add(new DevButton(5, 150, 20, "-", "-VehicleTexture"));
		buttonList.add(new DevButton(45, 150, 20, "+", "+VehicleTexture"));
		textBoxList.add(new DevTextBox(25, 150, 20, "#VehicleTexture"));
		textBoxList.add(new DevTextBox(75, 150, 200, "%VehicleTexture"));
		buttonList.add(new DevButton(300, 150, 75, "CONFIRM", "ConfirmVehicleTexture"));
		
		addNavigationButtons(buttonList);
		buttons.put(DevScreen.MODEL, buttonList);
		textBoxes.put(DevScreen.MODEL, textBoxList);
		texts.put(DevScreen.MODEL, textList);
	}
	
	private void addNavigationButtons(List<DevButton> buttonList){
		buttonList.add(new DevButton(5, height - 40, 75, "MAIN", "MAIN"));
		buttonList.add(new DevButton(5, height - 20, 75, "MODEL", "MODEL"));
		buttonList.add(new DevButton(80, height - 40, 75, "PARTS", "PARTS"));
		buttonList.add(new DevButton(80, height - 20, 75, "COLLISION", "COLLISION"));
		buttonList.add(new DevButton(155, height - 40, 75, "INSTRUMENTS", "INSTRUMENTS"));
		buttonList.add(new DevButton(155, height - 20, 75, "ROTATIONS", "ROTATIONS"));
	}

	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks){
		this.drawDefaultBackground();

		//Draw buttons for the screen specified.
		for(Entry<DevScreen, List<DevButton>> screenButtonSet : buttons.entrySet()){
			if(screenButtonSet.getKey().equals(currentScreen)){
				for(GuiButton button : screenButtonSet.getValue()){
					button.enabled = true;
					button.visible = true;
					button.drawButton(mc, mouseX, mouseY, partialTicks);
				}
			}else{
				for(GuiButton button : screenButtonSet.getValue()){
					button.enabled = false;
					button.visible = false;
				}
			}
		}
		
		//Draw text boxes for the screen specified.
		for(Entry<DevScreen, List<DevTextBox>> screenTextBoxSet : textBoxes.entrySet()){
			if(screenTextBoxSet.getKey().equals(currentScreen)){
				for(GuiTextField textBox : screenTextBoxSet.getValue()){
					textBox.setEnabled(true);
					textBox.setVisible(true);
					textBox.drawTextBox();
				}
			}else{
				for(GuiTextField textBox : screenTextBoxSet.getValue()){
					textBox.setEnabled(false);
					textBox.setVisible(false);
				}
			}
		}
		
		//Draw text for the screen specified.
		for(Entry<DevScreen, List<DevText>> screenTextSet : texts.entrySet()){
			if(screenTextSet.getKey().equals(currentScreen)){
				for(GuiLabel text : screenTextSet.getValue()){
					text.visible = true;
					text.drawLabel(mc, mouseX, mouseY);
				}
			}else{
				for(GuiLabel text : screenTextSet.getValue()){
					text.visible = false;
				}
			}
		}
	}
	
	@Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException{
		if(mouseButton == 0){
			//Update text box statuses.
			for(GuiTextField textBox : textBoxes.get(currentScreen)){
				if(textBox.getVisible()){
					textBox.mouseClicked(mouseX, mouseY, mouseButton);
				}
			}
			
			//Check to see if we clicked a button.
			for(DevButton button : buttons.get(currentScreen)){
				if(button.mousePressed(mc, mouseX, mouseY)){
					if(button.componentName.startsWith("+") || button.componentName.startsWith("-")){
						//We clicked an index +/- button.
						//Update the index and the textBox associated with that index.
						String indexName = button.componentName.substring(1);
						savedIndexes.put(indexName, (byte) (savedIndexes.get(indexName) + (button.componentName.startsWith("+") ? 1 : -1)));
						for(DevTextBox textBox : textBoxes.get(currentScreen)){
							if(textBox.componentName.contains(indexName)){
								if(textBox.componentName.startsWith("#")){
									textBox.setText(Byte.toString(savedIndexes.get(indexName)));
								}else{
									if(!savedText.containsKey(indexName + Byte.toString(savedIndexes.get(indexName)))){
										savedText.put(indexName + Byte.toString(savedIndexes.get(indexName)), "");
									}
									textBox.setText(savedText.get(indexName + Byte.toString(savedIndexes.get(indexName))));
								}
							}
						}
						return;
					}else if(button.componentName.startsWith("Confirm")){
						//We clicked a confirm button.  Check to see if resource can be loaded.
						//If so, make the text green.  If not, make it red.
						String resourceType = button.componentName.substring("Confirm".length());
						String resourceName;
						boolean resourceLoaded = false;
						for(DevTextBox box : textBoxes.get(currentScreen)){
							if(box.componentName.equals(resourceType)){
								resourceLoaded = loadResource(resourceType, box.getText());
								break;
							}else if(box.componentName.startsWith("%")){
								String indexName = box.componentName.substring(1);
								if(indexName.equals(resourceType)){
									resourceLoaded = loadResource(resourceType, box.getText());
									break;
								}
							}
						}
						if(resourceLoaded){
							button.packedFGColour = Color.GREEN.getRGB();
						}else{
							button.packedFGColour = Color.RED.getRGB();
						}
					}
					
					//Check to see if we clicked a navigation button.
					for(DevScreen screen : DevScreen.values()){
						if(screen.name().equals(button.componentName)){
							currentScreen = screen;
							return;
						}
					}
				}
			}
		}
	}
	
	private boolean loadResource(String resourceType, String resourceName){
		//Load from packs if we have a colon in the name.
		//Otherwise, load from the dev dir.
		if(resourceName.contains(":")){
			ResourceLocation resource;
			if(resourceType.equals("VehicleModel")){
				resource = new ResourceLocation(resourceName.substring(0, resourceName.indexOf(':')), "objmodels/vehicles/" + resourceName.substring(resourceName.indexOf(':') + 1));
			}else{
				return false;
			}
			internalResources.put(resourceName, resource);
		}else{
			
		}
		return true;
	}
	
	@Override
    protected void keyTyped(char key, int bytecode) throws IOException {
    	super.keyTyped(key, bytecode);
    	if(bytecode!=1){
    		//Check to see if we need to forward a keystroke to a text box.
			for(GuiTextField textBox : textBoxes.get(currentScreen)){
				textBox.textboxKeyTyped(key, bytecode);
			}
        }
    }
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	private enum DevScreen{
		MAIN,
		MODEL;
	}
	
	private class DevButton extends GuiButton{
		public final String componentName;
		
		public DevButton(int x, int y, int width, String buttonText, String componentName){
			super(0, x, y, width, 20, buttonText);
			this.componentName = componentName;
		}
	}
	
	private class DevTextBox extends GuiTextField{
		public final String componentName;
		
		public DevTextBox(int x, int y, int width, String componentName){
			super(0, fontRenderer, x, y, width, 20);
			this.componentName = componentName;
			//Check if we are a reference text box.  If so limit our input and set our text.
			if(componentName.startsWith("#")){
				setMaxStringLength(2);
				String indexName = componentName.substring(1); 
				if(!savedIndexes.containsKey(indexName)){
					savedIndexes.put(indexName, (byte) 0);
				}
				setText(Byte.toString(savedIndexes.get(indexName)));
			}else if(componentName.startsWith("%")){
				//We reference the indexed system to load the correct value.
				//We know the index must exist as we have loaded the reference box beforehand.
				//Well, at least we SHOULD have.  If not, we crash here.
				String indexName = componentName.substring(1);
				if(!savedText.containsKey(indexName + Byte.toString(savedIndexes.get(indexName)))){
					savedText.put(indexName + Byte.toString(savedIndexes.get(indexName)), "");
				}
				setText(savedText.get(indexName + Byte.toString(savedIndexes.get(indexName))));
			}
		}
		
		@Override
		public void setResponderEntryValue(int idIn, String textIn){
			super.setResponderEntryValue(idIn, textIn);
			//Update index if we change text.  Check to make sure text is valid so we don't crash.
			if(componentName.startsWith("#")){
				String indexName = componentName.substring(1);
				try{
					savedIndexes.put(indexName, Byte.valueOf(getText()));
				}catch(NumberFormatException e){
					setText(Byte.toString(savedIndexes.get(indexName)));
					return;
				}
				//Index is valid.  Update associated text box.
				for(DevTextBox textBox : textBoxes.get(currentScreen)){
					if(textBox.componentName.startsWith("%") && textBox.componentName.endsWith(indexName)){
						textBox.setText(indexName + Byte.toString(savedIndexes.get(indexName)));
					}
				}
			}else if(componentName.startsWith("%")){
				//Save our text using an index rather than the raw number.
				String indexName = componentName.substring(1);
				savedText.put(indexName + Byte.toString(savedIndexes.get(indexName)), getText());
			}else{
				savedText.put(componentName, getText());
			}
	    }
	}
	
	private class DevText extends GuiLabel{
		public DevText(int x, int y, Color color, String... text){
			super(fontRenderer, 0, x, y, fontRenderer.getStringWidth(text[0]), 20, color.getRGB());
			for(String textLine : text){
				addLine(textLine);
			}
		}
	}
	
}
