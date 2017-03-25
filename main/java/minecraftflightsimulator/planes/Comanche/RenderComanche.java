package minecraftflightsimulator.planes.Comanche;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.rendering.partrenders.RenderPlane;
import minecraftflightsimulator.systems.GL11DrawSystem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderComanche extends RenderPlane{
	private static final ModelComanche model = new ModelComanche();
	private static final ResourceLocation[] exteriorTextures = getExteriorTextures();

	public RenderComanche(RenderManager manager){
		super(manager);
	}
	
	@Override
	protected float[] getRenderOffset(){
		return noOffset;
	}
	
	@Override
	protected void renderPlane(EntityPlane plane){
		GL11.glTranslatef(0, 0F, 0F);
		GL11.glRotatef(180, 1, 0, 0);
		GL11DrawSystem.bindTexture(exteriorTextures[plane.textureOptions > exteriorTextures.length ? 0 : plane.textureOptions]);
        model.renderFuselage();
        model.renderAilerons(plane.aileronAngle/10F * 0.017453292F);
        model.renderElevators(plane.elevatorAngle/10F * 0.017453292F);
        model.renderRudder(-plane.rudderAngle/10F * 0.017453292F);
        model.renderFlaps(plane.flapAngle/10F * 0.017453292F);
        GL11.glRotatef(180, 1, 0, 0);
	}

	@Override
	protected void renderWindows(EntityPlane plane){
		GL11DrawSystem.bindTexture(GL11DrawSystem.glassTexture);
		GL11DrawSystem.renderQuad(-0.85, -0.85, 0.85, 0.85, 1.875, 1.1875, 1.1875, 1.875, 0.625, 1.25, 1.25, 0.625, true);
		GL11DrawSystem.renderTriangle(0.85, 0.85, 0.85, 1.875, 1.1875, 1.1875, 0.625, 0.625, 1.25, true);
		GL11DrawSystem.renderTriangle(-0.85, -0.85, -0.85, 1.875, 1.1875, 1.1875, 0.625, 0.625, 1.25, true);
		GL11DrawSystem.renderSquare(0.85, 0.85, 1.1875, 1.875, -0.625, 0.5625, true);
		GL11DrawSystem.renderSquare(-0.85, -0.85, 1.1875, 1.875, -0.625, 0.5625, true);
		GL11DrawSystem.renderSquare(0.85, 0.85, 1.1875, 1.875, -1.8125, -0.6875, true);
		GL11DrawSystem.renderSquare(-0.85, -0.85, 1.1875, 1.875, -1.8125, -0.6875, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();

		GL11.glPopMatrix();
	}


	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glPushMatrix();

		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderNavigationLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(7.245F, 0.522F, 0.5625F);
		GL11.glRotatef(-87, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 0, 0, brightness, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-7.245F, 0.522F, 0.5625F);
		GL11.glRotatef(87, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(0, 1, 0, brightness, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 3.375F, -5.25F);
		GL11.glRotatef(180 + plane.rudderAngle/10F, 0, 1, 0);
		GL11.glTranslatef(0, 0, 0.3125F);
		GL11DrawSystem.drawBulbLight(1, 1, 1, brightness, 0.125F, 0.03125F);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderStrobeLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(7.245F, 0.522F, 0.4375F);
		GL11.glRotatef(-87, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 1, 1, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-7.245F, 0.522F, 0.4375F);
		GL11.glRotatef(87, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 1, 1, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 3.375F, -5.0F);
		GL11DrawSystem.drawBulbLight(1, 0, 0, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderTaxiLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 0.3125F, 3.375F);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderLandingLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(7F, 0.445F, 0.6875F);
		GL11.glRotatef(3, 0, 0, 1);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-7F, 0.445F, 0.6875F);
		GL11.glRotatef(-3, 0, 0, 1);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glPopMatrix();
	}

	@Override
	public void renderTaxiBeam(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 0.375F, 3.375F);
		GL11.glRotatef(55, 1, 0, 0);
		GL11DrawSystem.drawLightBeam(plane, 5, 5, 20, false);
		GL11.glPopMatrix();
	}

	@Override
	public void renderLandingBeam(EntityPlane plane) {
		GL11.glPushMatrix();
		GL11.glTranslatef(7F, 0.5075F, 0.625F);
		GL11.glRotatef(-25, 0, 1, 0);
		GL11.glRotatef(31, 1, 0, 0);
		GL11DrawSystem.drawLightBeam(plane, 7, 15, 20, false);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-7F, 0.5075F, 0.625F);
		GL11.glRotatef(25, 0, 1, 0);
		GL11.glRotatef(31, 1, 0, 0);
		GL11DrawSystem.drawLightBeam(plane, 7, 15, 20, false);
		GL11.glPopMatrix();
	}
	
	private static ResourceLocation[] getExteriorTextures(){
		ResourceLocation[] texArray = new ResourceLocation[7];
		for(byte i=0; i<4; ++i){
			texArray[i] = new ResourceLocation("mfs", "textures/planes/comanche/fuselage" + i + ".png");
		}
		return texArray;
	}
}
