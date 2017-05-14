package minecrafttransportsimulator.rendering;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**Main render class for all multipart entities.
 * Renders the parent model, and all child models that have been registered by
 * {@link registerChildRender}.  Ensures all parts are rendered in the exact
 * location they should be in as all rendering is done in the same operation.
 * Entities don't render above 255 well due to the new chunk visibility system.
 * This code is present to be called manually from
 * {@link ClientEventSystem#on(RenderWorldLastEvent)}.
 *
 * @author don_bruce
 */
public final class RenderMultipart{
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	private static final Map<String, ResourceLocation> textureArray = new HashMap<String, ResourceLocation>();
    
	public static void render(EntityMultipartMoving mover, EntityPlayer playerRendering, float partialTicks){
		if(mover.pack == null){
			return;
		}
		
		GL11.glPushMatrix();
		GlStateManager.depthFunc(515);
        minecraft.entityRenderer.enableLightmap();
        RenderHelper.enableStandardItemLighting();
		
        //TODO look into how PartialTicks affects this.
        //Bet this is why entities jerk around.
        Entity renderViewEntity = minecraft.getRenderViewEntity();
        double playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double)partialTicks;
        double playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double)partialTicks;
        double playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double)partialTicks;
        double thisX = mover.lastTickPosX + (mover.posX - mover.lastTickPosX) * (double)partialTicks;
        double thisY = mover.lastTickPosY + (mover.posY - mover.lastTickPosY) * (double)partialTicks;
        double thisZ = mover.lastTickPosZ + (mover.posZ - mover.lastTickPosZ) * (double)partialTicks;
        GL11.glTranslated(thisX - playerX, thisY - playerY, thisZ - playerZ);
        
        int i = mover.getBrightnessForRender(partialTicks);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, i % 65536, i / 65536);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
		if(mover.pack.rendering.flipModel){
			GL11.glRotatef(180, 1, 0, 0);
		}
		
		//Bind texture.  Adds new element to cache if needed.
		if(!textureArray.containsKey(mover.pack.general.name)){
			ResourceLocation textureLocation;
			if(mover.pack.rendering.useCustomModelTexture){
				textureLocation = new ResourceLocation(mover.pack.rendering.modelTexture);
			}else{
				textureLocation = new ResourceLocation(MTS.MODID, mover.pack.rendering.modelTexture);
			}
			textureArray.put(mover.pack.general.name, textureLocation);
		}
		GL11DrawSystem.bindTexture(textureArray.get(mover.pack.general.name));
		
		//TODO once we settle on a parser, the main model rendering goes here.
		//RENDER ME
		
		for(EntityMultipartChild child : mover.getChildren()){
        	if(MTSRegistryClient.childRenderMap.get(child.getClass()) != null){
        		//Render child model.
        		MTSVector childOffset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, mover.rotationPitch, mover.rotationYaw, mover.rotationRoll);
				MTSRegistryClient.childRenderMap.get(child.getClass()).render(child, childOffset.xCoord, childOffset.yCoord, childOffset.zCoord, partialTicks);
			}
        }
	
		//Render bounding boxes if applicable.
		if(minecraft.gameSettings.showDebugInfo){
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glColor3f(0.0F, 0.0F, 0.0F);
			GL11.glLineWidth(3.0F);
			for(Entity entity : minecraft.theWorld.loadedEntityList){
				if(entity instanceof EntityMultipartChild){
					EntityMultipartChild child = (EntityMultipartChild) entity;
					if(child.parent != null){
						if(child.parent.UUID.equals(mover.UUID)){
							GL11.glPushMatrix();
							MTSVector childOffset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, mover.rotationPitch, mover.rotationYaw, mover.rotationRoll);
							GL11.glTranslated(childOffset.xCoord, childOffset.yCoord, childOffset.zCoord);
							renderBoundingBox(child.getEntityBoundingBox().offset(-child.posX, -child.posY, -child.posZ));
							GL11.glPopMatrix();
						}
					}
				}
			}
			GL11.glLineWidth(1.0F);
			GL11.glColor3f(1.0F, 1.0F, 1.0F);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glPopMatrix();
		}
		
        RenderHelper.disableStandardItemLighting();
        minecraft.entityRenderer.disableLightmap();
        GL11.glPopMatrix();
	}
	
	private static void renderBoundingBox(AxisAlignedBB box){
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(box.minX, box.minY, box.minZ);
		GL11.glVertex3d(box.maxX, box.minY, box.minZ);
		GL11.glVertex3d(box.minX, box.minY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
		GL11.glVertex3d(box.minX, box.maxY, box.minZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
		GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
		
		GL11.glVertex3d(box.minX, box.minY, box.minZ);
		GL11.glVertex3d(box.minX, box.minY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.minY, box.minZ);
		GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
		GL11.glVertex3d(box.minX, box.maxY, box.minZ);
		GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
		
		GL11.glVertex3d(box.minX, box.minY, box.minZ);
		GL11.glVertex3d(box.minX, box.maxY, box.minZ);
		GL11.glVertex3d(box.maxX, box.minY, box.minZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
		GL11.glVertex3d(box.minX, box.minY, box.maxZ);
		GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
		GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
		GL11.glEnd();
	}
	
	/*
	private void renderLightBeams(EntityPlane plane){
    	GL11.glPushMatrix();
    	Minecraft.getMinecraft().entityRenderer.disableLightmap();
    	if(MinecraftForgeClient.getRenderPass() == -1){
    		GL11.glTranslated(-renderOffset[0], -renderOffset[1], -renderOffset[2]);
        }
    	if((plane.lightStatus & 4) == 4){
        	renderTaxiBeam(plane);
        }
    	if((plane.lightStatus & 8) == 8){
    		renderLandingBeam(plane);
    	}
    	Minecraft.getMinecraft().entityRenderer.enableLightmap();
    	GL11.glPopMatrix();
	}
	
	protected abstract float[] getRenderOffset();
	protected abstract void renderPlane(EntityPlane plane);
	protected abstract void renderWindows(EntityPlane plane);
	protected abstract void renderConsole(EntityPlane plane);
	protected abstract void renderMarkings(EntityPlane plane);
	protected abstract void renderNavigationLights(EntityPlane plane, float brightness);
	protected abstract void renderStrobeLights(EntityPlane plane, float brightness);
	protected abstract void renderTaxiLights(EntityPlane plane, float brightness);
	protected abstract void renderLandingLights(EntityPlane plane, float brightness);
	protected abstract void renderTaxiBeam(EntityPlane plane);
	protected abstract void renderLandingBeam(EntityPlane plane);
		*/
	
	/**
	 * Binds the specified texture.  Used for MTS textures.  Cached for efficiency.
	 */
	
	private void renderDebugVectors(EntityPlane plane){
		double[] debugForces = plane.getDebugForces();
    	GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glLineWidth(1);
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.headingVec.xCoord*debugForces[0], 2 + plane.headingVec.yCoord*debugForces[0],  plane.headingVec.zCoord*debugForces[0]);
		
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)), 2, Math.sin(Math.toRadians(plane.rotationYaw)));
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);

		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)), 2, -Math.sin(Math.toRadians(plane.rotationYaw)));
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glEnd();
		
		GL11.glLineWidth(5);
		GL11.glColor4f(1, 0, 0, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(plane.headingVec.xCoord*debugForces[0], 2 + plane.headingVec.yCoord*debugForces[0],  plane.headingVec.zCoord*debugForces[0]);
		GL11.glVertex3d(plane.headingVec.xCoord*(debugForces[0] - debugForces[1]), 2 + plane.headingVec.yCoord*(debugForces[0] - debugForces[1]),  plane.headingVec.zCoord*(debugForces[0] - debugForces[1]));
		
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glVertex3d(Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*(debugForces[2] - debugForces[3])/10, 2 + plane.verticalVec.yCoord*(debugForces[2] - debugForces[3])/10,  Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*(debugForces[2] - debugForces[3])/10);
		
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*debugForces[2]/10, 2 + plane.verticalVec.yCoord*debugForces[2]/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*debugForces[2]/10);
		GL11.glVertex3d(-Math.cos(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.xCoord*(debugForces[2] - debugForces[3])/10, 2 + plane.verticalVec.yCoord*(debugForces[2] - debugForces[3])/10,  -Math.sin(Math.toRadians(plane.rotationYaw)) + plane.verticalVec.zCoord*(debugForces[2] - debugForces[3])/10);
		GL11.glEnd();
		
		GL11.glColor4f(0, 0, 1, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.velocityVec.xCoord*plane.velocity, 2 + plane.velocityVec.yCoord*plane.velocity,  plane.velocityVec.zCoord*plane.velocity);
		GL11.glEnd();
		
		GL11.glColor4f(0, 1, 0, 1);
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 0);
		GL11.glVertex3d(plane.headingVec.xCoord, 2 + plane.headingVec.yCoord,  plane.headingVec.zCoord);
		GL11.glEnd();
				
		GL11.glLineWidth(1);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
	}
}