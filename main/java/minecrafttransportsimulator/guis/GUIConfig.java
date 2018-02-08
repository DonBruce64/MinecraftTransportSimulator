package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import net.java.games.input.Component;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GUIConfig extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/wide_blank.png");
		
	
	private boolean invertButtonPressed;
	
	//Global variables.
	private boolean changedThisTick;
	private int guiLeft;
	private int guiTop;	
	private GuiButton configScreenButton;
	private GuiButton controlScreenButton;
	
	//Config variables.
	private boolean configuringControls = true;
	private Map<GuiButton, ConfigButtons> configButtons = new HashMap<GuiButton, ConfigButtons>();
	
	//Keybind selection variables.
	private String vehicleConfiguring = "";
	private Map<GuiButton, String> vehicleSelectionButtons = new HashMap<GuiButton, String>();
	private GuiButton finishKeyboardBindingsButton;
	
	//Keyboard assignment variables.
	private boolean configuringKeyboard;
	private Map<ControlsKeyboard, GuiTextField> keyboardBoxes = new HashMap<ControlsKeyboard, GuiTextField>();
	
	//Joystick selection variables.
	private List<GuiButton> joystickSelectionButtons = new ArrayList<GuiButton>();
	
	//Joystick component selection variables.
	private int scrollSpot = 0;
	private Component joystick;
	private GuiButton component_list_upButton;
	private GuiButton component_list_downButton;
	private GuiButton deadzone_moreButton;
	private GuiButton deadzone_lessButton;
	private List<GuiButton> joystickComponentSelectionButtons = new ArrayList<GuiButton>();
	
	//Joystick assignment variables.
	private int joystickComponentId;
	private GuiButton cancelAssignmentButton;
	private GuiButton clearAssignmentButton;
	
	//Joystick digital assignment variables.
	private List<GuiButton> digitalAssignButtons = new ArrayList<GuiButton>();
	
	//Joystick analog assignment variables.
	private List<GuiButton> analogAssignButtons = new ArrayList<GuiButton>();
	
	//Joystick analog calibration variables.
	private GuiButton confirmBoundsButton;
	private GuiButton invertAxisButton;
	private GuiTextField axisMaxBoundsTextBox;
	private GuiTextField axisMinBoundsTextBox;
	
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
		initKeyboardBoxes();

		
		//initJoysticks();
		//initConfigControls();
		//initJoystickSelectionButtons();
		//initJoystickControls();
		//setInputStatesByLevel();
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
		
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 192);
		
		configScreenButton.enabled = configuringControls;
		controlScreenButton.enabled = !configScreenButton.enabled;
		configScreenButton.drawButton(mc, mouseX, mouseY);
		controlScreenButton.drawButton(mc, mouseX, mouseY);
		
		if(configuringControls){
			if(vehicleConfiguring.isEmpty()){
				drawVehicleSelectionScreen(mouseX, mouseY, renderPartialTicks);
			}else{
				if(configuringKeyboard){
					drawKeyboardScreen(mouseX, mouseY, renderPartialTicks);
				}else{
					
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
				}if(buttonClicked.equals(finishKeyboardBindingsButton)){
					vehicleConfiguring = "";
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
		for(GuiButton button : configButtons.keySet()){
			button.enabled = true;
			button.drawButton(mc, mouseX, mouseY);
			fontRendererObj.drawStringWithShadow(configButtons.get(button).formattedName, guiLeft+10, button.yPosition + 5, Color.WHITE.getRGB());
		}
		//Need to do mouseover after main rendering or you get rendering issues.
		for(GuiButton button : configButtons.keySet()){
			if(button.isMouseOver()){
				drawHoveringText(Arrays.asList(configButtons.get(button).mouseoverText), mouseX, mouseY, fontRendererObj);
			}
		}
	}
	
	private void drawVehicleSelectionScreen(int mouseX, int mouseY, float renderPartialTicks){
		fontRendererObj.drawStringWithShadow(I18n.format("gui.config.controls.title"), guiLeft+10, guiTop+10, Color.WHITE.getRGB());
		for(GuiButton button : vehicleSelectionButtons.keySet()){
			button.enabled = true;
			button.drawButton(mc, mouseX, mouseY);
		}
	}
	
	private void drawKeyboardScreen(int mouseX, int mouseY, float renderPartialTicks){
		fontRendererObj.drawStringWithShadow(I18n.format("gui.config.controls." + vehicleConfiguring + ".keyboard"), guiLeft+15, guiTop+15, Color.WHITE.getRGB());
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
				fontRendererObj.drawStringWithShadow(I18n.format(keyboardControl.buttonName) + ":", box.xPosition - 70, box.yPosition + 2, Color.WHITE.getRGB());
			}
		}
		finishKeyboardBindingsButton.enabled = true;
		finishKeyboardBindingsButton.drawButton(mc, mouseX, mouseY);
	}
	
	private void initHeaderButtons(){
		configScreenButton = new GuiButton(0, guiLeft + 0, guiTop - 20, 128, 20, I18n.format("gui.config.header.config"));
		controlScreenButton = new GuiButton(0, guiLeft + 128, guiTop - 20, 128, 20, I18n.format("gui.config.header.controls"));
		configScreenButton.enabled = true;
		controlScreenButton.enabled = false;
		buttonList.add(configScreenButton);
		buttonList.add(controlScreenButton);
	}
	
	private void initConfigButtons(){
		int line = 0;
		int xOffset = 140;
		for(ConfigButtons buttonEnum : ConfigButtons.values()){
			GuiButton newButton = new GuiButton(0, guiLeft+xOffset, guiTop+10+(line++)*20, 60, 20, String.valueOf(ConfigSystem.getBooleanConfig(buttonEnum.configName)));
			newButton.enabled = false;
			configButtons.put(newButton, buttonEnum);
			buttonList.add(newButton);
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
				GuiButton buttonKeyboard = new GuiButton(0, guiLeft + 10, guiTop + 30 + 20*numTypes, 118, 20, I18n.format("gui.config.controls." + vehicleType + ".keyboard"));
				GuiButton buttonJoystick = new GuiButton(0, guiLeft + 128, guiTop + 30 + 20*numTypes, 118, 20, I18n.format("gui.config.controls." + vehicleType + ".joystick"));
				buttonKeyboard.enabled = false;
				buttonJoystick.enabled = false;
				vehicleSelectionButtons.put(buttonKeyboard, vehicleType + ".keyboard");
				vehicleSelectionButtons.put(buttonJoystick,  vehicleType + ".joystick");
				buttonList.add(buttonKeyboard);
				buttonList.add(buttonJoystick);
				++numTypes;
			}
		}
	}
	
	private void initKeyboardBoxes(){
		int verticalOffset = 40;
		int horizontalOffset = 80;
		String prefix = ControlSystem.ControlsKeyboard.values()[0].name().substring(0, ControlSystem.ControlsKeyboard.values()[0].name().indexOf('_'));
		for(ControlsKeyboard keyboardControl : ControlSystem.ControlsKeyboard.values()){
			if(!prefix.equals(keyboardControl.name().substring(0, keyboardControl.name().indexOf('_')))){
				verticalOffset = 40;
				horizontalOffset = 80;
				prefix = keyboardControl.name().substring(0, keyboardControl.name().indexOf('_'));
			}
			GuiTextField box = new GuiTextField(0, fontRendererObj, guiLeft + horizontalOffset, guiTop + verticalOffset, 40, 15);
			box.setText(keyboardControl.getCurrentButton());
			keyboardBoxes.put(keyboardControl, box);
			verticalOffset += 17;
			if(verticalOffset > 40 + 17*7){
				verticalOffset = 40;
				horizontalOffset += 120;
			}
		}
		finishKeyboardBindingsButton = new GuiButton(0, guiLeft + 140, guiTop + 10, 100, 20, I18n.format("gui.config.controls.confirm"));
		buttonList.add(finishKeyboardBindingsButton);
	}
	
	/*
	private void initJoysticks(){
		joysticks = ControllerEnvironment.getDefaultEnvironment().getControllers();
		byte jsNumber = 0;
		byte jsIndex = 0;
		for(Controller joystick : joysticks){
			if(joystick.getType() != null && joystick.getName() != null){
				if(!joystick.getType().equals(Controller.Type.MOUSE) && !joystick.getType().equals(Controller.Type.KEYBOARD)){
					if(joystick.getComponents().length != 0){
						GuiButton jsButton = new GuiButton(0, guiLeft + 5, guiTop + 40 + 15*jsNumber, 240, 15, "");
						joystickButtons.put(jsButton, jsIndex);
						buttonList.add(jsButton);
						jsButton.enabled = false;
						++jsNumber;
					}
				}
			}
			++jsIndex;
		}
	}

	
	
	private void initJoystickSelectionButtons(){
		for(int i=0; i<9; ++i){
			joystickConfigureButtons.add(new GuiButton(0, guiLeft+5, guiTop+40+15*i, 215, 15, ""));
			buttonList.add(joystickConfigureButtons.get(i));
			joystickConfigureButtons.get(i).enabled = false;
		}
	}

	
	private void initJoystickControls(){
		buttonList.add(contol_list_upButton = new GuiButton(0, guiLeft + 225, guiTop + 40, 20, 20, "/\\"));
		buttonList.add(control_list_downButton = new GuiButton(0, guiLeft + 225, guiTop + 155, 20, 20, "\\/"));
		buttonList.add(deadzone_lessButton = new GuiButton(0, guiLeft + 100, guiTop + 5, 20, 20, "<"));
		buttonList.add(deadzone_moreButton = new GuiButton(0, guiLeft + 220, guiTop + 5, 20, 20, ">"));
		buttonList.add(confirmButton = new GuiButton(0, guiLeft + 25, guiTop + 160, 100, 20, I18n.format("gui.config.joystick.confirm")));
		buttonList.add(cancelButton = new GuiButton(0, guiLeft + 125, guiTop + 160, 100, 20, I18n.format("gui.config.joystick.cancel")));
		buttonList.add(clearButton = new GuiButton(0, guiLeft + 25, guiTop + 160, 100, 20, I18n.format("gui.config.joystick.clear")));
		buttonList.add(invertButton = new GuiButton(0, guiLeft + 50, guiTop + 120, 150, 20, I18n.format("gui.config.joystick.clear")));
		
		createAssignmentButtonAt(guiLeft + 85, guiTop + 40, ControlSystem.Controls.PITCH.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 60, ControlSystem.Controls.ROLL.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 80, ControlSystem.Controls.YAW.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 100,ControlSystem.Controls.THROTTLE.joystickName, analogAssignButtons);
		
		createAssignmentButtonAt(guiLeft + 5, guiTop + 30, ControlSystem.Controls.FLAPS_U.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 50, ControlSystem.Controls.FLAPS_D.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 70, ControlSystem.Controls.BRAKE.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 90,ControlSystem.Controls.PANEL.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 110,ControlSystem.Controls.MOD.joystickName, digitalAssignButtons);

		createAssignmentButtonAt(guiLeft + 85, guiTop + 30,ControlSystem.Controls.ZOOM_I.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 50,ControlSystem.Controls.ZOOM_O.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 70,ControlSystem.Controls.CAM.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 90,ControlSystem.Controls.CHANGEVIEW.joystickName, digitalAssignButtons);
		
		createAssignmentButtonAt(guiLeft + 165, guiTop + 30,ControlSystem.Controls.LOOK_L.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 50,ControlSystem.Controls.LOOK_R.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 70,ControlSystem.Controls.LOOK_U.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 90,ControlSystem.Controls.LOOK_D.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 110,ControlSystem.Controls.LOOK_ALL.joystickName, digitalAssignButtons);
		
		maxTextBox = new GuiTextField(0, fontRendererObj, guiLeft+50, guiTop+60, 150, 15);
		minTextBox = new GuiTextField(0, fontRendererObj, guiLeft+50, guiTop+90, 150, 15);
	}
	
	
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		changedThisTick = false;
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 192);
		
		//Draw menu buttons.
		for(GuiButton menuButton : menuButtons.keySet()){
			menuButton.enabled = !this.guiState.equals(menuButtons.get(menuButton));
			menuButton.drawButton(mc, mouseX, mouseY);
		}
		
		if(guiState.equals(GUIStates.CONFIG)){
			drawConfigScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.CONTROLS_PLANE_KEYBOARD)){
			drawPlaneScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.CONTROLS_HELICOPTER_KEYBOARD)){
			drawHelicopterScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.CONTROLS_CAR_KEYBOARD)){
			drawVehicleScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.JS_SELECT)){
			drawJoystickSelectionScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.JS_BUTTON)){
			drawJoystickButtonScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.JS_DIGITAL)){
			drawJoystickDigitalScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.JS_ANALOG)){
			drawJoystickAnalogScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.JS_CALIBRATION)){
			drawJoystickCalibrationScreen(mouseX, mouseY);
		}
		
		//Draw keybindingBoxes if required.
		//Has to happen after other secondary screens are rendered.
		for(GuiTextField keybindBox : keybindBoxes.keySet()){
			if(keybindBoxes.get(keybindBox).stateToDisplayOn.equals(this.guiState)){
				String keybindName = keybindBoxes.get(keybindBox).keybindName;
				if(keybindBox.isFocused()){
					keybindBox.setText("");
				}else{
					keybindBox.setText(ControlSystem.getKeyboardKeyname(keybindName));
				}
				keybindBox.drawTextBox();
				fontRendererObj.drawStringWithShadow(keybindName + ":", keybindBox.xPosition - 70, keybindBox.yPosition + 2, Color.WHITE.getRGB());
			}
		}
	}
	
	private void drawConfigScreen(int mouseX, int mouseY){
		for(GuiButton button : configButtons.keySet()){
			button.drawButton(mc, mouseX, mouseY);
			fontRendererObj.drawStringWithShadow(configButtons.get(button).formattedName, guiLeft+10, button.yPosition + 5, Color.WHITE.getRGB());
		}
		//Need to do mouseover after main rendering or you get rendering issues.
		for(GuiButton button : configButtons.keySet()){
			if(button.isMouseOver()){
				drawHoveringText(Arrays.asList(configButtons.get(button).mouseoverText), mouseX, mouseY, fontRendererObj);
			}
		}
	}
	
	private void drawPlaneScreen(int mouseX, int mouseY){
		int line = 8;
		int xOffset = 10;
		fontRendererObj.drawStringWithShadow("ParkBrake:", guiLeft+xOffset, guiTop+15+(line)*17, Color.WHITE.getRGB());
		fontRendererObj.drawString(ControlSystem.getKeyboardKeyname(ControlSystem.Controls.MOD.keyboardName) + "+" +  ControlSystem.getKeyboardKeyname(ControlSystem.Controls.BRAKE.keyboardName), guiLeft+xOffset+60, guiTop+15+(line++)*17, Color.BLACK.getRGB());
		fontRendererObj.drawStringWithShadow("HUDMode:", guiLeft+xOffset, guiTop+15+(line)*17, Color.WHITE.getRGB());
		fontRendererObj.drawString(ControlSystem.getKeyboardKeyname(ControlSystem.Controls.MOD.keyboardName) + "+" +  ControlSystem.getKeyboardKeyname(ControlSystem.Controls.CAM.keyboardName), guiLeft+xOffset+60, guiTop+15+(line++)*17, Color.BLACK.getRGB());
	}
	
	private void drawHelicopterScreen(int mouseX, int mouseY){

	}
	
	private void drawVehicleScreen(int mouseX, int mouseY){

	}
	
	private void drawJoystickSelectionScreen(int mouseX, int mouseY){
		fontRendererObj.drawString(I18n.format("gui.config.joystick.select"), guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.name"), guiLeft+10, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.type"), guiLeft+140, guiTop+25, Color.BLACK.getRGB());
		
		for(GuiButton jsButton : joystickButtons.keySet()){
			jsButton.drawButton(mc, mouseX, mouseY);
			byte jsNumber = joystickButtons.get(jsButton);
			fontRendererObj.drawString(joysticks[jsNumber].getName().substring(0, joysticks[jsNumber].getName().length() > 20 ? 20 : joysticks[jsNumber].getName().length()), guiLeft+10, jsButton.yPosition + 5, Color.WHITE.getRGB());
			fontRendererObj.drawString(joysticks[jsNumber].getType().toString(), guiLeft+140, jsButton.yPosition + 5, Color.WHITE.getRGB());
		}
	}
	
	private void drawJoystickButtonScreen(int mouseX, int mouseY){
		fontRendererObj.drawString(I18n.format("gui.config.joystick.mapping"), guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		drawRect(guiLeft+120, guiTop+5, guiLeft+220, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.deadzone"), guiLeft+125, guiTop+10, Color.WHITE.getRGB());
		fontRendererObj.drawString(String.valueOf(ConfigSystem.getDoubleConfig("JoystickDeadZone")), guiLeft+190, guiTop+10, Color.WHITE.getRGB());
		
		fontRendererObj.drawString("#", guiLeft+10, guiTop+30, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.name"), guiLeft+25, guiTop+30, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.state"), guiLeft+100, guiTop+30, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.assignment"), guiLeft+150, guiTop+30, Color.BLACK.getRGB());
		
		for(int i=0; i<9 && i<joystickComponents.length && i+scrollSpot<joystickComponents.length; ++i){
			joystickConfigureButtons.get(i).drawButton(mc, mouseX, mouseY);
			fontRendererObj.drawString(String.valueOf(i+scrollSpot+1), guiLeft+10, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(joystickComponents[i+scrollSpot].getName().substring(0, joystickComponents[i+scrollSpot].getName().length() > 15 ? 15 : joystickComponents[i+scrollSpot].getName().length()), guiLeft+25, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(ControlSystem.getJoystickControlName(i+scrollSpot), guiLeft+140, guiTop+44+15*i, Color.WHITE.getRGB());
			
			ControlSystem.getJoystick().poll();
			float pollData = joystickComponents[i+scrollSpot].getPollData();
			if(joystickComponents[i+scrollSpot].isAnalog()){
				drawRect(guiLeft+95, guiTop+53+15*i, guiLeft+135, guiTop+43+15*i, Color.BLACK.getRGB());
				if(Math.abs(pollData) > ConfigSystem.getDoubleConfig("JoystickDeadZone") && Math.abs(pollData) > joystickComponents[i+scrollSpot].getDeadZone()){
					if(pollData > 0){
						drawRect(guiLeft+115, guiTop+53+15*i, (int) (guiLeft+115+pollData*20), guiTop+43+15*i, Color.RED.getRGB());
					}else{
						drawRect((int) (guiLeft+115+pollData*20), guiTop+53+15*i, guiLeft+115, guiTop+43+15*i, Color.RED.getRGB());
					}
				}
			}else{
				if(pollData == 0){
					drawRect(guiLeft+110, guiTop+53+15*i, guiLeft+120, guiTop+43+15*i, Color.BLACK.getRGB());
				}else if(pollData == 1){
					drawRect(guiLeft+110, guiTop+53+15*i, guiLeft+120, guiTop+43+15*i, Color.RED.getRGB());
				}else{
					//For digitals with fractions like hats.
					drawRect(guiLeft+110, guiTop+53+15*i, guiLeft+120, guiTop+43+15*i, Color.YELLOW.getRGB());
				}
			}
		}

		deadzone_moreButton.drawButton(mc, mouseX, mouseY);
		deadzone_lessButton.drawButton(mc, mouseX, mouseY);
		contol_list_upButton.drawButton(mc, mouseX, mouseY);
		control_list_downButton.drawButton(mc, mouseX, mouseY);
	}
	
	private void drawJoystickDigitalScreen(int mouseX, int mouseY){
		fontRendererObj.drawString(I18n.format("gui.config.joystick.choosemap"), guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.selectdigital"), guiLeft+10, guiTop+20, Color.BLACK.getRGB());
		for(GuiButton button : digitalAssignButtons){
			button.drawButton(mc, mouseX, mouseY);
		}
		cancelButton.drawButton(mc, mouseX, mouseY);
		clearButton.drawButton(mc, mouseX, mouseY);
	}
	
	private void drawJoystickAnalogScreen(int mouseX, int mouseY){
		fontRendererObj.drawString(I18n.format("gui.config.joystick.choosemap"), guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.selectanalog"), guiLeft+10, guiTop+20, Color.BLACK.getRGB());
		for(GuiButton button : analogAssignButtons){
			button.drawButton(mc, mouseX, mouseY);
		}
		cancelButton.drawButton(mc, mouseX, mouseY);
		clearButton.drawButton(mc, mouseX, mouseY);
	}
	
	private void drawJoystickCalibrationScreen(int mouseX, int mouseY){
		fontRendererObj.drawString(I18n.format("gui.config.joystick.calibrate1"), guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.calibrate2"), guiLeft+10, guiTop+20, Color.BLACK.getRGB());
		
		ControlSystem.getJoystick().poll();
		if(joystickComponents[joystickComponentId].getPollData() > 0){
			maxTextBox.setText(String.valueOf(Math.max(Double.valueOf(maxTextBox.getText()), joystickComponents[joystickComponentId].getPollData())));
		}else{
			minTextBox.setText(String.valueOf(Math.min(Double.valueOf(minTextBox.getText()), joystickComponents[joystickComponentId].getPollData())));
		}
		maxTextBox.drawTextBox();
		minTextBox.drawTextBox();
		
		confirmButton.drawButton(mc, mouseX, mouseY);
		cancelButton.drawButton(mc, mouseX, mouseY);
		invertButton.displayString = I18n.format("gui.config.joystick.axismode") + (invertButtonPressed ? I18n.format("gui.config.joystick.invert") : I18n.format("gui.config.joystick.normal"));
		invertButton.drawButton(mc, mouseX, mouseY);
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked){
		try{
			super.actionPerformed(buttonClicked);
			if(changedThisTick){
				return;
			}else if(menuButtons.containsKey(buttonClicked)){
				guiState = menuButtons.get(buttonClicked);
			}else if(configButtons.containsKey(buttonClicked)){
				ConfigSystem.setClientConfig(configButtons.get(buttonClicked).configName, !Boolean.valueOf(buttonClicked.displayString));
				buttonClicked.displayString = String.valueOf(ConfigSystem.getBooleanConfig(configButtons.get(buttonClicked).configName));
			}else if(joystickButtons.containsKey(buttonClicked)){
				guiState = GUIStates.JS_BUTTON;
				ControlSystem.setJoystick(joysticks[joystickButtons.get(buttonClicked)]);
				joystickComponents = ControlSystem.getJoystick().getComponents();
			}else if(joystickConfigureButtons.contains(buttonClicked)){
				joystickComponentId = joystickConfigureButtons.indexOf(buttonClicked) + scrollSpot;
				guiState = joystickComponents[joystickComponentId].isAnalog() ? GUIStates.JS_ANALOG : GUIStates.JS_DIGITAL;
				invertButtonPressed = false;
			}else if(digitalAssignButtons.contains(buttonClicked)){
				guiState = GUIStates.JS_BUTTON;
				ControlSystem.setJoystickControl(buttonClicked.displayString, joystickComponentId);
			}else if(analogAssignButtons.contains(buttonClicked)){
				guiState = GUIStates.JS_CALIBRATION;
				controlName = buttonClicked.displayString;
			}else if(buttonClicked.equals(clearButton)){
				byte buttonTypeCode = 2;
				if(guiState.equals(GUIStates.JS_ANALOG)){
					ControlSystem.setAxisBounds(ControlSystem.getJoystickControlName(joystickComponentId), -1, 1, false);
					buttonTypeCode = 0;
				}
				guiState = GUIStates.JS_BUTTON;
				ControlSystem.setJoystickControl(ControlSystem.getJoystickControlName(joystickComponentId), ControlSystem.getNullComponent());
			}else if(buttonClicked.equals(contol_list_upButton)){
				scrollSpot = Math.max(scrollSpot - 9, 0);
			}else if(buttonClicked.equals(control_list_downButton)){
				scrollSpot = Math.min(scrollSpot + 9, joystickComponents.length - joystickComponents.length%9);
			}else if(buttonClicked.equals(deadzone_moreButton)){
				ConfigSystem.setClientConfig("JoystickDeadZone", ((int) (ConfigSystem.getDoubleConfig("JoystickDeadZone")*100) + 1)/100F);
			}else if(buttonClicked.equals(deadzone_lessButton)){
				ConfigSystem.setClientConfig("JoystickDeadZone", ((int) (ConfigSystem.getDoubleConfig("JoystickDeadZone")*100) - 1)/100F);
			}else if(buttonClicked.equals(confirmButton)){
				guiState = GUIStates.JS_BUTTON;
				ControlSystem.setAxisBounds(controlName, Double.valueOf(minTextBox.getText()), Double.valueOf(maxTextBox.getText()), invertButtonPressed);
				ControlSystem.setJoystickControl(controlName, joystickComponentId);
			}else if(buttonClicked.equals(cancelButton)){
				guiState = GUIStates.JS_BUTTON;
			}else if(buttonClicked.equals(invertButton)){
				invertButtonPressed = !invertButtonPressed;
			}
			setInputStatesByLevel();
			changedThisTick = true;
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	private void setInputStatesByLevel(){
		for(GuiButton button : configButtons.keySet()){
			button.enabled = guiState.equals(GUIStates.CONFIG);
		}
		for(GuiTextField textBox : keybindBoxes.keySet()){
			textBox.setVisible(guiState.equals(keybindBoxes.get(textBox).stateToDisplayOn));
		}
		for(GuiButton button : joystickButtons.keySet()){
			button.enabled = guiState.equals(GUIStates.JS_SELECT);
		}
		for(GuiButton button : joystickConfigureButtons){
			button.enabled = guiState.equals(GUIStates.JS_BUTTON);
		}
		for(GuiButton button : digitalAssignButtons){
			button.enabled = guiState.equals(GUIStates.JS_DIGITAL);
		}
		for(GuiButton button : analogAssignButtons){
			button.enabled = guiState.equals(GUIStates.JS_ANALOG);
		}
		
		contol_list_upButton.enabled = guiState.equals(GUIStates.JS_BUTTON);
		control_list_downButton.enabled = guiState.equals(GUIStates.JS_BUTTON);
		deadzone_moreButton.enabled = guiState.equals(GUIStates.JS_BUTTON) && ConfigSystem.getDoubleConfig("JoystickDeadZone") < 1.0F;
		deadzone_lessButton.enabled = guiState.equals(GUIStates.JS_BUTTON) && ConfigSystem.getDoubleConfig("JoystickDeadZone") > 0;
		cancelButton.enabled = guiState.equals(GUIStates.JS_ANALOG) || guiState.equals(GUIStates.JS_DIGITAL) || guiState.equals(GUIStates.JS_CALIBRATION);
		clearButton.enabled = guiState.equals(GUIStates.JS_ANALOG) || guiState.equals(GUIStates.JS_DIGITAL);
		confirmButton.enabled = guiState.equals(GUIStates.JS_CALIBRATION);
		invertButton.enabled = guiState.equals(GUIStates.JS_CALIBRATION);
		maxTextBox.setVisible(guiState.equals(GUIStates.JS_CALIBRATION));
		minTextBox.setVisible(guiState.equals(GUIStates.JS_CALIBRATION));
		maxTextBox.setText("0");
		minTextBox.setText("0");
	}
	
    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
    	super.mouseClicked(x, y, button);
    	for(GuiTextField keybindBox : keybindBoxes.keySet()){
    		if(keybindBox.getVisible()){
    			keybindBox.mouseClicked(x, y, button);
    		}
    	}
    }
	
    @Override
    protected void keyTyped(char key, int bytecode) throws IOException {
    	super.keyTyped(key, bytecode);
    	if(bytecode==1){
            return;
        }
    	for(GuiTextField keybindBox : keybindBoxes.keySet()){
    		if(keybindBox.isFocused()){
    			keybindBox.setText(Keyboard.getKeyName(bytecode));
    			ControlSystem.setKeyboardKey(keybindBoxes.get(keybindBox).keybindName, bytecode);
    			keybindBox.setFocused(false);
    			return;
    		}
    	}
    }
	 */
    	
	private enum ConfigButtons{
		SEA_LEVEL_OFFSET("SeaLevelOffset", "Sea Level Offset", new String[]{"Does altimeter display 0", "at average sea level", "instead of Y=0?"}),
		ELECTRIC_START("ElectricStart", "Electric Start", new String[]{"Enable electric starter?", "If disabled players must", "start engines by hand."}),
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
