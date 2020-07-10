package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import mcinterface.BuilderGUI;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.sound.IRadioProvider;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.Radio.RadioSources;

/**GUI for interfacing with radios.  Radios are stored on classes that
 * extend {@link IRadioProvider} and are obtained via {@link IRadioProvider#getRadio()}.
 * This GUI allows for state changes to those objects.  Those changes are then
 * picked up by the audio system to affect the playing song.
*
* @author don_bruce
*/
public class GUIRadio extends AGUIBase{	
	//Buttons.
	private GUIComponentButton offButton;
	private GUIComponentButton localButton;
	private GUIComponentButton remoteButton;
	private GUIComponentButton serverButton;
	private GUIComponentButton randomButton;
	private GUIComponentButton orderedButton;
	private GUIComponentButton setButton;
	private GUIComponentButton equalizerButton;
	private GUIComponentButton equalizerBackButton;
	private GUIComponentButton equalizerResetButton;
	private GUIComponentButton volUpButton;
	private GUIComponentButton volDnButton;
	private List<GUIComponentButton> presetButtons = new ArrayList<GUIComponentButton>();
	private List<GUIComponentButton> equalizerButtons = new ArrayList<GUIComponentButton>();
	
	//Input boxes
	private GUIComponentTextBox stationDisplay;
	private GUIComponentTextBox volumeDisplay;
	
	//Runtime information.
	private final Radio radio;
	private final int bandsToSkip;
	private final int bandsToShow;
	private final int bandButtonSize;
	private static boolean equalizerMode = false;
	private static boolean teachMode = false;
	
	public GUIRadio(IRadioProvider provider){
		this.radio = provider.getRadio();
		this.bandsToSkip = 4;
		this.bandsToShow = radio.equalizer.getBandCount()/bandsToSkip;
		this.bandButtonSize = 20;
	}
	
	@Override
	public void setupComponents(int guiLeft, int guiTop){
		//Source selector block.
		addButton(offButton = new GUIComponentButton(guiLeft + 20, guiTop + 25, 55, "OFF", 15, true){
			@Override
			public void onClicked(){
				radio.stop();
				teachMode = false;
			}
		});
		addLabel(new GUIComponentLabel(offButton.x + offButton.width/2, offButton.y - 10, Color.BLACK, "SOURCE", 1.0F, true, false, 0).setButton(offButton));
		addButton(localButton = new GUIComponentButton(offButton.x, offButton.y + offButton.height, offButton.width, "PC", offButton.height, true){
			@Override
			public void onClicked(){
				radio.changeSource(RadioSources.LOCAL);
				teachMode = false;
			}
		});
		addButton(remoteButton = new GUIComponentButton(offButton.x, localButton.y + localButton.height, offButton.width, "INTERNET", offButton.height, true){
			@Override
			public void onClicked(){
				radio.changeSource(RadioSources.INTERNET);
				teachMode = false;
			}
		});
		addButton(serverButton = new GUIComponentButton(offButton.x, remoteButton.y + remoteButton.height, offButton.width, "SERVER", offButton.height, true){
			@Override
			public void onClicked(){
				radio.changeSource(RadioSources.SERVER);
				teachMode = false;
			}
		});
		
		//Playback order for local files.
		addButton(randomButton = new GUIComponentButton(offButton.x + offButton.width + 15, offButton.y, offButton.width, "RANDOM", offButton.height, true){public void onClicked(){radio.sorted = false;}});
		addLabel(new GUIComponentLabel(randomButton.x + randomButton.width/2, randomButton.y - 10, Color.BLACK, "PLAY ORDER", 1.0F, true, false, 0).setButton(randomButton));
		addButton(orderedButton = new GUIComponentButton(randomButton.x, randomButton.y + randomButton.height, randomButton.width, "SORTED", randomButton.height, true){public void onClicked(){radio.sorted = true;}});
		
		//Internet set button.
		addButton(setButton = new GUIComponentButton(orderedButton.x, orderedButton.y + 2*orderedButton.height, orderedButton.width, "SET URL", orderedButton.height, true){
			@Override
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
		
		//Volume controls.
		addButton(volUpButton = new GUIComponentButton(guiLeft + 205, offButton.y, 30, "UP"){public void onClicked(){radio.changeVolume(++radio.volume);}});
		addButton(volDnButton = new GUIComponentButton(volUpButton.x, volUpButton.y + volUpButton.height, volUpButton.width, "DN"){public void onClicked(){radio.changeVolume(--radio.volume);}});
		addTextBox(volumeDisplay = new GUIComponentTextBox(guiLeft + 180, volUpButton.y, 25, "", 40, Color.WHITE, Color.BLACK, 32));
		addButton(equalizerButton = new GUIComponentButton(volumeDisplay.x, volumeDisplay.y + volumeDisplay.height, volumeDisplay.width + volDnButton.width, "EQ", volUpButton.height, true){public void onClicked(){equalizerMode = true;}});
		addLabel(new GUIComponentLabel(volumeDisplay.x + volumeDisplay.width, volumeDisplay.y - 10, Color.BLACK, "VOLUME", 1.0F, true, false, 0).setButton(volUpButton));
		
		//Preset buttons.
		presetButtons.clear();
		int x = 25;
		for(byte i=1; i<7; ++i){
			presetButtons.add(new GUIComponentButton(guiLeft + x, guiTop + 155, 35, String.valueOf(i)){public void onClicked(){presetButtonClicked(this);}});
			addButton(presetButtons.get(i-1));
			x += 35;
		}
		
		//Station display box.
		addTextBox(stationDisplay = new GUIComponentTextBox(guiLeft + 20, guiTop + 105, 220, radio.displayText, 45, Color.WHITE, Color.BLACK, 100));
		
		//Add equalizer screen buttons.
		addButton(equalizerBackButton = new GUIComponentButton(guiLeft + 40, guiTop + 162, 80, "BACK"){public void onClicked(){equalizerMode = false;}});
		addButton(equalizerResetButton = new GUIComponentButton(guiLeft + getWidth() - 80 - 40, guiTop + 162, 80, "RESET"){
			@Override
			public void onClicked(){
				for(int i=0; i<radio.equalizer.getBandCount(); ++i){
					radio.equalizer.setBand(i, 0.0F);
				}
			}
		});
		
		//Equalizer band setting buttons.
		//We only show one in every 4 bands (8 bands total).  Nobody needs a 32-band equalizer...
		equalizerButtons.clear();
		int startingOffset = (getWidth() - (bandsToShow - 1)*bandButtonSize)/2;
		for(int i=0; i < bandsToShow; ++i){
			GUIComponentButton bandUpButton = new GUIComponentEqualizerButton(guiLeft + startingOffset - bandButtonSize/2 + bandButtonSize*i, guiTop + 20, true);
			GUIComponentButton bandDownButton = new GUIComponentEqualizerButton(guiLeft + startingOffset - bandButtonSize/2 + bandButtonSize*i, guiTop + 140, false);
			equalizerButtons.add(bandUpButton);
			equalizerButtons.add(bandDownButton);
			addButton(bandUpButton);
			addButton(bandDownButton);
		}
	}
	
	@Override
	public void setStates(){
		//Set visibility based on if we are in equalizer mode or not.
		for(GUIComponentButton button : this.buttons){
			button.visible = !((button.equals(equalizerBackButton) || equalizerButtons.contains(button)) ^ equalizerMode); 
		}
		equalizerButton.visible = !equalizerMode;
		equalizerBackButton.visible = equalizerMode;
		equalizerResetButton.visible = equalizerMode;
		volumeDisplay.visible = !equalizerMode;
		stationDisplay.visible = !equalizerMode;
		
		//Off button is enabled when radio is playing.
		offButton.enabled = radio.playing();
		
		//Local-remote are toggles.
		localButton.enabled = !radio.source.equals(RadioSources.LOCAL);
		remoteButton.enabled = !radio.source.equals(RadioSources.INTERNET);
		serverButton.enabled = !radio.source.equals(RadioSources.SERVER);
		
		//Random/ordered buttons are toggles, but only active when playing locally from folder.
		randomButton.enabled = radio.source.equals(RadioSources.LOCAL) && radio.sorted;
		orderedButton.enabled = radio.source.equals(RadioSources.LOCAL) && !radio.sorted;
		
		//Equalizer button isn't available for internet streams.
		equalizerButton.enabled = !radio.source.equals(RadioSources.INTERNET);
		
		//Set button only works if in Internet mode (playing from radio URL).
		//Once button is pressed, teach mode activates and stationDisplay becomes a station entry box.
		//Otherwise, it's just a box that displays what's playing.
		setButton.enabled = radio.source.equals(RadioSources.INTERNET);
		stationDisplay.enabled = teachMode;
		if(!teachMode){
			stationDisplay.setText(radio.displayText);
		}
		
		//Set volume system states to current volume settings.
		volumeDisplay.enabled = false;
		volumeDisplay.setText("VOL        " + String.valueOf(radio.volume));
		volUpButton.enabled = radio.volume < 10;
		volDnButton.enabled = radio.volume > 0;
		
		//Set preset button states depending on which preset the radio has selected.
		for(byte i=0; i<6; ++i){
			presetButtons.get(i).enabled = radio.presetIndex != i;
		}
	}
	
	private void presetButtonClicked(GUIComponentButton buttonClicked){
		int presetClicked = presetButtons.indexOf(buttonClicked);
		if(teachMode){
			//In teach mode.  Set Internet radio stations.
			Radio.setRadioStation(stationDisplay.getText(), presetClicked);
			stationDisplay.setText("Station set to preset " + (presetClicked + 1));
			teachMode = false;
		}else{
			//Do preset press logic.
			radio.pressPreset((byte) presetButtons.indexOf(buttonClicked));
		}
	}
	
	private class GUIComponentEqualizerButton extends GUIComponentButton{
		private final boolean increment;
		
		public GUIComponentEqualizerButton(int x, int y, boolean increment){
			super(x, y, bandButtonSize, increment ? "/\\" : "\\/", bandButtonSize, true);
			this.increment = increment;
		}
		
		@Override
		public void onClicked(){
			//Set the current band.  We use integer division as we have two buttons per band.
			int bandIndex = bandsToSkip*(equalizerButtons.indexOf(this)/2);
			float level = radio.equalizer.getBand(bandIndex);
			if(increment ? level < 0.9F : level > -0.9F){
				level += increment ? 0.2F : -0.2F;
				radio.equalizer.setBand(bandIndex, level);
				
				//Also set the 4 bands before and after this one depending on other band states.
				//We need to do interpolation here.
				if(bandIndex + bandsToSkip < radio.equalizer.getBandCount()){
					int nextBandIndex = bandIndex + bandsToSkip;
					float nextBandLevel = radio.equalizer.getBand(nextBandIndex);
					for(int i=1; i < bandsToSkip; ++i){
						radio.equalizer.setBand(bandIndex + i, level + i*(nextBandLevel - level)/bandsToSkip);
					}
				}

				if(bandIndex - bandsToSkip >= 0){
					int priorBandIndex = bandIndex - bandsToSkip;
					float priorBandLevel = radio.equalizer.getBand(priorBandIndex);
					for(int i=1; i < bandsToSkip; ++i){
						radio.equalizer.setBand(bandIndex - i, level - i*(level - priorBandLevel)/bandsToSkip);
					}
				}
			}
		}
		
		@Override
		public void renderText(){
			super.renderText();
			//We need to manually draw the equalizer level here.  Do so via built-in rectangle render method
			//as that is render-safe so we won't mess up any texture operations.  Only do this for the increment
			//equalizer button, and only if it's visible.
			if(visible && increment){
				//Get the upper and lower rectangle bounds.  Lower is below this button,
				//upper is above the button for decrementing.  We know that button is after us
				//in the list of equalizer buttons, so get it and use that y value.
				int upperBounds = y + bandButtonSize;
				int lowerBounds = equalizerButtons.get(equalizerButtons.indexOf(this) + 1).y;
				
				//Render a black rectangle between the buttons.
				int middlePoint = x + width/2;
				BuilderGUI.renderRectangle(middlePoint - 2 , lowerBounds, 4, upperBounds - lowerBounds, Color.BLACK);
				
				//Now render a red square where the equalizer value is.
				///Level is between -1.0 and 1.0, so we need to normalize it.
				int squareSize = 8;
				float level = (1.0F + radio.equalizer.getBand(bandsToSkip*(equalizerButtons.indexOf(this)/2)))/2F;
				int bandCenter = lowerBounds - squareSize + (int)(level*((upperBounds+squareSize/2) - (lowerBounds-squareSize/2)));
				BuilderGUI.renderRectangle(middlePoint - squareSize/2, bandCenter, squareSize, squareSize, Color.RED);
			}
		}
	}
}
