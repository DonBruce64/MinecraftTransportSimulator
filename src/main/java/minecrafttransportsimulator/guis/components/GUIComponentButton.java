package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.rendering.instances.RenderText;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;

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
public abstract class GUIComponentButton extends GUIComponentCutout{
	private static final int DEFAULT_BUTTON_SECTION_WIDTH = 200;
	private static final int DEFAULT_BUTTON_SECTION_HEIGHT = 20;
	private static final int DEFAULT_BUTTON_SECTION_WIDTH_OFFSET = 0;
	private static final int DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET = 196;
	public static final int ITEM_BUTTON_SIZE = 18;
	
	public final boolean centeredText;
	
	public boolean enabled = true;
	public final ColorRGB textColor;
	
	/**A Simple button with a rendered string in grey and center-aligned.**/
	public GUIComponentButton(int x, int y, int width, int height, String text){
		this(x, y, width, height, text, true, ColorRGB.DARK_GRAY, DEFAULT_BUTTON_SECTION_WIDTH_OFFSET, DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET, DEFAULT_BUTTON_SECTION_WIDTH , DEFAULT_BUTTON_SECTION_HEIGHT);
	}
	
	/**A button made to render with item slots.  Renders the set item size, and renders the item slot texture off the "Inventory" gui.**/
	public GUIComponentButton(int x, int y){
		this(x, y, ITEM_BUTTON_SIZE, ITEM_BUTTON_SIZE, 194, 0, ITEM_BUTTON_SIZE, ITEM_BUTTON_SIZE);
	}
	
	/**A button without texture or text.  Useful when you want a button for something that needs to render as another component.**/
	public GUIComponentButton(int x, int y, int width, int height){
		this(x, y, width, height, "", true, ColorRGB.DARK_GRAY, 0, 0, 0, 0);
	}
	
	/**A button made to render with custom button textures.  Does not render font, but does provide additional parameters for the size of the button.**/
	public GUIComponentButton(int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight){
		this(x, y, width, height, "", true, ColorRGB.DARK_GRAY, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
	}
	
	/**A complex button with custom height, text alignment, and text color.  Does not render the background texture.*/
	public GUIComponentButton(int x, int y, int width, int height, String text, boolean centeredText, ColorRGB textColor){
		this(x, y, width, height, text, centeredText, textColor, DEFAULT_BUTTON_SECTION_WIDTH_OFFSET, DEFAULT_BUTTON_SECTION_HEIGHT_OFFSET, 0, 0);
	}
	
	/**A fully-customizable button with custom texture alignment and font color.  Note that making the width or the height of the texture section 0 will result in no texture being rendered.**/
	private GUIComponentButton(int x, int y, int width, int height, String text, boolean centeredText, ColorRGB textColor, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight){
		super(x, y, width, height, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
		this.text = text;
		this.centeredText = centeredText;
		this.textColor = textColor;
	}
	
	/**
	 *  Returns true if the mouse can click this button, given the mouse
	 *  position and current button state.  Used to know if we need to call
	 *  {@link #onClicked(boolean)} to do clicking actions.
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
	public abstract void onClicked(boolean leftSide);
	
	/**
	 *  Called when the mouse that clicked this button is released.  At this point
	 *  the mouse may no longer be over the button, but it can be assured that
	 *  the last thing the user clicked was this button.  Use this method for
	 *  things that need to have a maintained state while clicked rather than
	 *  have a single momentary action.
	 */
	public void onReleased(){};
	
	@Override
    public void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks){
    	if(textureSectionWidth != 0 && textureSectionHeight != 0){
			int textureUStart;
    		if(enabled){
				if(mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height){
					textureUStart = textureYOffset + 2*textureSectionHeight;//Highlighted
				}else{
					textureUStart = textureYOffset + 1*textureSectionHeight;//Normal
				}
			}else{
				textureUStart = textureYOffset;//Disabled
			}
    		//Render the left and right sides of the texture, but cut the middle so they don't squash pixels.
    		InterfaceGUI.renderSheetTexture(x, y, width/2, height, textureXOffset, textureUStart, textureXOffset + width/2, textureUStart + textureSectionHeight, textureWidth, textureHeight);
    		InterfaceGUI.renderSheetTexture(x + width/2, y, width/2, height, textureXOffset + textureSectionWidth - width/2, textureUStart, textureXOffset + textureSectionWidth, textureUStart + textureSectionHeight, textureWidth, textureHeight);
		}
    }
    
    @Override
	public void renderText(boolean renderTextLit){
    	if(!text.isEmpty()){
    		RenderText.draw2DText(text, null, centeredText ? x + width/2 : x, y + (height-8)/2, textColor, centeredText ? TextAlignment.CENTERED : TextAlignment.LEFT_ALIGNED, 1.0F, false, 0);
    	}
    }
}
