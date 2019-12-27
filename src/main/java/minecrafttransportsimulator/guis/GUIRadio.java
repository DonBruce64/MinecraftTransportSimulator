package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.radio.Radio;
import minecrafttransportsimulator.radio.RadioContainer;
import minecrafttransportsimulator.radio.RadioManager;

public class GUIRadio extends GUIBase{	
	//Buttons.
	private GUIComponentButton offButton;
	private GUIComponentButton localButton;
	private GUIComponentButton remoteButton;
	private GUIComponentButton serverButton;
	private GUIComponentButton randomButton;
	private GUIComponentButton orderedButton;
	private GUIComponentButton setButton;
	private GUIComponentButton volUpButton;
	private GUIComponentButton volDnButton;
	private List<GUIComponentButton> presetButtons = new ArrayList<GUIComponentButton>();
	
	//Input boxes
	private GUIComponentTextBox stationDisplay;
	private GUIComponentTextBox volumeDisplay;
	
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
	public void setupComponents(int guiLeft, int guiTop){
		addButton(offButton = new GUIComponentButton(guiLeft + 15, guiTop + 20, 45, "OFF"){
			public void onClicked(){
				presetButtons.get(radio.getPresetSelected()).enabled = true;
				radio.stopPlaying();
				stationDisplay.setText("");
				teachMode = false;
			}
		});
		addButton(localButton = new GUIComponentButton(guiLeft + 15, guiTop + 40, 45, "FOLDER"){
			public void onClicked(){
				localMode = true;
				teachMode = false;
				if(radio.getPresetSelected() != -1){
					offButton.onClicked();
				}
			}
		});
		addButton(remoteButton = new GUIComponentButton(guiLeft + 15, guiTop + 60, 45, "RADIO"){
			public void onClicked(){
				localMode = false;
				if(radio.getPresetSelected() != -1){
					offButton.onClicked();
				}
			}
		});
		addButton(serverButton = new GUIComponentButton(guiLeft + 15, guiTop + 80, 45, "SERVER"){public void onClicked(){}});
		addButton(randomButton = new GUIComponentButton(guiLeft + 80, guiTop + 30, 45, "RANDOM"){public void onClicked(){randomMode = true;}});
		addButton(orderedButton = new GUIComponentButton(guiLeft + 80, guiTop + 50, 45, "SORTED"){public void onClicked(){randomMode = false;}});
		addButton(setButton = new GUIComponentButton(guiLeft + 190, guiTop + 80, 45, "SET"){
			public void onClicked(){
				if(teachMode){
					teachMode = false;
					stationDisplay.setText("");
				}else{
					teachMode = true;
					stationDisplay.setText("Type or paste a URL (CTRL+V).\nThen press press a preset button.");
					stationDisplay.focused = true;
				}
			}
		});
		addButton(volUpButton = new GUIComponentButton(guiLeft + 205, guiTop + 20, 30, "UP"){public void onClicked(){radio.setVolume((byte) (radio.getVolume() + 1));}});
		addButton(volDnButton = new GUIComponentButton(guiLeft + 205, guiTop + 40, 30, "DN"){public void onClicked(){radio.setVolume((byte) (radio.getVolume() - 1));}});
		
		presetButtons.clear();
		int x = 25;
		for(byte i=1; i<7; ++i){
			presetButtons.add(new GUIComponentButton(guiLeft + x, guiTop + 155, 35, String.valueOf(i)){public void onClicked(){presetButtonClicked(this);}});
			addButton(presetButtons.get(i-1));
			x += 35;
		}
		
		addTextBox(stationDisplay = new GUIComponentTextBox(guiLeft + 20, guiTop + 105, 220, radio.getSource(), 45, Color.WHITE, Color.BLACK, 100));
		addTextBox(volumeDisplay = new GUIComponentTextBox(guiLeft + 180, guiTop + 20, 25, "", 40, Color.WHITE, Color.BLACK, 32));
	}
	
	@Override
	public void setStates(){
		//Off button is enabled when radio is playing.
		offButton.enabled = radio.getPlayState() != -1;
		
		//Local-remote are toggles.  Server is for future use and permanently disabled.
		localButton.enabled = !localMode;
		remoteButton.enabled = localMode;
		serverButton.enabled = false;
		
		//Random/ordered buttons are toggles, but only active when playing locally from folder.
		randomButton.enabled = !randomMode && localMode;
		orderedButton.enabled = randomMode && localMode;
		
		//Set button only works if not in local mode (playing from radio URL).
		//Once button is pressed, teach mode activates and stationDisplay becomes a station entry box.
		//Otherwise, it's just a box that displays what's playing.
		setButton.enabled = !localMode;
		stationDisplay.enabled = teachMode;
		if(!teachMode){
			stationDisplay.setText(radio.getSource());
		}
		
		//Set volume system states to current volume settings.
		volumeDisplay.enabled = false;
		volumeDisplay.setText("VOL        " + String.valueOf(radio.getVolume()));
		volUpButton.enabled = radio.getVolume() < 10;
		volDnButton.enabled = radio.getVolume() > 0;
	}
	
	private void presetButtonClicked(GUIComponentButton buttonClicked){
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
