package minecraftflightsimulator.planes.Trimotor;

import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.modelrenders.RenderPlane;
import net.minecraft.block.material.MapColor;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderTrimotor extends RenderPlane{
	private static final ModelTrimotor model = new ModelTrimotor();
    private static final ResourceLocation sideTexture = new ResourceLocation("mfs", "textures/trimotor_side.png");
    private static final ResourceLocation rotatedSideTexture = new ResourceLocation("mfs", "textures/trimotor_side_rotated.png");
    private static final ResourceLocation detailTexture = new ResourceLocation("minecraft", "textures/blocks/stone.png");

    private int colorcode = 0;
    
	public RenderTrimotor(){
		super();
	}

	@Override
	protected void renderPlane(EntityPlane plane){
		RenderHelper.bindTexture(sideTexture);
		model.renderFirstStage(plane.textureOptions, plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F, plane.flapAngle/10F * 0.017453292F);
		RenderHelper.bindTexture(rotatedSideTexture);
		model.renderSecondStage(plane.textureOptions, plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F, plane.flapAngle/10F * 0.017453292F);
		
		RenderHelper.bindTexture(detailTexture);
        //MapColor.getMapColorForBlockColored(textureCode).colorValue;
        int color = MapColor.getMapColorForBlockColored(colorcode/16).colorValue;
        if(colorcode/20==16)colorcode=0;
        ++colorcode;
        GL11.glColor3ub((byte) ((color >> 16) & 0xFF), (byte) ((color >> 8) & 0xFF), (byte) (color & 0xFF));
		model.renderThirdStage(plane.textureOptions, plane.aileronAngle/10F * 0.017453292F, plane.elevatorAngle/10F * 0.017453292F, plane.rudderAngle/10F * 0.017453292F, plane.flapAngle/10F * 0.017453292F);
	}
	
	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
			
	}
}
