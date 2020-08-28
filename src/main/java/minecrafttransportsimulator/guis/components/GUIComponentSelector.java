package minecrafttransportsimulator.guis.components;

import java.awt.Color;

import mcinterface.BuilderGUI;
import mcinterface.BuilderGUI.TextPosition;

/**Custom class for selectors.  Selectors are like buttons, except they allow for side-sensitive clicking.
 * Their {@link #onClicked()} method takes a boolean parameter that allows them to perform different actions 
 * depending on their state.  This allows for multi-state selectors that can be clicked, rather that two-
 * state buttons.  It is assumed the selector will have a custom texture area as it will need
 * to be offset for every click, so the long-form constructor is the only one available here.  Note that
 * the text passed-in will be rendered below the selector rather than at the center like buttons.
 * Additional labels may be desired should more descriptions be needed.
 *
 * @author don_bruce
 */
public abstract class GUIComponentSelector{
	
	public final int x;
	public final int y;
	public final int width;
	public final int height;
	public final String text;
	public final Color regularColor;
	public final Color litColor;
	public final int selectorSectionWidth;
	public final int selectorSectionHeight;
	public final int selectorSectionWidthOffset;
	public final int selectorSectionHeightOffset;
	public final int textureWidth;
	public final int textureHeight;
	
	public boolean visible = true;
	public int selectorState = 0;
	
	public GUIComponentSelector(int x, int y, int width, int height, String text, String regularColor, String litColor, int selectorSectionWidth, int selectorSectionHeight, int selectorSectionWidthOffset, int selectorSectionHeightOffset, int textureWidth, int textureHeight){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.text = text;
		this.regularColor = regularColor != null ? Color.decode(regularColor) : Color.WHITE;
		this.litColor = regularColor != null ? Color.decode(litColor) : Color.WHITE;
		this.selectorSectionWidth = selectorSectionWidth;
		this.selectorSectionHeight = selectorSectionHeight;
		this.selectorSectionWidthOffset = selectorSectionWidthOffset;
		this.selectorSectionHeightOffset = selectorSectionHeightOffset;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}
	
	/**
	 *  Returns true if the mouse can click this selector, given the mouse
	 *  position and current button state.  Used to know if we need to call
	 *  {@link #onClicked(boolean)} to do clicking actions.
	 */
	public boolean canClick(int xPos, int yPos){
		return visible && x < xPos && xPos < x + width && y < yPos && yPos < y + height; 
	}
	
	/**
	 *  Called when this selector is clicked by a mouse.  The selector will
	 *  already have been verified to be visible at this point, so this click
	 *  action is a true action.  Either extend this class and override this 
	 *  method, or simply use generics in a constructor for simple tasks.
	 */
	public abstract void onClicked(boolean leftSide);
	
	/**
	 *  Called when the mouse that clicked this selector is released.  At this point
	 *  the mouse may no longer be over the selector, but it can be assured that
	 *  the last thing the user clicked was this selector.  Use this method for
	 *  things that need to have a maintained state while clicked rather than
	 *  have a single momentary action.
	 */
	public abstract void onReleased();
    
	/**
	 *  Renders the selector's texture using the default MC system for rendering 2D textures.
	 *  This call is dependent on the value of {@link #selectorState}, which essential
	 *  defines the offset vertically for this texture.  This offset may be negative if desired.
	 *  note that the text is NOT rendered here; that is done in its own method as rendering text would 
	 *  require a texture switch in this method.
	 */
    public void renderSelector(int mouseX, int mouseY){
    	if(visible){
			int textureUStart = selectorSectionHeightOffset + selectorState*selectorSectionHeight;
    		BuilderGUI.renderSheetTexture(x, y, width, height, selectorSectionWidthOffset, textureUStart, selectorSectionWidthOffset + selectorSectionWidth, textureUStart + selectorSectionHeight, textureWidth, textureHeight);
		}
    }
	
    /**
	 *  Renders the selector's text.  This is done separately from the selector to allow all selectors to render in one pass
	 *  before binding the font texture for rendering text.  It also prevents oddities that occur from font
	 *  rendering with respect to OpenGL states.  The lightsOn parameter allows for switching of the text color
	 *  based on the lighting state of this GUI.  Useful if you have black text normally but want it to be white when lit..
	 */
    public void renderText(boolean lightsOn){
    	if(visible){
    		BuilderGUI.drawScaledText(text, x + width/2, y + height + 1, lightsOn ? litColor : regularColor, TextPosition.CENTERED, 0, 0.75F, false);
    	}
    }
}
