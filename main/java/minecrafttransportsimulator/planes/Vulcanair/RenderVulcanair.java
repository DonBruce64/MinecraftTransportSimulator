package minecrafttransportsimulator.planes.Vulcanair;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityPlane;
import minecrafttransportsimulator.rendering.AircraftInstruments;
import minecrafttransportsimulator.rendering.AircraftInstruments.AircraftControls;
import minecrafttransportsimulator.rendering.partrenders.RenderPlane;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderVulcanair extends RenderPlane{
	private static final ModelVulcanair model = new ModelVulcanair();
	private static final ResourceLocation[] exteriorTextures = getExteriorTextures();
	private static final float[] offset = new float[]{0, 0.75F, 0.12F};

	public RenderVulcanair(RenderManager manager){
		super(manager);
	}
	
	@Override
	protected float[] getRenderOffset(){
		return offset;
	}
	
	@Override
	protected void renderPlane(EntityPlane plane){
		GL11.glRotatef(180, 1, 0, 0);
		GL11DrawSystem.bindTexture(exteriorTextures[plane.textureOptions > exteriorTextures.length ? 0 : plane.textureOptions]);
        model.renderFuselage();
        model.renderAilerons(plane.aileronAngle/10F * 0.017453292F);
        model.renderElevators(plane.elevatorAngle/10F * 0.017453292F);
        model.renderRudder(-plane.rudderAngle/10F * 0.017453292F);
        model.renderFlaps(plane.flapAngle/10F * 0.017453292F);
        model.renderInterior();
        GL11.glTranslatef(0, -0.75F, -0.12F);
        GL11.glRotatef(180, 1, 0, 0);
	}

	@Override
	protected void renderWindows(EntityPlane plane){
		GL11DrawSystem.bindTexture(GL11DrawSystem.glassTexture);		
		GL11DrawSystem.renderQuad(-0.95, -0.95, 0.95, 0.95, 0.08, -0.45, -0.45, 0.08, 2.12, 2.9, 2.9, 2.12, true);		
		GL11DrawSystem.renderQuadUVCustom(-0.95, -0.95, -0.95, -0.95, 0.08, 0.08, -0.58, -0.58, 1.69, 2.02, 3, 1.69, 0, 0.25, 1, 0, 0, 0, 1, 1, false);
		GL11DrawSystem.renderQuadUVCustom(-0.95, -0.95, -0.95, -0.95, 0.08, 0.08, -0.58, -0.58, 2.02, 1.69, 1.69, 3, 0.75, 1, 1, 0, 0, 0, 1, 1, false);
		GL11DrawSystem.renderQuadUVCustom(0.95, 0.95, 0.95, 0.95, 0.08, 0.08, -0.58, -0.58, 1.69, 2.02, 3, 1.69, 0, 0.25, 1, 0, 0, 0, 1, 1, false);
		GL11DrawSystem.renderQuadUVCustom(0.95, 0.95, 0.95, 0.95, 0.08, 0.08, -0.58, -0.58, 2.02, 1.69, 1.69, 3, 0.75, 1, 1, 0, 0, 0, 1, 1, false);
		GL11DrawSystem.renderSquare(-0.95, -0.95, -0.58, 0.08, -0.12, 1.135, true);
		GL11DrawSystem.renderSquare(0.95, 0.95, -0.58, 0.08, -0.12, 1.135, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.7F, -0.6F, 2.8F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.0F, 0.00390625F*1.0F, 0.00390625F*1.0F);
		for(byte i=0; i<10; ++i){
			AircraftInstruments.drawFlyableInstrument(plane, (i%5)*62, i<5 ? 0 : 62, plane.instruments.get(i) != null ? plane.instruments.get(i) : -1, false, (byte) -1);
		}
		GL11.glPushMatrix();
		GL11.glTranslatef(310F, -10F, 0);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		for(byte j=0; j<2; ++j){
			for(byte i=0; i<4; ++i){
				AircraftInstruments.drawFlyableInstrument(plane, 62 + (i%2)*62, (i/2)*62 + j*124, plane.instruments.get((byte) (i+10+j*10)) != null ? plane.instruments.get((byte) (i+10+j*10)) : -1, false, j);
			}
		}
		GL11.glPopMatrix();
		AircraftInstruments.drawFlyableControl(plane, 300, -15, AircraftControls.THROTTLE, false);
		AircraftInstruments.drawFlyableControl(plane, 300, 60, AircraftControls.BRAKE, false);
		AircraftInstruments.drawFlyableControl(plane, 300, 20, AircraftControls.FLAPS, false);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(102, 0, 1, 0);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glScalef(1.5F, 1.5F, 1.5F);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, -2.2F/1.5F, 0.2F/1.5F, -1.25F/1.5F, 1F/32F, Color.black);
		GL11.glRotatef(156, 0, 1, 0);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, 2.2F/1.5F, 0.2F/1.5F, -1.25F/1.5F, 1F/32F, Color.black);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderNavigationLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(7.25F, 0.40625F, 0.25F);
		GL11.glRotatef(-90, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 0, 0, brightness, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-7.25F, 0.40625F, 0.25F);
		GL11.glRotatef(90, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(0, 1, 0, brightness, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 2.25F, -5.245F);
		GL11.glRotatef(180 + plane.rudderAngle/10F, 0, 1, 0);
		GL11.glTranslatef(0, 0, 0.3175F);
		GL11DrawSystem.drawBulbLight(1, 1, 1, brightness, 0.125F, 0.03125F);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderStrobeLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(7.25F, 0.40625F, 0.125F);
		GL11.glRotatef(-90, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 1, 1, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-7.25F, 0.40625F, 0.125F);
		GL11.glRotatef(90, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 1, 1, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 2.25F, -4.75F);
		GL11DrawSystem.drawBulbLight(1, 0, 0, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderTaxiLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(4.375F, 0.1875F, 0.88F);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderLandingLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(4.5F, 0.1875F, 0.88F);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glPopMatrix();
	}
	
	@Override
	public void renderTaxiBeam(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(4.40625F, 1.8125F, 1.0625F);
		GL11.glRotatef(30, 1, 0, 0);
		GL11.glRotatef(-25, 0, 1, 0);
		GL11DrawSystem.drawLightBeam(plane, 2.25, 10, 20, false);
		GL11.glPopMatrix();
	}

	@Override
	public void renderLandingBeam(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(4.53125F, 1.8125F, 1.0625F);
		GL11.glRotatef(30, 1, 0, 0);
		GL11.glRotatef(-18, 0, 1, 0);
		GL11DrawSystem.drawLightBeam(plane, 5, 15, 20, true);
		GL11.glPopMatrix();
	}
	
	private static ResourceLocation[] getExteriorTextures(){
		ResourceLocation[] texArray = new ResourceLocation[7];
		for(byte i=0; i<7; ++i){
			texArray[i] = new ResourceLocation(MTS.MODID, "textures/planes/vulcanair/fuselage" + i + ".png");
		}
		return texArray;
	}
}