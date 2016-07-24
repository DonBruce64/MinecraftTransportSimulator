package minecraftflightsimulator.planes.MC172;

import java.awt.Color;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.modelrenders.RenderPlane;
import minecraftflightsimulator.utilities.InstrumentHelper;
import minecraftflightsimulator.utilities.RenderHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderMC172 extends RenderPlane{
	private static final ModelMC172 model = new ModelMC172();
	private static final ResourceLocation[] planeTextures = getPlaneTextures();

	public RenderMC172(){}
	
	@Override
	protected void renderPlane(EntityPlane plane){
    	RenderHelper.bindTexture(planeTextures[plane.textureOptions > 5 ? 0 : plane.textureOptions]);
        model.renderPlane();
        model.renderAilerons(plane.aileronAngle/10F * 0.017453292F);
        model.renderElevators(plane.elevatorAngle/10F * 0.017453292F);
        model.renderRudder(plane.rudderAngle/10F * 0.017453292F);
        model.renderFlaps(plane.flapAngle/10F * 0.017453292F);
	}

	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);
		RenderHelper.renderQuad(-0.75, -0.75, 0.75, 0.75, 1.625, 0.625, 0.625, 1.625, 0.875, 1.75, 1.75, 0.875, true);
		RenderHelper.renderTriangle(-0.75, -0.75, -0.75, 1.625, 0.625, 0.625, 0.875, 0.875, 1.75, true);
		RenderHelper.renderTriangle(0.75, 0.75, 0.75, 1.625, 0.625, 0.625, 0.875, 0.875, 1.75, true);
		RenderHelper.renderSquare(0.85, 0.85, 0.625, 1.625, -0.25, 0.625, true);
		RenderHelper.renderSquare(-0.85, -0.85, 0.625, 1.625, -0.25, 0.625, true);
		RenderHelper.renderTriangle(-0.85, -0.85, -0.7, 1.6, 0.625, 0.625, -0.5, -0.5, -1.95, true);
		RenderHelper.renderTriangle(0.85, 0.85, 0.7, 1.6, 0.625, 0.625, -0.5, -0.5, -1.95, true);
		RenderHelper.renderQuad(-0.8, -0.525, 0.525, 0.8, 1.625, 0.625, 0.625, 1.625, -0.5, -2.1, -2.1, -0.5, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.92F, 0.35F, 0.715F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.5F, 0.00390625F*1.5F, 0.00390625F*1.5F);
		for(byte i=0; i<plane.instrumentList.size(); ++i){
			if(plane.instrumentList.get(i) != null){
				int type = plane.instrumentList.get(i).getItemDamage();
				if(i==0 || i==5){
					GL11.glPushMatrix();
					GL11.glRotatef(-90, 0, 1, 0);
					GL11.glTranslatef(-80, 0, -30);
					GL11.glScalef(0.75F, 0.75F, 0.75F);
					InstrumentHelper.drawFlyableInstrument(plane, 72 + (i%5)*62, i<5 ? -10 : 52, type, false);
					GL11.glPopMatrix();
				}else if(i==4 || i==9){
					GL11.glPushMatrix();
					GL11.glScalef(0.75F, 0.75F, 0.75F);
					InstrumentHelper.drawFlyableInstrument(plane, 72 + (i%5)*62, i<5 ? -10 : 52, type, false);
					GL11.glPopMatrix();
				}else{
					InstrumentHelper.drawFlyableInstrument(plane, (i%5)*62, i<5 ? 0 : 62, type, false);
				}
			}
		}
		InstrumentHelper.drawFlyableInstrument(plane, 272, -5, 15, false);
		InstrumentHelper.drawFlyableInstrument(plane, 272, 60, 16, false);
		InstrumentHelper.drawFlyableInstrument(plane, 232, 80, 17, false);
		GL11.glPopMatrix();
	}
	
	private static ResourceLocation[] getPlaneTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_jungle.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_acacia.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_big_oak.png");
		return texArray;
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(105, 0, 1, 0);
		GL11.glDisable(GL11.GL_LIGHTING);
		RenderHelper.drawScaledStringAt(plane.displayName, -2.8F, -0.35F, -1.36F, 1F/32F, Color.lightGray);
		GL11.glRotatef(150, 0, 1, 0);
		RenderHelper.drawScaledStringAt(plane.displayName, 2.8F, -0.35F, -1.36F, 1F/32F, Color.lightGray);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
}
