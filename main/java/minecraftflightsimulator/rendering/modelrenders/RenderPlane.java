package minecraftflightsimulator.rendering.modelrenders;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.systems.RenderSystem.RenderParent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.MinecraftForgeClient;

public abstract class RenderPlane extends RenderParent{
	private float[] renderOffset;
	protected static final float[] noOffset = new float[]{0, 0, 0};
	
    public RenderPlane(RenderManager manager){
    	super(manager);
    }
    
	@Override
	public void renderParentModel(EntityParent parent, float partialTicks){
		EntityPlane plane=(EntityPlane) parent;
		GL11.glPushMatrix();
		GL11.glRotatef(-plane.rotationYaw, 0, 1, 0);
		GL11.glRotatef(plane.rotationPitch, 1, 0, 0);
		GL11.glRotatef(plane.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        renderOffset = getRenderOffset();
        if(MinecraftForgeClient.getRenderPass() <= 0){
        	GL11.glTranslated(renderOffset[0], renderOffset[1], renderOffset[2]);
	        renderPlane(plane);
	        GL11.glColor4f(1, 1, 1, 1);
	        renderWindows(plane);
	        renderConsole(plane);
	        renderMarkings(plane);
	        renderNavigationLights(plane, plane.getLightBrightness((byte) 1));
	        renderStrobeLights(plane, plane.getLightBrightness((byte) 2));
	        renderTaxiLights(plane, plane.getLightBrightness((byte) 4));
	        renderLandingLights(plane, plane.getLightBrightness((byte) 8));
	        if(Minecraft.getMinecraft().gameSettings.showDebugInfo){
	        	renderDebugVectors(plane);
	        }
        }
        if(MinecraftForgeClient.getRenderPass() != 0){
        	renderLightBeams(plane);
        }
        GL11.glPopMatrix();
	}
	
	private void renderLightBeams(EntityPlane plane){
    	GL11.glPushMatrix();
    	Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
    	if(MinecraftForgeClient.getRenderPass() == -1){
    		GL11.glTranslated(-renderOffset[0], -renderOffset[1], -renderOffset[2]);
        }
    	if((plane.lightStatus & 4) == 4){
        	renderTaxiBeam(plane);
        }
    	if((plane.lightStatus & 8) == 8){
    		renderLandingBeam(plane);
    	}
    	Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
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