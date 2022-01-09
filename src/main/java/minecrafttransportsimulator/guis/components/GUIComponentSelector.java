package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import minecrafttransportsimulator.rendering.instances.RenderText;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;

/**Custom class for selectors.  Selectors are like buttons, except they have a defined state that's saved internally.
 * This state allows for the selector's texture to be manually set.  They also render their text below the selector,
 * rather than on it like how buttons do.  It is assumed the selector will have a custom texture area as it will need
 * to be offset for every click, so the long-form constructor is the only one available here.  Note that
 * the text passed-in will be rendered below the selector rather than at the center like buttons.
 *
 * @author don_bruce
 */
public abstract class GUIComponentSelector extends GUIComponentButton{
	
	public final ColorRGB regularColor;
	public final ColorRGB litColor;
	
	public int selectorState = 0;
	
	public GUIComponentSelector(int x, int y, int width, int height, String text, ColorRGB regularColor, ColorRGB litColor, int textureXOffset, int textureYOffset, int textureSectionWidth, int textureSectionHeight){
		super(x, y, width, height, textureXOffset, textureYOffset, textureSectionWidth, textureSectionHeight);
		this.text = text;
		this.textPosition.set(position.x + width/2, position.y - height - 1, textPosition.z);
		this.regularColor = regularColor != null ? regularColor : ColorRGB.WHITE;
		this.litColor = regularColor != null ? litColor : ColorRGB.WHITE;
	}
    
	@Override
	public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks){
		if(renderable == null){
			for(int i=0; i<3; ++i){
				int textureUStart = textureYOffset + i*textureSectionHeight;
    			FloatBuffer buffer = FloatBuffer.allocate(8*6);
    			gui.addRenderToBuffer(buffer, 0, 0, width, height, textureXOffset, textureUStart, textureXOffset + textureSectionWidth, textureUStart + textureSectionHeight, gui.getTextureWidth(), gui.getTextureHeight());
    			buffer.flip();
    			
    			if(i==0){
    				renderable = new RenderableObject("gui_selector_0", gui.getTexture(), ColorRGB.WHITE, buffer, false);
    			}else if(i==1){
    				renderable2 = new RenderableObject("gui_selector_1", gui.getTexture(), ColorRGB.WHITE, buffer, false);
    			}else{
    				renderable3 = new RenderableObject("gui_selector_2", gui.getTexture(), ColorRGB.WHITE, buffer, false);
    			}
			}
		}
		
		GL11.glTranslated(position.x, position.y, position.z);
		if(selectorState == 0){
			renderable.disableLighting = renderBright || ignoreGUILightingState;
			renderable.texture = renderLitTexture ? gui.getTexture().replace(".png", "_lit.png") : gui.getTexture();
			renderable.render();
		}else if(selectorState == 1){
			renderable2.disableLighting = renderBright || ignoreGUILightingState;
			renderable2.texture = renderLitTexture ? gui.getTexture().replace(".png", "_lit.png") : gui.getTexture();
			renderable2.render();
		}else{
			renderable3.disableLighting = renderBright || ignoreGUILightingState;
			renderable3.texture = renderLitTexture ? gui.getTexture().replace(".png", "_lit.png") : gui.getTexture();
			renderable3.render();
		}
		GL11.glTranslated(-position.x, -position.y, -position.z);
    }
	
    @Override
	public void renderText(boolean renderTextLit){
    	RenderText.drawText(text, null, textPosition, null, (renderTextLit || ignoreGUILightingState) ? litColor : regularColor, TextAlignment.CENTERED, 0.75F, false, 0, 1.0F, renderTextLit || ignoreGUILightingState);
    }
}
