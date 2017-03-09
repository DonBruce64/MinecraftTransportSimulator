package minecraftflightsimulator.rendering.renders.blocks;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.blocks.TileEntitySurveyFlag;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.rendering.models.blocks.ModelTEMP;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderTileBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderSurveyFlag extends RenderTileBase{
	//private static final ModelTrackTie model = new ModelTrackTie();
	//private static final ResourceLocation tieSidesTexture = new ResourceLocation("textures/blocks/log_big_oak.png");
	//private static final ResourceLocation tieEndsTexture = new ResourceLocation("textures/blocks/log_big_oak_top.png");
	//private static final ResourceLocation railTexture = new ResourceLocation("mfs", "textures/blocks/rail.png");

	@Override
	protected void doRender(TileEntity tile, double x, double y, double z){
		TileEntitySurveyFlag flag = (TileEntitySurveyFlag) tile;
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glTranslatef(0, -1F, 0.5F);
		GL11.glRotatef(flag.angle, 0, 1, 0);
		GL11DrawSystem.bindTexture(new ResourceLocation("mfs", "textures/blocks/TestTexture.png"));
		ModelTEMP model = new ModelTEMP();
		model.render();
		//TODO render flag base model.
		
		if(flag.linkedCurve != null){
			//Ensure flag hologram hasn't already been rendered.
			if(!flag.isPrimary){			
				TileEntitySurveyFlag otherEnd = (TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(flag.getWorldObj(), flag.linkedCurve.blockEndPoint[0], flag.linkedCurve.blockEndPoint[1], flag.linkedCurve.blockEndPoint[2]);
				if(otherEnd != null){
					if(otherEnd.renderedLastPass){
						return;
					}
				}
			}
			

			GL11.glEnable(GL11.GL_BLEND);
			GL11.glColor4f(0, 1, 0, 0.25F);
			RenderTrack.renderTrackSegmentFromCurve(flag.getWorldObj(), flag.linkedCurve);
			if(flag.isPrimary){
				flag.renderedLastPass = true;
			}
		}
		GL11.glPopMatrix();
	}
}
