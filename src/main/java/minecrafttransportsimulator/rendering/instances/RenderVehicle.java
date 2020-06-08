package minecrafttransportsimulator.rendering.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Vector3d;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.PackInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDisplayText;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.rendering.components.ARenderableTransform;
import minecrafttransportsimulator.rendering.components.RenderTickData;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;
import minecrafttransportsimulator.rendering.components.TransformLight;
import minecrafttransportsimulator.rendering.components.TransformRotatable;
import minecrafttransportsimulator.rendering.components.TransformTranslatable;
import minecrafttransportsimulator.rendering.components.TransformTreadRoller;
import minecrafttransportsimulator.systems.ClientEventSystem;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.systems.VehicleEffectsSystem.FXPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDeviceTread;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperGame;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperRender;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**Main render class for all vehicles.  Renders the vehicle, along with all parts.
 * As entities don't render above 255 well due to the new chunk visibility system, 
 * this code is called both from the regular render loop and manually from
 * {@link ClientEventSystem#on(RenderWorldLastEvent)}.
 *
 * @author don_bruce
 */
public final class RenderVehicle extends Render<EntityVehicleE_Powered>{	
	//VEHICLE MAPS.  Maps are keyed by generic name.
	private static final Map<String, Integer> vehicleDisplayLists = new HashMap<String, Integer>();
	private static final Map<String, String> vehicleModelOverrides = new HashMap<String, String>();
	private static final Map<String, List<RenderableModelObject>> vehicleObjectLists = new HashMap<String, List<RenderableModelObject>>();
	private static final Map<String, List<Float[]>> treadDeltas = new HashMap<String, List<Float[]>>();
	private static final Map<String, List<Double[]>> treadPoints = new HashMap<String, List<Double[]>>();
	
	//PART MAPS.  Maps are keyed by the part model name.
	private static final Map<String, Integer> partDisplayLists = new HashMap<String, Integer>();
	private static final Map<String, List<RenderableModelObject>> partObjectLists = new HashMap<String, List<RenderableModelObject>>();
	
	//RENDER DATA MAPS.  Keyed by each instance of each vehicle loaded.
	private static final Map<EntityVehicleE_Powered, RenderTickData> renderData = new HashMap<EntityVehicleE_Powered, RenderTickData>();
	
	public RenderVehicle(RenderManager renderManager){
		super(renderManager);
	}
	
	/**Used to clear out the rendering caches of the passed-in vehicle in dev mode to allow the re-loading of models.**/
	public static void clearVehicleCaches(EntityVehicleE_Powered vehicle){
		vehicleDisplayLists.remove(vehicle.definition.genericName);
		for(RenderableModelObject modelObject : vehicleObjectLists.get(vehicle.definition.genericName)){
			modelObject.resetDisplayList();
		}
		vehicleObjectLists.remove(vehicle.definition.genericName);
		
		vehicleObjectLists.remove(vehicle.definition.genericName);
		treadDeltas.remove(vehicle.definition.genericName);
		treadPoints.remove(vehicle.definition.genericName);
	}
	
	/**
	 * Used to inject a new model into the model map for vehicles.
	 * Allow for hotloading models outside of the normal jar locations.
	 **/
	public static void injectModel(EntityVehicleE_Powered vehicle, String modelLocation){
		vehicleModelOverrides.put(vehicle.definition.genericName, modelLocation);
	}
	
	@Override
	protected ResourceLocation getEntityTexture(EntityVehicleE_Powered entity){
		return null;
	}
	
	@Override
	public void doRender(EntityVehicleE_Powered vehicle, double x, double y, double z, float entityYaw, float partialTicks){
		if(vehicle.definition != null){
			//If we don't have render data yet, create one now.
			if(!renderData.containsKey(vehicle)){
				renderData.put(vehicle, new RenderTickData(new WrapperWorld(vehicle.world)));
			}
			
			//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
			int renderPass = WrapperRender.getRenderPass();
			if(renderPass == -1){
				renderPass = 2;
			}
			
			//If we need to render, do so now.
			if(renderData.get(vehicle).shouldRender(renderPass, partialTicks)){
				render(vehicle, partialTicks);
			}
		}
	}
	
	public static boolean doesVehicleHaveLight(EntityVehicleE_Powered vehicle, LightType light){
		for(RenderableModelObject modelObject : vehicleObjectLists.get(vehicle.definition.genericName)){
			for(ARenderableTransform transform : modelObject.transforms){
				if(transform instanceof TransformLight){
					if(((TransformLight) transform).type.equals(light)){
						return true;
					}
				}
			}
		}
		for(APart part : vehicle.getVehicleParts()){
			if(partObjectLists.containsKey(part.getModelLocation())){
				for(RenderableModelObject modelObject : partObjectLists.get(part.getModelLocation())){
					for(ARenderableTransform transform : modelObject.transforms){
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
     * Checks if lights are on for this vehicle and instruments need to be lit up.
     */
	public static boolean isVehicleIlluminated(EntityVehicleE_Powered vehicle){
		return (vehicle.lightsOn.contains(LightType.NAVIGATIONLIGHT) || vehicle.lightsOn.contains(LightType.RUNNINGLIGHT) || vehicle.lightsOn.contains(LightType.HEADLIGHT)) && vehicle.electricPower > 3;
	}
	
	/**
	 *  Renders the vehicle in its entirety.  Rendering happens normally in pass 0 (solid) and 1 (transparent), but may happen in the
	 *  special pass -1 (end) if the vehicle wasn't rendered in either pass 0 or 1 due to chunk render culling.  Some rendering routines
	 *  only run on specific passes, so see the comments on the called methods for information on what is rendered when.
	 */
	private static void render(EntityVehicleE_Powered vehicle, float partialTicks){
		//Set render camera position.
		Vector3d renderPosition = WrapperGame.getRenderViewEntity().getRenderedPosition(partialTicks);
		
		//Get vehicle position and rotation.
        double vehicleX = vehicle.lastTickPosX + (vehicle.posX - vehicle.lastTickPosX) * partialTicks;
        double vehicleY = vehicle.lastTickPosY + (vehicle.posY - vehicle.lastTickPosY) * partialTicks;
        double vehicleZ = vehicle.lastTickPosZ + (vehicle.posZ - vehicle.lastTickPosZ) * partialTicks;
		double rotateYaw = -vehicle.rotationYaw + (vehicle.rotationYaw - vehicle.prevRotationYaw)*(double)(1 - partialTicks);
        double rotatePitch = vehicle.rotationPitch - (vehicle.rotationPitch - vehicle.prevRotationPitch)*(double)(1 - partialTicks);
        double rotateRoll = vehicle.rotationRoll - (vehicle.rotationRoll - vehicle.prevRotationRoll)*(double)(1 - partialTicks);
       
        //Set up lighting.
        WrapperRender.setLightingToVehicle(vehicle);
        
        //Use smooth shading for main model rendering.
		GL11.glShadeModel(GL11.GL_SMOOTH);
        
        //Push the matrix on the stack and translate and rotate to the vehicle's position.
        GL11.glPushMatrix();
        GL11.glTranslated(vehicleX - renderPosition.x, vehicleY - renderPosition.y, vehicleZ - renderPosition.z);
        
        GL11.glPushMatrix();
        GL11.glRotated(rotateYaw, 0, 1, 0);
        GL11.glRotated(rotatePitch, 1, 0, 0);
        GL11.glRotated(rotateRoll, 0, 0, 1);
		
        //Render the main model.
		renderMainModel(vehicle, partialTicks);
		
		//Render all the parts.  Parts get translated to their offset position prior to rendering.
		for(APart part : vehicle.getVehicleParts()){
			//Only render valid parts that aren't sub parts.  SubParts need to be rendered relative to their main part.
			if(part.isValid() && !part.vehicleDefinition.isSubPart){
				GL11.glPushMatrix();
				if(part instanceof PartGroundDeviceTread){
					//Treads don't get translated by y, or z.
					GL11.glTranslated(part.placementOffset.x, 0, 0);
					renderPart(part, partialTicks, part.placementOffset);
				}else{
					Vec3d offset = part.placementOffset.add(part.getPositionOffset(partialTicks));
					GL11.glTranslated(offset.x, offset.y, offset.z);
					renderPart(part, partialTicks, offset);
				}
				GL11.glPopMatrix();
			}
		}
		
		//Set shading back to normal now that all model bits have been rendered.
		GL11.glShadeModel(GL11.GL_FLAT);
		
		//Render all instruments on the vehicle.
		renderInstruments(vehicle);
		
		//Render text markings.
		renderTextMarkings(vehicle);
		
		//Render holograms for missing parts.
		renderPartBoxes(vehicle);
		
		//Pop vehicle rotation matrix as the following calls use world coords.
		GL11.glPopMatrix();
		
		//Check to see if we need to manually render riders.
		//This happens if we force-render this vehicle in pass -1.
		if(WrapperRender.getRenderPass() == -1){
			WrapperRender.renderVehicleRiders(vehicle, partialTicks);
		}
		
		//Render bounding boxes for parts and collision points.
		if(WrapperRender.shouldRenderBoundingBoxes()){
			renderBoundingBoxes(vehicle);
		}
		
		//Pop vehicle translation matrix and reset all states.
		GL11.glPopMatrix();
		WrapperRender.resetStates();
		
		//Spawn particles, but only once per render cycle.
		if(WrapperRender.getRenderPass() != 1 && !WrapperGame.isGamePaused()){
			for(APart part : vehicle.getVehicleParts()){
				if(part instanceof FXPart){
					((FXPart) part).spawnParticles();
				}
			}
		}
	}
	
	/**
	 *  Renders the main vehicle model.  The model file is determined from the general name of the JSON, which is really
	 *  just the JSON's file name.  Vehicle model is first translated to the position of the vehicle in the world,
	 *  rotated to the roll, pitch, and yaw, of the vehicle, and then all static portions are rendered.  Dynamic
	 *  animated portions like {@link TransformRotatable}s, {@link TransformTranslatable}s, and
	 *  {@link WindowPart}s are rendered after this with their respective transformations applied.  All renders are
	 *  cached in DisplayLists, as we only need to translate and rotate them, not apply any transforms or splits.
	 *  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 */
	private static void renderMainModel(EntityVehicleE_Powered vehicle, float partialTicks){
		//Normally we use the pack name, but since all displaylists
		//are the same for all models, this is more appropriate.
		if(!vehicleDisplayLists.containsKey(vehicle.definition.genericName)){
			//No distplay list for this model.  Parse and create it now.
			Map<String, Float[][]> parsedModel;
			if(vehicleModelOverrides.containsKey(vehicle.definition.genericName)){
				parsedModel = OBJParserSystem.parseOBJModel(null, vehicleModelOverrides.get(vehicle.definition.genericName));
			}else{
				parsedModel = OBJParserSystem.parseOBJModel(vehicle.definition.packID, "objmodels/vehicles/" + vehicle.definition.genericName + ".obj");
			}
			
			//Set up the Gl operations and lists.
			List<RenderableModelObject> modelObjects = new ArrayList<RenderableModelObject>();
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				//Create model objects for all components of the model.
				//If we have any objects that don't have transforms, ignore them.
				RenderableModelObject modelObject = new RenderableModelObject(vehicle.definition.genericName, entry.getKey(), entry.getValue(), vehicle, null);
				if(modelObject.transforms.isEmpty()){
					for(Float[] vertex : entry.getValue()){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}
				}else{
					modelObjects.add(modelObject);
				}
			}
			GL11.glEnd();
			GL11.glEndList();
			
			//Now finalize the maps.
			vehicleDisplayLists.put(vehicle.definition.genericName, displayListIndex);
			vehicleObjectLists.put(vehicle.definition.genericName, modelObjects);
		}
		
		//Bind the texture and render.
		//Don't render on the transparent pass.
		WrapperRender.setTexture(vehicle.definition.packID, "textures/vehicles/" + vehicle.definition.systemName + ".png");
		if(WrapperRender.getRenderPass() != 1){
			GL11.glCallList(vehicleDisplayLists.get(vehicle.definition.genericName));
		}
		
		//The display list only renders static objects.  We need to render dynamic ones manually.
		for(RenderableModelObject modelObject : vehicleObjectLists.get(vehicle.definition.genericName)){
			modelObject.render(vehicle, null, partialTicks);
		}
	}
	
	/**
	 *  Renders all parts on the vehicle.  Parts are first translated to their actual position, which they keep track of.
	 *  After this they are rotated via {@link #rotatePart(APart, Vec3d, boolean)}.  Finally, any parts of the part
	 *  model that are {@link TransformRotatable}s or {@link TransformTranslatable}s are rendered with
	 *  their rotations applied.  This makes rendering a split process.  Translate to position, rotate at position,
	 *  render static portions of part model, apply transforms to animated portions of the part model, and then
	 *  render the animated portions.  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 */
	private static void renderPart(APart part, float partialTicks, Vec3d offset){
		String partModelLocation = part.getModelLocation();
		if(!partDisplayLists.containsKey(partModelLocation)){
			//Create the part display list and modelObjects.
			List<RenderableModelObject> modelObjects = new ArrayList<RenderableModelObject>();
			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(part.definition.packID, partModelLocation);
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				//Create model objects for all components of the model.
				//If we have any objects that don't have transforms, ignore them.
				RenderableModelObject modelObject = new RenderableModelObject(partModelLocation, entry.getKey(), entry.getValue(), part.vehicle, part);
				if(modelObject.transforms.isEmpty()){
					for(Float[] vertex : entry.getValue()){
						GL11.glTexCoord2f(vertex[3], vertex[4]);
						GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
						GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
					}
				}else{
					modelObjects.add(modelObject);
				}
			}
			GL11.glEnd();
			GL11.glEndList();
			
			//Now finalize the maps
			partDisplayLists.put(partModelLocation, displayListIndex);
			partObjectLists.put(partModelLocation, modelObjects);
		}
		
		//If we aren't using the vehicle texture, bind the texture for this part.
		//Otherwise, bind the vehicle texture as it may have been un-bound prior to this from another part.
		if(!part.definition.general.useVehicleTexture){
			WrapperRender.setTexture(part.definition.packID, part.getTextureLocation());
		}else{
			WrapperRender.setTexture(part.vehicle.definition.packID, "textures/vehicles/" + part.vehicle.definition.systemName + ".png");
		}
		
		//Rotate the part prior to rendering the displayList.
		//We will already have been translated to our position prior to this call.
		GL11.glPushMatrix();
		rotatePart(part, partialTicks);
		
		//If we are a tread, do the tread-specific render rather than the display list.
		if(part instanceof PartGroundDeviceTread){
			if(part.vehicleDefinition.treadZPoints != null){
				doManualTreadRender((PartGroundDeviceTread) part, partialTicks, partDisplayLists.get(partModelLocation));	
			}else{
				doAutomaticTreadRender((PartGroundDeviceTread) part, partialTicks, partDisplayLists.get(partModelLocation));
			}
		}else{
    		//Render the part DisplayList.
			GL11.glCallList(partDisplayLists.get(partModelLocation));
			
			//The display list only renders static object.  We need to render dynamic ones manually.
			for(RenderableModelObject modelObject : partObjectLists.get(partModelLocation)){
				modelObject.render(part.vehicle, part, partialTicks);
			}
			
			//Now that we have rendered this part, render any sub-part children.
			//Make sure to translate "back" to the vehicle center prior to rendering.
			//This allows the subParts to translate to their correct positions.
			GL11.glTranslated(-offset.x, -offset.y, -offset.z);
			for(APart childPart : part.childParts){
				if(childPart.vehicleDefinition.isSubPart){
					//Get the relative distance between our offset and our parent's offset.
					Vec3d relativeOffset = childPart.placementOffset.add(childPart.getPositionOffset(partialTicks)).subtract(part.placementOffset);
					
					//Rotate by the parent's rotation to match orientation.
					Vec3d parentRotation = part.getPositionRotation(partialTicks);
					relativeOffset = RotationSystem.getRotatedPoint(relativeOffset, (float) parentRotation.x, (float) parentRotation.y, (float) parentRotation.z);
					
					//Add parent offset to our offset to get actual point.
					Vec3d totalOffset = offset.add(relativeOffset);
					GL11.glPushMatrix();
					GL11.glTranslated(totalOffset.x, totalOffset.y, totalOffset.z);
					renderPart(childPart, partialTicks, totalOffset);
					GL11.glPopMatrix();
				}
			}
		}
		
		//Set cull face back to normal.  This may have been changed
		//during rotation operations.
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glPopMatrix();
	}
	
	/**
	 *  Rotates a part on the model.  The rotation takes into account the vehicle, static, JSON-applied rotation, 
	 *  as well as the dynamic rotation that depend on the part itself.  Rotation needs to be done after translation
	 *   to the part's position to avoid coordinate system conflicts. 
	 */
	private static void rotatePart(APart part, float partialTicks){
		boolean mirrored = ((part.placementOffset.x < 0 && !part.vehicleDefinition.inverseMirroring) || (part.placementOffset.x > 0 && part.vehicleDefinition.inverseMirroring)) && !part.disableMirroring; 
		if(mirrored){
			GL11.glScalef(-1.0F, 1.0F, 1.0F);
			GL11.glCullFace(GL11.GL_FRONT);
		}
		
		if(!part.placementRotation.equals(Vec3d.ZERO)){
			if(part.parentPart != null){
				GL11.glRotated(part.placementRotation.z, -Math.sin(Math.toRadians(part.parentPart.getActionRotation(0).y)), 0, Math.cos(Math.toRadians(part.parentPart.getActionRotation(0).y)));
				GL11.glRotated(part.placementRotation.y, 0, 1, 0);
				GL11.glRotated(part.placementRotation.x, Math.cos(Math.toRadians(part.parentPart.getActionRotation(0).y)), 0, Math.sin(Math.toRadians(part.parentPart.getActionRotation(0).y)));
			}else{
				GL11.glRotated(part.placementRotation.x, 1, 0, 0);
				GL11.glRotated(part.placementRotation.y, 0, 1, 0);
				GL11.glRotated(part.placementRotation.z, 0, 0, 1);
			}
		}
		
		Vec3d positionRotation = part.getPositionRotation(partialTicks);
		if(!positionRotation.equals(Vec3d.ZERO)){
			if(mirrored){
				GL11.glRotated(-positionRotation.y, 0, 1, 0);
				GL11.glRotated(-positionRotation.x, 1, 0, 0);
				GL11.glRotated(-positionRotation.z, 0, 0, 1);
			}else{
				GL11.glRotated(positionRotation.y, 0, 1, 0);
				GL11.glRotated(positionRotation.x, 1, 0, 0);
				GL11.glRotated(positionRotation.z, 0, 0, 1);
			}
		}

		Vec3d actionRotation = part.getActionRotation(partialTicks);
		if(!actionRotation.equals(Vec3d.ZERO)){
			GL11.glRotated(actionRotation.y, 0, 1, 0);
			GL11.glRotated(actionRotation.x, 1, 0, 0);
			GL11.glRotated(actionRotation.z, 0, 0, 1);
		}
	}
	
	/**
	 *  Renders the treads using a manual system.  Points are defined by pack authors and are located in the
	 *  vehicle JSON.  This method is more cumbersome for the authors, but allows for precise path control.
	 */
	private static void doManualTreadRender(PartGroundDeviceTread treadPart, float partialTicks, int displayListIndex){
		List<Float[]> deltas = treadDeltas.get(treadPart.vehicle.definition.genericName);
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
			final float spacing = treadPart.definition.tread.spacing;
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
			treadDeltas.put(treadPart.vehicle.definition.genericName, deltas);
		}
		
		
		float treadMovementPercentage = (float) ((Math.abs(treadPart.angularPosition) + treadPart.angularVelocity*partialTicks)*treadPart.getHeight()/Math.PI%treadPart.definition.tread.spacing/treadPart.definition.tread.spacing);
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
	 *  Renders the treads using an automatic calculation system.  This system is good for simple treads,
	 *  though will render oddly on complex paths.
	 */
	private static void doAutomaticTreadRender(PartGroundDeviceTread treadPart, float partialTicks, int displayListIndex){
		List<Double[]> points = treadPoints.get(treadPart.vehicle.definition.genericName);
		if(points == null){
			//If we don't have the deltas, calculate them based on the points of the rollers on the vehicle.			
			//Search through rotatable parts on the vehicle and grab the rollers.
			Map<Integer, TransformTreadRoller> parsedRollers = new HashMap<Integer, TransformTreadRoller>();
			for(RenderableModelObject modelObject : vehicleObjectLists.get(treadPart.vehicle.definition.genericName)){
				for(ARenderableTransform transform : modelObject.transforms){
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
					throw new IndexOutOfBoundsException("ERROR: Attempted to render roller_" + i + " on " + treadPart.vehicle.definition.packID + ":" + treadPart.vehicle.definition.genericName + ", but it was not found.  Did you not make it in the OBJ model?");
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
				TransformTreadRoller roller = rollers[i];
				totalPathLength += 2*Math.PI*roller.radius*Math.abs(roller.endAngle - (i == 0 ? roller.startAngle - 360 : roller.startAngle))/360D;
				TransformTreadRoller nextRoller = i == rollers.length - 1 ? rollers[0] : rollers[i + 1];
				totalPathLength += Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
			}
			
			double deltaDist = treadPart.definition.tread.spacing + (totalPathLength%treadPart.definition.tread.spacing)/(totalPathLength/treadPart.definition.tread.spacing);
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
				//If we are on the last roller, we need to get the first roller to complete the loop.
				TransformTreadRoller nextRoller = i == rollers.length - 1 ? rollers[0] : rollers[i + 1];
				double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ) + leftoverPathLength + rollerPathLength;
				double normalizedY = (nextRoller.startY - yPoint)/straightPathLength;
				double normalizedZ = (nextRoller.startZ - zPoint)/straightPathLength;
				while(straightPathLength > deltaDist){
					//Go to and add the next point on the straight path.
					straightPathLength -= deltaDist;
					yPoint += normalizedY*deltaDist;
					zPoint += normalizedZ*deltaDist;
					points.add(new Double[]{yPoint, zPoint, roller.endAngle + 180});
				}
				leftoverPathLength = straightPathLength;
			}
			
			treadPoints.put(treadPart.vehicle.definition.genericName, points);
		}
				
		//Render the treads along their points.
		//We manually set point 0 here due to the fact it's a joint between two differing angles.
		//We also need to translate to that point to start rendering as we're currently at 0,0,0.
		//For each remaining point, we only translate the delta of the point.
		float treadLinearPosition = (float) ((Math.abs(treadPart.angularPosition) + treadPart.angularVelocity*partialTicks)*treadPart.vehicle.SPEED_FACTOR);
		float treadMovementPercentage = treadLinearPosition%treadPart.definition.tread.spacing/treadPart.definition.tread.spacing;
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
	 */
	private static void renderInstruments(EntityVehicleE_Powered vehicle){
		for(byte i=0; i<vehicle.definition.motorized.instruments.size(); ++i){
			PackInstrument packInstrument = vehicle.definition.motorized.instruments.get(i);
			GL11.glPushMatrix();
			GL11.glTranslatef(packInstrument.pos[0], packInstrument.pos[1], packInstrument.pos[2]);
			GL11.glRotatef(packInstrument.rot[0], 1, 0, 0);
			GL11.glRotatef(packInstrument.rot[1], 0, 1, 0);
			GL11.glRotatef(packInstrument.rot[2], 0, 0, 1);
			//Need to scale by -1 to get the coordinate system to behave and align to the texture-based coordinate system.
			GL11.glScalef(-packInstrument.scale/16F, -packInstrument.scale/16F, -packInstrument.scale/16F);
			if(vehicle.instruments.containsKey(i)){
				RenderInstrument.drawInstrument(vehicle.instruments.get(i), packInstrument.optionalPartNumber, vehicle);
			}
			GL11.glPopMatrix();
		}
	}
	
	/**
	 *  Renders all text markings for this vehicle.  Text is changed via the player,
	 *  and may be lit up if configured to do so in the JSON.  This should only be called 
	 *  in pass 0, as we don't do any alpha blending in this routine.
	 */
	private static void renderTextMarkings(EntityVehicleE_Powered vehicle){
		if(WrapperRender.getRenderPass() != 1){
			//Disable system lighting as we have issues with it in 3D rendering.
			WrapperRender.setSystemLightingState(false);
			
			//If we have light-up text, disable lightmap too.
			if(vehicle.definition.rendering.textLighted && isVehicleIlluminated(vehicle)){
				WrapperRender.setInternalLightingState(false);
			}
			
			//Render all text strings.
			for(VehicleDisplayText text : vehicle.definition.rendering.textMarkings){
				GL11.glPushMatrix();
				GL11.glTranslatef(text.pos[0], text.pos[1], text.pos[2]);
				GL11.glScalef(1F/16F, 1F/16F, 1F/16F);
				//First rotate 180 along the X-axis to get us rendering right-side up.
				GL11.glRotatef(180F, 1, 0, 0);
				
				//Next, apply rotations.  Only doing so if they exist.
				//Apply the Y-rotation first as it will always be used and allows for correct X-rotations.
				if(text.rot[1] != 0){
					GL11.glRotatef(-text.rot[1], 0, 1, 0);
				}
				if(text.rot[0] != 0){
					GL11.glRotatef(text.rot[0], 1, 0, 0);
				}
				if(text.rot[2] != 0){
					GL11.glRotatef(text.rot[2], 0, 0, 1);
				}
				
				//Finally, scale and render the text.
				GL11.glScalef(text.scale, text.scale, text.scale);
				WrapperGUI.drawScaledText(vehicle.displayText, 0, 0, Color.decode(text.color), true, false, 0, text.scale);
				GL11.glPopMatrix();
			}
		}
	}
	
	/**
	 *  Renders holographic part boxes when holding parts that can go on this vehicle.  This
	 *  needs to be rendered in pass 1 to do alpha blending.
	 */
	private static void renderPartBoxes(EntityVehicleE_Powered vehicle){
		if(WrapperRender.getRenderPass() != 0){
			//Disable lighting and texture rendering, and enable blending.
			WrapperRender.setLightingState(false);
			WrapperRender.setBlendState(true, false);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			
			//Enable blending if on pass -1 for transparency.
			if(WrapperRender.getRenderPass() == -1){
				WrapperRender.setBlendState(true, false);
			}
			
			WrapperPlayer player = WrapperGame.getClientPlayer();
			ItemStack heldStack = player.getHeldStack();
			if(heldStack != null){
				if(heldStack.getItem() instanceof AItemPart){
					AItemPart heldItem = (AItemPart) heldStack.getItem();
					for(Entry<Vec3d, VehiclePart> packPartEntry : vehicle.getAllPossiblePackParts().entrySet()){
						boolean isPresent = false;
						boolean isHoldingPart = false;
						boolean isPartValid = false;
						
						if(vehicle.getPartAtLocation(packPartEntry.getKey().x, packPartEntry.getKey().y, packPartEntry.getKey().z) != null){
							isPresent = true;
						}
						
						if(packPartEntry.getValue().types.contains(heldItem.definition.general.type)){
							isHoldingPart = true;
							if(heldItem.isPartValidForPackDef(packPartEntry.getValue())){
								isPartValid = true;
							}
						}
								
						if(!isPresent && isHoldingPart){
							Vec3d offset = packPartEntry.getKey();
							//If we are a custom part, use the custom hitbox.  Otherwise use the regular one.
							AxisAlignedBB box;
							if(packPartEntry.getValue().types.contains("custom") && heldItem.definition.general.type.equals("custom")){
								box = new AxisAlignedBB((float) (offset.x) - heldItem.definition.custom.width/2F, (float) (offset.y) - heldItem.definition.custom.height/2F, (float) (offset.z) - heldItem.definition.custom.width/2F, (float) (offset.x) + heldItem.definition.custom.width/2F, (float) (offset.y) + heldItem.definition.custom.height/2F, (float) (offset.z) + heldItem.definition.custom.width/2F);		
							}else{
								box = new AxisAlignedBB((float) (offset.x) - 0.375F, (float) (offset.y) - 0.5F, (float) (offset.z) - 0.375F, (float) (offset.x) + 0.375F, (float) (offset.y) + 1.25F, (float) (offset.z) + 0.375F);
							}
							
							if(isPartValid){
								WrapperRender.setColorState(0, 1, 0, 0.5F);
							}else{
								WrapperRender.setColorState(1, 0, 0, 0.5F);
							}
							GL11.glBegin(GL11.GL_QUADS);
							
							GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
							GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
							GL11.glVertex3d(box.minX, box.minY, box.maxZ);
							GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
							
							GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
							GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
							GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
							GL11.glVertex3d(box.maxX, box.minY, box.minZ);
							
							GL11.glVertex3d(box.maxX, box.minY, box.minZ);
							GL11.glVertex3d(box.minX, box.minY, box.minZ);
							GL11.glVertex3d(box.minX, box.maxY, box.minZ);
							GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
							
							GL11.glVertex3d(box.minX, box.minY, box.minZ);
							GL11.glVertex3d(box.minX, box.minY, box.maxZ);
							GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
							GL11.glVertex3d(box.minX, box.maxY, box.minZ);
							
							GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
							GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
							GL11.glVertex3d(box.minX, box.maxY, box.minZ);
							GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
							
							GL11.glVertex3d(box.minX, box.minY, box.maxZ);
							GL11.glVertex3d(box.minX, box.minY, box.minZ);
							GL11.glVertex3d(box.maxX, box.minY, box.minZ);
							GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
							GL11.glEnd();
						}
					}
				}
			}
		}
	}
	
	/**
	 *  Renders the bounding boxes for the vehicle collision, and centers of all
	 *  parts currently on the vehicle.
	 */
	private static void renderBoundingBoxes(EntityVehicleE_Powered vehicle){
		//Set states for box render.
		WrapperRender.setLightingState(false);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		WrapperRender.setColorState(0.0F, 0.0F, 0.0F, 1.0F);
		GL11.glLineWidth(3.0F);
		
		//Draw collision boxes for the vehicle.
		for(VehicleAxisAlignedBB box : vehicle.collisionBoxes){
			Vec3d boxOffset = box.pos.subtract(vehicle.posX, vehicle.posY, vehicle.posZ);
			GL11.glBegin(GL11.GL_LINES);
			//Bottom
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			
			//Top
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			
			//Vertical sides.
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z - box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x - box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y - box.height/2F, boxOffset.z + box.width/2F);
			GL11.glVertex3d(boxOffset.x + box.width/2F, boxOffset.y + box.height/2F, boxOffset.z + box.width/2F);
			GL11.glEnd();
		}
		
		//Draw part center points.
		WrapperRender.setColorState(0.0F, 1.0F, 0.0F, 1.0F);
		GL11.glBegin(GL11.GL_LINES);
		for(APart part : vehicle.getVehicleParts()){
			GL11.glVertex3d(part.worldPos.x - vehicle.posX, part.worldPos.y - vehicle.posY - part.getHeight(), part.worldPos.z - vehicle.posZ);
			GL11.glVertex3d(part.worldPos.x - vehicle.posX, part.worldPos.y - vehicle.posY + part.getHeight(), part.worldPos.z - vehicle.posZ);
		}
		GL11.glEnd();
		GL11.glLineWidth(1.0F);
	}
}
