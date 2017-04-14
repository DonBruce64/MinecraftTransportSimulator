package minecrafttransportsimulator.planes.PZLP11;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.rendering.AircraftInstruments;
import minecrafttransportsimulator.rendering.AircraftInstruments.AircraftControls;
import minecrafttransportsimulator.rendering.partrenders.RenderPlane;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderPZLP11 extends RenderPlane{
	private static final ModelPZLP11Fuselage fuselage = new ModelPZLP11Fuselage();
	private static final ModelPZLP11AileronL aileronL = new ModelPZLP11AileronL();
	private static final ModelPZLP11AileronR aileronR = new ModelPZLP11AileronR();
	private static final ModelPZLP11ElevatorL elevatorL = new ModelPZLP11ElevatorL();
	private static final ModelPZLP11ElevatorR elevatorR = new ModelPZLP11ElevatorR();
	private static final ModelPZLP11Rudder rudder = new ModelPZLP11Rudder();
	
	private static final ResourceLocation fuselageTexture = new ResourceLocation(MTS.MODID, "textures/planes/pzlp11/fuselage.png");
	private static final ResourceLocation aileronlTexture = new ResourceLocation(MTS.MODID, "textures/planes/pzlp11/aileronl.png");
	private static final ResourceLocation aileronrTexture = new ResourceLocation(MTS.MODID, "textures/planes/pzlp11/aileronr.png");
	private static final ResourceLocation elevatorlTexture = new ResourceLocation(MTS.MODID, "textures/planes/pzlp11/elevatorl.png");
	private static final ResourceLocation elevatorrTexture = new ResourceLocation(MTS.MODID, "textures/planes/pzlp11/elevatorr.png");
	private static final ResourceLocation rudderTexture = new ResourceLocation(MTS.MODID, "textures/planes/pzlp11/rudder.png");
    	
	public RenderPZLP11(RenderManager manager){
		super(manager);
	}
	
	@Override
	protected float[] getRenderOffset(){
		return noOffset;
	}

	@Override
	protected void renderPlane(EntityPlane plane){
		GL11.glRotatef(180, 1, 0, 0);
		GL11DrawSystem.bindTexture(fuselageTexture);
		fuselage.render();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-3.935F, -1.125F, 0.8125F);
		GL11.glRotatef(plane.aileronAngle/10F, 1, 0, 0);
		GL11DrawSystem.bindTexture(aileronrTexture);
		aileronR.render();
		GL11.glTranslatef(2*3.935F, 0, 0);
		GL11.glRotatef(2 * -plane.aileronAngle/10F, 1, 0, 0);
		GL11DrawSystem.bindTexture(aileronlTexture);
		aileronL.render();
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-0.875F, -0.625F, 5.5F);
		GL11.glRotatef(plane.elevatorAngle/10F, 1, 0, 0);
		GL11DrawSystem.bindTexture(elevatorrTexture);
		elevatorR.render();
		GL11.glTranslatef(1.685F, 0, 0);
		GL11DrawSystem.bindTexture(elevatorlTexture);
		elevatorL.render();
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, -1.44F, 5.44F);
		GL11.glRotatef(-plane.rudderAngle/10F, 0, 1, 0);
		GL11DrawSystem.bindTexture(rudderTexture);
		rudder.render();
		GL11.glPopMatrix();
	
		GL11.glRotatef(180, 1, 0, 0);
	}
	
	@Override
	protected void renderWindows(EntityPlane plane){
		GL11DrawSystem.bindTexture(GL11DrawSystem.glassTexture);
		GL11DrawSystem.renderQuad(-0.25, -0.25, 0.25, 0.25, 1.125, 0.88, 0.88, 1.125, -0.75, -0.325, -0.325, -0.75, true);
		GL11DrawSystem.renderTriangle(-0.25, -0.25, -0.25,     1.05, 0.88, 0.88,     -0.75, -0.75, -0.45, true);
		GL11DrawSystem.renderTriangle(0.25, 0.25, 0.25,     1.05, 0.88, 0.88,     -0.75, -0.75, -0.45, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.47F, 0.70F, -0.825F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1F, 0.00390625F*1F, 0.00390625F*1F);
		for(byte i=0; i<10; ++i){
			if(plane.instruments.get(i) != null){
				AircraftInstruments.drawFlyableInstrument(plane, (i%5)*60, i<5 ? 0 : 62, plane.instruments.get(i), false, (byte) -1);
			}
		}
		

		GL11.glPushMatrix();
		GL11.glTranslatef(75F, 115F, 0);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		for(byte i=10; i<14; ++i){
			AircraftInstruments.drawFlyableInstrument(plane, (i%4)*62, 0, plane.instruments.get(i), false, (byte) 0);
		}
		GL11.glPopMatrix();

		AircraftInstruments.drawFlyableControl(plane, 44, 112, AircraftControls.THROTTLE, false);
		AircraftInstruments.drawFlyableControl(plane, 196, 112, AircraftControls.BRAKE, false);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(99, 0, 1, 0);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, -2.8F, -0.4F, -1.03F, 1F/32F, Color.lightGray);
		GL11.glRotatef(162, 0, 1, 0);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, 2.8F, -0.4F, -1.03F, 1F/32F, Color.lightGray);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
	
	@Override
	protected void renderNavigationLights(EntityPlane plane, float brightness){}

	@Override
	protected void renderStrobeLights(EntityPlane plane, float brightness){}
	
	@Override
	protected void renderTaxiLights(EntityPlane plane, float brightness){}
	
	@Override
	protected void renderLandingLights(EntityPlane plane, float brightness){}

	@Override
	public void renderTaxiBeam(EntityPlane plane){}

	@Override
	public void renderLandingBeam(EntityPlane plane){}
}
