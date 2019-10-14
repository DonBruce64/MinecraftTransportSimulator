package minecrafttransportsimulator.mcinterface;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**Simple static class to interface with MC rendering systems.
 * Use this class when rendering using MC systems, like texture
 * binding, font rendering, or item display.  This class will
 * cache renderers, which <i>shouldn't,/i> cause problems as
 * MC doesn't re-create the whole rendering system instances.
 * Might cause issues with other mods, but I don't think it will...
 * 
 * @author don_bruce
 */
public class MTSRendererInterface{
	private static TextureManager mcTextureManager;
	private static FontRenderer mcFontRenderer;
	private static RenderItem mcItemRenderer;
	private static EntityRenderer mcEntityRenderer;
	
	//---------------START OF TEXTURE SYSTEM METHODS---------------//
	/**Loads the current texture into the render engine for rendering.*/
	public static void bindTexture(ResourceLocation texture){
		if(mcTextureManager == null){
			mcTextureManager = Minecraft.getMinecraft().renderEngine;
		}
		mcTextureManager.bindTexture(texture);
	}

	
	
	//---------------START OF FONT SYSTEM METHODS---------------//
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
	
	
	
	//---------------START OF ITEM SYSTEM METHODS---------------//
	/**Renders an item.  Used in GUI operations to allow for proper item rendering.*/
	public static void renderItem(ItemStack stack, int x, int y){
		if(mcItemRenderer == null){
			mcItemRenderer = Minecraft.getMinecraft().getRenderItem();
		}
		mcItemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
	}
	
	
	
	//---------------START OF ENTITY SYSTEM METHODS---------------//
	/**Disables the lightmap.  This prevents shading on objects by MC.
	 * Note that while the MC shading is disabled, the OpenGL shading
	 * will still be active.  Essentially, all this does is makes the
	 * GPU see everything as facing the sun, so anything rendered won't
	 * take the normal position relative to the light source into account.
	 * If you want to make something bright in a dark area, you'll have
	 * to disable the OpenGL lighting via GL11.glDisable(GL11.GL_LIGHTING);
	 * Doing so will likely break shaders, so be warned.
	 * */
	public static void disableLightmap(){
		if(mcEntityRenderer == null){
			mcEntityRenderer = Minecraft.getMinecraft().entityRenderer;
		}
		mcEntityRenderer.disableLightmap();		
	}
	
	/**Re-enables the lightmap..*/
	public static void enableLightmap(){
		if(mcEntityRenderer == null){
			mcEntityRenderer = Minecraft.getMinecraft().entityRenderer;
		}
		mcEntityRenderer.enableLightmap();		
	}
}
