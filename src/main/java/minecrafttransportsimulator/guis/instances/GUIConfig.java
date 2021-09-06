package minecrafttransportsimulator.guis.instances;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.jsondefs.JSONConfig.JSONConfigEntry;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfaceInput;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboardDynamic;

public class GUIConfig extends AGUIBase{
	//Global variables.
	private GUIComponentButton renderConfigScreenButton;
	private GUIComponentButton controlConfigScreenButton;
	private GUIComponentButton controlScreenButton;
	
	//Config variables.
	private boolean configuringControls = true;
	private boolean configuringRendering = false;
	private Map<GUIComponentButton, JSONConfigEntry<Boolean>> renderConfigButtons = new HashMap<GUIComponentButton, JSONConfigEntry<Boolean>>();
	private Map<GUIComponentButton, JSONConfigEntry<Boolean>> controlConfigButtons = new HashMap<GUIComponentButton, JSONConfigEntry<Boolean>>();
	
	//Keybind selection variables.
	private String vehicleConfiguring = "";
	private String[] vehicleTypes = new String[]{"car", "aircraft"};
	private Map<GUIComponentButton, String> vehicleSelectionButtons = new HashMap<GUIComponentButton, String>();
	private GUIComponentLabel vehicleSelectionFaultLabel;
	private GUIComponentButton finishKeyboardBindingsButton;
	
	//Keyboard assignment variables.
	private boolean configuringKeyboard;
	private Map<String, Map<GUIComponentTextBox, ControlsKeyboard>> keyboardBoxes = new HashMap<String, Map<GUIComponentTextBox, ControlsKeyboard>>();
	private Map<String, Map<GUIComponentLabel, ControlsKeyboardDynamic>> keyboardLabels = new HashMap<String, Map<GUIComponentLabel, ControlsKeyboardDynamic>>();
	
	//Joystick selection variables.
	private Map<GUIComponentButton, String> joystickSelectionButtons = new HashMap<GUIComponentButton, String>();
	
	//Joystick component selection variables.
	private int scrollSpot = 0;
	private int selectedJoystickComponentCount = 0;
	private String selectedJoystick;
	private GUIComponentButton componentListUpButton;
	private GUIComponentButton componentListDownButton;
	private GUIComponentButton deadzone_moreButton;
	private GUIComponentButton deadzone_lessButton;
	private GUIComponentTextBox deadzone_text;
	private List<JoystickControlButton> joystickComponentSelectionButtons = new ArrayList<JoystickControlButton>();
	
	//Joystick assignment variables.
	private boolean assigningDigital;
	private int joystickComponentId = -1;
	private GUIComponentButton cancelAssignmentButton;
	private GUIComponentButton clearAssignmentButton;
	
	//Joystick digital assignment variables.
	private Map<String, Map<GUIComponentButton, ControlsJoystick>> digitalAssignButtons = new HashMap<String, Map<GUIComponentButton, ControlsJoystick>>();
	
	//Joystick analog assignment variables.
	private Map<String, Map<GUIComponentButton, ControlsJoystick>> analogAssignButtons = new HashMap<String, Map<GUIComponentButton, ControlsJoystick>>();
	
	//Joystick analog calibration variables.
	private boolean calibrating = false;
	private ControlsJoystick controlCalibrating;
	private GUIComponentButton confirmBoundsButton;
	private GUIComponentButton invertAxisButton;
	private GUIComponentTextBox axisMinBoundsTextBox;
	private GUIComponentTextBox axisMaxBoundsTextBox;
	
	public GUIConfig(){
		if(!InterfaceInput.isJoystickSupportEnabled()){
			InterfaceInput.initJoysticks();
		}
	}
	
	@Override
	public void setupComponents(int guiLeft, int guiTop){
		//Global header buttons.
		addButton(renderConfigScreenButton = new GUIComponentButton(guiLeft + 0, guiTop - 20, 85, InterfaceCore.translate("gui.config.header.config.rendering")){
			@Override
			public void onClicked(){
				configuringControls = false;
				configuringRendering = true;
				vehicleConfiguring = "";
				selectedJoystick = null;
				scrollSpot = 0;
				joystickComponentId = -1;
				calibrating = false;
			}
		});
		addButton(controlConfigScreenButton = new GUIComponentButton(guiLeft + 85, guiTop - 20, 85, InterfaceCore.translate("gui.config.header.config.controls")){
			@Override
			public void onClicked(){
				configuringControls = false;
				configuringRendering = false; 
				vehicleConfiguring = "";
				selectedJoystick = null;
				scrollSpot = 0;
				joystickComponentId = -1;
				calibrating = false;
			}
		});
		addButton(controlScreenButton = new GUIComponentButton(guiLeft + 171, guiTop - 20, 85, InterfaceCore.translate("gui.config.header.controls")){@Override
		public void onClicked(){configuringControls = true;}});
		
		
		//Config buttons and text.
		//We have two sets here.  One for rendering, one for controls.
		populateConfigButtonList(guiLeft, guiTop, renderConfigButtons, ConfigSystem.configObject.clientRendering);
		populateConfigButtonList(guiLeft, guiTop, controlConfigButtons, ConfigSystem.configObject.clientControls);
		
		
		//Vehicle selection buttons and text.
		//We only have two types.  Car and aircraft.
		vehicleSelectionButtons.clear();		
		addLabel(vehicleSelectionFaultLabel = new GUIComponentLabel(guiLeft+10, guiTop+90, ColorRGB.BLACK, InterfaceInput.isJoystickSupportBlocked() ? InterfaceCore.translate("gui.config.joystick.disabled") : InterfaceCore.translate("gui.config.joystick.error"), null, TextPosition.LEFT_ALIGNED, 240, 1.0F, false));
		for(String vehicleType : vehicleTypes){
			GUIComponentButton buttonKeyboard = new GUIComponentButton(guiLeft + 68, guiTop + 30 + 20*(vehicleSelectionButtons.size()/(InterfaceInput.isJoystickSupportEnabled() ? 2 : 1)), 120, InterfaceCore.translate("gui.config.controls." + vehicleType + ".keyboard")){
				@Override
				public void onClicked(){
					String lookupString = vehicleSelectionButtons.get(this);
					vehicleConfiguring = lookupString.substring(0, lookupString.indexOf('.'));
					configuringKeyboard = true;
				}
			};
			vehicleSelectionButtons.put(buttonKeyboard, vehicleType + ".keyboard");
			addButton(buttonKeyboard);
			//Add screen label if we haven't already.
			if(vehicleSelectionButtons.size() == 1){
				addLabel(new GUIComponentLabel(guiLeft+20, guiTop+10, ColorRGB.BLACK, InterfaceCore.translate("gui.config.controls.title")).setButton(buttonKeyboard));
			}
			
			GUIComponentButton buttonJoystick = new GUIComponentButton(guiLeft + 68, guiTop + 90 + 20*(vehicleSelectionButtons.size()/2), 120, InterfaceCore.translate("gui.config.controls." + vehicleType + ".joystick")){
				@Override
				public void onClicked(){
					String lookupString = vehicleSelectionButtons.get(this);
					vehicleConfiguring = lookupString.substring(0, lookupString.indexOf('.'));
					configuringKeyboard = false;
				}
			};
			vehicleSelectionButtons.put(buttonJoystick,  vehicleType + ".joystick");
			addButton(buttonJoystick);
		}
		
		
		
		//Keyboard buttons and text.
		keyboardBoxes.clear();
		keyboardLabels.clear();
		for(String vehicleType : vehicleTypes){
			//First add the editable controls.
			int verticalOffset = 20;
			int horizontalOffset = 80;
			Map<GUIComponentTextBox, ControlsKeyboard> boxesForVehicle = new HashMap<GUIComponentTextBox, ControlsKeyboard>();
			for(ControlsKeyboard keyboardControl : ControlSystem.ControlsKeyboard.values()){
				if(keyboardControl.systemName.contains(vehicleType)){
					//First create the text box for input.
					GUIComponentTextBox box = new GUIComponentTextBox(guiLeft + horizontalOffset, guiTop + verticalOffset, 40, "", 10, ColorRGB.WHITE, ColorRGB.BLACK, 5){
						@Override
						public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
							setText(InterfaceInput.getNameForKeyCode(typedCode));
		        			keyboardBoxes.get(vehicleConfiguring).get(this).config.keyCode =  typedCode;
		        			ConfigSystem.saveToDisk();
		        			focused = false;
						};
					};
					boxesForVehicle.put(box, keyboardControl);
					addTextBox(box);
					
					//Now create the label.
					addLabel(new GUIComponentLabel(box.x - 70, box.y + 2, ColorRGB.BLACK, keyboardControl.translatedName + ":").setBox(box));
					
					verticalOffset += 11;
					if(verticalOffset > 20 + 11*9){
						verticalOffset = 20;
						horizontalOffset += 120;
					}
				}
			}
			keyboardBoxes.put(vehicleType, boxesForVehicle);
			
			//Now add the dynamic controls.
			byte offset = 0;
			Map<GUIComponentLabel, ControlsKeyboardDynamic> dynamicLabels = new HashMap<GUIComponentLabel, ControlsKeyboardDynamic>();
			for(ControlsKeyboardDynamic dynamicControl : ControlsKeyboardDynamic.values()){
				if(dynamicControl.name().toLowerCase().contains(vehicleType)){
					GUIComponentLabel label = new GUIComponentLabel(guiLeft + 10, guiTop + 135 + offset, ColorRGB.BLACK, ""); 
					dynamicLabels.put(label, dynamicControl);
					addLabel(label);
					offset+=11;
				}
			}
			keyboardLabels.put(vehicleType, dynamicLabels);
		}
		addButton(finishKeyboardBindingsButton = new GUIComponentButton(guiLeft + 180, guiTop + 150, 50, InterfaceCore.translate("gui.config.controls.confirm")){@Override
		public void onClicked(){vehicleConfiguring = "";}});
		
		
		
		//Joystick selection buttons and text.
		joystickSelectionButtons.clear();
		for(String joystick : InterfaceInput.getAllJoysticks()){
			GUIComponentButton button = new GUIComponentButton(guiLeft + 10, guiTop + 40 + 20*joystickSelectionButtons.size(), 235, String.format(" %-30.28s", joystick), 20, false){@Override
			public void onClicked(){
				selectedJoystick = joystickSelectionButtons.get(this);
				selectedJoystickComponentCount = InterfaceInput.getJoystickComponentCount(selectedJoystick);
			}};
			joystickSelectionButtons.put(button, joystick);
			addButton(button);
			
			//Link the header text to the first joystick button.
			if(joystickSelectionButtons.size() == 1){
				addLabel(new GUIComponentLabel(guiLeft+20, guiTop+10, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.select")).setButton(button));
				addLabel(new GUIComponentLabel(guiLeft+15, guiTop+25, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.name")).setButton(button));
			}
		}
		
		
		
		//Joystick component selection buttons and text.
		joystickComponentSelectionButtons.clear();
		for(int i=0; i<9; ++i){
			JoystickControlButton button = new JoystickControlButton(guiLeft+10, guiTop+45+15*i);
			joystickComponentSelectionButtons.add(button);
			addButton(button);
		}
		addButton(componentListUpButton = new GUIComponentButton(guiLeft + 225, guiTop + 45, 20, "/\\"){@Override
		public void onClicked(){scrollSpot -= 9;}});
		addButton(componentListDownButton = new GUIComponentButton(guiLeft + 225, guiTop + 155, 20, "\\/"){@Override
		public void onClicked(){scrollSpot += 9;}});
		addButton(deadzone_lessButton = new GUIComponentButton(guiLeft + 100, guiTop + 10, 20, "<"){@Override
		public void onClicked(){ConfigSystem.configObject.clientControls.joystickDeadZone.value = ((ConfigSystem.configObject.clientControls.joystickDeadZone.value*100 - 1)/100F);}});
		addButton(deadzone_moreButton = new GUIComponentButton(guiLeft + 220, guiTop + 10, 20, ">"){@Override
		public void onClicked(){ConfigSystem.configObject.clientControls.joystickDeadZone.value = ((ConfigSystem.configObject.clientControls.joystickDeadZone.value*100 + 1)/100F);}});
		addTextBox(deadzone_text = new GUIComponentTextBox(guiLeft + 120, guiTop + 10, 100, ""));
		
		addLabel(new GUIComponentLabel(guiLeft+15, guiTop+20, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.mapping")).setButton(componentListUpButton));
		addLabel(new GUIComponentLabel(guiLeft+15, guiTop+35, ColorRGB.BLACK, "#").setButton(componentListUpButton));
		addLabel(new GUIComponentLabel(guiLeft+30, guiTop+35, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.name")).setButton(componentListUpButton));
		addLabel(new GUIComponentLabel(guiLeft+100, guiTop+35, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.state")).setButton(componentListUpButton));
		addLabel(new GUIComponentLabel(guiLeft+140, guiTop+35, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.assignment")).setButton(componentListUpButton));

		
		
		//Joystick assignment buttons and text.
		//Global buttons and labels for digital and analog.
		addButton(cancelAssignmentButton = new GUIComponentButton(guiLeft + 125, guiTop + 160, 100, InterfaceCore.translate("gui.config.joystick.cancel")){@Override
		public void onClicked(){joystickComponentId = -1; calibrating = false;}});
		addButton(clearAssignmentButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, InterfaceCore.translate("gui.config.joystick.clear")){
			@Override
			public void onClicked(){
				for(ControlsJoystick joystickControl : ControlsJoystick.values()){
					if(selectedJoystick.equals(joystickControl.config.joystickName)){
						if((joystickControl.isAxis ^ assigningDigital) && joystickControl.config.buttonIndex == joystickComponentId && joystickControl.systemName.startsWith(vehicleConfiguring)){
							joystickControl.clearControl();
						}
					}
				}
				joystickComponentId = -1;
			}
		});
		addLabel(new GUIComponentLabel(guiLeft+20, guiTop+10, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.choosemap")).setButton(clearAssignmentButton));
		
		
		
		//Digital and analog buttons.
		digitalAssignButtons.clear();
		analogAssignButtons.clear();
		for(String vehicleType : vehicleTypes){
			short leftOffsetDigital = 0;
			short topOffsetDigital = 0;
			short topOffsetAnalog = 0;
			Map<GUIComponentButton, ControlsJoystick> digitalControlButtons = new HashMap<GUIComponentButton, ControlsJoystick>();
			Map<GUIComponentButton, ControlsJoystick> analogControlButtons = new HashMap<GUIComponentButton, ControlsJoystick>();
			for(ControlsJoystick joystickControl : ControlsJoystick.values()){
				if(joystickControl.systemName.startsWith(vehicleType)){
					if(!joystickControl.isAxis){
						GUIComponentButton button = new GUIComponentButton(guiLeft + 8 + leftOffsetDigital, guiTop + 20 + topOffsetDigital, 80, joystickControl.translatedName, 15, true){
							@Override
							public void onClicked(){
								digitalAssignButtons.get(vehicleConfiguring).get(this).setControl(selectedJoystick, joystickComponentId);
								joystickComponentId = -1;
							}
						};
						digitalControlButtons.put(button, joystickControl);
						addButton(button);
						topOffsetDigital += button.height;
					}else{
						GUIComponentButton button = new GUIComponentButton(guiLeft + 85, guiTop + 40 + topOffsetAnalog, 80, joystickControl.translatedName, 20, true){
							@Override
							public void onClicked(){
								controlCalibrating = analogAssignButtons.get(vehicleConfiguring).get(this);
								axisMinBoundsTextBox.setText("0.0");
								axisMaxBoundsTextBox.setText("0.0");
								calibrating = true;
							}
						};
						analogControlButtons.put(button, joystickControl);
						addButton(button);
						topOffsetAnalog += button.height;
					}
				}
				if(topOffsetDigital >= 135){
					topOffsetDigital = 0;
					leftOffsetDigital += 80;
				}
			}
			digitalAssignButtons.put(vehicleType, digitalControlButtons);
			analogAssignButtons.put(vehicleType, analogControlButtons);
		}
		
		
		
		//Analog calibration components.
		addButton(confirmBoundsButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, InterfaceCore.translate("gui.config.joystick.confirm")){
			@Override
			public void onClicked(){
				boolean isInverted = invertAxisButton.text.contains(InterfaceCore.translate("gui.config.joystick.invert"));
				controlCalibrating.setAxisControl(selectedJoystick, joystickComponentId, Double.valueOf(axisMinBoundsTextBox.getText()), Double.valueOf(axisMaxBoundsTextBox.getText()), isInverted);
				joystickComponentId = -1;
				calibrating = false;
			}
		});
		addButton(invertAxisButton = new GUIComponentButton(guiLeft + 50, guiTop + 120, 150, InterfaceCore.translate("gui.config.joystick.axismode") + InterfaceCore.translate("gui.config.joystick.normal")){
			@Override
			public void onClicked(){
				if(text.contains(InterfaceCore.translate("gui.config.joystick.invert"))){
					text = InterfaceCore.translate("gui.config.joystick.axismode") + InterfaceCore.translate("gui.config.joystick.normal");
				}else{
					text = InterfaceCore.translate("gui.config.joystick.axismode") + InterfaceCore.translate("gui.config.joystick.invert");
				}
			}
		});
		addTextBox(axisMinBoundsTextBox = new GUIComponentTextBox(guiLeft+50, guiTop+90, 150, "0.0"));
		axisMinBoundsTextBox.enabled = false;
		addTextBox(axisMaxBoundsTextBox = new GUIComponentTextBox(guiLeft+50, guiTop+60, 150, "0.0"));
		axisMaxBoundsTextBox.enabled = false;
		addLabel(new GUIComponentLabel(guiLeft+20, guiTop+10, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.calibrate1")).setButton(confirmBoundsButton));
		addLabel(new GUIComponentLabel(guiLeft+20, guiTop+20, ColorRGB.BLACK, InterfaceCore.translate("gui.config.joystick.calibrate2")).setButton(confirmBoundsButton));
	}
	
	@Override
	public void setStates(){
		//Global headers are just toggles depending on operation.
		renderConfigScreenButton.enabled = configuringControls || (!configuringControls && !configuringRendering);
		controlConfigScreenButton.enabled = configuringControls || (!configuringControls && configuringRendering);
		controlScreenButton.enabled = !configuringControls;
		
		
		//If we are not configuring controls, render the appropriate config buttons and labels.
		for(GUIComponentButton button : renderConfigButtons.keySet()){
			button.visible = !renderConfigScreenButton.enabled;
		}
		for(GUIComponentButton button : controlConfigButtons.keySet()){
			button.visible = !controlConfigScreenButton.enabled;
		}
		
		
		//If we are configuring controls, and haven't selected a vehicle, render the vehicle selection components.
		vehicleSelectionFaultLabel.visible = !InterfaceInput.isJoystickSupportEnabled() && configuringControls && !configuringKeyboard;
		for(GUIComponentButton button : vehicleSelectionButtons.keySet()){
			button.visible = InterfaceInput.isJoystickSupportEnabled() && configuringControls && vehicleConfiguring.isEmpty();
		}
		
		
		
		//If we have selected a vehicle, and are configuring a keyboard, render the keyboard controls.
		//Only enable the boxes and labels for the vehicle we are configuring, however.
		//If a box is focused, we should set the text to a blank value.
		finishKeyboardBindingsButton.visible = configuringControls && !vehicleConfiguring.isEmpty() && configuringKeyboard;
		for(String vehicleType : keyboardBoxes.keySet()){
			for(GUIComponentTextBox textBox : keyboardBoxes.get(vehicleType).keySet()){
				textBox.visible = finishKeyboardBindingsButton.visible && vehicleType.equals(vehicleConfiguring);
				if(textBox.focused){
					textBox.setText("");
				}else{
					textBox.setText(InterfaceInput.getNameForKeyCode(keyboardBoxes.get(vehicleType).get(textBox).config.keyCode));
				}
			}
			for(GUIComponentLabel label : keyboardLabels.get(vehicleType).keySet()){
				label.visible = finishKeyboardBindingsButton.visible && vehicleType.equals(vehicleConfiguring);
				ControlsKeyboardDynamic dynamicControl = keyboardLabels.get(vehicleType).get(label);
				label.text = dynamicControl.translatedName + ": " + InterfaceInput.getNameForKeyCode(dynamicControl.modControl.config.keyCode) + " + " + InterfaceInput.getNameForKeyCode(dynamicControl.mainControl.config.keyCode);
			}
		}
		
		
		
		//If we have selected a vehicle, and are not configuring a keyboard, but haven't selected a joystick
		//make the joystick selection screen components visible.
		for(GUIComponentButton button : joystickSelectionButtons.keySet()){
			button.visible = configuringControls && !configuringKeyboard && !vehicleConfiguring.isEmpty() && selectedJoystick == null;
		}
		
		
		
		//If we have selected a joystick, but not a component, make the component selection buttons visible.
		boolean onComponentSelectScreen = selectedJoystick != null && joystickComponentId == -1;
		for(byte i=0; i<9; ++i){
			GUIComponentButton button = joystickComponentSelectionButtons.get(i);
			button.visible = onComponentSelectScreen && i + scrollSpot < selectedJoystickComponentCount;
			if(button.visible){
				//Set basic button text.
				int controlIndex = i+scrollSpot;
				button.text = String.format(" %02d  %-15.15s", controlIndex+1, InterfaceInput.getJoystickComponentName(selectedJoystick, controlIndex));
				
				//If this joystick is assigned to a control, append that to the text string.
				for(ControlsJoystick joystickControl : ControlsJoystick.values()){
					if(selectedJoystick.equals(joystickControl.config.joystickName)){
						if(joystickControl.config.buttonIndex == controlIndex && joystickControl.systemName.startsWith(vehicleConfiguring)){
							button.text += String.format("          %s", joystickControl.translatedName);
						}
					}
				}
			}
		}
		componentListUpButton.visible = onComponentSelectScreen;
		componentListDownButton.visible = onComponentSelectScreen;
		deadzone_lessButton.visible = onComponentSelectScreen;
		deadzone_moreButton.visible = onComponentSelectScreen;
		deadzone_text.visible = onComponentSelectScreen;
		if(onComponentSelectScreen){
			componentListUpButton.enabled = scrollSpot - 9 >= 0;
			componentListDownButton.enabled = scrollSpot + 9 < selectedJoystickComponentCount;
			deadzone_lessButton.enabled = ConfigSystem.configObject.clientControls.joystickDeadZone.value > 0;
			deadzone_moreButton.enabled = ConfigSystem.configObject.clientControls.joystickDeadZone.value < 1;
			deadzone_text.enabled = false;
			deadzone_text.setText(InterfaceCore.translate("gui.config.joystick.deadzone") + " " + String.valueOf(ConfigSystem.configObject.clientControls.joystickDeadZone.value));
		}
		
		
		
		//If we have selected a component, render the assignment buttons.
		//These are global, so they are always visible.
		cancelAssignmentButton.visible = joystickComponentId != -1;
		cancelAssignmentButton.enabled = cancelAssignmentButton.visible;
		clearAssignmentButton.visible = cancelAssignmentButton.visible && !calibrating;
		clearAssignmentButton.enabled = cancelAssignmentButton.visible && !calibrating;
		
		//Set states of digital buttons.
		for(String vehicleType : digitalAssignButtons.keySet()){
			for(GUIComponentButton button : digitalAssignButtons.get(vehicleType).keySet()){
				button.visible = joystickComponentId != -1 && vehicleConfiguring.equals(vehicleType) && assigningDigital;
			}
		}
		
		//Set status of analog buttons.
		for(String vehicleType : analogAssignButtons.keySet()){
			for(GUIComponentButton button : analogAssignButtons.get(vehicleType).keySet()){
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
		if(calibrating){
			float pollData = InterfaceInput.getJoystickAxisValue(selectedJoystick, joystickComponentId);
			if(pollData < 0){
				axisMinBoundsTextBox.setText(String.valueOf(Math.min(Double.valueOf(axisMinBoundsTextBox.getText()), pollData)));
			}else{
				axisMaxBoundsTextBox.setText(String.valueOf(Math.max(Double.valueOf(axisMaxBoundsTextBox.getText()), pollData)));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void populateConfigButtonList(int guiLeft, int guiTop, Map<GUIComponentButton, JSONConfigEntry<Boolean>> configButtons, Object configObject){
		configButtons.clear();
		for(Field field : configObject.getClass().getFields()){
			if(field.getType().equals(JSONConfigEntry.class)){
				try{
					JSONConfigEntry<?> configEntry = (JSONConfigEntry<?>) field.get(configObject);
					if(configEntry.value.getClass().equals(Boolean.class)){
						GUIComponentButton button = new GUIComponentButton(guiLeft + 85 + 120*(configButtons.size()%2), guiTop + 20 + 16*(configButtons.size()/2), 40, String.valueOf(configEntry.value), 16, true){
							@Override
							public void onClicked(){
								configButtons.get(this).value = !Boolean.valueOf(text);
								ConfigSystem.saveToDisk();
								text = String.valueOf(configButtons.get(this).value);
							}
							
							@Override
							public void renderTooltip(AGUIBase gui, int mouseX, int mouseY){
								if(visible){
									if(mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height){
										InterfaceGUI.drawGenericTooltip(gui, mouseX, mouseY, configEntry.comment);
									}
								}
							}
						};
						addButton(button);
						configButtons.put(button, (JSONConfigEntry<Boolean>) configEntry);
						addLabel(new GUIComponentLabel(button.x - 75, button.y + 5, ColorRGB.BLACK, field.getName()).setButton(button));
					}
				}catch(Exception e){
					//How the heck does this even happen?
				}
			}
		}
	}
	
	
	/**Custom button class.  We use this here to render the state of the joystick polled
	 * on top of the button after after we render the button.
	 *
	 * @author don_bruce
	 */
	private class JoystickControlButton extends GUIComponentButton{

		public JoystickControlButton(int x, int y){
			super(x, y, 215, "", 15, false);
		}

		@Override
		public void onClicked(){
			joystickComponentId = joystickComponentSelectionButtons.indexOf(this) + scrollSpot;
			if(InterfaceInput.isJoystickComponentAxis(selectedJoystick, joystickComponentId)){
				assigningDigital = false;
			}else{
				assigningDigital = true;
			}
		}
		
		@Override
		public void renderText(){
			super.renderText();
			if(visible){
				//We need to manually draw the joystick state here.  Do so via built-in rectangle render method
				//as that is render-safe so we won't mess up any texture operations.
				int buttonIndex = joystickComponentSelectionButtons.indexOf(this);
				int controlIndex = buttonIndex + scrollSpot;
				float pollData = InterfaceInput.getJoystickAxisValue(selectedJoystick, controlIndex);
				if(InterfaceInput.isJoystickComponentAxis(selectedJoystick, controlIndex)){
					InterfaceGUI.renderRectangle(x + 85, y + 2, 40, height - 4, ColorRGB.BLACK);
					if(Math.abs(pollData) > ConfigSystem.configObject.clientControls.joystickDeadZone.value){
						InterfaceGUI.renderRectangle(x + 85 + 20, y + 2, (int) (pollData*20), height - 4, ColorRGB.RED);
					}
				}else{
					if(pollData == 0){
						InterfaceGUI.renderRectangle(x + 85 + 20 - (height - 4)/2, y + 2, height - 4, height - 4, ColorRGB.BLACK);
					}else if(pollData == 1){
						InterfaceGUI.renderRectangle(x + 85 + 20 - (height - 4)/2, y + 2, height - 4, height - 4, ColorRGB.RED);
					}else{
						//For digitals with fractions like hats.
						InterfaceGUI.renderRectangle(x + 85 + 20 - (height - 4)/2, y + 2, height - 4, height - 4, ColorRGB.YELLOW);
					}
				}
			}
		}
	}
}
