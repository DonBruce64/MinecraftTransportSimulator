package minecrafttransportsimulator.guis.components;

import java.awt.Color;

import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;

/**Custom text box class.  This class is designed to remove the need to check for clicking of
 * text boxes and to manually set their text, as well as the odd getter/setter methods
 * that MC's text boxes have that aren't in other components.  Provides a more streamlined
 * object this way.  This text box also allows for word-wrapping of text.  Something
 * sorely needed in the base text box rendering routines...
 *
 * @author don_bruce
 */
public class GUIComponentTextBox{	
	private static final String VALID_SPECIAL_CHARS = "/*!@#$%^&*()\"{}_[]|\\?/<>,.- ";
	
	public final int x;
	public final int y;
	public final int width;
	public final int height;
	public Color fontColor;
	public final Color backgroundColor;
	
	public boolean visible = true;
	public boolean enabled = true;
	public boolean focused = false;
	
	private int position;
	private int maxTextLength;
	private String text;
	private String flashText;
	    	
	public GUIComponentTextBox(int x, int y, int width, String text){
		this(x, y, width, text, 20, Color.WHITE, Color.BLACK, 32);
	}
	
	public GUIComponentTextBox(int x, int y, int width, String text, int height, Color fontColor, Color backgroundColor, int maxTextLength){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.fontColor = fontColor;
		this.backgroundColor = backgroundColor;
		this.maxTextLength = maxTextLength;
		setText(text);
	}
	
	/**
	 *  Updates the focused property depending on mouse position and state.  Should be called whenever
	 *  the mouse is clicked to focus the text box and allow the GUI to send user input to it. 
	 */
	public void updateFocus(int xPos, int yPos){
		focused = visible && enabled && x < xPos && xPos < x + width && y < yPos && yPos < y + height;
	}
	
	/**
	 *  Handles the current key input.  Depending on the key, this will either add 
	 *  the typed character to the text, delete a character, or change the current
	 *  cursor position.  Note that keyCode is one of the pre-defined {@link TextBoxControlKey}s.
	 *  This allows for consistent handling of inputs locally, and makes it up to the caller of
	 *  this function to decide what inputs constitute the control keys.  The main reason
	 *  for this is that it prevents the need to call in any specific LWJGL libraries that may
	 *  or may not change for the heck of it.
	 */
	public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
		//Handle control operation, or basic key typed.
		if(control != null){
			if(control.equals(TextBoxControlKey.BACKSPACE)){
				if(position != 0){
					//Remove the char before the current position.
					text = text.substring(0, position - 1) + text.substring(position, text.length());
					--position;
				}
			}else if(control.equals(TextBoxControlKey.DELETE)){
				if(position != text.length()){
					//Remove the char at the current position.
					text = text.substring(0, position) + text.substring(position + 1, text.length());
				}
			}else if(control.equals(TextBoxControlKey.LEFT)){
				if(position > 0){
					--position;
				}
			}else if(control.equals(TextBoxControlKey.RIGHT)){
				if(position < text.length()){
					++position;
				}
			}
		}else if(Character.isLetterOrDigit(typedChar) || VALID_SPECIAL_CHARS.contains(Character.toString(typedChar))){
			if(text.length() < maxTextLength){
				String newText = text.substring(0, position) + typedChar + text.substring(position, text.length());
				if(validateText(newText)){
					text = newText;
					++position;
				}
			}
		}
		
		//Update flashText;
		if(text.isEmpty()){
			flashText = "_";
		}else if(position == text.length()){
			flashText = text + "_";
		}else{
			flashText = text.substring(0, position) + "_" + text.substring(position + 1, text.length());
		}
	}
	
	/**
	 *  Validates the text.  This is called with new text prior to setting it.
	 *  If the function returns false, then the text will not change.
	 *  This is only called for key presses, as it is assumed that the code-based
	 *  text will already be validated to the appropriate data type.
	 */
	public boolean validateText(String newText){
		return true;
	}
	
	/**
	 *  Gets the current text of this component.
	 */
	public String getText(){
		return text;
	}
	
	/**
	 *  Sets the text for this component.  Trimming it to meet the maxTextLength
	 *  parameter if required.  Also updates flashText to ensure proper rendering.
	 *  This also resets the pointer to the end of the passed-in string to prevent
	 *  an old reference.
	 */
	public void setText(String newText){
		if(newText.length() > maxTextLength){
			text = newText.substring(0, maxTextLength);
		}else{
			text = newText;
		}
		position = text.length();
		
		//Set flash text for cursor.  Should flash at current position.
		if(text.isEmpty()){
			flashText = "_";
		}else if(position == 0){
			flashText = "_" + text.substring(1);
		}else if(position == text.length()){
			flashText = text + "_";
		}else{
			flashText = text.substring(0, position) + text.substring(position + 1);
		} 
	}
	
	/**
	 *  Renders the box's background and text.  The background will always be a solid color
	 *  independent of the currently-bound texture, while the text will cause the font texture
	 *  to be the currently-bound texture at the end of the render.  Because of this, it is
	 *  recommended to render all text boxes with {@link GUIComponentLabel}s and other font-based
	 *  GUI components so textures don't need to be switched out.
	 */
    public void renderBox(){
    	if(visible){
    		if(backgroundColor != null){
    			InterfaceGUI.renderRectangle(x, y, width, height, backgroundColor);
    		}
    		//If this box is less than 20px high, center the text.
    		//Otherwise, render it at the top aligned.
    		if(enabled){
    			if(focused && AGUIBase.inClockPeriod(20, 10)){
    				InterfaceGUI.drawBasicText(flashText, null, x + 4, y + (height >= 20 ? 5 : 1 + height/10), fontColor, TextPosition.LEFT_ALIGNED, width);
    			}else{
    				InterfaceGUI.drawBasicText(text, null, x + 4, y + (height >= 20 ? 5 : 1 + height/10), fontColor, TextPosition.LEFT_ALIGNED, width);
    			}
    		}else{
    			InterfaceGUI.drawBasicText(text, null, x + 4, y + (height >= 20 ? 5 : 1 + height/10), Color.GRAY, TextPosition.LEFT_ALIGNED, width);
    		}
    	}
    }
    
	/**
	 *  Enums for control keys of this text box.  Send these in when a specific action needs to occur other
	 *  than adding text.
	 */
    public enum TextBoxControlKey{
    	BACKSPACE, DELETE, LEFT, RIGHT;
    }
}
