package minecraftflightsimulator.planes.MC172;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.rendering.AircraftInstruments;
import minecraftflightsimulator.rendering.AircraftInstruments.AircraftControls;
import minecraftflightsimulator.rendering.partrenders.RenderPlane;
import minecraftflightsimulator.systems.GL11DrawSystem;
import net.minecraft.client.renderer.entity.RenderManager;

public class RenderMC172 extends RenderPlane{
	private static final ModelMC172 model = new ModelMC172();

	public RenderMC172(RenderManager manager){
		super(manager);
	}
	
	@Override
	protected float[] getRenderOffset(){
		return noOffset;
	}
	
	@Override
	protected void renderPlane(EntityPlane plane){
		GL11DrawSystem.bindTexture(plane.getBackplateTexture());
        model.renderPlane();
        model.renderAilerons(plane.aileronAngle/10F * 0.017453292F);
        model.renderElevators(plane.elevatorAngle/10F * 0.017453292F);
        model.renderRudder(plane.rudderAngle/10F * 0.017453292F);
        model.renderFlaps(plane.flapAngle/10F * 0.017453292F);
	}
	
	@Override
	protected void renderWindows(EntityPlane plane){
		GL11DrawSystem.bindTexture(GL11DrawSystem.glassTexture);
		GL11DrawSystem.renderQuad(-0.75, -0.75, 0.75, 0.75, 1.625, 0.625, 0.625, 1.625, 0.875, 1.75, 1.75, 0.875, true);
		GL11DrawSystem.renderTriangle(-0.75, -0.75, -0.75, 1.625, 0.625, 0.625, 0.875, 0.875, 1.75, true);
		GL11DrawSystem.renderTriangle(0.75, 0.75, 0.75, 1.625, 0.625, 0.625, 0.875, 0.875, 1.75, true);
		GL11DrawSystem.renderSquare(0.85, 0.85, 0.625, 1.625, -0.25, 0.625, true);
		GL11DrawSystem.renderSquare(-0.85, -0.85, 0.625, 1.625, -0.25, 0.625, true);
		GL11DrawSystem.renderTriangle(-0.85, -0.85, -0.7, 1.6, 0.625, 0.625, -0.5, -0.5, -1.95, true);
		GL11DrawSystem.renderTriangle(0.85, 0.85, 0.7, 1.6, 0.625, 0.625, -0.5, -0.5, -1.95, true);
		GL11DrawSystem.renderQuad(-0.8, -0.525, 0.525, 0.8, 1.625, 0.625, 0.625, 1.625, -0.5, -2.1, -2.1, -0.5, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.92F, 0.35F, 0.715F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.5F, 0.00390625F*1.5F, 0.00390625F*1.5F);
		for(byte i=0; i<10; ++i){
			if(i==0 || i==5){
				GL11.glPushMatrix();
				GL11.glRotatef(-90, 0, 1, 0);
				GL11.glTranslatef(-80, 0, -30);
				GL11.glScalef(0.75F, 0.75F, 0.75F);
				AircraftInstruments.drawFlyableInstrument(plane, 72 + (i%5)*62, i<5 ? -10 : 52, plane.instruments.get(i) != null ? plane.instruments.get(i) : -1, false, (byte) -1);
				GL11.glPopMatrix();
			}else if(i==4 || i==9){
				GL11.glPushMatrix();
				GL11.glScalef(0.75F, 0.75F, 0.75F);
				AircraftInstruments.drawFlyableInstrument(plane, 72 + (i%5)*62, i<5 ? -10 : 52, plane.instruments.get(i) != null ? plane.instruments.get(i) : -1, false, (byte) -1);
				GL11.glPopMatrix();
			}else{
				AircraftInstruments.drawFlyableInstrument(plane, (i%5)*62, i<5 ? 0 : 62, plane.instruments.get(i) != null ? plane.instruments.get(i) : -1, false, (byte) -1);
			}
		}
		AircraftInstruments.drawFlyableControl(plane, 272, -5, AircraftControls.THROTTLE, false);
		AircraftInstruments.drawFlyableControl(plane, 272, 60, AircraftControls.BRAKE, false);
		AircraftInstruments.drawFlyableControl(plane, 232, 80, AircraftControls.FLAPS, false);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(105, 0, 1, 0);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, -2.8F, -0.35F, -1.36F, 1F/32F, Color.lightGray);
		GL11.glRotatef(150, 0, 1, 0);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, 2.8F, -0.35F, -1.36F, 1F/32F, Color.lightGray);
		GL11.glRotatef(105, 0, 1, 0);
		GL11.glRotatef(-180, 1, 0, 0);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	@Override
	protected void renderNavigationLights(EntityPlane plane, float brightness){
			GL11.glPushMatrix();
			GL11.glTranslatef(4.5F, 1.6875F, 0.8125F);
			GL11.glRotatef(-90, 0, 0, 1);
			GL11DrawSystem.drawBulbLight(1, 0, 0, brightness, 0.125F, 0.03125F);
			GL11.glPopMatrix();
			
			GL11.glPushMatrix();
			GL11.glTranslatef(-4.5F, 1.6875F, 0.8125F);
			GL11.glRotatef(90, 0, 0, 1);
			GL11DrawSystem.drawBulbLight(0, 1, 0, brightness, 0.125F, 0.03125F);
			GL11.glPopMatrix();
			
			GL11.glPushMatrix();
			GL11.glTranslatef(0F, 2F, -4.25F);
			GL11.glRotatef(180 + plane.rudderAngle/10F, 0, 1, 0);
			GL11.glTranslatef(0, 0, 0.4375F);
			GL11DrawSystem.drawBulbLight(1, 1, 1, brightness, 0.125F, 0.03125F);
			GL11.glPopMatrix();
	}

	@Override
	protected void renderStrobeLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(4.5F, 1.6875F, 0.6875F);
		GL11.glRotatef(-90, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 1, 1, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-4.5F, 1.6875F, 0.6875F);
		GL11.glRotatef(90, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 1, 1, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 2F, -3.8125F);
		GL11DrawSystem.drawBulbLight(1, 0, 0, plane.ticksExisted%20 < 3 ? brightness : 0, 0.125F, 0.03125F);
		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderTaxiLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(2.375F, 1.625F, 0.875F);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderLandingLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(2.5F, 1.625F, 0.875F);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glPopMatrix();
	}
	
	@Override
	public void renderTaxiBeam(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(2.375F, 1.75F, 0.8125F);
		GL11.glRotatef(40, 1, 0, 0);
		GL11.glRotatef(-20, 0, 1, 0);
		GL11DrawSystem.drawLightBeam(plane, 2, 7, 20, false);
		GL11.glPopMatrix();
	}

	@Override
	public void renderLandingBeam(EntityPlane plane) {
		GL11.glPushMatrix();
		GL11.glTranslatef(2.5F, 1.75F, 0.8125F);
		GL11.glRotatef(45, 1, 0, 0);
		GL11.glRotatef(-15, 0, 1, 0);
		GL11DrawSystem.drawLightBeam(plane, 5, 9, 20, true);
		GL11.glPopMatrix();
	}
}
