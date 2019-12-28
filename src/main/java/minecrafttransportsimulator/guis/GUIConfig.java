package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboardDynamic;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

public class GUIConfig extends GUIBase{
	//Global variables.
	private boolean changedThisTick;
	private GUIComponentButton configScreenButton;
	private GUIComponentButton controlScreenButton;
	
	//Config variables.
	private boolean configuringControls = true;
	private Map<GUIComponentButton, String> configButtons = new HashMap<GUIComponentButton, String>();
	
	//Keybind selection variables.
	private String vehicleConfiguring = "";
	private String[] vehicleTypes = new String[]{"car", "aircraft"};
	private Map<GUIComponentButton, String> vehicleSelectionButtons = new HashMap<GUIComponentButton, String>();
	private GUIComponentLabel vehicleSelectionLabel;
	private GUIComponentLabel vehicleSelectionFaultLabel;
	private GUIComponentButton finishKeyboardBindingsButton;
	
	//Keyboard assignment variables.
	private boolean configuringKeyboard;
	private Map<String, Map<GUIComponentTextBox, ControlsKeyboard>> keyboardBoxes = new HashMap<String, Map<GUIComponentTextBox, ControlsKeyboard>>();
	private Map<String, Map<GUIComponentLabel, ControlsKeyboardDynamic>> keyboardLabels = new HashMap<String, Map<GUIComponentLabel, ControlsKeyboardDynamic>>();
	
	//Joystick selection variables.
	private Map<GUIComponentButton, Controller> joystickSelectionButtons = new HashMap<GUIComponentButton, Controller>();
	
	//Joystick component selection variables.
	private int scrollSpot = 0;
	private Controller joystick;
	private GUIComponentButton componentListUpButton;
	private GUIComponentButton componentListDownButton;
	private GUIComponentButton deadzone_moreButton;
	private GUIComponentButton deadzone_lessButton;
	private List<GUIComponentButton> joystickComponentSelectionButtons = new ArrayList<GUIComponentButton>();
	
	//Joystick assignment variables.
	private boolean isDigital;
	private int joystickComponentId = -1;
	private GUIComponentButton cancelAssignmentButton;
	private GUIComponentButton clearAssignmentButton;
	
	//Joystick digital assignment variables.
	private Map<GUIComponentButton, ControlsJoystick> digitalAssignButtons = new HashMap<GUIComponentButton, ControlsJoystick>();
	
	//Joystick analog assignment variables.
	private Map<GUIComponentButton, ControlsJoystick> analogAssignButtons = new HashMap<GUIComponentButton, ControlsJoystick>();
	
	//Joystick analog calibration variables.
	private boolean calibrating;
	private ControlsJoystick controlCalibrating;
	private GUIComponentButton confirmBoundsButton;
	private GUIComponentButton invertAxisButton;
	private GuiTextField axisMinBoundsTextBox;
	private GuiTextField axisMaxBoundsTextBox;
	
	public GUIConfig(){
		this.allowUserInput=true;
	}
	
	@Override
	public void setupComponents(int guiLeft, int guiTop){
		//Global header buttons.
		addButton(configScreenButton = new GUIComponentButton(guiLeft + 0, guiTop - 20, 128, translate("config.header.config")){public void onClicked(){configuringControls = false; vehicleConfiguring = "";}});
		addButton(controlScreenButton = new GUIComponentButton(guiLeft + 128, guiTop - 20, 128, translate("config.header.controls")){public void onClicked(){configuringControls = true;}});
		
		//Config buttons and text.
		configButtons.clear();
		for(String configName : new String[]{"Sea Level Offset", "Mouse Yoke", "Inner Windows", "Keyboard Override"}){
			String formattedConfigName = configName.replace(" ", "");
			GUIComponentButton button = new GUIComponentButton(guiLeft+140, guiTop+20+configButtons.size()*20, 60, String.valueOf(ConfigSystem.getBooleanConfig(formattedConfigName))){
				public void onClicked(){
					System.out.println(configButtons.get(this));
					ConfigSystem.setClientConfig(configButtons.get(this), !Boolean.valueOf(text));
					text = String.valueOf(ConfigSystem.getBooleanConfig(configButtons.get(this)));
				}
			};
			addButton(button);
			configButtons.put(button, configName.replace(" ", ""));
			addLabel(new GUIComponentLabel(guiLeft+15, button.y + 5, Color.WHITE, configName).setButton(button));
		}
		
		//Vehicle selection buttons and text.
		//We only have two types.  Car and aircraft.
		vehicleSelectionButtons.clear();
		addLabel(vehicleSelectionLabel = new GUIComponentLabel(guiLeft+20, guiTop+10, Color.BLACK, translate("config.controls.title")));
		addLabel(vehicleSelectionFaultLabel = new GUIComponentLabel(guiLeft+10, guiTop+110, Color.BLACK, translate("config.joystick.error"), 1.0F, false, false, 240));
		for(String vehicleType : vehicleTypes){
			GUIComponentButton buttonKeyboard = new GUIComponentButton(guiLeft + 68, guiTop + 30 + 20*(vehicleSelectionButtons.size()/(ControlSystem.isJoystickSupportEnabled() ? 2 : 1)), 120, translate("config.controls." + vehicleType + ".keyboard")){
				public void onClicked(){
					String lookupString = vehicleSelectionButtons.get(this);
					vehicleConfiguring = lookupString.substring(0, lookupString.indexOf('.'));
					configuringKeyboard = true;
				}
			};
			vehicleSelectionButtons.put(buttonKeyboard, vehicleType + ".keyboard");
			addButton(buttonKeyboard);
			if(ControlSystem.isJoystickSupportEnabled()){
				GUIComponentButton buttonJoystick = new GUIComponentButton(guiLeft + 68, guiTop + 90 + 20*(vehicleSelectionButtons.size()/2), 120, translate("config.controls." + vehicleType + ".joystick")){
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
		for(String vehicleType : vehicleTypes){
			//First add the editable controls.
			int verticalOffset = 20;
			int horizontalOffset = 80;
			Map<GUIComponentTextBox, ControlsKeyboard> boxesForVehicle = new HashMap<GUIComponentTextBox, ControlsKeyboard>();
			for(ControlsKeyboard keyboardControl : ControlSystem.ControlsKeyboard.values()){
				if(keyboardControl.name().toLowerCase().contains(vehicleType)){
					//First create the text box for input.
					GUIComponentTextBox box = new GUIComponentTextBox(guiLeft + horizontalOffset, guiTop + verticalOffset, 40, "", 10, Color.WHITE, Color.BLACK, 5){
						public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
							setText(ControlSystem.getNameForKeyCode(typedCode));
		        			ControlSystem.setKeyboardKey(keyboardBoxes.get(vehicleConfiguring).get(this), typedCode);
		        			focused = false;
						};
					};
					boxesForVehicle.put(box, keyboardControl);
					addTextBox(box);
					
					//Now create the label.
					addLabel(new GUIComponentLabel(box.x - 70, box.y + 2, Color.BLACK, I18n.format(keyboardControl.buttonName) + ":").setBox(box));
					
					verticalOffset += 11;
					if(verticalOffset > 20 + 11*8){
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
					GUIComponentLabel label = new GUIComponentLabel(guiLeft + 10, guiTop + 125 + offset, Color.BLACK, ""); 
					dynamicLabels.put(label, dynamicControl);
					addLabel(label);
					offset+=11;
				}
			}
			keyboardLabels.put(vehicleType, dynamicLabels);
		}
		addButton(finishKeyboardBindingsButton = new GUIComponentButton(guiLeft + 180, guiTop + 150, 50, translate("config.controls.confirm")){public void onClicked(){vehicleConfiguring = "";}});
		
		//Joystick selection buttons and text.
		
		
		//initJoystickSelecionButtons(guiLeft, guiTop);
		//initJoystickComponentSelecionButtons(guiLeft, guiTop);
		//initAssignmentButtons(guiLeft, guiTop);
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
		vehicleSelectionLabel.visible = configuringControls && vehicleConfiguring.isEmpty();
		vehicleSelectionFaultLabel.visible = !ControlSystem.isJoystickSupportEnabled();
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
					textBox.setText(keyboardBoxes.get(vehicleType).get(textBox).getCurrentButton());
				}
			}
			for(GUIComponentLabel label : keyboardLabels.get(vehicleType).keySet()){
				label.visible = finishKeyboardBindingsButton.visible && vehicleType.equals(vehicleConfiguring);
				ControlsKeyboardDynamic dynamicControl = keyboardLabels.get(vehicleType).get(label);
				label.text = I18n.format(dynamicControl.buttonName) + ": " + dynamicControl.modControl.getCurrentButton() + " + " + dynamicControl.mainControl.getCurrentButton();
			}
		}
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		changedThisTick = false;
		
		if(configuringControls){
			if(vehicleConfiguring.isEmpty()){
				
			}else{
				if(configuringKeyboard){
					
				}else{
					if(joystick == null){
//						drawJoystickSelectionScreen(mouseX, mouseY, renderPartialTicks);
					}else{
						if(joystickComponentId == -1){
	//						drawJoystickComponentSelectionScreen(mouseX, mouseY, renderPartialTicks);
						}else{
							if(isDigital){
		//						drawDigitalAssignmentScreen(mouseX, mouseY, renderPartialTicks);
							}else{
								if(!calibrating){
			//						drawAnalogAssignmentScreen(mouseX, mouseY, renderPartialTicks);
								}else{
				//					drawAnalogCalibrationScreen(mouseX, mouseY, renderPartialTicks);
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked){
		if(buttonClicked != null)return;
		try{
			super.actionPerformed(buttonClicked);
			if(!changedThisTick){
				changedThisTick = true;
				if(joystickSelectionButtons.containsKey(buttonClicked)){
					joystick = joystickSelectionButtons.get(buttonClicked);
					scrollSpot = 0;
				}else if(buttonClicked.equals(componentListUpButton)){
					scrollSpot -= 9;
				}else if(buttonClicked.equals(componentListDownButton)){
					scrollSpot += 9;
				}else if(buttonClicked.equals(deadzone_lessButton)){
					ConfigSystem.setClientConfig("JoystickDeadZone", ((int) (ConfigSystem.getDoubleConfig("JoystickDeadZone")*100) - 1)/100F);
				}else if(buttonClicked.equals(deadzone_moreButton)){
					ConfigSystem.setClientConfig("JoystickDeadZone", ((int) (ConfigSystem.getDoubleConfig("JoystickDeadZone")*100) + 1)/100F);
				}else if(joystickComponentSelectionButtons.contains(buttonClicked)){
					joystickComponentId = joystickComponentSelectionButtons.indexOf(buttonClicked) + scrollSpot;
					isDigital = !joystick.getComponents()[joystickComponentId].isAnalog();
					if(isDigital){
						initDigitalAssignmentButtons();
					}else{
						initAnalogAssignmentButtons();
					}
				}else if(digitalAssignButtons.containsKey(buttonClicked)){
					ControlSystem.setControlJoystick(digitalAssignButtons.get(buttonClicked), joystick.getName(), joystickComponentId);
					joystickComponentId = -1;
				}else if(analogAssignButtons.containsKey(buttonClicked)){
					controlCalibrating = analogAssignButtons.get(buttonClicked);
					initAnalogCalibrationScreen();
				}else if(buttonClicked.equals(confirmBoundsButton)){
					boolean isInverted = invertAxisButton.displayString.contains(I18n.format("gui.config.joystick.invert"));
					ControlSystem.setAxisJoystick(controlCalibrating, joystick.getName(), joystickComponentId, Double.valueOf(axisMinBoundsTextBox.getText()), Double.valueOf(axisMaxBoundsTextBox.getText()), isInverted);
					joystickComponentId = -1;
				}else if(buttonClicked.equals(invertAxisButton)){
					if(invertAxisButton.displayString.contains(I18n.format("gui.config.joystick.invert"))){
						invertAxisButton.displayString = I18n.format("gui.config.joystick.axismode") + I18n.format("gui.config.joystick.normal");
					}else{
						invertAxisButton.displayString = I18n.format("gui.config.joystick.axismode") + I18n.format("gui.config.joystick.invert");
					}
				}else if(buttonClicked.equals(cancelAssignmentButton)){
					joystickComponentId = -1;
				}else if(buttonClicked.equals(clearAssignmentButton)){
					for(ControlsJoystick joystickControl : ControlsJoystick.values()){
						if(joystickControl.getCurrentJoystick().equals(joystick.getName())){
							if(joystickControl.getCurrentButton() == joystickComponentId && joystickControl.name().toLowerCase().contains(vehicleConfiguring)){
								ControlSystem.clearControlJoystick(joystickControl);
								break;
							}
						}
					}
					joystickComponentId = -1;
				}
			}
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private void drawJoystickSelectionScreen(int mouseX, int mouseY, float renderPartialTicks){
		fontRenderer.drawString(I18n.format("gui.config.joystick.select"), guiLeft+20, guiTop+10, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.name"), guiLeft+15, guiTop+25, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.type"), guiLeft+140, guiTop+25, Color.BLACK.getRGB());
		
		for(GUIComponentButton button : joystickSelectionButtons.keySet()){
			button.enabled = true;
			button.drawButton(mc, mouseX, mouseY, 0);
			fontRenderer.drawString(joystickSelectionButtons.get(button).getName().substring(0, joystickSelectionButtons.get(button).getName().length() > 20 ? 20 : joystickSelectionButtons.get(button).getName().length()), guiLeft+15, button.y + 5, Color.WHITE.getRGB());
			fontRenderer.drawString(joystickSelectionButtons.get(button).getType().toString(), guiLeft+140, button.y + 5, Color.WHITE.getRGB());
		}
	}
	
	private void drawJoystickComponentSelectionScreen(int mouseX, int mouseY, float renderPartialTicks){
		fontRenderer.drawString(I18n.format("gui.config.joystick.mapping"), guiLeft+15, guiTop+20, Color.BLACK.getRGB());
		drawRect(guiLeft+120, guiTop+10, guiLeft+220, guiTop+30, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.deadzone"), guiLeft+125, guiTop+15, Color.WHITE.getRGB());
		fontRenderer.drawString(String.valueOf(ConfigSystem.getDoubleConfig("JoystickDeadZone")), guiLeft+190, guiTop+15, Color.WHITE.getRGB());
		
		fontRenderer.drawString("#", guiLeft+15, guiTop+35, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.name"), guiLeft+30, guiTop+35, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.state"), guiLeft+100, guiTop+35, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.assignment"), guiLeft+140, guiTop+35, Color.BLACK.getRGB());
		
		for(int i=0; i<9 && i<joystick.getComponents().length && i+scrollSpot<joystick.getComponents().length; ++i){
			GUIComponentButton currentButton = joystickComponentSelectionButtons.get(i);
			currentButton.enabled = true;
			currentButton.drawButton(mc, mouseX, mouseY, 0);
			fontRenderer.drawString(String.valueOf(i+scrollSpot+1), guiLeft+15, currentButton.y + 5, Color.WHITE.getRGB());
			fontRenderer.drawString(joystick.getComponents()[i+scrollSpot].getName().substring(0, joystick.getComponents()[i+scrollSpot].getName().length() > 15 ? 15 : joystick.getComponents()[i+scrollSpot].getName().length()), guiLeft+30, currentButton.y + 5, Color.WHITE.getRGB());
			for(ControlsJoystick joystickControl : ControlsJoystick.values()){
				if(joystickControl.getCurrentJoystick().equals(joystick.getName())){
					if(joystickControl.getCurrentButton() == i+scrollSpot && joystickControl.name().toLowerCase().contains(vehicleConfiguring)){
						fontRenderer.drawString(I18n.format(joystickControl.buttonName), guiLeft+140, currentButton.y + 5, Color.WHITE.getRGB());
					}
				}
			}
			
			joystick.poll();
			float pollData = joystick.getComponents()[i+scrollSpot].getPollData();
			if(joystick.getComponents()[i+scrollSpot].isAnalog()){
				drawRect(guiLeft+95, currentButton.y + 2, guiLeft+135, currentButton.y + currentButton.height - 2, Color.BLACK.getRGB());
				if(Math.abs(pollData) > ConfigSystem.getDoubleConfig("JoystickDeadZone")){
					if(pollData > 0){
						drawRect(guiLeft+115, currentButton.y + 2, (int) (guiLeft+115+pollData*20), currentButton.y + currentButton.height - 2, Color.RED.getRGB());
					}else{
						drawRect((int) (guiLeft+115+pollData*20), currentButton.y + 2, guiLeft+115, currentButton.y + currentButton.height - 2, Color.RED.getRGB());
					}
				}
			}else{
				if(pollData == 0){
					drawRect(guiLeft+110, currentButton.y + 2, guiLeft+120, currentButton.y + currentButton.height - 2, Color.BLACK.getRGB());
				}else if(pollData == 1){
					drawRect(guiLeft+110, currentButton.y + 2, guiLeft+120, currentButton.y + currentButton.height - 2, Color.RED.getRGB());
				}else{
					//For digitals with fractions like hats.
					drawRect(guiLeft+110, currentButton.y + 2, guiLeft+120, currentButton.y + currentButton.height - 2, Color.YELLOW.getRGB());
				}
			}
		}

		deadzone_moreButton.enabled = ConfigSystem.getDoubleConfig("JoystickDeadZone") < 1;
		deadzone_lessButton.enabled = ConfigSystem.getDoubleConfig("JoystickDeadZone") > 0;
		componentListUpButton.enabled = scrollSpot - 9 >= 0;
		componentListDownButton.enabled = scrollSpot + 9 < joystick.getComponents().length;
		deadzone_moreButton.drawButton(mc, mouseX, mouseY, 0);
		deadzone_lessButton.drawButton(mc, mouseX, mouseY, 0);
		componentListUpButton.drawButton(mc, mouseX, mouseY, 0);
		componentListDownButton.drawButton(mc, mouseX, mouseY, 0);
	}
	
	private void drawDigitalAssignmentScreen(int mouseX, int mouseY, float renderPartialTicks){
		fontRenderer.drawString(I18n.format("gui.config.joystick.choosemap"), guiLeft+20, guiTop+10, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.selectdigital"), guiLeft+20, guiTop+20, Color.BLACK.getRGB());
		for(GUIComponentButton button : digitalAssignButtons.keySet()){
			button.enabled = true;
			button.drawButton(mc, mouseX, mouseY, 0);
		}
		
		for(ControlsJoystick joystickControl : ControlsJoystick.values()){
			if(joystickControl.getCurrentJoystick().equals(joystick.getName())){
				if(joystickControl.getCurrentButton() == joystickComponentId && joystickControl.name().toLowerCase().contains(vehicleConfiguring)){
					clearAssignmentButton.enabled = true;
					break;
				}
			}
		}
		cancelAssignmentButton.enabled = true;
		cancelAssignmentButton.drawButton(mc, mouseX, mouseY, 0);
		clearAssignmentButton.drawButton(mc, mouseX, mouseY, 0);
	}
	
	private void drawAnalogAssignmentScreen(int mouseX, int mouseY, float renderPartialTicks){
		fontRenderer.drawString(I18n.format("gui.config.joystick.choosemap"), guiLeft+20, guiTop+10, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.selectanalog"), guiLeft+20, guiTop+20, Color.BLACK.getRGB());
		for(GUIComponentButton button : analogAssignButtons.keySet()){
			button.enabled = true;
			button.drawButton(mc, mouseX, mouseY, 0);
		}
		
		for(ControlsJoystick joystickControl : ControlsJoystick.values()){
			if(joystickControl.getCurrentJoystick().equals(joystick.getName())){
				if(joystickControl.getCurrentButton() == joystickComponentId && joystickControl.name().toLowerCase().contains(vehicleConfiguring)){
					clearAssignmentButton.enabled = true;
					break;
				}
			}
		}
		cancelAssignmentButton.enabled = true;
		cancelAssignmentButton.drawButton(mc, mouseX, mouseY, 0);
		clearAssignmentButton.drawButton(mc, mouseX, mouseY, 0);
	}
	
	private void drawAnalogCalibrationScreen(int mouseX, int mouseY, float renderPartialTicks){
		fontRenderer.drawString(I18n.format("gui.config.joystick.calibrate1"), guiLeft+20, guiTop+10, Color.BLACK.getRGB());
		fontRenderer.drawString(I18n.format("gui.config.joystick.calibrate2"), guiLeft+20, guiTop+20, Color.BLACK.getRGB());
		
		joystick.poll();
		float pollData = joystick.getComponents()[joystickComponentId].getPollData();
		if(pollData < 0){
			axisMinBoundsTextBox.setText(String.valueOf(Math.min(Double.valueOf(axisMinBoundsTextBox.getText()), pollData)));
		}else{
			axisMaxBoundsTextBox.setText(String.valueOf(Math.max(Double.valueOf(axisMaxBoundsTextBox.getText()), pollData)));
		}
		axisMinBoundsTextBox.drawTextBox();
		axisMaxBoundsTextBox.drawTextBox();
		
		confirmBoundsButton.enabled = true;
		cancelAssignmentButton.enabled = true;
		invertAxisButton.enabled = true;
		confirmBoundsButton.drawButton(mc, mouseX, mouseY, 0);
		cancelAssignmentButton.drawButton(mc, mouseX, mouseY, 0);		
		invertAxisButton.drawButton(mc, mouseX, mouseY, 0);
	}
	
	private void initJoystickSelecionButtons(int guiLeft, int guiTop){
		for(Controller joystick : ControllerEnvironment.getDefaultEnvironment().getControllers()){
			if(joystick.getType() != null && joystick.getName() != null){
				if(!joystick.getType().equals(Controller.Type.MOUSE) && !joystick.getType().equals(Controller.Type.KEYBOARD)){
					if(joystick.getComponents().length != 0){
						GUIComponentButton button = new GUIComponentButton(guiLeft + 10, guiTop + 40 + 20*joystickSelectionButtons.size(), 235, "");
						joystickSelectionButtons.put(button, joystick);
						buttonList.add(button);
					}
				}
			}
		}
	}
	
	private void initJoystickComponentSelecionButtons(int guiLeft, int guiTop){
		for(int i=0; i<9; ++i){
			GUIComponentButton button = new GUIComponentButton(guiLeft+10, guiTop+45+15*i, 215, "", 15);
			joystickComponentSelectionButtons.add(button);
			buttonList.add(button);
		}
		buttonList.add(componentListUpButton = new GUIComponentButton(guiLeft + 225, guiTop + 45, 20, "/\\"));
		buttonList.add(componentListDownButton = new GUIComponentButton(guiLeft + 225, guiTop + 155, 20, "\\/"));
		buttonList.add(deadzone_lessButton = new GUIComponentButton(guiLeft + 100, guiTop + 10, 20, "<"));
		buttonList.add(deadzone_moreButton = new GUIComponentButton(guiLeft + 220, guiTop + 10, 20, ">"));
	}
	
	private void initAssignmentButtons(int guiLeft, int guiTop){
		buttonList.add(cancelAssignmentButton = new GUIComponentButton(guiLeft + 125, guiTop + 160, 100, I18n.format("gui.config.joystick.cancel")));
		buttonList.add(clearAssignmentButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, I18n.format("gui.config.joystick.clear")));
	}
	
	private void initDigitalAssignmentButtons(int guiLeft, int guiTop){
		buttonList.removeAll(digitalAssignButtons.keySet());
		digitalAssignButtons.clear();
		
		short leftOffset = 0;
		byte topOffset = 0;
		for(ControlsJoystick joystickControl : ControlsJoystick.values()){
			if(joystickControl.name().toLowerCase().contains(vehicleConfiguring)){
				if(!joystickControl.isAxis){
					GUIComponentButton button = new GUIComponentButton(guiLeft + 8 + leftOffset, guiTop + 30 + topOffset, 80, I18n.format(joystickControl.buttonName), 15);
					digitalAssignButtons.put(button, joystickControl);
					buttonList.add(button);
					topOffset += button.height;
				}
			}
			if(topOffset >= 120){
				topOffset = 0;
				leftOffset += 80;
			}
		}
	}
	
	private void initAnalogAssignmentButtons(int guiLeft, int guiTop){
		buttonList.removeAll(analogAssignButtons.keySet());
		analogAssignButtons.clear();
		calibrating = false;
		
		byte topOffset = 0;
		for(ControlsJoystick joystickControl : ControlsJoystick.values()){
			if(joystickControl.name().toLowerCase().contains(vehicleConfiguring)){
				if(joystickControl.isAxis){
					GUIComponentButton button = new GUIComponentButton(guiLeft + 85, guiTop + 40 + topOffset, 80, I18n.format(joystickControl.buttonName));
					analogAssignButtons.put(button, joystickControl);
					buttonList.add(button);
					topOffset += button.height;
				}
			}
		}
	}
	
	private void initAnalogCalibrationScreen(int guiLeft, int guiTop){
		calibrating = true;
		buttonList.add(confirmBoundsButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, I18n.format("gui.config.joystick.confirm")));
		buttonList.add(invertAxisButton = new GUIComponentButton(guiLeft + 50, guiTop + 120, 150, I18n.format("gui.config.joystick.axismode") + I18n.format("gui.config.joystick.normal")));
		
		axisMinBoundsTextBox = new GuiTextField(0, fontRenderer, guiLeft+50, guiTop+90, 150, 15);
		axisMaxBoundsTextBox = new GuiTextField(0, fontRenderer, guiLeft+50, guiTop+60, 150, 15);
		axisMinBoundsTextBox.setEnabled(false);
		axisMaxBoundsTextBox.setEnabled(false);
		axisMinBoundsTextBox.setText("0.0");
		axisMaxBoundsTextBox.setText("0.0");
	}
}
