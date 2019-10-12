package minecrafttransportsimulator.guis;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.mcinterface.MTSGui;
import minecrafttransportsimulator.mcinterface.MTSMetronome;
import minecrafttransportsimulator.mcinterface.MTSRenderer;
import net.minecraft.client.resources.I18n;

public class GUIPackMissing extends MTSGui{
	private int guiTextureLeft;
	private int guiTextureTop;
	
	@Override
	protected void handleInit(int width, int height){
		guiTextureLeft = (width - 256)/2;
		guiTextureTop = (height - 192)/2;
	}
	
	@Override
	protected void handleDraw(int mouseX, int mouseY){
		GL11.glColor3f(1, 1, 1);
		renderBackground();
		MTSRenderer.bindTexture(GUI_STANDARD_TEXTURE);
		renderTexturedQuad(guiTextureLeft, guiTextureTop, 0, 0, 256, 192, 256, 192);

		
		if(MTSMetronome.isOnHalf(40)){
			GL11.glPushMatrix();
			GL11.glTranslatef(guiTextureLeft + 90 - MTSRenderer.getTextWidth(I18n.format("gui.packmissing.title"))/2F, guiTextureTop + 10, 0);
			GL11.glScalef(3, 3, 3);
			MTSRenderer.drawText(I18n.format("gui.packmissing.title"), 0, 0, Color.RED, false, false);
			GL11.glPopMatrix();
		}
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiTextureLeft + 10, guiTextureTop + 40, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		MTSRenderer.drawEssay(I18n.format("gui.packmissing.reason"), 0, 0, Color.BLACK, 320);
		
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiTextureLeft + 10, guiTextureTop + 65, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		MTSRenderer.drawEssay(I18n.format("gui.packmissing.nomod"), 0, 0, Color.BLACK, 320);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiTextureLeft + 10, guiTextureTop + 90, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		MTSRenderer.drawEssay(I18n.format("gui.packmissing.modlink"), 0, 0, Color.BLACK, 320);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiTextureLeft + 10, guiTextureTop + 115, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		MTSRenderer.drawEssay(I18n.format("gui.packmissing.misplaced"), 0, 0, Color.BLACK, 320);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(guiTextureLeft + 10, guiTextureTop + 150, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		MTSRenderer.drawEssay(I18n.format("gui.packmissing.versionerror"), 0, 0, Color.BLACK, 320);
		GL11.glPopMatrix();
	}

	@Override
	protected void handleMouseClick(int mouseX, int mouseY){}

	@Override
	protected void handleButtonClick(MTSButton buttonClicked){}

	@Override
	protected void handleKeyTyped(char key, int code){}

	@Override
	public boolean pauses(){
		return true;
	}
}