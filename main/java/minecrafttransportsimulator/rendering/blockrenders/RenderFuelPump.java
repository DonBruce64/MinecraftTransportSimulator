package minecrafttransportsimulator.rendering.blockrenders;

import java.awt.Color;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityFuelPump;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderFuelPump extends TileEntitySpecialRenderer{
	//private static final ResourceLocation texture = new ResourceLocation(MTS.MODID, "textures/blockmodels/fuelpump.png");
	private static final ResourceLocation texture = new ResourceLocation("minecraft", "textures/blocks/quartz_block_top.png");
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static int displayListIndex = -1;
	
	private TileEntityFuelPump pump;

	public RenderFuelPump(){}
	
	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(tile, x, y, z, partialTicks, destroyStage);
		this.pump = (TileEntityFuelPump) tile;
		
		if(displayListIndex == -1){
			displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : MTSRegistryClient.modelMap.get("fuelpump").entrySet()){
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
		bindTexture(texture);
		GL11.glCallList(displayListIndex);
		
		bindTexture(vanillaGlassTexture);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3f(-0.4375F, 1.25F, 0.3125F);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3f(-0.4375F, 0.6875F, 0.3125F);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3f(0.4375F, 0.6875F, 0.3125F);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3f(0.4375F, 1.25F, 0.3125F);
		GL11.glEnd();
		
		GL11.glScalef(0.015625F, 0.015625F, 0.015625F);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glTranslatef(0, 0, -16.1F);
		GL11.glDisable(GL11.GL_LIGHTING);
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
		getFontRenderer().drawString(MTS.MODID.toUpperCase(), -this.getFontRenderer().getStringWidth(MTS.MODID.toUpperCase())/2, -28, Color.BLACK.getRGB());
		if(pump.getFluid() != null){
			getFontRenderer().drawString(pump.getFluid().getLocalizedName().toUpperCase(), -this.getFontRenderer().getStringWidth(pump.getFluid().getLocalizedName().toUpperCase())/2, -73, Color.ORANGE.getRGB());
			GL11.glScalef(0.5F, 0.5F, 0.5F);
			if(pump.getFluid().amount < 10000){
				getFontRenderer().drawString("0" + String.valueOf(pump.getFluid().amount) + "mb", -15, -120, Color.ORANGE.getRGB());
			}else{
				getFontRenderer().drawString(String.valueOf(pump.getFluid().amount) + "mb", -15, -120, Color.ORANGE.getRGB());
			}
		}else{
			GL11.glScalef(0.5F, 0.5F, 0.5F);
			getFontRenderer().drawString("00000mb", -15, -120, Color.ORANGE.getRGB());
		}

		String zeros = pump.totalTransfered < 10 ? "0000" : pump.totalTransfered < 100 ? "000" : pump.totalTransfered < 1000 ? "00" : pump.totalTransfered < 1000 ? "0" : "";
		getFontRenderer().drawString(zeros + String.valueOf(pump.totalTransfered) + "mb", -15, -110, Color.ORANGE.getRGB());
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}
}
