package minecrafttransportsimulator.guis.components;

import java.awt.Color;

import minecrafttransportsimulator.guis.GUIBase;

/**Custom label class.  Allows for batch rendering of text, and easier rendering of labels using
 * state variables rather than actual text boxes.
 *
 * @author don_bruce
 */
public class GUIComponentLabel{
	private static final int BUTTON_TEXTURE_U_OFFSET = 196;
	private static final int BUTTON_TEXTURE_WIDTH = 200;
	private static final int BUTTON_TEXTURE_HEIGHT = 20;
	
	public final int x;
	public final int y;
	public final Color color;
	public final float scale;
	public final boolean centered;
	public final boolean shadow;
	public final int wrapWidth;
	
	public String text;
	    	
	public GUIComponentLabel(int x, int y, Color color, float scale, boolean centered, boolean shadow, int wrapWidth, String text){
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
		this(x, y, color, 1.0F, false, false, -1, text);
	}
	
	/**
	 *  Used for state-based rendering.  This should be overridden for dynamic labels.
	 */
	public boolean visible(){
		return true;
	}
	
    
    /**
	 *  Renders the text that makes up this component.
	 */
    public void renderText(GUIBase currentGUI){
		if(visible()){
	    	if(scale == 1.0){
				currentGUI.drawText(text, x, y, color, centered, shadow, wrapWidth);
			}else{
				currentGUI.drawScaledText(text, x, y, color, centered, shadow, wrapWidth, scale);
			}
		}
    }
}
