package minecrafttransportsimulator.rendering.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.instances.BlockDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.wrappers.WrapperRender;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor, BlockDecor>{
	private static final Map<JSONDecor, Integer> displayListMap = new HashMap<JSONDecor, Integer>();
		
	@Override
	public void render(TileEntityDecor tile, BlockDecor block, float partialTicks){
		//If we don't have the displaylist and texture cached, do it now.
		if(!displayListMap.containsKey(block.definition)){
			String optionalModelName = block.definition.general.modelName;
			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(block.definition.packID, "objmodels/decors/" + (optionalModelName != null ? optionalModelName : block.definition.systemName) + ".obj");
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
			displayListMap.put(block.definition, displayListIndex);
		}
		
		//Bind the decor texture and render.
		WrapperRender.bindTexture(block.definition.packID, "textures/decors/" + block.definition.systemName + ".png");
		GL11.glCallList(displayListMap.get(block.definition));
	}
}
