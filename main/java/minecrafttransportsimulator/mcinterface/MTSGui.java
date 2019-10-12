package minecrafttransportsimulator.mcinterface;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**Class that is used by MTS for all GUI operations.  This provides a standard set of
 * methods for creating GUIs, all without requiring classes that extend this class to use
 * any MC methods.  Also changes a few things in the rendering and interaction systems
 * to allow for multi-page GUIs.  MC doesn't handle those well as it makes some rather
 * broad assumptions with visibility of interfaces.
 * <br>
 * <br>
 * Several classes are also provided in this class for creating objects in the GUI.
 * These should ALWAYS be used in place of MC objects as they are assured to not change.
 * Whenever an x or y position is mentioned for an object, it is the object's top-left
 * position.  Of course, this means that 0,0 is the top-left of the screen.
 * 
 * @author don_bruce
 */
public abstract class MTSGui extends GuiScreen{
	protected static final ResourceLocation GUI_STANDARD_TEXTURE = new ResourceLocation(MTS.MODID, "textures/guis/standard.png");	
	protected final List<MTSButton> buttons = new ArrayList<MTSButton>();
	
	
	
	//---------------START OF FORWARDED METHODS---------------//
	@Override 
	public void initGui(){
		handleInit(this.width, this.height);
	}
	/**Called during init.  Width and height of the screen are known here.*/
	protected abstract void handleInit(int width, int height);
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks){
		handleDraw(mouseX, mouseY);
	}
	/**Called when the main draw function is.  This is where drawing of components should happen.*/
	protected abstract void handleDraw(int mouseX, int mouseY);
	
	
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException{
    	for(MTSButton button : buttons){
    		if(button.mouseOver(mouseX, mouseY)){
    			handleButtonClick(button);
    			return;
    		}
    	}
    	handleMouseClick(mouseX, mouseY);
    }
	/**Called when the mouse is clicked.  Only for a left-click.*/
    protected abstract void handleMouseClick(int mouseX, int mouseY);
	/**Called when a button is clicked.  This will be called instead of handleMouseClick().*/
    protected abstract void handleButtonClick(MTSButton buttonClicked);
	

	@Override
    protected void keyTyped(char key, int code) throws IOException{
    	handleKeyTyped(key, code);
    }
    /**Called when a key is pressed.  Does not get called if the ESC key is pressed as that closes the GUI.*/
	protected abstract void handleKeyTyped(char key, int code);
	
	@Override
	public boolean doesGuiPauseGame(){
		return pauses();
	}
	/**Return true if this GUI pauses the game..*/
	public abstract boolean pauses();
	
	
	
	//---------------START OF CUSTOM METHODS---------------//
	/**Closes this GUI.*/
	public void close(){
		mc.player.closeScreen();
	}
	
	/**Renders the shaded background common to screens like the pause menu.*/
	public void renderBackground(){
		drawDefaultBackground();
	}
	
	/**Renders the tooltip of an item.  Does NOT check to make sure the mouse is over the item.*/
	public void renderItemTooltip(ItemStack stack, int x, int y){
		renderToolTip(stack, x, y);
	}
	
	/**Renders a square from the currently bound texture.  Note that all measurements are in pixels.*/
	public void renderTexturedQuad(int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight){
		drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
	}
	
	
	//---------------START OF CUSTOM CLASSES---------------//
	
	/**A simple button.  Mimics the MC behavior of buttons by highlighting itself when
	 * the mouse is over it.  Does not use the standard MC button code, so don't expect
	 * it to behave the same.
	 * 
	 * @author don_bruce
	 */
	protected class MTSButton{
		public final int x;
		public final int y;
		public final int width;
		public final int height;
		public String text;
		/**If this button is not enabled it will be grayed-out and won't react to input.*/
		public boolean enabled;
		/**If this button is not visible it will not be drawn, even if draw() is called.*/
		public boolean visible;
		
		private static final int BUTTON_TEXTURE_U_OFFSET = 198;
		private static final int BUTTON_TEXTURE_HEIGHT = 20;
		private static final int BUTTON_TEXTURE_WIDTH = 200;
		
		public MTSButton(int x, int y, int width, int height, String text){
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.text = text;
		}
		
		/**Returns true if the passed-in mouse position is inside the bounds of this button.
		 * If this button is not visible or not enabled, then it will return false no matter what.*/
		public boolean mouseOver(int mouseX, int mouseY){
			return visible && enabled && mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
		}
		
		/**Draws the button with whatever texture is currently-loaded.*/
		public void draw(int mouseX, int mouseY){
			if(visible){
				if(enabled){
					if(mouseOver(mouseX, mouseY)){
						renderTexturedQuad(x, y, 0, BUTTON_TEXTURE_U_OFFSET + 2*BUTTON_TEXTURE_HEIGHT, width, height, BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT);
					}else{
						renderTexturedQuad(x, y, 0, BUTTON_TEXTURE_U_OFFSET + BUTTON_TEXTURE_HEIGHT, width, height, BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT);
					}
				}else{
					renderTexturedQuad(x, y, 0, BUTTON_TEXTURE_U_OFFSET, width, height, BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT);
				}
				MTSRenderer.drawText(text, width + width/2, height + height/2, Color.GRAY, true, true);
			}
		}
	}
}
