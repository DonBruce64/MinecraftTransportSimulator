package minecraftflightsimulator.planes.PZLP11;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.modelrenders.RenderPlane;
import net.minecraft.util.ResourceLocation;

public class RenderPZLP11 extends RenderPlane{
	private static final ModelPZLP11 model = new ModelPZLP11();
	private static final ResourceLocation texture = new ResourceLocation("minecraft", "textures/blocks/stone.png");
    
	public RenderPZLP11(){
		super();
	}

	@Override
	protected void renderFuselage(EntityPlane plane){
		GL11.glRotatef(180, 1, 0, 0);
		RenderHelper.bindTexture(texture);
		model.render(plane, plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F, plane.flapAngle/10F * 0.017453292F, 0, 0.0625F);
		GL11.glRotatef(180, 1, 0, 0);
	}
	
	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);
		RenderHelper.renderQuad(-0.25, -0.25, 0.25, 0.25, 1.12, 0.88, 0.88, 1.12, -0.75, -0.35, -0.35, -0.75, true);
		RenderHelper.renderTriangle(-0.25, -0.25, -0.25,     1.05, 0.88, 0.88,     -0.75, -0.75, -0.45, true);
		RenderHelper.renderTriangle(0.25, 0.25, 0.25,     1.05, 0.88, 0.88,     -0.75, -0.75, -0.45, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
			
	}
}
