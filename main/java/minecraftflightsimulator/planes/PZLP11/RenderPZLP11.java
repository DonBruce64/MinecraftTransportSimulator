package minecraftflightsimulator.planes.PZLP11;

import java.awt.Color;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.helpers.InstrumentHelper;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.modelrenders.RenderPlane;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderPZLP11 extends RenderPlane{
	private static final ModelPZLP11Fuselage fuselage = new ModelPZLP11Fuselage();
	private static final ModelPZLP11AileronL aileronL = new ModelPZLP11AileronL();
	private static final ModelPZLP11AileronR aileronR = new ModelPZLP11AileronR();
	private static final ModelPZLP11ElevatorL elevatorL = new ModelPZLP11ElevatorL();
	private static final ModelPZLP11ElevatorR elevatorR = new ModelPZLP11ElevatorR();
	private static final ModelPZLP11Rudder rudder = new ModelPZLP11Rudder();
	
	private static final ResourceLocation fuselageTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/fuselage.png");
	private static final ResourceLocation aileronlTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/aileronl.png");
	private static final ResourceLocation aileronrTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/aileronr.png");
	private static final ResourceLocation elevatorlTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/elevatorl.png");
	private static final ResourceLocation elevatorrTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/elevatorr.png");
	private static final ResourceLocation rudderTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/rudder.png");
    
	private static final Color letteringColor = new Color((63 << 16) + (28 << 8) + 25);
	
	public RenderPZLP11(){
		super();
	}

	@Override
	protected void renderPlane(EntityPlane plane){
		GL11.glRotatef(180, 1, 0, 0);
		RenderHelper.bindTexture(fuselageTexture);
		fuselage.render();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-3.935F, -1.125F, 0.8125F);
		GL11.glRotatef(plane.aileronAngle/10F, 1, 0, 0);
		RenderHelper.bindTexture(aileronrTexture);
		aileronR.render();
		GL11.glTranslatef(2*3.935F, 0, 0);
		GL11.glRotatef(2 * -plane.aileronAngle/10F, 1, 0, 0);
		RenderHelper.bindTexture(aileronlTexture);
		aileronL.render();
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-0.875F, -0.625F, 5.5F);
		GL11.glRotatef(plane.elevatorAngle/10F, 1, 0, 0);
		RenderHelper.bindTexture(elevatorrTexture);
		elevatorR.render();
		GL11.glTranslatef(1.685F, 0, 0);
		RenderHelper.bindTexture(elevatorlTexture);
		elevatorL.render();
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, -1.44F, 5.44F);
		GL11.glRotatef(-plane.rudderAngle/10F, 0, 1, 0);
		RenderHelper.bindTexture(rudderTexture);
		rudder.render();
		GL11.glPopMatrix();
	
		GL11.glRotatef(180, 1, 0, 0);
	}
	
	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);
		RenderHelper.renderQuad(-0.25, -0.25, 0.25, 0.25, 1.125, 0.88, 0.88, 1.125, -0.75, -0.325, -0.325, -0.75, true);
		RenderHelper.renderTriangle(-0.25, -0.25, -0.25,     1.05, 0.88, 0.88,     -0.75, -0.75, -0.45, true);
		RenderHelper.renderTriangle(0.25, 0.25, 0.25,     1.05, 0.88, 0.88,     -0.75, -0.75, -0.45, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.47F, 0.70F, -0.825F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1F, 0.00390625F*1F, 0.00390625F*1F);
		for(byte i=0; i<plane.instrumentList.size(); ++i){
			if(plane.instrumentList.get(i) != null){
				InstrumentHelper.drawInstrument(plane, (i%5)*60, i<5 ? 0 : 62, plane.instrumentList.get(i).getItemDamage(), false);
			}
		}
		GL11.glScalef(2F, 2F, 2F);
		InstrumentHelper.drawInstrument(plane, 32, 52, 15, false);
		InstrumentHelper.drawInstrument(plane, 96, 52, 16, false);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(100, 0, 1, 0);
		GL11.glDisable(GL11.GL_LIGHTING);
		RenderHelper.drawScaledStringAt(plane.ownerName, -3.75F, -0.4F, -1.1F, 1F/32F, Color.white);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
}
