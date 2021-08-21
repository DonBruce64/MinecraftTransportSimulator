package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;

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
	public final ColorRGB color;
	public final String fontName;
	public final TextPosition renderMode;
	public final int wrapWidth;
	public final float scale;
	public final boolean autoScale;
	
	public boolean visible = true;
	public String text;
	private GUIComponentButton button;
	private GUIComponentTextBox box;

	public GUIComponentLabel(int x, int y, ColorRGB color, String text){
		this(x, y, color, text, null, TextPosition.LEFT_ALIGNED, 0, 1.0F, false);
	}
	
	public GUIComponentLabel(int x, int y, ColorRGB color, String text, String fontName, TextPosition renderMode, int wrapWidth, float scale, boolean autoScale){
		this.x = x;
		this.y = y;
		this.color = color;
		this.text = text;
		this.fontName = fontName;
		this.renderMode = renderMode;
		this.autoScale = autoScale;
		this.scale = scale;
		this.wrapWidth = wrapWidth;
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
			InterfaceGUI.drawScaledText(text, fontName, x, y, color, renderMode, wrapWidth, scale, autoScale);
		}
    }
}
