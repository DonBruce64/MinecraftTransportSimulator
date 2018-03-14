package minecrafttransportsimulator.rendering.blockrenders;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.rendering.blockmodels.ModelPropellerBench;
import minecrafttransportsimulator.rendering.partmodels.ModelPropeller;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

public class RenderPropellerBench extends TileEntitySpecialRenderer<TileEntityPropellerBench>{
	private static final ModelPropellerBench benchModel = new ModelPropellerBench();
	private static final ModelPropeller propellerModel = new ModelPropeller();
	private static final ResourceLocation tierOneTexture = new ResourceLocation("minecraft", "textures/blocks/planks_oak.png");
	private static final ResourceLocation tierTwoTexture = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
	private static final ResourceLocation tierThreeTexture = new ResourceLocation("minecraft", "textures/blocks/obsidian.png");
	private static final ResourceLocation benchTexture = new ResourceLocation(MTS.MODID, "textures/blocks/propellerbench.png");
		
	public RenderPropellerBench(){}
	
	@Override
	public void renderTileEntityAt(TileEntityPropellerBench bench, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(bench, x, y, z, partialTicks, destroyStage);
		
		float tableOffsetX = 0;
		float tableOffsetZ = 0;
		if(bench.isRunning()){
			short timeLeft = (short) (bench.timeOperationFinished - bench.getWorld().getTotalWorldTime());
			if(timeLeft > 955){
				tableOffsetX = -0.5F*(1000 - timeLeft)/45F;
				tableOffsetZ = 0.2625F*(1000 - timeLeft)/45F;
			}else if(timeLeft <= 45){
				tableOffsetX = 0.5F*timeLeft/45F;
				tableOffsetZ = -0.3625F*timeLeft/45F;
			}else{
				//Move the table to cut or line up for another pass.
				//If we're between markers of 45 and 55 we're moving in the Z direction to line up for a cut.
				//Otherwise we're cutting in the X direction.
				if(timeLeft%100 > 45 && timeLeft%100 <= 55){
					//Z offset is based on how far we went in the transition.
					//X offset is based what side of the cut we are on.
					tableOffsetZ = (((int) timeLeft)/100 + (timeLeft%100 - 46)/9F)*0.0625F - 0.3625F;
					if(timeLeft%200 >= 100){
						tableOffsetX = -0.5F;
					}else{
						tableOffsetX = 0.5F;
					}
				}else{
					//Z offset is based on time left brackets.
					//X offset is based on actual time left in the cut.
					tableOffsetZ = ((int) (timeLeft + 54))/100*0.0625F - 0.3625F;
					if((timeLeft + 50)%200 >= 100){
						tableOffsetX = 0.5F - (timeLeft + 44)%100/90F;
					}else{
						tableOffsetX = (timeLeft + 44)%100/90F - 0.5F;
					}
				}
			}
		}
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslatef(0.5F, 0F, 0.5F);
		GL11.glRotatef(180 - 45*bench.rotation, 0, 1, 0);
		GL11.glTranslatef(-0.5F, 0F, -0.5F);
		GL11.glTranslatef(0, 0.44F, 1.25F);
		GL11.glRotatef(180, 1, 0, 0);
		bindTexture(benchTexture);
		benchModel.renderBase();
		GL11.glTranslatef(tableOffsetX, 0, tableOffsetZ);
		benchModel.renderTable();
		renderMaterial(bench);
		bindTexture(benchTexture);
		GL11.glTranslatef(-tableOffsetX, 0, -tableOffsetZ);
		GL11.glTranslatef(0, 0, -0.25F);
		benchModel.renderBody();
		benchModel.renderBit(bench.isRunning() ? bench.getWorld().getTotalWorldTime()*partialTicks : 0);
		GL11.glPopMatrix();
	}

	private void renderMaterial(TileEntityPropellerBench bench){
		switch(bench.propellerType){
			case 0: bindTexture(tierOneTexture); break;
			case 1: bindTexture(tierTwoTexture); break;
			case 2: bindTexture(tierThreeTexture); break;
		}
		
		short timeLeft = (short) (bench.timeOperationFinished - bench.getWorld().getTotalWorldTime());
		if(bench.isRunning()){
			GL11.glPushMatrix();
			GL11.glTranslatef(0.5F, -0.8F, -0.03125F);
			if(timeLeft > 945){
				renderMaterialBlock(-0.5F, 0.5F, 0.4375F, 1F);
			}else if(timeLeft > 55){
				if(timeLeft > 145){
					renderMaterialBlock(-0.5F, 0.5F, 0.4375F + 0.0625F*(9 - (timeLeft-50)/100), 1F);
				}
				if((timeLeft + 50)%200 >= 100){
					renderMaterialBlock(-0.5F, ((timeLeft + 50)%200 - 100)/100F - 0.5F, 0.4375F + 0.0625F*(8 - (timeLeft-50)/100), 0.4375F + 0.0625F*(9 - (timeLeft-50)/100));
				}else{
					renderMaterialBlock(0.5F - ((timeLeft + 50)%200)/100F, 0.5F, 0.4375F + 0.0625F*(8 - (timeLeft-50)/100), 0.4375F + 0.0625F*(9 - (timeLeft-50)/100));
				}
			}
			GL11.glPopMatrix();
		}
		if((timeLeft >= 0 && bench.isRunning()) || bench.getPropellerOnBench() != null){
			GL11.glPushMatrix();
			GL11.glTranslatef(0.5F, -0.7F, 0.78125F);
			GL11.glRotatef(270, 1, 0, 0);
			GL11.glScalef(0.25F, 0.25F, 0.25F);
			propellerModel.renderPropeller(bench.numberBlades, bench.diameter, 0);
			GL11.glPopMatrix();
		}
		
	}
	
	private void renderMaterialBlock(float x1, float x2, float z1, float z2){
		//This whole system is backwards.  +Y is actually down due to Techne model flipping.
		GL11.glPushMatrix();
		GL11.glBegin(GL11.GL_QUADS);
		//Not sure why I only need one of these rather than one for each vertex, but eh...
		GL11.glNormal3f(0, -1.0F, 0);
		
		//Right side
		GL11.glTexCoord2f(z1*4 + 2, 0);
		GL11.glVertex3f(x2, 0, z1);
		GL11.glTexCoord2f(z1*4 + 2, 1);
		GL11.glVertex3f(x2, 0.24F, z1);
		GL11.glTexCoord2f(z2*4 + 2, 1);
		GL11.glVertex3f(x2, 0.24F, z2);
		GL11.glTexCoord2f(z2*4 + 2, 0);
		GL11.glVertex3f(x2, 0, z2);
		
		//Back side
		GL11.glTexCoord2f(x1*4 + 2, 0);
		GL11.glVertex3f(x2, 0, z2);
		GL11.glTexCoord2f(x1*4 + 2, 1);
		GL11.glVertex3f(x2, 0.24F, z2);
		GL11.glTexCoord2f(x2*4 + 2, 1);
		GL11.glVertex3f(x1, 0.24F, z2);
		GL11.glTexCoord2f(x2*4 + 2, 0);
		GL11.glVertex3f(x1, 0, z2);
		
		//Left side
		GL11.glTexCoord2f(z2*4 + 2, 0);
		GL11.glVertex3f(x1, 0, z2);
		GL11.glTexCoord2f(z2*4 + 2, 1);
		GL11.glVertex3f(x1, 0.24F, z2);
		GL11.glTexCoord2f(z1*4 + 2, 1);
		GL11.glVertex3f(x1, 0.24F, z1);
		GL11.glTexCoord2f(z1*4 + 2, 0);
		GL11.glVertex3f(x1, 0, z1);
		
		//Front side
		GL11.glTexCoord2f(x1*4 + 2, 0);
		GL11.glVertex3f(x1, 0, z1);
		GL11.glTexCoord2f(x1*4 + 2, 1);
		GL11.glVertex3f(x1, 0.24F, z1);
		GL11.glTexCoord2f(x2*4 + 2, 1);
		GL11.glVertex3f(x2, 0.24F, z1);
		GL11.glTexCoord2f(x2*4 + 2, 0);
		GL11.glVertex3f(x2, 0, z1);
		
		//Top side
		GL11.glTexCoord2f(x1*4 + 2, z2*4 + 2);
		GL11.glVertex3f(x1, 0, z2);
		GL11.glTexCoord2f(x1*4 + 2, z1*4 + 2);
		GL11.glVertex3f(x1, 0, z1);
		GL11.glTexCoord2f(x2*4 + 2, z1*4 + 2);
		GL11.glVertex3f(x2, 0, z1);
		GL11.glTexCoord2f(x2*4 + 2, z2*4 + 2);
		GL11.glVertex3f(x2, 0, z2);
		
		GL11.glEnd();
		GL11.glPopMatrix();
	}
}
