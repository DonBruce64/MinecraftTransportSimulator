package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
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
		this.regularColor = regularColor != null ? regularColor : ColorRGB.WHITE;
		this.litColor = regularColor != null ? litColor : ColorRGB.WHITE;
	}
    
	@Override
	public void render(int mouseX, int mouseY, int textureWidth, int textureHeight, boolean blendingEnabled, float partialTicks){
		int textureUStart = textureYOffset + selectorState*textureSectionHeight;
		InterfaceGUI.renderSheetTexture(x, y, width, height, textureXOffset, textureUStart, textureXOffset + textureSectionWidth, textureUStart + textureSectionHeight, textureWidth, textureHeight);
    }
	
    @Override
	public void renderText(boolean lightsOn){
    	RenderText.draw2DText(text, null, x + width/2, y + height + 1, lightsOn ? litColor : regularColor, TextAlignment.CENTERED, 0.75F, false, 0);
    }
}
