package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.systems.RadioSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GUIRadio extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/standard.png");	
	
	//Global variables for rendering.
	private int guiLeft;
	private int guiTop;
	
	//Buttons.
	private GuiRadioButton offButton;
	private GuiRadioButton localButton;
	private GuiRadioButton remoteButton;
	private GuiRadioButton serverButton;
	private GuiRadioButton randomButton;
	private GuiRadioButton orderedButton;
	private GuiRadioButton setButton;
	private List<GuiRadioButton> presetButtons = new ArrayList<GuiRadioButton>();
	
	//Input boxes
	private GuiTextField stationDisplay;
	
	//Vehicle this radio is a part of.  Used to tell the radio which vehicle to link to when we press play.
	private final EntityVehicleE_Powered vehicle;
	
	//Static runtime information.
	private static List<String> presets = new ArrayList<String>();
	private static boolean localMode = true;
	private static boolean serverMode = false;
	private static boolean randomMode = false;
	private static int stationSelected = -1;
	
	public GUIRadio(EntityVehicleE_Powered vehicle){
		RadioSystem.init();
		this.allowUserInput=true;
		this.vehicle = vehicle;
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 256)/2;
		guiTop = (this.height - 192)/2;
		
		buttonList.add(offButton = new GuiRadioButton(guiLeft + 15, guiTop + 20, 45, "OFF"));
		buttonList.add(localButton = new GuiRadioButton(guiLeft + 15, guiTop + 40, 45, "FOLDER"));
		buttonList.add(remoteButton = new GuiRadioButton(guiLeft + 15, guiTop + 60, 45, "RADIO"));
		buttonList.add(serverButton = new GuiRadioButton(guiLeft + 15, guiTop + 80, 45, "SERVER"));
		buttonList.add(randomButton = new GuiRadioButton(guiLeft + 80, guiTop + 30, 45, "RANDOM"));
		buttonList.add(orderedButton = new GuiRadioButton(guiLeft + 80, guiTop + 50, 45, "SORTED"));
		buttonList.add(setButton = new GuiRadioButton(guiLeft + 190, guiTop + 90, 45, "SET"));
		
		int x = 25;
		for(byte i=1; i<7; ++i){
			GuiRadioButton presetButton = new GuiRadioButton(guiLeft + x, guiTop + 150, 35, String.valueOf(i));
			presetButtons.add(presetButton);
			x += 35;
		}
		buttonList.addAll(presetButtons);
		
		stationDisplay = new GuiTextField(0, fontRenderer, guiLeft + 20, guiTop + 120, 220, 20);
		stationDisplay.setMaxStringLength(100);
		
		//Get presets based on state.
		if(localMode){
			presets = RadioSystem.getMusicDirectories();
			stationDisplay.setEnabled(false);
		}else{
			presets = RadioSystem.getRadioStations();
		}
		
		//Set station display based on last button pressed.
		if(stationSelected != -1){
			presetButtons.get(stationSelected).enabled = false;
			stationDisplay.setText(presets.get(stationSelected));
		}else{
			stationDisplay.setText("");
		}
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks){
		//Draw Background.
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 192);
		
		//Set toggle button enable states.
		offButton.enabled = stationSelected != -1;
		localButton.enabled = !localMode;
		remoteButton.enabled = localMode;
		serverButton.enabled = false;
		randomButton.enabled = !randomMode && localMode;
		orderedButton.enabled = randomMode && localMode;
		setButton.enabled = !localMode && stationSelected != -1;
		
		//Draw buttons.
		for(GuiButton button : buttonList){
			button.drawButton(mc, mouseX, mouseY, partialTicks);
		}
		
		//Draw text boxes.
		stationDisplay.drawTextBox();
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(offButton)){
			//Stop the radio and re-enable the preset button to be pushed again to start the radio.
			RadioSystem.stop();
			presetButtons.get(stationSelected).enabled = true;
			stationSelected = -1;
			stationDisplay.setText("");
		}else if(buttonClicked.equals(localButton)){
			localMode = true;
			presets = RadioSystem.getMusicDirectories();
			stationDisplay.setEnabled(false);
			if(stationSelected != -1){
				actionPerformed(offButton);
			}
		}else if(buttonClicked.equals(remoteButton)){
			localMode = false;
			presets = RadioSystem.getRadioStations();
			stationDisplay.setEnabled(true);
			if(stationSelected != -1){
				actionPerformed(offButton);
			}
		}else if(buttonClicked.equals(randomButton)){
			randomMode = true;
		}else if(buttonClicked.equals(orderedButton)){
			randomMode = false;
		}else if(buttonClicked.equals(setButton)){
			//Set the station in the currently-selected preset button slot to the station in the display.
			//Un-set the preset after this to allow the user to press it and play the station.
			presets.set(stationSelected, stationDisplay.getText());
			RadioSystem.setRadioStations(presets);
		}else if(presetButtons.contains(buttonClicked)){
			//Set the selection index to the button pressed and disable the button.
			//Enable the last-selected button if needed.
			if(stationSelected != -1){
				presetButtons.get(stationSelected).enabled = true;
			}
			stationSelected = presetButtons.indexOf(buttonClicked);
			buttonClicked.enabled = false;
			
			//Attempt to play the station selected.
			if(localMode){
				String directory = RadioSystem.getMusicDirectories().get(stationSelected);
				if(!directory.isEmpty()){
					stationDisplay.setText(directory);
					RadioSystem.stop();
					RadioSystem.play(directory, vehicle, !randomMode);
				}else{
					stationDisplay.setText("Folder " + stationSelected + " not found in mts_music.");
				}
			}else{
				String station = presets.get(stationSelected);
				if(station.isEmpty()){
					stationDisplay.setText("Enter a streaming URL and press SET.");
				}else{
					if(!RadioSystem.play(new URL(station), vehicle)){
						stationDisplay.setText("INVALID URL!");
						presets.set(presetButtons.indexOf(buttonClicked), "");
						RadioSystem.setRadioStations(presets);
					}else{
						stationDisplay.setText(station);
						buttonClicked.enabled = false;
					}
				}
			}
		}
	}
	
	
    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException{
    	super.mouseClicked(x, y, button);
    	stationDisplay.mouseClicked(x, y, button);
    }
	
    @Override
    protected void keyTyped(char key, int bytecode) throws IOException {
    	super.keyTyped(key, bytecode);
    	if(stationDisplay.isFocused()){
    		stationDisplay.textboxKeyTyped(key, bytecode);
    	}
    }
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
    
    /**Custom button class.  This allows ups to use a custom texture.**/
    private class GuiRadioButton extends GuiButton{
		private static final int BUTTON_TEXTURE_U_OFFSET = 196;
		private static final int BUTTON_TEXTURE_WIDTH = 200;
		private static final int BUTTON_TEXTURE_HEIGHT = 20;
		    	
    	public GuiRadioButton(int x, int y, int width, String text){
    		super(0, x, y, width, 20, text);
    	}

    	@Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks){
    		//super.drawButton(mc, mouseX, mouseY, partialTicks);
            if(this.visible){
            	if(visible){
            		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            		mc.getTextureManager().bindTexture(background);
    				if(enabled){
    					if(mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height){
    			            drawTexturedModalRect(x, y, 0, BUTTON_TEXTURE_U_OFFSET + 2*BUTTON_TEXTURE_HEIGHT, width / 2, height);
    			            drawTexturedModalRect(x + width / 2, y, BUTTON_TEXTURE_WIDTH - width / 2, BUTTON_TEXTURE_U_OFFSET + 2*BUTTON_TEXTURE_HEIGHT, width / 2, height);
    					}else{
    						drawTexturedModalRect(x, y, 0, BUTTON_TEXTURE_U_OFFSET + BUTTON_TEXTURE_HEIGHT, width / 2, height);
    						drawTexturedModalRect(x + width / 2, y, BUTTON_TEXTURE_WIDTH - width / 2, BUTTON_TEXTURE_U_OFFSET + BUTTON_TEXTURE_HEIGHT, width / 2, height);
    					}
    				}else{
    					drawTexturedModalRect(x, y, 0, BUTTON_TEXTURE_U_OFFSET, width / 2, height);
						drawTexturedModalRect(x + width / 2, y, BUTTON_TEXTURE_WIDTH - width / 2, BUTTON_TEXTURE_U_OFFSET, width / 2, height);
    				}
    				this.drawCenteredString(mc.fontRenderer, displayString, x + width / 2, y + (height - 8) / 2, Color.GRAY.getRGB());
    			}
            }
        }
    }
}
