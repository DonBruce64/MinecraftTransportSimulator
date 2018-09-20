package minecrafttransportsimulator.rendering.blockrenders;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.decor.BlockDecor6AxisOriented;
import minecrafttransportsimulator.blocks.decor.TileEntityDecor6AxisOriented;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;

public class RenderDecor extends TileEntitySpecialRenderer<TileEntityDecor6AxisOriented>{
	private static final ResourceLocation vanillaGlassTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");
	private static final ResourceLocation lensFlareTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lensflare.png");
	private static final ResourceLocation lightTexture = new ResourceLocation(MTS.MODID, "textures/rendering/light.png");
	private static final ResourceLocation lightBeamTexture = new ResourceLocation(MTS.MODID, "textures/rendering/lightbeam.png");
		
	public RenderDecor(){}
	
	@Override
	public void renderTileEntityAt(TileEntityDecor6AxisOriented decor, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(decor, x, y, z, partialTicks, destroyStage);
		final Vec3i facingVec = EnumFacing.VALUES[decor.rotation].getDirectionVec();
		final BlockDecor6AxisOriented decorBlock = (BlockDecor6AxisOriented) decor.getWorld().getBlockState(decor.getPos()).getBlock();
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
		
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
		GL11.glDisable(GL11.GL_LIGHTING);
		if(decorBlock.equals(MTSRegistry.trafficSignal)){
			renderTrafficSignal(decor, facingVec, lightBrightness);
		}else if(decorBlock.equals(MTSRegistry.streetLight)){
			renderStreetLight(decor, facingVec, lightBrightness);
		}
		GL11.glEnable(GL11.GL_LIGHTING);
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glPopMatrix();
	}
	
	private void renderLightedSquare(float lightSize, float lightBrightness, Color lightColor){
		final float flareSize = lightSize*4F;
		bindTexture(lightTexture);
		GL11.glColor3f(lightColor.getRed()/255F, lightColor.getGreen()/255F, lightColor.getBlue()/255F);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3f(-lightSize/2F, -lightSize/2F, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3f(lightSize/2F, -lightSize/2F, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3f(lightSize/2F, lightSize/2F, 0);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3f(-lightSize/2F, lightSize/2F, 0);
		GL11.glEnd();
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		bindTexture(lensFlareTexture);
		GL11.glColor4f(lightColor.getRed()/255F, lightColor.getGreen()/255F, lightColor.getBlue()/255F, lightBrightness);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(1, 0);
		GL11.glVertex3f(-flareSize/2F, -flareSize/2F, 0);
		GL11.glTexCoord2f(1, 1);
		GL11.glVertex3f(flareSize/2F, -flareSize/2F, 0);
		GL11.glTexCoord2f(0, 1);
		GL11.glVertex3f(flareSize/2F, flareSize/2F, 0);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3f(-flareSize/2F, flareSize/2F, 0);
		GL11.glEnd();
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void renderTrafficSignal(TileEntityDecor6AxisOriented decor, Vec3i facingVec, float lightBrightness){
		//Render the lights for the traffic signal.  What lights we render depends on the world time.
		final float lightYPos;
		final Color lightColor;
		//Green is 1px up, Yellow is 6px, Red is 11px.
		if(decor.getWorld().getTotalWorldTime()%440 < 140){
			//NS Green, EW Red.
			lightYPos = facingVec.getZ() == 0 ? 3/16F : 13/16F;
			lightColor = facingVec.getZ() == 0 ? Color.GREEN : Color.RED;
		}else if(decor.getWorld().getTotalWorldTime()%440 < 200){
			//NS Yellow, EW Red.
			lightYPos = facingVec.getZ() == 0 ? 8/16F : 13/16F;
			lightColor = facingVec.getZ() == 0 ? Color.YELLOW : Color.RED;
		}else if(decor.getWorld().getTotalWorldTime()%440 < 220){
			//NS Red, EW Red.
			lightYPos = facingVec.getZ() == 0 ? 13/16F : 13/16F;
			lightColor = facingVec.getZ() == 0 ? Color.RED : Color.RED;
		}else if(decor.getWorld().getTotalWorldTime()%440 < 360){
			//NS Red, EW Green.
			lightYPos = facingVec.getZ() == 0 ? 13/16F : 3/16F;
			lightColor = facingVec.getZ() == 0 ? Color.RED : Color.GREEN;
		}else if(decor.getWorld().getTotalWorldTime()%440 < 420){
			//NS Red, EW Yellow.
			lightYPos = facingVec.getZ() == 0 ? 13/16F : 8/16F;
			lightColor = facingVec.getZ() == 0 ? Color.RED : Color.YELLOW;
		}else{
			//NS Red, EW Red.
			lightYPos = facingVec.getZ() == 0 ? 13/16F : 13/16F;
			lightColor = facingVec.getZ() == 0 ? Color.RED : Color.RED;
		}
		
		GL11.glTranslatef(0, lightYPos, 0.225F);
		renderLightedSquare(4F/16F, lightBrightness, lightColor);
	}

	private void renderStreetLight(TileEntityDecor6AxisOriented decor, Vec3i facingVec, float lightBrightness){
		//Render light square
		GL11.glTranslatef(0, 6.45F/16F, 6F/16F);
		GL11.glRotatef(90, 1, 0, 0);
		renderLightedSquare(4F/16F, lightBrightness,  Color.WHITE);
		
		//Render light beam
		GL11.glPushMatrix();
    	GL11.glDisable(GL11.GL_LIGHTING);
    	GL11.glEnable(GL11.GL_BLEND);
		bindTexture(lightBeamTexture);
    	GL11.glColor4f(1, 1, 1, Math.min(1.0F, lightBrightness/2F));
    	//Allows making things brighter by using alpha blending.
    	GL11.glDepthMask(false);
    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, -0.15F);
		Vec3d endpointVec = new Vec3d(0, 0, 6);
		GL11.glDepthMask(false);
		for(byte j=0; j<=2; ++j){
			drawLightCone(endpointVec, false);
    	}
		drawLightCone(endpointVec, true);
		GL11.glPopMatrix();
		
    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    	GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
	}
	
	private static void drawLightCone(Vec3d endPoint, boolean reverse){
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glTexCoord2f(0, 0);
		GL11.glVertex3d(0, 0, 0);
		if(reverse){
			for(float theta=0; theta < 2*Math.PI + 0.1; theta += 2F*Math.PI/40F){
				GL11.glTexCoord2f(theta, 1);
				GL11.glVertex3d(endPoint.xCoord + 3.0F*Math.cos(theta), endPoint.yCoord + 3.0F*Math.sin(theta), endPoint.zCoord);
			}
		}else{
			for(float theta=(float) (2*Math.PI); theta>=0 - 0.1; theta -= 2F*Math.PI/40F){
				GL11.glTexCoord2f(theta, 1);
				GL11.glVertex3d(endPoint.xCoord + 3.0F*Math.cos(theta), endPoint.yCoord + 3.0F*Math.sin(theta), endPoint.zCoord);
			}
		}
		GL11.glEnd();
	}
}
