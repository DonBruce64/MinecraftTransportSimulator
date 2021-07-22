package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.instances.RenderBoundingBox;
import minecrafttransportsimulator.rendering.instances.RenderInstrument;

/**Base Entity rendering class.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntity<RenderedEntity extends AEntityC_Definable<?>>{
	//Object lists for models parsed in this renderer.  Maps are keyed by the model name.
	private final Map<String, List<RenderableModelObject<RenderedEntity>>> objectLists = new HashMap<String, List<RenderableModelObject<RenderedEntity>>>();

	//Instrument transforms for each instrument slot.
	private final Map<String, Map<Integer, RenderableTransform<RenderedEntity>>> instrumentTransforms = new HashMap<String, Map<Integer, RenderableTransform<RenderedEntity>>>();
	
	//Static map for caching created render instances to know which ones to send events to.
	private static final List<ARenderEntity<?>> createdRenderers = new ArrayList<ARenderEntity<?>>();
	
	public ARenderEntity(){
		createdRenderers.add(this);
	}
	
	/**
	 *  Called to render this entity.  This is the setup method that sets states to the appropriate values.
	 *  After this, the main model rendering method is called.
	 */
	public final void render(RenderedEntity entity, boolean blendingEnabled, float partialTicks){
		//If we need to render, do so now.
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
	        
	        //Update internal lighting states.
	        entity.updateLightBrightness(partialTicks);
	        
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
	        String modelLocation = entity.definition.getModelLocation(entity.subName);
	        if(!objectLists.containsKey(modelLocation)){
	        	parseModel(entity, modelLocation);
	        }
	        
	        boolean mirrored = isMirrored(entity);
	        double scale = getScale(entity, partialTicks);
    		if(mirrored){
    			GL11.glScaled(-scale, scale, scale);
    			GL11.glCullFace(GL11.GL_FRONT);
    		}else if(scale != 1.0){
    			GL11.glScaled(scale, scale, scale);
    		}
			
			//Render all modelObjects.
			List<RenderableModelObject<RenderedEntity>> modelObjects = objectLists.get(modelLocation);
			for(RenderableModelObject<RenderedEntity> modelObject : modelObjects){
				if(modelObject.applyAfter == null){
					modelObject.render(entity, blendingEnabled, partialTicks, modelObjects);
				}
			}
			
			//Render any additional model bits before we render text.
			renderAdditionalModels(entity, blendingEnabled, partialTicks);
			
			//Render all instruments.  These use flat shading.
			GL11.glShadeModel(GL11.GL_FLAT);
			renderInstruments(entity, blendingEnabled);
			
			//Render any static text.
			if(!blendingEnabled){
				InterfaceRender.renderTextMarkings(entity, null);
			}
			
			//End rotation render matrix and reset states.
			if(mirrored){
				GL11.glCullFace(GL11.GL_BACK);
			}
			GL11.glPopMatrix();
			InterfaceRender.resetStates();
			
			//Render bounding boxes for parts and collision points.
			if(!blendingEnabled && InterfaceRender.shouldRenderBoundingBoxes()){
				//Set states for box render.
				InterfaceRender.setLightingState(false);
				InterfaceRender.setTextureState(false);
				GL11.glLineWidth(3.0F);
				renderBoundingBoxes(entity, entityPositionDelta);
				GL11.glLineWidth(1.0F);
				InterfaceRender.setTextureState(true);
				InterfaceRender.setLightingState(true);
			}
			
			//Spawn particles, if we aren't paused and this is the main render pass.
			if(!blendingEnabled && !InterfaceClient.isGamePaused()){
				entity.spawnParticles(partialTicks);
			}
		}
		
		//Render supplementals.
		renderSupplementalModels(entity, blendingEnabled, partialTicks);
	}
	
	/**
	 *  Returns the texture that should be bound to this entity.  This may change between render passes, but only ONE texture
	 *  may be used for any given entity render operation!  By default this returns the JSON-defined texture.
	 */
	public String getTexture(RenderedEntity entity){
		return entity.definition.getTextureLocation(entity.subName);
	}
	
	/**
	 *  Called to parse out this model for the modelObjects.  This can be used to set-up any additional caches.
	 *  Make sure you call super to ensure the model caches get parsed!
	 */
	public void parseModel(RenderedEntity entity, String modelLocation){
		objectLists.put(modelLocation, AModelParser.generateRenderables(entity, modelLocation, entity.definition.rendering != null ? entity.definition.rendering.animatedObjects : null, entity.definition.rendering != null ? entity.definition.rendering.lightObjects : null));
		//Got the normal transforms.  Now check the JSON for any instrument animation transforms.
		Map<Integer, RenderableTransform<RenderedEntity>> instrumentSlotTransforms = new HashMap<Integer, RenderableTransform<RenderedEntity>>();
		if(entity instanceof AEntityD_Interactable){
			AEntityD_Interactable<?> interactable = (AEntityD_Interactable<?>) entity;
			if(interactable.definition.instruments != null){
				for(int i=0; i<interactable.definition.instruments.size(); ++i){
					JSONInstrumentDefinition packInstrument = interactable.definition.instruments.get(i);
					if(packInstrument.animations != null){
						instrumentSlotTransforms.put(i, new RenderableTransform<RenderedEntity>(packInstrument.animations));
					}
				}
			}
			instrumentTransforms.put(modelLocation, instrumentSlotTransforms);
		}
	}
	
	/**
	 *  If the main model needs to be skipped in rendering for any reason, return true here.
	 *  This should be done in place of just leaving the {@link #renderModel(AEntityC_Definable, float)}
	 *  method blank, as that method will do OpenGL setups which have a performance cost.  Note
	 *  that this will NOT disable supplemental model rendering via {@link #renderSupplementalModels(AEntityC_Definable, float)}.
	 */
	public boolean disableMainRendering(RenderedEntity entity, float partialTicks){
		//Don't render on the first tick, as we might have not created some variables yet.
		return entity.ticksExisted == 0;
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
	 *  Returns the scale to render this model at.  Is normally 1.0, but may be scaled if desired.
	 */
	public double getScale(RenderedEntity entity, float partialTicks){
		return 1.0;
	}
	
	/**
	 *  Called after the main model objects have been rendered on this entity, but before the states for setting up the render have
	 *  been reset.  At this point, the texture will still be bound, which allows for additional rendering if so desired.
	 */
	public void renderAdditionalModels(RenderedEntity entity, boolean blendingEnabled, float partialTicks){}
	
	/**
	 *  Called to render supplemental models on this entity.  Used mainly for entities that have other entities
	 *  on them that need to render with the main entity.
	 */
	protected void renderSupplementalModels(RenderedEntity entity, boolean blendingEnabled, float partialTicks){}
	
	/**
	 *  Renders all instruments on the entity.  Uses the instrument's render code.
	 *  We only apply the appropriate translation and rotation.
	 *  Normalization is required here, as otherwise the normals get scaled with the
	 *  scaling operations, and shading gets applied funny. 
	 */
	protected void renderInstruments(RenderedEntity entity, boolean blendingEnabled){
		if(entity instanceof AEntityD_Interactable){
			AEntityD_Interactable<?> interactable = (AEntityD_Interactable<?>) entity;
			if(interactable.definition.instruments != null){
				GL11.glEnable(GL11.GL_NORMALIZE);
				for(int i=0; i<interactable.definition.instruments.size(); ++i){
					if(interactable.instruments.containsKey(i)){
						JSONInstrumentDefinition packInstrument = interactable.definition.instruments.get(i);
						
						//Translate and rotate to standard position.
						GL11.glPushMatrix();
						GL11.glTranslated(packInstrument.pos.x, packInstrument.pos.y, packInstrument.pos.z);
						GL11.glRotated(packInstrument.rot.x, 1, 0, 0);
						GL11.glRotated(packInstrument.rot.y, 0, 1, 0);
						GL11.glRotated(packInstrument.rot.z, 0, 0, 1);
						
						//Do transforms if required.
						RenderableTransform<RenderedEntity> transform = instrumentTransforms.get(entity.definition.getModelLocation(entity.subName)).get(i);
						boolean doRender = true;
						if(transform != null){
							doRender = transform.doPreRenderTransforms(entity, blendingEnabled, 0);
						}
						
						if(doRender){
							//Need to scale by -1 to get the coordinate system to behave and align to the texture-based coordinate system.
							GL11.glScalef(-packInstrument.scale/16F, -packInstrument.scale/16F, -packInstrument.scale/16F);
							
							//Render instrument.
							RenderInstrument.drawInstrument(interactable.instruments.get(i), packInstrument.optionalPartNumber, interactable, blendingEnabled);
						}
						
						if(transform != null){
							transform.doPostRenderTransforms(entity, blendingEnabled, 0);
						}
						GL11.glPopMatrix();
					}
				}
				GL11.glDisable(GL11.GL_NORMALIZE);
			}
		}
	}
	
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
	 *  Call to clear out the object caches for this model.  This resets all caches to cause the rendering
	 *  JSON to be re-parsed.
	 */
	protected void resetModelCache(String modelLocation){
		objectLists.remove(modelLocation);
		instrumentTransforms.remove(modelLocation);
	}
	
	/**
	 *  Called externally to reset all caches for all renders with this definition.  Actual renderer will extend
	 *  the non-static method: this is to allow external systems to trigger this call without them accessing
	 *  the list of created objects.
	 */
	public static void clearObjectCaches(AJSONMultiModelProvider definition){
		for(JSONSubDefinition subDef : definition.definitions){
			String modelLocation = definition.getModelLocation(subDef.subName);
			for(ARenderEntity<?> render : createdRenderers){
				render.resetModelCache(modelLocation);
			}
		}
	}
}
