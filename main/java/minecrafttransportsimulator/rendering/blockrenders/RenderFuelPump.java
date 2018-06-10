package minecrafttransportsimulator.rendering.blockrenders;

import java.awt.Color;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityFuelPump;
import minecrafttransportsimulator.systems.OBJParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class RenderFuelPump extends TileEntitySpecialRenderer<TileEntityFuelPump>{
	private static final ResourceLocation texture = new ResourceLocation(MTS.MODID, "textures/blocks/fuelpump.png");
	private static int displayListIndex = -1;
	
	public RenderFuelPump(){}
	
	@Override
	public void renderTileEntityAt(TileEntityFuelPump pump, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(pump, x, y, z, partialTicks, destroyStage);
		
		if(displayListIndex == -1){
			displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : OBJParserSystem.parseOBJModel(new ResourceLocation(MTS.MODID, "objmodels/fuelpump.obj")).entrySet()){
				for(Float[] vertex : entry.getValue()){
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
				}
			}
			GL11.glEnd();
			GL11.glEndList();
		}

		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslatef(0.5F, 0F, 0.5F);
		GL11.glRotatef(180 - 45*pump.rotation, 0, 1, 0);
		GL11.glColor3f(1.0F, 1.0F, 1.0F);
		bindTexture(texture);
		GL11.glCallList(displayListIndex);
		
		GL11.glPushMatrix();
		GL11.glScalef(0.0625F, 0.0625F, 0.0625F);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glTranslatef(0, -8, -4.001F);
		GL11.glScalef(0.25F, 0.25F, 0.25F);
		getFontRenderer().drawString(MTS.MODID.toUpperCase(), -this.getFontRenderer().getStringWidth(MTS.MODID.toUpperCase())/2, 0, Color.BLACK.getRGB());
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glScalef(0.0625F, 0.0625F, 0.0625F);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glTranslatef(0, -17.25F, -4.001F);
		GL11.glScalef(0.125F, 0.125F, 0.125F);
		GL11.glDisable(GL11.GL_LIGHTING);
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
		
		getFontRenderer().drawString(I18n.format("tile.fuelpump.level"), -40, 10, Color.ORANGE.getRGB());
		getFontRenderer().drawString(I18n.format("tile.fuelpump.dispensed"), -40, 20, Color.ORANGE.getRGB());
		if(pump.getFluid() != null){
			String zeros = pump.getFluid().amount < 10 ? "0000" : pump.getFluid().amount < 100 ? "000" : pump.getFluid().amount < 1000 ? "00" : pump.getFluid().amount < 10000 ? "0" : "";
			getFontRenderer().drawString(pump.getFluid().getLocalizedName().toUpperCase(), -this.getFontRenderer().getStringWidth(pump.getFluid().getLocalizedName().toUpperCase())/2, 0, Color.ORANGE.getRGB());
			getFontRenderer().drawString(zeros + String.valueOf(pump.getFluid().amount) + "mb", 0, 10, Color.ORANGE.getRGB());
		}else{
			getFontRenderer().drawString("00000mb", 0, 10, Color.ORANGE.getRGB());
		}
		String zeros = pump.totalTransfered < 10 ? "0000" : pump.totalTransfered < 100 ? "000" : pump.totalTransfered < 1000 ? "00" : pump.totalTransfered < 10000 ? "0" : "";
		getFontRenderer().drawString(zeros + String.valueOf(pump.totalTransfered) + "mb", 0, 20, Color.ORANGE.getRGB());
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
		
		GL11.glPopMatrix();
	}
}
