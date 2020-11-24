package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.rendering.components.OBJParser;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
	private static final Map<JSONDecor, Integer> displayListMap = new HashMap<JSONDecor, Integer>();
		
	@Override
	public void render(TileEntityDecor decor, float partialTicks){
		//If we don't have the displaylist and texture cached, do it now.
		if(!displayListMap.containsKey(decor.definition)){
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(decor.definition.getModelLocation());
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
			displayListMap.put(decor.definition, displayListIndex);
		}
		
		//Don't do solid model rendering on the blend pass.
		if(MasterLoader.renderInterface.getRenderPass() != 1){
			//Bind the texture and render.
			MasterLoader.renderInterface.bindTexture(decor.definition.getTextureLocation(decor.currentSubName));
			GL11.glCallList(displayListMap.get(decor.definition));
			//If we have text objects, render them now.
			if(decor.definition.general.textObjects != null){
				MasterLoader.renderInterface.setLightingState(false);
				MasterLoader.renderInterface.renderTextMarkings(decor.definition.general.textObjects, decor.getTextLines(), null, null, true);
				MasterLoader.renderInterface.setLightingState(true);
			}
		}
	}
}
