package minecrafttransportsimulator.guis.components;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.mcinterface.BuilderGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Interface for MC GUI classes.  Allows access to various GUI-specific functions.
*
* @author don_bruce
*/
public class InterfaceGUI{
	private static RenderItem itemRenderer;
	private static Map<String, FontRenderer> fontRenderers = new HashMap<String, FontRenderer>();
	
	/**
	 *  Draws the item's tooltip on the GUI.  This should be
	 *  the last thing that gets rendered, as otherwise it may render
	 *  behind other components.
	 */
	public static void drawItemTooltip(AGUIBase gui, int mouseX, int mouseY, ItemStack stack){
		Minecraft mc = Minecraft.getMinecraft();
		List<String> tooltipText = stack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        for(int i = 0; i < tooltipText.size(); ++i){
            if(i == 0){
                tooltipText.set(i, tooltipText.get(i));
            }else{
                tooltipText.set(i, TextFormatting.GRAY + tooltipText.get(i));
            }
        }
		GuiUtils.drawHoveringText(stack, tooltipText, mouseX, mouseY, mc.currentScreen.width, mc.currentScreen.height, -1, mc.fontRenderer);
	}
	
	/**
	 *  Draws a tooltip into the GUI.  This is for things that are NOT items, so
	 *  rather than passing-in item parameters you need to pass in the lines to render.
	 *  This should be rendered at the end of the render call to prevent the odd texture
	 *  binding of this method from conflicting from other renders.
	 */
	public static void drawGenericTooltip(AGUIBase gui, int mouseX, int mouseY, String tooltip){
		Minecraft mc = Minecraft.getMinecraft();
		GuiUtils.drawHoveringText(Arrays.asList(new String[]{tooltip}), mouseX, mouseY, mc.currentScreen.width, mc.currentScreen.height, -1, mc.fontRenderer);
	}
	
	/**
	 *  Helper method for getting the font renderer for a font.
	 */
	private static FontRenderer getRenderer(String fontName){
		FontRenderer fontRenderer = fontRenderers.get(fontName);
		if(fontRenderer == null){
			if(fontName != null){
				fontRenderer = new CustomFontRenderer(fontName);
			}else{
				fontRenderer = Minecraft.getMinecraft().fontRenderer;
			}
			fontRenderers.put(fontName, fontRenderer);
		}
		return fontRenderer;
	}
	
	private static class CustomFontRenderer extends FontRenderer{
		private final String fontName;
		
		public CustomFontRenderer(String fontName){
			super(Minecraft.getMinecraft().gameSettings, new ResourceLocation(fontName + ".png"), Minecraft.getMinecraft().renderEngine, false);
			this.fontName = fontName;
			
			//Manually parse glyph sizes because MC hard-codes it like a stupid game.
			readGlyphSizes();
		}
		
		@Override
		public void onResourceManagerReload(IResourceManager resourceManager){
	        super.onResourceManagerReload(resourceManager);
	        this.readGlyphSizes();
	    }
		
		private void readGlyphSizes(){
			IResource iresource = null;
			try{
				//Try custom sizes.
	            iresource = getResource(new ResourceLocation(fontName + ".bin"));
	            iresource.getInputStream().read(this.glyphWidth);
	            return;
	        }catch(IOException ioexception){
	        	//Do nothing.  Try normal sizes later.
	        }finally{
	            IOUtils.closeQuietly(iresource);
	        }
			
			try{
        		//Try normal sizes.
                iresource = getResource(new ResourceLocation("font/glyph_sizes.bin"));
                iresource.getInputStream().read(this.glyphWidth);
            }catch (IOException ioexception){
                throw new RuntimeException(ioexception);
            }finally{
                IOUtils.closeQuietly(iresource);
            }
		}
	};
	
	/**
	 *  Draws the specified text using the font passed-in.  May be null to use the default font as specified by MC
	 *  or loaded by the current ResourcePack..  This method can render the text in multiple ways depending
	 *  on the parameters passed-in.  If a centered string is specified, then the point passed-in should be  the center 
	 *  point of the string, rather that the top-left of the string like normal.  If wrapWidth is anything else but 0, 
	 *  then the wordWrap method will be called to render multi-line text.  Note that after this operation the font texture 
	 *  will be bound, so take care when calling this method in the middle of rendering operations.
	 */
	public static void drawBasicText(String text, String customFontName, int x, int y, ColorRGB color, TextPosition renderPosition, int wrapWidth){
		FontRenderer fontRenderer = getRenderer(customFontName);
		if(renderPosition.equals(TextPosition.CENTERED)){
			if(wrapWidth == 0){
				x -= fontRenderer.getStringWidth(text)/2;
			}else{
				x -= Math.min(wrapWidth/2, fontRenderer.getStringWidth(text)/2);
			}
		}else if(renderPosition.equals(TextPosition.RIGHT_ALIGNED)){
			if(wrapWidth == 0){
				x -= fontRenderer.getStringWidth(text);
			}else{
				x -= Math.min(wrapWidth, fontRenderer.getStringWidth(text));
			}
		}
		
		if(wrapWidth == 0){
			fontRenderer.drawString(text, x, y, color.rgbInt);
		}else{
			fontRenderer.drawSplitString(text, x, y, wrapWidth, color.rgbInt);
		}
	}
	
	/**
	 *  Similar to {@link #drawBasicText(String, int, int, ColorRGB, TextPosition, int)}, except this method
	 *  does OpenGL scaling to render the text bigger or smaller than normal.  Requires a few different bits
	 *  to get this to work, so it's in it's own method for code simplicity.
	 */
	public static void drawScaledText(String text, String customFontName, int x, int y, ColorRGB color, TextPosition renderPosition, int wrapWidth, float scale, boolean autoScaled){
		FontRenderer fontRenderer = getRenderer(customFontName);
		//Check for auto-scaling.
		float heightOffset = 0F;
		if(autoScaled){
			//Get the string width.  This is in text-pixels, and by default 1tp=1block.
			//We scale this to the actual pixel-width by multiplying it by the incoming scale.
			float stringWidth = scale*fontRenderer.getStringWidth(text);
			
			//If the string width in pixels is greater than the wrap width, adjust scale.
			//We also need to cancel wrapping if our scaled value is within bounds.
			float scaleFactor = stringWidth > 0 ? wrapWidth/stringWidth : 1.0F;
			if(stringWidth/scale > wrapWidth){
				if(stringWidth > wrapWidth){
					scale *= scaleFactor;
					heightOffset = scaleFactor*scale*(fontRenderer.FONT_HEIGHT - fontRenderer.FONT_HEIGHT*scaleFactor);
				}
				wrapWidth = 0;
			}
		}
		
		//Push to translate text.
		GL11.glPushMatrix();
		if(renderPosition.equals(TextPosition.CENTERED)){
			GL11.glTranslatef(x - scale*fontRenderer.getStringWidth(text)/2, y + heightOffset, 0);
		}else if(renderPosition.equals(TextPosition.RIGHT_ALIGNED)){
			GL11.glTranslatef(x - scale*fontRenderer.getStringWidth(text), y + heightOffset, 0);
		}else{
			GL11.glTranslatef(x, y + heightOffset, 0);
		}
		GL11.glScalef(scale, scale, scale);
		drawBasicText(text, customFontName, 0, 0, color, TextPosition.LEFT_ALIGNED, wrapWidth);
		GL11.glPopMatrix();
	}
	
	/**
	 *  Returns the width of the passed-in text string.
	 */
	public static int getStringWidth(String text, String fontName){
		return getRenderer(fontName).getStringWidth(text);
	}
	
	/**
	 *  Returns the formatting code for the passed-in color.
	 */
	public static String getFormattingCode(String color){
		for(TextFormatting format : TextFormatting.values()){
			if(format.name().toLowerCase().equals(color)){
				return format.toString();
			}
		}
		return "";
	}
	
	/**
	 *  Draws the specified item on the GUI at the specified scale.  Note that MC
	 *  renders all items from their top-left corner, so take this into account when
	 *  choosing where to put this component in your GUI.
	 */
	public static void drawItem(ItemStack stack, int x, int y, float scale){
		if(itemRenderer == null){
			itemRenderer = Minecraft.getMinecraft().getRenderItem();
		}
		if(scale != 1.0F){
			GL11.glPushMatrix();
			GL11.glTranslatef(x, y, 0);
			GL11.glScalef(scale, scale, scale);
			itemRenderer.renderItemAndEffectIntoGUI(stack, 0, 0);
			if(stack.getCount() > 1){
				itemRenderer.renderItemOverlays(Minecraft.getMinecraft().fontRenderer, stack, 0, 0);
			}
			GL11.glPopMatrix();
		}else{
			itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
			if(stack.getCount() > 1){
				itemRenderer.renderItemOverlays(Minecraft.getMinecraft().fontRenderer, stack, x, y);
			}
		}
	}
	
	/**
	 *  Draws the specified portion of the currently-bound texture.  Texture size needs to be
	 *  passed-in here to allow this method to translate pixels into relative texture coords.  
	 *  Draw starts at the  bottom-left point and goes counter-clockwise to the top-left point.
	 */
	public static void renderSheetTexture(int x, int y, int width, int height, float u, float v, float U, float V, int textureWidth, int textureHeight){
	 	float widthPixelPercent = 1.0F/textureWidth;
        float heightPixelPercent = 1.0F/textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, 			y + height, 0.0D).tex(u * widthPixelPercent, 	V * heightPixelPercent).endVertex();
        bufferbuilder.pos(x + width, 	y + height, 0.0D).tex(U * widthPixelPercent, 	V * heightPixelPercent).endVertex();
        bufferbuilder.pos(x + width, 	y, 			0.0D).tex(U * widthPixelPercent, 	v * heightPixelPercent).endVertex();
        bufferbuilder.pos(x, 			y, 			0.0D).tex(u * widthPixelPercent, 	v * heightPixelPercent).endVertex();
        tessellator.draw();
	}
	
	/**
	 *  Draws a colored rectangle at the specified point.  This does NOT change the currently-bound
	 *  texture, nor does it modify any OpelGL states, so it may safely be called during rendering operations.
	 */
	public static void renderRectangle(int x, int y, int width, int height, ColorRGB color){
		GuiScreen.drawRect(x, y, x + width, y + height, color.rgbInt);
	}
	
	/**
	 *  Returns the currently-active GUI, or null if no GUI is active.
	 */
	public static AGUIBase getActiveGUI(){
		return Minecraft.getMinecraft().currentScreen instanceof BuilderGUI ? ((BuilderGUI) Minecraft.getMinecraft().currentScreen).gui : null;
	}
	
	/**
	 *  Closes the currently-opened GUI, returning back to the main game.
	 */
	public static void closeGUI(){
		//Set current screen to null and clear out the OBJ DisplayLists if we have any.
		Minecraft.getMinecraft().displayGuiScreen(null);
		GUIComponent3DModel.clearModelCaches();
	}
	
	/**
	 *  Opens the passed-in GUI, replacing any opened GUI in the process.
	 */
	public static void openGUI(AGUIBase gui){
		FMLCommonHandler.instance().showGuiScreen(new BuilderGUI(gui));
	}
}
