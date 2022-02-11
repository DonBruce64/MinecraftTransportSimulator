package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.jsondefs.JSONRendering.ModelType;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.rendering.instances.RenderInstrument;
import minecrafttransportsimulator.rendering.instances.RenderText;

/**Base Entity rendering class.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntityDefinable<RenderedEntity extends AEntityD_Definable<?>> extends ARenderEntity<RenderedEntity>{
	//Object lists for models parsed in this renderer.  Maps are keyed by the model name.
	protected final Map<String, List<RenderableModelObject<RenderedEntity>>> objectLists = new HashMap<String, List<RenderableModelObject<RenderedEntity>>>();
	
	//Static map for caching created render instances to know which ones to send events to.
	private static final List<ARenderEntityDefinable<?>> createdRenderers = new ArrayList<ARenderEntityDefinable<?>>();
	
	public ARenderEntityDefinable(){
		createdRenderers.add(this);
	}
	
	@Override
	protected void renderModel(RenderedEntity entity, boolean blendingEnabled, float partialTicks){
		//Update internal lighting states.
		entity.world.beginProfiling("LightStateUpdates", true);
        entity.updateLightBrightness(partialTicks);
    	
        //Parse model if it hasn't been already.
        entity.world.beginProfiling("ParsingMainModel", false);
    	String modelLocation = entity.definition.getModelLocation(entity.subName);
        if(!objectLists.containsKey(modelLocation)){
        	objectLists.put(modelLocation, AModelParser.generateRenderables(entity));
        }
        
        //Render model.
        entity.world.beginProfiling("RenderingMainModel", false);
        List<RenderableModelObject<RenderedEntity>> map = objectLists.get(modelLocation);
		for(RenderableModelObject<RenderedEntity> modelObject : map){
			JSONAnimatedObject animation = entity.animatedObjectDefinitions.get(modelObject.object.name);
			if(animation == null || animation.applyAfter == null){
				modelObject.render(entity, blendingEnabled, partialTicks);
			}
		}
		
		
		//Render any static text.
		entity.world.beginProfiling("MainText", false);
		if(!blendingEnabled){
			for(JSONText textDef : entity.text.keySet()){
				if(textDef.attachedTo == null){
					RenderText.draw3DText(entity.text.get(textDef), entity, textDef, entity.scale, false);
				}
			}
		}
			
		//Render all instruments.
		entity.world.beginProfiling("Instruments", false);
		renderInstruments(entity, blendingEnabled, partialTicks);
		
		//Handle particles.
		entity.world.beginProfiling("Particles", false);
		entity.spawnParticles(partialTicks);
		entity.world.endProfiling();
		
		//Handle sounds.  These will be partial-tick only ones.
		//Normal sounds are handled on the main tick loop.
		entity.world.beginProfiling("Sounds", false);
		entity.updateSounds(partialTicks);
		entity.world.endProfiling();
	}
	
	@Override
	protected boolean disableRendering(RenderedEntity entity, float partialTicks){
		//Don't render if we don't have a model.
		return super.disableRendering(entity, partialTicks) || entity.definition.rendering.modelType.equals(ModelType.NONE);
	}
	
	/**
	 *  Renders all instruments on the entity.  Uses the instrument's render code.
	 *  We only apply the appropriate translation and rotation.
	 *  Normalization is required here, as otherwise the normals get scaled with the
	 *  scaling operations, and shading gets applied funny. 
	 */
	protected void renderInstruments(RenderedEntity entity, boolean blendingEnabled, float partialTicks){
		if(entity instanceof AEntityE_Interactable){
			AEntityE_Interactable<?> interactable = (AEntityE_Interactable<?>) entity;
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
							RenderInstrument.drawInstrument(interactable.instruments.get(i), packInstrument.optionalPartNumber, interactable, packInstrument.scale/16F, blendingEnabled, partialTicks);
						}
						GL11.glPopMatrix();
					}
				}
			}
		}
	}
	
	@Override
	protected void renderBoundingBoxes(RenderedEntity entity, Point3d entityPositionDelta){
		if(entity instanceof AEntityE_Interactable){
			AEntityE_Interactable<?> interactable = (AEntityE_Interactable<?>) entity;
			//Draw encompassing box for the entity.
			GL11.glTranslated(entityPositionDelta.x, entityPositionDelta.y, entityPositionDelta.z);
			interactable.encompassingBox.renderable.render();
			GL11.glTranslated(-entityPositionDelta.x, -entityPositionDelta.y, -entityPositionDelta.z);
			
			//Draw collision boxes for the entity.
			for(BoundingBox box : interactable.interactionBoxes){
				Point3d boxCenterDelta = box.globalCenter.copy().subtract(entity.position).add(entityPositionDelta);
				GL11.glTranslated(boxCenterDelta.x, boxCenterDelta.y, boxCenterDelta.z);
				box.renderable.render();
				GL11.glTranslated(-boxCenterDelta.x, -boxCenterDelta.y, -boxCenterDelta.z);
			}
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
			for(ARenderEntityDefinable<?> render : createdRenderers){
				render.resetModelCache(modelLocation);
			}
		}
	}
}
