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
	private int guiLevel = 0;
	private int scrollSpot = 0;
	private int joystickComponentId;
	private String controlName;
	private Controller[] joysticks;
	private Component[] joystickComponents;
	
	private GuiButton keyboardButton;
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
	private Map<String, GuiTextField> textBoxes = new HashMap<String, GuiTextField>();
	
	public GUIConfig(){
		this.allowUserInput=true;
	}
	
	@Override 
	public void initGui(){
		guiLeft = (this.width - this.xSize)/2;
		guiTop = (this.height - this.ySize)/2;
		
		keyboardButton = new GuiButton(0, guiLeft + 10, guiTop - 20, 115, 20, "Keyboard");
		joystickButton = new GuiButton(0, guiLeft + 125, guiTop - 20, 120, 20, "Joystick");
		upButton = new GuiButton(0, guiLeft + 225, guiTop + 40, 20, 20, "/\\");
		downButton = new GuiButton(0, guiLeft + 225, guiTop + 155, 20, 20, "\\/");
		confirmButton = new GuiButton(0, guiLeft + 25, guiTop + 160, 100, 20, "Confirm");
		cancelButton = new GuiButton(0, guiLeft + 125, guiTop + 160, 100, 20, "Cancel");
		clearButton = new GuiButton(0, guiLeft + 25, guiTop + 160, 100, 20, "Clear Assignment");
		
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
		
		
		buttonList.add(keyboardButton);
		buttonList.add(joystickButton);
		buttonList.add(upButton);
		buttonList.add(downButton);
		buttonList.add(confirmButton);
		buttonList.add(cancelButton);
		buttonList.add(clearButton);
		
		int line = 0;
		createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset, ControlHelper.controls.PITCH.keyboardIncrementName);
		createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset, ControlHelper.controls.PITCH.keyboardDecrementName);
		createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset, ControlHelper.controls.ROLL.keyboardIncrementName);
		createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset, ControlHelper.controls.ROLL.keyboardDecrementName);
		createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset, ControlHelper.controls.THROTTLE.keyboardIncrementName);
		createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset, ControlHelper.controls.THROTTLE.keyboardDecrementName);
		createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset, ControlHelper.controls.BRAKE.keyboardName);
		createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset, ControlHelper.controls.STARTER.keyboardName);
		textBoxes.put(ControlHelper.controls.CAM.keyboardName, new GuiTextField(fontRendererObj, guiLeft+60, guiTop+10+(line++)*offset, 60, 15));
		textBoxes.put(ControlHelper.controls.MOD.keyboardName, new GuiTextField(fontRendererObj, guiLeft+60, guiTop+10+(line++)*offset, 60, 15));
		
		line = 0;
		createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset, ControlHelper.controls.YAW.keyboardIncrementName);
		createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset, ControlHelper.controls.YAW.keyboardDecrementName);
		createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset, ControlHelper.controls.FLAPS_U.keyboardName);
		createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset, ControlHelper.controls.FLAPS_D.keyboardName);		
		createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset, ControlHelper.controls.ZOOM_I.keyboardName);
		createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset, ControlHelper.controls.ZOOM_O.keyboardName);
		
		joysticks = ControllerEnvironment.getDefaultEnvironment().getControllers();
		for(int i=0;i<joysticks.length;i++){
			joystickButtons.add(new GuiButton(0, guiLeft + 5, guiTop + 40 + 15*i, 240, 15, ""));
			buttonList.add(joystickButtons.get(i));
			joystickButtons.get(i).enabled = false;
		}
		for(int i=0; i<9; ++i){
			configureButtons.add(new GuiButton(0, guiLeft+5, guiTop+40+15*i, 215, 15, ""));
			buttonList.add(configureButtons.get(i));
			configureButtons.get(i).enabled = false;
		}
		
		maxTextBox = new GuiTextField(fontRendererObj, guiLeft+40, guiTop+60, 160, 15);
		minTextBox = new GuiTextField(fontRendererObj, guiLeft+40, guiTop+90, 160, 15);
		
		setButtonStatesByLevel();
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		changedThisTick = false;
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		keyboardButton.drawButton(mc, mouseX, mouseY);
		joystickButton.drawButton(mc, mouseX, mouseY);
		if(guiLevel==0){
			for(Entry<String, GuiTextField> entry : textBoxes.entrySet()){
				entry.getValue().setText(ControlHelper.getKeyboardKeyname(entry.getKey()));
				if(entry.getValue().isFocused()){
					entry.getValue().setText("");
				}
				entry.getValue().drawTextBox();
			}
		
			int line = 0;
			fontRendererObj.drawStringWithShadow("PitchUp:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("PitchDown:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("RollRight:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("RollLeft:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("ThrottleUp:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("ThrottleDown:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("Brake:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("Starter:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("CamLock:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("Modifier:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			
			line = 0;
			fontRendererObj.drawStringWithShadow("YawRight:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("YawLeft:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("FlapsUp:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("FlapsDown:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("ZoomIn:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("ZoomOut:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("ParkBrake:", guiLeft+130, guiTop+15+(line)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.BRAKE.keyboardName), guiLeft+190, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("EngineOff:", guiLeft+130, guiTop+15+(line)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow(ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.STARTER.keyboardName), guiLeft+190, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
			fontRendererObj.drawStringWithShadow("HUDMode:", guiLeft+130, guiTop+15+(line)*offset, Color.WHITE.getRGB());
			String text = ControlHelper.getKeyboardKeyname(ControlHelper.controls.MOD.keyboardName) + "+" +  ControlHelper.getKeyboardKeyname(ControlHelper.controls.CAM.keyboardName);
			fontRendererObj.drawStringWithShadow(text.substring(0, text.length() > 10 ? 10 : text.length()), guiLeft+190, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		
		}else if(guiLevel == 1){
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
			
		}else if(guiLevel == 2){
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
		}else if(guiLevel == 3){
			fontRendererObj.drawString("Choose what gets mapped to this button.", guiLeft+10, guiTop+10, Color.BLACK.getRGB());
			fontRendererObj.drawString("This DIGITAL button can control:", guiLeft+10, guiTop+20, Color.BLACK.getRGB());
			for(GuiButton button : digitalAssignButtons){
				button.drawButton(mc, mouseX, mouseY);
			}
			cancelButton.drawButton(mc, mouseX, mouseY);
			clearButton.drawButton(mc, mouseX, mouseY);
		}else if(guiLevel == 4){
			fontRendererObj.drawString("Choose what gets mapped to this button.", guiLeft+10, guiTop+10, Color.BLACK.getRGB());
			fontRendererObj.drawString("This ANALOG button can control:", guiLeft+10, guiTop+20, Color.BLACK.getRGB());
			for(GuiButton button : analogAssignButtons){
				button.drawButton(mc, mouseX, mouseY);
			}
			cancelButton.drawButton(mc, mouseX, mouseY);
			clearButton.drawButton(mc, mouseX, mouseY);
		}else if(guiLevel == 5){
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
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked){
		super.actionPerformed(buttonClicked);
		if(changedThisTick){
			return;
		}else if(buttonClicked.equals(keyboardButton)){
			guiLevel = 0;
		}else if(buttonClicked.equals(joystickButton)){
			guiLevel = 1;
		}else if(joystickButtons.contains(buttonClicked)){
			guiLevel = 2;
			ControlHelper.setJoystick(joysticks[joystickButtons.indexOf(buttonClicked)]);
			joystickComponents = ControlHelper.getJoystick().getComponents();
		}else if(configureButtons.contains(buttonClicked)){
			joystickComponentId = configureButtons.indexOf(buttonClicked) + scrollSpot;
			guiLevel = joystickComponents[joystickComponentId].isAnalog() ? 4 : 3;
		}else if(digitalAssignButtons.contains(buttonClicked)){
			guiLevel = 2;
			ControlHelper.setJoystickControl(buttonClicked.displayString, joystickComponentId);
		}else if(analogAssignButtons.contains(buttonClicked)){
			guiLevel = 5;
			controlName = buttonClicked.displayString;
		}else if(buttonClicked.equals(clearButton)){
			guiLevel = 2;
			if(guiLevel == 4){
				ControlHelper.setAxisBounds(ControlHelper.getJoystickControlName(joystickComponentId), -1, 1);
			}
			ControlHelper.setJoystickControl(ControlHelper.getJoystickControlName(joystickComponentId), ControlHelper.getNullComponent());
		}else if(buttonClicked.equals(upButton)){
			scrollSpot = Math.max(scrollSpot - 9, 0);
		}else if(buttonClicked.equals(downButton)){
			scrollSpot = Math.min(scrollSpot + 9, joystickComponents.length - joystickComponents.length%9);
		}else if(buttonClicked.equals(confirmButton)){
			guiLevel = 2;
			ControlHelper.setAxisBounds(controlName, Double.valueOf(minTextBox.getText()), Double.valueOf(maxTextBox.getText()));
			ControlHelper.setJoystickControl(controlName, joystickComponentId);
		}else if(buttonClicked.equals(cancelButton)){
			guiLevel = 2;
		}
		setButtonStatesByLevel();
		changedThisTick = true;
	}

	private void setButtonStatesByLevel(){
		for(GuiTextField box : textBoxes.values()){
			box.setEnabled(guiLevel == 0);
			box.setVisible(guiLevel == 0);
		}
		for(GuiButton button : joystickButtons){
			button.enabled = guiLevel == 1;
			button.visible = guiLevel == 1;
		}
		for(GuiButton button : configureButtons){
			button.enabled = guiLevel == 2;
			button.visible = guiLevel == 2;
		}
		for(GuiButton button : digitalAssignButtons){
			button.enabled = guiLevel == 3;
			button.visible = guiLevel == 3;
		}
		for(GuiButton button : analogAssignButtons){
			button.enabled = guiLevel == 4;
			button.visible = guiLevel == 4;
		}
		upButton.enabled = guiLevel == 2;
		downButton.enabled = guiLevel == 2;
		keyboardButton.enabled = guiLevel != 0;
		joystickButton.enabled = guiLevel != 1;
		confirmButton.enabled = guiLevel == 5;
		cancelButton.enabled = guiLevel > 2;
		clearButton.enabled = (guiLevel == 3 || guiLevel == 4);
		maxTextBox.setVisible(guiLevel == 5);
		minTextBox.setVisible(guiLevel == 5);
		maxTextBox.setText("0");
		minTextBox.setText("0");
	}
	
    @Override
    protected void mouseClicked(int x, int y, int p_73864_3_){
    	super.mouseClicked(x, y, p_73864_3_);
    	for(GuiTextField box : textBoxes.values()){
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
    	for(Entry<String, GuiTextField> entry : textBoxes.entrySet()){
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
    
	private GuiTextField createKeyBoxAt(int posX, int posY, String keyname){
		GuiTextField box = new GuiTextField(fontRendererObj, posX, posY, 40, 15);
		textBoxes.put(keyname, box);
		return box;
	}
}