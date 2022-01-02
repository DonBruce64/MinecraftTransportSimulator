package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.components.RenderableObject;

/**Class that renders a cut-out area of the currently bound texture.  This is cleaner than doing manual
 * rendering in the draw calls, and allows for standardized component batch-rendering.
 * 
 *
 * @author don_bruce
 */
public class GUIComponentCutout extends AGUIComponent{
	
	//Texture variables.
	public int textureXOffset;
	public int textureYOffset;
	public int textureSectionWidth;
	public int textureSectionHeight;
	
	/**Standard constructor for a 1:1 rendering.**/
	public GUIComponentCutout(int x, int y, int width, int height, int textureXOffset, int textureYOffset){
		this(x, y, width, height, textureXOffset, textureYOffset, width, height);
	}
	
	/**Expanded constructor for 1:x rendering.  This allows the rendered texture component to be a different size than the texture.**/
	public GUIComponentCutout(int x, int y, int width, int height, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight){
		super(x, y, width, height);
		this.textureXOffset = textureXOffset;
		this.textureYOffset = textureYOffset;
		this.textureSectionWidth = textureSectionWidth;
		this.textureSectionHeight = textureSectionHeight;
	}
	
	@Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean blendingEnabled, float partialTicks){
		if(renderable == null){
			renderable = new RenderableObject("gui_cutout", gui.getTexture(), ColorRGB.WHITE, FloatBuffer.allocate(8*6), false);
		}
		if(renderable.vertices.position() == 0){
			gui.addRenderToBuffer(renderable.vertices, 0, 0, width, height, textureXOffset, textureYOffset, textureXOffset + textureSectionWidth, textureYOffset + textureSectionHeight, gui.getTextureWidth(), gui.getTextureHeight());
			renderable.vertices.flip();
		}
		GL11.glTranslated(position.x, position.y, position.z);
		renderable.render();
		GL11.glTranslated(-position.x, -position.y, -position.z);
    }
}
