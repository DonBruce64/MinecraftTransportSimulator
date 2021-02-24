package minecrafttransportsimulator.rendering.instances;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;

public class RenderPoleComponent extends ARenderEntity<ATileEntityPole_Component>{
	
	@Override
	public void renderModel(ATileEntityPole_Component component, float partialTicks){
		//Cache the displaylists and lights if we haven't already.
		String modelLocation = component.definition.getModelLocation();
		if(!objectLists.containsKey(modelLocation)){
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(modelLocation);
			objectLists.put(modelLocation, OBJParser.generateRenderables(component, modelLocation, parsedModel, component.definition.rendering != null ? component.definition.rendering.animatedObjects : null));
		}
		
		//Render the component parts.
		InterfaceRender.setTexture(component.definition.getTextureLocation(component.subName));
		List<RenderableModelObject<ATileEntityPole_Component>> modelObjects = objectLists.get(modelLocation);
		for(RenderableModelObject<ATileEntityPole_Component> modelObject : modelObjects){
			if(modelObject.applyAfter == null){
				modelObject.render(component, partialTicks, modelObjects);
			}
		}
		
		//Render any static text.
		InterfaceRender.renderTextMarkings(component, null);
	}
	
	@Override
	public void adjustPositionRotation(ATileEntityPole_Component component, Point3d entityPosition, Point3d entityRotation){
		component.core.getRenderer().adjustPositionRotation(component.core, entityPosition, entityRotation);
	}
}
