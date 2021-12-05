package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.mcinterface.InterfaceGUI;

/**Class that renders a cut-out area of the currently bound texture.  This is cleaner than doing manual
 * rendering in the draw calls, and allows for standardized component batch-rendering.
 * 
 *
 * @author don_bruce
 */
public class GUIComponentCutout{
	
	//Rendering variables.
	public final int x;
	public int offsetX;
	public final int y;
	public int offsetY;
	public final int width;
	public final int height;
	
	//Texture variables.
	protected final int textureXOffset;
	protected final int textureYOffset;
	protected final int textureSectionWidth;
	protected final int textureSectionHeight;
	
	//State variables.
	public boolean visible = true;
	
	/**Standard constructor for a 1:1 rendering.**/
	public GUIComponentCutout(int x, int y, int width, int height, int textureXOffset, int textureYOffset){
		this(x, y, width, height, textureXOffset, textureYOffset, width, height);
	}
	
	/**Expanded constructor for 1:x rendering.  This allows the rendered texture component to be a different size than the texture.**/
	public GUIComponentCutout(int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.textureXOffset = textureXOffset;
		this.textureYOffset = textureYOffset;
		this.textureSectionWidth = textureSectionWidth;
		this.textureSectionHeight = textureSectionHeight;
	}
	
	/**
	 *  Renders the texture portion of the cutout.
	 */
    public void renderTexture(int mouseX, int mouseY, int textureWidth, int textureHeight){
    	if(visible){
    		InterfaceGUI.renderSheetTexture(x + offsetX, y + offsetY, width, height, textureXOffset, textureYOffset, textureXOffset + textureSectionWidth, textureYOffset + textureSectionHeight, textureWidth, textureHeight);
		}
    }
}
