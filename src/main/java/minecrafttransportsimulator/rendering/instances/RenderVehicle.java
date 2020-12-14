package minecrafttransportsimulator.rendering.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.items.instances.ItemPartScanner;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.rendering.components.ATransform;
import minecrafttransportsimulator.rendering.components.IVehiclePartFXProvider;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;
import minecrafttransportsimulator.rendering.components.RenderableTransform;
import minecrafttransportsimulator.rendering.components.TransformLight;
import minecrafttransportsimulator.rendering.components.TransformTranslatable;
import minecrafttransportsimulator.rendering.components.TransformTreadRoller;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;

/**Main render class for all vehicles.  Renders the vehicle, along with all parts.
 * As entities don't render above 255 well due to the new chunk visibility system, 
 * this code is called both from the regular render loop and manually from
 * the event-based last pass.  This pass is -1, and should allow both the regular
 * and blending operations to run.
 *
 * @author don_bruce
 */
public final class RenderVehicle{	
	//VEHICLE MAPS.  Maps are keyed by system name.
	private static final Map<String, Integer> vehicleDisplayLists = new HashMap<String, Integer>();
	private static final Map<String, List<RenderableModelObject>> vehicleObjectLists = new HashMap<String, List<RenderableModelObject>>();
	private static final Map<String, Map<Integer, RenderableTransform>> vehicleInstrumentTransforms = new HashMap<String, Map<Integer, RenderableTransform>>();
	@Deprecated
	private static final Map<String, List<Float[]>> treadDeltas = new HashMap<String, List<Float[]>>();
	private static final Map<String, List<Double[]>> treadPoints = new HashMap<String, List<Double[]>>();
	
	//PART MAPS.  Maps are keyed by the part model name.
	private static final Map<String, Integer> partDisplayLists = new HashMap<String, Integer>();
	private static final Map<String, List<RenderableModelObject>> partObjectLists = new HashMap<String, List<RenderableModelObject>>();
	
	
	/**Used to clear out the rendering caches of any vehicles with the passed-in definition.
	 * Used in dev mode to allow the re-loading of models.**/
	public static void clearVehicleCaches(JSONVehicle definition){
		if(vehicleDisplayLists.containsKey(definition.systemName)){
			GL11.glDeleteLists(vehicleDisplayLists.remove(definition.systemName), 1);
			for(RenderableModelObject modelObject : vehicleObjectLists.get(definition.systemName)){
				modelObject.resetDisplayList();
			}
			vehicleObjectLists.remove(definition.systemName);
			treadDeltas.remove(definition.systemName);
			treadPoints.remove(definition.systemName);
			vehicleInstrumentTransforms.remove(definition.systemName);
		}
	}
	
	/**Used to clear out the rendering caches of any parts with the passed-in definition.
	 * Used in dev mode to allow the re-loading of models.**/
	public static void clearPartCaches(JSONPart definition){
		String modelName = definition.getModelLocation();
		if(partDisplayLists.containsKey(modelName)){
			GL11.glDeleteLists(partDisplayLists.remove(modelName), 1);
		}
		if(partObjectLists.containsKey(modelName)){
			for(RenderableModelObject modelObject : partObjectLists.get(modelName)){
				modelObject.resetDisplayList();
			}
			partObjectLists.remove(definition.systemName);
		}
	}
	
	public static boolean doesVehicleHaveLight(EntityVehicleF_Physics vehicle, LightType light){
		for(RenderableModelObject modelObject : vehicleObjectLists.get(vehicle.definition.systemName)){
			for(ATransform transform : modelObject.transforms){
				if(transform instanceof TransformLight){
					if(((TransformLight) transform).type.equals(light)){
						return true;
					}
				}
			}
		}
		for(APart part : vehicle.parts){
			String partModelLocation = part.definition.getModelLocation();
			if(partObjectLists.containsKey(partModelLocation)){
				for(RenderableModelObject modelObject : partObjectLists.get(partModelLocation)){
					for(ATransform transform : modelObject.transforms){
						if(transform instanceof TransformLight){
							if(((TransformLight) transform).type.equals(light)){
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	/**
	 *  Renders the vehicle in its entirety.  Rendering happens normally in pass 0 (solid) and 1 (transparent), but may happen in the
	 *  special pass -1 (end) if the vehicle wasn't rendered in either pass 0 or 1 due to chunk render culling.  Some rendering routines
	 *  only run on specific passes, so see the comments on the called methods for information on what is rendered when.
	 */
	public static void render(EntityVehicleF_Physics vehicle, float partialTicks){
		//Get the render offset.
		//This is the interpolated movement, plus the prior position.
		Point3d vehiclePosition = vehicle.position.copy().subtract(vehicle.prevPosition).multiply((double) partialTicks).add(vehicle.prevPosition);
		
		//Subtract the vehcle's position by the render entity position to get the delta for translating.
		Point3d renderPosition = vehiclePosition.copy().subtract(MasterLoader.gameInterface.getRenderViewEntity().getRenderedPosition(partialTicks));
		
		//Get the vehicle rotation.
		Point3d renderRotation = vehicle.angles.copy().subtract(vehicle.prevAngles).multiply(1D - partialTicks).multiply(-1D).add(vehicle.angles);
       
        //Set up lighting.
        MasterLoader.renderInterface.setLightingToEntity(vehicle);
        
        //Use smooth shading for main model rendering.
		GL11.glShadeModel(GL11.GL_SMOOTH);
        
        //Push the matrix on the stack and translate and rotate to the vehicle's position.
        GL11.glPushMatrix();
        GL11.glTranslated(renderPosition.x, renderPosition.y, renderPosition.z);
        
        GL11.glPushMatrix();
        GL11.glRotated(renderRotation.y, 0, 1, 0);
        GL11.glRotated(renderRotation.x, 1, 0, 0);
        GL11.glRotated(renderRotation.z, 0, 0, 1);
		
        //Render the main model.
		renderMainModel(vehicle, partialTicks);
		
		//Render all the parts.  Parts get translated to their offset position prior to rendering.
		for(APart part : vehicle.parts){
			//Only render real parts that aren't sub parts.  SubParts need to be rendered relative to their main part.
			if(!part.isFake() && !part.vehicleDefinition.isSubPart){
				GL11.glPushMatrix();
				if(part.definition.ground != null && part.definition.ground.isTread){
					//Treads don't get translated by y, or z.
					GL11.glTranslated(part.placementOffset.x, 0, 0);
					renderPart(part, partialTicks);
				}else{
					Point3d offset = part.getPositionOffset(partialTicks).add(part.placementOffset);
					GL11.glTranslated(offset.x, offset.y, offset.z);
					renderPart(part, partialTicks);
				}
				GL11.glPopMatrix();
			}
		}
		
		//Set shading back to normal now that all model bits have been rendered.
		GL11.glShadeModel(GL11.GL_FLAT);
		
		//Render all instruments on the vehicle.
		renderInstruments(vehicle);
		
		//Pop vehicle rotation matrix as the following calls use world coords.
		GL11.glPopMatrix();
		
		//Check to see if we need to manually render riders.
		//This happens if we force-render this vehicle in pass -1.
		if(MasterLoader.renderInterface.getRenderPass() == -1){
			MasterLoader.renderInterface.renderEntityRiders(vehicle, partialTicks);
		}
		
		//Translate the vehicle's world position to render the hitboxes as they use global coords.
        GL11.glTranslated(-vehiclePosition.x, -vehiclePosition.y, -vehiclePosition.z);
		
		//Render holograms for missing parts.
		renderPartBoxes(vehicle);
		
		//Render bounding boxes for parts and collision points.
		if(MasterLoader.renderInterface.shouldRenderBoundingBoxes()){
			renderBoundingBoxes(vehicle);
		}
		
		//Pop vehicle translation matrix and reset all states.
		GL11.glPopMatrix();
		MasterLoader.renderInterface.resetStates();
		
		//Spawn particles, but only once per render cycle.
		if(MasterLoader.renderInterface.getRenderPass() != 1 && !MasterLoader.gameInterface.isGamePaused()){
			for(APart part : vehicle.parts){
				if(part instanceof IVehiclePartFXProvider){
					((IVehiclePartFXProvider) part).spawnParticles();
				}
			}
		}
	}
	
	/**
	 *  Renders the main vehicle model.  The model file is determined from the general name of the JSON, which is really
	 *  just the JSON's file name.  Vehicle model is first translated to the position of the vehicle in the world,
	 *  rotated to the roll, pitch, and yaw, of the vehicle, and then all static portions are rendered.  Dynamic
	 *  animated portions like {@link TransformRotatable2}s, {@link TransformTranslatable}s, and
	 *  {@link WindowPart}s are rendered after this with their respective transformations applied.  All renders are
	 *  cached in DisplayLists, as we only need to translate and rotate them, not apply any transforms or splits.
	 *  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 */
	private static void renderMainModel(EntityVehicleF_Physics vehicle, float partialTicks){
		//Normally we use the pack name, but since all displaylists
		//are the same for all models, this is more appropriate.
		if(!vehicleDisplayLists.containsKey(vehicle.definition.systemName)){
			//No distplay list for this model.  Parse and create it now.
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(vehicle.definition.getModelLocation());
			
			//For anything that has a definition as an animation, add it to an animated list.
			//If we find a definition, we remove the object so it doesn't get packed into the main DisplayList.
			List<RenderableModelObject> modelObjects = new ArrayList<RenderableModelObject>();
			if(vehicle.definition.rendering.animatedObjects != null){
				for(VehicleAnimatedObject definition : vehicle.definition.rendering.animatedObjects){
					if(parsedModel.containsKey(definition.objectName)){
						modelObjects.add(new RenderableModelObject(vehicle.definition.systemName, definition.objectName, definition, parsedModel.get(definition.objectName), vehicle, null));
						parsedModel.remove(definition.objectName);
					}
				}
			}
			
			//Now check for any non-animated model objects.
			Iterator<Entry<String, Float[][]>> iterator = parsedModel.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<String, Float[][]> entry = iterator.next();
				RenderableModelObject modelObject = new RenderableModelObject(vehicle.definition.systemName, entry.getKey(), null, entry.getValue(), vehicle, null);
				if(!modelObject.transforms.isEmpty()){
					modelObjects.add(modelObject);
					iterator.remove();
				}
			}
			
			//Now check for any animated instruments.
			Map<Integer, RenderableTransform> instrumentTransforms = new HashMap<Integer, RenderableTransform>();
			for(int i=0; i<vehicle.definition.motorized.instruments.size(); ++i){
				PackInstrument packInstrument = vehicle.definition.motorized.instruments.get(i);
				if(packInstrument.animations != null){
					instrumentTransforms.put(i, new RenderableTransform(packInstrument.animations));
				}
			}
			
			//Now finalize the maps.
			vehicleDisplayLists.put(vehicle.definition.systemName, OBJParser.generateDisplayList(parsedModel));
			vehicleObjectLists.put(vehicle.definition.systemName, modelObjects);
			vehicleInstrumentTransforms.put(vehicle.definition.systemName, instrumentTransforms);
		}
		
		//Bind the texture and render.
		//Don't render on the transparent pass.
		MasterLoader.renderInterface.setTexture(vehicle.definition.getTextureLocation(vehicle.currentSubName));
		if(MasterLoader.renderInterface.getRenderPass() != 1){
			GL11.glCallList(vehicleDisplayLists.get(vehicle.definition.systemName));
		}
		
		//Render any static text.
		if(MasterLoader.renderInterface.renderTextMarkings(vehicle.definition.rendering != null ? vehicle.definition.rendering.textObjects : null, vehicle.textLines, vehicle.getSubDefinition().secondColor, null, vehicle.areInteriorLightsOn())){
			MasterLoader.renderInterface.recallTexture();
		}
		
		//The display list only renders static objects.  We need to render dynamic ones manually.
		List<RenderableModelObject> modelObjects = vehicleObjectLists.get(vehicle.definition.systemName);
		for(RenderableModelObject modelObject : modelObjects){
			if(modelObject.applyAfter == null){
				modelObject.render(vehicle, null, partialTicks, modelObjects);
			}
		}
	}
	
	/**
	 *  Renders all parts on the vehicle.  Parts are first translated to their actual position, which they keep track of.
	 *  After this they are rotated via {@link #rotatePart(APart, float)}.  Finally, any parts of the part
	 *  model that are {@link TransformRotatable2}s or {@link TransformTranslatable}s are rendered with
	 *  their rotations applied.  This makes rendering a split process.  Translate to position, rotate at position,
	 *  render static portions of part model, apply transforms to animated portions of the part model, and then
	 *  render the animated portions.  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 */
	private static void renderPart(APart part, float partialTicks){
		String partModelLocation = part.definition.getModelLocation();
		if(!partDisplayLists.containsKey(partModelLocation)){
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(partModelLocation);
			
			//For anything that has a definition as an animation, add it to an animated list.
			//If we find a definition, we remove the object so it doesn't get packed into the main DisplayList.
			List<RenderableModelObject> modelObjects = new ArrayList<RenderableModelObject>();
			if(part.definition.rendering != null && part.definition.rendering.animatedObjects != null){
				for(VehicleAnimatedObject definition : part.definition.rendering.animatedObjects){
					if(parsedModel.containsKey(definition.objectName)){
						modelObjects.add(new RenderableModelObject(partModelLocation, definition.objectName, definition, parsedModel.get(definition.objectName), part.vehicle, part));
						parsedModel.remove(definition.objectName);
					}
				}
			}
			
			//Now check for any non-animated model objects.
			Iterator<Entry<String, Float[][]>> iterator = parsedModel.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<String, Float[][]> entry = iterator.next();
				RenderableModelObject modelObject = new RenderableModelObject(partModelLocation, entry.getKey(), null, entry.getValue(), part.vehicle, part);
				if(!modelObject.transforms.isEmpty()){
					modelObjects.add(modelObject);
					iterator.remove();
				}
			}
			
			//Now finalize the maps.
			partDisplayLists.put(partModelLocation, OBJParser.generateDisplayList(parsedModel));
			partObjectLists.put(partModelLocation, modelObjects);
		}
		
		//If we aren't using the vehicle texture, bind the texture for this part.
		//Otherwise, bind the vehicle texture as it may have been un-bound prior to this from another part.
		if(!part.definition.general.useVehicleTexture){
			MasterLoader.renderInterface.setTexture(part.definition.getTextureLocation(part.currentSubName));
		}else{
			MasterLoader.renderInterface.setTexture(part.vehicle.definition.getTextureLocation(part.vehicle.currentSubName));
		}
		
		//Rotate the part prior to rendering the displayList.
		//We will already have been translated to our position prior to this call.
		GL11.glPushMatrix();
		rotatePart(part, partialTicks);
		
		//Mirror the model if we need to do so.
		//If we are a sub-part, don't mirror as we'll already be mirrored.
		boolean mirrored = ((part.placementOffset.x < 0 && !part.vehicleDefinition.inverseMirroring) || (part.placementOffset.x >= 0 && part.vehicleDefinition.inverseMirroring)) && !part.disableMirroring && !part.vehicleDefinition.isSubPart;
		if(mirrored){
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			GL11.glCullFace(GL11.GL_FRONT);
		}
		
		//If we are a tread, do the tread-specific render rather than the display list.
		//Don't do this for pass 1 though as treads don't have transparency.
		if(part.definition.ground != null && part.definition.ground.isTread && MasterLoader.renderInterface.getRenderPass() != 1){
			if(part.vehicleDefinition.treadZPoints != null){
				doManualTreadRender((PartGroundDevice) part, partialTicks, partDisplayLists.get(partModelLocation));	
			}else{
				doAutomaticTreadRender((PartGroundDevice) part, partialTicks, partDisplayLists.get(partModelLocation));
			}
		}else{
    		//Render the part DisplayList, but only if we aren't in the transparent pass.
			if(MasterLoader.renderInterface.getRenderPass() != 1){
				GL11.glCallList(partDisplayLists.get(partModelLocation));
			}
			
			//Render any static text.
			if(MasterLoader.renderInterface.renderTextMarkings(part.definition.rendering != null ? part.definition.rendering.textObjects : null, part.textLines, part.vehicle.getSubDefinition().secondColor, null, part.vehicle.areInteriorLightsOn())){
				MasterLoader.renderInterface.recallTexture();
			}
			
			//The display list only renders static object.  We need to render dynamic ones manually.
			List<RenderableModelObject> modelObjects = partObjectLists.get(partModelLocation);
			for(RenderableModelObject modelObject : modelObjects){
				if(modelObject.applyAfter == null){
					modelObject.render(part.vehicle, part, partialTicks, modelObjects);
				}
			}
			
			//Now that we have rendered this part, render any sub-part children.
			for(APart childPart : part.childParts){
				if(!childPart.isFake() && childPart.vehicleDefinition.isSubPart){
					//Get the relative distance between our offset and our parent's offset.
					Point3d relativeOffset = childPart.getPositionOffset(partialTicks).add(childPart.placementOffset).subtract(part.placementOffset);
					
					
					//Translate to our new center and render.
					//If we are mirroring, and are a child part that shouldn't mirror, don't do so.
					GL11.glPushMatrix();
					GL11.glTranslated(mirrored ? -relativeOffset.x : relativeOffset.x, relativeOffset.y, relativeOffset.z);
					if(mirrored && childPart.disableMirroring){
						GL11.glScalef(-1.0F, 1.0F, 1.0F);
						GL11.glCullFace(GL11.GL_BACK);
						renderPart(childPart, partialTicks);
						GL11.glScalef(-1.0F, 1.0F, 1.0F);
						GL11.glCullFace(GL11.GL_FRONT);
					}else{
						renderPart(childPart, partialTicks);
					}
					GL11.glPopMatrix();
				}
			}
		}
		//Set cullface back to normal if we switched it and pop matrix.
		if(mirrored){
			GL11.glCullFace(GL11.GL_BACK);
		}
		GL11.glPopMatrix();
	}
	
	/**
	 *  Rotates a part on the model.  The rotation takes into account the vehicle, static, JSON-applied rotation, 
	 *  as well as the dynamic rotation that depend on the part itself.  Rotation needs to be done after translation
	 *   to the part's position to avoid coordinate system conflicts.  Note that yaw rotations are inverted, as MC's
	 *   Y axis rotation is backwards from RHR convention.
	 */
	private static void rotatePart(APart part, float partialTicks){
		if(!part.placementRotation.isZero()){
			if(part.parentPart != null && part.vehicleDefinition.isSubPart){
				GL11.glRotated(part.placementRotation.y - part.parentPart.placementRotation.y, 0, 1, 0);
				GL11.glRotated(part.placementRotation.x - part.parentPart.placementRotation.x, 1, 0, 0);
				GL11.glRotated(part.placementRotation.z - part.parentPart.placementRotation.z, 0, 0, 1);
			}else{
				GL11.glRotated(part.placementRotation.y, 0, 1, 0);
				GL11.glRotated(part.placementRotation.x, 1, 0, 0);
				GL11.glRotated(part.placementRotation.z, 0, 0, 1);
			}
		}
		
		Point3d positionRotation = part.getPositionRotation(partialTicks);
		if(!positionRotation.isZero()){
			GL11.glRotated(positionRotation.y, 0, 1, 0);
			GL11.glRotated(positionRotation.x, 1, 0, 0);
			GL11.glRotated(positionRotation.z, 0, 0, 1);
		}

		Point3d actionRotation = part.getActionRotation(partialTicks);
		if(!actionRotation.isZero()){
			GL11.glRotated(actionRotation.y, 0, 1, 0);
			GL11.glRotated(actionRotation.x, 1, 0, 0);
			GL11.glRotated(actionRotation.z, 0, 0, 1);
		}
	}
	
	/**
	 *  Renders the treads using a manual system.  Points are defined by pack authors and are located in the
	 *  vehicle JSON.  This method is more cumbersome for the authors, but allows for precise path control.
	 */
	@Deprecated
	private static void doManualTreadRender(PartGroundDevice treadPart, float partialTicks, int displayListIndex){
		List<Float[]> deltas = treadDeltas.get(treadPart.vehicle.definition.systemName);
		if(deltas == null){
			//First calculate the total distance the treads need to be rendered.
			float totalDistance = 0;
			float lastY = treadPart.vehicleDefinition.treadYPoints[0];
			float lastZ = treadPart.vehicleDefinition.treadZPoints[0];
			for(byte i=1; i<treadPart.vehicleDefinition.treadYPoints.length; ++i){
				totalDistance += Math.hypot((treadPart.vehicleDefinition.treadYPoints[i] - lastY), (treadPart.vehicleDefinition.treadYPoints[i] - lastZ));
				lastY = treadPart.vehicleDefinition.treadYPoints[i];
				lastZ = treadPart.vehicleDefinition.treadZPoints[i];
			}
			
			//Now that we have the total distance, generate a set of points for the path.
			//These points should be as far apart as the spacing parameter.
			deltas = new ArrayList<Float[]>();
			final float spacing = treadPart.definition.ground.spacing;
			byte pointIndex = 0;
			float currentY = treadPart.vehicleDefinition.treadYPoints[pointIndex];
			float currentZ = treadPart.vehicleDefinition.treadZPoints[pointIndex];
			float nextY = treadPart.vehicleDefinition.treadYPoints[pointIndex + 1];
			float nextZ = treadPart.vehicleDefinition.treadZPoints[pointIndex + 1];
			float deltaYBeforeSegment = 0;
			float deltaZBeforeSegment = 0;
			float deltaBeforeSegment = 0;
			float segmentDeltaY = (nextY - currentY);
			float segmentDeltaZ = (nextZ - currentZ);
			float segmentDeltaTotal = (float) Math.hypot(segmentDeltaY, segmentDeltaZ);
			float angle = treadPart.vehicleDefinition.treadAngles[pointIndex];
			float currentAngle = 0;
			
			//Keep moving along the sets of points, making another set of evenly-spaced points.
			//This set of points will be used for rendering.
			while(totalDistance > 0){
				//If we are further than the delta between points, go to the next one.
				//Set the points to the next index set and increment delta and angle.
				while(deltaBeforeSegment + segmentDeltaTotal < spacing){
					++pointIndex;
					//If we run out of points go back to the start of the point set.
					//If we are out again, exit the loop.
					if(pointIndex + 1 == treadPart.vehicleDefinition.treadYPoints.length){
						currentY = treadPart.vehicleDefinition.treadYPoints[pointIndex];
						currentZ = treadPart.vehicleDefinition.treadZPoints[pointIndex];
						nextY = treadPart.vehicleDefinition.treadYPoints[0];
						nextZ = treadPart.vehicleDefinition.treadZPoints[0];
						//Ensure we rotate the angle by the correct amount for the joint.
						//It's possible that we will add a negative angle here due to going from something like 270 to 0.
						//This will cause a -270 rotation rather than the +30 we want.
						float angleToAdd = treadPart.vehicleDefinition.treadAngles[0] - treadPart.vehicleDefinition.treadAngles[pointIndex];
						while(angleToAdd < 0){
							angleToAdd += 360; 
						}
						angle += angleToAdd;
					}else if(pointIndex + 1 > treadPart.vehicleDefinition.treadYPoints.length){
						break;
					}else{
						currentY = treadPart.vehicleDefinition.treadYPoints[pointIndex];
						currentZ = treadPart.vehicleDefinition.treadZPoints[pointIndex];
						nextY = treadPart.vehicleDefinition.treadYPoints[pointIndex + 1];
						nextZ = treadPart.vehicleDefinition.treadZPoints[pointIndex + 1];
						angle += treadPart.vehicleDefinition.treadAngles[pointIndex] - treadPart.vehicleDefinition.treadAngles[pointIndex - 1];
					}
					
					//Update deltas.
					deltaBeforeSegment += segmentDeltaTotal;
					deltaYBeforeSegment += segmentDeltaY;
					deltaZBeforeSegment += segmentDeltaZ;
					segmentDeltaY = nextY - currentY;
					segmentDeltaZ = nextZ - currentZ;
					segmentDeltaTotal = (float) Math.hypot(segmentDeltaY, segmentDeltaZ);
				}
				
				//If we have enough distance for a segment, make one.
				//Otherwise add the end distance and set the total to 0.
				if(deltaBeforeSegment + segmentDeltaTotal >= spacing){
					//We are now at a point where the distance between the current point and the next point
					//are greater than the inter-point distance.  Use the slope of these two points to make a delta.
					//If we have any delta before the point, make sure we take that into account when getting the new point.
					float segmentPercentage = (spacing - deltaBeforeSegment)/segmentDeltaTotal;
					float segmentY = deltaYBeforeSegment + segmentDeltaY*segmentPercentage;
					float segmentZ = deltaZBeforeSegment + segmentDeltaZ*segmentPercentage;
					
					//Normally we could add the point now, but since OpenGL rotation changes the coordinate system
					//we need to correct for that here.  Use trigonometry to rotate the segment before adding it.
					currentAngle += angle;
					float correctedZ = (float) (Math.cos(Math.toRadians(currentAngle))*segmentZ - Math.sin(Math.toRadians(currentAngle))*segmentY);
					float correctedY = (float) (Math.sin(Math.toRadians(currentAngle))*segmentZ + Math.cos(Math.toRadians(currentAngle))*segmentY);
					deltas.add(new Float[]{correctedY, correctedZ, angle});
					//Decrement distance traveled off the variables.
					totalDistance -= spacing;
					segmentDeltaTotal -= spacing;
					segmentDeltaY -= segmentDeltaY*segmentPercentage;
					segmentDeltaZ -= segmentDeltaZ*segmentPercentage;
					deltaBeforeSegment = 0;
					deltaYBeforeSegment = 0;
					deltaZBeforeSegment = 0;
					angle = 0;
				}else{
					//If we have half or more a link left, make an extra one before exiting.
					if(deltaBeforeSegment + segmentDeltaTotal > spacing/2F){
						deltas.add(deltas.get(deltas.size() - 1));
					}
					totalDistance = 0;
				}
			}
			//Add the finalized delta list to the map.
			treadDeltas.put(treadPart.vehicle.definition.systemName, deltas);
		}
		
		
		float treadMovementPercentage = (float) ((Math.abs(treadPart.angularPosition) + treadPart.angularVelocity*partialTicks)*treadPart.getHeight()/Math.PI%treadPart.definition.ground.spacing/treadPart.definition.ground.spacing);
		if(treadPart.angularPosition < 0){
			treadMovementPercentage = 1 - treadMovementPercentage;
		}
		GL11.glPushMatrix();
		//First translate to the initial point.
		GL11.glTranslated(0, treadPart.placementOffset.y + treadPart.vehicleDefinition.treadYPoints[0], treadPart.placementOffset.z + treadPart.vehicleDefinition.treadZPoints[0]);
		//Next use the deltas to get the amount needed to translate and rotate each link.
		for(Float[] point : deltas){
			if(point[2] != 0){
				GL11.glRotatef(point[2], 1, 0, 0);
				GL11.glTranslatef(0, point[0]*treadMovementPercentage, point[1]*treadMovementPercentage);
				GL11.glRotatef(-point[2]*(1 - treadMovementPercentage), 1, 0, 0);
				GL11.glCallList(displayListIndex);
				GL11.glRotatef(point[2]*(1 - treadMovementPercentage), 1, 0, 0);
				GL11.glTranslatef(0, point[0]*(1 - treadMovementPercentage), point[1]*( 1 - treadMovementPercentage));
			}else{
				GL11.glTranslatef(0, point[0]*treadMovementPercentage, point[1]*treadMovementPercentage);
				GL11.glCallList(displayListIndex);
				GL11.glTranslatef(0, point[0]*(1 - treadMovementPercentage), point[1]*( 1 - treadMovementPercentage));
			}
			
		}
		GL11.glPopMatrix();
	}
	
	/**
	 *  Renders the treads using an automatic calculation system.
	 *  This is required to prevent the need to manually input a ton of points and reduce pack creator's work.
	 */
	private static void doAutomaticTreadRender(PartGroundDevice treadPart, float partialTicks, int displayListIndex){
		List<Double[]> points = treadPoints.get(treadPart.vehicle.definition.systemName);
		if(points == null){
			//If we don't have the deltas, calculate them based on the points of the rollers on the vehicle.			
			//Search through rotatable parts on the vehicle and grab the rollers.
			Map<Integer, TransformTreadRoller> parsedRollers = new HashMap<Integer, TransformTreadRoller>();
			for(RenderableModelObject modelObject : vehicleObjectLists.get(treadPart.vehicle.definition.systemName)){
				for(ATransform transform : modelObject.transforms){
					if(transform instanceof TransformTreadRoller){
						TransformTreadRoller treadTransform = (TransformTreadRoller) transform;
						parsedRollers.put(treadTransform.rollerNumber, treadTransform);
					}
				}
			}
			
			//Now that we have all the rollers, we can start calculating points.
			//First calculate the endpoints on the rollers by calling the calculation method.
			//We also transfer the rollers to an ordered array for convenience later.
			TransformTreadRoller[] rollers = new TransformTreadRoller[parsedRollers.size()];
			for(int i=0; i<parsedRollers.size(); ++ i){
				if(!parsedRollers.containsKey(i)){
					throw new IndexOutOfBoundsException("ERROR: Attempted to render roller_" + i + " on " + treadPart.vehicle.definition.packID + ":" + treadPart.vehicle.definition.systemName + ", but it was not found.  Did you not make it in the OBJ model?");
				}
				if(i < parsedRollers.size() - 1){
					parsedRollers.get(i).calculateEndpoints(parsedRollers.get(i + 1));
				}else{
					parsedRollers.get(i).calculateEndpoints(parsedRollers.get(0));
				}
				rollers[i] = parsedRollers.get(i);
			}
			
			//We need to ensure the endpoints are all angle-aligned.
			//It's possible to have a start angle of -181 and end angle of
			//181, which is really just 2 degress of angle (179-181).
			//To do this, we set the end angle of roller 0 and start
			//angle of roller 1 to be around 180, or downward-facing.
			//From there, we add angles to align things.
			//At the end, we should have an end angle of 540, or 180 + 360.
			rollers[0].endAngle = 180;
			for(int i=1; i<rollers.length; ++i){
				TransformTreadRoller roller = rollers[i];
				roller.startAngle = rollers[i - 1].endAngle;
				//End angle should be 0-360 greater than start angle, or within
				//30 degrees less, as is the case for concave rollers. 
				while(roller.endAngle < roller.startAngle - 30){
					roller.endAngle += 360;
				}
				while(roller.endAngle > roller.startAngle + 360){
					roller.endAngle += 360;
				}
			}
			//Set the end angle of the last roller, or start angle of the first roller, manually.
			//Need to get it between the value of 360 + 0-180 as that's where we will connect.
			while(rollers[0].startAngle < 0){
				rollers[0].startAngle += 360;
			}
			if(rollers[0].startAngle > 180){
				rollers[0].startAngle -= 360;
			}
			rollers[0].startAngle += 360;
			rollers[rollers.length - 1].endAngle = rollers[0].startAngle;
			
			
			//Now that the endpoints are set, we can calculate the path.
			//Do this by following the start and end points at small increments.
			//First calculate the total path length, and determine the optimum spacing.
			//This is the closest value to the definition's tread spacing.
			double totalPathLength = 0;
			for(int i=0; i<rollers.length; ++i){
				//Get roller and add roller path contribution.
				TransformTreadRoller roller = rollers[i];
				totalPathLength += 2*Math.PI*roller.radius*Math.abs(roller.endAngle - (i == 0 ? roller.startAngle - 360 : roller.startAngle))/360D;
				
				//Get next roller and add distance path contribution.
				//For points that start and end at an angle of around 0 (top of rollers) we add droop.
				//This is a hyperbolic function, so we need to calculate the integral value to account for the path.
				TransformTreadRoller nextRoller = i == rollers.length - 1 ? rollers[0] : rollers[i + 1];
				double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
				if(treadPart.vehicleDefinition.treadDroopConstant > 0 && (roller.endAngle%360 < 10 || roller.endAngle%360 > 350) && (nextRoller.startAngle%360 < 10 || nextRoller.startAngle%360 > 350)){
					totalPathLength += 2D*treadPart.vehicleDefinition.treadDroopConstant*Math.sinh(straightPathLength/2D/treadPart.vehicleDefinition.treadDroopConstant);
				}else{
					totalPathLength += straightPathLength;
				}
			}
			
			double deltaDist = treadPart.definition.ground.spacing + (totalPathLength%treadPart.definition.ground.spacing)/(totalPathLength/treadPart.definition.ground.spacing);
			double leftoverPathLength = 0;
			double yPoint = 0;
			double zPoint = 0; 
			points = new ArrayList<Double[]>();
			for(int i=0; i<rollers.length; ++i){
				TransformTreadRoller roller = rollers[i];
				//Follow the curve of the roller from the start and end point.
				//Do this until we don't have enough roller path left to make a point.
				//If we have any remaining path from a prior operation, we
				//need to offset our first point on the roller path to account for it.
				//It can very well be that this remainder will be more than the path length
				//of the roller.  If so, we just skip the roller entirely.
				//For the first roller we need to do some special math, as the angles will be inverted
				//For start and end due to the tread making a full 360 path.				
				double rollerPathLength = 2*Math.PI*roller.radius*Math.abs(roller.endAngle - (i == 0 ? roller.startAngle - 360 : roller.startAngle))/360D;
				double currentAngle = roller.startAngle;
				
				//Add the first point here, and add more as we follow the path.
				if(i == 0){
					yPoint = roller.yPos + roller.radius*Math.cos(Math.toRadians(currentAngle));
					zPoint = roller.zPos + roller.radius*Math.sin(Math.toRadians(currentAngle));
					points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
				}
				
				//If we have any leftover straight path, account for it here to keep spacing consistent.
				//We will need to interpolate the point that the straight path would have gone to, but
				//take our rotation angle into account.  Only do this if we have enough of a path to do so.
				//If not, we should just skip this roller as we can't put any points on it.
				if(deltaDist - leftoverPathLength < rollerPathLength){
					if(leftoverPathLength > 0){
						//Make a new point that's along a line from the last point and the start of this roller.
						//Then increment currentAngle to account for the new point made.
						//Add an angle relative to the point on the roller.
						Double[] lastPoint = points.get(points.size() - 1);
						yPoint = roller.yPos + roller.radius*Math.cos(Math.toRadians(currentAngle));
						zPoint = roller.zPos + roller.radius*Math.sin(Math.toRadians(currentAngle));
						double pointDist = Math.hypot(yPoint - lastPoint[0], zPoint - lastPoint[1]);
						double normalizedY = (yPoint - lastPoint[0])/pointDist;
						double normalizedZ = (zPoint - lastPoint[1])/pointDist;
						double rollerAngleSpan = 360D*((deltaDist - leftoverPathLength)/roller.circumference);
						
						points.add(new Double[]{lastPoint[0] + deltaDist*normalizedY, lastPoint[1] + deltaDist*normalizedZ, lastPoint[2] + rollerAngleSpan});
						currentAngle += rollerAngleSpan;
						rollerPathLength -= (deltaDist - leftoverPathLength);
						leftoverPathLength = 0;
					}
					
					while(rollerPathLength > deltaDist){
						//Go to and add the next point on the roller path.
						rollerPathLength -= deltaDist;
						currentAngle += 360D*(deltaDist/roller.circumference);
						yPoint = roller.yPos + roller.radius*Math.cos(Math.toRadians(currentAngle));
						zPoint = roller.zPos + roller.radius*Math.sin(Math.toRadians(currentAngle));
						points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
					}
					
					//Done following roller.  Set angle to end angle.
					currentAngle = roller.endAngle;
				}
				
				//If we have any leftover roller path, account for it here to keep spacing consistent.
				//We may also have leftover straight path length if we didn't do anything on a roller.
				//If we have roller length, make sure to offset it to account for the curvature of the roller.
				//If we don't do this, the line won't start at the end of the prior roller.
				//If we are on the last roller, we need to get the first roller to complete the loop.
				//For points that start and end at an angle of around 0 (top of rollers) we add droop.
				//This is a hyperbolic function, so we need to calculate the integral value to account for the path,
				//as well as model the function for the actual points.  This requires formula-driven points rather than normalization.
				TransformTreadRoller nextRoller = i == rollers.length - 1 ? rollers[0] : rollers[i + 1];
				double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
				double extraPathLength = rollerPathLength + leftoverPathLength;
				double normalizedY = (nextRoller.startY - roller.endY)/straightPathLength;
				double normalizedZ = (nextRoller.startZ - roller.endZ)/straightPathLength;
				if(treadPart.vehicleDefinition.treadDroopConstant > 0 && (roller.endAngle%360 < 10 || roller.endAngle%360 > 350) && (nextRoller.startAngle%360 < 10 || nextRoller.startAngle%360 > 350)){
					double hyperbolicPathLength = 2D*treadPart.vehicleDefinition.treadDroopConstant*Math.sinh(straightPathLength/2D/treadPart.vehicleDefinition.treadDroopConstant);
					double hyperbolicFunctionStep = deltaDist*straightPathLength/hyperbolicPathLength;
					double hyperbolicPathMaxY = treadPart.vehicleDefinition.treadDroopConstant*Math.cosh((-straightPathLength/2D)/treadPart.vehicleDefinition.treadDroopConstant);
					double hyperbolicFunctionCurrent = 0;
					while(straightPathLength + extraPathLength - hyperbolicFunctionCurrent > hyperbolicFunctionStep){
						//Go to and add the next point on the hyperbolic path.
						if(extraPathLength > 0){
							hyperbolicFunctionCurrent += extraPathLength*hyperbolicFunctionStep;
							extraPathLength = 0;
						}else{
							hyperbolicFunctionCurrent += hyperbolicFunctionStep;
						}
						yPoint = roller.endY + normalizedY*hyperbolicFunctionCurrent + treadPart.vehicleDefinition.treadDroopConstant*Math.cosh((hyperbolicFunctionCurrent - straightPathLength/2D)/treadPart.vehicleDefinition.treadDroopConstant) - hyperbolicPathMaxY;
						zPoint = roller.endZ + normalizedZ*hyperbolicFunctionCurrent;
						points.add(new Double[]{yPoint, zPoint, roller.endAngle + 180 - Math.toDegrees(Math.asin((hyperbolicFunctionCurrent - straightPathLength/2D)/treadPart.vehicleDefinition.treadDroopConstant))});
					}
					leftoverPathLength = (straightPathLength - hyperbolicFunctionCurrent)/(straightPathLength/hyperbolicPathLength);
				}else{
					while(straightPathLength + extraPathLength > deltaDist){
						//Go to and add the next point on the straight path.
						if(extraPathLength > 0){
							yPoint = roller.endY + normalizedY*(deltaDist - extraPathLength);
							zPoint = roller.endZ + normalizedZ*(deltaDist - extraPathLength);
							straightPathLength -= (deltaDist - extraPathLength);
							extraPathLength = 0;
						}else{
							yPoint += normalizedY*deltaDist;
							zPoint += normalizedZ*deltaDist;
							straightPathLength -= deltaDist;
						}
						points.add(new Double[]{yPoint, zPoint, roller.endAngle + 180});
					}
					leftoverPathLength = straightPathLength;
				}
			}
			treadPoints.put(treadPart.vehicle.definition.systemName, points);
		}
				
		//Render the treads along their points.
		//We manually set point 0 here due to the fact it's a joint between two differing angles.
		//We also need to translate to that point to start rendering as we're currently at 0,0,0.
		//For each remaining point, we only translate the delta of the point.
		float treadLinearPosition = (float) ((Math.abs(treadPart.angularPosition) + treadPart.angularVelocity*partialTicks)*treadPart.vehicle.SPEED_FACTOR);
		float treadMovementPercentage = treadLinearPosition%treadPart.definition.ground.spacing/treadPart.definition.ground.spacing;
		if(treadPart.angularPosition < 0){
			treadMovementPercentage = 1 - treadMovementPercentage;
		}
		Double[] point;
		Double[] nextPoint;
		double yDelta;
		double zDelta;
		double angleDelta;
		GL11.glPushMatrix();
		for(int i=0; i<points.size() - 1; ++i){
			//Update variables.
			//If we're at the last point, set the next point to the first point.
			point = points.get(i);
			if(i == points.size() - 1){
				nextPoint = points.get(0);
			}else{
				nextPoint = points.get(i + 1);
			}
			yDelta = nextPoint[0] - point[0];
			zDelta = nextPoint[1] - point[1];
			angleDelta = nextPoint[2] - point[2];
			
			//If we're at the first point, do initial translation.
			if(i == 0){
				GL11.glTranslated(0, point[0], point[1]);
			}
			
			//If our angle delta is greater than 180, we can assume that we're inverted.
			//This happens when we cross the 360 degree rotation barrier.
			if(angleDelta > 180){
				angleDelta -= 360;
			}else if(angleDelta < -180){
				angleDelta += 360;
			}
			
			//If there's no rotation to the point, and no delta between points, don't do rotation.
			//That's an expensive operation due to sin and cos operations.
			//Do note that the model needs to be flipped 180 on the X-axis due to all our points
			//assuming a YZ coordinate system with 0 degrees rotation being in +Y.
			//This is why 180 is added to all points cached in the operations above.
			if(point[2] != 0 || angleDelta != 0){
				//We can't use a running rotation here as we'll end up translating in the rotated
				//coordinate system.  To combat this, we translate like normal, but then push a
				//stack and rotate prior to rendering.  This keeps us from having to do another
				//rotation to get the old coordinate system back.
				GL11.glPushMatrix();
				GL11.glTranslated(0, yDelta*treadMovementPercentage, zDelta*treadMovementPercentage);
				GL11.glRotated(point[2] + angleDelta*treadMovementPercentage, 1, 0, 0);
				GL11.glCallList(displayListIndex);
				GL11.glPopMatrix();
				GL11.glTranslated(0, yDelta, zDelta);
			}else{
				//Translate to the current position of the tread based on the percent it has moved.
				//This is determined by partial ticks and actual tread position.
				//Once there, render the tread.  Then translate the remainder of the way to prepare
				//to render the next tread.
				GL11.glTranslated(0, yDelta*treadMovementPercentage, zDelta*treadMovementPercentage);
				GL11.glCallList(displayListIndex);
				GL11.glTranslated(0, yDelta*(1 - treadMovementPercentage), zDelta*(1 - treadMovementPercentage));
			}
		}
		GL11.glPopMatrix();
	}
	
	/**
	 *  Renders all instruments on the vehicle.  Uses the instrument's render code.
	 *  We only apply the appropriate translation and rotation.
	 *  Normalization is required here, as otherwise the normals get scaled with the
	 *  scaling operations, and shading gets applied funny. 
	 */
	private static void renderInstruments(EntityVehicleF_Physics vehicle){
		GL11.glEnable(GL11.GL_NORMALIZE);
		for(int i=0; i<vehicle.definition.motorized.instruments.size(); ++i){
			if(vehicle.instruments.containsKey(i)){
				PackInstrument packInstrument = vehicle.definition.motorized.instruments.get(i);
				
				//Translate and rotate to standard position.
				GL11.glPushMatrix();
				GL11.glTranslated(packInstrument.pos.x, packInstrument.pos.y, packInstrument.pos.z);
				GL11.glRotated(packInstrument.rot.x, 1, 0, 0);
				GL11.glRotated(packInstrument.rot.y, 0, 1, 0);
				GL11.glRotated(packInstrument.rot.z, 0, 0, 1);
				
				//Do transforms if required.
				RenderableTransform transform = vehicleInstrumentTransforms.get(vehicle.definition.systemName).get(i);
				boolean doRender = true;
				if(transform != null){
					doRender = transform.doPreRenderTransforms(vehicle, null, 0);
				}
				
				if(doRender){
					//Need to scale by -1 to get the coordinate system to behave and align to the texture-based coordinate system.
					GL11.glScalef(-packInstrument.scale/16F, -packInstrument.scale/16F, -packInstrument.scale/16F);
					
					//Render instrument.
					RenderInstrument.drawInstrument(vehicle.instruments.get(i), packInstrument.optionalPartNumber, vehicle);
				}
				
				if(transform != null){
					transform.doPostRenderTransforms(vehicle, null, 0);
				}
				GL11.glPopMatrix();
			}
		}
		GL11.glDisable(GL11.GL_NORMALIZE);
	}
	
	/**
	 *  Renders holographic part boxes when holding parts that can go on this vehicle.  This
	 *  needs to be rendered in pass 1 to do alpha blending.
	 */
	private static void renderPartBoxes(EntityVehicleF_Physics vehicle){
		if(MasterLoader.renderInterface.getRenderPass() != 0){
			//Disable lighting and texture rendering, and enable blending.
			MasterLoader.renderInterface.setLightingState(false);
			MasterLoader.renderInterface.setBlendState(true, false);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			
			//If we are holding a part, render the valid slots.
			//If we are holding a scanner, render all slots, but only render the looked-at one with items above it.
			IWrapperPlayer player = MasterLoader.gameInterface.getClientPlayer();
			AItemBase heldItem = player.getHeldItem();
			
			if(heldItem instanceof ItemPart){
				ItemPart heldPart = (ItemPart) heldItem;
				for(Entry<BoundingBox, VehiclePart> partSlotEntry : vehicle.activePartSlotBoxes.entrySet()){
					boolean isHoldingPart = false;
					boolean isPartValid = false;
					
					if(partSlotEntry.getValue().types.contains(heldPart.definition.general.type)){
						isHoldingPart = true;
						if(heldPart.isPartValidForPackDef(partSlotEntry.getValue())){
							isPartValid = true;
						}
					}
							
					if(isHoldingPart){
						if(isPartValid){
							MasterLoader.renderInterface.setColorState(0, 1, 0, 0.5F);
							RenderBoundingBox.renderSolid(partSlotEntry.getKey());
						}else{
							MasterLoader.renderInterface.setColorState(1, 0, 0, 0.5F);
							RenderBoundingBox.renderSolid(partSlotEntry.getKey());
						}
					}
				}
			}else if(heldItem instanceof ItemPartScanner){
				Point3d playerEyes = player.getPosition().add(0, player.getEyeHeight(), 0);
				Point3d playerLookVector = playerEyes.copy().add(new Point3d(0, 0, 10).rotateFine(new Point3d(player.getPitch(), player.getHeadYaw(), 0)));
				BoundingBox highlightedBox = null;
				for(Entry<BoundingBox, VehiclePart> partSlotEntry : vehicle.partSlotBoxes.entrySet()){
					MasterLoader.renderInterface.setColorState(0, 0, 1, 0.5F);
					BoundingBox currentBox = partSlotEntry.getKey();
					RenderBoundingBox.renderSolid(currentBox);
					if(currentBox.getIntersectionPoint(playerEyes, playerLookVector) != null){
						if(highlightedBox == null || (currentBox.globalCenter.distanceTo(playerEyes) < highlightedBox.globalCenter.distanceTo(playerEyes))){
							highlightedBox = currentBox;
						}
					}
				}
				
				if(highlightedBox != null){
					//Get the definition for this box.
					VehiclePart packVehicleDef = vehicle.partSlotBoxes.get(highlightedBox);
					
					//Set blending back to false and re-enable 2D texture before rendering item and text.
					MasterLoader.renderInterface.setBlendState(false, false);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					
					//Get all parts that go to this boxes position.
					List<ItemPart> validParts = new ArrayList<ItemPart>();
					for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
						if(packItem instanceof ItemPart){
							ItemPart part = (ItemPart) packItem;
							if(part.isPartValidForPackDef(packVehicleDef)){
								validParts.add(part);
							}
						}
					}
					
					//Render the type, min/max, and customTypes info.
					//To do this, we first need to translate to the top-center of the bounding box.
					//We also rotate to face the player.
					GL11.glPushMatrix();
					GL11.glTranslated(highlightedBox.globalCenter.x, highlightedBox.globalCenter.y + highlightedBox.heightRadius, highlightedBox.globalCenter.z);
					GL11.glRotated(player.getHeadYaw(), 0, 1, 0);
					
					//Rotate by 180 on the z-axis.  This changes the X and Y coords from GUI to world coords. 
					GL11.glRotated(180, 0, 0, 1);
					
					//Translate to the spot above where the item would render and render the standard text.
					GL11.glTranslated(0, -1.75F, 0);
					MasterLoader.guiInterface.drawScaledText("Types: " + packVehicleDef.types.toString(), 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
					GL11.glTranslated(0, 0.15F, 0);
					MasterLoader.guiInterface.drawScaledText("Min/Max: " + String.valueOf(packVehicleDef.minValue) + "/" + String.valueOf(packVehicleDef.maxValue), 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
					GL11.glTranslated(0, 0.15F, 0);
					if(packVehicleDef.customTypes != null){
						MasterLoader.guiInterface.drawScaledText("CustomTypes: " + packVehicleDef.customTypes.toString(), 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
					}else{
						MasterLoader.guiInterface.drawScaledText("CustomTypes: None", 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
					}
					GL11.glTranslated(0, 0.25F, 0);
					
					//If we have valid parts, render one of them.
					if(!validParts.isEmpty()){
						//Get current part to render based on the cycle.
						int cycle = player.isSneaking() ? 30 : 15;
						ItemPart partToRender = validParts.get((int) ((vehicle.world.getTime()/cycle)%validParts.size()));
						
						//If we are on the start of the cycle, beep.
						if(vehicle.world.getTime()%cycle == 0){
							MasterLoader.audioInterface.playQuickSound(new SoundInstance(vehicle, MasterLoader.resourceDomain + ":scanner_beep"));
						}
						
						//Render the part's name.
						MasterLoader.guiInterface.drawScaledText(partToRender.getItemName(), 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
						
						//Do translations to get to the center of where the item will render and render it.
						//Items also need to be offset by -150 units due to how MC does rendering.
						//Also need to translate to the center as items are rendered from the top-left corner.
						GL11.glTranslated(-0.5D, 0.25F, -150D/16D);
						MasterLoader.guiInterface.drawItem(MasterLoader.coreInterface.getStack(partToRender), 0, 0, 1F/16F);
					}
					GL11.glPopMatrix();
				}
			}
		}
	}
	
	/**
	 *  Renders the bounding boxes for the vehicle collision, and centers of all
	 *  parts currently on the vehicle.
	 */
	private static void renderBoundingBoxes(EntityVehicleF_Physics vehicle){
		//Set states for box render.
		MasterLoader.renderInterface.setLightingState(false);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		MasterLoader.renderInterface.setColorState(0.0F, 0.0F, 0.0F, 1.0F);
		GL11.glLineWidth(3.0F);
		
		//Draw collision boxes for the vehicle.
		for(BoundingBox box : vehicle.interactionBoxes){
			RenderBoundingBox.renderWireframe(box);
		}
		
		//Draw part center points.
		MasterLoader.renderInterface.setColorState(0.0F, 1.0F, 0.0F, 1.0F);
		GL11.glBegin(GL11.GL_LINES);
		for(APart part : vehicle.parts){
			Point3d partRotatedCenter = part.totalOffset.copy().rotateCoarse(vehicle.angles);
			GL11.glVertex3d(partRotatedCenter.x, partRotatedCenter.y - part.getHeight(), partRotatedCenter.z);
			GL11.glVertex3d(partRotatedCenter.x, partRotatedCenter.y + part.getHeight(), partRotatedCenter.z);
		}
		GL11.glEnd();
		GL11.glLineWidth(1.0F);
	}
}
