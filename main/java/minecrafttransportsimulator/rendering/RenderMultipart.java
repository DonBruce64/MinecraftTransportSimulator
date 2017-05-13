package minecrafttransportsimulator.rendering;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.GL11DrawSystem;
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
	//private float[] renderOffset;
	//protected static final float[] noOffset = new float[]{0, 0, 0};
    
	public static void render(EntityMultipartParent parent, float partialTicks){
		GL11.glPushMatrix();
		GL11.glRotatef(-parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11DrawSystem.renderSquare(-1, -1, 0, 1, -1, 1, true);
        GL11DrawSystem.renderSquare(1, 1, 0, 1, -1, 1, true);
        GL11DrawSystem.renderSquare(-1, 1, 0, 1, -1, -1, true);
        GL11DrawSystem.renderSquare(-1, 1, 0, 1, 1, 1, true);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
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