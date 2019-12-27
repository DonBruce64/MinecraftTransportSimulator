package minecrafttransportsimulator.guis;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.resources.I18n;

public class GUIPackMissing extends GUIBase{	
	private int guiLeft;
	private int guiTop;
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 256)/2;
		guiTop = (this.height - 192)/2;
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		GL11.glColor3f(1, 1, 1);
		this.drawDefaultBackground();
		mc.getTextureManager().bindTexture(standardTexture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 192);
		
		if(mc.world.getTotalWorldTime()%40 > 20){
			GL11.glPushMatrix();
			GL11.glTranslatef(guiLeft + 90 - fontRenderer.getStringWidth(I18n.format("gui.packmissing.title"))/2F, guiTop + 10, 0);
			GL11.glScalef(3, 3, 3);
			fontRenderer.drawString(I18n.format("gui.packmissing.title"), 0, 0, Color.RED.getRGB());
			GL11.glPopMatrix();
		}
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 10, guiTop + 40, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		fontRenderer.drawSplitString(I18n.format("gui.packmissing.reason"),0, 0, 320, Color.BLACK.getRGB());
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 10, guiTop + 65, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		fontRenderer.drawSplitString(I18n.format("gui.packmissing.nomod"),0, 0, 320, Color.BLACK.getRGB());
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 10, guiTop + 90, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		fontRenderer.drawSplitString(I18n.format("gui.packmissing.modlink"),0, 0, 320, Color.BLACK.getRGB());
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 10, guiTop + 115, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		fontRenderer.drawSplitString(I18n.format("gui.packmissing.misplaced"),0, 0, 320, Color.BLACK.getRGB());
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 10, guiTop + 150, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		fontRenderer.drawSplitString(I18n.format("gui.packmissing.versionerror"),0, 0, 320, Color.BLACK.getRGB());
		GL11.glPopMatrix();
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
}