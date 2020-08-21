package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import mcinterface.InterfaceRender;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

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
	public final String applyAfter;
	public final List<ATransformRenderable> transforms = new ArrayList<ATransformRenderable>();
	
	private static final Map<String, Map<String, Integer>> displayLists = new HashMap<String, Map<String, Integer>>();
	
	public RenderableModelObject(String modelName, String objectName, VehicleAnimatedObject definition, Float[][] vertices, EntityVehicleF_Physics vehicle, APart optionalPart){
		this.modelName = modelName;
		this.objectName = objectName;
		this.vertices = vertices;
		
		if(definition != null){
			this.applyAfter = definition.applyAfter;
			for(VehicleAnimationDefinition animation : definition.animations){
				switch(animation.animationType){
					case("translation") :{
						transforms.add(new TransformTranslatable(animation));
						break;
					}
					case("rotation") :{
						if(objectName.toLowerCase().contains("roller")){
							transforms.add(TransformTreadRoller.create(objectName, animation, vehicle, vertices));
						}else{
							transforms.add(new TransformRotatable(animation));
						}
						break;
					}
				}
			}
		}else{
			this.applyAfter = null;
			if(objectName.contains("&")){
				transforms.add(new TransformLight(modelName, objectName, vertices));
			}
			if(objectName.toLowerCase().contains("roller")){
				transforms.add(TransformTreadRoller.create(objectName, null, vehicle, vertices));
			}
			if(objectName.toLowerCase().contains("window")){
				transforms.add(new TransformWindow(vertices));
			}
		}
	}
	
	/**
	 *  Renders this object, applying any transforms that need to happen.  This method also
	 *  renders any objects that depend on this object's transforms after rendering.
	 */
	public void render(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, List<RenderableModelObject> allObjects){
		GL11.glPushMatrix();
		double priorOffset = 0;
		for(ATransformRenderable transform : transforms){
			if(!transform.shouldRender()){
				//Found a transform that told us not to render.
				//Return to prevent rendering.
				GL11.glPopMatrix();
				return;
			}
			//If the transform is a cumulative offset, send the prior operation's offset down the pipeline. 
			if(transform.definition != null && transform.definition.addPriorOffset){
				priorOffset = transform.applyTransform(vehicle, optionalPart, partialTicks, priorOffset);
			}else{
				priorOffset = transform.applyTransform(vehicle, optionalPart, partialTicks, 0);
			}
		}
		
		//Render, caching the displayList if needed.
		//Don't render on pass 1, as that's for transparency.
		if(InterfaceRender.getRenderPass() != 1){
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
		
		//Do post-render logic.
		for(ATransformRenderable transform : transforms){
			transform.doPostRenderLogic(vehicle, optionalPart, partialTicks);
		}
		
		//Render any parts that depend on us before we pop our state.
		//if(!objectName.contains("&"))System.out.println(objectName);
		for(RenderableModelObject modelObject : allObjects){
			if(objectName.equals(modelObject.applyAfter)){
				modelObject.render(vehicle, optionalPart, partialTicks, allObjects);
			}
		}
		
		//Pop state.
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
