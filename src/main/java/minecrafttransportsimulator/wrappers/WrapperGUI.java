package minecrafttransportsimulator.wrappers;

import java.awt.Color;
import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIBase.GUILightingMode;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox.TextBoxControlKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Wrapper for MC GUI classes.  Constructor takes a type of {@link AGUIBase}, but
 * is only visible when calling {@link #openGUI(AGUIBase)}.  This will automatically
 * construct the wrapper and open the GUI, all without exposing MC-specific code.
 * On that note, this is where all MC-specific code should be located.  Preferably
 * in static methods that can be accessed by anything that needs GUI
 * functionality, even if it doesn't extend the {@link AGUIBase} class.
 *
 * @author don_bruce
 */
public class WrapperGUI extends GuiScreen{
	private static FontRenderer fontRenderer;
	private static RenderItem itemRenderer;
	
	private int guiLeft;
	private int guiTop;
	private GUIComponentSelector lastSelectorClicked;
	
	private final AGUIBase gui;
	
	private WrapperGUI(AGUIBase gui){
		this.gui = gui;
	}
	
	/**
	 *  This is called by the main MC system when this GUI is first initialized, or when it
	 *  is re-sized.  We use this call, and send out the re-factored width and height
	 *  point of the top-left of the GUI texture via {@link #setupComponents(int, int)} for
	 *  GUIs to create their objects with.
	 */
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (width - gui.getWidth())/2;
		if(gui.renderFlushBottom()){
			guiTop = height - gui.getHeight();
		}else{
			guiTop = (height - gui.getHeight())/2;
		}
		
		
		//Clear out the component lists before populating them again.
		//If we don't, we get duplicates when re-sizing.
		gui.clearComponents();
		gui.setupComponents(guiLeft, guiTop);
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
		gui.setStates();
		
		//Set color to default in case some other rendering was being done prior and didn't clean up.
		GL11.glColor3f(1, 1, 1);
		
		//Draw gradient if required.
		if(gui.renderDarkBackground()){
			drawDefaultBackground();
		}
		
		//If we are light-sensitive, enable the lightmap.
		if(!gui.getGUILightMode().equals(GUILightingMode.NONE)){
			mc.entityRenderer.enableLightmap();
		}
		
		//Bind the standard texture and render the background.
		mc.getTextureManager().bindTexture(new ResourceLocation(gui.getTexture()));
		renderSheetTexture(guiLeft, guiTop, gui.getWidth(), gui.getHeight(), 0, 0, gui.getWidth(), gui.getHeight(), gui.getTextureWidth(), gui.getTextureHeight());
		
		//Render buttons and selectors.  These choose if they render or not depending on visibility.
		for(GUIComponentButton button : gui.buttons){
			button.renderButton(mouseX, mouseY);
		}
		for(GUIComponentSelector selector : gui.selectors){
			selector.renderSelector(mouseX, mouseY);
		}
		
		//If we are light-sensitive, and this GUI is said to be lit up, disable the lightmap.
		//This allows all text to be lit up if required.  We also render the lit texture now.
		//This requires a re-render of all the components to ensure the lit texture portions of said components render.
		if(gui.getGUILightMode().equals(GUILightingMode.LIT)){
			mc.entityRenderer.disableLightmap();
			mc.getTextureManager().bindTexture(new ResourceLocation(gui.getTexture().replace(".png", "_lit.png")));
			renderSheetTexture(guiLeft, guiTop, gui.getWidth(), gui.getHeight(), 0, 0, gui.getWidth(), gui.getHeight(), gui.getTextureWidth(), gui.getTextureHeight());
			for(GUIComponentButton button : gui.buttons){
				button.renderButton(mouseX, mouseY);
			}
			for(GUIComponentSelector selector : gui.selectors){
				selector.renderSelector(mouseX, mouseY);
			}
		}
		
		//Now that all main rendering is done, render text.
		//This includes labels, button text, and text boxes.
		for(GUIComponentLabel label : gui.labels){
			label.renderText();
		}
		for(GUIComponentButton button : gui.buttons){
			button.renderText();
		}
		for(GUIComponentSelector selector : gui.selectors){
			selector.renderText(gui.getGUILightMode().equals(GUILightingMode.LIT));
		}
		for(GUIComponentTextBox textBox : gui.textBoxes){
        	textBox.renderBox();
        }
		
		//Now render items.  These need to go after the components due to their tooltips.
		RenderHelper.enableGUIStandardItemLighting();
		for(GUIComponentItem item : gui.items){
			item.renderItem();
		}
		for(GUIComponentItem item : gui.items){
			item.renderTooltip(this, mouseX, mouseY);
		}
		
		//Finally, render any instruments.
		//We need to do an OpenGL inversion here to ensure the instruments don't render upside-down.
		GL11.glPushMatrix();
		GL11.glScalef(-1, -1, 0);
		for(GUIComponentInstrument instrument : gui.instruments){
			instrument.renderInstrument();
		}
		GL11.glPopMatrix();
		
		//Make sure the lightmap is disabled after this rendering is done.
		mc.entityRenderer.disableLightmap();
	}
	
	/**
	 *  This is called by the main MC system for click events.  We override it here to check
	 *  to see if we have clicked any of the registered components.  If so, we fire the appropriate 
	 *  event for those components.  If we click something, we don't check any other components as 
	 *  that could result in us being in a transition state when doing checks.
	 */
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException{
        for(GUIComponentButton button : gui.buttons){
        	if(button.canClick(mouseX, mouseY)){
    			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    			button.onClicked();
    			return;
        	}
        }
        for(GUIComponentSelector selector : gui.selectors){
        	if(selector.canClick(mouseX, mouseY)){
    			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    			selector.onClicked(mouseX <= selector.x + selector.width/2);
    			lastSelectorClicked = selector;
    			return;
        	}
        }
        for(GUIComponentTextBox textBox : gui.textBoxes){
        	textBox.updateFocus(mouseX, mouseY);
        }
    }
	
	/**
	 *  This is called by the main MC system for click events.  We override it here to tell 
	 *  the last selector we clicked, if any, that the mouse has been released.  This allows
	 *  the selector to resume to it's "resting" state.  This is dependent on the selector code;
	 *  some selectors may not do anything with this action.
	 */
	@Override
	protected void mouseReleased(int mouseX, int mouseY, int actionType){
	    if(lastSelectorClicked != null){
	    	lastSelectorClicked.onReleased();
	    	lastSelectorClicked = null;
	    }
	}
	
	/**
	 *  This is called by the main MC system for keyboard events.  We Override it here to check
	 *  to forward the inputs to focused textBoxes for further processing.
	 */
	@Override
	protected void keyTyped(char key, int keyCode) throws IOException{
		super.keyTyped(key, keyCode);
		for(GUIComponentTextBox textBox : gui.textBoxes){
			if(textBox.focused){
				//If we did a paste from the clipboard, we need to replace everything in the box.
				//Otherwise, just send the char for further processing.
				if(isKeyComboCtrlV(keyCode)){
					textBox.setText(getClipboardString());
				}else{
					switch(keyCode){
						case Keyboard.KEY_BACK: textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.BACKSPACE); continue;
						case Keyboard.KEY_DELETE: textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.DELETE); continue;
						case Keyboard.KEY_LEFT: textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.LEFT); continue;
						case Keyboard.KEY_RIGHT: textBox.handleKeyTyped(key, keyCode, TextBoxControlKey.RIGHT); continue;
						default: textBox.handleKeyTyped(key, keyCode, null); continue;
					}
				}
			}
        }
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return gui.pauseOnOpen();
	}
	
	
	//--------------------START OF INSTANCE HELPER METHODS--------------------	
	/**
	 *  Draws the specified tooltip on the GUI.  This should be
	 *  the last thing that gets rendered, as otherwise it may render
	 *  behind other components.
	 */
	public void drawItemTooltip(String itemName, int qty, int metadata, int mouseX, int mouseY){
		ItemStack stack = new ItemStack(Item.getByNameOrId(itemName), qty, metadata);
		this.renderToolTip(stack, mouseX, mouseY);
	}

	
	//--------------------START OF STATIC HELPER METHODS--------------------
	/**
	 *  Draws the specified text using the MC fontRenderer.  This method can render the text in multiple ways depending
	 *  on the parameters passed-in.  If a centered string is specified, then the point passed-in should be the center
	 *  point of the string, not the top-left as normal.  If wrapWidth is anything else but 0, then the wordWrap
	 *  method will be called to render multi-line text.  Note that after this operation the font texture will be bound, 
	 *  so take care when calling this method in the middle of rendering operations.
	 */
	public static void drawText(String text, int x, int y, Color color, boolean centered, boolean shadow, int wrapWidth){
		if(fontRenderer == null){
			fontRenderer = Minecraft.getMinecraft().fontRenderer;
		}
		if(centered){
			x -= fontRenderer.getStringWidth(text)/2;
		}
		if(shadow){
			fontRenderer.drawStringWithShadow(text, x, y, color.getRGB());
		}else{
			if(wrapWidth == 0){
				fontRenderer.drawString(text, x, y, color.getRGB());
			}else{
				fontRenderer.drawSplitString(text, x, y, wrapWidth, color.getRGB());
			}
		}
	}
	
	/**
	 *  Similar to {@link #drawText(String, int, int, Color, boolean, boolean, int)}, except this method
	 *  does OpenGL scaling to render the text bigger or smaller than normal.  Requires a few different bits
	 *  to get this to work, so it's in it's own method for code simplicity.
	 */
	public static void drawScaledText(String text, int x, int y, Color color, boolean centered, boolean shadow, int wrapWidth, float scale){
		if(fontRenderer == null){
			fontRenderer = Minecraft.getMinecraft().fontRenderer;
		}
		GL11.glPushMatrix();
		if(centered){
			GL11.glTranslatef(x - scale*fontRenderer.getStringWidth(text)/2, y, 0);
		}else{
			GL11.glTranslatef(x, y, 0);
		}
		GL11.glScalef(scale, scale, scale);
		drawText(text, 0, 0, color, false, shadow, wrapWidth);
		GL11.glPopMatrix();
	}
	
	/**
	 *  Draws the specified item on the GUI at the specified scale.  Note that MC
	 *  renders all items from their top-left corner, so take this into account when
	 *  choosing where to put this component in your GUI.
	 */
	public static void drawItem(String itemName, int qty, int metadata, int x, int y, float scale){
		if(itemRenderer == null){
			itemRenderer = Minecraft.getMinecraft().getRenderItem();
		}
		ItemStack stack = new ItemStack(Item.getByNameOrId(itemName), qty, metadata);
		if(scale != 1.0F){
			GL11.glPushMatrix();
			GL11.glTranslatef(x, y, 0);
			GL11.glScalef(scale, scale, scale);
			itemRenderer.renderItemAndEffectIntoGUI(stack, 0, 0);
			if(qty > 1){
				itemRenderer.renderItemOverlays(fontRenderer, stack, 0, 0);
			}
			GL11.glPopMatrix();
		}else{
			itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
			if(qty > 1){
				itemRenderer.renderItemOverlays(fontRenderer, stack, x, y);
			}
		}
	}
	
	/**
	 *  Draws the specified portion of the currently-bound texture.  Texture size needs to be
	 *  passed-in here to allow this method to translate pixels into relative texture coords.  
	 *  Draw starts at the  bottom-left point and goes counter-clockwise to the top-left point.
	 */
	public static void renderSheetTexture(int x, int y, int width, int height, int u, int v, int U, int V, int textureWidth, int textureHeight){
	 	float widthPixelPercent = 1.0F/textureWidth;
        float heightPixelPercent = 1.0F/textureHeight;
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
	 *  Returns the translation of the passed-in text from the lang file.
	 *  Put here to prevent the need for referencing the MC class directly, which
	 *  may change during updates.
	 */
	public static String translate(String text){
		return I18n.format(text);
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
	 *  Returns true if the passed-in GUI is currently active.
	 *  If null is passed-in, then this method returns true if no GUI is active.
	 */
	public static boolean isGUIActive(Class<? extends GuiScreen> guiClass){
		if(guiClass == null){
			return Minecraft.getMinecraft().currentScreen == null;
		}else{
			return Minecraft.getMinecraft().currentScreen == null ? false : Minecraft.getMinecraft().currentScreen.getClass().equals(guiClass);
		}
	}
	
	/**
	 *  Closes the currently-opened GUI, returning back to the main game.
	 */
	public static void closeGUI(){
		Minecraft.getMinecraft().displayGuiScreen(null);
	}
	
	/**
	 *  Opens the passed-in GUI, wrapping it in an instance of this class in the process.
	 *  This makes it compatible with all MC-specific systems.
	 */
	public static void openGUI(AGUIBase gui){
		FMLCommonHandler.instance().showGuiScreen(new WrapperGUI(gui));
	}
}
