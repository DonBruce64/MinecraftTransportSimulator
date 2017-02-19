package minecraftflightsimulator.rendering.modelrenders;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.rendering.partmodels.ModelPropeller;
import minecraftflightsimulator.rendering.partmodels.ModelPropellerBench;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderTileBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

public class RenderPropellerBench extends RenderTileBase{
	private static final ModelPropellerBench benchModel = new ModelPropellerBench();
	private static final ModelPropeller propellerModel = new ModelPropeller();
	private static final ResourceLocation tierOneTexture = new ResourceLocation("minecraft", "textures/blocks/planks_oak.png");
	private static final ResourceLocation tierTwoTexture = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
	private static final ResourceLocation tierThreeTexture = new ResourceLocation("minecraft", "textures/blocks/obsidian.png");
	private static final ResourceLocation benchTexture = new ResourceLocation("mfs", "textures/parts/propellerbench.png");
	
	private static final int moveEndTime = 35;
	private TileEntityPropellerBench bench;
	private float tableTranslation;
	private float bodyTranslation;
	private float bitRotation;

	@Override
	public void doRender(TileEntity tile, double x, double y, double z){
		this.bench = (TileEntityPropellerBench) tile;
		
		if(bench.timeLeft > bench.opTime - moveEndTime){
			tableTranslation = 0.5F*(moveEndTime - bench.timeLeft%(bench.opTime-moveEndTime))/moveEndTime;
			bodyTranslation = 0;
			bitRotation = 0;
		}else if(bench.timeLeft > moveEndTime){
			tableTranslation = 1F*(bench.timeLeft - moveEndTime)/(bench.opTime - 2*moveEndTime) - 0.5F;
			bodyTranslation = (float) (Math.cos(50*Math.PI*(bench.timeLeft-moveEndTime)/(bench.opTime - 2*moveEndTime))/6 - 1/6F);
			bitRotation = bench.isOn ? (--bitRotation)%360 : 0;
		}else if(bench.timeLeft > 0){
			tableTranslation = -0.5F*bench.timeLeft/moveEndTime;
			bodyTranslation = 0;
			bitRotation = 0;
		}else{
			tableTranslation = bodyTranslation = bitRotation = 0;
		}
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y + 0.44, z + 1.25);
		GL11.glRotatef(180, 1, 0, 0);
		GL11DrawSystem.bindTexture(benchTexture);
		benchModel.renderBase();
		GL11.glTranslatef(tableTranslation, 0, 0);
		benchModel.renderTable();
		renderMaterial(bench);
		GL11.glTranslatef(-tableTranslation, 0, bodyTranslation);
		benchModel.renderBody();
		benchModel.renderBit(bitRotation);
		GL11.glPopMatrix();
	}

	private static void renderMaterial(TileEntityPropellerBench bench){
		switch(bench.propertyCode%10){
			case 0: GL11DrawSystem.bindTexture(tierOneTexture); break;
			case 1: GL11DrawSystem.bindTexture(tierTwoTexture); break;
			case 2: GL11DrawSystem.bindTexture(tierThreeTexture); break;
		}
		
		if(bench.timeLeft > moveEndTime){
			float progress;
			if(bench.timeLeft > bench.opTime - moveEndTime){
				progress = 0;
			}else{
				progress = 1F*(bench.opTime - bench.timeLeft - 2*moveEndTime)/(bench.opTime - 2*moveEndTime); 
			}
			GL11DrawSystem.renderSquare(progress, progress, -0.8, -0.56, 0.25, 1, false);
			GL11DrawSystem.renderSquareUV(progress, 1, -0.8, -0.56, 1, 1, progress, 1, 0, 1, false);
			GL11DrawSystem.renderSquareUV(1, progress, -0.8, -0.56, 0.25, 0.25, 0, 1 - progress, 0, 1, false);
			GL11DrawSystem.renderSquare(1, 1, -0.8, -0.56, 1, 0.25, false);
			GL11DrawSystem.renderQuadUV(progress,  progress, 1, 1, -0.8, -0.8, -0.8, -0.8, 1, 0.25, 0.25, 1, progress, 1, 0, 1, false);
		}
		
		if(bench.timeLeft > 0 || (bench.timeLeft == 0 && bench.isOn) || bench.getStackInSlot(3) != null){
			GL11.glPushMatrix();
			GL11.glTranslatef(0.5F, -0.7F, 0.75F);
			GL11.glRotatef(270, 1, 0, 0);
			GL11.glScalef(0.25F, 0.25F, 0.25F);
			propellerModel.renderPropeller(bench.propertyCode%100/10, 70+5*(bench.propertyCode/1000), 0);
			GL11.glPopMatrix();
		}
		GL11DrawSystem.bindTexture(benchTexture);
	}
}
