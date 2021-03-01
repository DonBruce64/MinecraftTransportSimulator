package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.rendering.instances.RenderBoundingBox;

/**Base Entity rendering class.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntity<RenderedEntity extends AEntityC_Definable<?>>{
	//Object lists for models parsed in this renderer.  Maps are keyed by the model name.
	protected final Map<String, List<RenderableModelObject<RenderedEntity>>> objectLists = new HashMap<String, List<RenderableModelObject<RenderedEntity>>>();
	
	/**
	 *  Called to render this entity.  This is the setup method that sets states to the appropriate values.
	 *  After this, the main model rendering method is called.
	 */
	public final void render(RenderedEntity entity, int renderPass, float partialTicks, boolean isSupplemental){
		//If we need to render, do so now.
		if(isSupplemental || entity.renderData.shouldRender(renderPass, partialTicks)){
			if(!disableMainRendering(entity, partialTicks)){
				//Get the render offset.
				//This is the interpolated movement, plus the prior position.
				Point3d entityPositionDelta = entity.prevPosition.getInterpolatedPoint(entity.position, partialTicks);
				
				//Subtract the entity's position by the render entity position to get the delta for translating.
				entityPositionDelta.subtract(InterfaceClient.getRenderViewEntity().getRenderedPosition(partialTicks));
				
				//Get the entity rotation.
				Point3d entityRotation = entity.prevAngles.getInterpolatedPoint(entity.angles, partialTicks);
		       
		        //Set up lighting.  Set it to 1 block above, as entities can travel low and easily clip into blocks.
				//That results in black entities.
				++entity.position.y;
		        InterfaceRender.setLightingToPosition(entity.position);
		        --entity.position.y;
		        
		        //Use smooth shading for main model rendering.
				GL11.glShadeModel(GL11.GL_SMOOTH);
		        
		        //Push the matrix on the stack and translate and rotate to the enitty's position.
				adjustPositionRotation(entity, partialTicks, entityPositionDelta, entityRotation);
				GL11.glPushMatrix();
		        GL11.glTranslated(entityPositionDelta.x, entityPositionDelta.y, entityPositionDelta.z);
		        GL11.glRotated(entityRotation.y, 0, 1, 0);
		        GL11.glRotated(entityRotation.x, 1, 0, 0);
		        GL11.glRotated(entityRotation.z, 0, 0, 1);
				
		        //Render the main model.
		        InterfaceRender.setTexture(getTexture(entity));
		        String modelLocation = entity.definition.getModelLocation();
		        if(!objectLists.containsKey(modelLocation)){
		        	parseModel(entity, modelLocation);
		        }
		        
		        boolean mirrored = isMirrored(entity);
        		if(mirrored){
        			GL11.glScalef(-1.0F, 1.0F, 1.0F);
        			GL11.glCullFace(GL11.GL_FRONT);
        		}
				
				//Render all modelObjects.
				List<RenderableModelObject<RenderedEntity>> modelObjects = objectLists.get(modelLocation);
				for(RenderableModelObject<RenderedEntity> modelObject : modelObjects){
					if(modelObject.applyAfter == null){
						modelObject.render(entity, partialTicks, modelObjects);
					}
				}
				
				//Render any additional model bits before we render text.
				renderAdditionalModels(entity, partialTicks);
				
				//Render any static text.
				InterfaceRender.renderTextMarkings(entity, null);
				
				//End rotation render matrix and reset states.
				if(mirrored){
					GL11.glCullFace(GL11.GL_BACK);
				}
				GL11.glShadeModel(GL11.GL_FLAT);
				GL11.glPopMatrix();
				InterfaceRender.resetStates();
				
				//Render bounding boxes for parts and collision points.
				if(InterfaceRender.shouldRenderBoundingBoxes() && entity instanceof AEntityD_Interactable){
					//Set states for box render.
					InterfaceRender.setLightingState(false);
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glLineWidth(3.0F);
					renderBoundingBoxes(entity, entityPositionDelta);
					GL11.glLineWidth(1.0F);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					InterfaceRender.setLightingState(true);
				}
				
				//Spawn particles, if we aren't paused and this is the main render pass.
				if(InterfaceRender.getRenderPass() != 1 && !InterfaceClient.isGamePaused()){
					entity.spawnParticles();
				}
			}
			
			//Render supplementals.
			renderSupplementalModels(entity, renderPass, partialTicks);
		}
	}
	
	/**
	 *  Returns the texture that should be bound to this entity.  This may change between render passes, but only ONE texture
	 *  may be used for any given entity render operation!
	 */
	public abstract String getTexture(RenderedEntity entity);
	
	/**
	 *  Called to parse out this model for the modelObjects.  This can be used to set-up any additional caches.
	 *  Make sure you call super to ensure the model caches get parsed!
	 */
	public void parseModel(RenderedEntity entity, String modelLocation){
		Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(modelLocation);
		objectLists.put(modelLocation, OBJParser.generateRenderables(entity, modelLocation, parsedModel, entity.definition.rendering != null ? entity.definition.rendering.animatedObjects : null));
	}
	
	/**
	 *  If the main model needs to be skipped in rendering for any reason, return true here.
	 *  This should be done in place of just leaving the {@link #renderModel(AEntityC_Definable, float)}
	 *  method blank, as that method will do OpenGL setups which have a performance cost.  Note
	 *  that this will NOT disable supplemental model rendering via {@link #renderSupplementalModels(AEntityC_Definable, float)}.
	 */
	public boolean disableMainRendering(RenderedEntity entity, float partialTicks){
		return false;
	}
	
	/**
	 *  If the model should be mirrored, return true.  Mirroring will only affect
	 *  the model itself, and will not affect any offset transformations applied in
	 *   {@link #adjustPositionRotation(AEntityC_Definable, float, Point3d, Point3d)}
	 */
	public boolean isMirrored(RenderedEntity entity){
		return false;
	}
	
	/**
	 *  Called to do supplemental modifications to the position and rotation of the entity prior to rendering.
	 *  The passed-in position and rotation are where the code thinks the entity is: where you want to render
	 *  it may not be at this position/rotation.  Hence the ability to modify these parameters.
	 */
	public void adjustPositionRotation(RenderedEntity entity, float partialTicks, Point3d entityPosition, Point3d entityRotation){}
	
	/**
	 *  Called after the main model objects have been rendered on this entity, but before the states for setting up the render have
	 *  been reset.  At this point, the texture will still be bound, which allows for additional rendering if so desired.
	 */
	public void renderAdditionalModels(RenderedEntity entity, float partialTicks){}
	
	/**
	 *  Called to render supplemental models on this entity.  Used mainly for entities that have other entities
	 *  on them that need to render with the main entity.
	 */
	protected void renderSupplementalModels(RenderedEntity entity, int renderPass, float partialTicks){}
	
	/**
	 *  Renders the bounding boxes for the entity collision.
	 *  At this point, the rotation done for the rendering 
	 *  will be un-done, as boxes need to be rendered according to their world state.
	 *  Translation, however, will still be in effect as that allows for relative rendering.
	 */
	protected void renderBoundingBoxes(RenderedEntity entity, Point3d entityPositionDelta){
		if(entity instanceof AEntityD_Interactable){
			AEntityD_Interactable<?> interactable = (AEntityD_Interactable<?>) entity;
			//Draw collision boxes for the entity.
			for(BoundingBox box : interactable.interactionBoxes){
				if(interactable.doorBoxes.containsKey(box)){
					//Green for doors.
					InterfaceRender.setColorState(0.0F, 1.0F, 0.0F, 1.0F);
				}else if(interactable.blockCollisionBoxes.contains(box)){
					//Red for block collisions.
					InterfaceRender.setColorState(1.0F, 0.0F, 0.0F, 1.0F);
				}else if(interactable.collisionBoxes.contains(box)){
					//Black for general collisions.
					InterfaceRender.setColorState(0.0F, 0.0F, 0.0F, 1.0F);
				}else{
					//None of the above.  Must be an interaction box.  Yellow.
					InterfaceRender.setColorState(1.0F, 1.0F, 0.0F, 1.0F);
				}
				
				Point3d boxCenterDelta = box.globalCenter.copy().subtract(entity.position).add(entityPositionDelta);
				GL11.glTranslated(boxCenterDelta.x, boxCenterDelta.y, boxCenterDelta.z);
				RenderBoundingBox.renderWireframe(box);
				GL11.glTranslated(-boxCenterDelta.x, -boxCenterDelta.y, -boxCenterDelta.z);
			}
			InterfaceRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
	
	/**
	 *  Call to clear out the object caches for this entity definition.  This resets all caches to cause the rendering
	 *  JSON to be re-parsed.
	 */
	public void clearObjectCaches(RenderedEntity entity){
		String modelLocation = entity.definition.getModelLocation();
		if(objectLists.containsKey(modelLocation)){
			objectLists.remove(modelLocation);
		}
	}
	
	/**
	 *  Returns true if this entity has the passed-in light on it.
	 */
	public boolean doesEntityHaveLight(RenderedEntity entity, LightType light){
		for(RenderableModelObject<RenderedEntity> modelObject : objectLists.get(entity.definition.getModelLocation())){
			for(ATransform<RenderedEntity> transform : modelObject.transforms){
				if(transform instanceof TransformLight){
					if(((TransformLight<RenderedEntity>) transform).type.equals(light)){
						return true;
					}
				}
			}
		}
		return false;
	}
}
