package minecrafttransportsimulator.mcinterface;

import java.awt.Color;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;

/**Interface for MC GUI classes.  Allows access to various GUI-specific functions.
 *
 * @author don_bruce
 */
public interface IInterfaceGUI{
	
	/**
	 *  Draws the item's tooltip on the GUI.  This should be
	 *  the last thing that gets rendered, as otherwise it may render
	 *  behind other components.
	 */
	public void drawItemTooltip(AGUIBase gui, int mouseX, int mouseY, IWrapperItemStack stack);
	
	/**
	 *  Draws a tooltip into the GUI.  This is for things that are NOT items, so
	 *  rather than passing-in item parameters you need to pass in the lines to render.
	 *  This should be rendered at the end of the render call to prevent the odd texture
	 *  binding of this method from conflicting from other renders.
	 */
	public void drawGenericTooltip(AGUIBase gui, int mouseX, int mouseY, String tooltip);
	
	/**
	 *  Draws the specified text using the MC fontRenderer.  This method can render the text in multiple ways depending
	 *  on the parameters passed-in.  If a centered string is specified, then the point passed-in should be  the center 
	 *  point of the string, rather that the top-left of the string like normal.  If wrapWidth is anything else but 0, 
	 *  then the wordWrap method will be called to render multi-line text.  Note that after this operation the font texture 
	 *  will be bound, so take care when calling this method in the middle of rendering operations.
	 */
	public void drawBasicText(String text, int x, int y, Color color, TextPosition renderPosition, int wrapWidth);
	
	/**
	 *  Similar to {@link #drawBasicText(String, int, int, Color, TextPosition, int)}, except this method
	 *  does OpenGL scaling to render the text bigger or smaller than normal.  Requires a few different bits
	 *  to get this to work, so it's in it's own method for code simplicity.
	 */
	public void drawScaledText(String text, int x, int y, Color color, TextPosition renderPosition, int wrapWidth, float scale, boolean autoScaled);
	
	/**
	 *  Returns the width of the passed-in text string.
	 */
	public int getStringWidth(String text);
	
	/**
	 *  Returns the formatting code for the passed-in color.
	 */
	public String getFormattingCode(String color);
	
	/**
	 *  Draws the specified item on the GUI at the specified scale.  Note that MC
	 *  renders all items from their top-left corner, so take this into account when
	 *  choosing where to put this component in your GUI.
	 */
	public void drawItem(IWrapperItemStack stack, int x, int y, float scale);
	
	/**
	 *  Draws the specified portion of the currently-bound texture.  Texture size needs to be
	 *  passed-in here to allow this method to translate pixels into relative texture coords.  
	 *  Draw starts at the  bottom-left point and goes counter-clockwise to the top-left point.
	 */
	public void renderSheetTexture(int x, int y, int width, int height, float u, float v, float U, float V, int textureWidth, int textureHeight);
	
	/**
	 *  Draws a colored rectangle at the specified point.  This does NOT change the currently-bound
	 *  texture, nor does it modify any OpelGL states, so it may safely be called during rendering operations.
	 */
	public void renderRectangle(int x, int y, int width, int height, Color color);
	
	/**
	 *  Returns true if the passed-in GUI is currently active.
	 *  If null is passed-in, then this method returns true if no GUI is active.
	 */
	public boolean isGUIActive(Class<? extends AGUIBase> guiClass);
	
	/**
	 *  Closes the currently-opened GUI, returning back to the main game.
	 */
	public void closeGUI();
	
	/**
	 *  Opens the passed-in GUI, replacing any opened GUI in the process.
	 */
	public void openGUI(AGUIBase gui);
}
