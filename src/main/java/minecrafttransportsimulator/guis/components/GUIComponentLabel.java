package minecrafttransportsimulator.guis.components;

import java.awt.Color;

import minecrafttransportsimulator.wrappers.WrapperGUI;

/**Custom label class.  Allows for batch rendering of text, and easier rendering of labels using
 * state variables rather than actual text boxes.  Also allows for linking with either a
 * {@link GUIComponentButton} or {@link GUIComponentTextBox} to avoid having to set visibility
 * and instead to use the visibility of those objects.
 *
 * @author don_bruce
 */
public class GUIComponentLabel{
	public final int x;
	public final int y;
	public final Color color;
	public final float scale;
	public final boolean centered;
	public final boolean shadow;
	public final int wrapWidth;
	
	public boolean visible = true;
	public String text;
	private GUIComponentButton button;
	private GUIComponentTextBox box;
	    	
	public GUIComponentLabel(int x, int y, Color color, String text, float scale, boolean centered, boolean shadow, int wrapWidth){
		this.x = x;
		this.y = y;
		this.color = color;
		this.scale = scale;
		this.text = text;
		this.centered = centered;
		this.shadow = shadow;
		this.wrapWidth = wrapWidth;
	}
	
	//Simpler constructor for simple labels.
	public GUIComponentLabel(int x, int y, Color color, String text){
		this(x, y, color, text, 1.0F, false, false, 0);
	}
	
	/**
	 *  Sets the associated button for this class.  This will make
	 *  this label render only when the button is visible.
	 *  Returns this object for ease of constructing.
	 */
	public GUIComponentLabel setButton(GUIComponentButton button){
		this.button = button;
		return this;
	}
	
	/**
	 *  Sets the associated textBox for this class.  This will make
	 *  this label render only when the textBox is visible.
	 *  Returns this object for ease of constructing.
	 */
	public GUIComponentLabel setBox(GUIComponentTextBox box){
		this.box = box;
		return this;
	}
    
    /**
	 *  Renders the text that makes up this component.
	 *  Uses the button or box visibility if those components are set.
	 */
    public void renderText(){
		if(button == null ? (box == null ? visible : box.visible) : button.visible){
	    	if(scale == 1.0){
	    		WrapperGUI.drawText(text, x, y, color, centered, shadow, wrapWidth);
			}else{
				WrapperGUI.drawScaledText(text, x, y, color, centered, shadow, wrapWidth, scale);
			}
		}
    }
}
