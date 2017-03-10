package minecraftflightsimulator.rendering.renders.blocks;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.blocks.TileEntitySurveyFlag;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.rendering.models.blocks.ModelSurveyFlag;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderTileBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderSurveyFlag extends RenderTileBase{
	private static final ModelSurveyFlag flagModel = new ModelSurveyFlag();
	private static final ResourceLocation flagPoleTexture = new ResourceLocation("textures/blocks/planks_oak.png");
	private static final ResourceLocation flagFlagTexture = new ResourceLocation("textures/blocks/wool_colored_red.png");

	@Override
	protected void doRender(TileEntity tile, double x, double y, double z){
		TileEntitySurveyFlag flag = (TileEntitySurveyFlag) tile;
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0.5F, 0, 0.5F);
		GL11.glRotatef(-flag.angle, 0, 1, 0);
		GL11DrawSystem.bindTexture(flagPoleTexture);
		flagModel.renderPole();
		GL11DrawSystem.bindTexture(flagFlagTexture);
		flagModel.renderFlag();
		GL11.glPopMatrix();
		
		if(flag.linkedCurve != null){
			//Ensure flag hologram hasn't already been rendered.
			if(!flag.isPrimary){			
				TileEntitySurveyFlag otherEnd = (TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(flag.getWorldObj(), flag.linkedCurve.blockEndPoint[0], flag.linkedCurve.blockEndPoint[1], flag.linkedCurve.blockEndPoint[2]);
				if(otherEnd != null){
					if(otherEnd.renderedLastPass){
						GL11.glPopMatrix();
						return;
					}
				}
			}
			RenderTrack.renderTrackSegmentFromCurve(flag.getWorldObj(), flag.linkedCurve, true);
			if(flag.isPrimary){
				flag.renderedLastPass = true;
			}
		}
		GL11.glPopMatrix();
	}
}
