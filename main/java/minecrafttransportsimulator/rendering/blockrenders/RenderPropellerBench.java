package minecrafttransportsimulator.rendering.blockrenders;

import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.rendering.partmodels.ModelPropeller;
import minecrafttransportsimulator.systems.OBJParserSystem;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

public class RenderPropellerBench extends TileEntitySpecialRenderer<TileEntityPropellerBench>{
	private static final ModelPropeller propellerModel = new ModelPropeller();
	private static final ResourceLocation tierOneTexture = new ResourceLocation("minecraft", "textures/blocks/planks_oak.png");
	private static final ResourceLocation tierTwoTexture = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
	private static final ResourceLocation tierThreeTexture = new ResourceLocation("minecraft", "textures/blocks/obsidian.png");
	private static final ResourceLocation benchTexture = new ResourceLocation(MTS.MODID, "textures/blocks/propellerbench.png");
	
	private static int baseDisplayListIndex = -1;
	private static Float[][] tableCoords;
	private static Float[][] bitCoords;
	
	public RenderPropellerBench(){}
	
	@Override
	public void renderTileEntityAt(TileEntityPropellerBench bench, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(bench, x, y, z, partialTicks, destroyStage);
		
		if(baseDisplayListIndex == -1){
			baseDisplayListIndex = GL11.glGenLists(1);
			GL11.glNewList(baseDisplayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : OBJParserSystem.parseOBJModel(new ResourceLocation(MTS.MODID, "objmodels/propellerbench.obj")).entrySet()){
				if(entry.getKey().equals("Table")){
					tableCoords = entry.getValue();
				}else if(entry.getKey().equals("Bit")){
					bitCoords = entry.getValue();
				}else{
					for(Float[] vertex : entry.getValue()){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}	
				}
			}
			GL11.glEnd();
			GL11.glEndList();
		}
		
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
		bindTexture(benchTexture);
		GL11.glCallList(baseDisplayListIndex);
		
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, -0.125F);
		GL11.glRotatef(bench.isRunning() ? bench.getWorld().getTotalWorldTime()%360*35 : 0, 0, 1, 0);
		GL11.glTranslatef(0, 0, 0.125F);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : bitCoords){
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
		}
		GL11.glEnd();
		GL11.glPopMatrix();
		
		GL11.glPushMatrix();
		GL11.glTranslatef(tableOffsetX, 0, tableOffsetZ);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : tableCoords){
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
		}
		GL11.glEnd();
		GL11.glTranslatef(0, 1.0F, -0.125F);
		renderMaterial(bench);
		GL11.glPopMatrix();
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
			if(timeLeft > 945){
				renderMaterialBlock(-0.5F, 0.5F, -0.25F, 0.3125F);
			}else if(timeLeft > 55 && timeLeft <= 945){
				if(timeLeft > 145){
					renderMaterialBlock(-0.5F, 0.5F, -0.25F + 0.0625F*(9 - (timeLeft-50)/100), 0.3125F);
				}
				if((timeLeft + 50)%200 >= 100){
					renderMaterialBlock(-0.5F, ((timeLeft + 50)%200 - 100)/100F - 0.5F, -0.25F + 0.0625F*(8 - (timeLeft-50)/100), -0.25F + 0.0625F*(9 - (timeLeft-50)/100));
				}else{
					renderMaterialBlock(0.5F - ((timeLeft + 50)%200)/100F, 0.5F, -0.25F + 0.0625F*(8 - (timeLeft-50)/100), -0.25F + 0.0625F*(9 - (timeLeft-50)/100));
				}
			}
			GL11.glPopMatrix();
		}
		if((timeLeft >= 0 && timeLeft <= 945 && bench.isRunning()) || bench.getPropellerOnBench() != null){
			GL11.glPushMatrix();
			GL11.glTranslatef(0, 0.125F, 0);
			GL11.glRotatef(90, 1, 0, 0);
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
		GL11.glNormal3f(0, 1.0F, 0);
		
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
		GL11.glTexCoord2f(x2*4 + 2, z2*4 + 2);
		GL11.glVertex3f(x2, 0.24F, z2);
		GL11.glTexCoord2f(x2*4 + 2, z1*4 + 2);
		GL11.glVertex3f(x2, 0.24F, z1);
		GL11.glTexCoord2f(x1*4 + 2, z1*4 + 2);
		GL11.glVertex3f(x1, 0.24F, z1);
		GL11.glTexCoord2f(x1*4 + 2, z2*4 + 2);
		GL11.glVertex3f(x1, 0.24F, z2);
		
		GL11.glEnd();
		GL11.glPopMatrix();
	}
}
