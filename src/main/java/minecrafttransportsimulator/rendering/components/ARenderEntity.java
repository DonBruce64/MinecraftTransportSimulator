package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.mcinterface.InterfaceClient;

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
	public final void render(RenderedEntity entity, float partialTicks){
		//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
		int renderPass = InterfaceRender.getRenderPass();
		if(renderPass == -1){
			renderPass = 2;
		}
		
		//If we need to render, do so now.
		if(entity.renderData.shouldRender(renderPass, partialTicks)){
			if(!disableMainRendering(entity, partialTicks)){
				//Get the render offset.
				//This is the interpolated movement, plus the prior position.
				Point3d entityPosition = entity.prevPosition.getInterpolatedPoint(entity.position, partialTicks);
				
				//Subtract the entity's position by the render entity position to get the delta for translating.
				entityPosition.subtract(InterfaceClient.getRenderViewEntity().getRenderedPosition(partialTicks));
				
				//Get the entity rotation.
				Point3d entityRotation = entity.prevAngles.getInterpolatedPoint(entity.angles, partialTicks);
		       
		        //Set up lighting.
		        InterfaceRender.setLightingToPosition(entity.position);
		        
		        //Use smooth shading for main model rendering.
				GL11.glShadeModel(GL11.GL_SMOOTH);
		        
		        //Push the matrix on the stack and translate and rotate to the enitty's position.
				adjustPositionRotation(entity, partialTicks, entityPosition, entityRotation);
		        GL11.glPushMatrix();
		        GL11.glTranslated(entityPosition.x, entityPosition.y, entityPosition.z);
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
				
				//End render matrix and reset states.
				if(mirrored){
					GL11.glCullFace(GL11.GL_BACK);
				}
				GL11.glShadeModel(GL11.GL_FLAT);
				GL11.glPopMatrix();
				InterfaceRender.resetStates();
				
				//Spawn particles, if we aren't paused and this is the main render pass.
				if(InterfaceRender.getRenderPass() != 1 && !InterfaceClient.isGamePaused()){
					entity.spawnParticles();
				}
			}
			
			//Render supplementals.
			renderSupplementalModels(entity, partialTicks);
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
	 *  If the main model needs to be skipped in rendering for any reason, return false here.
	 *  This should be done in place of just leaving the {@link #renderModel(AEntityC_Definable, float)}
	 *  method blank, as that method will do OpenGL setups which have a performance cost.  Note
	 *  that this won't disable supplemental model rendering via {@link #renderSupplementalModels(AEntityC_Definable, float)} 
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
	protected void renderSupplementalModels(RenderedEntity entity, float partialTicks){}
	
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
