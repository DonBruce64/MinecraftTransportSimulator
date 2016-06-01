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
    	
    	model.renderPart("Mesh.006_Mesh.007_Fuselage");
    	
    	GL11.glRotatef(plane.aileronAngle/10F, 1, 0, 0);
    	model.renderPart("Mesh_Mesh6_Aileron_2_Model");
    	GL11.glRotatef(-2*plane.aileronAngle/10F, 1, 0, 0);
    	model.renderPart("Mesh.006_Mesh4_Aileron_1_Model");
    	GL11.glRotatef(plane.aileronAngle/10F, 1, 0, 0);
    	
    	GL11.glRotatef(plane.elevatorAngle/10F, 1, 0, 0);
    	model.renderPart("Mesh.001_Mesh8_Group8_Group7_Group6_Group5_Group4_Elevator_1_Model");
    	model.renderPart("Mesh.008_Mesh2_Group3_Model");
    	GL11.glRotatef(-plane.elevatorAngle/10F, 1, 0, 0);
    	
    	GL11.glRotatef(plane.rudderAngle/10F, 0, 1, 0);
    	model.renderPart("Mesh.004_Mesh1_Group2_Group1_Rudder_Model");
    	GL11.glRotatef(-2*plane.rudderAngle/10F, 0, 1, 0);
    	RenderHelper.bindTexture(new ResourceLocation("mfs", "textures/planes/otter/wheelframe.png"));
    	model.renderPart("Mesh.005_Mesh.002_Steering_wheel_frame");
    	GL11.glRotatef(plane.rudderAngle/10F, 0, 1, 0);
    	
    	GL11.glRotatef(-plane.flapAngle/10F, 1, 0, 0);
    	model.renderPart("Mesh.005_Mesh5_Flap_1_Model");
    	model.renderPart("Mesh.003_Mesh7_Flaps_2_Model");
    	GL11.glRotatef(plane.flapAngle/10F, 1, 0, 0);  
    	GL11.glTranslated(0, +0.5, -6.4);
	}

	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);		
		RenderHelper.renderQuad(-1, -1, 0, 0, 2.3, 1.85, 1.85, 2.3, 3.5, 3.97, 3.97, 3.5, true);
		RenderHelper.renderQuad(0, 0, 1, 1, 2.3, 1.85, 1.85, 2.3, 3.5, 3.97, 3.97, 3.5, true);
		RenderHelper.renderTriangle(-1, -1, -1, 2.3, 1.8, 1.8, 3.45, 3.45, 3.97, true);
		RenderHelper.renderTriangle(1, 1, 1, 2.3, 1.8, 1.8, 3.45, 3.45, 3.97, true);
		RenderHelper.renderSquare(-1, -1, 1.8, 2.3, 3.09, 3.45, true);
		RenderHelper.renderSquare(1, 1, 1.8, 2.3, 3.09, 3.45, true);
		
		
		
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
		for(byte i=0; i<plane.instrumentList.size(); ++i){
			if(plane.instrumentList.get(i) != null){
				InstrumentHelper.drawInstrument(plane, (i%5)*66, i<5 ? 0 : 62, plane.instrumentList.get(i).getItemDamage(), false);
			}
		}
		InstrumentHelper.drawInstrument(plane, 320, -5, 15, false);
		InstrumentHelper.drawInstrument(plane, 320, 70, 16, false);
		InstrumentHelper.drawInstrument(plane, 320, 30, 17, false);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderMarkings(EntityPlane plane){}
}
