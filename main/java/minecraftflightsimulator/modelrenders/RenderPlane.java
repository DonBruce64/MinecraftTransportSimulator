package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.utilities.InstrumentHelper;
import minecraftflightsimulator.utilities.RenderHelper;
import minecraftflightsimulator.utilities.RenderHelper.RenderParent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public abstract class RenderPlane extends RenderParent{
    protected static final ResourceLocation windowTexture = new ResourceLocation("minecraft", "textures/blocks/glass.png");

    public RenderPlane(RenderManager manager){
    	super(manager);
    }
    
	@Override
	public void renderParentModel(EntityParent parent){
		EntityPlane plane=(EntityPlane) parent;
		InstrumentHelper.updateAircraftEngineProperties(plane);
		GL11.glPushMatrix();
		GL11.glRotatef(-plane.rotationYaw, 0, 1, 0);
		GL11.glRotatef(plane.rotationPitch, 1, 0, 0);
		GL11.glRotatef(plane.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        renderPlane(plane);
        renderWindows(plane);
        renderConsole(plane);
        renderMarkings(plane);
        renderStrobeLightCovers(plane);
        if(plane.lightsOn){
        	renderStrobeLights(plane);
        }
        GL11.glPopMatrix();   
        if(Minecraft.getMinecraft().gameSettings.showDebugInfo){
        	renderDebugVectors(plane);
        }
	}
	
	protected void drawStrobeLightCover(float x, float y, float z, float rotation){
		RenderHelper.bindTexture(windowTexture);
		GL11.glPushMatrix();
		GL11.glColor3f(1, 1, 1);
		GL11.glTranslatef(x, y, z);
		GL11.glRotatef(rotation, 0, 1, 0);
		RenderHelper.renderSquare(-0.0625, 0.0625, 0, 0.125, 0.0002, 0.0002, false);
		GL11.glPopMatrix();
	}
	
	protected void drawStrobeLight(EntityPlane plane, float x, float y, float z, float rotation, float red, float green, float blue){
		if(plane.lightsOn && plane.ticksExisted%20 < 3 && plane.electricPower > 2){
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glTranslatef(x, y, z);
			GL11.glRotatef(rotation, 0, 1, 0);
			GL11.glColor4f(red, green, blue, (float) plane.electricPower/12F);
			Minecraft.getMinecraft().entityRenderer.disableLightmap(0);
			RenderHelper.renderSquare(-0.0625, 0.0625, 0, 0.125, 0.0001, 0.0001, false);	
			Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
			GL11.glColor4f(1, 1, 1, 1);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glPopMatrix();
		}
	}
	
	protected abstract void renderPlane(EntityPlane plane);
	protected abstract void renderWindows(EntityPlane plane);
	protected abstract void renderConsole(EntityPlane plane);
	protected abstract void renderMarkings(EntityPlane plane);
	protected abstract void renderStrobeLightCovers(EntityPlane plane);
	protected abstract void renderStrobeLights(EntityPlane plane);
	public abstract void renderLights(EntityPlane plane);
	
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