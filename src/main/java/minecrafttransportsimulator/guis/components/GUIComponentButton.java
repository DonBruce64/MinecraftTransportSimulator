package minecrafttransportsimulator.guis.components;

import java.awt.Color;

import minecrafttransportsimulator.guis.GUIBase;

/**Custom button class.  This allows for a custom button texture, as well as a cleaner constructor.
 * It also allows us to cut out a few MC methods from their own button class and use our own.
 * 
 *
 * @author don_bruce
 */
public abstract class GUIComponentButton{
	private static final int BUTTON_TEXTURE_U_OFFSET = 196;
	private static final int BUTTON_TEXTURE_WIDTH = 200;
	private static final int BUTTON_TEXTURE_HEIGHT = 20;
	
	public final int x;
	public final int y;
	public final int width;
	public final int height;
	public final boolean centeredText;
	
	public boolean visible = true;
	public boolean enabled = true;
	public String text;
	    	
	public GUIComponentButton(int x, int y, int width, String text){
		this(x, y, width, text, 20, true);
	}
	
	public GUIComponentButton(int x, int y, int width, String text, int height, boolean centeredText){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.text = text;
		this.centeredText = centeredText;
	}
	
	/**
	 *  Returns true if the mouse could have clicked this button, given the mouse
	 *  position and current button state.  Used to know if we need to call
	 *  {@link #onClicked()} to do clicking actions.
	 */
	public boolean clicked(int xPos, int yPos){
		return visible && enabled && x < xPos && xPos < x + width && y < yPos && yPos < y + height; 
	}
	
	/**
	 *  Called when this button is clicked by a mouse.  The button will
	 *  already have been verified to be both visible and enabled at
	 *  this point, so this click action is a true action.  Either extend
	 *  this class and override this method, or simply use generics in a 
	 *  constructor for simple tasks.  The purpose of this is to prevent
	 *  the need to check every button to see which one was clicked like
	 *  MC does.
	 */
	public abstract void onClicked();
	
	/**
	 *  Renders the button's texture using the default MC system for rendering 2D textures.
	 *  This scales the texture vertically to match the height of the button, unlike the traditional 
	 *  MC way of using a constant value which can overrun or underrun.  Because of this, button sizes 
	 *  other than 20px tall are supported, though values significantly outside this range may look odd.
	 *  When rendering, we use the currently-bound texture.  This is done to allow for dynamic button
	 *  textures in different GUIs without the need to sub-class this class.  Also note that the
	 *  text is NOT rendered here; that is done in its own method as rendering text would require
	 *  a texture switch in this method.
	 */
    public void renderButton(int mouseX, int mouseY){
    	if(visible){
			int textureUStart;
    		if(enabled){
				if(mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height){
					textureUStart = BUTTON_TEXTURE_U_OFFSET + 2*BUTTON_TEXTURE_HEIGHT;//Highlighted
				}else{
					textureUStart = BUTTON_TEXTURE_U_OFFSET + 1*BUTTON_TEXTURE_HEIGHT;//Normal
				}
			}else{
				textureUStart = BUTTON_TEXTURE_U_OFFSET;//Disabled
			}
    		GUIBase.renderSheetTexture(x, y, width/2, height, 0, textureUStart, width/2, textureUStart + BUTTON_TEXTURE_HEIGHT);
			GUIBase.renderSheetTexture(x + width/2, y, width/2, height, BUTTON_TEXTURE_WIDTH - width/2, textureUStart, BUTTON_TEXTURE_WIDTH, textureUStart + BUTTON_TEXTURE_HEIGHT);
		}
    }
    
    /**
	 *  Renders the button's text.  This is done separately from the button to allow all buttons to render in one pass
	 *  before binding the font texture for rendering text.  It also prevents oddities that occur from font
	 *  rendering with respect to OpenGL states.  {@link GUIBase} is passed-in here as a parameter to allow
	 *  the button to call the {@link GUIBase#drawText(int, int)} method of the current screen instance.
	 */
    public void renderText(GUIBase currentGUI){
    	if(visible){
    		currentGUI.drawText(text, centeredText ? x + width/2 : x, y + (height-8)/2, Color.DARK_GRAY, centeredText, false, -1);
    	}
    }
}
