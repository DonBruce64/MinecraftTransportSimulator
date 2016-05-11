package minecraftflightsimulator.planes.Otter;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.helpers.InstrumentHelper;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.modelrenders.RenderPlane;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

public class RenderOtter extends RenderPlane{
	private static final IModelCustom model = AdvancedModelLoader.loadModel(new ResourceLocation("mfs", "models/planes/Otter.obj"));

	public RenderOtter(){}
	
	@Override
	protected void renderPlane(EntityPlane plane){
    	RenderHelper.bindTexture(new ResourceLocation("minecraft", "textures/blocks/stone.png"));
    	GL11.glTranslated(0, -0.5, 6.4);
    	
    	model.renderPart("Mesh.015_Mesh19_Model");
    	model.renderPart("Mesh.008_Mesh5_Steering_wheel_frame_Model");
    	
    	GL11.glRotatef(plane.aileronAngle/10F, 1, 0, 0);
    	model.renderPart("Mesh.018_Mesh10_Aileron_2_Model");
    	GL11.glRotatef(-2*plane.aileronAngle/10F, 1, 0, 0);
    	model.renderPart("Mesh.011_Mesh6_Aileron_1_Model");
    	GL11.glRotatef(plane.aileronAngle/10F, 1, 0, 0);
    	
    	GL11.glRotatef(plane.rudderAngle/10F, 0, 1, 0);
    	model.renderPart("Mesh.019_Mesh1_Group1_Rudder_Model");
    	GL11.glRotatef(-plane.rudderAngle/10F, 0, 1, 0);
    	
    	GL11.glRotatef(-plane.flapAngle/10F, 1, 0, 0);
    	model.renderPart("Mesh.013_Mesh9_Flap_1_Model");
    	model.renderPart("Mesh.016_Mesh13_Flaps_2_Model");
    	GL11.glRotatef(plane.flapAngle/10F, 1, 0, 0);  
    	GL11.glTranslated(0, +0.5, -6.4);
	}

	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);		
		RenderHelper.renderQuad(-1, -1, 0, 0, 2.3, 1.85, 1.85, 2.3, 3.5, 3.97, 3.97, 3.5, true);
		RenderHelper.renderQuad(0, 0, 1, 1, 2.3, 1.85, 1.85, 2.3, 3.5, 3.97, 3.97, 3.5, true);
		RenderHelper.renderTriangle(-1, -1, -1, 2.435, 1.8, 1.8, 3.275, 3.275, 3.97, true);
		RenderHelper.renderTriangle(1, 1, 1, 2.435, 1.8, 1.8, 3.275, 3.275, 3.97, true);
		
		RenderHelper.renderSquare(-1.05, -1.05, 1.44, 1.88, 2.29, 2.64, true);
		RenderHelper.renderSquare(1.05, 1.05, 1.44, 1.88, 2.29, 2.64, true);		
		RenderHelper.renderSquare(-1.05, -1.05, 1.44, 1.88, 1.63, 1.98, true);
		RenderHelper.renderSquare(1.05, 1.05, 1.44, 1.88, 1.63, 1.98, true);
		RenderHelper.renderSquare(-1.05, -1.05, 1.44, 1.88, 0.79, 1.14, true);
		RenderHelper.renderSquare(1.05, 1.05, 1.44, 1.88, 0.79, 1.14, true);
		RenderHelper.renderSquare(-1.05, -1.05, 1.44, 1.88, 0.26, 0.61, true);
		RenderHelper.renderSquare(1.05, 1.05, 1.44, 1.88, 0.26, 0.61, true);
		RenderHelper.renderSquare(-1.05, -1.05, 1.44, 1.88, -0.26, 0.09, true);
		RenderHelper.renderSquare(1.05, 1.05, 1.44, 1.88, -0.26, 0.09, true);
		RenderHelper.renderSquare(-1.05, -1.05, 1.44, 1.88, -0.76, -0.41, true);
		RenderHelper.renderSquare(1.05, 1.05, 1.44, 1.88, -0.76, -0.41, true);
		RenderHelper.renderSquare(-1.05, -1.05, 1.44, 1.88, -1.26, -0.91, true);
		RenderHelper.renderSquare(1.05, 1.05, 1.44, 1.88, -1.26, -0.91, true);
		RenderHelper.renderSquare(-1.025, -1.025, 1.44, 1.88, -2.42, -2.08, true);
		RenderHelper.renderSquare(1.025, 1.025, 1.44, 1.88, -2.42, -2.08, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.7985F, 1.7F, 3.95F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.3F, 0.00390625F*1.3F, 0.00390625F*1.3F);
		for(int i=0; i<plane.instrumentList.size(); ++i){
			if(plane.instrumentList.get(i) != null){
				InstrumentHelper.drawInstrument(plane, (i%5)*66, i<5 ? 0 : 62, plane.instrumentList.get(i).getItemDamage(), false);
			}
		}
		InstrumentHelper.drawInstrument(plane, 320, -5, 15, false);
		InstrumentHelper.drawInstrument(plane, 320, 70, 16, false);
		InstrumentHelper.drawInstrument(plane, 320, 30, 17, false);
		GL11.glPopMatrix();
	}
}
