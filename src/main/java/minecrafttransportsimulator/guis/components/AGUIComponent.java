package minecrafttransportsimulator.guis.components;

import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.instances.RenderText;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;

/**Base class for all components.  Contains basic common variables to all rendering.
 *
 * @author don_bruce
 */
public abstract class AGUIComponent{
	
	//Rendering variables.
	public final int x;
	public int offsetX;
	public final int y;
	public int offsetY;
	public final int width;
	public final int height;
	
	//State variables.
	public boolean visible = true;
	public String text;
	
	private static final int TOOLTIP_BORDER_PADDING = 4;
	private static final int TOOLTIP_SECTION_WIDTH = 100;
	private static final int TOOLTIP_SECTION_HEIGHT = 60;
	private static final int TOOLTIP_SECTION_BORDER = 3;
	private static final int TOOLTIP_SECTION_WIDTH_OFFSET = 100;
	private static final int TOOLTIP_SECTION_HEIGHT_OFFSET = 196;
	private static final int TOOLTIP_TEXTURE_WIDTH = 256;
	private static final int TOOLTIP_TEXTURE_HEIGHT =256;
	
	public AGUIComponent(int x, int y, int width, int height){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	/**
	 *  Returns true if the mouse is within the bounds of this component.
	 */
    public boolean isMouseInBounds(int mouseX, int mouseY){
    	return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
	
	/**
	 *  Renders the main portion of the component.
	 *  Note that this method, and all other methods, are not called if {@link #visible} is false.
	 */
    public abstract void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks);
    
    /**
	 *  Renders the component's text.  This is done separately from the main render as text uses its own texture,
	 *  and we don't want to do this until the rest of the GUI is rendered as it will cause a texture bind.
	 *  Note that this method is not called if {@link #text} is null or empty.
	 */
    public void renderText(boolean renderTextLit){}
    
    /**
	 *  Renders the tooltip for this component.  This needs to be done after the main render
	 *  of all components as otherwise it will render behind other components.
	 *  Most components don't have tooltips, but components that have complex functionality 
	 *  may need them to help explain what they do.  Note that this method is not called if
	 *  {@link #isMouseInBounds(int, int)} returns false, and that this method won't render
	 *  anything if {@link #getTooltipText()} returns null.
	 */
    public final void renderTooltip(int mouseX, int mouseY, int screenWidth, int screenHeight){
    	List<String> tooltipTextLines = getTooltipText();
    	if(tooltipTextLines != null && !tooltipTextLines.isEmpty()){
    		//Find the max string width.  This is used to define text bounds.
    		//We need to know how many lines we wrap to offset our rendering of our text.
    		int wrapWidth = screenWidth - mouseX > mouseX ? screenWidth - mouseX - 2*TOOLTIP_BORDER_PADDING :  mouseX - 2*TOOLTIP_BORDER_PADDING;
    		float longestLineWidth = 0;
    		int linesOfText = 0;
    		String tooltipCombinedText = "";
    		for(String tooltipText : tooltipTextLines){
    			float lineWidth = RenderText.getStringWidth(tooltipText, null);
    			linesOfText += Math.ceil(lineWidth/wrapWidth);
    			tooltipCombinedText += tooltipText + "\n";
    			if(lineWidth > longestLineWidth){
    				longestLineWidth = lineWidth;
    			}
    		}
    		int actualStringWidth = (int) (longestLineWidth > wrapWidth ? wrapWidth : longestLineWidth);
    		int actualStringHeight = (int) Math.ceil(RenderText.getHeight(linesOfText, null));
    		
    		//We now know how wide the text will be, and how high.  Find quadrant.
    		int xOffset;
    		if(screenWidth - mouseX > mouseX){
    			//Render right.
    			xOffset = mouseX + TOOLTIP_BORDER_PADDING;
    		}else{
    			//Render left.
    			xOffset = mouseX - actualStringWidth - 2*TOOLTIP_BORDER_PADDING;
    		}
    		
    		int yOffset;
    		if(screenHeight - mouseY > mouseY){
    			//Render bottom.
				yOffset = mouseY + TOOLTIP_BORDER_PADDING;
			}else{
				//Render top.
				yOffset = mouseY - actualStringHeight - 2*TOOLTIP_BORDER_PADDING;
			}
    		
    		//Do the text and background render.
    		//TODO make this dynamic or something?
        	GL11.glTranslated(0, 0, 250);
        	InterfaceRender.bindTexture(AGUIBase.STANDARD_TEXTURE_NAME);
    		//TODO make this use static parameters in the main GUI when it's moved there.
        	
        	//Render the 4 corners, and then the 4 edge bits.  This prevents stretching.
        	int horizontalSegmentSize = actualStringWidth + 2*TOOLTIP_BORDER_PADDING;
        	int verticalSegmentSize = actualStringHeight + 2*TOOLTIP_BORDER_PADDING;
        	
        	//Top-left.
    		InterfaceGUI.renderSheetTexture(xOffset, yOffset, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET, TOOLTIP_SECTION_HEIGHT_OFFSET, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		//Top-right.
    		InterfaceGUI.renderSheetTexture(xOffset + horizontalSegmentSize - TOOLTIP_SECTION_BORDER, yOffset, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		//Bottom-left.
    		InterfaceGUI.renderSheetTexture(xOffset, yOffset + verticalSegmentSize - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		//Bottom-right.
    		InterfaceGUI.renderSheetTexture(xOffset + horizontalSegmentSize - TOOLTIP_SECTION_BORDER, yOffset + verticalSegmentSize - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		
    		//Top-center.
    		InterfaceGUI.renderSheetTexture(xOffset + TOOLTIP_SECTION_BORDER, yOffset, horizontalSegmentSize - 2*TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		//Bottom-center.
    		InterfaceGUI.renderSheetTexture(xOffset + TOOLTIP_SECTION_BORDER, yOffset + verticalSegmentSize - TOOLTIP_SECTION_BORDER, horizontalSegmentSize - 2*TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		//Left-center.
    		InterfaceGUI.renderSheetTexture(xOffset, yOffset + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, verticalSegmentSize - 2*TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		//Right-center.
    		InterfaceGUI.renderSheetTexture(xOffset + horizontalSegmentSize - TOOLTIP_SECTION_BORDER, yOffset + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_BORDER, verticalSegmentSize - 2*TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		
    		//Actual-center.
    		InterfaceGUI.renderSheetTexture(xOffset + TOOLTIP_SECTION_BORDER, yOffset + TOOLTIP_SECTION_BORDER, horizontalSegmentSize - 2*TOOLTIP_SECTION_BORDER, verticalSegmentSize - 2*TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_WIDTH_OFFSET + TOOLTIP_SECTION_WIDTH - TOOLTIP_SECTION_BORDER, TOOLTIP_SECTION_HEIGHT_OFFSET + TOOLTIP_SECTION_HEIGHT - TOOLTIP_SECTION_BORDER, TOOLTIP_TEXTURE_WIDTH, TOOLTIP_TEXTURE_HEIGHT);
    		
    		GL11.glTranslated(0, 0, 50);
    		RenderText.draw2DText(tooltipCombinedText, null, xOffset + TOOLTIP_BORDER_PADDING, yOffset + TOOLTIP_BORDER_PADDING, ColorRGB.WHITE, TextAlignment.LEFT_ALIGNED, 1.0F, false, wrapWidth);
    		GL11.glTranslated(0, 0, -300);
    	}
    }
    
    /**
	 *  Returns the tooltip text lines for this tooltip.  Only called if {@link #renderTooltip(int, int, int)}
	 *  is called.
	 */
    public List<String> getTooltipText(){
    	return null;
    }
}
