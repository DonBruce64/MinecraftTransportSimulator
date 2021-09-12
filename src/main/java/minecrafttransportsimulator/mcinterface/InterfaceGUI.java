package minecrafttransportsimulator.mcinterface;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponent3DModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Interface for MC GUI classes.  Allows access to various GUI-specific functions.
*
* @author don_bruce
*/
public class InterfaceGUI{
	private static RenderItem itemRenderer;
	
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
		//Need to pack in a 255 alpha value or rectangles are invisible.
		GuiScreen.drawRect(x, y, x + width, y + height, color.rgbInt | -16777216);
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
