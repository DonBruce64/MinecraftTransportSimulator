package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox.TextBoxControlKey;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;

/**Base GUI class, and interface to MC's GUI systems.  All MTS GUIs should extend this class
 * rather than user their own.  This prevents the need to use MC classes and makes updating easier.
 *
 * @author don_bruce
 */
public abstract class GUIBase extends GuiScreen{
	protected static final ResourceLocation standardTexture = new ResourceLocation(MTS.MODID, "textures/guis/standard.png");
	private static final int STANDARD_TEXTURE_WIDTH = 256;
	private static final int STANDARD_TEXTURE_HEIGHT = 192;
	
	private final List<GUIComponentLabel> labels = new ArrayList<GUIComponentLabel>();
	private final List<GUIComponentButton> buttons = new ArrayList<GUIComponentButton>();
	private final List<GUIComponentTextBox> textBoxes = new ArrayList<GUIComponentTextBox>();
	
	private int guiLeft;
	private int guiTop;
	
	
	/**
	 *  This is called by the main MC system when this GUI is first initialized, or when it
	 *  is re-sized.  We use this call, and send out the re-factored width and height
	 *  point of the top-left of the GUI texture via {@link #setupComponents(int, int)} for
	 *  GUIs to create their objects with.
	 */
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (width - STANDARD_TEXTURE_WIDTH)/2;
		guiTop = (height - STANDARD_TEXTURE_HEIGHT)/2;
		
		//Clear out the component lists before populating them again.
		//If we don't, we get duplicates when re-sizing.
		labels.clear();
		buttons.clear();
		setupComponents(guiLeft, guiTop);
	}
	
	/**
	 *  This is called by the main MC system when this GUI is set to render.  We do all
	 *  the rendering here using the component states to ensure a better order that
	 *  doesn't risk clashing textures or other issues like normal rendering does.
	 *  Because of this, this method should never need to be overridden by classes
	 *  extending this class.  Instead, they should populate the appropriate component
	 *  lists and set the appropriate states.
	 */
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		//First set the states for things in thie GUI.
		setStates();
		
		//Set color to default in case some other rendering was being done prior and didn't clean up.
		GL11.glColor3f(1, 1, 1);
		
		//Draw gradient if required.
		if(renderDarkBackground()){
			drawDefaultBackground();
		}
		
		//Bind the standard texture and render the background.
		mc.getTextureManager().bindTexture(standardTexture);
		renderSheetTexture(guiLeft, guiTop, STANDARD_TEXTURE_WIDTH, STANDARD_TEXTURE_HEIGHT, 0, 0, STANDARD_TEXTURE_WIDTH, STANDARD_TEXTURE_HEIGHT);
		
		//Render buttons.  Buttons choose if they render or not depending on visibility.
		for(GUIComponentButton button : buttons){
			button.renderButton(mouseX, mouseY);
		}
		
		//Now that all main rendering is done, render text.
		//This includes labels, button text, and text boxes.
		for(GUIComponentLabel label : labels){
			label.renderText(this);
		}
		for(GUIComponentButton button : buttons){
			button.renderText(this);
		}
		for(GUIComponentTextBox textBox : textBoxes){
        	textBox.renderBox(this);
        }
	}
	
	/**
	 *  This is called by the main MC system for click events.  We Override it here to check
	 *  to see if we have clicked any of the registered buttons or text boxes.  If so,
	 *  we fire the appropriate event for those components.  In the case of {@link GUIComponentButton}
	 *  we fire {@link GUIComponentButton#onClicked()}.  If we click a button, we don't check any other
	 *  buttons or text boxes as that could result in us being in a transition state when doing checks.
	 */
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException{
        for(GUIComponentButton button : buttons){
        	if(button.clicked(mouseX, mouseY)){
    			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    			button.onClicked();
    			return;
        	}
        }
        for(GUIComponentTextBox textBox : textBoxes){
        	textBox.updateFocus(mouseX, mouseY);
        }
    }
	
	/**
	 *  This is called by the main MC system for keyboard events.  We Override it here to check
	 *  to forward the inputs to focused textBoxes for further processing.
	 */
	@Override
	protected void keyTyped(char key, int keyCode) throws IOException{
		super.keyTyped(key, keyCode);
		for(GUIComponentTextBox textBox : textBoxes){
			if(textBox.focused){
				//If we did a paste from the clipboard, we need to replace everything in the box.
				//Otherwise, just send the char for further processing.
				if(isKeyComboCtrlV(keyCode)){
					textBox.setText(getClipboardString());
				}else{
					switch(keyCode){
						case Keyboard.KEY_BACK: textBox.handleKeyTyped(key, TextBoxControlKey.BACKSPACE); continue;
						case Keyboard.KEY_DELETE: textBox.handleKeyTyped(key, TextBoxControlKey.DELETE); continue;
						case Keyboard.KEY_LEFT: textBox.handleKeyTyped(key, TextBoxControlKey.LEFT); continue;
						case Keyboard.KEY_RIGHT: textBox.handleKeyTyped(key, TextBoxControlKey.RIGHT); continue;
						default: textBox.handleKeyTyped(key, null); continue;
					}
				}
			}
        }
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return pauseOnOpen();
	}
	
	
	//--------------------START OF NEW CUSTOM METHODS FOR MAKING GUIS--------------------
	/**
	 *  Called during init to allow for the creation of GUI components.  All components
	 *  should be created in this method, and should be added via the appropriate calls.
	 *  The passed-in guiLeft and guiTop parameters are the top-left of the TEXTURE of
	 *  this GUI, not the screen.  This allows for all objects to be created using offsets
	 *  that won't change, rather than screen pixels that will. 
	 */
	public abstract void setupComponents(int guiLeft, int guiTop);
	
	/**
	 *  Called right before rendering to allow GUIs to set the states of their objects. 
	 */
	public abstract void setStates();
	
	/**
	 *  If this is true, then the dark background gradient will be rendered behind the GUI.
	 */
	public boolean renderDarkBackground(){
		return false;
	}
	
	/**
	 *  If this is true, then the GUI will pause the game when open.
	 */
	public boolean pauseOnOpen(){
		return false;
	}
	
	/**
	 *  Adds an {@link GUIComponentLabel} to this GUIs component set.  These are rendered
	 *  automatically given their current state.  Said state should be set in {@link #setStates()}.
	 */
	public void addLabel(GUIComponentLabel label){
		labels.add(label);
	}
	
	/**
	 *  Adds an {@link GUIComponentButton} to this GUIs component set.  When a mouse click is
	 *  sensed, this GUI will attempt to click all buttons in this set via {@link GUIComponentButton#clicked()}.
	 *  If any of those buttons say they were clicked, their {@link GUIComponentButton#onClicked()} method 
	 *  is fired to allow the button to handle clicking actions.
	 */
	public void addButton(GUIComponentButton button){
		buttons.add(button);
	}
	
	/**
	 *  Adds an {@link GUIComponentTextBox} to this GUIs component set.  When a mouse click is
	 *  sensed, this GUI will attempt to focus all text boxes.  When a key is typed, any focused
	 *  text boxes will get that input set to them via {@link GUIComponentTextBox#handleKeyTyped(char, TextBoxControlKey)}.
	 */
	public void addTextBox(GUIComponentTextBox textBox){
		textBoxes.add(textBox);
	}
	
	
	//--------------------START OF NORMAL HELPER METHODS--------------------	
	/**
	 *  Draws the specified text using the MC fontRenderer.  This method can render the text in multiple ways depending
	 *  on the parameters passed-in.  If a centered string is specified, then the point passed-in should be the center
	 *  point of the string, not the top-left as normal.  If wrapWidth is anything else but 0, then the wordWrap
	 *  method will be called to render multi-line text.  Note that after this operation the font texture will be bound, 
	 *  so take care when calling this method in the middle of rendering operations.
	 */
	public void drawText(String text, int x, int y, Color color, boolean centered, boolean shadow, int wrapWidth){
		if(centered){
			x -= mc.fontRenderer.getStringWidth(text)/2;
		}
		if(shadow){
			mc.fontRenderer.drawStringWithShadow(text, x, y, color.getRGB());
		}else{
			if(wrapWidth == -1){
				mc.fontRenderer.drawString(text, x, y, color.getRGB());
			}else{
				mc.fontRenderer.drawSplitString(text, x, y, wrapWidth, color.getRGB());
			}
		}
	}
	
	/**
	 *  Similar to {@link #drawText(String, int, int, Color, boolean, boolean, int)}, except this method
	 *  does OpenGL scaling to render the text bigger or smaller than normal.  Requires a few different bits
	 *  to get this to work, so it's in it's own method for code simplicity.
	 */
	public void drawScaledText(String text, int x, int y, Color color, boolean centered, boolean shadow, int wrapWidth, float scale){
		GL11.glPushMatrix();
		if(centered){
			GL11.glTranslatef(x - mc.fontRenderer.getStringWidth(text)/2, y, 0);
		}else{
			GL11.glTranslatef(x, y, 0);
		}
		GL11.glScalef(scale, scale, scale);
		drawText(text, 0, 0, color, false, shadow, wrapWidth);
		GL11.glPopMatrix();
	}
	
	
	//--------------------START OF STATIC HELPER METHODS--------------------
	
	/**
	 *  Draws the specified portion of the currently-bound texture.  Normally, this will be the standardTexture,
	 *  but other textures are possible if they are bound prior to calling this method.  A texture size
	 *  of 256x256 is assumed here, so don't use anything but that!  Draw starts at the bottom-left
	 *  point and goes counter-clockwise to the top-left point.
	 */
	public static void renderSheetTexture(int x, int y, int width, int height, int u, int v, int U, int V){
	 	float widthPixelPercent = 1.0F/256F;
        float heightPixelPercent = 1.0F/256F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, 			y + height, 0.0D).tex(u * widthPixelPercent, 	V * heightPixelPercent).endVertex();
        bufferbuilder.pos(x + width, 	y + height, 0.0D).tex(U * widthPixelPercent, 	V * heightPixelPercent).endVertex();
        bufferbuilder.pos(x + width, 	y, 			0.0D).tex(U * widthPixelPercent, 	v * heightPixelPercent).endVertex();
        bufferbuilder.pos(x, 			y, 			0.0D).tex(u * widthPixelPercent, 	v * heightPixelPercent).endVertex();
        tessellator.draw();
	}
	
	/**
	 *  Draws a colored rectangle at the specified point.  This does NOT change the currently-bound
	 *  texture, nor does it modify any OpelGL states, so it may safely be called during rendering operations.
	 */
	public static void renderRectangle(int x, int y, int width, int height, Color color){
		drawRect(x, y, x + width, y + height, color.getRGB());
	}
	
	/**
	 *  Clock method used to make flashing text and icons on screen.  Put here
	 *  for all GUIs to use.  Returns true if the period is active.  Both
	 *  parameters are in ticks, or 1/20 a second.
	 */
	public static boolean inClockPeriod(int totalPeriod, int onPeriod){
		return System.currentTimeMillis()*0.02D%totalPeriod <= onPeriod;
	}
	
	/**
	 *  Returns the translation of the passed-in text from the lang file.
	 *  Put here to prevent the need for referencing the MC class directly, which
	 *  may change during updates.  Prefixed by "gui." for convenience.
	 */
	public static String translate(String text){
		return I18n.format("gui." + text);
	}
}
