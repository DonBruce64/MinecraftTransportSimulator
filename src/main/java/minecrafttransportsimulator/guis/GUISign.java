package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.pole.BlockPoleSign;
import minecrafttransportsimulator.blocks.pole.TileEntityPoleSign;
import minecrafttransportsimulator.dataclasses.PackSignObject;
import minecrafttransportsimulator.packets.tileentities.PacketSignChange;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GUISign extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/crafting.png");	
	private final EntityPlayer player;
	private final TileEntityPoleSign sign;
	private final TileEntityPoleSign signTemp;
	private final List<GuiTextField> signTextBoxes = new ArrayList<GuiTextField>();
	
	private GuiButton leftPackButton;
	private GuiButton rightPackButton;
	private GuiButton leftSignButton;
	private GuiButton rightSignButton;
	private GuiButton startButton;
	private GuiButton textButton;
	
	private int guiLeft;
	private int guiTop;
	
	private String packName = "";
	private String prevPackName = "";
	private String nextPackName = "";
	
	private String signName = "";
	private String prevSignName = "";
	private String nextSignName = "";
	
	private PackSignObject pack;
		
	public GUISign(BlockPoleSign block, EntityPlayer player){
		this.sign = (TileEntityPoleSign) player.world.getTileEntity(block.lastClickedPos);
		this.signTemp = new TileEntityPoleSign();
		this.player = player;
		if(!sign.definition.isEmpty()){
			packName = sign.definition.substring(0, sign.definition.indexOf(':'));
			signName = sign.definition;
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
		buttonList.add(textButton = new GuiButton(0, guiLeft + 8, guiTop + 55, 126, 108, ""));
		
		for(byte i=0; i<10; ++i){
			signTextBoxes.add(new GuiTextField(0, fontRenderer, guiLeft + 9, guiTop + 54 + i*10, 125, 10));
			signTextBoxes.get(i).setEnabled(false);
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
		drawCenteredString(!packName.isEmpty() ? I18n.format("itemGroup." + packName) : "", guiLeft + 130, guiTop + 10);
		drawCenteredString(!signName.isEmpty() ? I18n.format("sign." + packName + "." + signName.substring(signName.indexOf(':') + 1) + ".name") : "", guiLeft + 130, guiTop + 30);
		
		//Set button states and render.
		startButton.enabled = !signName.isEmpty();
		leftPackButton.enabled = !prevPackName.isEmpty();
		rightPackButton.enabled = !nextPackName.isEmpty();
		leftSignButton.enabled = !prevSignName.isEmpty();
		rightSignButton.enabled = !nextSignName.isEmpty();
		textButton.enabled = !signName.isEmpty() && pack.general.textLines != null;
		for(GuiButton button : buttonList){
			if(!button.equals(textButton)){
				button.drawButton(mc, mouseX, mouseY, 0);
			}
		}
		this.drawRect(guiLeft + 190, guiTop + 188, guiLeft + 206, guiTop + 172, startButton.enabled ? Color.GREEN.getRGB() : Color.RED.getRGB());
		
		//Now make the selected sign render in the GUI using the TE code.
		if(!signName.isEmpty()){
			//Set the definition and text of the sign.
			signTemp.definition = signName;
			if(pack.general.textLines != null){
				for(byte i=0; i<PackParserSystem.getSign(signTemp.definition).general.textLines.length; ++i){
					if(sign.definition.equals(signTemp.definition) && sign.text.size() > i){
						signTemp.text.add(sign.text.get(i));
					}else{
						signTemp.text.add("");
					}
				}
			}
			
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glTranslatef(guiLeft + 196, guiTop + 107, 100);
			GL11.glRotatef(180, 0, 1, 0);
			float scale = -90F;
			GL11.glScalef(scale, scale, scale);
			TileEntityRendererDispatcher.instance.render(signTemp, -0.5F, -0.5F, -0.5F, renderPartialTicks, 0);
			GL11.glPopMatrix();
			
			//If we have text on the sign, render it in the text boxes.
			if(pack.general.textLines != null){
				for(byte i=0; i<pack.general.textLines.length; ++i){
					GuiTextField textBox = signTextBoxes.get(i);
					textBox.setText(signTemp.text.get(i));
					textBox.setMaxStringLength(pack.general.textLines[i].characters);
					textBox.drawTextBox();
					textBox.setEnabled(true);
				}
			}
		}
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(startButton)){
			MTS.MTSNet.sendToServer(new PacketSignChange(sign, signName, signTemp.text, player.getEntityId()));
			mc.player.closeScreen();
			return;
		}else{
			if(buttonClicked.equals(leftPackButton)){
				packName = prevPackName;
				signName = "";
			}else if(buttonClicked.equals(rightPackButton)){
				packName = nextPackName;
				signName = "";
			}else if(buttonClicked.equals(leftSignButton)){
				signName = prevSignName;
			}else if(buttonClicked.equals(rightSignButton)){
				signName = nextSignName;
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
        		if(signTemp.text.size() > i){
	        		GuiTextField box = signTextBoxes.get(i);
	        		if(box.textboxKeyTyped(typedChar, keyCode)){
	        			signTemp.text.set(i, box.getText());
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
		prevPackName = "";
		nextPackName = "";	
		prevSignName = "";
		nextSignName = "";
		
		boolean passedPack = false;
		boolean passedSign = false;
		for(String signFullName : PackParserSystem.getAllSigns()){
			if(packName.isEmpty()){
				packName = signFullName.substring(0, signFullName.indexOf(':'));
			}else if(!passedPack && !signFullName.startsWith(packName)){
				prevPackName = signFullName.substring(0, signFullName.indexOf(':'));
			}
			if(signFullName.startsWith(packName)){
				passedPack = true;
				if(signName.isEmpty()){
					signName = signFullName;
					passedSign = true;
				}else if(signName.equals(signFullName)){
					passedSign = true;
				}else if(!passedSign){
					prevSignName = signFullName;
				}else if(nextSignName.isEmpty()){
					nextSignName = signFullName;
				}
			}else if(nextPackName.isEmpty() && passedPack){
				nextPackName = signFullName.substring(0, signFullName.indexOf(':'));
			}
		}
		if(signName != null){
			pack = PackParserSystem.getSign(signName);
			for(GuiTextField textBox : signTextBoxes){
				textBox.setText("");
				textBox.setEnabled(false);
			}
			signTemp.text.clear();
		}
	}
}
