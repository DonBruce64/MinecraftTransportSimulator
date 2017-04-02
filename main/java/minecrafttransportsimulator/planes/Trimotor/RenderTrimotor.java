package minecrafttransportsimulator.planes.Trimotor;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityPlane;
import minecrafttransportsimulator.rendering.AircraftInstruments;
import minecrafttransportsimulator.rendering.AircraftInstruments.AircraftControls;
import minecrafttransportsimulator.rendering.AircraftInstruments.AircraftGauges;
import minecrafttransportsimulator.rendering.partrenders.RenderPlane;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderTrimotor extends RenderPlane{
	private static final ModelTrimotor model = new ModelTrimotor();
    private static final ResourceLocation sideTexture = new ResourceLocation(MTS.MODID, "textures/planes/trimotor/side.png");
    private static final ResourceLocation rotatedSideTexture = new ResourceLocation(MTS.MODID, "textures/planes/trimotor/side_rotated.png");
    private static final ResourceLocation detailTexture = new ResourceLocation("minecraft", "textures/blocks/stone.png");
    private static final ResourceLocation logo1 = new ResourceLocation(MTS.MODID, "textures/planes/trimotor/logo1.png");
    private static final ResourceLocation logo2 = new ResourceLocation(MTS.MODID, "textures/planes/trimotor/logo2.png");
    private static final float[] offset = new float[]{0, 0, 1.5F};
    private int color;
    
	public RenderTrimotor(RenderManager manager){
		super(manager);
	}

	@Override
	protected float[] getRenderOffset(){
		return offset;
	}
	
	@Override
	protected void renderPlane(EntityPlane plane){
		GL11DrawSystem.bindTexture(sideTexture);
		model.renderRegularParts(plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F);
		GL11DrawSystem.bindTexture(rotatedSideTexture);
		model.renderRotatedTextureParts(plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F);
		GL11DrawSystem.bindTexture(detailTexture);
		color = getColorForMeta(plane.textureOptions);
		GL11.glColor3f(((color >> 16) & 255)/255F, ((color >> 8) & 255)/255F, (color & 255)/255F);
		model.renderColoredParts(plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F);
	}
	
	@Override
	protected void renderWindows(EntityPlane plane){
		GL11DrawSystem.bindTexture(GL11DrawSystem.glassTexture);
		GL11DrawSystem.renderSquare(1.15, 1.00, 0.81, 1.063, 0.04, 0.725, true);
		GL11DrawSystem.renderSquare(-1.15, -1.00, 0.81, 1.063, 0.04, 0.725, true);
		GL11DrawSystem.renderSquare(0.99, 0.89, 0.69, 1.063, 0.775, 1.24, true);
		GL11DrawSystem.renderSquare(-0.99, -0.89, 0.69, 1.063, 0.775, 1.24, true);
		GL11DrawSystem.renderSquare(0.83, 0.05, 0.69, 1.063, 1.28, 1.65, true);
		GL11DrawSystem.renderSquare(-0.83, -0.05, 0.69, 1.063, 1.28, 1.65, true);
		GL11DrawSystem.renderTriangle(0.063, 0.063, 0.72, 1.15, 1.15, 1.15, 1.6, 1.31, 1.31, true);
		GL11DrawSystem.renderTriangle(-0.063, -0.063, -0.72, 1.15, 1.15, 1.15, 1.6, 1.31, 1.31, true);
		GL11DrawSystem.renderQuad(0.5, 0.063, 0.063, 0.5, 1.2, 1.2, 1.2, 1.2, 1.25, 1.25, 0.75, 0.75, true);
		GL11DrawSystem.renderQuad(-0.5, -0.063, -0.063, -0.5, 1.2, 1.2, 1.2, 1.2, 1.25, 1.25, 0.75, 0.75, true);
		
		GL11DrawSystem.renderSquare(1.2, 1.2, 0.06, 0.44, -0.94, -0.05, true);
		GL11DrawSystem.renderSquare(-1.2, -1.2, 0.06, 0.44, -0.94, -0.05, true);
		GL11DrawSystem.renderSquare(1.2, 1.2, 0.06, 0.44, -1.94, -1.05, true);
		GL11DrawSystem.renderSquare(-1.2, -1.2, 0.06, 0.44, -1.94, -1.05, true);
		GL11DrawSystem.renderSquare(1.2, 1.2, 0.06, 0.44, -2.94, -2.05, true);
		GL11DrawSystem.renderSquare(-1.2, -1.2, 0.06, 0.44, -2.94, -2.05, true);
		GL11DrawSystem.renderSquare(1.2, 1.2, 0.06, 0.44, -3.94, -3.05, true);
		GL11DrawSystem.renderSquare(-1.2, -1.2, 0.06, 0.44, -3.94, -3.05, true);
		GL11DrawSystem.renderSquare(1.2, 1.2, 0.06, 0.44, -4.94, -4.05, true);
		GL11DrawSystem.renderSquare(-1.2, -1.2, 0.06, 0.44, -4.94, -4.05, true);
		GL11DrawSystem.renderSquare(1.2, 1.2, 0.06, 0.44, -5.94, -5.05, true);
		GL11DrawSystem.renderSquare(-1.2, -1.2, 0.06, 0.44, -5.94, -5.05, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		//Main console
		GL11.glPushMatrix();
		GL11.glTranslatef(0.68F, 0.4F, 1.25F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.3F, 0.00390625F*1.3F, 0.00390625F*1.3F);
		for(byte i=0; i<10; ++i){
			AircraftInstruments.drawFlyableInstrument(plane, (i%5)*62, i<5 ? 0 : 62, plane.instruments.get(i) != null ? plane.instruments.get(i) : -1, false, (byte) -1);
		}
		
		GL11.glPushMatrix();
		GL11.glTranslatef(35F, 25F, 0);
		GL11.glScalef(1F/1.3F, 1F/1.3F, 1F/1.3F);
		//Center engine
		for(byte i=20; i<24; ++i){
			AircraftInstruments.drawFlyableInstrument(plane, 30 + (i-20)*60, 124, plane.instruments.get(i) != null ? plane.instruments.get(i) : -1, false, (byte) 1);
		}
		GL11.glPopMatrix();
		
		AircraftInstruments.drawFlyableControl(plane, 290, -5, AircraftControls.THROTTLE, false);
		AircraftInstruments.drawFlyableControl(plane, 290, 70, AircraftControls.BRAKE, false);
		GL11.glPopMatrix();
		
		//Left engine
		GL11.glPushMatrix();
		GL11.glTranslatef(2.374F, 0.25F, -0.75F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glRotatef(-90, 0, 1, 0);
		GL11.glScalef(0.00390625F*1.0F, 0.00390625F*1.0F, 0.00390625F*1.0F);
		for(byte i=10; i<14; ++i){
			AircraftInstruments.drawFlyableInstrument(plane, 0, (i-10)*62, plane.instruments.get(i) != null ? plane.instruments.get(i) : -1, false, (byte) 0);
		}
		GL11.glPopMatrix();
		
		//Right engine
		GL11.glPushMatrix();
		GL11.glTranslatef(-2.374F, 0.25F, -0.75F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glRotatef(90, 0, 1, 0);
		GL11.glScalef(0.00390625F*1.0F, 0.00390625F*1.0F, 0.00390625F*1.0F);
		for(byte i=30; i<34; ++i){
			AircraftInstruments.drawFlyableInstrument(plane, 0, (i-30)*62, plane.instruments.get(i) != null ? plane.instruments.get(i) : -1, false, (byte) 2);
		}
		GL11.glPopMatrix();
		
		//Cabin altimeter
		GL11.glPushMatrix();
		GL11.glTranslatef(-0.75F, 0.75F, -0.07F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.25F, 0.00390625F*1.25F, 0.00390625F*1.25F);
		AircraftInstruments.drawFlyableInstrument(plane, 0, 0, (byte) AircraftGauges.ALTIMETER.ordinal(), false, (byte) -1);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glPushMatrix();
		GL11DrawSystem.bindTexture(logo1);
		GL11DrawSystem.renderSquare(0.0626, 0.0626, 0.91, 1.91, -10.8, -11.8, false);
		GL11DrawSystem.renderSquare(-0.0626, -0.0626, 0.91, 1.91, -11.8, -10.8, false);
		GL11DrawSystem.bindTexture(logo2);
		GL11DrawSystem.renderSquare(1.26, 0.697869586, 0.0, 1.0, -6.0, -11.0, false);
		GL11DrawSystem.renderSquare(-0.697869586, -1.26, 0.0, 1.0, -11.0, -6.0, false);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 2.5F, -11.625F);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(90 - plane.rudderAngle/10F, 0, 1, 0);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, -0F, -0F, -0.0313F, 0.01F, Color.black);
		GL11.glRotatef(180, 0, 1, 0);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, -0F, -0F, -0.0313F, 0.01F, Color.black);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glRotatef(-90, 1, 0, 0);
		GL11DrawSystem.drawScaledStringAt(plane.displayName, 7F, 1.2F, -1.57F, 0.1F, Color.black);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderNavigationLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(11.125F, 1.53125F, -0.5F);
		GL11.glRotatef(-90, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(1, 0, 0, brightness, 0.0625F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-11.125F, 1.53125F, -0.5F);
		GL11.glRotatef(90, 0, 0, 1);
		GL11DrawSystem.drawBulbLight(0, 1, 0, brightness, 0.0625F, 0.03125F);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 2.5625F, -11.625F);
		GL11.glRotatef(180 + plane.rudderAngle/10F, 0, 1, 0);
		GL11DrawSystem.drawBulbLight(1, 1, 1, brightness, 0.0625F, 0.03125F);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderStrobeLights(EntityPlane plane, float brightness){}
	
	@Override
	protected void renderTaxiLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(-0.5F, -1, 0.375F);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glTranslatef(1F, 0, 0);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.125F);
		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderLandingLights(EntityPlane plane, float brightness){
		GL11.glPushMatrix();
		GL11.glTranslatef(-4.75F, 1.25F, 0);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.25F);
		GL11.glTranslatef(9.5F, 0, 0);
		GL11DrawSystem.drawLight(1, 1, 1, brightness, 0.25F);
		GL11.glPopMatrix();
	}

	@Override
	public void renderTaxiBeam(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(-0.5F, -0.9F, 1.85F);
		GL11.glRotatef(57, 1, 0, 0);
		GL11DrawSystem.drawLightBeam(plane, 3, 5, 20, false);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0.5F, -0.9F, 1.85F);
		GL11.glRotatef(57, 1, 0, 0);
		GL11DrawSystem.drawLightBeam(plane, 3, 5, 20, false);
		GL11.glPopMatrix();
	}

	@Override
	public void renderLandingBeam(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(4.75F, 1.5F, 1.4F);
		GL11.glRotatef(45, 1, 0, 0);
		GL11DrawSystem.drawLightBeam(plane, 7, 15, 20, true);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-4.75F, 1.5F, 1.4F);
		GL11.glRotatef(45, 1, 0, 0);
		GL11DrawSystem.drawLightBeam(plane, 7, 15, 20, true);
		GL11.glPopMatrix();
	}
	
	private int getColorForMeta(int meta){
		switch (meta){
            case 0: return 1644825;
            case 1: return 10040115;
            case 2: return 6717235;
            case 3: return 6704179;
            case 4: return 3361970;
            case 5: return 8339378;
            case 6: return 5013401;
            case 7: return 10066329;
            case 8: return 5000268;
            case 9: return 15892389;
            case 10: return 8375321;
            case 11: return 15066419;
            case 12: return 6724056;
            case 13: return 11685080;
            case 14: return 14188339;
            case 15: return 16777215;
            default: return 0;
        }
	}
}
