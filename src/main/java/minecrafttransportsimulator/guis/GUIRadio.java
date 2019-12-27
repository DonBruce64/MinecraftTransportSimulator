package minecrafttransportsimulator.guis;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.radio.Radio;
import minecrafttransportsimulator.radio.RadioContainer;
import minecrafttransportsimulator.radio.RadioManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;

public class GUIRadio extends GUIBase{	
	//Global variables for rendering.
	private int guiLeft;
	private int guiTop;
	
	//Buttons.
	private GUIButton offButton;
	private GUIButton localButton;
	private GUIButton remoteButton;
	private GUIButton serverButton;
	private GUIButton randomButton;
	private GUIButton orderedButton;
	private GUIButton setButton;
	private GUIButton volUpButton;
	private GUIButton volDnButton;
	private List<GUIButton> presetButtons = new ArrayList<GUIButton>();
	
	//Input boxes
	private GuiTextField stationDisplay;
	private GuiTextField volumeDisplay;
	
	//Runtime information.
	private final Radio radio;
	private static boolean localMode = true;
	private static boolean randomMode = false;
	private static boolean teachMode = false;
	
	public GUIRadio(RadioContainer container){
		RadioManager.init(MTS.minecraftDir);
		radio = RadioManager.getRadio(container);
		this.allowUserInput=true;
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 256)/2;
		guiTop = (this.height - 192)/2;
		
		buttonList.add(offButton = new GUIButton(guiLeft + 15, guiTop + 20, 45, "OFF"));
		buttonList.add(localButton = new GUIButton(guiLeft + 15, guiTop + 40, 45, "FOLDER"));
		buttonList.add(remoteButton = new GUIButton(guiLeft + 15, guiTop + 60, 45, "RADIO"));
		buttonList.add(serverButton = new GUIButton(guiLeft + 15, guiTop + 80, 45, "SERVER"));
		buttonList.add(randomButton = new GUIButton(guiLeft + 80, guiTop + 30, 45, "RANDOM"));
		buttonList.add(orderedButton = new GUIButton(guiLeft + 80, guiTop + 50, 45, "SORTED"));
		buttonList.add(setButton = new GUIButton(guiLeft + 190, guiTop + 90, 45, "SET"));
		buttonList.add(volUpButton = new GUIButton(guiLeft + 205, guiTop + 20, 30, "UP"));
		buttonList.add(volDnButton = new GUIButton(guiLeft + 205, guiTop + 40, 30, "DN"));
		
		int x = 25;
		for(byte i=1; i<7; ++i){
			presetButtons.add(new GUIButton(guiLeft + x, guiTop + 150, 35, String.valueOf(i)));
			x += 35;
		}
		buttonList.addAll(presetButtons);
		
		stationDisplay = new GuiTextField(0, fontRenderer, guiLeft + 20, guiTop + 120, 220, 20);
		stationDisplay.setMaxStringLength(100);
		volumeDisplay = new GuiTextField(0, fontRenderer, guiLeft + 160, guiTop + 20, 45, 40);
		volumeDisplay.setEnabled(false);
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks){		
		//Draw Background.
		this.mc.getTextureManager().bindTexture(standardTexture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 192);
				
		//Enable station entry if wa are in teach mode.
		stationDisplay.setEnabled(teachMode);
		
		//Set volume display text box.
		volumeDisplay.setText("VOL: " + String.valueOf(radio.getVolume()));
		
		//Set button states.
		offButton.enabled = radio.getPlayState() != -1;
		localButton.enabled = !localMode;
		remoteButton.enabled = localMode;
		serverButton.enabled = false;
		randomButton.enabled = !randomMode && localMode;
		orderedButton.enabled = randomMode && localMode;
		setButton.enabled = !localMode;
		volUpButton.enabled = radio.getVolume() < 10;
		volDnButton.enabled = radio.getVolume() > 0;
		
		//Draw buttons.
		for(GuiButton button : buttonList){
			button.drawButton(mc, mouseX, mouseY, partialTicks);
		}
		
		//Draw text boxes.
		stationDisplay.drawTextBox();
		volumeDisplay.drawTextBox();
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
		}else if(buttonClicked.equals(volUpButton)){
			radio.setVolume((byte) (radio.getVolume() + 1));
		}else if(buttonClicked.equals(volDnButton)){
			radio.setVolume((byte) (radio.getVolume() - 1));
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
}
