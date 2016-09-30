package minecraftflightsimulator.planes.Trimotor;

import java.awt.Color;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.modelrenders.RenderPlane;
import minecraftflightsimulator.utilities.InstrumentHelper;
import minecraftflightsimulator.utilities.InstrumentHelper.AircraftControls;
import minecraftflightsimulator.utilities.InstrumentHelper.AircraftGauges;
import minecraftflightsimulator.utilities.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderTrimotor extends RenderPlane{
	private static final ModelTrimotor model = new ModelTrimotor();
    private static final ResourceLocation sideTexture = new ResourceLocation("mfs", "textures/planes/trimotor/side.png");
    private static final ResourceLocation rotatedSideTexture = new ResourceLocation("mfs", "textures/planes/trimotor/side_rotated.png");
    private static final ResourceLocation detailTexture = new ResourceLocation("minecraft", "textures/blocks/stone.png");
    private static final ResourceLocation logo1 = new ResourceLocation("mfs", "textures/planes/trimotor/logo1.png");
    private static final ResourceLocation logo2 = new ResourceLocation("mfs", "textures/planes/trimotor/logo2.png");
    private int color;
    
	public RenderTrimotor(RenderManager manager){
		super(manager);
	}

	@Override
	protected void renderPlane(EntityPlane plane){		
		GL11.glTranslatef(0, 0, 1);
		RenderHelper.bindTexture(sideTexture);
		model.renderRegularParts(plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F);
		RenderHelper.bindTexture(rotatedSideTexture);
		model.renderRotatedTextureParts(plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F);
		RenderHelper.bindTexture(detailTexture);
		color = getColorForMeta(plane.textureOptions);
		GL11.glColor3f(((color >> 16) & 255)/255F, ((color >> 8) & 255)/255F, (color & 255)/255F);
		model.renderColoredParts(plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F);
		GL11.glColor3f(1, 1, 1);
	}
	
	@Override
	protected void renderLightCovers(EntityPlane plane){
		//Landing light cover2.
		GL11.glPushMatrix();
		GL11.glColor3f(1, 1, 1);
		RenderHelper.bindTexture(windowTexture);
		GL11.glTranslatef(4.75F, 1.25F, 0.0F);
		RenderHelper.renderSquare(-0.125, 0.125, 0, 0.25, 0.002, 0.002, false);
		GL11.glTranslatef(-9.5F, 0, 0);
		RenderHelper.renderSquare(-0.125, 0.125, 0, 0.25, 0.002, 0.002, false);
		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderStrobeLights(EntityPlane plane){}
	
	@Override
	public void renderLights(EntityPlane plane){
		if(plane.lightsOn && plane.auxLightsOn  && plane.electricPower > 2){
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glColor4f(1, 1, 1, (float) plane.electricPower/12F);
			GL11.glTranslatef(4.75F, 1.25F, 1.0F);
			RenderHelper.renderSquare(-0.125, 0.125, 0, 0.25, 0.001, 0.001, false);
			GL11.glTranslatef(-9.5F, 0, 0);
			RenderHelper.renderSquare(-0.125, 0.125, 0, 0.25, 0.001, 0.001, false);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glPopMatrix();
			
			GL11.glPushMatrix();
			GL11.glTranslatef(4.75F, 1.5F, 0.9F);
			GL11.glRotatef(45, 1, 0, 0);
			RenderHelper.drawLightBeam(plane, 7, 15, 20);
			GL11.glPopMatrix();
			
			GL11.glPushMatrix();
			GL11.glTranslatef(-4.75F, 1.5F, 0.9F);
			GL11.glRotatef(45, 1, 0, 0);
			RenderHelper.drawLightBeam(plane, 7, 15, 20);
			GL11.glPopMatrix();
		}
	}
	
	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);
		RenderHelper.renderSquare(1.15, 1.00, 0.81, 1.063, 0.04, 0.725, true);
		RenderHelper.renderSquare(-1.15, -1.00, 0.81, 1.063, 0.04, 0.725, true);
		RenderHelper.renderSquare(0.99, 0.89, 0.69, 1.063, 0.775, 1.24, true);
		RenderHelper.renderSquare(-0.99, -0.89, 0.69, 1.063, 0.775, 1.24, true);
		RenderHelper.renderSquare(0.83, 0.05, 0.69, 1.063, 1.28, 1.65, true);
		RenderHelper.renderSquare(-0.83, -0.05, 0.69, 1.063, 1.28, 1.65, true);
		RenderHelper.renderTriangle(0.063, 0.063, 0.72, 1.15, 1.15, 1.15, 1.6, 1.31, 1.31, true);
		RenderHelper.renderTriangle(-0.063, -0.063, -0.72, 1.15, 1.15, 1.15, 1.6, 1.31, 1.31, true);
		RenderHelper.renderQuad(0.5, 0.063, 0.063, 0.5, 1.2, 1.2, 1.2, 1.2, 1.25, 1.25, 0.75, 0.75, true);
		RenderHelper.renderQuad(-0.5, -0.063, -0.063, -0.5, 1.2, 1.2, 1.2, 1.2, 1.25, 1.25, 0.75, 0.75, true);
		
		RenderHelper.renderSquare(1.2, 1.2, 0.06, 0.44, -0.94, -0.05, true);
		RenderHelper.renderSquare(-1.2, -1.2, 0.06, 0.44, -0.94, -0.05, true);
		RenderHelper.renderSquare(1.2, 1.2, 0.06, 0.44, -1.94, -1.05, true);
		RenderHelper.renderSquare(-1.2, -1.2, 0.06, 0.44, -1.94, -1.05, true);
		RenderHelper.renderSquare(1.2, 1.2, 0.06, 0.44, -2.94, -2.05, true);
		RenderHelper.renderSquare(-1.2, -1.2, 0.06, 0.44, -2.94, -2.05, true);
		RenderHelper.renderSquare(1.2, 1.2, 0.06, 0.44, -3.94, -3.05, true);
		RenderHelper.renderSquare(-1.2, -1.2, 0.06, 0.44, -3.94, -3.05, true);
		RenderHelper.renderSquare(1.2, 1.2, 0.06, 0.44, -4.94, -4.05, true);
		RenderHelper.renderSquare(-1.2, -1.2, 0.06, 0.44, -4.94, -4.05, true);
		RenderHelper.renderSquare(1.2, 1.2, 0.06, 0.44, -5.94, -5.05, true);
		RenderHelper.renderSquare(-1.2, -1.2, 0.06, 0.44, -5.94, -5.05, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.68F, 0.4F, 1.25F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.3F, 0.00390625F*1.3F, 0.00390625F*1.3F);
		for(byte i=0; i<plane.instrumentList.size(); ++i){
			if(plane.instrumentList.get(i) != null){
				InstrumentHelper.drawFlyableInstrument(plane, (i%5)*62, i<5 ? 0 : 62, AircraftGauges.values()[plane.instrumentList.get(i).getItemDamage()], false);
			}
		}
		InstrumentHelper.drawFlyableControl(plane, 290, -5, AircraftControls.THROTTLE, false);
		InstrumentHelper.drawFlyableControl(plane, 290, 70, AircraftControls.BRAKE, false);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		RenderHelper.bindTexture(logo1);
		RenderHelper.renderSquare(0.0626, 0.0626, 0.91, 1.91, -10.8, -11.8, false);
		RenderHelper.renderSquare(-0.0626, -0.0626, 0.91, 1.91, -11.8, -10.8, false);
		RenderHelper.bindTexture(logo2);
		RenderHelper.renderSquare(1.26, 0.697869586, 0.0, 1.0, -6.0, -11.0, false);
		RenderHelper.renderSquare(-0.697869586, -1.26, 0.0, 1.0, -11.0, -6.0, false);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 2.5F, -11.625F);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(90 - plane.rudderAngle/10F, 0, 1, 0);
		RenderHelper.drawScaledStringAt(plane.displayName, -0F, -0F, -0.0313F, 0.01F, Color.black);
		GL11.glRotatef(180, 0, 1, 0);
		RenderHelper.drawScaledStringAt(plane.displayName, -0F, -0F, -0.0313F, 0.01F, Color.black);
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glRotatef(-90, 1, 0, 0);
		RenderHelper.drawScaledStringAt(plane.displayName, 7F, 1.2F, -1.57F, 0.1F, Color.black);
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
