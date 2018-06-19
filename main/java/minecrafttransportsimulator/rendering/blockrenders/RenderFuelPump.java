package minecrafttransportsimulator.rendering.blockrenders;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityFuelPump;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

public class RenderFuelPump extends TileEntitySpecialRenderer<TileEntityFuelPump>{
	
	public RenderFuelPump(){}
	
	@Override
	public void renderTileEntityAt(TileEntityFuelPump pump, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(pump, x, y, z, partialTicks, destroyStage);
		final Vec3i facingVec = EnumFacing.VALUES[pump.rotation].getDirectionVec();
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslatef(0.5F, 0F, 0.5F);
		
		if(facingVec.getX() == 1){
			GL11.glRotatef(90, 0, 1, 0);
		}else if(facingVec.getX() == -1){
			GL11.glRotatef(270, 0, 1, 0);
		}else if(facingVec.getZ() == -1){
			GL11.glRotatef(180, 0, 1, 0);
		}
		
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
