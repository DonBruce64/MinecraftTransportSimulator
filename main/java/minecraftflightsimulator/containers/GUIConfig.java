package minecraftflightsimulator.containers;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.other.EntityController;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Property;

import org.lwjgl.input.Keyboard;

import com.google.common.base.Strings;

public class GUIConfig extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation("mfs", "textures/gui_config.png");
	private static final int xSize = 256;
	private static final int ySize = 192;
	private static final int offset = 17;
	private int guiLeft;
	private int guiTop;
	
	private GuiTextField pitchUpKeyBox;
	private GuiTextField pitchDownKeyBox;
	private GuiTextField rollLeftKeyBox;
	private GuiTextField rollRightKeyBox;
	private GuiTextField throttleUpKeyBox;
	private GuiTextField throttleDownKeyBox;
	private GuiTextField yawLeftKeyBox;
	private GuiTextField yawRightKeyBox;
	private GuiTextField flapsUpKeyBox;
	private GuiTextField flapsDownKeyBox;
	private GuiTextField brakeKeyBox;
	private GuiTextField starterKeyBox;
	private GuiTextField zoomInKeyBox;
	private GuiTextField zoomOutKeyBox;
	private GuiTextField camLockKeyBox;
	private GuiTextField modKeyBox;
	private List<GuiTextField> boxes = new ArrayList<GuiTextField>();
	
	public GUIConfig(){
		this.allowUserInput=true;
	}
	
	@Override 
	public void initGui(){
		guiLeft = (this.width - this.xSize)/2;
		guiTop = (this.height - this.ySize)/2;
		int line = 0;
		pitchUpKeyBox = createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset);
		pitchDownKeyBox = createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset);
		rollLeftKeyBox = createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset);
		rollRightKeyBox = createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset);
		throttleUpKeyBox = createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset);
		throttleDownKeyBox = createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset);
		brakeKeyBox = createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset);
		starterKeyBox = createKeyBoxAt(guiLeft+80, guiTop+10+(line++)*offset);
		camLockKeyBox = new GuiTextField(fontRendererObj, guiLeft+60, guiTop+10+(line++)*offset, 60, 15);
		boxes.add(camLockKeyBox);
		modKeyBox = new GuiTextField(fontRendererObj, guiLeft+60, guiTop+10+(line++)*offset, 60, 15);
		boxes.add(modKeyBox);
		
		line = 0;
		yawLeftKeyBox = createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset);
		yawRightKeyBox = createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset);
		flapsUpKeyBox = createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset);
		flapsDownKeyBox = createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset);		
		zoomInKeyBox = createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset);
		zoomOutKeyBox = createKeyBoxAt(guiLeft+200, guiTop+10+(line++)*offset);
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		pitchUpKeyBox.setText(Keyboard.getKeyName(EntityController.pitchUpKey));
		pitchDownKeyBox.setText(Keyboard.getKeyName(EntityController.pitchDownKey));
		rollLeftKeyBox.setText(Keyboard.getKeyName(EntityController.rollLeftKey));
		rollRightKeyBox.setText(Keyboard.getKeyName(EntityController.rollRightKey));
		throttleUpKeyBox.setText(Keyboard.getKeyName(EntityController.throttleUpKey));
		throttleDownKeyBox.setText(Keyboard.getKeyName(EntityController.throttleDownKey));
		brakeKeyBox.setText(Keyboard.getKeyName(EntityController.brakeKey));
		starterKeyBox.setText(Keyboard.getKeyName(EntityController.starterKey));
		camLockKeyBox.setText(Keyboard.getKeyName(EntityController.camLockKey));
		modKeyBox.setText(Keyboard.getKeyName(EntityController.modKey));
		
		yawLeftKeyBox.setText(Keyboard.getKeyName(EntityController.yawLeftKey));
		yawRightKeyBox.setText(Keyboard.getKeyName(EntityController.yawRightKey));
		flapsUpKeyBox.setText(Keyboard.getKeyName(EntityController.flapsUpKey));
		flapsDownKeyBox.setText(Keyboard.getKeyName(EntityController.flapsDownKey));
		zoomInKeyBox.setText(Keyboard.getKeyName(EntityController.zoomInKey));
		zoomOutKeyBox.setText(Keyboard.getKeyName(EntityController.zoomOutKey));
		for(GuiTextField box : boxes){
			if(box.isFocused()){
				box.setText("");
			}
    	}
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		for(GuiTextField box : boxes){
			box.drawTextBox();
		}
		int line = 0;
		fontRendererObj.drawStringWithShadow("PitchUp:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("PitchDown:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("RollLeft:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("RollRight:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("ThrottleUp:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("ThrottleDown:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("Brake:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("Starter:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("CamLock:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("Modifier:", guiLeft+10, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		
		line = 0;
		fontRendererObj.drawStringWithShadow("YawLeft:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("YawRight:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("FlapsUp:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("FlapsDown:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("ZoomIn:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("ZoomOut:", guiLeft+130, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("ParkBrake:", guiLeft+130, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(Keyboard.getKeyName(EntityController.modKey) + "+" + Keyboard.getKeyName(EntityController.brakeKey), guiLeft+190, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("EngineOff:", guiLeft+130, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow(Keyboard.getKeyName(EntityController.modKey) + "+" + Keyboard.getKeyName(EntityController.starterKey), guiLeft+190, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
		fontRendererObj.drawStringWithShadow("HUDMode:", guiLeft+130, guiTop+15+(line)*offset, Color.WHITE.getRGB());
		String text = Keyboard.getKeyName(EntityController.modKey) + "+" + Keyboard.getKeyName(EntityController.camLockKey);
		fontRendererObj.drawStringWithShadow(text.substring(0, text.length() > 10 ? 10 : text.length()), guiLeft+190, guiTop+15+(line++)*offset, Color.WHITE.getRGB());
	}
	
    @Override
    protected void mouseClicked(int x, int y, int p_73864_3_){
    	super.mouseClicked(x, y, p_73864_3_);
    	for(GuiTextField box : boxes){
    		box.mouseClicked(x, y, p_73864_3_);
    	}
    }
	
    @Override
    protected void keyTyped(char key, int bytecode){
    	super.keyTyped(key, bytecode);
    	if(key=='e' || bytecode==1){
            this.mc.displayGuiScreen((GuiScreen)null);
            this.mc.setIngameFocus();
            return;
        }
    	for(GuiTextField box : boxes){
    		if(box.isFocused()){
    			box.setText(Keyboard.getKeyName(bytecode));
    			if(box.equals(pitchUpKeyBox)){
    				EntityController.pitchUpKey=bytecode;
    				MFS.config.getCategory("controls").put("PitchUpKey", new Property("PitchUpKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(pitchDownKeyBox)){
    				EntityController.pitchDownKey=bytecode;
    				MFS.config.getCategory("controls").put("PitchDownKey", new Property("PitchDownKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(rollLeftKeyBox)){
    				EntityController.rollLeftKey=bytecode;
    				MFS.config.getCategory("controls").put("RollLeftKey", new Property("RollLeftKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(rollRightKeyBox)){
    				EntityController.rollRightKey=bytecode;
    				MFS.config.getCategory("controls").put("RollRightKey", new Property("RollRightKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(throttleUpKeyBox)){
    				EntityController.throttleUpKey=bytecode;
    				MFS.config.getCategory("controls").put("ThrottleUpKey", new Property("ThrottleUpKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(throttleDownKeyBox)){
    				EntityController.throttleDownKey=bytecode;
    				MFS.config.getCategory("controls").put("ThrottleDownKey", new Property("ThrottleDownKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(yawLeftKeyBox)){
    				EntityController.yawLeftKey=bytecode;
    				MFS.config.getCategory("controls").put("YawLeftKey", new Property("YawLeftKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(yawRightKeyBox)){
    				EntityController.yawRightKey=bytecode;
    				MFS.config.getCategory("controls").put("YawRightKey", new Property("YawRightKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(flapsUpKeyBox)){
    				EntityController.flapsUpKey=bytecode;
    				MFS.config.getCategory("controls").put("FlapsUpKey", new Property("FlapsUpKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(flapsDownKeyBox)){
    				EntityController.flapsDownKey=bytecode;
    				MFS.config.getCategory("controls").put("FlapsDownKey", new Property("FlapsDownKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(brakeKeyBox)){
    				EntityController.brakeKey=bytecode;
    				MFS.config.getCategory("controls").put("BrakeKey", new Property("BrakeKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(starterKeyBox)){
    				EntityController.starterKey=bytecode;
    				MFS.config.getCategory("controls").put("StarterKey", new Property("StarterKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(zoomInKeyBox)){
    				EntityController.zoomInKey=bytecode;
    				MFS.config.getCategory("controls").put("ZoomInKey", new Property("ZoomInKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(zoomOutKeyBox)){
    				EntityController.zoomOutKey=bytecode;
    				MFS.config.getCategory("controls").put("ZoomOutKey", new Property("ZoomOutKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(modKeyBox)){
    				EntityController.modKey=bytecode;
    				MFS.config.getCategory("controls").put("ModKey", new Property("ModKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}else if(box.equals(camLockKeyBox)){
    				EntityController.camLockKey=bytecode;
    				MFS.config.getCategory("controls").put("CamLockKey", new Property("CamLockKey", String.valueOf(bytecode), Property.Type.INTEGER));
    			}
    			box.setFocused(false);
    			MFS.config.save();
    		}
    	}
    }
    
	private GuiTextField createKeyBoxAt(int posX, int posY){
		GuiTextField box = new GuiTextField(fontRendererObj, posX, posY, 40, 15);
		boxes.add(box);
		return box;
	}
}