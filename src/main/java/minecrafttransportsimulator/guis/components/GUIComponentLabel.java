package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.instances.RenderText;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;

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
	public final TextAlignment alignment;
	public final int wrapWidth;
	public final float scale;
	
	public boolean visible = true;
	public String text;
	private GUIComponentButton button;
	private GUIComponentTextBox box;

	public GUIComponentLabel(int x, int y, ColorRGB color, String text){
		this(x, y, color, text, TextAlignment.LEFT_ALIGNED, 1.0F, 0, null);
	}
	
	public GUIComponentLabel(int x, int y, ColorRGB color, String text, TextAlignment alignment, float scale){
		this(x, y, color, text, alignment, scale, 0, null);
	}
	
	public GUIComponentLabel(int x, int y, ColorRGB color, String text, TextAlignment alignment, float scale, int wrapWidth){
		this(x, y, color, text, alignment, scale, wrapWidth, null);
	}
	
	public GUIComponentLabel(int x, int y, ColorRGB color, String text, TextAlignment alignment, float scale, int wrapWidth, String fontName){
		this.x = x;
		this.y = y;
		this.color = color;
		this.text = text;
		this.fontName = fontName;
		this.alignment = alignment;
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
			RenderText.draw2DText(text, fontName, x, y, color, alignment, scale, wrapWidth);
		}
    }
}
