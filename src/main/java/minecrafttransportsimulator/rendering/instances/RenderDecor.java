package minecrafttransportsimulator.rendering.instances;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
		
	@Override
	public void renderModel(TileEntityDecor decor, float partialTicks){
		String modelLocation = decor.definition.getModelLocation();
		if(!objectLists.containsKey(modelLocation)){
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(modelLocation);
			objectLists.put(modelLocation, OBJParser.generateRenderables(decor, modelLocation, parsedModel, decor.definition.rendering != null ? decor.definition.rendering.animatedObjects : null));
		}
		
		//Render all modelObjects.
		InterfaceRender.setTexture(decor.definition.getTextureLocation(decor.subName));
		List<RenderableModelObject<TileEntityDecor>> modelObjects = objectLists.get(modelLocation);
		for(RenderableModelObject<TileEntityDecor> modelObject : modelObjects){
			if(modelObject.applyAfter == null){
				modelObject.render(decor, partialTicks, modelObjects);
			}
		}
		
		//Render any static text.
		InterfaceRender.renderTextMarkings(decor, null);
	}
}
