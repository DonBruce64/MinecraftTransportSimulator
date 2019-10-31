package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.radio.Radio;
import minecrafttransportsimulator.radio.RadioContainer;
import minecrafttransportsimulator.radio.RadioManager;
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
	
	//Runtime information.
	//[private final List<String> localPresets;
	//private final List<String> internetPresets;
	private final Radio radio;
	private static boolean localMode = true;
	private static boolean randomMode = false;
	private static boolean teachMode = false;
	
	public GUIRadio(RadioContainer container){
		RadioManager.init();
		radio = RadioManager.getRadio(container);
		this.allowUserInput=true;
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
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks){		
		//Draw Background.
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 192);
				
		//Enable station entry if wa are in teach mode.
		stationDisplay.setEnabled(teachMode);
		
		//Set button states.
		offButton.enabled = radio.getPlayState() != -1;
		localButton.enabled = !localMode;
		remoteButton.enabled = localMode;
		serverButton.enabled = false;
		randomButton.enabled = !randomMode && localMode;
		orderedButton.enabled = randomMode && localMode;
		setButton.enabled = !localMode;
		
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
			presetButtons.get(radio.getPresetSelected()).enabled = true;
			radio.stopPlaying();
			stationDisplay.setText("");
			teachMode = false;
		}else if(buttonClicked.equals(localButton)){
			localMode = true;
			teachMode = false;
			if(radio.getPresetSelected() != -1){
				actionPerformed(offButton);
			}
		}else if(buttonClicked.equals(remoteButton)){
			localMode = false;
			if(radio.getPresetSelected() != -1){
				actionPerformed(offButton);
			}
		}else if(buttonClicked.equals(randomButton)){
			randomMode = true;
		}else if(buttonClicked.equals(orderedButton)){
			randomMode = false;
		}else if(buttonClicked.equals(setButton)){
			if(teachMode){
				teachMode = false;
				stationDisplay.setText("");
			}else{
				teachMode = true;
				stationDisplay.setText("Enter a URL and press a preset button.");
			}
		}else if(presetButtons.contains(buttonClicked)){
			int presetClicked = presetButtons.indexOf(buttonClicked);
			//Enable the last-selected button if needed.
			if(radio.getPresetSelected() != -1){
				presetButtons.get(radio.getPresetSelected()).enabled = true;
			}
			
			if(teachMode){
				RadioManager.setRadioStation(stationDisplay.getText(), presetClicked);
				stationDisplay.setText("Station set to preset " + (presetClicked + 1));
				teachMode = false;
			}else if(localMode){
				String directory = RadioManager.getMusicDirectories().get(presetClicked);
				if(!directory.isEmpty()){
					stationDisplay.setText(directory);
					radio.playLocal(directory, presetClicked, !randomMode);
					buttonClicked.enabled = false;
				}else{
					stationDisplay.setText("Fewer than " + (presetClicked + 1) + " folders in mts_music.");
				}
			}else{
				String station = RadioManager.getRadioStations().get(presetClicked);
				if(station.isEmpty()){
					stationDisplay.setText("Press SET to teach a station.");
				}else{
					URL url = null;
					try{
						url = new URL(station);
					}catch(Exception e){}
					if(url == null || !radio.playInternet(url, presetClicked)){
						stationDisplay.setText("INVALID URL!");
						RadioManager.setRadioStation("", presetClicked);
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
