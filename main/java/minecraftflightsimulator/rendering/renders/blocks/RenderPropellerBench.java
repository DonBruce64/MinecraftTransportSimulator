package minecraftflightsimulator.rendering.renders.blocks;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.rendering.models.blocks.ModelPropellerBench;
import minecraftflightsimulator.rendering.models.parts.ModelPropeller;
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
	
	private TileEntityPropellerBench bench;
	private float tableTranslationX;
	private float tableTranslationZ;
	private float bitRotation;
	private float lastOperationTime;

	@Override
	public void doRender(TileEntity tile, double x, double y, double z){
		this.bench = (TileEntityPropellerBench) tile;
		if(bench.isRunning()){
			short timeLeft = (short) (bench.timeOperationFinished - bench.getWorldObj().getTotalWorldTime());
			if(bench.getWorldObj().getTotalWorldTime() != lastOperationTime){
				//Only update table on each tick.  MUCH simpler this way.
				lastOperationTime = bench.getWorldObj().getTotalWorldTime();
				if(timeLeft > 955){
					tableTranslationX -= 0.5F/50F;
					tableTranslationZ += 0.2F/50F;
				}else if(timeLeft > 45){
					if(timeLeft%100 >= 45 && timeLeft%100 <= 55){
						tableTranslationZ -= 0.05F/10F;
					}else{
						if((timeLeft + 50)%200 >= 100){
							tableTranslationX += 0.5F/50F;
						}else{
							tableTranslationX -= 0.5F/50F;
						}
					}
				}else{
					tableTranslationX -= 0.5F/50F;
					tableTranslationZ += 0.35F/45F;
				}
			}else if(timeLeft > 45 && timeLeft <= 955){
				bitRotation = (--bitRotation)%360;
			}
		}else{
			tableTranslationX = 0;
			tableTranslationZ = 0;
			bitRotation = 0;
		}
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y + 0.44, z + 1.25);
		GL11.glRotatef(180, 1, 0, 0);
		GL11DrawSystem.bindTexture(benchTexture);
		benchModel.renderBase();
		GL11.glTranslatef(tableTranslationX, 0, tableTranslationZ);
		benchModel.renderTable();
		renderMaterial(bench);
		GL11DrawSystem.bindTexture(benchTexture);
		GL11.glTranslatef(-tableTranslationX, 0, -tableTranslationZ);
		GL11.glTranslatef(0, 0, -0.25F);
		benchModel.renderBody();
		benchModel.renderBit(bitRotation);
		GL11.glPopMatrix();
	}

	private void renderMaterial(TileEntityPropellerBench bench){
		switch(bench.propellerType){
			case 0: GL11DrawSystem.bindTexture(tierOneTexture); break;
			case 1: GL11DrawSystem.bindTexture(tierTwoTexture); break;
			case 2: GL11DrawSystem.bindTexture(tierThreeTexture); break;
		}
		
		short timeLeft = (short) (bench.timeOperationFinished - bench.getWorldObj().getTotalWorldTime());
		//short timeLeft = 710;
		GL11.glPushMatrix();
		GL11.glTranslatef(0.5F, -0.8F, 0);
		if(timeLeft > 955){
			GL11DrawSystem.renderSquare(0.5, -0.5, 0, 0.24, 0.25, 0.25, false);
			GL11DrawSystem.renderSquare(-0.5, -0.5, 0, 0.24, 0.25, 1, false);
			GL11DrawSystem.renderSquare(0.5, 0.5, 0, 0.24, 1, 0.25, false);
			GL11DrawSystem.renderSquare(-0.5, 0.5, 0, 0.24, 1, 1, false);
			GL11DrawSystem.renderQuad(-0.5, -0.5, 0.5, 0.5, 0, 0, 0, 0, 1, 0.25, 0.25, 1, false);
		}else if(timeLeft > 50){
			//Rear part
			if(timeLeft > 150){
				GL11DrawSystem.renderSquare(-0.5, 0.5, 0, 0.24, 1, 1, false);
			}else{
				GL11DrawSystem.renderSquareUV(-0.5, (timeLeft - 50)/100F - 0.5, 0, 0.24, 1, 1, 0, (timeLeft - 50)/100F, 0, 1, false);
			}
			//Inner part
			if(timeLeft > 150){
				GL11DrawSystem.renderSquare(0.5, -0.5, 0, 0.24, 0.5 - tableTranslationZ, 0.5 - tableTranslationZ, false);
			}else{
				GL11DrawSystem.renderSquareUV(-tableTranslationX, -0.5, 0, 0.24, 1, 1, 0.5 + tableTranslationX, 1, 0, 1, false);
			}
			
			//Front part
			if((timeLeft + 50)%200 >= 100){
				GL11DrawSystem.renderSquareUV(-tableTranslationX, -0.5, 0, 0.24, 0.4 - tableTranslationZ, 0.4 - tableTranslationZ, 0.5 + tableTranslationX, 1, 0, 1, false);
			}else{
				GL11DrawSystem.renderSquareUV(0.5, -tableTranslationX, 0, 0.24, 0.4 - tableTranslationZ, 0.4 - tableTranslationZ, 0, 0.5 + tableTranslationX, 0, 1, false);				
			}
		}else{
			if((timeLeft >= 0 && bench.isRunning()) || bench.getPropellerOnBench() != null){
				GL11.glPushMatrix();
				GL11.glTranslatef(0.5F, 0.1F, 0.75F);
				GL11.glRotatef(270, 1, 0, 0);
				GL11.glScalef(0.25F, 0.25F, 0.25F);
				propellerModel.renderPropeller(bench.numberBlades, bench.diameter, 0);
				GL11.glPopMatrix();
			}
		}
		GL11.glPopMatrix();
	}
}
