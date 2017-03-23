package minecraftflightsimulator.planes.Comanche;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.rendering.partrenders.RenderPlane;
import minecraftflightsimulator.systems.GL11DrawSystem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderComanche extends RenderPlane{
	private static final ModelComanche model = new ModelComanche();
	private static final ResourceLocation[] exteriorTextures = getExteriorTextures();

	public RenderComanche(RenderManager manager){
		super(manager);
	}
	
	@Override
	protected float[] getRenderOffset(){
		return noOffset;
	}
	
	@Override
	protected void renderPlane(EntityPlane plane){
		GL11.glTranslatef(0, 0F, 0F);
		GL11.glRotatef(180, 1, 0, 0);
		GL11DrawSystem.bindTexture(exteriorTextures[plane.textureOptions > exteriorTextures.length ? 0 : plane.textureOptions]);
        model.renderFuselage();
        model.renderAilerons(plane.aileronAngle/10F * 0.017453292F);
        model.renderElevators(plane.elevatorAngle/10F * 0.017453292F);
        model.renderRudder(-plane.rudderAngle/10F * 0.017453292F);
        model.renderFlaps(plane.flapAngle/10F * 0.017453292F);
        GL11.glRotatef(180, 1, 0, 0);
	}

	@Override
	protected void renderWindows(EntityPlane plane){
		GL11DrawSystem.bindTexture(GL11DrawSystem.glassTexture);		

	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();

		GL11.glPopMatrix();
	}


	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glPushMatrix();

		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderNavigationLights(EntityPlane plane, float brightness){
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderStrobeLights(EntityPlane plane, float brightness){
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderTaxiLights(EntityPlane plane, float brightness){
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderLandingLights(EntityPlane plane, float brightness){
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renderTaxiBeam(EntityPlane plane) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void renderLandingBeam(EntityPlane plane) {
		// TODO Auto-generated method stub
		
	}
	
	private static ResourceLocation[] getExteriorTextures(){
		ResourceLocation[] texArray = new ResourceLocation[7];
		for(byte i=0; i<4; ++i){
			texArray[i] = new ResourceLocation("mfs", "textures/planes/comanche/fuselage" + i + ".png");
		}
		return texArray;
	}
}
