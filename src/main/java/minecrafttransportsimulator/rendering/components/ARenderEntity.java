package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.instances.RenderBoundingBox;
import minecrafttransportsimulator.rendering.instances.RenderInstrument;
import minecrafttransportsimulator.rendering.instances.RenderText;

/**Base Entity rendering class.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntity<RenderedEntity extends AEntityC_Definable<?>>{
	//Object lists for models parsed in this renderer.  Maps are keyed by the model name.
	protected final Map<String, List<RenderableModelObject<RenderedEntity>>> objectLists = new HashMap<String, List<RenderableModelObject<RenderedEntity>>>();
	
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
		if(!disableRendering(entity, partialTicks)){
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
	        
	        //Mirror model, if required.
	        boolean mirrored = isMirrored(entity);
	        float scale = (float) getScale(entity, partialTicks);
    		if(mirrored){
    			GL11.glScalef(-scale, scale, scale);
    			GL11.glCullFace(GL11.GL_FRONT);
    		}else if(scale != 1.0){
    			GL11.glScalef(scale, scale, scale);
    		}
			
	        //Render the main model if we can.
	        if(!disableModelRendering(entity, partialTicks)){
	        	String modelLocation = entity.definition.getModelLocation(entity.subName);
		        if(!objectLists.containsKey(modelLocation)){
		        	objectLists.put(modelLocation, AModelParser.generateRenderables(entity));
		        }
		        
				for(RenderableModelObject<RenderedEntity> modelObject : objectLists.get(modelLocation)){
					JSONAnimatedObject animation = entity.animatedObjectDefinitions.get(modelObject.object.name);
					if(animation == null || animation.applyAfter == null){
						modelObject.render(entity, blendingEnabled, partialTicks);
					}
				}
	        }
			
			//Render any additional model bits before we render text.
			renderAdditionalModels(entity, blendingEnabled, partialTicks);
			
			//Render all instruments.  These use flat shading.
			GL11.glShadeModel(GL11.GL_FLAT);
			renderInstruments(entity, blendingEnabled, partialTicks);
			
			//Render any static text.
			if(!blendingEnabled){
				for(JSONText textDef : entity.text.keySet()){
					if(textDef.attachedTo == null){
						RenderText.draw3DText(entity.text.get(textDef), entity, textDef, scale, false);
					}
				}
			}
			
			//End rotation render matrix and reset states.
			if(mirrored){
				GL11.glCullFace(GL11.GL_BACK);
			}
			GL11.glPopMatrix();
			
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
			
			//Handle sounds.  These will be partial-tick only ones.
			//Normal sounds are handled on the main tick loop.
			entity.updateSounds(partialTicks);
		}
	}
	
	/**
	 *  If rendering needs to be skipped in rendering for any reason, return true here..
	 */
	public boolean disableRendering(RenderedEntity entity, float partialTicks){
		//Don't render on the first tick, as we might have not created some variables yet.
		return entity.ticksExisted == 0;
	}
	
	/**
	 *  If the main model needs to be skipped in rendering for any reason, return true here.
	 *  This is different than disabling rendering, as this only blocks the main model,
	 *  and not the setup, components, or the other various things.
	 */
	public boolean disableModelRendering(RenderedEntity entity, float partialTicks){
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
	 *  Renders all instruments on the entity.  Uses the instrument's render code.
	 *  We only apply the appropriate translation and rotation.
	 *  Normalization is required here, as otherwise the normals get scaled with the
	 *  scaling operations, and shading gets applied funny. 
	 */
	protected void renderInstruments(RenderedEntity entity, boolean blendingEnabled, float partialTicks){
		if(entity instanceof AEntityD_Interactable){
			AEntityD_Interactable<?> interactable = (AEntityD_Interactable<?>) entity;
			if(interactable.definition.instruments != null){
				for(int i=0; i<interactable.definition.instruments.size(); ++i){
					if(interactable.instruments.containsKey(i)){
						JSONInstrumentDefinition packInstrument = interactable.definition.instruments.get(i);
						
						//Translate and rotate to standard position.
						//Note that instruments with rotation of Y=0 face backwards, which is opposite of normal rendering.
						//To compensate, we rotate them 180 here.
						GL11.glPushMatrix();
						GL11.glTranslated(packInstrument.pos.x, packInstrument.pos.y, packInstrument.pos.z);
						GL11.glRotated(packInstrument.rot.x, 1, 0, 0);
						GL11.glRotated(packInstrument.rot.y + 180, 0, 1, 0);
						GL11.glRotated(packInstrument.rot.z, 0, 0, 1);
						
						//Do transforms if required and render if allowed.
						if(RenderableModelObject.doPreRenderTransforms(entity, packInstrument.animations, blendingEnabled, partialTicks)){
							//Instruments render with 1 unit being 1 pixel, not 1 block, so scale by the set scale, but divided by 16.
							float scale = packInstrument.scale/16F;
							RenderInstrument.drawInstrument(interactable.instruments.get(i), packInstrument.optionalPartNumber, interactable, scale, blendingEnabled, partialTicks);
						}
						GL11.glPopMatrix();
					}
				}
			}
		}
	}
	
	/**
	 *  Renders the bounding boxes for the entity collision.
	 *  At this point, the translation and rotation done for the rendering 
	 *  will be un-done, as boxes need to be rendered according to their world state.
	 *  The passed-in delta is the delta between the player and the entity.
	 */
	protected void renderBoundingBoxes(RenderedEntity entity, Point3d entityPositionDelta){
		if(entity instanceof AEntityD_Interactable){
			AEntityD_Interactable<?> interactable = (AEntityD_Interactable<?>) entity;
			//Draw collision boxes for the entity.
			for(BoundingBox box : interactable.interactionBoxes){
				if(box.definition != null){
					if(box.definition.variableName != null){
						//Green for doors.
						InterfaceRender.setColorState(ColorRGB.GREEN);
					}else if(interactable.blockCollisionBoxes.contains(box)){
						//Red for block collisions.
						InterfaceRender.setColorState(ColorRGB.RED);
					}else{
						//Black for general collisions.
						InterfaceRender.setColorState(ColorRGB.BLACK);
					}
				}else{
					//Not a defined collision box.  Must be an interaction box.  Yellow.
					InterfaceRender.setColorState(ColorRGB.YELLOW);
				}
				
				Point3d boxCenterDelta = box.globalCenter.copy().subtract(entity.position).add(entityPositionDelta);
				GL11.glTranslated(boxCenterDelta.x, boxCenterDelta.y, boxCenterDelta.z);
				RenderBoundingBox.renderWireframe(box);
				GL11.glTranslated(-boxCenterDelta.x, -boxCenterDelta.y, -boxCenterDelta.z);
			}
			InterfaceRender.setColorState(ColorRGB.WHITE);
		}
	}
	
	/**
	 *  Call to clear out the object caches for this model.  This resets all caches to cause the rendering
	 *  JSON to be re-parsed.
	 */
	private void resetModelCache(String modelLocation){
		List<RenderableModelObject<RenderedEntity>> resetObjects = objectLists.remove(modelLocation);
		if(resetObjects != null){
			for(RenderableModelObject<RenderedEntity> modelObject : resetObjects){
				modelObject.destroy();
			}
		}
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
