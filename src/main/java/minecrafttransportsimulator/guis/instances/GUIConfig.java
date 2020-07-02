package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigBoolean;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboardDynamic;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperInput;

public class GUIConfig extends AGUIBase{
	//Global variables.
	private GUIComponentButton configScreenButton;
	private GUIComponentButton controlScreenButton;
	
	//Config variables.
	private boolean configuringControls = true;
	private Map<GUIComponentButton, ConfigBoolean> configButtons = new HashMap<GUIComponentButton, ConfigBoolean>();
	
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
	}
	
	@Override
	public void setupComponents(int guiLeft, int guiTop){
		//Global header buttons.
		addButton(configScreenButton = new GUIComponentButton(guiLeft + 0, guiTop - 20, 128, WrapperGUI.translate("gui.config.header.config")){
			public void onClicked(){
				configuringControls = false; 
				vehicleConfiguring = "";
				selectedJoystick = null;
				scrollSpot = 0;
				joystickComponentId = -1;
				calibrating = false;
			}
		});
		addButton(controlScreenButton = new GUIComponentButton(guiLeft + 128, guiTop - 20, 128, WrapperGUI.translate("gui.config.header.controls")){public void onClicked(){configuringControls = true;}});
		
		
		
		//Config buttons and text.
		configButtons.clear();
		for(Field field : ConfigSystem.configObject.client.getClass().getFields()){
			if(field.getType().equals(ConfigBoolean.class)){
				try{
					ConfigBoolean config = (ConfigBoolean) field.get(ConfigSystem.configObject.client);
					GUIComponentButton button = new GUIComponentButton(guiLeft + 85 + 120*(configButtons.size()%2), guiTop + 20 + 20*(configButtons.size()/2), 40, String.valueOf(config.value)){
						@Override
						public void onClicked(){
							configButtons.get(this).value = !Boolean.valueOf(text);
							ConfigSystem.saveToDisk();
							text = String.valueOf(configButtons.get(this).value);
						}
						
						@Override
						public void renderTooltip(WrapperGUI wrapper, int mouseX, int mouseY){
							if(visible){
								if(mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height){
									wrapper.drawGenericTooltip(config.comment, mouseX, mouseY);
								}
							}
						}
					};
					addButton(button);
					configButtons.put(button, config);
					addLabel(new GUIComponentLabel(button.x - 75, button.y + 5, Color.BLACK, field.getName()).setButton(button));
				}catch(Exception e){
					//How the heck does this even happen?
				}
			}
		}
		
		//Vehicle selection buttons and text.
		//We only have two types.  Car and aircraft.
		vehicleSelectionButtons.clear();		
		addLabel(vehicleSelectionFaultLabel = new GUIComponentLabel(guiLeft+10, guiTop+110, Color.BLACK, WrapperGUI.translate("gui.config.joystick.error"), 1.0F, false, false, 240));
		for(String vehicleType : vehicleTypes){
			GUIComponentButton buttonKeyboard = new GUIComponentButton(guiLeft + 68, guiTop + 30 + 20*(vehicleSelectionButtons.size()/(WrapperInput.isJoystickSupportEnabled() ? 2 : 1)), 120, WrapperGUI.translate("gui.config.controls." + vehicleType + ".keyboard")){
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
				addLabel(new GUIComponentLabel(guiLeft+20, guiTop+10, Color.BLACK, WrapperGUI.translate("gui.config.controls.title")).setButton(buttonKeyboard));
			}
			
			if(WrapperInput.isJoystickSupportEnabled()){
				GUIComponentButton buttonJoystick = new GUIComponentButton(guiLeft + 68, guiTop + 90 + 20*(vehicleSelectionButtons.size()/2), 120, WrapperGUI.translate("gui.config.controls." + vehicleType + ".joystick")){
					public void onClicked(){
						String lookupString = vehicleSelectionButtons.get(this);
						vehicleConfiguring = lookupString.substring(0, lookupString.indexOf('.'));
						configuringKeyboard = false;
					}
				};
				vehicleSelectionButtons.put(buttonJoystick,  vehicleType + ".joystick");
				addButton(buttonJoystick);
			}
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
					GUIComponentTextBox box = new GUIComponentTextBox(guiLeft + horizontalOffset, guiTop + verticalOffset, 40, "", 10, Color.WHITE, Color.BLACK, 5){
						public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
							setText(WrapperInput.getNameForKeyCode(typedCode));
		        			keyboardBoxes.get(vehicleConfiguring).get(this).config.keyCode =  typedCode;
		        			ConfigSystem.saveToDisk();
		        			focused = false;
						};
					};
					boxesForVehicle.put(box, keyboardControl);
					addTextBox(box);
					
					//Now create the label.
					addLabel(new GUIComponentLabel(box.x - 70, box.y + 2, Color.BLACK, keyboardControl.translatedName + ":").setBox(box));
					
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
					GUIComponentLabel label = new GUIComponentLabel(guiLeft + 10, guiTop + 135 + offset, Color.BLACK, ""); 
					dynamicLabels.put(label, dynamicControl);
					addLabel(label);
					offset+=11;
				}
			}
			keyboardLabels.put(vehicleType, dynamicLabels);
		}
		addButton(finishKeyboardBindingsButton = new GUIComponentButton(guiLeft + 180, guiTop + 150, 50, WrapperGUI.translate("gui.config.controls.confirm")){public void onClicked(){vehicleConfiguring = "";}});
		
		
		
		//Joystick selection buttons and text.
		joystickSelectionButtons.clear();
		for(String joystick : WrapperInput.getAllJoysticks()){
			GUIComponentButton button = new GUIComponentButton(guiLeft + 10, guiTop + 40 + 20*joystickSelectionButtons.size(), 235, String.format(" %-30.28s", joystick), 20, false){public void onClicked(){selectedJoystick = joystickSelectionButtons.get(this);}};
			joystickSelectionButtons.put(button, joystick);
			addButton(button);
			
			//Link the header text to the first joystick button.
			if(joystickSelectionButtons.size() == 1){
				addLabel(new GUIComponentLabel(guiLeft+20, guiTop+10, Color.BLACK, WrapperGUI.translate("gui.config.joystick.select")).setButton(button));
				addLabel(new GUIComponentLabel(guiLeft+15, guiTop+25, Color.BLACK, WrapperGUI.translate("gui.config.joystick.name")).setButton(button));
			}
		}
		
		
		
		//Joystick component selection buttons and text.
		joystickComponentSelectionButtons.clear();
		for(int i=0; i<9; ++i){
			JoystickControlButton button = new JoystickControlButton(guiLeft+10, guiTop+45+15*i);
			joystickComponentSelectionButtons.add(button);
			addButton(button);
		}
		addButton(componentListUpButton = new GUIComponentButton(guiLeft + 225, guiTop + 45, 20, "/\\"){public void onClicked(){scrollSpot -= 9;}});
		addButton(componentListDownButton = new GUIComponentButton(guiLeft + 225, guiTop + 155, 20, "\\/"){public void onClicked(){scrollSpot += 9;}});
		addButton(deadzone_lessButton = new GUIComponentButton(guiLeft + 100, guiTop + 10, 20, "<"){public void onClicked(){ConfigSystem.configObject.client.joystickDeadZone.value = ((ConfigSystem.configObject.client.joystickDeadZone.value*100 - 1)/100F);}});
		addButton(deadzone_moreButton = new GUIComponentButton(guiLeft + 220, guiTop + 10, 20, ">"){public void onClicked(){ConfigSystem.configObject.client.joystickDeadZone.value = ((ConfigSystem.configObject.client.joystickDeadZone.value*100 + 1)/100F);}});
		addTextBox(deadzone_text = new GUIComponentTextBox(guiLeft + 120, guiTop + 10, 100, ""));
		
		addLabel(new GUIComponentLabel(guiLeft+15, guiTop+20, Color.BLACK, WrapperGUI.translate("gui.config.joystick.mapping")).setButton(componentListUpButton));
		addLabel(new GUIComponentLabel(guiLeft+15, guiTop+35, Color.BLACK, "#").setButton(componentListUpButton));
		addLabel(new GUIComponentLabel(guiLeft+30, guiTop+35, Color.BLACK, WrapperGUI.translate("gui.config.joystick.name")).setButton(componentListUpButton));
		addLabel(new GUIComponentLabel(guiLeft+100, guiTop+35, Color.BLACK, WrapperGUI.translate("gui.config.joystick.state")).setButton(componentListUpButton));
		addLabel(new GUIComponentLabel(guiLeft+140, guiTop+35, Color.BLACK, WrapperGUI.translate("gui.config.joystick.assignment")).setButton(componentListUpButton));

		
		
		//Joystick assignment buttons and text.
		//Global buttons and labels for digital and analog.
		addButton(cancelAssignmentButton = new GUIComponentButton(guiLeft + 125, guiTop + 160, 100, WrapperGUI.translate("gui.config.joystick.cancel")){public void onClicked(){joystickComponentId = -1; calibrating = false;}});
		addButton(clearAssignmentButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, WrapperGUI.translate("gui.config.joystick.clear")){
			public void onClicked(){
				for(ControlsJoystick joystickControl : ControlsJoystick.values()){
					if(selectedJoystick.equals(joystickControl.config.joystickName)){
						if(joystickControl.config.buttonIndex == joystickComponentId && joystickControl.systemName.startsWith(vehicleConfiguring)){
							joystickControl.clearControl();
						}
					}
				}
				joystickComponentId = -1;
			}
		});
		addLabel(new GUIComponentLabel(guiLeft+20, guiTop+10, Color.BLACK, WrapperGUI.translate("gui.config.joystick.choosemap")).setButton(clearAssignmentButton));
		
		
		
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
		addButton(confirmBoundsButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, WrapperGUI.translate("gui.config.joystick.confirm")){
			public void onClicked(){
				boolean isInverted = invertAxisButton.text.contains(WrapperGUI.translate("gui.config.joystick.invert"));
				controlCalibrating.setAxisControl(selectedJoystick, joystickComponentId, Double.valueOf(axisMinBoundsTextBox.getText()), Double.valueOf(axisMaxBoundsTextBox.getText()), isInverted);
				joystickComponentId = -1;
				calibrating = false;
			}
		});
		addButton(invertAxisButton = new GUIComponentButton(guiLeft + 50, guiTop + 120, 150, WrapperGUI.translate("gui.config.joystick.axismode") + WrapperGUI.translate("gui.config.joystick.normal")){
			public void onClicked(){
				if(text.contains(WrapperGUI.translate("gui.config.joystick.invert"))){
					text = WrapperGUI.translate("gui.config.joystick.axismode") + WrapperGUI.translate("gui.config.joystick.normal");
				}else{
					text = WrapperGUI.translate("gui.config.joystick.axismode") + WrapperGUI.translate("gui.config.joystick.invert");
				}
			}
		});
		addTextBox(axisMinBoundsTextBox = new GUIComponentTextBox(guiLeft+50, guiTop+90, 150, "0.0"));
		axisMinBoundsTextBox.enabled = false;
		addTextBox(axisMaxBoundsTextBox = new GUIComponentTextBox(guiLeft+50, guiTop+60, 150, "0.0"));
		axisMaxBoundsTextBox.enabled = false;
		addLabel(new GUIComponentLabel(guiLeft+20, guiTop+10, Color.BLACK, WrapperGUI.translate("gui.config.joystick.calibrate1")).setButton(confirmBoundsButton));
		addLabel(new GUIComponentLabel(guiLeft+20, guiTop+20, Color.BLACK, WrapperGUI.translate("gui.config.joystick.calibrate2")).setButton(confirmBoundsButton));
	}
	
	@Override
	public void setStates(){
		//Global headers are just toggles depending on operation.
		configScreenButton.enabled = configuringControls;
		controlScreenButton.enabled = !configScreenButton.enabled;
		
		
		
		//If we are not configuring controls, render the config buttons and labels.
		for(GUIComponentButton button : configButtons.keySet()){
			button.visible = !configuringControls;
		}
		
		
		
		//If we are configuring controls, and haven't selected a vehicle, render the vehicle selection components.
		vehicleSelectionFaultLabel.visible = !WrapperInput.isJoystickSupportEnabled() && configuringControls;
		for(GUIComponentButton button : vehicleSelectionButtons.keySet()){
			button.visible = configuringControls && vehicleConfiguring.isEmpty();
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
					textBox.setText(WrapperInput.getNameForKeyCode(keyboardBoxes.get(vehicleType).get(textBox).config.keyCode));
				}
			}
			for(GUIComponentLabel label : keyboardLabels.get(vehicleType).keySet()){
				label.visible = finishKeyboardBindingsButton.visible && vehicleType.equals(vehicleConfiguring);
				ControlsKeyboardDynamic dynamicControl = keyboardLabels.get(vehicleType).get(label);
				label.text = dynamicControl.translatedName + ": " + WrapperInput.getNameForKeyCode(dynamicControl.modControl.config.keyCode) + " + " + WrapperInput.getNameForKeyCode(dynamicControl.mainControl.config.keyCode);
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
			button.visible = onComponentSelectScreen && i + scrollSpot < WrapperInput.getJoystickInputCount(selectedJoystick);
			if(button.visible){
				//Set basic button text.
				button.text = String.format(" %02d  %-15.15s", i+scrollSpot+1, WrapperInput.getJoystickInputName(selectedJoystick, i+scrollSpot));
				
				//If this joystick is assigned to a control, append that to the text string.
				for(ControlsJoystick joystickControl : ControlsJoystick.values()){
					if(selectedJoystick.equals(joystickControl.config.joystickName)){
						if(joystickControl.config.buttonIndex == i+scrollSpot && joystickControl.systemName.startsWith(vehicleConfiguring)){
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
			componentListDownButton.enabled = scrollSpot + 9 < WrapperInput.getJoystickInputCount(selectedJoystick);
			deadzone_lessButton.enabled = ConfigSystem.configObject.client.joystickDeadZone.value > 0;
			deadzone_moreButton.enabled = ConfigSystem.configObject.client.joystickDeadZone.value < 1;
			deadzone_text.enabled = false;
			deadzone_text.setText(WrapperGUI.translate("gui.config.joystick.deadzone") + " " + String.valueOf(ConfigSystem.configObject.client.joystickDeadZone.value));
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
			float pollData = WrapperInput.getJoystickInputValue(selectedJoystick, joystickComponentId);
			if(pollData < 0){
				axisMinBoundsTextBox.setText(String.valueOf(Math.min(Double.valueOf(axisMinBoundsTextBox.getText()), pollData)));
			}else{
				axisMaxBoundsTextBox.setText(String.valueOf(Math.max(Double.valueOf(axisMaxBoundsTextBox.getText()), pollData)));
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
			assigningDigital = !WrapperInput.isJoystickInputAnalog(selectedJoystick, joystickComponentId);
		}
		
		@Override
		public void renderText(){
			super.renderText();
			if(visible){
				//We need to manually draw the joystick state here.  Do so via built-in rectangle render method
				//as that is render-safe so we won't mess up any texture operations.
				int buttonIndex = joystickComponentSelectionButtons.indexOf(this);
				float pollData = WrapperInput.getJoystickInputValue(selectedJoystick, buttonIndex+scrollSpot);
				if(WrapperInput.isJoystickInputAnalog(selectedJoystick, buttonIndex+scrollSpot)){
					WrapperGUI.renderRectangle(x + 85, y + 2, 40, height - 4, Color.BLACK);
					if(Math.abs(pollData) > ConfigSystem.configObject.client.joystickDeadZone.value){
						WrapperGUI.renderRectangle(x + 85 + 20, y + 2, (int) (pollData*20), height - 4, Color.RED);
					}
				}else{
					if(pollData == 0){
						WrapperGUI.renderRectangle(x + 85 + 20 - (height - 4)/2, y + 2, height - 4, height - 4, Color.BLACK);
					}else if(pollData == 1){
						WrapperGUI.renderRectangle(x + 85 + 20 - (height - 4)/2, y + 2, height - 4, height - 4, Color.RED);
					}else{
						//For digitals with fractions like hats.
						WrapperGUI.renderRectangle(x + 85 + 20 - (height - 4)/2, y + 2, height - 4, height - 4, Color.YELLOW);
					}
				}
			}
		}
	}
}
