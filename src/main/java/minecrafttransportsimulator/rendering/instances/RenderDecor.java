package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
	private static final Map<String, Integer> displayListMap = new HashMap<String, Integer>();
	private static final Map<String, List<RenderableModelObject<TileEntityDecor>>> objectListMap = new HashMap<String, List<RenderableModelObject<TileEntityDecor>>>();
		
	@Override
	public void renderModel(TileEntityDecor decor, float partialTicks){
		String modelLocation = decor.definition.getModelLocation();
		if(!displayListMap.containsKey(modelLocation)){
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(modelLocation);
			objectListMap.put(modelLocation, OBJParser.generateRenderables(decor, modelLocation, parsedModel, decor.definition.rendering != null ? decor.definition.rendering.animatedObjects : null));
			displayListMap.put(modelLocation, OBJParser.generateDisplayList(parsedModel));
		}
		
		//Bind the texture and render.
		//Don't render on the transparent pass.
		InterfaceRender.setTexture(decor.definition.getTextureLocation(decor.subName));
		if(InterfaceRender.getRenderPass() != 1){
			GL11.glCallList(displayListMap.get(modelLocation));
		}
		
		//Render any static text.
		if(InterfaceRender.renderTextMarkings(decor, null)){
			InterfaceRender.recallTexture();
		}
		
		//The display list only renders static objects.  We need to render dynamic ones manually.
		List<RenderableModelObject<TileEntityDecor>> modelObjects = objectListMap.get(modelLocation);
		for(RenderableModelObject<TileEntityDecor> modelObject : modelObjects){
			if(modelObject.applyAfter == null){
				modelObject.render(decor, partialTicks, modelObjects);
			}
		}
	}
}
