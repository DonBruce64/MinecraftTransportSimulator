package minecrafttransportsimulator.guis;

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
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class GUIConfig extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/wide_blank.png");
	
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
	
	private GuiButton configButton;
	private GuiButton planeButton;
	private GuiButton helicopterButton;
	private GuiButton vehicleButton;
	private GuiButton joystickButton;
	
	private GuiButton seaLevelOffsetButton;
	private GuiButton electricStartButton;
	private GuiButton xaerosCompatibilityButton;
	private GuiButton mouseYokeButton;
	private GuiTextField joystickForceFactor;
	private GuiTextField controlSurfaceCooldown;
	private GuiTextField joystickDeadZone;
	
	private GuiButton upButton;
	private GuiButton downButton;
	private GuiButton confirmButton;
	private GuiButton cancelButton;
	private GuiButton clearButton;
	private GuiTextField maxTextBox;
	private GuiTextField minTextBox;
	
	private List<GuiButton> configureButtons = new ArrayList<GuiButton>();
	private List<GuiButton> joystickConfigureButtons = new ArrayList<GuiButton>();
	private List<GuiButton> analogAssignButtons = new ArrayList<GuiButton>();
	private List<GuiButton> digitalAssignButtons = new ArrayList<GuiButton>();
	private Map<String, GuiTextField> configureBoxes = new HashMap<String, GuiTextField>();
	private Map<String, GuiTextField> planeBoxes = new HashMap<String, GuiTextField>();
	private Map<String, GuiTextField> helicopterBoxes = new HashMap<String, GuiTextField>();
	private Map<String, GuiTextField> vehicleBoxes = new HashMap<String, GuiTextField>();
	private Map<GuiButton, Byte> joystickButtons = new HashMap<GuiButton, Byte>();
	
	public GUIConfig(){
		this.allowUserInput=true;
	}
	
	@Override 
	public void initGui(){
		guiLeft = (this.width - this.xSize)/2;
		guiTop = (this.height - this.ySize)/2;
		buttonList.add(configButton = new GuiButton(0, guiLeft + 0, guiTop - 20, 50, 20, I18n.format("gui.config.title.config")));
		buttonList.add(planeButton = new GuiButton(0, guiLeft + 50, guiTop - 20, 50, 20, I18n.format("gui.config.title.plane")));
		buttonList.add(helicopterButton = new GuiButton(0, guiLeft + 100, guiTop - 20, 50, 20, I18n.format("gui.config.title.heli")));
		buttonList.add(vehicleButton = new GuiButton(0, guiLeft + 150, guiTop - 20, 50, 20, I18n.format("gui.config.title.vehicle")));
		buttonList.add(joystickButton = new GuiButton(0, guiLeft + 200, guiTop - 20, 56, 20, I18n.format("gui.config.title.joystick")));
		guiLevel = GUILevels.PLANE;
		initConfigControls();
		initPlaneControls();
		initJoysticks();
		initJoystickButtonList();
		initJoystickControls();
		setButtonStatesByLevel();
	}
	
	private void initConfigControls(){
		int line = 0;
		int xOffset = 140;
		//TOOD add other configs.
		configureButtons.add(seaLevelOffsetButton = new GuiButton(0, guiLeft+xOffset, guiTop+10+(line++)*20, 60, 20, String.valueOf(ConfigSystem.getBooleanConfig("SeaLevelOffset"))));
		configureButtons.add(electricStartButton = new GuiButton(0, guiLeft+xOffset, guiTop+10+(line++)*20, 60, 20, String.valueOf(ConfigSystem.getBooleanConfig("ElectricStart"))));
		configureButtons.add(xaerosCompatibilityButton = new GuiButton(0, guiLeft+xOffset, guiTop+10+(line++)*20, 60, 20, String.valueOf(ConfigSystem.getBooleanConfig("XaerosCompatibility"))));
		configureButtons.add(mouseYokeButton = new GuiButton(0, guiLeft+xOffset, guiTop+10+(line++)*20, 60, 20, String.valueOf(ConfigSystem.getBooleanConfig("MouseYoke"))));
		for(GuiButton button : configureButtons){
			buttonList.add(button);
		}
	}
	
	private void initPlaneControls(){
		int line = 0;
		int xOffset = 80;
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.PITCH.keyboardIncrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.PITCH.keyboardDecrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.ROLL.keyboardIncrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.ROLL.keyboardDecrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.YAW.keyboardIncrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.YAW.keyboardDecrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.THROTTLE.keyboardIncrementName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.THROTTLE.keyboardDecrementName);
		
		line = 0;
		xOffset = 200;
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.FLAPS_U.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.FLAPS_D.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.BRAKE.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.PANEL.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.ZOOM_I.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.ZOOM_O.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.CAM.keyboardName);
		createKeyBox1At(guiLeft+xOffset, guiTop+10+(line++)*offset, ControlSystem.controls.MOD.keyboardName);
	}
	
	private void initHelicopterControls(){
		
	}
	
	private void initJoysticks(){
		joysticks = ControllerEnvironment.getDefaultEnvironment().getControllers();
		byte jsNumber = 0;
		byte jsIndex = 0;
		for(Controller joystick : joysticks){
			if(!joystick.getType().equals(Controller.Type.MOUSE) && !joystick.getType().equals(Controller.Type.KEYBOARD)){
				GuiButton jsButton = new GuiButton(0, guiLeft + 5, guiTop + 40 + 15*jsNumber, 240, 15, "");
				joystickButtons.put(jsButton, jsIndex);
				buttonList.add(jsButton);
				jsButton.enabled = false;
				++jsNumber;
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
		buttonList.add(upButton = new GuiButton(0, guiLeft + 225, guiTop + 40, 20, 20, "/\\"));
		buttonList.add(downButton = new GuiButton(0, guiLeft + 225, guiTop + 155, 20, 20, "\\/"));
		buttonList.add(confirmButton = new GuiButton(0, guiLeft + 25, guiTop + 160, 100, 20, I18n.format("gui.config.joystick.confirm")));
		buttonList.add(cancelButton = new GuiButton(0, guiLeft + 125, guiTop + 160, 100, 20, I18n.format("gui.config.joystick.cancel")));
		buttonList.add(clearButton = new GuiButton(0, guiLeft + 25, guiTop + 160, 100, 20, I18n.format("gui.config.joystick.clear")));
		
		createAssignmentButtonAt(guiLeft + 85, guiTop + 40, ControlSystem.controls.PITCH.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 60, ControlSystem.controls.ROLL.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 80, ControlSystem.controls.YAW.joystickName, analogAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 100,ControlSystem.controls.THROTTLE.joystickName, analogAssignButtons);
		
		createAssignmentButtonAt(guiLeft + 5, guiTop + 30, ControlSystem.controls.FLAPS_U.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 50, ControlSystem.controls.FLAPS_D.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 70, ControlSystem.controls.BRAKE.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 90,ControlSystem.controls.PANEL.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 5, guiTop + 110,ControlSystem.controls.MOD.joystickName, digitalAssignButtons);

		createAssignmentButtonAt(guiLeft + 85, guiTop + 30,ControlSystem.controls.ZOOM_I.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 50,ControlSystem.controls.ZOOM_O.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 70,ControlSystem.controls.CAM.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 85, guiTop + 90,ControlSystem.controls.CHANGEVIEW.joystickName, digitalAssignButtons);
		
		createAssignmentButtonAt(guiLeft + 165, guiTop + 30,ControlSystem.controls.LOOK_L.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 50,ControlSystem.controls.LOOK_R.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 70,ControlSystem.controls.LOOK_U.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 90,ControlSystem.controls.LOOK_D.joystickName, digitalAssignButtons);
		createAssignmentButtonAt(guiLeft + 165, guiTop + 110,ControlSystem.controls.LOOK_ALL.joystickName, digitalAssignButtons);
		
		maxTextBox = new GuiTextField(fontRendererObj, guiLeft+40, guiTop+60, 160, 15);
		minTextBox = new GuiTextField(fontRendererObj, guiLeft+40, guiTop+90, 160, 15);
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		changedThisTick = false;
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		configButton.drawButton(mc, mouseX, mouseY);
		planeButton.drawButton(mc, mouseX, mouseY);
		helicopterButton.drawButton(mc, mouseX, mouseY);
		vehicleButton.drawButton(mc, mouseX, mouseY);
		joystickButton.drawButton(mc, mouseX, mouseY);
		if(guiLevel.equals(GUILevels.CONFIG)){
			drawConfigScreen(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.PLANE)){
			drawPlaneScreen(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.HELICOPTER)){
			drawHelicopterScreen(mouseX, mouseY);
		}else if(guiLevel.equals(GUILevels.VEHICLE)){
			drawVehicleScreen(mouseX, mouseY);
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
	
	private static boolean isPointInRegion(int minX, int maxX, int minY, int maxY, int mouseX, int mouseY){
		return mouseX > minX && mouseX < maxX && mouseY > minY && mouseY < maxY;
	}
	
	private void drawConfigScreen(int mouseX, int mouseY){
		int line = 0;
		fontRendererObj.drawStringWithShadow("Sea Level Offset:", guiLeft+10, guiTop+15+(line++)*20, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("Electric Start:", guiLeft+10, guiTop+15+(line++)*20, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("Xaeros Compatibility:", guiLeft+10, guiTop+15+(line++)*20, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("Mouse Yoke:", guiLeft+10, guiTop+15+(line++)*20, Color.WHITE.getRGB());
		
		for(GuiButton button : configureButtons){
			button.drawButton(mc, mouseX, mouseY);
		}
		
		if(isPointInRegion(seaLevelOffsetButton.xPosition, seaLevelOffsetButton.xPosition + seaLevelOffsetButton.width, seaLevelOffsetButton.yPosition, seaLevelOffsetButton.yPosition + seaLevelOffsetButton.height, mouseX, mouseY)){
			drawHoveringText(Arrays.asList(new String[] {"Does altimeter display 0", "at average sea level", "instead of Y=0?"}), mouseX, mouseY, fontRendererObj);
		}else if(isPointInRegion(electricStartButton.xPosition, electricStartButton.xPosition + electricStartButton.width, electricStartButton.yPosition, electricStartButton.yPosition + electricStartButton.height, mouseX, mouseY)){
			drawHoveringText(Arrays.asList(new String[] {"Enable electric starter?", "If disabled players must", "start engines by hand."}), mouseX, mouseY, fontRendererObj);
		}else if(isPointInRegion(xaerosCompatibilityButton.xPosition, xaerosCompatibilityButton.xPosition + xaerosCompatibilityButton.width, xaerosCompatibilityButton.yPosition, xaerosCompatibilityButton.yPosition + xaerosCompatibilityButton.height, mouseX, mouseY)){
			drawHoveringText(Arrays.asList(new String[] {"Enable Xaeros Minimap Compatibility?", "This allows Xaeros Minimap to be shown,", "but makes the hotbar render over the HUD."}), mouseX, mouseY, fontRendererObj);
		}else if(isPointInRegion(mouseYokeButton.xPosition, mouseYokeButton.xPosition + mouseYokeButton.width, mouseYokeButton.yPosition, mouseYokeButton.yPosition + mouseYokeButton.height, mouseX, mouseY)){
			drawHoveringText(Arrays.asList(new String[] {"Enable Mouse Yoke?", "Prevents looking around unless unlocked.", "Think MCHeli controls."}), mouseX, mouseY, fontRendererObj);
		}
	}
	
	private void drawPlaneScreen(int mouseX, int mouseY){
		for(Entry<String, GuiTextField> entry : planeBoxes.entrySet()){
			entry.getValue().setText(ControlSystem.getKeyboardKeyname(entry.getKey()));
			fontRendererObj.drawStringWithShadow(entry.getKey() + ":", entry.getValue().xPosition - 70, entry.getValue().yPosition + 2, Color.WHITE.getRGB());
			if(entry.getValue().isFocused()){
				entry.getValue().setText("");
			}
			entry.getValue().drawTextBox();
		}
		int line = 8;
		int xOffset = 10;
		fontRendererObj.drawStringWithShadow("ParkBrake:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawString(ControlSystem.getKeyboardKeyname(ControlSystem.controls.MOD.keyboardName) + "+" +  ControlSystem.getKeyboardKeyname(ControlSystem.controls.BRAKE.keyboardName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.BLACK.getRGB());
		fontRendererObj.drawStringWithShadow("HUDMode:", guiLeft+xOffset, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawString(ControlSystem.getKeyboardKeyname(ControlSystem.controls.MOD.keyboardName) + "+" +  ControlSystem.getKeyboardKeyname(ControlSystem.controls.CAM.keyboardName), guiLeft+xOffset+60, guiTop+15+(line++)*offset, Color.BLACK.getRGB());
	}
	
	private void drawHelicopterScreen(int mouseX, int mouseY){
		for(Entry<String, GuiTextField> entry : helicopterBoxes.entrySet()){
			entry.getValue().setText(ControlSystem.getKeyboardKeyname(entry.getKey()));
			fontRendererObj.drawStringWithShadow(entry.getKey() + ":", entry.getValue().xPosition - 70, entry.getValue().yPosition + 2, Color.WHITE.getRGB());
			if(entry.getValue().isFocused()){
				entry.getValue().setText("");
			}
			entry.getValue().drawTextBox();
		}
	}
	
	private void drawVehicleScreen(int mouseX, int mouseY){
		for(Entry<String, GuiTextField> entry : vehicleBoxes.entrySet()){
			entry.getValue().setText(ControlSystem.getKeyboardKeyname(entry.getKey()));
			fontRendererObj.drawStringWithShadow(entry.getKey() + ":", entry.getValue().xPosition - 70, entry.getValue().yPosition + 2, Color.WHITE.getRGB());
			if(entry.getValue().isFocused()){
				entry.getValue().setText("");
			}
			entry.getValue().drawTextBox();
		}
	}
	
	private void drawJoystickSelectionScreen(int mouseX, int mouseY){
		
		fontRendererObj.drawString(I18n.format("gui.config.joystick.select"), guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.name"), guiLeft+10, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.type"), guiLeft+140, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.rumble"), guiLeft+200, guiTop+25, Color.BLACK.getRGB());
		
		for(GuiButton jsButton : joystickButtons.keySet()){
			jsButton.drawButton(mc, mouseX, mouseY);
		}
		
		byte jsCount = 0;
		for(GuiButton jsButton : joystickButtons.keySet()){
			byte jsNumber = joystickButtons.get(jsButton);
			fontRendererObj.drawString(joysticks[jsNumber].getName().substring(0, joysticks[jsNumber].getName().length() > 20 ? 20 : joysticks[jsNumber].getName().length()), guiLeft+10, guiTop+44+15*jsCount, Color.WHITE.getRGB());
			fontRendererObj.drawString(joysticks[jsNumber].getType().toString(), guiLeft+140, guiTop+44+15*jsCount, Color.WHITE.getRGB());
			fontRendererObj.drawString(joysticks[jsNumber].getRumblers().length > 0 ? "X" : "", guiLeft+200, guiTop+44+15*jsCount, Color.WHITE.getRGB());
			++jsCount;
		}
	}
	
	private void drawJoystickButtonScreen(int mouseX, int mouseY){
		upButton.drawButton(mc, mouseX, mouseY);
		downButton.drawButton(mc, mouseX, mouseY);
		fontRendererObj.drawString(I18n.format("gui.config.joystick.domap"), guiLeft+10, guiTop+10, Color.BLACK.getRGB());
		fontRendererObj.drawString("#", guiLeft+10, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.name"), guiLeft+25, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.analog"), guiLeft+90, guiTop+25, Color.BLACK.getRGB());
		fontRendererObj.drawString(I18n.format("gui.config.joystick.assignment"), guiLeft+140, guiTop+25, Color.BLACK.getRGB());
		for(int i=0; i<9 && i<joystickComponents.length && i+scrollSpot<joystickComponents.length; ++i){
			joystickConfigureButtons.get(i).drawButton(mc, mouseX, mouseY);
			fontRendererObj.drawString(String.valueOf(i+scrollSpot+1), guiLeft+10, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(joystickComponents[i+scrollSpot].getName().substring(0, joystickComponents[i+scrollSpot].getName().length() > 15 ? 15 : joystickComponents[i+scrollSpot].getName().length()), guiLeft+25, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(joystickComponents[i+scrollSpot].isAnalog() ? "X" : "", guiLeft+100, guiTop+44+15*i, Color.WHITE.getRGB());
			fontRendererObj.drawString(ControlSystem.getJoystickControlName(i+scrollSpot), guiLeft+140, guiTop+44+15*i, Color.WHITE.getRGB());
		}
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
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked){
		try {
			super.actionPerformed(buttonClicked);
			if(changedThisTick){
				return;
			}else if(buttonClicked.equals(configButton)){
				guiLevel = GUILevels.CONFIG;
			}else if(buttonClicked.equals(planeButton)){
				guiLevel = GUILevels.PLANE;
			}else if(buttonClicked.equals(helicopterButton)){
				guiLevel = GUILevels.HELICOPTER;
			}else if(buttonClicked.equals(vehicleButton)){
				guiLevel = GUILevels.VEHICLE;
			}else if(buttonClicked.equals(joystickButton)){
				guiLevel = GUILevels.JS_SELECT;
			}else if(buttonClicked.equals(seaLevelOffsetButton)){
				ConfigSystem.setClientConfig("SeaLevelOffset", !Boolean.valueOf(seaLevelOffsetButton.displayString));
				seaLevelOffsetButton.displayString = String.valueOf(ConfigSystem.getBooleanConfig("SeaLevelOffset"));
			}else if(buttonClicked.equals(electricStartButton)){
				ConfigSystem.setClientConfig("ElectricStart", !Boolean.valueOf(electricStartButton.displayString));
				electricStartButton.displayString = String.valueOf(ConfigSystem.getBooleanConfig("ElectricStart"));
			}else if(buttonClicked.equals(xaerosCompatibilityButton)){
				ConfigSystem.setClientConfig("XaerosCompatibility", !Boolean.valueOf(xaerosCompatibilityButton.displayString));
				xaerosCompatibilityButton.displayString = String.valueOf(ConfigSystem.getBooleanConfig("XaerosCompatibility"));
			}else if(buttonClicked.equals(mouseYokeButton)){
				ConfigSystem.setClientConfig("MouseYoke", !Boolean.valueOf(mouseYokeButton.displayString));
				mouseYokeButton.displayString = String.valueOf(ConfigSystem.getBooleanConfig("MouseYoke"));
			}else if(joystickButtons.containsKey(buttonClicked)){
				guiLevel = GUILevels.JS_BUTTON;
				ControlSystem.setJoystick(joysticks[joystickButtons.get(buttonClicked)]);
				joystickComponents = ControlSystem.getJoystick().getComponents();
			}else if(joystickConfigureButtons.contains(buttonClicked)){
				joystickComponentId = joystickConfigureButtons.indexOf(buttonClicked) + scrollSpot;
				guiLevel = joystickComponents[joystickComponentId].isAnalog() ? GUILevels.JS_ANALOG : GUILevels.JS_DIGITAL;
			}else if(digitalAssignButtons.contains(buttonClicked)){
				guiLevel = GUILevels.JS_BUTTON;
				ControlSystem.setJoystickControl(buttonClicked.displayString, joystickComponentId);
			}else if(analogAssignButtons.contains(buttonClicked)){
				guiLevel = GUILevels.JS_CALIBRATION;
				controlName = buttonClicked.displayString;
			}else if(buttonClicked.equals(clearButton)){
				if(guiLevel.equals(GUILevels.JS_ANALOG)){
					ControlSystem.setAxisBounds(ControlSystem.getJoystickControlName(joystickComponentId), -1, 1);
				}
				guiLevel = GUILevels.JS_BUTTON;
				ControlSystem.setJoystickControl(ControlSystem.getJoystickControlName(joystickComponentId), ControlSystem.getNullComponent());
			}else if(buttonClicked.equals(upButton)){
				scrollSpot = Math.max(scrollSpot - 9, 0);
			}else if(buttonClicked.equals(downButton)){
				scrollSpot = Math.min(scrollSpot + 9, joystickComponents.length - joystickComponents.length%9);
			}else if(buttonClicked.equals(confirmButton)){
				guiLevel = GUILevels.JS_BUTTON;
				ControlSystem.setAxisBounds(controlName, Double.valueOf(minTextBox.getText()), Double.valueOf(maxTextBox.getText()));
				ControlSystem.setJoystickControl(controlName, joystickComponentId);
			}else if(buttonClicked.equals(cancelButton)){
				guiLevel = GUILevels.JS_BUTTON;
			}
			setButtonStatesByLevel();
			changedThisTick = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setButtonStatesByLevel(){
		for(GuiButton button : configureButtons){
			button.enabled = guiLevel.equals(GUILevels.CONFIG);
		}
		for(GuiTextField box : planeBoxes.values()){
			box.setEnabled(guiLevel.equals(GUILevels.PLANE));
		}
		for(GuiTextField box : helicopterBoxes.values()){
			box.setEnabled(guiLevel.equals(GUILevels.HELICOPTER));
		}
		for(GuiTextField box : vehicleBoxes.values()){
			box.setEnabled(guiLevel.equals(GUILevels.VEHICLE));
		}
		for(GuiButton button : joystickButtons.keySet()){
			button.enabled = guiLevel.equals(GUILevels.JS_SELECT);
		}
		for(GuiButton button : joystickConfigureButtons){
			button.enabled = guiLevel.equals(GUILevels.JS_BUTTON);
		}
		for(GuiButton button : digitalAssignButtons){
			button.enabled = guiLevel.equals(GUILevels.JS_DIGITAL);
		}
		for(GuiButton button : analogAssignButtons){
			button.enabled = guiLevel.equals(GUILevels.JS_ANALOG);
		}
		
		upButton.enabled = guiLevel.equals(GUILevels.JS_BUTTON);
		downButton.enabled = guiLevel.equals(GUILevels.JS_BUTTON);
		configButton.enabled = !guiLevel.equals(GUILevels.CONFIG);
		planeButton.enabled = !guiLevel.equals(GUILevels.PLANE);
		helicopterButton.enabled = !guiLevel.equals(GUILevels.HELICOPTER);
		vehicleButton.enabled = !guiLevel.equals(GUILevels.VEHICLE);
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
    protected void mouseClicked(int x, int y, int button) throws IOException {
    	super.mouseClicked(x, y, button);
    	for(GuiTextField box : planeBoxes.values()){
    		if(box.getVisible()){
    			box.mouseClicked(x, y, button);
    		}
    	}
    	for(GuiTextField box : helicopterBoxes.values()){
    		if(box.getVisible()){
    			box.mouseClicked(x, y, button);
    		}
    	}
    }
	
    @Override
    protected void keyTyped(char key, int bytecode) throws IOException {
    	super.keyTyped(key, bytecode);
    	if(bytecode==1){
            return;
        }
    	for(Entry<String, GuiTextField> entry : planeBoxes.entrySet()){
    		if(entry.getValue().isFocused()){
    			entry.getValue().setText(Keyboard.getKeyName(bytecode));
    			ControlSystem.setKeyboardKey(entry.getKey(), bytecode);
    			entry.getValue().setFocused(false);
    			return;
    		}
    	}
    	for(Entry<String, GuiTextField> entry : helicopterBoxes.entrySet()){
    		if(entry.getValue().isFocused()){
    			entry.getValue().setText(Keyboard.getKeyName(bytecode));
    			ControlSystem.setKeyboardKey(entry.getKey(), bytecode);
    			entry.getValue().setFocused(false);
    			return;
    		}
    	}
    	for(Entry<String, GuiTextField> entry : vehicleBoxes.entrySet()){
    		if(entry.getValue().isFocused()){
    			entry.getValue().setText(Keyboard.getKeyName(bytecode));
    			ControlSystem.setKeyboardKey(entry.getKey(), bytecode);
    			entry.getValue().setFocused(false);
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
		planeBoxes.put(keyname, box);
		return box;
	}
	
	private enum GUILevels{
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
}