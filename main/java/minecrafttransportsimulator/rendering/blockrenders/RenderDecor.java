package minecrafttransportsimulator.rendering.blockrenders;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityDecor;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

public class RenderDecor extends TileEntitySpecialRenderer<TileEntityDecor>{
	private static final ResourceLocation defaultSignTexture = new ResourceLocation(MTS.MODID, "textures/blocks/trafficsign.png");
	private static final Map<String, Integer> displayListMap = new HashMap<String, Integer>();
	private static final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	
	public RenderDecor(){}
	
	@Override
	public void renderTileEntityAt(TileEntityDecor decor, double x, double y, double z, float partialTicks, int destroyStage){
		super.renderTileEntityAt(decor, x, y, z, partialTicks, destroyStage);
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glTranslatef(0.5F, 0.0F, 0.5F);
		if(decor.rotation != 0){
			GL11.glRotatef(-90F*decor.rotation, 0, 1, 0);
		}
		
		//If we don't have the displaylist and texture cached, do it now.
		if(!displayListMap.containsKey(decor.decorName)){
			//Check to make sure this block is still in the packs before we try to parse an OBJ that isn't there.
			if(PackParserSystem.getDecor(decor.decorName) == null){
				//We are an invalid decor.  Render a MTS sign to let players know.
				bindTexture(defaultSignTexture);
				//Top-left
				GL11.glTexCoord2f(0.0F, 0.0F);
				GL11.glNormal3f(0.0F, 1.0F, 0.0F);
				GL11.glVertex3f(-0.5F, 0.0F, 0.5F);
				//Bottom-left
				GL11.glTexCoord2f(0.0F, 0.5F);
				GL11.glNormal3f(0.0F, 1.0F, 0.0F);
				GL11.glVertex3f(-0.5F, 0.0F, -0.5F);
				//Bottom-right
				GL11.glTexCoord2f(1.0F, 0.5F);
				GL11.glNormal3f(0.0F, 1.0F, 0.0F);
				GL11.glVertex3f(0.5F, 0.0F, -0.5F);
				//Top-right
				GL11.glTexCoord2f(1.0F, 0.0F);
				GL11.glNormal3f(0.0F, 1.0F, 0.0F);
				GL11.glVertex3f(0.5F, 0.0F, 0.5F);
				GL11.glPopMatrix();
				return;
			}else{
				Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(decor.decorName.substring(0, decor.decorName.indexOf(':')), "objmodels/decors/" + decor.decorName.substring(decor.decorName.indexOf(':') + 1) + ".obj");
				int displayListIndex = GL11.glGenLists(1);
				
				GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
				GL11.glBegin(GL11.GL_TRIANGLES);
				for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
					for(Float[] vertex : entry.getValue()){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}
				}
				GL11.glEnd();
				GL11.glEndList();
				displayListMap.put(decor.decorName, displayListIndex);
				textureMap.put(decor.decorName, new ResourceLocation(decor.decorName.substring(0, decor.decorName.indexOf(':')), "textures/decors/" + decor.decorName.substring(decor.decorName.indexOf(':') + 1) + ".png"));
			}
		}
		
		//Bind the decor texture and render.
		bindTexture(textureMap.get(decor.decorName));
		GL11.glCallList(displayListMap.get(decor.decorName));
		GL11.glPopMatrix();
	}
}
