package minecraftflightsimulator.planes.Vulcanair;

import java.awt.Color;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.modelrenders.RenderPlane;
import minecraftflightsimulator.utilities.InstrumentHelper;
import minecraftflightsimulator.utilities.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderVulcanair extends RenderPlane{
	private static final ModelVulcanair model = new ModelVulcanair();
	private static final ResourceLocation[] exteriorTextures = getExteriorTextures();

	public RenderVulcanair(RenderManager manager){
		super(manager);
	}
	
	@Override
	protected void renderPlane(EntityPlane plane){
		GL11.glTranslatef(0, 0.75F, 0.12F);
		GL11.glRotatef(180, 1, 0, 0);
    	RenderHelper.bindTexture(exteriorTextures[plane.textureOptions > 6 ? 0 : plane.textureOptions]);
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
	protected void renderLights(EntityPlane plane){
		//TODO add lights
	}
	
	@Override
	public void renderAuxLights(EntityPlane plane){
		//TODO add lights
	}

	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);		
		RenderHelper.renderQuad(-0.95, -0.95, 0.95, 0.95, 0.08, -0.45, -0.45, 0.08, 2.12, 2.9, 2.9, 2.12, true);		
		RenderHelper.renderQuadUVCustom(-0.95, -0.95, -0.95, -0.95, 0.08, 0.08, -0.58, -0.58, 1.69, 2.02, 3, 1.69, 0, 0.25, 1, 0, 0, 0, 1, 1, false);
		RenderHelper.renderQuadUVCustom(-0.95, -0.95, -0.95, -0.95, 0.08, 0.08, -0.58, -0.58, 2.02, 1.69, 1.69, 3, 0.75, 1, 1, 0, 0, 0, 1, 1, false);
		RenderHelper.renderQuadUVCustom(0.95, 0.95, 0.95, 0.95, 0.08, 0.08, -0.58, -0.58, 1.69, 2.02, 3, 1.69, 0, 0.25, 1, 0, 0, 0, 1, 1, false);
		RenderHelper.renderQuadUVCustom(0.95, 0.95, 0.95, 0.95, 0.08, 0.08, -0.58, -0.58, 2.02, 1.69, 1.69, 3, 0.75, 1, 1, 0, 0, 0, 1, 1, false);
		RenderHelper.renderSquare(-0.95, -0.95, -0.58, 0.08, -0.12, 1.135, true);
		RenderHelper.renderSquare(0.95, 0.95, -0.58, 0.08, -0.12, 1.135, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.7F, -0.7F, 2.8F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.3F, 0.00390625F*1.3F, 0.00390625F*1.3F);
		for(byte i=0; i<plane.instrumentList.size(); ++i){
			if(plane.instrumentList.get(i) != null){
				InstrumentHelper.drawFlyableInstrument(plane, (i%5)*62, i<5 ? 0 : 62, plane.instrumentList.get(i).getItemDamage(), false);
			}
		}
		InstrumentHelper.drawFlyableInstrument(plane, 295, -5, 15, false);
		InstrumentHelper.drawFlyableInstrument(plane, 295, 70, 16, false);
		InstrumentHelper.drawFlyableInstrument(plane, 295, 30, 17, false);
		GL11.glPopMatrix();
	}
	
	private static ResourceLocation[] getExteriorTextures(){
		ResourceLocation[] texArray = new ResourceLocation[7];
		for(byte i=0; i<7; ++i){
			texArray[i] = new ResourceLocation("mfs", "textures/planes/vulcanair/fuselage" + i + ".png");
		}
		return texArray;
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(102, 0, 1, 0);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glScalef(1.5F, 1.5F, 1.5F);
		RenderHelper.drawScaledStringAt(plane.displayName, -2.2F/1.5F, 0.2F/1.5F, -1.25F/1.5F, 1F/32F, Color.black);
		GL11.glRotatef(156, 0, 1, 0);
		RenderHelper.drawScaledStringAt(plane.displayName, 2.2F/1.5F, 0.2F/1.5F, -1.25F/1.5F, 1F/32F, Color.black);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
}
