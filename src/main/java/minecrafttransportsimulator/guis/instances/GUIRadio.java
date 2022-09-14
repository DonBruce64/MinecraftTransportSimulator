package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketRadioStateChange;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.sound.RadioManager;
import minecrafttransportsimulator.sound.RadioManager.RadioSources;

/**
 * GUI for interfacing with radios.
 * This GUI allows for state changes to radios, which are then
 * picked up by the audio system to affect the playing song.
 *
 * @author don_bruce
 */
public class GUIRadio extends AGUIBase {
    //Buttons.
    private GUIComponentButton offButton;
    private GUIComponentButton localButton;
    private GUIComponentButton remoteButton;
    private GUIComponentButton serverButton;
    private GUIComponentButton orderedButton;
    private GUIComponentButton shuffleButton;
    private GUIComponentButton setButton;
    private GUIComponentButton equalizerButton;
    private GUIComponentButton equalizerBackButton;
    private GUIComponentButton equalizerResetButton;
    private GUIComponentButton volUpButton;
    private GUIComponentButton volDnButton;
    private final List<GUIComponentButton> presetButtons = new ArrayList<>();
    private final List<GUIComponentButton> equalizerButtons = new ArrayList<>();
    private final List<GUIComponentCutout> equalizerSliderBands = new ArrayList<>();
    private final List<GUIComponentCutout> equalizerSliders = new ArrayList<>();

    //Input boxes
    private GUIComponentTextBox stationDisplay;
    private GUIComponentTextBox volumeDisplay;

    //Runtime information.
    private final EntityRadio radio;
    private final int bandsToSkip;
    private final int bandsToShow;
    private final int bandButtonSize;
    private boolean equalizerMode = false;
    private boolean teachMode = false;

    public GUIRadio(EntityRadio radio) {
        super();
        this.radio = radio;
        this.bandsToSkip = 4;
        this.bandsToShow = 32 / bandsToSkip;
        this.bandButtonSize = 20;
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        //Source selector block.
        addComponent(offButton = new GUIComponentButton(guiLeft + 20, guiTop + 25, 55, 15, "OFF") {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio));
                teachMode = false;
            }
        });
        addComponent(new GUIComponentLabel(offButton.constructedX + offButton.width / 2, offButton.constructedY - 10, ColorRGB.BLACK, "SOURCE", TextAlignment.CENTERED, 1.0F).setButton(offButton));
        addComponent(localButton = new GUIComponentButton(offButton.constructedX, offButton.constructedY + offButton.height, offButton.width, offButton.height, "PC") {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio, RadioSources.LOCAL));
                teachMode = false;
            }
        });
        addComponent(remoteButton = new GUIComponentButton(offButton.constructedX, localButton.constructedY + localButton.height, offButton.width, offButton.height, "INTERNET") {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio, RadioSources.INTERNET));
                teachMode = false;
            }
        });
        addComponent(serverButton = new GUIComponentButton(offButton.constructedX, remoteButton.constructedY + remoteButton.height, offButton.width, offButton.height, "SERVER") {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio, RadioSources.SERVER));
                teachMode = false;
            }
        });

        //Ordered/shuffle buttons.
        addComponent(orderedButton = new GUIComponentButton(offButton.constructedX + offButton.width, offButton.constructedY, offButton.width, offButton.height, "ORDERED") {
            @Override
            public void onClicked(boolean leftSide) {
                orderedButton.enabled = false;
                shuffleButton.enabled = true;
            }
        });
        addComponent(shuffleButton = new GUIComponentButton(orderedButton.constructedX, orderedButton.constructedY + orderedButton.height, orderedButton.width, orderedButton.height, "SHUFFLE") {
            @Override
            public void onClicked(boolean leftSide) {
                orderedButton.enabled = true;
                shuffleButton.enabled = false;
            }
        });
        orderedButton.enabled = false;

        //Internet set button.
        addComponent(setButton = new GUIComponentButton(shuffleButton.constructedX, shuffleButton.constructedY + shuffleButton.height, shuffleButton.width, shuffleButton.height, "SET URL") {
            @Override
            public void onClicked(boolean leftSide) {
                if (teachMode) {
                    teachMode = false;
                    stationDisplay.setText("");
                } else {
                    InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio));
                    teachMode = true;
                    stationDisplay.setText("Type or paste a URL (CTRL+V).\nThen press press a preset button.");
                    stationDisplay.focused = true;
                }
            }
        });

        //Volume controls.
        addComponent(volUpButton = new GUIComponentButton(guiLeft + 205, offButton.constructedY, 30, 20, "UP") {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio, radio.volume + 1));
            }
        });
        addComponent(volDnButton = new GUIComponentButton(volUpButton.constructedX, volUpButton.constructedY + volUpButton.height, volUpButton.width, 20, "DN") {
            @Override
            public void onClicked(boolean leftSide) {
                InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio, radio.volume - 1));
            }
        });
        addComponent(volumeDisplay = new GUIComponentTextBox(guiLeft + 180, volUpButton.constructedY, 25, 40, "", ColorRGB.WHITE, 32));
        addComponent(equalizerButton = new GUIComponentButton(volumeDisplay.constructedX, volumeDisplay.constructedY + volumeDisplay.height, volumeDisplay.width + volDnButton.width, volUpButton.height, "EQ") {
            @Override
            public void onClicked(boolean leftSide) {
                equalizerMode = true;
            }
        });
        addComponent(new GUIComponentLabel(volumeDisplay.constructedX + volumeDisplay.width, volumeDisplay.constructedY - 10, ColorRGB.BLACK, "VOLUME", TextAlignment.CENTERED, 1.0F).setButton(volUpButton));

        //Preset buttons.
        presetButtons.clear();
        int x = 25;
        for (byte i = 1; i < 7; ++i) {
            presetButtons.add(new GUIComponentButton(guiLeft + x, guiTop + 155, 35, 20, String.valueOf(i)) {
                @Override
                public void onClicked(boolean leftSide) {
                    presetButtonClicked(this);
                }
            });
            addComponent(presetButtons.get(i - 1));
            x += 35;
        }

        //Station display box.
        addComponent(stationDisplay = new GUIComponentTextBox(guiLeft + 20, guiTop + 105, 220, 45, radio.displayText, ColorRGB.WHITE, 150));

        //Add equalizer screen buttons.
        addComponent(equalizerBackButton = new GUIComponentButton(guiLeft + 40, guiTop + 162, 80, 20, "BACK") {
            @Override
            public void onClicked(boolean leftSide) {
                equalizerMode = false;
            }
        });
        addComponent(equalizerResetButton = new GUIComponentButton(guiLeft + getWidth() - 80 - 40, guiTop + 162, 80, 20, "RESET") {
            @Override
            public void onClicked(boolean leftSide) {
                for (int i = 0; i < radio.currentStation.equalizer.getBandCount(); ++i) {
                    radio.currentStation.equalizer.setBand(i, 0.0F);
                }
            }
        });

        //Equalizer band setting buttons, slots, and sliders.
        //We only show one in every 4 bands (8 bands total).  Nobody needs a 32-band equalizer...
        equalizerButtons.clear();
        equalizerSliderBands.clear();
        equalizerSliders.clear();
        int startingOffset = (getWidth() - (bandsToShow - 1) * bandButtonSize) / 2;
        for (int i = 0; i < bandsToShow; ++i) {
            int centerXOffset = guiLeft + startingOffset + bandButtonSize * i;
            GUIComponentButton bandUpButton = new GUIComponentEqualizerButton(centerXOffset - bandButtonSize / 2, guiTop + 20, true);
            GUIComponentButton bandDownButton = new GUIComponentEqualizerButton(centerXOffset - bandButtonSize / 2, guiTop + 140, false);
            GUIComponentCutout sliderBand = new GUIComponentCutout(centerXOffset - 2, bandUpButton.constructedY + bandUpButton.height, 4, 100, STANDARD_COLOR_WIDTH_OFFSET, STANDARD_BLACK_HEIGHT_OFFSET, STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT);
            GUIComponentCutout slider = new GUIComponentCutout(centerXOffset - 4, guiTop + 90 - 4, 8, 8, STANDARD_COLOR_WIDTH_OFFSET, STANDARD_RED_HEIGHT_OFFSET, STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT);
            equalizerButtons.add(bandUpButton);
            equalizerButtons.add(bandDownButton);
            equalizerSliderBands.add(sliderBand);
            equalizerSliders.add(slider);
            addComponent(bandUpButton);
            addComponent(bandDownButton);
            addComponent(sliderBand);
            addComponent(slider);
        }

    }

    @Override
    public void setStates() {
        super.setStates();
        //Set all components invisible if we running EQ mode.
        //We then manually enable or disable the EQ components.
        for (AGUIComponent component : components) {
            component.visible = !equalizerMode;
        }
        background.visible = true;

        //Off button is enabled when radio is playing.
        offButton.enabled = radio.currentStation != null;

        //Local-remote are toggles.
        localButton.enabled = !radio.getSource().equals(RadioSources.LOCAL);
        remoteButton.enabled = !radio.getSource().equals(RadioSources.INTERNET);
        serverButton.visible = false;//serverButton.enabled = !radio.getSource().equals(RadioSources.SERVER);

        //Equalizer button isn't available for internet streams.
        equalizerButton.enabled = !equalizerMode && !radio.getSource().equals(RadioSources.INTERNET) && radio.currentStation != null && radio.currentStation.equalizer != null;

        //Set button only works if in Internet mode (playing from radio URL).
        //Once button is pressed, teach mode activates and stationDisplay becomes a station entry box.
        //Otherwise, it's just a box that displays what's playing.
        setButton.enabled = !equalizerMode && radio.getSource().equals(RadioSources.INTERNET);
        stationDisplay.enabled = teachMode;
        if (!teachMode) {
            if (radio.currentStation == null) {
                stationDisplay.setText(radio.displayText);
            } else {
                stationDisplay.setText(radio.currentStation.displayText);
            }
        }

        //Set volume system states to current volume settings.
        volumeDisplay.enabled = false;
        volumeDisplay.setText("VOL        " + radio.volume);
        volUpButton.enabled = radio.volume < 10;
        volDnButton.enabled = radio.volume > 1;

        //Set preset button states depending on which preset the radio has selected.
        for (byte i = 0; i < 6; ++i) {
            presetButtons.get(i).enabled = radio.preset - 1 != i;
        }

        //Adjust equalizer buttons, sliders, and bands.
        equalizerBackButton.visible = equalizerMode;
        equalizerResetButton.visible = equalizerMode;
        for (GUIComponentButton button : equalizerButtons) {
            button.visible = equalizerMode;
        }
        for (GUIComponentCutout band : equalizerSliderBands) {
            band.visible = equalizerMode;
        }
        for (GUIComponentCutout slider : equalizerSliders) {
            slider.visible = equalizerMode;
            if (equalizerMode) {
                slider.position.y = -slider.constructedY + radio.currentStation.equalizer.getBand(bandsToSkip * (equalizerSliders.indexOf(slider))) * 46;
            }
        }
    }

    private void presetButtonClicked(GUIComponentButton buttonClicked) {
        int presetClicked = presetButtons.indexOf(buttonClicked);
        if (teachMode) {
            //In teach mode.  Set Internet radio stations.
            RadioManager.setLocalStationURL(stationDisplay.getText(), presetClicked);
            stationDisplay.setText("Station set to preset " + (presetClicked + 1));
            teachMode = false;
        } else {
            //Do preset press logic.
            if (radio.getSource().equals(RadioSources.LOCAL)) {
                InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio, presetClicked + 1, orderedButton.enabled));
            } else {
                InterfaceManager.packetInterface.sendToServer(new PacketRadioStateChange(radio, presetClicked + 1, RadioManager.getLocalStationURL(presetClicked + 1)));
            }
        }
    }

    private class GUIComponentEqualizerButton extends GUIComponentButton {
        private final boolean increment;

        public GUIComponentEqualizerButton(int x, int y, boolean increment) {
            super(x, y, bandButtonSize, bandButtonSize, increment ? "/\\" : "\\/");
            this.increment = increment;
        }

        @Override
        public void onClicked(boolean leftSide) {
            //Set the current band.  We use integer division as we have two buttons per band.
            int bandIndex = bandsToSkip * (equalizerButtons.indexOf(this) / 2);
            float level = radio.currentStation.equalizer.getBand(bandIndex);
            if (increment ? level < 0.9F : level > -0.9F) {
                level += increment ? 0.2F : -0.2F;
                radio.currentStation.equalizer.setBand(bandIndex, level);

                //Also set the 4 bands before and after this one depending on other band states.
                //We need to do interpolation here.
                if (bandIndex + bandsToSkip < radio.currentStation.equalizer.getBandCount()) {
                    int nextBandIndex = bandIndex + bandsToSkip;
                    float nextBandLevel = radio.currentStation.equalizer.getBand(nextBandIndex);
                    for (int i = 1; i < bandsToSkip; ++i) {
                        radio.currentStation.equalizer.setBand(bandIndex + i, level + i * (nextBandLevel - level) / bandsToSkip);
                    }
                }

                if (bandIndex - bandsToSkip >= 0) {
                    int priorBandIndex = bandIndex - bandsToSkip;
                    float priorBandLevel = radio.currentStation.equalizer.getBand(priorBandIndex);
                    for (int i = 1; i < bandsToSkip; ++i) {
                        radio.currentStation.equalizer.setBand(bandIndex - i, level - i * (level - priorBandLevel) / bandsToSkip);
                    }
                }
            }
        }
    }
}
