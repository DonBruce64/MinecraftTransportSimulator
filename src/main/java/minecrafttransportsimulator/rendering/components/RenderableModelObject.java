package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**This class represents an object that can be rendered from an model.  This object is a set of
 * faces that are rendered during the main rendering routine.  Various transforms may be performed on
 * this object via the extended rendering classes.  These transforms are applied to the mesh prior
 * to rendering, either manipulating the mesh directly, or manipulating the OpenGL state.  In all
 * cases, there will be at least one transform on any model object.  This being if it's solid or translucent.
 * Other transforms are added as required.
 *
 * @author don_bruce
 */
public class RenderableModelObject<AnimationEntity extends AEntityC_Definable<?>> extends RenderableTransform<AnimationEntity>{
	private final String objectName;
	public final String applyAfter;
	public final int cachedVertexIndex;
	
	private static final Map<String, Map<String, Integer>> cachedVertexIndexLists = new HashMap<String, Map<String, Integer>>();
	
	public RenderableModelObject(String modelName, String objectName, JSONAnimatedObject animationDefinition, JSONLight lightAnimation, Float[][] vertices, AnimationEntity entity){
		super(animationDefinition != null ? animationDefinition.animations : new ArrayList<JSONAnimationDefinition>());
		
		//Cache the displayList, if we haven't already.
		if(!cachedVertexIndexLists.containsKey(modelName) || !cachedVertexIndexLists.get(modelName).containsKey(objectName)){
			if(!cachedVertexIndexLists.containsKey(modelName)){
				cachedVertexIndexLists.put(modelName, new HashMap<String, Integer>());
			}
			cachedVertexIndexLists.get(modelName).put(objectName, InterfaceRender.cacheVertices(vertices));
		}
		this.cachedVertexIndex = cachedVertexIndexLists.get(modelName).get(objectName);
		
		//Set all parameters for the appropriate transforms.
		this.objectName = objectName;
		if(animationDefinition != null){
			this.applyAfter = animationDefinition.applyAfter;
		}else{
			this.applyAfter = null;
			//Roller found.  Create a transform for it.
			if(objectName.toLowerCase().contains(AModelParser.ROLLER_OBJECT_NAME)){
				transforms.add(new TransformTreadRoller<AnimationEntity>(objectName, vertices, ((AJSONPartProvider) entity.definition).parts));
			}else if(entity instanceof PartGroundDevice){
				PartGroundDevice grounder = (PartGroundDevice) entity;
				if(grounder.definition.ground != null && grounder.definition.ground.isTread){
					//Found tread-based ground device.  Need a transform for tread rendering.
					transforms.add(new TransformTreadRenderer<AnimationEntity>(cachedVertexIndexLists.get(modelName).get(objectName)));
				}
			}
		}
		
		//Check if this is a window or online texture.
		if(objectName.toLowerCase().contains(AModelParser.WINDOW_OBJECT_NAME)){
			transforms.add(new TransformWindow<AnimationEntity>(objectName.toLowerCase().endsWith(AModelParser.INTERIOR_WINDOW_SUFFIX)));
		}
		if(objectName.toLowerCase().startsWith(AModelParser.ONLINE_TEXTURE_OBJECT_NAME) || objectName.toLowerCase().endsWith(AModelParser.ONLINE_TEXTURE_OBJECT_NAME)){
			transforms.add(new TransformOnlineTexture<AnimationEntity>(objectName));
		}
		
		//Check if this is a light.  Depending on if it's a light-up texture or not we adjust our final render pass.
		boolean isBlendedLight = false;
		if(objectName.endsWith(AModelParser.LIGHT_COVER_SUFFIX)){
			transforms.add(new TransformLight_Cover<AnimationEntity>(lightAnimation));
		}else if(objectName.endsWith(AModelParser.LIGHT_FLARE_SUFFIX)){
			transforms.add(new TransformLight_Flare<AnimationEntity>(lightAnimation));
			isBlendedLight = true;
		}else if(objectName.endsWith(AModelParser.LIGHT_BEAM_SUFFIX)){
			transforms.add(new TransformLight_Beam<AnimationEntity>(lightAnimation));
			isBlendedLight = true;
		}else if(lightAnimation != null && !lightAnimation.emissive){
			transforms.add(new TransformLight_LightupTexture<AnimationEntity>(lightAnimation));
		}else if(lightAnimation != null && lightAnimation.emissive){
			transforms.add(new TransformLight_Emissive<AnimationEntity>(lightAnimation));
			isBlendedLight = true;
		}
		
		//Add the main blending/not blending transform to the start of the transform list now that we have all others.
		//This prevents excess operations if we can't even render the object in the current pass.
		if(objectName.toLowerCase().contains(AModelParser.TRANSLUCENT_OBJECT_NAME) || isBlendedLight){
			transforms.add(0, new TransformTranslucent<AnimationEntity>());
		}else{
			transforms.add(0, new TransformSolid<AnimationEntity>());
		}
	}
	
	/**
	 *  Renders this object, applying any transforms that need to happen.  This method also
	 *  renders any objects that depend on this object's transforms after rendering.
	 */
	public void render(AnimationEntity entity, boolean blendingEnabled, float partialTicks, List<RenderableModelObject<AnimationEntity>> allObjects){
		GL11.glPushMatrix();
		if(doPreRenderTransforms(entity, blendingEnabled, partialTicks)){
			//Render the model.
			InterfaceRender.renderVertices(cachedVertexIndex);
			
			//Do post-render logic.
			doPostRenderTransforms(entity, blendingEnabled, partialTicks);
			
			//Render text on this object.
			if(!blendingEnabled){
				if(InterfaceRender.renderTextMarkings(entity, objectName)){
					InterfaceRender.recallTexture();
				}
			}
			
			//Render any parts that depend on us before we pop our state.
			for(RenderableModelObject<AnimationEntity> modelObject : allObjects){
				if(objectName.equals(modelObject.applyAfter)){
					modelObject.render(entity, blendingEnabled, partialTicks, allObjects);
				}
			}
		}
		
		//Pop state.
		GL11.glPopMatrix();
	}
}
