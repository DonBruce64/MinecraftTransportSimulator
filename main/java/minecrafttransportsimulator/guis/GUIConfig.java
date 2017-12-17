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
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GUIConfig extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/wide_blank.png");
		
	private boolean changedThisTick;
	private boolean invertButtonPressed;
	
	private int guiLeft;
	private int guiTop;
	private int scrollSpot = 0;
	private int joystickComponentId;
	private String controlName;
	private GUIStates guiState;
	private Controller[] joysticks;
	private Component[] joystickComponents;
	
	private GuiButton contol_list_upButton;
	private GuiButton control_list_downButton;
	private GuiButton deadzone_moreButton;
	private GuiButton deadzone_lessButton;
	private GuiButton confirmButton;
	private GuiButton cancelButton;
	private GuiButton clearButton;
	private GuiButton invertButton;
	private GuiTextField maxTextBox;
	private GuiTextField minTextBox;
	
	private List<GuiButton> joystickConfigureButtons = new ArrayList<GuiButton>();
	private List<GuiButton> analogAssignButtons = new ArrayList<GuiButton>();
	private List<GuiButton> digitalAssignButtons = new ArrayList<GuiButton>();
	
	private Map<GuiButton, GUIStates> menuButtons = new HashMap<GuiButton, GUIStates>();
	private Map<GuiButton, ConfigButtons> configButtons = new HashMap<GuiButton, ConfigButtons>();
	private Map<GuiTextField, KeybindBoxes> keybindBoxes = new HashMap<GuiTextField, KeybindBoxes>();
	private Map<GuiButton, Byte> joystickButtons = new HashMap<GuiButton, Byte>();
	
	public GUIConfig(){
		this.allowUserInput=true;
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 256)/2;
		guiTop = (this.height - 192)/2;
		guiState = GUIStates.PLANE;
		initMenuButtons();
		initConfigControls();
		initKeybindBoxes();
		initJoysticks();
		initJoystickButtonList();
		initJoystickControls();
		setInputStatesByLevel();
	}
	
	private void initMenuButtons(){
		menuButtons.put(new GuiButton(0, guiLeft + 0, guiTop - 20, 50, 20, I18n.format("gui.config.title.config")), GUIStates.CONFIG);
		menuButtons.put(new GuiButton(0, guiLeft + 50, guiTop - 20, 50, 20, I18n.format("gui.config.title.plane")), GUIStates.PLANE);
		menuButtons.put(new GuiButton(0, guiLeft + 100, guiTop - 20, 50, 20, I18n.format("gui.config.title.heli")), GUIStates.HELICOPTER);
		menuButtons.put(new GuiButton(0, guiLeft + 150, guiTop - 20, 50, 20, I18n.format("gui.config.title.vehicle")), GUIStates.VEHICLE);
		menuButtons.put(new GuiButton(0, guiLeft + 200, guiTop - 20, 56, 20, I18n.format("gui.config.title.joystick")), GUIStates.JS_SELECT);
		for(GuiButton button : menuButtons.keySet()){
			buttonList.add(button);
		}
	}
	
	private void initConfigControls(){
		int line = 0;
		int xOffset = 140;
		for(ConfigButtons buttonEnum : ConfigButtons.values()){
			GuiButton newButton = new GuiButton(0, guiLeft+xOffset, guiTop+10+(line++)*20, 60, 20, String.valueOf(ConfigSystem.getBooleanConfig(buttonEnum.configName))); 
			configButtons.put(newButton, buttonEnum);
			buttonList.add(newButton);
		}
	}
	
	private void initKeybindBoxes(){
		for(GUIStates state : GUIStates.values()){
			List<KeybindBoxes> boxEnumList = new ArrayList<KeybindBoxes>();
			for(KeybindBoxes boxEnum : KeybindBoxes.values()){
				if(boxEnum.stateToDisplayOn.equals(state)){
					boxEnumList.add(boxEnum);
				}
			}
			
			byte boxesPerColumn = (byte) (boxEnumList.size()/2);
			for(byte i=0; i<boxEnumList.size(); ++i){
				KeybindBoxes boxEnum = boxEnumList.get(i);
				GuiTextField box;
				if(i < boxesPerColumn){
					box = new GuiTextField(0, fontRendererObj, guiLeft+80, guiTop+10+i*17, 40, 15);
				}else{
					box = new GuiTextField(0, fontRendererObj, guiLeft+200, guiTop+10+(i-boxesPerColumn)*17, 40, 15);
				} 
				keybindBoxes.put(box, boxEnum);
			}
		}
	}
	
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
	
	private void initJoystickButtonList(){
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
		}else if(guiState.equals(GUIStates.PLANE)){
			drawPlaneScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.HELICOPTER)){
			drawHelicopterScreen(mouseX, mouseY);
		}else if(guiState.equals(GUIStates.VEHICLE)){
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
    
    private GuiButton createAssignmentButtonAt(int posX, int posY, String name, List<GuiButton> listToAddTo){
    	GuiButton button = new GuiButton(0, posX, posY, 80, 20, name);
    	buttonList.add(button);
    	listToAddTo.add(button);
    	button.enabled = false;
    	return button;
    }
    
    
	
	private enum GUIStates{
		CONFIG,
		PLANE,
		VEHICLE,
		HELICOPTER,
		JS_SELECT,
		JS_BUTTON,
		JS_DIGITAL,
		JS_ANALOG,
		JS_CALIBRATION;
	}
	
	private enum ConfigButtons{
		SEA_LEVEL_OFFSET("SeaLevelOffset", "Sea Level Offset", new String[]{"Does altimeter display 0", "at average sea level", "instead of Y=0?"}),
		ELECTRIC_START("ElectricStart", "Electric Start", new String[]{"Enable electric starter?", "If disabled players must", "start engines by hand."}),
		XAEROS_COMPATIBILITY("XaerosCompatibility", "Xaeros Compatibility", new String[]{"Enable Xaeros Minimap Compatibility?", "This allows Xaeros Minimap to be shown,", "but makes the hotbar render over the HUD."}),
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
	
	private enum KeybindBoxes{
		AIRCRAFT_PITCH_UP(ControlSystem.Controls.PITCH.keyboardIncrementName, GUIStates.PLANE),
		AIRCRAFT_PITCH_DOWN(ControlSystem.Controls.PITCH.keyboardDecrementName, GUIStates.PLANE),
		AIRCRAFT_ROLL_UP(ControlSystem.Controls.ROLL.keyboardIncrementName, GUIStates.PLANE),
		AIRCRAFT_ROLL_DOWN(ControlSystem.Controls.ROLL.keyboardDecrementName, GUIStates.PLANE),
		AIRCRAFT_YAW_UP(ControlSystem.Controls.YAW.keyboardIncrementName, GUIStates.PLANE),
		AIRCRAFT_YAW_DOWNP(ControlSystem.Controls.YAW.keyboardDecrementName, GUIStates.PLANE),
		AIRCRAFT_THROTTLE_UP(ControlSystem.Controls.THROTTLE.keyboardIncrementName, GUIStates.PLANE),
		AIRCRAFT_THROTTLE_DOWN(ControlSystem.Controls.THROTTLE.keyboardDecrementName, GUIStates.PLANE),
		AIRCRAFT_FLAPS_UP(ControlSystem.Controls.FLAPS_U.keyboardName, GUIStates.PLANE),
		AIRCRAFT_FLAPS_DOWN(ControlSystem.Controls.FLAPS_D.keyboardName, GUIStates.PLANE),
		AIRCRAFT_BRAKE(ControlSystem.Controls.BRAKE.keyboardName, GUIStates.PLANE),
		AIRCRAFT_PANEL(ControlSystem.Controls.PANEL.keyboardName, GUIStates.PLANE),
		AIRCRAFT_ZOOM_IN(ControlSystem.Controls.ZOOM_I.keyboardName, GUIStates.PLANE),
		AIRCRAFT_ZOOM_OUT(ControlSystem.Controls.ZOOM_O.keyboardName, GUIStates.PLANE),
		AIRCRAFT_CAM(ControlSystem.Controls.CAM.keyboardName, GUIStates.PLANE),
		AIRCRAFT_MOD(ControlSystem.Controls.MOD.keyboardName, GUIStates.PLANE);

		private final String keybindName;
		private final GUIStates stateToDisplayOn;
		private KeybindBoxes(String keybindName, GUIStates stateToDisplayOn){
			this.keybindName = keybindName;
			this.stateToDisplayOn = stateToDisplayOn;
		}
	}
}
