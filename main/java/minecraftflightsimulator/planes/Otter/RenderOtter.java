package minecraftflightsimulator.planes.Otter;

import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.helpers.InstrumentHelper;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.modelrenders.RenderPlane;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

public class RenderOtter extends RenderPlane{
	private static final IModelCustom model = AdvancedModelLoader.loadModel(new ResourceLocation("mfs", "models/planes/Otter4.obj"));

	public RenderOtter(){}
	
	@Override
	protected void renderFuselage(EntityPlane plane){
    	RenderHelper.bindTexture(new ResourceLocation("minecraft", "textures/blocks/stone.png"));
    	//GL11.glScalef(1.5F, 1.5F, 1.5F);
    	GL11.glTranslated(0, -0.5, 6.25);
    	model.renderAll();  
    	GL11.glTranslated(0, +0.5, -6.25);
        //model.renderPlane(plane.textureOptions, plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F, plane.flapAngle/10F * 0.017453292F);
	}

	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.92F, 0.35F, 0.715F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.5F, 0.00390625F*1.5F, 0.00390625F*1.5F);
		for(int i=0; i<plane.instrumentList.size(); ++i){
			if(plane.instrumentList.get(i) != null){
				int type = plane.instrumentList.get(i).getItemDamage();
				if(i==0 || i==5){
					GL11.glPushMatrix();
					GL11.glRotatef(-90, 0, 1, 0);
					GL11.glTranslatef(-80, 0, -30);
					GL11.glScalef(0.75F, 0.75F, 0.75F);
					InstrumentHelper.drawInstrument(plane, 72 + (i%5)*62, i<5 ? -10 : 52, type, false);
					GL11.glPopMatrix();
				}else if(i==4 || i==9){
					GL11.glPushMatrix();
					GL11.glScalef(0.75F, 0.75F, 0.75F);
					InstrumentHelper.drawInstrument(plane, 72 + (i%5)*62, i<5 ? -10 : 52, type, false);
					GL11.glPopMatrix();
				}else{
					InstrumentHelper.drawInstrument(plane, (i%5)*62, i<5 ? 0 : 62, type, false);
				}
			}
		}
		InstrumentHelper.drawInstrument(plane, 272, -5, 15, false);
		InstrumentHelper.drawInstrument(plane, 272, 60, 16, false);
		InstrumentHelper.drawInstrument(plane, 232, 80, 17, false);
		GL11.glPopMatrix();
	}
}
