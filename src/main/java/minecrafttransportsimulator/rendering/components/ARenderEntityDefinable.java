package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.instances.ItemInstrument;
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
	/**Object lists for models parsed in this renderer.  Maps are keyed by the model name.**/
	protected final Map<String, List<RenderableModelObject<RenderedEntity>>> objectLists = new HashMap<String, List<RenderableModelObject<RenderedEntity>>>();
	
	/**Static map for caching created render instances to know which ones to send events to.**/
	private static final List<ARenderEntityDefinable<?>> createdRenderers = new ArrayList<ARenderEntityDefinable<?>>();
	
	/**Static helper matrix for transforming instrument positions.**/
	private static final Matrix4dPlus instrumentTransform = new Matrix4dPlus();
	
	public ARenderEntityDefinable(){
		createdRenderers.add(this);
	}
	
	@Override
	protected void renderModel(RenderedEntity entity, Matrix4dPlus transform, boolean blendingEnabled, float partialTicks){
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
				modelObject.render(entity, transform, blendingEnabled, partialTicks);
			}
		}
		
		//Render any static text.
		entity.world.beginProfiling("MainText", false);
		if(!blendingEnabled){
			for(JSONText textDef : entity.text.keySet()){
				if(textDef.attachedTo == null){
					RenderText.draw3DText(entity.text.get(textDef), entity, transform, textDef, false);
				}
			}
		}
			
		//Render all instruments.
		entity.world.beginProfiling("Instruments", false);
		renderInstruments(entity, transform, blendingEnabled, partialTicks);
		
		//Handle particles.
		entity.world.beginProfiling("Particles", false);
		entity.spawnParticles(partialTicks);
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
	private void renderInstruments(RenderedEntity entity, Matrix4dPlus transform, boolean blendingEnabled, float partialTicks){
		if(entity instanceof AEntityE_Interactable){
			AEntityE_Interactable<?> interactable = (AEntityE_Interactable<?>) entity;
			if(interactable.definition.instruments != null){
				for(int i=0; i<interactable.definition.instruments.size(); ++i){
					ItemInstrument instrument = interactable.instruments.get(i);
					if(instrument != null){
						JSONInstrumentDefinition packInstrument = interactable.definition.instruments.get(i);
						
						//Translate and rotate to standard position.
						//Note that instruments with rotation of Y=0 face backwards, which is opposite of normal rendering.
						//To compensate, we rotate them 180 here.
						instrumentTransform.set(transform);
						instrumentTransform.translate(packInstrument.pos);
						instrumentTransform.matrix(packInstrument.rot);
						instrumentTransform.rotate(180, 0, 1, 0);
						
						//Do transforms if required and render if allowed.
						AnimationSwitchbox switchbox = interactable.instrumentSlotSwitchboxes.get(packInstrument);
						if(switchbox == null || switchbox.runSwitchbox(partialTicks)){
							if(switchbox != null){
								instrumentTransform.matrix(switchbox.netMatrix);
							}
							//Instruments render with 1 unit being 1 pixel, not 1 block, so scale by 1/16.
							instrumentTransform.scale(1/16F);
							RenderInstrument.drawInstrument(interactable, instrumentTransform, i, false, blendingEnabled, partialTicks);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void renderBoundingBoxes(RenderedEntity entity, Matrix4dPlus transform){
		if(entity instanceof AEntityF_Multipart){
			AEntityE_Interactable<?> interactable = (AEntityE_Interactable<?>) entity;
			interactable.encompassingBox.renderWireframe(entity, transform, null, null);
			for(BoundingBox box : interactable.interactionBoxes){
				box.renderWireframe(entity, transform, null, null);
			}
		}else{
			super.renderBoundingBoxes(entity, transform);
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
