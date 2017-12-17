package minecrafttransportsimulator.rendering.blockrenders;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntitySurveyFlag;
import minecrafttransportsimulator.rendering.blockmodels.ModelSurveyFlag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderSurveyFlag extends TileEntitySpecialRenderer{
	private static final ModelSurveyFlag model = new ModelSurveyFlag();
	private static final ResourceLocation texture = new ResourceLocation(MTS.MODID, "textures/blockmodels/surveyflag.png");
	
	public RenderSurveyFlag(){}

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(tile, x, y, z, partialTicks, destroyStage);
		TileEntitySurveyFlag flag = (TileEntitySurveyFlag) tile;
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0.5F, 0, 0.5F);
		GL11.glRotatef(180 - flag.rotation*45, 0, 1, 0);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		model.render();
		GL11.glPopMatrix();
		
		if(flag.linkedCurve != null){
			TileEntitySurveyFlag otherEnd = (TileEntitySurveyFlag) flag.getWorld().getTileEntity(flag.getPos().add(flag.linkedCurve.endPos));
			//Make sure not to render if the other end has done so.
			if(otherEnd != null){
				//Try to keep the same flag rendering if possible.
				//If the flag isn't null, render that one instead.
				boolean renderFromOtherEnd = otherEnd.getPos().getX() != flag.getPos().getX() ? otherEnd.getPos().getX() > flag.getPos().getX() : otherEnd.getPos().getZ() > flag.getPos().getZ(); 
				if(renderFromOtherEnd){
					GL11.glPopMatrix();
					this.renderTileEntityAt(otherEnd, x + otherEnd.getPos().getX() - flag.getPos().getX(), y + otherEnd.getPos().getY() - flag.getPos().getY(), z + otherEnd.getPos().getZ() - flag.getPos().getZ(), partialTicks, destroyStage);
					return;
				}
			}
			RenderTrack.renderTrackSegmentFromCurve(flag.getWorld(), flag.getPos(), flag.linkedCurve, true, null, null);
		}
		GL11.glPopMatrix();
	}
}
