package minecrafttransportsimulator.rendering.blockrenders;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.decor.TileEntityDecor6AxisOriented;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;

public class RenderDecor extends TileEntitySpecialRenderer<TileEntityDecor6AxisOriented>{
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static final ResourceLocation lensFlareTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lensflare.png");
	private static final ResourceLocation lightTexture = new ResourceLocation(MTS.MODID, "textures/rendering/light.png");
		
	public RenderDecor(){}
	
	@Override
	public void renderTileEntityAt(TileEntityDecor6AxisOriented decor, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(decor, x, y, z, partialTicks, destroyStage);
		Vec3i facingVec = EnumFacing.VALUES[decor.rotation].getDirectionVec();
		final float sunLight = decor.getWorld().getSunBrightness(0)*decor.getWorld().getLightBrightness(decor.getPos());
		final float blockLight = decor.getWorld().getLightFromNeighborsFor(EnumSkyBlock.BLOCK, decor.getPos())/15F;
		final float lightBrightness = (float) Math.min((1 - Math.max(sunLight, blockLight)), 1);
		
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
		
		GL11.glTranslatef(0, 0, 0.225F);
		
		if(decor.getWorld().getBlockState(decor.getPos()).getBlock().equals(MTSRegistry.trafficSignal)){
			//Render the lights for the traffic signal.  What lights we render depends on the world time.
			final float lightSize = 4F/16F;
			final float flareSize = 16F/16F;
			final float lightYPos;
			final Color lightColor;
			//Green is 1px up, Yellow is 6px, Red is 11px.
			if(decor.getWorld().getTotalWorldTime()%440 < 140){
				//NS Green, EW Red.
				lightYPos = facingVec.getZ() == 0 ? 1/16F : 11/16F;
				lightColor = facingVec.getZ() == 0 ? Color.GREEN : Color.RED;
			}else if(decor.getWorld().getTotalWorldTime()%440 < 200){
				//NS Yellow, EW Red.
				lightYPos = facingVec.getZ() == 0 ? 6/16F : 11/16F;
				lightColor = facingVec.getZ() == 0 ? Color.YELLOW : Color.RED;
			}else if(decor.getWorld().getTotalWorldTime()%440 < 220){
				//NS Red, EW Red.
				lightYPos = facingVec.getZ() == 0 ? 11/16F : 11/16F;
				lightColor = facingVec.getZ() == 0 ? Color.RED : Color.RED;
			}else if(decor.getWorld().getTotalWorldTime()%440 < 360){
				//NS Red, EW Green.
				lightYPos = facingVec.getZ() == 0 ? 11/16F : 1/16F;
				lightColor = facingVec.getZ() == 0 ? Color.RED : Color.GREEN;
			}else if(decor.getWorld().getTotalWorldTime()%440 < 420){
				//NS Red, EW Yellow.
				lightYPos = facingVec.getZ() == 0 ? 11/16F : 6/16F;
				lightColor = facingVec.getZ() == 0 ? Color.RED : Color.YELLOW;
			}else{
				//NS Red, EW Red.
				lightYPos = facingVec.getZ() == 0 ? 11/16F : 11/16F;
				lightColor = facingVec.getZ() == 0 ? Color.RED : Color.RED;
			}
						
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_NORMALIZE);
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
			
			bindTexture(lightTexture);
			GL11.glColor3f(lightColor.getRed()/255F, lightColor.getGreen()/255F, lightColor.getBlue()/255F);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glTexCoord2f(1, 0);
			GL11.glVertex3f(-lightSize/2F, lightYPos, 0);
			GL11.glTexCoord2f(1, 1);
			GL11.glVertex3f(lightSize/2F, lightYPos, 0);
			GL11.glTexCoord2f(0, 1);
			GL11.glVertex3f(lightSize/2F, lightYPos + lightSize, 0);
			GL11.glTexCoord2f(0, 0);
			GL11.glVertex3f(-lightSize/2F, lightYPos + lightSize, 0);
			GL11.glEnd();
			
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			bindTexture(lensFlareTexture);
			GL11.glColor4f(lightColor.getRed()/255F, lightColor.getGreen()/255F, lightColor.getBlue()/255F, lightBrightness);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glTexCoord2f(1, 0);
			GL11.glVertex3f(-flareSize/2F, lightYPos + lightSize/2F - flareSize/2F, 0);
			GL11.glTexCoord2f(1, 1);
			GL11.glVertex3f(flareSize/2F, lightYPos + lightSize/2F - flareSize/2F, 0);
			GL11.glTexCoord2f(0, 1);
			GL11.glVertex3f(flareSize/2F, lightYPos + lightSize/2F + flareSize/2F, 0);
			GL11.glTexCoord2f(0, 0);
			GL11.glVertex3f(-flareSize/2F, lightYPos + lightSize/2F + flareSize/2F, 0);
			GL11.glEnd();
			GL11.glDisable(GL11.GL_BLEND);
			
			GL11.glColor3f(1, 1, 1);
			GL11.glDisable(GL11.GL_NORMALIZE);
			GL11.glEnable(GL11.GL_LIGHTING);
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}
		GL11.glPopMatrix();
	}
}
