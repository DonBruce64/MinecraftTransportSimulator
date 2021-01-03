package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
	private static final Map<String, Integer> displayListMap = new HashMap<String, Integer>();
	private static final Map<String, List<RenderableModelObject>> objectListMap = new HashMap<String, List<RenderableModelObject>>();
		
	@Override
	public void render(TileEntityDecor decor, float partialTicks){
		String modelLocation = decor.definition.getModelLocation();
		if(!displayListMap.containsKey(modelLocation)){
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(modelLocation);
			objectListMap.put(modelLocation, OBJParser.generateRenderables(decor, modelLocation, parsedModel, decor.definition.rendering != null ? decor.definition.rendering.animatedObjects : null));
			displayListMap.put(modelLocation, OBJParser.generateDisplayList(parsedModel));
		}
		
		//Bind the texture and render.
		//Don't render on the transparent pass.
		MasterLoader.renderInterface.setTexture(decor.definition.getTextureLocation(decor.currentSubName));
		if(MasterLoader.renderInterface.getRenderPass() != 1){
			GL11.glCallList(displayListMap.get(modelLocation));
		}
		
		//Render any static text.
		if(MasterLoader.renderInterface.renderTextMarkings(decor, null)){
			MasterLoader.renderInterface.recallTexture();
		}
		
		//The display list only renders static objects.  We need to render dynamic ones manually.
		List<RenderableModelObject> modelObjects = objectListMap.get(modelLocation);
		for(RenderableModelObject modelObject : modelObjects){
			if(modelObject.applyAfter == null){
				modelObject.render(decor, partialTicks, modelObjects);
			}
		}
	}
}
