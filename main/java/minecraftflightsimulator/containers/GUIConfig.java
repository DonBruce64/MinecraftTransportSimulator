package minecraftflightsimulator.containers;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.utilities.ControlHelper;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;

public class GUIConfig extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation("mfs", "textures/guis/gui_config.png");
	
	private final int xSize = 256;
	private final int ySize = 192;
	private final int offset = 17;
	
	private boolean changedThisTick;
	private int guiLeft;
	private int guiTop;
	private GUILevels guiLevel;
	private int scrollSpot = 0;
	private int joystickComponentId;
	private String controlName;
	private Controller[] joysticks;
	private Component[] joystickComponents;
	
	private GuiButton keyboardButton1;
	private GuiButton keyboardButton2;
	private GuiButton joystickButton;
	private GuiButton upButton;
	private GuiButton downButton;
	private GuiButton confirmButton;
	private GuiButton cancelButton;
	private GuiButton clearButton;
	private GuiTextField maxTextBox;
	private GuiTextField minTextBox;
	
	private List<GuiButton> joystickButtons = new ArrayList<GuiButton>();
	private List<GuiButton> configureButtons = new ArrayList<GuiButton>();
	private List<GuiButton> analogAssignButtons = new ArrayList<GuiButton>();
	private List<GuiButton> digitalAssignButtons = new ArrayList<GuiButton>();
	private Map<String, GuiTextField> keyboard1Boxes = new HashMap<String, GuiTextField>();
	private Map<String, GuiTextField> keyboard2Boxes = new HashMap<String, GuiTextField>();
	
	public GUIConfig(){
		this.allowUserInput=true;
	}
	
	@Override 
	public void initGui(){
		guiLeft = (this.width - this.xSize)/2;
		guiTop = (this.height - this.ySize)/2;
		buttonList.add(keyboardButton1 = new GuiButton(0, guiLeft + 10, guiTop - 20, 85, 20, "Keyboard1"));
		buttonList.add(keyboardButton2 = new GuiButton(0, guiLeft + 95, guiTop - 20, 85, 20, "Keyboard2"));
		buttonList.add(joystickButton = new GuiButton(0, guiLeft + 180, guiTop - 20, 65, 20, "Joystick"));
		guiLevel = GUILevels.KEYBOARD1;
		initKeyboard1Controls();
		initKeyboard2Controls();
		initJoysticks();
		initJoystickButtonList();
		initJoystickControls();
		setButtonStatesByLevel();
	}
	
	private void initKeyboard1Controls(){
		int line = 0;
		int xOffset = 80;
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.PITCH.keyboardIncrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.PITCH.keyboardDecrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.ROLL.keyboardIncrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.ROLL.keyboardDecrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.YAW.keyboardIncrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.YAW.keyboardDecrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.THROTTLE.keyboardIncrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.THROTTLE.keyboardDecrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.FLAPS_U.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.FLAPS_D.keyboardName);
		
		line = 0;
		xOffset = 200;
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.BRAKE.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.STARTER.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.LIGHTS.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.ZOOM_I.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlHelper.controls.ZOOM_O.keyboardName);
	}
	
	private void initKeyboard2Controls(){
		int line = 0;
		int xOffset = 80;
		keyboard2Boxes.put(ControlHelper.controls.CAM.keyboardName, new GuiTextField(fontRendererObj, guiLeft+xOffset, guiTop+10+(line++)*offset, 60, 15));
		keyboard2Boxes.put(ControlHelper.controls.MOD.keyboardName, new GuiTextField(fontRendererObj, guiLeft+xOffset, guiTop+10+(line++)*offset, 60, 15));
	}
	
	private void initJoysticks(){
		joysticks = ControllerEnvironment.getDefaultEnvironment().getControllers();
		for(int i=0;i<joysticks.length;i++){
			joystickButtons.add(new GuiButton(0, guiLeft + 5, guiTop + 40 + 15*i, 240, 15, ""));
			buttonList.add(joystickButtons.get(i));
			joystickButtons.get(i).enabled = false;
		}
	}
	
	private void initJoystickButtonList(){
		for(int i=0; i<9; ++i){
			configureButtons.add(new GuiButton(0, guiLeft+5, guiTop+40+15*i, 215, 15, ""));
			buttonList.add(configureButtons.get(i));
			configureButtons.get(i).enabled = false;
		}
	}
	
	private void initJoystickControls(){
		buttonList.add(upButton = new GuiButton(0, guiLeft + 225, guiTop + 40, 20, 20, "/\\"));
		buttonList.add(downButton = new GuiButton(0, guiLeft + 225, guiTop + 155, 20, 20, "\\/"));
		buttonList.add(confirmButton = new GuiButton(0, guiLeft + 25, guiTop + 160, 100, 20, "Confirm"));
		buttonList.add(cancelButton = new GuiButton(0, guiLeft + 125, guiTop + 160, 100, 20, "Cancel"));
		buttonList.add(clearButton = new GuiButton(0, guiLeft + 25, guiTop + 160, 100, 20, "Clear Assignment"));
		
		createAssignmentButtonAt(guiLeft + 85, guiTop + 40, ControlHelper.controls.PITCH.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 60, ControlHelper.controls.ROLL.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 80, ControlHelper.controls.YAW.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 100,ControlHelper.controls.THROTTLE.joystickName, analogAssignButtons);
		
		createAssignmentButtonAt(guiLeft + 5, guiTop + 30, ControlHelper.controls.FLAPS_U.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 50, ControlHelper.controls.FLAPS_D.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 70, ControlHelper.controls.BRAKE.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 90,ControlHelper.controls.STARTER.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 110,ControlHelper.controls.MOD.joystickName, digitalAssignButtons);

		createAssignmentButtonAt(guiLeft + 85, guiTop + 30,ControlHelper.controls.ZOOM_I.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 50,ControlHelper.controls.ZOOM_O.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 70,ControlHelper.controls.CAM.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 90,ControlHelper.controls.CHANGEVIEW.joystickName, digitalAssignButtons);
		
		createAssignmentButtonAt(guiLeft + 165, guiTop + 30,ControlHelper.controls.LOOK_L.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 50,ControlHelper.controls.LOOK_R.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 70,ControlHelper.controls.LOOK_U.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 90,ControlHelper.controls.LOOK_D.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 110,ControlHelper.controls.LOOK_ALL.joystickName, digitalAssignButtons);
		
		maxTextBox = new GuiTextField(fontRendererObj, guiLeft+40, guiTop+60, 160, 15);
		minTextBox = new GuiTextField(fontRendererObj, guiLeft+40, guiTop+90, 160, 15);
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		changedThisTick = false;
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		keyboardButton1.drawButton(mc, mouseX, mouseY);
		keyboardButton2.drawButton(mc, mouseX, mouseY);
		joystickButton.drawButton(mc, mouseX, mouseY);
		if(guiLevel.equals(GUILevels.KEYBOARD1)){
			drawKeyboardScreen1(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.KEYBOARD2)){
			drawKeyboardScreen2(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.JS_SELECT)){
			drawJoystickSelectionScreen(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.JS_BUTTON)){
			drawJoystickButtonScreen(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.JS_DIGITAL)){
			drawJoystickDigitalScreen(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.JS_ANALOG)){
			drawJoystickAnalogScreen(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.JS_CALIBRATION)){
			drawJoystickCalibrationScreen(mouseX, mouseY);
		}
	}
	
	private void drawKeyboardScreen1(int mouseX, int mouseY){
		int line = 0;
		for(Entry<String, GuiTextField> entry : keyboard1Boxes.entrySet()){
			entry.getValue().setText(ControlHelper.getKeyboardKeyname(entry.getKey()));
			fontRendererObj.drawStringWithShadow(entry.getKey().substring(0, entry.getKey().length() - 3) + ":", entry.getValue().xPosition - 70, entry.getValue().yPosition + 2, Color.WHITE.getRGB());
			if(entry.getValue().isFocused()){
				entry.getValue().setText("");
			}
			entry.getValue().drawTextBox();
		}
	}
	
	private void drawKeyboardScreen2(int mouseX, int mouseY){
		int line = 0;
		int xOffset = 10;
		for(Entry<String, GuiTextField> entry : keyboard2Boxes.entrySet()){
			entry.getValue().setText(ControlHelper.getKeyboardKeyname(entry.getKey()));
			fontRendererObj.drawStringWithShadow(entry.getKey().substring(0, entry.getKey().length() - 3) + ":", entry.getValue().xPosition - 70, entry.getValue().yPosition + 2, Color.WHITE.getRGB());
			if(entry.getValue().isFocused()){
				entry.getValue().setText("");
			}
			entry.getValue().drawTextBox();
			++line;
		}
		int line2 = line;
		fontRendererObj.drawStringWithShadow("RollTrimL:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.ROLL.keyboardDecrementName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("PitchTrimD:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.PITCH.keyboardDecrementName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("YawTrimL:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.YAW.keyboardDecrementName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		++line;
		fontRendererObj.drawStringWithShadow("ParkBrake:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.BRAKE.keyboardName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("EngineOff:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.STARTER.keyboardName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("AUXLights:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.LIGHTS.keyboardName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("HUDMode:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.CAM.keyboardName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		
		xOffset = 130;
		line = line2;
		fontRendererObj.drawStringWithShadow("RollTrimR:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.ROLL.keyboardIncrementName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("PitchTrimU:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.PITCH.keyboardIncrementName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("YawTrimR:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.YAW.keyboardIncrementName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
	}
	
	private void drawJoystickSelectionScreen(int mouseX, int mouseY){
		fontRendererObj.drawString("Chose a joystick:", guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString("Name:", guiLeft+10, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString("Type:", guiLeft+140, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString("Rumble:", guiLeft+200, guiTop+25, Color.BLACK.getRGB());
		for(int i=0; i<joysticks.length; ++i){
			joystickButtons.get(i).drawButton(mc, mouseX, mouseY);
			fontRendererObj.drawString(joysticks[i].getName().substring(0, joysticks[i].getName().length() > 20 ? 20 : joysticks[i].getName().length()), guiLeft+10, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(joysticks[i].getType().toString(), guiLeft+140, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(joysticks[i].getRumblers().length > 0 ? "Yes" : "No", guiLeft+200, guiTop+44+15*i, Color.WHITE.getRGB());
		}
	}
	
	private void drawJoystickButtonScreen(int mouseX, int mouseY){
		upButton.drawButton(mc, mouseX, mouseY);
		downButton.drawButton(mc, mouseX, mouseY);
		fontRendererObj.drawString("Now we need to map buttons.", guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString("#", guiLeft+10, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString("Name:", guiLeft+25, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString("Analog:", guiLeft+90, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString("Assigned to:", guiLeft+140, guiTop+25, Color.BLACK.getRGB());
		for(int i=0; i<9 && i<joystickComponents.length && i+scrollSpot<joystickComponents.length; ++i){
			configureButtons.get(i).drawButton(mc, mouseX, mouseY);
			fontRendererObj.drawString(String.valueOf(i+scrollSpot+1), guiLeft+10, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(joystickComponents[i+scrollSpot].getName().substring(0, joystickComponents[i+scrollSpot].getName().length() > 15 ? 15 : joystickComponents[i+scrollSpot].getName().length()), guiLeft+25, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(joystickComponents[i+scrollSpot].isAnalog() ? "Yes" : "No", guiLeft+100, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(ControlHelper.getJoystickControlName(i+scrollSpot), guiLeft+140, guiTop+44+15*i, Color.WHITE.getRGB());
		}
	}
	
	private void drawJoystickDigitalScreen(int mouseX, int mouseY){
		fontRendererObj.drawString("Choose what gets mapped to this button.", guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString("This DIGITAL button can control:", guiLeft+10, guiTop+20, Color.BLACK.getRGB());
		for(GuiButton button : digitalAssignButtons){
			button.drawButton(mc, mouseX, mouseY);
		}
		cancelButton.drawButton(mc, mouseX, mouseY);
		clearButton.drawButton(mc, mouseX, mouseY);
	}
	
	private void drawJoystickAnalogScreen(int mouseX, int mouseY){
		fontRendererObj.drawString("Choose what gets mapped to this button.", guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString("This ANALOG button can control:", guiLeft+10, guiTop+20, Color.BLACK.getRGB());
		for(GuiButton button : analogAssignButtons){
			button.drawButton(mc, mouseX, mouseY);
		}
		cancelButton.drawButton(mc, mouseX, mouseY);
		clearButton.drawButton(mc, mouseX, mouseY);
	}
	
	private void drawJoystickCalibrationScreen(int mouseX, int mouseY){
		fontRendererObj.drawString("Move the axis until the numbers stop changing.", guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString("Then hit confirm to save settings.", guiLeft+10, guiTop+20, Color.BLACK.getRGB());
		ControlHelper.getJoystick().poll();
		if(joystickComponents[joystickComponentId].getPollData() > 0){
			maxTextBox.setText(String.valueOf(Math.max(Double.valueOf(maxTextBox.getText()), joystickComponents[joystickComponentId].getPollData())));
		}else{
			minTextBox.setText(String.valueOf(Math.min(Double.valueOf(minTextBox.getText()), joystickComponents[joystickComponentId].getPollData())));
		}
		maxTextBox.drawTextBox();
		minTextBox.drawTextBox();
		confirmButton.drawButton(mc, mouseX, mouseY);
		cancelButton.drawButton(mc, mouseX, mouseY);
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked){
		super.actionPerformed(buttonClicked);
		if(changedThisTick){
			return;
		}else if(buttonClicked.equals(keyboardButton1)){
			guiLevel = GUILevels.KEYBOARD1;
		}else if(buttonClicked.equals(keyboardButton2)){
			guiLevel = GUILevels.KEYBOARD2;
		}else if(buttonClicked.equals(joystickButton)){
			guiLevel = GUILevels.JS_SELECT;
		}else if(joystickButtons.contains(buttonClicked)){
			guiLevel = GUILevels.JS_BUTTON;
			ControlHelper.setJoystick(joysticks[joystickButtons.indexOf(buttonClicked)]);
			joystickComponents = ControlHelper.getJoystick().getComponents();
		}else if(configureButtons.contains(buttonClicked)){
			joystickComponentId = configureButtons.indexOf(buttonClicked) + scrollSpot;
			guiLevel = joystickComponents[joystickComponentId].isAnalog() ? GUILevels.JS_ANALOG : GUILevels.JS_DIGITAL;
		}else if(digitalAssignButtons.contains(buttonClicked)){
			guiLevel = GUILevels.JS_BUTTON;
			ControlHelper.setJoystickControl(buttonClicked.displayString, joystickComponentId);
		}else if(analogAssignButtons.contains(buttonClicked)){
			guiLevel = GUILevels.JS_CALIBRATION;
			controlName = buttonClicked.displayString;
		}else if(buttonClicked.equals(clearButton)){
			if(guiLevel.equals(GUILevels.JS_ANALOG)){
				ControlHelper.setAxisBounds(ControlHelper.getJoystickControlName(joystickComponentId), -1, 1);
			}
			guiLevel = GUILevels.JS_BUTTON;
			ControlHelper.setJoystickControl(ControlHelper.getJoystickControlName(joystickComponentId), ControlHelper.getNullComponent());
		}else if(buttonClicked.equals(upButton)){
			scrollSpot = Math.max(scrollSpot - 9, 0);
		}else if(buttonClicked.equals(downButton)){
			scrollSpot = Math.min(scrollSpot + 9, joystickComponents.length - joystickComponents.length%9);
		}else if(buttonClicked.equals(confirmButton)){
			guiLevel = GUILevels.JS_BUTTON;
			ControlHelper.setAxisBounds(controlName, Double.valueOf(minTextBox.getText()), Double.valueOf(maxTextBox.getText()));
			ControlHelper.setJoystickControl(controlName, joystickComponentId);
		}else if(buttonClicked.equals(cancelButton)){
			guiLevel = GUILevels.JS_BUTTON;
		}
		setButtonStatesByLevel();
		changedThisTick = true;
	}

	private void setButtonStatesByLevel(){
		for(GuiTextField box : keyboard1Boxes.values()){
			box.setEnabled(guiLevel.equals(GUILevels.KEYBOARD1));
			box.setVisible(guiLevel.equals(GUILevels.KEYBOARD1));
		}
		for(GuiTextField box : keyboard2Boxes.values()){
			box.setEnabled(guiLevel.equals(GUILevels.KEYBOARD2));
			box.setVisible(guiLevel.equals(GUILevels.KEYBOARD2));
		}
		for(GuiButton button : joystickButtons){
			button.enabled = guiLevel.equals(GUILevels.JS_SELECT);
			button.visible = guiLevel.equals(GUILevels.JS_SELECT);
		}
		for(GuiButton button : configureButtons){
			button.enabled = guiLevel.equals(GUILevels.JS_BUTTON);
			button.visible = guiLevel.equals(GUILevels.JS_BUTTON);
		}
		for(GuiButton button : digitalAssignButtons){
			button.enabled = guiLevel.equals(GUILevels.JS_DIGITAL);
			button.visible = guiLevel.equals(GUILevels.JS_DIGITAL);
		}
		for(GuiButton button : analogAssignButtons){
			button.enabled = guiLevel.equals(GUILevels.JS_ANALOG);
			button.visible = guiLevel.equals(GUILevels.JS_ANALOG);
		}
		upButton.enabled = guiLevel.equals(GUILevels.JS_BUTTON);
		downButton.enabled = guiLevel.equals(GUILevels.JS_BUTTON);
		keyboardButton1.enabled = !guiLevel.equals(GUILevels.KEYBOARD1);
		keyboardButton2.enabled = !guiLevel.equals(GUILevels.KEYBOARD2);
		joystickButton.enabled = !guiLevel.equals(GUILevels.JS_SELECT);
		cancelButton.enabled = guiLevel.equals(GUILevels.JS_ANALOG) || guiLevel.equals(GUILevels.JS_DIGITAL) || guiLevel.equals(GUILevels.JS_CALIBRATION);
		clearButton.enabled = guiLevel.equals(GUILevels.JS_ANALOG) || guiLevel.equals(GUILevels.JS_DIGITAL);
		confirmButton.enabled = guiLevel.equals(GUILevels.JS_CALIBRATION);
		maxTextBox.setVisible(guiLevel.equals(GUILevels.JS_CALIBRATION));
		minTextBox.setVisible(guiLevel.equals(GUILevels.JS_CALIBRATION));
		maxTextBox.setText("0");
		minTextBox.setText("0");
	}
	
    @Override
    protected void mouseClicked(int x, int y, int p_73864_3_){
    	super.mouseClicked(x, y, p_73864_3_);
    	for(GuiTextField box : keyboard1Boxes.values()){
    		if(box.getVisible()){
    			box.mouseClicked(x, y, p_73864_3_);
    		}
    	}
    	for(GuiTextField box : keyboard2Boxes.values()){
    		if(box.getVisible()){
    			box.mouseClicked(x, y, p_73864_3_);
    		}
    	}
    }
	
    @Override
    protected void keyTyped(char key, int bytecode){
    	super.keyTyped(key, bytecode);
    	if(bytecode==1){
            this.mc.displayGuiScreen((GuiScreen)null);
            this.mc.setIngameFocus();
            return;
        }
    	for(Entry<String, GuiTextField> entry : keyboard1Boxes.entrySet()){
    		if(entry.getValue().isFocused()){
    			entry.getValue().setText(Keyboard.getKeyName(bytecode));
    			ControlHelper.setKeyboardKey(entry.getKey(), bytecode);
    			entry.getValue().setFocused(false);
    			MFS.config.save();
    			return;
    		}
    	}
    	for(Entry<String, GuiTextField> entry : keyboard2Boxes.entrySet()){
    		if(entry.getValue().isFocused()){
    			entry.getValue().setText(Keyboard.getKeyName(bytecode));
    			ControlHelper.setKeyboardKey(entry.getKey(), bytecode);
    			entry.getValue().setFocused(false);
    			MFS.config.save();
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
    
	private GuiTextField createKeyBox1At(int posX, int posY, String keyname){
		GuiTextField box = new GuiTextField(fontRendererObj, posX, posY, 40, 15);
		keyboard1Boxes.put(keyname, box);
		return box;
	}
	private GuiTextField createKeyBox2At(int posX, int posY, String keyname){
		GuiTextField box = new GuiTextField(fontRendererObj, posX, posY, 40, 15);
		keyboard2Boxes.put(keyname, box);
		return box;
	}
	
	private enum GUILevels{
		KEYBOARD1,
		KEYBOARD2,
		JS_SELECT,
		JS_BUTTON,
		JS_DIGITAL,
		JS_ANALOG,
		JS_CALIBRATION;
	}
}