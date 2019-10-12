package minecrafttransportsimulator.mcinterface;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**Simple static class to interface with the MC font-rendering system.
 * We don't always need to use this class as some MC interfaces come
 * bundled with a reference to the renderer (like GUIs).  Use the
 * reference when possible to keep us out of conflicts, but otherwise
 * use the renderer here.
 * 
 * @author don_bruce
 */
public class MTSFontRenderer{
	private static FontRenderer mcFontRenderer;
	
	/**Draws text at the position specified.*/
	public static void drawText(String text, int x, int y, Color color, boolean centered, boolean shadow){
		if(mcFontRenderer == null){
			mcFontRenderer = Minecraft.getMinecraft().fontRenderer;
		}
		if(centered){
			x += mcFontRenderer.getStringWidth(text)/2;
		}
		mcFontRenderer.drawString(text, x, y, color.getRGB());
	}
	
	/**Draws a long string of text.  WordWrapping is used here.  Does not support shadows.*/
	public static void drawEssay(String text, int x, int y, Color color, int wrapWidth){
		if(mcFontRenderer == null){
			mcFontRenderer = Minecraft.getMinecraft().fontRenderer;
		}
		mcFontRenderer.drawSplitString(text, x, y, wrapWidth, color.getRGB());
	}
	
	/**Gets the width of some text.  Not normally used, but good if we need to orient something based on text size.*/
	public static int getTextWidth(String text){
		if(mcFontRenderer == null){
			mcFontRenderer = Minecraft.getMinecraft().fontRenderer;
		}
		return mcFontRenderer.getStringWidth(text);
	}
}
