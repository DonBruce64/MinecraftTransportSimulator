package minecrafttransportsimulator.rendering.blockrenders;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntitySurveyFlag;
import minecrafttransportsimulator.rendering.blockmodels.ModelSurveyFlag;
import minecrafttransportsimulator.systems.GL11DrawSystem;
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
		GL11DrawSystem.bindTexture(texture);
		model.render();
		GL11.glPopMatrix();
		
		if(flag.linkedCurve != null){
			//Ensure flag hologram hasn't already been rendered.
			if(!flag.isPrimary){			
				TileEntitySurveyFlag otherEnd = (TileEntitySurveyFlag) flag.getWorld().getTileEntity(flag.linkedCurve.blockEndPos);
				if(otherEnd != null){
					if(otherEnd.renderedLastPass){
						GL11.glPopMatrix();
						return;
					}
				}
			}
			RenderTrack.renderTrackSegmentFromCurve(flag.getWorld(), flag.linkedCurve, true, null, null);
			if(flag.isPrimary){
				flag.renderedLastPass = true;
			}
		}
		GL11.glPopMatrix();
	}
}
