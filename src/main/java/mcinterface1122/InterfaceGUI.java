package mcinterface1122;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.guis.components.GUIComponentOBJModel;
import minecrafttransportsimulator.mcinterface.IInterfaceGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
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

class InterfaceGUI implements IInterfaceGUI{
	private static FontRenderer fontRenderer;
	private static RenderItem itemRenderer;
	
	@Override
	public void drawItemTooltip(AGUIBase gui, int mouseX, int mouseY, ItemStack stack){
		List<String> tooltipText = stack.getTooltip(Minecraft.getMinecraft().player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        for(int i = 0; i < tooltipText.size(); ++i){
            if(i == 0){
                tooltipText.set(i, tooltipText.get(i));
            }else{
                tooltipText.set(i, TextFormatting.GRAY + tooltipText.get(i));
            }
        }
		GuiUtils.drawHoveringText(stack, tooltipText, mouseX, mouseY, Minecraft.getMinecraft().currentScreen.width, Minecraft.getMinecraft().currentScreen.height, -1, fontRenderer);
	}
	
	@Override
	public void drawGenericTooltip(AGUIBase gui, int mouseX, int mouseY, String tooltip){
		GuiUtils.drawHoveringText(Arrays.asList(new String[]{tooltip}), mouseX, mouseY, Minecraft.getMinecraft().currentScreen.width, Minecraft.getMinecraft().currentScreen.height, -1, fontRenderer);
	}
	
	@Override
	public void drawBasicText(String text, int x, int y, Color color, TextPosition renderPosition, int wrapWidth){
		if(fontRenderer == null){
			fontRenderer = Minecraft.getMinecraft().fontRenderer;
		}
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
			fontRenderer.drawString(text, x, y, color.getRGB());
		}else{
			fontRenderer.drawSplitString(text, x, y, wrapWidth, color.getRGB());
		}
	}
	
	@Override
	public void drawScaledText(String text, int x, int y, Color color, TextPosition renderPosition, int wrapWidth, float scale, boolean autoScaled){
		//Get font renderer, if we don't have it yet.
		if(fontRenderer == null){
			fontRenderer = Minecraft.getMinecraft().fontRenderer;
		}
		
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
		drawBasicText(text, 0, 0, color, TextPosition.LEFT_ALIGNED, wrapWidth);
		GL11.glPopMatrix();
	}
	
	@Override
	public int getStringWidth(String text){
		return fontRenderer.getStringWidth(text);
	}
	
	@Override
	public String getFormattingCode(String color){
		for(TextFormatting format : TextFormatting.values()){
			if(format.name().toLowerCase().equals(color)){
				return format.toString();
			}
		}
		return "";
	}
	
	@Override
	public void drawItem(ItemStack stack, int x, int y, float scale){
		if(itemRenderer == null){
			itemRenderer = Minecraft.getMinecraft().getRenderItem();
		}
		if(scale != 1.0F){
			GL11.glPushMatrix();
			GL11.glTranslatef(x, y, 0);
			GL11.glScalef(scale, scale, scale);
			itemRenderer.renderItemAndEffectIntoGUI(stack, 0, 0);
			if(stack.getCount() > 1){
				itemRenderer.renderItemOverlays(fontRenderer, stack, 0, 0);
			}
			GL11.glPopMatrix();
		}else{
			itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
			if(stack.getCount() > 1){
				itemRenderer.renderItemOverlays(fontRenderer, stack, x, y);
			}
		}
	}
	
	@Override
	public void renderSheetTexture(int x, int y, int width, int height, float u, float v, float U, float V, int textureWidth, int textureHeight){
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
	
	@Override
	public void renderRectangle(int x, int y, int width, int height, Color color){
		GuiScreen.drawRect(x, y, x + width, y + height, color.getRGB());
	}
	
	@Override
	public boolean isGUIActive(Class<? extends AGUIBase> guiClass){
		if(guiClass == null){
			return Minecraft.getMinecraft().currentScreen == null;
		}else{
			return Minecraft.getMinecraft().currentScreen == null ? false : (Minecraft.getMinecraft().currentScreen instanceof BuilderGUI ? ((BuilderGUI) Minecraft.getMinecraft().currentScreen).gui.getClass().equals(guiClass) : false);
		}
	}
	
	@Override
	public void closeGUI(){
		//Set current screen to null and clear out the OBJ DisplayLists if we have any.
		Minecraft.getMinecraft().displayGuiScreen(null);
		GUIComponentOBJModel.clearDisplayListCaches();
	}
	
	@Override
	public void openGUI(AGUIBase gui){
		FMLCommonHandler.instance().showGuiScreen(new BuilderGUI(gui));
	}
}
