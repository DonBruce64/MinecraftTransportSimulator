package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleSign;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONSign;
import minecrafttransportsimulator.packets.tileentities.PacketSignChange;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GUISign extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/crafting.png");	
	private final WrapperPlayer player;
	private final TileEntityPoleSign sign;
	private final TileEntityPoleSign signGUIInstance;
	private final List<GuiTextField> signTextBoxes = new ArrayList<GuiTextField>();
	
	private GuiButton leftPackButton;
	private GuiButton rightPackButton;
	private GuiButton leftSignButton;
	private GuiButton rightSignButton;
	private GuiButton startButton;
	
	private int guiLeft;
	private int guiTop;
	
	private String currentPack;
	private String prevPack;
	private String nextPack;
	
	private JSONSign currentSign;
	private JSONSign prevSign;
	private JSONSign nextSign;
			
	public GUISign(TileEntityPoleSign sign, WrapperPlayer player){
		this.sign = sign;
		this.signGUIInstance = new TileEntityPoleSign();
		this.signGUIInstance.setPos(sign.getPos());
		this.player = player;
		if(sign.definition != null){
			currentPack = sign.definition.packID;
			currentSign = sign.definition;
		}
		updateSignNames();
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 256)/2;
		guiTop = (this.height - 220)/2;
		
		buttonList.add(leftPackButton = new GuiButton(0, guiLeft + 25, guiTop + 5, 20, 20, "<"));
		buttonList.add(rightPackButton = new GuiButton(0, guiLeft + 215, guiTop + 5, 20, 20, ">"));
		buttonList.add(leftSignButton = new GuiButton(0, guiLeft + 25, guiTop + 25, 20, 20, "<"));
		buttonList.add(rightSignButton = new GuiButton(0, guiLeft + 215, guiTop + 25, 20, 20, ">"));
		buttonList.add(startButton = new GuiButton(0, guiLeft + 188, guiTop + 170, 20, 20, ""));
		
		for(byte i=0; i<10; ++i){
			signTextBoxes.add(new GuiTextField(0, fontRenderer, guiLeft + 9, guiTop + 54 + i*10, 125, 10));
			signTextBoxes.get(i).setEnabled(false);
			//Set the text box text to the current sign if it has text.
			if(sign.text.size() > i){
				signTextBoxes.get(i).setText(sign.text.get(i));
			}else{
				signTextBoxes.get(i).setText("");
			}
		}
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		//Draw background layer.
		GL11.glColor3f(1, 1, 1); //Not sure why buttons make this grey, but whatever...
		mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 201);
		
		//If we have a valid sign, draw the start arrow.
		if(startButton.enabled){
			drawTexturedModalRect(guiLeft + 140, guiTop + 173, 0, 201, 44, 16);
		}
		
		//Render the text headers.
		drawCenteredString(currentPack != null ? I18n.format("itemGroup." + currentPack) : "", guiLeft + 130, guiTop + 10);
		drawCenteredString(currentSign != null ? (currentSign.general.name != null ? currentSign.general.name : currentSign.systemName) : "", guiLeft + 130, guiTop + 30);
		
		//Set button states and render.
		startButton.enabled = currentSign != null;
		leftPackButton.enabled = prevPack != null;
		rightPackButton.enabled = nextPack != null;
		leftSignButton.enabled = prevSign != null;
		rightSignButton.enabled = nextSign != null;
		for(GuiButton button : buttonList){
			button.drawButton(mc, mouseX, mouseY, 0);
		}
		drawRect(guiLeft + 190, guiTop + 188, guiLeft + 206, guiTop + 172, startButton.enabled ? Color.GREEN.getRGB() : Color.RED.getRGB());
		
		//Now make the selected sign render in the GUI using the TE code.
		//Also set the sign text boxes to render.
		if(currentSign != null){
			signGUIInstance.definition = currentSign;
			signGUIInstance.text.clear();
			if(currentSign.general.textLines != null){
				for(byte i=0; i<currentSign.general.textLines.length; ++i){
					signTextBoxes.get(i).setMaxStringLength(currentSign.general.textLines[i].characters);
					signTextBoxes.get(i).setEnabled(true);
					signTextBoxes.get(i).drawTextBox();
					signGUIInstance.text.add(signTextBoxes.get(i).getText());
				}
			}
			
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glTranslatef(guiLeft + 196, guiTop + 107, 100);
			GL11.glRotatef(180, 0, 1, 0);
			float scale = -90F;
			GL11.glScalef(scale, scale, scale);
			TileEntityRendererDispatcher.instance.render(signGUIInstance, -0.5F, -0.5F, -0.5F, renderPartialTicks, 0);
			GL11.glPopMatrix();
		}
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(startButton)){
			MTS.MTSNet.sendToServer(new PacketSignChange(signGUIInstance, player.getEntityId()));
			mc.player.closeScreen();
		}else{
			if(buttonClicked.equals(leftPackButton)){
				currentPack = prevPack;
				currentSign = null;
			}else if(buttonClicked.equals(rightPackButton)){
				currentPack = nextPack;
				currentSign = null;
			}else if(buttonClicked.equals(leftSignButton)){
				currentSign = prevSign;
			}else if(buttonClicked.equals(rightSignButton)){
				currentSign = nextSign;
			}
			updateSignNames();
		}
	}
	
	/**
	 * We also use the mouse wheel for selections as well as buttons.
	 * Forward the call to the button input system for processing.
	 */
	@Override
    public void handleMouseInput() throws IOException{
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();
        if(i > 0 && rightSignButton.enabled){
        	this.actionPerformed(rightSignButton);
        }else if(i < 0 && leftSignButton.enabled){
        	this.actionPerformed(leftSignButton);
        }
	}
	
	@Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
    	super.mouseClicked(x, y, button);
    	for(GuiTextField box : signTextBoxes){
    		if(box.getVisible()){
    			box.mouseClicked(x, y, button);
    		}
    	}
    }
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException{
		if(keyCode == 1){
			super.keyTyped('0', 1);
        }else{
        	for(byte i=0; i<signTextBoxes.size(); ++i){
        		//This check *shouldn't* be needed, but some users crash without it.
        		//Likely other mods not playing nice with GUIs....
        		if(signGUIInstance.text.size() > i){
	        		GuiTextField box = signTextBoxes.get(i);
	        		if(box.textboxKeyTyped(typedChar, keyCode)){
	        			signGUIInstance.text.set(i, box.getText());
	        		}
        		}
        	}
        }
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	private void drawCenteredString(String stringToDraw, int x, int y){
		fontRenderer.drawString(stringToDraw, x - fontRenderer.getStringWidth(stringToDraw)/2, y, 4210752);
	}
	
	private void updateSignNames(){
		//If we don't have a pack yet, set it now to the first pack if one exists.
		if(currentPack == null){
			if(MTSRegistry.packSignMap.size() > 0){
				currentPack = MTSRegistry.packSignMap.firstEntry().getKey();
			}else{
				//Bail, as we have no signs.
				return;
			}
		}
		
		//Set the prev and next packs.  Since the order of packs in the
		//map is the same, we can just get the entries to the left and right 
		//of this packID and be done.
		prevPack = null;
		nextPack = null;
		List<String> packIDs = new ArrayList<String>(MTSRegistry.packSignMap.keySet());
		int currentPackIndex = packIDs.indexOf(currentPack); 
		if(currentPackIndex > 0){
			prevPack = packIDs.get(currentPackIndex - 1);
		}
		if(currentPackIndex + 1 < packIDs.size() - 1){
			nextPack = packIDs.get(currentPackIndex + 1);
		}
		
		//Set the prev and next signs.  For these, we just get the next
		//sign in the list of signs for this pack in the map.
		prevSign = null;
		nextSign = null;
		List<JSONSign> signs = new ArrayList<JSONSign>(MTSRegistry.packSignMap.get(currentPack).values());
		int currentSignIndex = signs.indexOf(currentSign);
		//If the current sign is invalid (null) set it to the first sign.
		if(currentSignIndex == -1){
			currentSign = signs.get(0);
			currentSignIndex = 0;
		}
		if(currentSignIndex > 0){
			prevSign = signs.get(currentSignIndex - 1);
		}
		if(currentSignIndex + 1 < signs.size() - 1){
			nextSign = signs.get(currentSignIndex + 1);
		}
		
		//Clear out the text boxes so they don't bleed over onto a sign they shouldn't.
		for(GuiTextField textBox : signTextBoxes){
			textBox.setText("");
			textBox.setEnabled(false);
		}
		signGUIInstance.text.clear();
	}
}
