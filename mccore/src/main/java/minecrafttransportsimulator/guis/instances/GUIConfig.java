package minecrafttransportsimulator.guis.instances;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentCutout;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONConfigEntry;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboardDynamic;

public class GUIConfig extends AGUIBase {
    //Global variables.
    private GUIComponentButton renderConfigScreenButton;
    private GUIComponentButton controlConfigScreenButton;
    private GUIComponentButton controlScreenButton;

    //Config variables.
    private boolean configuringControls = true;
    private boolean configuringRendering = false;
    private final Map<GUIComponentButton, JSONConfigEntry<Boolean>> renderConfigButtons = new HashMap<>();
    private final Map<GUIComponentButton, JSONConfigEntry<Boolean>> controlConfigButtons = new HashMap<>();

    //Keybind selection variables.
    private String vehicleConfiguring = "";
    private final String[] vehicleTypes = new String[]{"car", "aircraft"};
    private final Map<GUIComponentButton, String> vehicleSelectionButtons = new HashMap<>();
    private GUIComponentLabel vehicleSelectionFaultLabel;
    private GUIComponentButton finishKeyboardBindingsButton;

    //Volume level variables.
    private GUIComponentButton volumeUpButton;
    private GUIComponentButton volumeDownButton;
    private GUIComponentLabel volumeLabel;

    //Keyboard assignment variables.
    private boolean configuringKeyboard;
    private final Map<String, Map<GUIComponentTextBox, ControlsKeyboard>> keyboardBoxes = new HashMap<>();
    private final Map<String, Map<GUIComponentLabel, ControlsKeyboardDynamic>> keyboardLabels = new HashMap<>();

    //Joystick selection variables.
    private final List<GUIComponentButton> joystickSelectionButtons = new ArrayList<>();

    //Joystick component selection variables.
    private int scrollSpot = 0;
    private int selectedJoystickComponentCount = 0;
    private String selectedJoystickName;
    private GUIComponentButton componentListUpButton;
    private GUIComponentButton componentListDownButton;
    private GUIComponentButton deadzone_moreButton;
    private GUIComponentButton deadzone_lessButton;
    private GUIComponentTextBox deadzone_text;
    private final List<GUIComponentButton> joystickComponentSelectionButtons = new ArrayList<>();
    private final List<GUIComponentCutout> joystickComponentStateBackgrounds = new ArrayList<>();
    private final List<GUIComponentCutout> joystickComponentStateForegrounds = new ArrayList<>();

    //Joystick assignment variables.
    private boolean assigningDigital;
    private int joystickComponentId = -1;
    private GUIComponentButton cancelAssignmentButton;
    private GUIComponentButton clearAssignmentButton;

    //Joystick digital assignment variables.
    private final Map<String, Map<GUIComponentButton, ControlsJoystick>> digitalAssignButtons = new HashMap<>();

    //Joystick analog assignment variables.
    private final Map<String, Map<GUIComponentButton, ControlsJoystick>> analogAssignButtons = new HashMap<>();

    //Joystick analog calibration variables.
    private boolean calibrating = false;
    private ControlsJoystick controlCalibrating;
    private GUIComponentButton confirmBoundsButton;
    private GUIComponentButton invertAxisButton;
    private GUIComponentTextBox axisMinBoundsTextBox;
    private GUIComponentTextBox axisMaxBoundsTextBox;

    public GUIConfig() {
        super();
        if (!InterfaceManager.inputInterface.isJoystickSupportEnabled()) {
            InterfaceManager.inputInterface.initJoysticks();
        }
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        //Global header buttons.
        addComponent(renderConfigScreenButton = new GUIComponentButton(guiLeft, guiTop - 20, 85, 20, JSONConfigLanguage.GUI_CONFIG_HEADER_RENDERING.value) {
            @Override
            public void onClicked(boolean leftSide) {
                configuringControls = false;
                configuringRendering = true;
                vehicleConfiguring = "";
                selectedJoystickName = null;
                scrollSpot = 0;
                joystickComponentId = -1;
                calibrating = false;
            }
        });
        addComponent(controlConfigScreenButton = new GUIComponentButton(guiLeft + 85, guiTop - 20, 85, 20, JSONConfigLanguage.GUI_CONFIG_HEADER_CONFIG.value) {
            @Override
            public void onClicked(boolean leftSide) {
                configuringControls = false;
                configuringRendering = false;
                vehicleConfiguring = "";
                selectedJoystickName = null;
                scrollSpot = 0;
                joystickComponentId = -1;
                calibrating = false;
            }
        });
        addComponent(controlScreenButton = new GUIComponentButton(guiLeft + 171, guiTop - 20, 85, 20, JSONConfigLanguage.GUI_CONFIG_HEADER_CONTROLS.value) {
            @Override
            public void onClicked(boolean leftSide) {
                configuringControls = true;
            }
        });

        //Config buttons and text.
        //We have two sets here.  One for rendering, one for controls.
        populateConfigButtonList(renderConfigButtons, ConfigSystem.client.renderingSettings);
        populateConfigButtonList(controlConfigButtons, ConfigSystem.client.controlSettings);

        //Vehicle selection buttons and text.
        //We only have two types.  Car and aircraft.
        vehicleSelectionButtons.clear();
        addComponent(vehicleSelectionFaultLabel = new GUIComponentLabel(guiLeft + 10, guiTop + 90, ColorRGB.BLACK, "", TextAlignment.LEFT_ALIGNED, 0.8F, 240));
        for (String vehicleType : vehicleTypes) {
            String label = vehicleType.equals("car") ? JSONConfigLanguage.GUI_CONFIG_CONTROLS_CAR_KEYBOARD.value : JSONConfigLanguage.GUI_CONFIG_CONTROLS_AIRCRAFT_KEYBOARD.value;
            GUIComponentButton buttonKeyboard = new GUIComponentButton(guiLeft + 68, guiTop + 30 + 20 * vehicleSelectionButtons.size(), 120, 20, label) {
                @Override
                public void onClicked(boolean leftSide) {
                    String lookupString = vehicleSelectionButtons.get(this);
                    vehicleConfiguring = lookupString.substring(0, lookupString.indexOf('.'));
                    configuringKeyboard = true;
                }
            };
            vehicleSelectionButtons.put(buttonKeyboard, vehicleType + ".keyboard");
            addComponent(buttonKeyboard);
            //Add screen label if we haven't already.
            if (vehicleSelectionButtons.size() == 1) {
                addComponent(new GUIComponentLabel(guiLeft + 20, guiTop + 10, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_CONTROLS_TITLE.value).setButton(buttonKeyboard));
            }
        }

        //Add volume buttons and label.
        addComponent(volumeUpButton = new GUIComponentButton(guiLeft + 68, guiTop + 160, 20, 20, "/\\") {
            @Override
            public void onClicked(boolean leftSide) {
                ConfigSystem.client.controlSettings.masterVolume.value = (((int) (ConfigSystem.client.controlSettings.masterVolume.value * 10)) + 1) / 10F;
                ConfigSystem.saveToDisk();
            }
        });
        addComponent(volumeDownButton = new GUIComponentButton(guiLeft + 168, guiTop + 160, 20, 20, "\\/") {
            @Override
            public void onClicked(boolean leftSide) {
                ConfigSystem.client.controlSettings.masterVolume.value = (((int) (ConfigSystem.client.controlSettings.masterVolume.value * 10)) - 1) / 10F;
                ConfigSystem.saveToDisk();
            }
        });
        addComponent(volumeLabel = new GUIComponentLabel(guiLeft + 128, guiTop + 165, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_CONTROLS_VOLUME.value + ConfigSystem.client.controlSettings.masterVolume.value, TextAlignment.CENTERED, 1.0F));
        volumeLabel.setButton(volumeDownButton);

        //Now add joystick buttons.
        for (String vehicleType : vehicleTypes) {
            String label = vehicleType.equals("car") ? JSONConfigLanguage.GUI_CONFIG_CONTROLS_CAR_JOYSTICK.value : JSONConfigLanguage.GUI_CONFIG_CONTROLS_AIRCRAFT_JOYSTICK.value;
            GUIComponentButton buttonJoystick = new GUIComponentButton(guiLeft + 68, guiTop + 70 + 20 * vehicleSelectionButtons.size(), 120, 20, label) {
                @Override
                public void onClicked(boolean leftSide) {
                    String lookupString = vehicleSelectionButtons.get(this);
                    vehicleConfiguring = lookupString.substring(0, lookupString.indexOf('.'));
                    configuringKeyboard = false;
                }
            };
            vehicleSelectionButtons.put(buttonJoystick, vehicleType + ".joystick");
            addComponent(buttonJoystick);
        }

        //Keyboard buttons and text.
        keyboardBoxes.clear();
        keyboardLabels.clear();
        for (String vehicleType : vehicleTypes) {
            //First add the editable controls.
            int verticalOffset = 20;
            int horizontalOffset = 80;
            Map<GUIComponentTextBox, ControlsKeyboard> boxesForVehicle = new HashMap<>();
            for (ControlsKeyboard keyboardControl : ControlSystem.ControlsKeyboard.values()) {
                if (keyboardControl.systemName.contains(vehicleType)) {
                    //First create the text box for input.
                    GUIComponentTextBox box = new GUIComponentTextBox(guiLeft + horizontalOffset, guiTop + verticalOffset, 40, 10, "", ColorRGB.WHITE, 5) {
                        @Override
                        public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                            setText(InterfaceManager.inputInterface.getNameForKeyCode(typedCode));
                            keyboardBoxes.get(vehicleConfiguring).get(this).config.keyCode = typedCode;
                            ConfigSystem.saveToDisk();
                            focused = false;
                        }

                    };
                    boxesForVehicle.put(box, keyboardControl);
                    addComponent(box);

                    //Now create the label.
                    addComponent(new GUIComponentLabel(box.constructedX - 70, box.constructedY + 2, ColorRGB.BLACK, keyboardControl.language.value + ":").setBox(box));

                    verticalOffset += 11;
                    if (verticalOffset > 20 + 11 * 9) {
                        verticalOffset = 20;
                        horizontalOffset += 120;
                    }
                }
            }
            keyboardBoxes.put(vehicleType, boxesForVehicle);

            //Now add the dynamic controls.
            byte offset = 0;
            Map<GUIComponentLabel, ControlsKeyboardDynamic> dynamicLabels = new HashMap<>();
            for (ControlsKeyboardDynamic dynamicControl : ControlsKeyboardDynamic.values()) {
                if (dynamicControl.name().toLowerCase().contains(vehicleType)) {
                    GUIComponentLabel label = new GUIComponentLabel(guiLeft + 10, guiTop + 135 + offset, ColorRGB.BLACK, "");
                    dynamicLabels.put(label, dynamicControl);
                    addComponent(label);
                    offset += 11;
                }
            }
            keyboardLabels.put(vehicleType, dynamicLabels);
        }
        addComponent(finishKeyboardBindingsButton = new GUIComponentButton(guiLeft + 180, guiTop + 150, 50, 20, JSONConfigLanguage.GUI_CONFIRM.value) {
            @Override
            public void onClicked(boolean leftSide) {
                vehicleConfiguring = "";
            }
        });

        //Joystick selection buttons.
        joystickSelectionButtons.clear();
        for (int i = 0; i < 9; ++i) {
            GUIComponentButton button = new GUIComponentButton(guiLeft + 10, guiTop + 40 + 20 * joystickSelectionButtons.size(), 235, 20, "") {
                @Override
                public void onClicked(boolean leftSide) {
                    selectedJoystickName = InterfaceManager.inputInterface.getAllJoystickNames().get(joystickSelectionButtons.indexOf(this));
                    selectedJoystickComponentCount = InterfaceManager.inputInterface.getJoystickComponentCount(selectedJoystickName);
                }
            };
            joystickSelectionButtons.add(button);
            addComponent(button);

            //Link the header text to the first joystick button.
            if (joystickSelectionButtons.size() == 1) {
                addComponent(new GUIComponentLabel(guiLeft + 20, guiTop + 10, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_SELECT.value).setButton(button));
                addComponent(new GUIComponentLabel(guiLeft + 15, guiTop + 25, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_NAME.value).setButton(button));
            }
        }

        //Joystick component selection buttons and text.
        joystickComponentSelectionButtons.clear();
        joystickComponentStateBackgrounds.clear();
        joystickComponentStateForegrounds.clear();
        for (int i = 0; i < 9; ++i) {
            GUIComponentButton button = new GUIComponentButton(guiLeft + 10, guiTop + 45 + 15 * i, 215, 15, "", false, ColorRGB.DARK_GRAY, true) {
                @Override
                public void onClicked(boolean leftSide) {
                    joystickComponentId = joystickComponentSelectionButtons.indexOf(this) + scrollSpot;
                    assigningDigital = !InterfaceManager.inputInterface.isJoystickComponentAxis(selectedJoystickName, joystickComponentId);
                }
            };
            joystickComponentSelectionButtons.add(button);
            addComponent(button);
            GUIComponentCutout componentBackground = new GUIComponentCutout(button.constructedX + 100, button.constructedY + 2, 40, 10, STANDARD_COLOR_WIDTH_OFFSET, STANDARD_BLACK_HEIGHT_OFFSET, STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT);
            joystickComponentStateBackgrounds.add(componentBackground);
            addComponent(componentBackground);
            GUIComponentCutout componentForeground = new GUIComponentCutout(button.constructedX + 100, button.constructedY + 2, 40, 10, STANDARD_COLOR_WIDTH_OFFSET, 0, STANDARD_COLOR_WIDTH, STANDARD_COLOR_HEIGHT);
            joystickComponentStateForegrounds.add(componentForeground);
            addComponent(componentForeground);
        }
        addComponent(componentListUpButton = new GUIComponentButton(guiLeft + 225, guiTop + 45, 20, 20, "/\\") {
            @Override
            public void onClicked(boolean leftSide) {
                scrollSpot -= 9;
            }
        });
        addComponent(componentListDownButton = new GUIComponentButton(guiLeft + 225, guiTop + 155, 20, 20, "\\/") {
            @Override
            public void onClicked(boolean leftSide) {
                scrollSpot += 9;
            }
        });
        addComponent(deadzone_lessButton = new GUIComponentButton(guiLeft + 100, guiTop + 10, 20, 20, "<") {
            @Override
            public void onClicked(boolean leftSide) {
                ConfigSystem.client.controlSettings.joystickDeadZone.value = ((ConfigSystem.client.controlSettings.joystickDeadZone.value * 100 - 1) / 100F);
            }
        });
        addComponent(deadzone_moreButton = new GUIComponentButton(guiLeft + 220, guiTop + 10, 20, 20, ">") {
            @Override
            public void onClicked(boolean leftSide) {
                ConfigSystem.client.controlSettings.joystickDeadZone.value = ((ConfigSystem.client.controlSettings.joystickDeadZone.value * 100 + 1) / 100F);
            }
        });
        addComponent(deadzone_text = new GUIComponentTextBox(guiLeft + 120, guiTop + 10, 100, ""));

        addComponent(new GUIComponentLabel(guiLeft + 15, guiTop + 20, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_MAPPING.value).setButton(componentListUpButton));
        addComponent(new GUIComponentLabel(guiLeft + 15, guiTop + 35, ColorRGB.BLACK, "#").setButton(componentListUpButton));
        addComponent(new GUIComponentLabel(guiLeft + 30, guiTop + 35, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_NAME.value).setButton(componentListUpButton));
        addComponent(new GUIComponentLabel(guiLeft + 100, guiTop + 35, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_STATE.value).setButton(componentListUpButton));
        addComponent(new GUIComponentLabel(guiLeft + 140, guiTop + 35, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_ASSIGNMENT.value).setButton(componentListUpButton));

        //Joystick assignment buttons and text.
        //Global buttons and labels for digital and analog.
        addComponent(cancelAssignmentButton = new GUIComponentButton(guiLeft + 125, guiTop + 160, 100, 20, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_CANCEL.value) {
            @Override
            public void onClicked(boolean leftSide) {
                joystickComponentId = -1;
                calibrating = false;
            }
        });
        addComponent(clearAssignmentButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, 20, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_CLEAR.value) {
            @Override
            public void onClicked(boolean leftSide) {
                for (ControlsJoystick joystickControl : ControlsJoystick.values()) {
                    if (selectedJoystickName.equals(joystickControl.config.joystickName)) {
                        if ((joystickControl.isAxis ^ assigningDigital) && joystickControl.config.buttonIndex == joystickComponentId && joystickControl.systemName.startsWith(vehicleConfiguring)) {
                            joystickControl.clearControl();
                        }
                    }
                }
                joystickComponentId = -1;
            }
        });
        addComponent(new GUIComponentLabel(guiLeft + 20, guiTop + 10, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_CHOOSEMAP.value).setButton(clearAssignmentButton));

        //Digital and analog buttons.
        digitalAssignButtons.clear();
        analogAssignButtons.clear();
        for (String vehicleType : vehicleTypes) {
            short leftOffsetDigital = 0;
            short topOffsetDigital = 0;
            short topOffsetAnalog = 0;
            Map<GUIComponentButton, ControlsJoystick> digitalControlButtons = new HashMap<>();
            Map<GUIComponentButton, ControlsJoystick> analogControlButtons = new HashMap<>();
            for (ControlsJoystick joystickControl : ControlsJoystick.values()) {
                if (joystickControl.systemName.startsWith(vehicleType)) {
                    if (!joystickControl.isAxis) {
                        GUIComponentButton button = new GUIComponentButton(guiLeft + 8 + leftOffsetDigital, guiTop + 20 + topOffsetDigital, 80, 15, joystickControl.language.value) {
                            @Override
                            public void onClicked(boolean leftSide) {
                                digitalAssignButtons.get(vehicleConfiguring).get(this).setControl(selectedJoystickName, joystickComponentId);
                                joystickComponentId = -1;
                            }
                        };
                        digitalControlButtons.put(button, joystickControl);
                        addComponent(button);
                        topOffsetDigital += button.height;
                    } else {
                        GUIComponentButton button = new GUIComponentButton(guiLeft + 85, guiTop + 40 + topOffsetAnalog, 80, 20, joystickControl.language.value) {
                            @Override
                            public void onClicked(boolean leftSide) {
                                controlCalibrating = analogAssignButtons.get(vehicleConfiguring).get(this);
                                axisMinBoundsTextBox.setText("0.0");
                                axisMaxBoundsTextBox.setText("0.0");
                                calibrating = true;
                            }
                        };
                        analogControlButtons.put(button, joystickControl);
                        addComponent(button);
                        topOffsetAnalog += button.height;
                    }
                }
                if (topOffsetDigital >= 135) {
                    topOffsetDigital = 0;
                    leftOffsetDigital += 80;
                }
            }
            digitalAssignButtons.put(vehicleType, digitalControlButtons);
            analogAssignButtons.put(vehicleType, analogControlButtons);
        }

        //Analog calibration components.
        addComponent(confirmBoundsButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, 20, JSONConfigLanguage.GUI_CONFIRM.value) {
            @Override
            public void onClicked(boolean leftSide) {
                boolean isInverted = invertAxisButton.text.contains(JSONConfigLanguage.GUI_CONFIG_JOYSTICK_INVERT.value);
                controlCalibrating.setAxisControl(selectedJoystickName, joystickComponentId, Double.parseDouble(axisMinBoundsTextBox.getText()), Double.parseDouble(axisMaxBoundsTextBox.getText()), isInverted);
                joystickComponentId = -1;
                calibrating = false;
            }
        });
        addComponent(invertAxisButton = new GUIComponentButton(guiLeft + 50, guiTop + 120, 150, 20, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_AXISMODE.value + JSONConfigLanguage.GUI_CONFIG_JOYSTICK_NORMAL.value) {
            @Override
            public void onClicked(boolean leftSide) {
                if (text.contains(JSONConfigLanguage.GUI_CONFIG_JOYSTICK_INVERT.value)) {
                    text = JSONConfigLanguage.GUI_CONFIG_JOYSTICK_AXISMODE.value + JSONConfigLanguage.GUI_CONFIG_JOYSTICK_NORMAL.value;
                } else {
                    text = JSONConfigLanguage.GUI_CONFIG_JOYSTICK_AXISMODE.value + JSONConfigLanguage.GUI_CONFIG_JOYSTICK_INVERT.value;
                }
            }
        });
        addComponent(axisMinBoundsTextBox = new GUIComponentTextBox(guiLeft + 50, guiTop + 90, 150, "0.0"));
        axisMinBoundsTextBox.enabled = false;
        addComponent(axisMaxBoundsTextBox = new GUIComponentTextBox(guiLeft + 50, guiTop + 60, 150, "0.0"));
        axisMaxBoundsTextBox.enabled = false;
        addComponent(new GUIComponentLabel(guiLeft + 20, guiTop + 10, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_CALIBRATE1.value).setButton(confirmBoundsButton));
        addComponent(new GUIComponentLabel(guiLeft + 20, guiTop + 20, ColorRGB.BLACK, JSONConfigLanguage.GUI_CONFIG_JOYSTICK_CALIBRATE2.value).setButton(confirmBoundsButton));
    }

    @Override
    public void setStates() {
        super.setStates();
        //Global headers are just toggles depending on operation.
        renderConfigScreenButton.enabled = configuringControls || (!configuringControls && !configuringRendering);
        controlConfigScreenButton.enabled = configuringControls || (!configuringControls && configuringRendering);
        controlScreenButton.enabled = !configuringControls;

        //If we are not configuring controls, render the appropriate config buttons and labels.
        for (GUIComponentButton button : renderConfigButtons.keySet()) {
            button.visible = !renderConfigScreenButton.enabled;
        }
        for (GUIComponentButton button : controlConfigButtons.keySet()) {
            button.visible = !controlConfigScreenButton.enabled;
        }

        //If we are configuring controls, and haven't selected a vehicle, render the vehicle selection components.
        vehicleSelectionFaultLabel.visible = !InterfaceManager.inputInterface.isJoystickSupportEnabled() && configuringControls && !configuringKeyboard;
        if (vehicleSelectionFaultLabel.visible) {
            vehicleSelectionFaultLabel.text = InterfaceManager.inputInterface.isJoystickSupportBlocked() ? JSONConfigLanguage.GUI_CONFIG_JOYSTICK_DISABLED.value : JSONConfigLanguage.GUI_CONFIG_JOYSTICK_ERROR.value;
        }
        for (GUIComponentButton button : vehicleSelectionButtons.keySet()) {
            button.visible = configuringControls && vehicleConfiguring.isEmpty() && (!vehicleSelectionButtons.get(button).endsWith(".joystick") || InterfaceManager.inputInterface.isJoystickSupportEnabled());
        }

        //If we haven't selected anything, render the volume controls.
        if (configuringControls && vehicleConfiguring.isEmpty()) {
            volumeUpButton.visible = true;
            volumeUpButton.enabled = ConfigSystem.client.controlSettings.masterVolume.value < 1.5;
            volumeDownButton.visible = true;
            volumeDownButton.enabled = ConfigSystem.client.controlSettings.masterVolume.value > 0;
            volumeLabel.text = JSONConfigLanguage.GUI_CONFIG_CONTROLS_VOLUME.value + ConfigSystem.client.controlSettings.masterVolume.value;
        } else {
            volumeUpButton.visible = false;
            volumeDownButton.visible = false;
        }

        //If we have selected a vehicle, and are configuring a keyboard, render the keyboard controls.
        //Only enable the boxes and labels for the vehicle we are configuring, however.
        //If a box is focused, we should set the text to a blank value.
        finishKeyboardBindingsButton.visible = configuringControls && !vehicleConfiguring.isEmpty() && configuringKeyboard;
        for (String vehicleType : keyboardBoxes.keySet()) {
            for (GUIComponentTextBox textBox : keyboardBoxes.get(vehicleType).keySet()) {
                textBox.visible = finishKeyboardBindingsButton.visible && vehicleType.equals(vehicleConfiguring);
                if (textBox.focused) {
                    textBox.setText("");
                } else {
                    textBox.setText(InterfaceManager.inputInterface.getNameForKeyCode(keyboardBoxes.get(vehicleType).get(textBox).config.keyCode));
                }
            }
            for (GUIComponentLabel label : keyboardLabels.get(vehicleType).keySet()) {
                label.visible = finishKeyboardBindingsButton.visible && vehicleType.equals(vehicleConfiguring);
                ControlsKeyboardDynamic dynamicControl = keyboardLabels.get(vehicleType).get(label);
                label.text = dynamicControl.language.value + ": " + InterfaceManager.inputInterface.getNameForKeyCode(dynamicControl.modControl.config.keyCode) + " + " + InterfaceManager.inputInterface.getNameForKeyCode(dynamicControl.mainControl.config.keyCode);
            }
        }

        //If we have selected a vehicle, and are not configuring a keyboard, but haven't selected a joystick
        //make the joystick selection screen components visible.
        List<String> allJoystickNames = InterfaceManager.inputInterface.getAllJoystickNames();
        for (byte i = 0; i < 9; ++i) {
            GUIComponentButton button = joystickSelectionButtons.get(i);
            if (allJoystickNames.size() > i) {
                button.visible = configuringControls && !configuringKeyboard && !vehicleConfiguring.isEmpty() && selectedJoystickName == null;
                button.text = String.format(" %-30.28s", allJoystickNames.get(i));
            } else {
                button.visible = false;
            }
        }

        //If we have selected a joystick, but not a component, make the component selection buttons visible.
        boolean onComponentSelectScreen = selectedJoystickName != null && joystickComponentId == -1;
        for (byte i = 0; i < 9; ++i) {
            GUIComponentButton button = joystickComponentSelectionButtons.get(i);
            GUIComponentCutout componentBackground = joystickComponentStateBackgrounds.get(i);
            GUIComponentCutout componentForeground = joystickComponentStateForegrounds.get(i);
            button.visible = onComponentSelectScreen && i + scrollSpot < selectedJoystickComponentCount;
            componentBackground.visible = button.visible;
            componentForeground.visible = button.visible;
            if (button.visible) {
                //Set basic button text.
                int controlIndex = i + scrollSpot;
                button.text = String.format(" %02d  %-15.15s", controlIndex + 1, InterfaceManager.inputInterface.getJoystickComponentName(selectedJoystickName, controlIndex));

                //If this joystick is assigned to a control, append that to the text string.
                for (ControlsJoystick joystickControl : ControlsJoystick.values()) {
                    if (selectedJoystickName.equals(joystickControl.config.joystickName)) {
                        if (joystickControl.config.buttonIndex == controlIndex && joystickControl.systemName.startsWith(vehicleConfiguring)) {
                            button.text += String.format("          %s", joystickControl.language.value);
                        }
                    }
                }

                //Set state of color rendering to display axis state.
                //Joystick component selection buttons and text.
                float pollData = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, controlIndex);
                if (InterfaceManager.inputInterface.isJoystickComponentAxis(selectedJoystickName, controlIndex)) {
                    int pollDataInt = (int) (pollData * 20);
                    componentBackground.visible = true;
                    componentForeground.position.x = componentForeground.constructedX;
                    if (pollDataInt > 0) {
                        componentForeground.position.x += 20;
                        componentForeground.width = pollDataInt;
                    } else {
                        componentForeground.position.x += (20 + pollDataInt);
                        componentForeground.width = -pollDataInt;
                    }
                    componentForeground.textureYOffset = STANDARD_RED_HEIGHT_OFFSET;
                } else {
                    componentBackground.visible = false;
                    componentForeground.position.x = componentForeground.constructedX + 15;
                    componentForeground.width = 10;
                    if (pollData == 0) {
                        componentForeground.textureYOffset = STANDARD_BLACK_HEIGHT_OFFSET;
                    } else if (pollData == 1) {
                        componentForeground.textureYOffset = STANDARD_RED_HEIGHT_OFFSET;
                    } else {
                        //For digitals with fractions like hats.
                        componentForeground.textureYOffset = STANDARD_YELLOW_HEIGHT_OFFSET;
                    }
                }
            }
        }
        componentListUpButton.visible = onComponentSelectScreen;
        componentListDownButton.visible = onComponentSelectScreen;
        deadzone_lessButton.visible = onComponentSelectScreen;
        deadzone_moreButton.visible = onComponentSelectScreen;
        deadzone_text.visible = onComponentSelectScreen;
        if (onComponentSelectScreen) {
            componentListUpButton.enabled = scrollSpot - 9 >= 0;
            componentListDownButton.enabled = scrollSpot + 9 < selectedJoystickComponentCount;
            deadzone_lessButton.enabled = ConfigSystem.client.controlSettings.joystickDeadZone.value > 0;
            deadzone_moreButton.enabled = ConfigSystem.client.controlSettings.joystickDeadZone.value < 1;
            deadzone_text.enabled = false;
            deadzone_text.setText(JSONConfigLanguage.GUI_CONFIG_JOYSTICK_DEADZONE.value + " " + ConfigSystem.client.controlSettings.joystickDeadZone.value);
        }

        //If we have selected a component, render the assignment buttons.
        //These are global, so they are always visible.
        cancelAssignmentButton.visible = joystickComponentId != -1;
        cancelAssignmentButton.enabled = cancelAssignmentButton.visible;
        clearAssignmentButton.visible = cancelAssignmentButton.visible && !calibrating;
        clearAssignmentButton.enabled = cancelAssignmentButton.visible && !calibrating;

        //Set states of digital buttons.
        for (String vehicleType : digitalAssignButtons.keySet()) {
            for (GUIComponentButton button : digitalAssignButtons.get(vehicleType).keySet()) {
                button.visible = joystickComponentId != -1 && vehicleConfiguring.equals(vehicleType) && assigningDigital;
            }
        }

        //Set status of analog buttons.
        for (String vehicleType : analogAssignButtons.keySet()) {
            for (GUIComponentButton button : analogAssignButtons.get(vehicleType).keySet()) {
                button.visible = joystickComponentId != -1 && vehicleConfiguring.equals(vehicleType) && !assigningDigital && !calibrating;
            }
        }

        //If we are calibrating an analog control, render the components of that screen.
        //Only attempt to set the state of the text boxes if we are calibrating.
        confirmBoundsButton.visible = calibrating;
        cancelAssignmentButton.visible = calibrating;
        invertAxisButton.visible = calibrating;
        axisMinBoundsTextBox.visible = calibrating;
        axisMaxBoundsTextBox.visible = calibrating;
        if (calibrating) {
            float pollData = InterfaceManager.inputInterface.getJoystickAxisValue(selectedJoystickName, joystickComponentId);
            if (pollData < 0) {
                axisMinBoundsTextBox.setText(String.valueOf(Math.min(Double.parseDouble(axisMinBoundsTextBox.getText()), pollData)));
            } else {
                axisMaxBoundsTextBox.setText(String.valueOf(Math.max(Double.parseDouble(axisMaxBoundsTextBox.getText()), pollData)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void populateConfigButtonList(Map<GUIComponentButton, JSONConfigEntry<Boolean>> configButtons, Object configObject) {
        configButtons.clear();
        for (Field field : configObject.getClass().getFields()) {
            if (field.getType().equals(JSONConfigEntry.class)) {
                try {
                    JSONConfigEntry<?> configEntry = (JSONConfigEntry<?>) field.get(configObject);
                    if (configEntry.value.getClass().equals(Boolean.class)) {
                        GUIComponentButton button = new GUIComponentButton(guiLeft + 85 + 120 * (configButtons.size() % 2), guiTop + 20 + 16 * (configButtons.size() / 2), 40, 16, String.valueOf(configEntry.value)) {
                            @Override
                            public void onClicked(boolean leftSide) {
                                configButtons.get(this).value = !Boolean.parseBoolean(text);
                                ConfigSystem.saveToDisk();
                                text = String.valueOf(configButtons.get(this).value);
                            }

                            @Override
                            public List<String> getTooltipText() {
                                List<String> tooltipText = new ArrayList<>();
                                tooltipText.add(configEntry.comment);
                                return tooltipText;
                            }
                        };
                        addComponent(button);
                        configButtons.put(button, (JSONConfigEntry<Boolean>) configEntry);
                        addComponent(new GUIComponentLabel(button.constructedX - 75, button.constructedY + 5, ColorRGB.BLACK, field.getName()).setButton(button));
                    }
                } catch (Exception e) {
                    //How the heck does this even happen?
                }
            }
        }
    }
}
