package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.wrappers.WrapperRender;

/**This class represents an object that can be rendered from an OBJ model.  This object is a set of
 * faces that are rendered during the main rendering routine.  These faces may be part of the vehicle
 * itself, or a part on on the vehicle with its own OBJ model.  Various transforms may be performed on
 * this object via the extended rendering classes.  These transforms are applied to the mesh prior
 * to rendering, either manipulating the mesh directly, or manipulating the OpenGL state.
 *
 * @author don_bruce
 */
public final class RenderableModelObject{
	private final String modelName;
	private final String objectName;
	private final Float[][] vertices;
	public final List<ARenderableTransform> transforms = new ArrayList<ARenderableTransform>();
	
	private static final Map<String, Map<String, Integer>> displayLists = new HashMap<String, Map<String, Integer>>();
	
	public RenderableModelObject(String modelName, String objectName, Float[][] vertices, EntityVehicleF_Physics vehicle, APart optionalPart){
		this.modelName = modelName;
		this.objectName = objectName;
		this.vertices = vertices;
		
		boolean isPart = optionalPart != null;
		if(objectName.contains("%")){
			if(isPart ? (optionalPart.definition.rendering != null && optionalPart.definition.rendering.translatableModelObjects != null) : vehicle.definition.rendering.translatableModelObjects != null){
				transforms.add(new TransformTranslatable(modelName, objectName, isPart ? optionalPart.definition.rendering.translatableModelObjects : vehicle.definition.rendering.translatableModelObjects));
			}else{
				throw new NullPointerException("ERROR: " + (isPart ? optionalPart.definition.packID : vehicle.definition.packID) + ":" + (isPart ? optionalPart.definition.systemName : vehicle.definition.genericName) + " has a translatable object:" + objectName + ", but no translatableModelObjects are present in the JSON!");
			}
		}
		if(objectName.contains("$")){
			if(objectName.toLowerCase().contains("roller")){
				if(vehicle.definition.rendering.rotatableModelObjects != null){
					transforms.add(TransformTreadRoller.create(modelName, objectName, vehicle, vertices));
				}else{
					throw new NullPointerException("ERROR: " + vehicle.definition.packID + ":" + vehicle.definition.genericName + " has a rotatable object:" + objectName + ", but no rotatableModelObjects are present in the JSON!");
				}
			}else{
				if(isPart ? (optionalPart.definition.rendering != null && optionalPart.definition.rendering.rotatableModelObjects != null) : vehicle.definition.rendering.rotatableModelObjects != null){
					transforms.add(new TransformRotatable(modelName, objectName, isPart ? optionalPart.definition.rendering.rotatableModelObjects : vehicle.definition.rendering.rotatableModelObjects));
				}else{
					throw new NullPointerException("ERROR: " + (isPart ? optionalPart.definition.packID : vehicle.definition.packID) + ":" + (isPart ? optionalPart.definition.systemName : vehicle.definition.genericName) + " has a rotatable object:" + objectName + ", but no rotatableModelObjects are present in the JSON!");
				}
			}
		}
		if(objectName.contains("&")){
			transforms.add(new TransformLight(modelName, objectName, vertices));
		}
		if(objectName.toLowerCase().contains("window")){
			//Window
			transforms.add(new TransformWindow(vertices));
		}
	}
	
	/**
	 *  Renders this object, applying any transforms that need to happen.
	 */
	public void render(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){
		//Push matrix and Apply transforms.
		GL11.glPushMatrix();
		for(ARenderableTransform transform : transforms){
			transform.applyTransforms(vehicle, optionalPart, partialTicks);
		}
		
		//Render, caching the displayList if needed.
		//Don't render on pass 1, as that's for transparency.
		if(WrapperRender.getRenderPass() != 1){
			if(!displayLists.containsKey(modelName) || !displayLists.get(modelName).containsKey(objectName)){
				int displayListIndex = GL11.glGenLists(1);
				GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
				GL11.glBegin(GL11.GL_TRIANGLES);
				for(Float[] vertex : vertices){
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
				}
				GL11.glEnd();
				GL11.glEndList();
				if(!displayLists.containsKey(modelName)){
					displayLists.put(modelName, new HashMap<String, Integer>());
				}
				displayLists.get(modelName).put(objectName, displayListIndex);
			}
			GL11.glCallList(displayLists.get(modelName).get(objectName));
		}
		
		//Do post-render logic and pop matrix.
		for(ARenderableTransform transform : transforms){
			transform.doPostRenderLogic(vehicle, optionalPart, partialTicks);
		}
		GL11.glPopMatrix();
	}
	
	/**
	 *  Used to reset the display list in dev mode to allow the re-loading of models.
	 */
	public void resetDisplayList(){
		GL11.glDeleteLists(displayLists.get(modelName).get(objectName), 1);
		displayLists.get(modelName).remove(objectName);
	}
}
