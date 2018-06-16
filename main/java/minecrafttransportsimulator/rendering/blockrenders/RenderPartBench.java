package minecrafttransportsimulator.rendering.blockrenders;

import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityPartBench;
import minecrafttransportsimulator.systems.OBJParserSystem;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

public abstract class RenderPartBench extends TileEntitySpecialRenderer<TileEntityPartBench>{
	
	public static class RenderPropellerBench extends RenderPartBench{
		private static final ResourceLocation propellerBenchTexture = new ResourceLocation(MTS.MODID, "textures/blocks/propellerbench.png");
		
		private static Integer baseDisplayListIndex = -1;
		
		public RenderPropellerBench(){}
		
		@Override
		public void renderTileEntityAt(TileEntityPartBench bench, double x, double y, double z, float partialTicks, int destroyStage){
			super.renderTileEntityAt(bench, x, y, z, partialTicks, destroyStage);
			
			if(baseDisplayListIndex == -1){
				baseDisplayListIndex = GL11.glGenLists(1);
				GL11.glNewList(baseDisplayListIndex, GL11.GL_COMPILE);
				GL11.glBegin(GL11.GL_TRIANGLES);
				for(Entry<String, Float[][]> entry : OBJParserSystem.parseOBJModel(new ResourceLocation(MTS.MODID, "objmodels/propellerbench.obj")).entrySet()){
					for(Float[] vertex : entry.getValue()){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}
				}
				GL11.glEnd();
				GL11.glEndList();
			}
			
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glTranslatef(0.5F, 0F, 0.5F);
			GL11.glRotatef(180 - 45*bench.rotation, 0, 1, 0);
			bindTexture(propellerBenchTexture);
			GL11.glCallList(baseDisplayListIndex);
			GL11.glPopMatrix();
		}
	}
}
