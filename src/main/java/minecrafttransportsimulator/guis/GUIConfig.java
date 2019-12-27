package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import minecrafttransportsimulator.guis.components.GUIComponentButton;
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
	private int guiLeft;
	private int guiTop;	
	private GUIComponentButton configScreenButton;
	private GUIComponentButton controlScreenButton;
	
	//Config variables.
	private boolean configuringControls = true;
	private Map<GUIComponentButton, ConfigButtons> configButtons = new HashMap<GUIComponentButton, ConfigButtons>();
	
	//Keybind selection variables.
	private String vehicleConfiguring = "";
	private Map<GUIComponentButton, String> vehicleSelectionButtons = new HashMap<GUIComponentButton, String>();
	private GUIComponentButton finishKeyboardBindingsButton;
	
	//Keyboard assignment variables.
	private boolean configuringKeyboard;
	private Map<ControlsKeyboard, GuiTextField> keyboardBoxes = new HashMap<ControlsKeyboard, GuiTextField>();
	
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
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 256)/2;
		guiTop = (this.height - 192)/2;
		
		//Create ALL the buttons & boxes.
		initHeaderButtons();
		initConfigButtons();
		initVehicleSelectionButtons();
		initJoystickSelecionButtons();
		initJoystickComponentSelecionButtons();
		initAssignmentButtons();
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		changedThisTick = false;
		for(GuiTextField box : keyboardBoxes.values()){
			box.setEnabled(false);
		}
		for(GuiButton button : buttonList){
			button.enabled = false;
		}
		
		this.mc.getTextureManager().bindTexture(standardTexture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 192);
		
		configScreenButton.enabled = configuringControls;
		controlScreenButton.enabled = !configScreenButton.enabled;
		configScreenButton.drawButton(mc, mouseX, mouseY, 0);
		controlScreenButton.drawButton(mc, mouseX, mouseY, 0);
		
		if(configuringControls){
			if(vehicleConfiguring.isEmpty()){
				drawVehicleSelectionScreen(mouseX, mouseY, renderPartialTicks);
			}else{
				if(configuringKeyboard){
					drawKeyboardScreen(mouseX, mouseY, renderPartialTicks);
				}else{
					if(joystick == null){
						drawJoystickSelectionScreen(mouseX, mouseY, renderPartialTicks);
					}else{
						if(joystickComponentId == -1){
							drawJoystickComponentSelectionScreen(mouseX, mouseY, renderPartialTicks);
						}else{
							if(isDigital){
								drawDigitalAssignmentScreen(mouseX, mouseY, renderPartialTicks);
							}else{
								if(!calibrating){
									drawAnalogAssignmentScreen(mouseX, mouseY, renderPartialTicks);
								}else{
									drawAnalogCalibrationScreen(mouseX, mouseY, renderPartialTicks);
								}
							}
						}
					}
				}
			}
		}else{
			drawConfigScreen(mouseX, mouseY, renderPartialTicks);
		}
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked){
		try{
			super.actionPerformed(buttonClicked);
			if(!changedThisTick){
				changedThisTick = true;
				if(buttonClicked.equals(configScreenButton)){
					configuringControls = false;
					vehicleConfiguring = "";
				}else if(buttonClicked.equals(controlScreenButton)){
					configuringControls = true;
				}else if(configButtons.containsKey(buttonClicked)){
					ConfigSystem.setClientConfig(configButtons.get(buttonClicked).configName, !Boolean.valueOf(buttonClicked.displayString));
					buttonClicked.displayString = String.valueOf(ConfigSystem.getBooleanConfig(configButtons.get(buttonClicked).configName));
				}else if(vehicleSelectionButtons.containsKey(buttonClicked)){
					String lookupString = vehicleSelectionButtons.get(buttonClicked);
					vehicleConfiguring = lookupString.substring(0, lookupString.indexOf('.'));
					configuringKeyboard = lookupString.contains("keyboard");
					initKeyboardBoxes();
				}else if(buttonClicked.equals(finishKeyboardBindingsButton)){
					vehicleConfiguring = "";
				}else if(joystickSelectionButtons.containsKey(buttonClicked)){
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
	
    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
    	super.mouseClicked(x, y, button);
    	if(!changedThisTick){
	    	for(GuiTextField box : keyboardBoxes.values()){
	    		if(box.getVisible()){
	    			box.mouseClicked(x, y, button);
	    		}
	    	}
    	}
    }
	
    @Override
    protected void keyTyped(char key, int bytecode) throws IOException {
    	super.keyTyped(key, bytecode);
    	if(bytecode!=1){
    		for(ControlsKeyboard keyboardControl : keyboardBoxes.keySet()){
    			GuiTextField box = keyboardBoxes.get(keyboardControl);
        		if(box.isFocused()){
        			box.setText(Keyboard.getKeyName(bytecode));
        			ControlSystem.setKeyboardKey(keyboardControl, bytecode);
        			box.setFocused(false);
        		}
        	}
        }
    }
	
	private void drawConfigScreen(int mouseX, int mouseY, float renderPartialTicks){
		for(GUIComponentButton button : configButtons.keySet()){
			button.enabled = true;
			button.drawButton(mc, mouseX, mouseY, 0);
			fontRenderer.drawStringWithShadow(configButtons.get(button).formattedName, guiLeft+15, button.y + 5, Color.WHITE.getRGB());
		}
		//Need to do mouseover after main rendering or you get rendering issues.
		for(GUIComponentButton button : configButtons.keySet()){
			if(button.isMouseOver()){
				drawHoveringText(Arrays.asList(configButtons.get(button).mouseoverText), mouseX, mouseY, fontRenderer);
			}
		}
	}
	
	private void drawVehicleSelectionScreen(int mouseX, int mouseY, float renderPartialTicks){
		fontRenderer.drawString(I18n.format("gui.config.controls.title"), guiLeft+20, guiTop+10, Color.BLACK.getRGB());
		for(GUIComponentButton button : vehicleSelectionButtons.keySet()){
			button.enabled = true;
			button.drawButton(mc, mouseX, mouseY, 0);
		}
		if(!ControlSystem.isJoystickSupportEnabled()){
			fontRenderer.drawSplitString(I18n.format("gui.config.joystick.error"), guiLeft+10, guiTop+120, 240, Color.BLACK.getRGB());
		}
	}
	
	private void drawKeyboardScreen(int mouseX, int mouseY, float renderPartialTicks){
		for(ControlsKeyboard keyboardControl : keyboardBoxes.keySet()){
			GuiTextField box = keyboardBoxes.get(keyboardControl);
			if(box.isFocused()){
				box.setText("");
			}else{
				if(keyboardControl.getCurrentButton().length() < 5){
					box.setText(keyboardControl.getCurrentButton());
				}else{
					box.setText(keyboardControl.getCurrentButton().substring(0, 5));
				}
			}
			if(keyboardControl.name().contains(vehicleConfiguring.toUpperCase())){
				box.setEnabled(true);
				box.drawTextBox();
				fontRenderer.drawString(I18n.format(keyboardControl.buttonName) + ":", box.x - 70, box.y + 2, Color.BLACK.getRGB());
			}
		}
		
		//Draw dynamic text boxes.
		byte offset = 0;
		for(ControlsKeyboardDynamic dynamicControl : ControlsKeyboardDynamic.values()){
			if(dynamicControl.name().contains(vehicleConfiguring.toUpperCase())){
				fontRenderer.drawString(I18n.format(dynamicControl.buttonName) + ": " + dynamicControl.modControl.getCurrentButton() + " + " + dynamicControl.mainControl.getCurrentButton(), guiLeft + 10, guiTop + 125 + offset, Color.BLACK.getRGB());
				offset+=11;
			}
		}
		finishKeyboardBindingsButton.enabled = true;
		finishKeyboardBindingsButton.drawButton(mc, mouseX, mouseY, 0);
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
	
	private void initHeaderButtons(){
		configScreenButton = new GUIComponentButton(guiLeft + 0, guiTop - 20, 128, I18n.format("gui.config.header.config"));
		controlScreenButton = new GUIComponentButton(guiLeft + 128, guiTop - 20, 128, I18n.format("gui.config.header.controls"));
		configScreenButton.enabled = true;
		controlScreenButton.enabled = false;
		buttonList.add(configScreenButton);
		buttonList.add(controlScreenButton);
	}
	
	private void initConfigButtons(){
		int line = 0;
		int xOffset = 140;
		for(ConfigButtons buttonEnum : ConfigButtons.values()){
			GUIComponentButton button = new GUIComponentButton(guiLeft+xOffset, guiTop+20+(line++)*20, 60, String.valueOf(ConfigSystem.getBooleanConfig(buttonEnum.configName)));
			button.enabled = false;
			configButtons.put(button, buttonEnum);
			buttonList.add(button);
		}
	}
	
	private void initVehicleSelectionButtons(){
		byte numTypes = 0;
		List<String> vehicleTypes = new ArrayList<String>();
		for(ControlsKeyboard keyboardControl : ControlSystem.ControlsKeyboard.values()){
			String vehicleType = keyboardControl.name().substring(0, keyboardControl.name().indexOf('_')).toLowerCase();
			if(vehicleTypes.contains(vehicleType)){
				continue;
			}else{
				vehicleTypes.add(vehicleType);
				GUIComponentButton buttonKeyboard = new GUIComponentButton(guiLeft + 10, guiTop + 30 + 20*numTypes, 118, I18n.format("gui.config.controls." + vehicleType + ".keyboard"));
				buttonKeyboard.enabled = false;
				vehicleSelectionButtons.put(buttonKeyboard, vehicleType + ".keyboard");
				buttonList.add(buttonKeyboard);
				if(ControlSystem.isJoystickSupportEnabled()){
					GUIComponentButton buttonJoystick = new GUIComponentButton(guiLeft + 128, guiTop + 30 + 20*numTypes, 118, I18n.format("gui.config.controls." + vehicleType + ".joystick"));
					buttonJoystick.enabled = false;
					vehicleSelectionButtons.put(buttonJoystick,  vehicleType + ".joystick");
					buttonList.add(buttonJoystick);
				}
				++numTypes;
			}
		}
	}
	
	private void initKeyboardBoxes(){
		keyboardBoxes.clear();
		
		int verticalOffset = 20;
		int horizontalOffset = 80;
		for(ControlsKeyboard keyboardControl : ControlSystem.ControlsKeyboard.values()){
			if(keyboardControl.name().toLowerCase().contains(vehicleConfiguring)){
				GuiTextField box = new GuiTextField(0, fontRenderer, guiLeft + horizontalOffset, guiTop + verticalOffset, 40, 10);
				keyboardBoxes.put(keyboardControl, box);
				verticalOffset += 11;
				if(verticalOffset > 20 + 11*8){
					verticalOffset = 20;
					horizontalOffset += 120;
				}
			}
		}
		buttonList.add(finishKeyboardBindingsButton = new GUIComponentButton(guiLeft + 180, guiTop + 150, 50, I18n.format("gui.config.controls.confirm")));
	}
	
	private void initJoystickSelecionButtons(){
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
	
	private void initJoystickComponentSelecionButtons(){
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
	
	private void initAssignmentButtons(){
		buttonList.add(cancelAssignmentButton = new GUIComponentButton(guiLeft + 125, guiTop + 160, 100, I18n.format("gui.config.joystick.cancel")));
		buttonList.add(clearAssignmentButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 100, I18n.format("gui.config.joystick.clear")));
	}
	
	private void initDigitalAssignmentButtons(){
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
	
	private void initAnalogAssignmentButtons(){
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
	
	private void initAnalogCalibrationScreen(){
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
    	
	private enum ConfigButtons{
		SEA_LEVEL_OFFSET("SeaLevelOffset", "Sea Level Offset", new String[]{"Does altimeter display 0", "at average sea level", "instead of Y=0?"}),
		MOUSE_YOKE("MouseYoke", "Mouse Yoke", new String[]{"Enable Mouse Yoke?", "Prevents looking around unless unlocked.", "Think MCHeli controls."}),
		INNER_WINDOWS("InnerWindows", "Inner Windows", new String[]{"Render the insides of windows on vehicles?"}),
		KEYBOARD_OVERRIDE("KeyboardOverride", "Keyboard Override", new String[]{"Should keyboard controls be overriden", "when a joystick control is mapped?", "Leave true to free up the keyboard", "while using a joysick."});
		
		
		private final String configName;
		private final String formattedName;
		private final String[] mouseoverText;
		private ConfigButtons(String configName, String formattedName, String[] mouseoverText){
			this.configName = configName;
			this.formattedName = formattedName;
			this.mouseoverText = mouseoverText;
		}
	}
}
