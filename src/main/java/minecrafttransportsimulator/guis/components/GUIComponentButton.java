package minecrafttransportsimulator.guis.components;

import java.awt.Color;

import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;

/**Custom button class.  This allows for a custom button texture, as well as a cleaner constructor.
 * It also allows us to cut out a few MC methods from their own button class and use our own.
 * The simplest constructor will create a button at the point specified with a height of 20
 * and centered text.  The more complex constructor will create it with the specified height
 * and gives the choice of centered text or not.  The most complex constructor gives all those
 * options, as well as allowing the selection of where on the texture sheet to render the button.
 * 
 * 
 *
 * @author don_bruce
 */
public abstract class GUIComponentButton{
	private static final int DEFAULT_TEXTURE_WIDTH = 256;
	private static final int DEFAULT_TEXTURE_HEIGHT = 256;
	private static final int DEFAULT_BUTTON_SECTION_WIDTH = 200;
	private static final int DEFAULT_BUTTON_SECTION_HEIGHT = 20;
	private static final int DEFAULT_BUTTON_SECTION_WIDTH_OFFSET = 0;
	private static final int DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET = 196;
	public static final int ITEM_BUTTON_SIZE = 18;
	
	public final int x;
	public final int y;
	public final int width;
	public final int height;
	public final boolean centeredText;
	public final int buttonSectionWidth;
	public final int buttonSectionHeight;
	public final int buttonSectionWidthOffset;
	public final int buttonSectionHeightOffset;
	public final int textureWidth;
	public final int textureHeight;
	
	public boolean visible = true;
	public boolean enabled = true;
	public String text;
	
	/**A Simple button with a set height of 20**/
	public GUIComponentButton(int x, int y, int width, String text){
		this(x, y, width, text, 20, true);
	}
	
	/**A button made to render with item slots.  Renders the set item size, and does not render with a texture.**/
	public GUIComponentButton(int x, int y){
		this(x, y, ITEM_BUTTON_SIZE, "", ITEM_BUTTON_SIZE, true, ITEM_BUTTON_SIZE, ITEM_BUTTON_SIZE, 194, 0, DEFAULT_TEXTURE_WIDTH, DEFAULT_TEXTURE_HEIGHT);
	}
	
	/**A complex button with a custom height and text alignment.**/
	public GUIComponentButton(int x, int y, int width, String text, int height, boolean centeredText){
		this(x, y, width, text, height, centeredText, DEFAULT_BUTTON_SECTION_WIDTH, DEFAULT_BUTTON_SECTION_HEIGHT, DEFAULT_BUTTON_SECTION_WIDTH_OFFSET, DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET, DEFAULT_TEXTURE_WIDTH, DEFAULT_TEXTURE_HEIGHT);
	}
	
	/**A fully-customizable button with custom texture alignment.**/
	public GUIComponentButton(int x, int y, int width, String text, int height, boolean centeredText, int buttonSectionWidth, int buttonSectionHeight, int buttonSectionWidthOffset, int buttonSectionHeightOffset, int textureWidth, int textureHeight){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.text = text;
		this.centeredText = centeredText;
		this.buttonSectionWidth = buttonSectionWidth;
		this.buttonSectionHeight = buttonSectionHeight;
		this.buttonSectionWidthOffset = buttonSectionWidthOffset;
		this.buttonSectionHeightOffset = buttonSectionHeightOffset;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}
	
	/**
	 *  Returns true if the mouse can click this button, given the mouse
	 *  position and current button state.  Used to know if we need to call
	 *  {@link #onClicked()} to do clicking actions.
	 */
	public boolean canClick(int xPos, int yPos){
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
	 *  When rendering, we use the currently-bound texture and selected points.  This is done to allow 
	 *  for dynamic button textures in different GUIs without the need to sub-class this class.  Also 
	 *  note that the text is NOT rendered here; that is done in its own method as rendering text would 
	 *  require a texture switch in this method.
	 */
    public void renderButton(int mouseX, int mouseY){
    	if(visible){
			int textureUStart;
    		if(enabled){
				if(mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height){
					textureUStart = buttonSectionHeightOffset + 2*buttonSectionHeight;//Highlighted
				}else{
					textureUStart = buttonSectionHeightOffset + 1*buttonSectionHeight;//Normal
				}
			}else{
				textureUStart = buttonSectionHeightOffset;//Disabled
			}
    		InterfaceGUI.renderSheetTexture(x, y, width/2, height, buttonSectionWidthOffset, textureUStart, buttonSectionWidthOffset + width/2, textureUStart + buttonSectionHeight, textureWidth, textureHeight);
    		InterfaceGUI.renderSheetTexture(x + width/2, y, width/2, height, buttonSectionWidthOffset + buttonSectionWidth - width/2, textureUStart, buttonSectionWidthOffset + buttonSectionWidth, textureUStart + buttonSectionHeight, textureWidth, textureHeight);
		}
    }
    
    /**
	 *  Renders the button's text.  This is done separately from the button to allow all buttons to render in one pass
	 *  before binding the font texture for rendering text.  It also prevents oddities that occur from font
	 *  rendering with respect to OpenGL states.
	 */
    public void renderText(){
    	if(visible){
    		InterfaceGUI.drawBasicText(text, null, centeredText ? x + width/2 : x, y + (height-8)/2, Color.DARK_GRAY, centeredText ? TextPosition.CENTERED : TextPosition.LEFT_ALIGNED, 0);
    	}
    }
    
    /**
	 *  Renders the tooltip for this button.  This needs to be done after the main buttons
	 *  render as otherwise it will render behind other buttons.  This method needs an
	 *  instance of {@link AGUIBase} to ensure the tooltip doesn't render off the screen.
	 *  Most buttons don't have tooltips, but buttons that have complex functionality 
	 *  may need them to help explain what they do.
	 */
    public void renderTooltip(AGUIBase gui, int mouseX, int mouseY){
    	//Do nothing.
    }
}
