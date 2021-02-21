package minecrafttransportsimulator.rendering.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.rendering.components.ATransform;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;
import minecrafttransportsimulator.rendering.components.TransformLight;
import minecrafttransportsimulator.rendering.components.TransformTreadRoller;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;

public final class RenderPart extends ARenderEntity<APart>{	
	//PART MAPS.  Maps are keyed by the part model name.
	private static final Map<String, Integer> partDisplayLists = new HashMap<String, Integer>();
	private static final Map<String, List<RenderableModelObject<APart>>> partObjectLists = new HashMap<String, List<RenderableModelObject<APart>>>();
	@Deprecated
	private static final Map<String, List<Float[]>> treadDeltas = new HashMap<String, List<Float[]>>();
	private static final Map<String, List<Double[]>> treadPoints = new HashMap<String, List<Double[]>>();
	
	/**Used to clear out the rendering caches of any parts with the passed-in definition.
	 * Used in dev mode to allow the re-loading of models.**/
	public static void clearPartCaches(JSONPart definition){
		String modelName = definition.getModelLocation();
		if(partDisplayLists.containsKey(modelName)){
			GL11.glDeleteLists(partDisplayLists.remove(modelName), 1);
		}
		if(partObjectLists.containsKey(modelName)){
			for(RenderableModelObject<APart> modelObject : partObjectLists.get(modelName)){
				modelObject.resetDisplayList();
			}
			partObjectLists.remove(definition.systemName);
		}
	}
	
	public static boolean doesPartHaveLight(APart part, LightType light){
		String partModelLocation = part.definition.getModelLocation();
		if(partObjectLists.containsKey(partModelLocation)){
			for(RenderableModelObject<APart> modelObject : partObjectLists.get(partModelLocation)){
				for(ATransform<APart> transform : modelObject.transforms){
					if(transform instanceof TransformLight){
						if(((TransformLight<APart>) transform).type.equals(light)){
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public void renderModel(APart part, float partialTicks){
		if(!part.isFake()){
			//Check to see if the part has a visibility animation and it's set to not be visible.
			if(part.placementDefinition.animations != null){
				for(JSONAnimationDefinition animation : part.placementDefinition.animations){
					if(animation.animationType.equals(AnimationComponentType.VISIBILITY)){
						double value = part.getAnimator().getAnimatedVariableValue(part, animation, 0, null, partialTicks);
						if(value < animation.clampMin || value > animation.clampMax){
							return;
						}
					}
				}
			}
			
			//Not a fake part, do rendering.
			GL11.glPushMatrix();
			if(part.definition.ground != null && (part.definition.ground.isTread && !part.placementDefinition.isSpare)){
				//Tread rendering is done via the vehicle, which will assume the part is centered at 0, 0, 0.
				//We need to undo the offset of the tread part for this routine.
				GL11.glTranslated(0, -part.placementOffset.y, -part.placementOffset.z);
			}
			
			String partModelLocation = part.definition.getModelLocation();
			if(!partDisplayLists.containsKey(partModelLocation)){
				Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(partModelLocation);
				partObjectLists.put(partModelLocation, OBJParser.generateRenderables(part, partModelLocation, parsedModel, part.definition.rendering != null ? part.definition.rendering.animatedObjects : null));
				partDisplayLists.put(partModelLocation, OBJParser.generateDisplayList(parsedModel));
			}
			
			//If we aren't using the vehicle texture, bind the texture for this part.
			//Otherwise, bind the vehicle texture as it may have been un-bound prior to this from another part.
			if(!part.definition.generic.useVehicleTexture){
				InterfaceRender.setTexture(part.definition.getTextureLocation(part.subName));
			}else{
				InterfaceRender.setTexture(part.entityOn.definition.getTextureLocation(part.entityOn.subName));
			}
			
			//Rotate the part prior to rendering the displayList.
			//We will already have been translated to our position prior to this call.
			Point3d renderingRotation = part.getRenderingRotation(partialTicks, false);
			if(!renderingRotation.isZero()){
				GL11.glRotated(renderingRotation.y, 0, 1, 0);
				GL11.glRotated(renderingRotation.x, 1, 0, 0);
				GL11.glRotated(renderingRotation.z, 0, 0, 1);
			}
			
			//Mirror the model if we need to do so.
			//If we are a sub-part, don't mirror as we'll already be mirrored.
			boolean mirrored = ((part.placementOffset.x < 0 && !part.placementDefinition.inverseMirroring) || (part.placementOffset.x >= 0 && part.placementDefinition.inverseMirroring)) && !part.disableMirroring;
			if(mirrored){
				GL11.glScalef(-1.0F, 1.0F, 1.0F);
				GL11.glCullFace(GL11.GL_FRONT);
			}
			
			//If we are a tread (that's not spare), do the tread-specific render rather than the display list.
			//Don't do this for pass 1 though as treads don't have transparency.
			if(part.definition.ground != null && part.definition.ground.isTread && !part.placementDefinition.isSpare && InterfaceRender.getRenderPass() != 1){
				if(part.placementDefinition.treadZPoints != null){
					doManualTreadRender((PartGroundDevice) part, partialTicks, partDisplayLists.get(partModelLocation));	
				}else{
					doAutomaticTreadRender((PartGroundDevice) part, partialTicks, partDisplayLists.get(partModelLocation));
				}
			}else{
	    		//Render the part DisplayList, but only if we aren't in the transparent pass.
				if(InterfaceRender.getRenderPass() != 1){
					GL11.glCallList(partDisplayLists.get(partModelLocation));
				}
				
				//Render any static text.
				if(InterfaceRender.renderTextMarkings(part, null)){
					InterfaceRender.recallTexture();
				}
				
				//The display list only renders static object.  We need to render dynamic ones manually.
				List<RenderableModelObject<APart>> modelObjects = partObjectLists.get(partModelLocation);
				for(RenderableModelObject<APart> modelObject : modelObjects){
					if(modelObject.applyAfter == null){
						modelObject.render(part, partialTicks, modelObjects);
					}
				}
			}
			//Set cullface back to normal if we switched it and pop matrix.
			if(mirrored){
				GL11.glCullFace(GL11.GL_BACK);
			}
			GL11.glPopMatrix();
		}
	}
	
	/**
	 *  Renders the treads using a manual system.  Points are defined by pack authors and are located in the
	 *  vehicle JSON.  This method is more cumbersome for the authors, but allows for precise path control.
	 */
	@Deprecated
	private static void doManualTreadRender(PartGroundDevice treadPart, float partialTicks, int displayListIndex){
		List<Float[]> deltas = treadDeltas.get(treadPart.entityOn.definition.systemName);
		if(deltas == null){
			//First calculate the total distance the treads need to be rendered.
			float totalDistance = 0;
			float lastY = treadPart.placementDefinition.treadYPoints[0];
			float lastZ = treadPart.placementDefinition.treadZPoints[0];
			for(byte i=1; i<treadPart.placementDefinition.treadYPoints.length; ++i){
				totalDistance += Math.hypot((treadPart.placementDefinition.treadYPoints[i] - lastY), (treadPart.placementDefinition.treadYPoints[i] - lastZ));
				lastY = treadPart.placementDefinition.treadYPoints[i];
				lastZ = treadPart.placementDefinition.treadZPoints[i];
			}
			
			//Now that we have the total distance, generate a set of points for the path.
			//These points should be as far apart as the spacing parameter.
			deltas = new ArrayList<Float[]>();
			final float spacing = treadPart.definition.ground.spacing;
			byte pointIndex = 0;
			float currentY = treadPart.placementDefinition.treadYPoints[pointIndex];
			float currentZ = treadPart.placementDefinition.treadZPoints[pointIndex];
			float nextY = treadPart.placementDefinition.treadYPoints[pointIndex + 1];
			float nextZ = treadPart.placementDefinition.treadZPoints[pointIndex + 1];
			float deltaYBeforeSegment = 0;
			float deltaZBeforeSegment = 0;
			float deltaBeforeSegment = 0;
			float segmentDeltaY = (nextY - currentY);
			float segmentDeltaZ = (nextZ - currentZ);
			float segmentDeltaTotal = (float) Math.hypot(segmentDeltaY, segmentDeltaZ);
			float angle = treadPart.placementDefinition.treadAngles[pointIndex];
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
					if(pointIndex + 1 == treadPart.placementDefinition.treadYPoints.length){
						currentY = treadPart.placementDefinition.treadYPoints[pointIndex];
						currentZ = treadPart.placementDefinition.treadZPoints[pointIndex];
						nextY = treadPart.placementDefinition.treadYPoints[0];
						nextZ = treadPart.placementDefinition.treadZPoints[0];
						//Ensure we rotate the angle by the correct amount for the joint.
						//It's possible that we will add a negative angle here due to going from something like 270 to 0.
						//This will cause a -270 rotation rather than the +30 we want.
						float angleToAdd = treadPart.placementDefinition.treadAngles[0] - treadPart.placementDefinition.treadAngles[pointIndex];
						while(angleToAdd < 0){
							angleToAdd += 360; 
						}
						angle += angleToAdd;
					}else if(pointIndex + 1 > treadPart.placementDefinition.treadYPoints.length){
						break;
					}else{
						currentY = treadPart.placementDefinition.treadYPoints[pointIndex];
						currentZ = treadPart.placementDefinition.treadZPoints[pointIndex];
						nextY = treadPart.placementDefinition.treadYPoints[pointIndex + 1];
						nextZ = treadPart.placementDefinition.treadZPoints[pointIndex + 1];
						angle += treadPart.placementDefinition.treadAngles[pointIndex] - treadPart.placementDefinition.treadAngles[pointIndex - 1];
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
			treadDeltas.put(treadPart.entityOn.definition.systemName, deltas);
		}
		
		
		float treadMovementPercentage = (float) ((Math.abs(treadPart.angularPosition) + treadPart.angularVelocity*partialTicks)*treadPart.getHeight()/Math.PI%treadPart.definition.ground.spacing/treadPart.definition.ground.spacing);
		if(treadPart.angularPosition < 0){
			treadMovementPercentage = 1 - treadMovementPercentage;
		}
		GL11.glPushMatrix();
		//First translate to the initial point.
		GL11.glTranslated(0, treadPart.placementOffset.y + treadPart.placementDefinition.treadYPoints[0], treadPart.placementOffset.z + treadPart.placementDefinition.treadZPoints[0]);
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
		List<Double[]> points = treadPoints.get(treadPart.entityOn.definition.systemName);
		if(points == null){
			//If we don't have the deltas, calculate them based on the points of the rollers on the vehicle.			
			//Search through rotatable parts on the vehicle and grab the rollers.
			Map<Integer, TransformTreadRoller<EntityVehicleF_Physics>> parsedRollers = new HashMap<Integer, TransformTreadRoller<EntityVehicleF_Physics>>();
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(treadPart.entityOn.definition.getModelLocation());
			for(RenderableModelObject<EntityVehicleF_Physics> modelObject : OBJParser.generateRenderables((EntityVehicleF_Physics) treadPart.entityOn, treadPart.entityOn.definition.getModelLocation(), parsedModel, treadPart.entityOn.definition.rendering.animatedObjects)){
				for(ATransform<EntityVehicleF_Physics> transform : modelObject.transforms){
					if(transform instanceof TransformTreadRoller){
						TransformTreadRoller<EntityVehicleF_Physics> treadTransform = (TransformTreadRoller<EntityVehicleF_Physics>) transform;
						parsedRollers.put(treadTransform.rollerNumber, treadTransform);
					}
				}
			}
			
			//Now that we have all the rollers, we can start calculating points.
			//First calculate the endpoints on the rollers by calling the calculation method.
			//We also transfer the rollers to an ordered array for convenience later.
			List<TransformTreadRoller<EntityVehicleF_Physics>> rollers = new ArrayList<TransformTreadRoller<EntityVehicleF_Physics>>();
			for(int i=0; i<parsedRollers.size(); ++ i){
				if(!parsedRollers.containsKey(i)){
					throw new IndexOutOfBoundsException("Attempted to render roller_" + i + " on " + treadPart.entityOn.definition.packID + ":" + treadPart.entityOn.definition.systemName + ", but it was not found.  Did you not make it in the OBJ model?");
				}
				if(i < parsedRollers.size() - 1){
					parsedRollers.get(i).calculateEndpoints(parsedRollers.get(i + 1));
				}else{
					parsedRollers.get(i).calculateEndpoints(parsedRollers.get(0));
				}
				rollers.add(parsedRollers.get(i));
			}
			
			//We need to ensure the endpoints are all angle-aligned.
			//It's possible to have a start angle of -181 and end angle of
			//181, which is really just 2 degress of angle (179-181).
			//To do this, we set the end angle of roller 0 and start
			//angle of roller 1 to be around 180, or downward-facing.
			//From there, we add angles to align things.
			//At the end, we should have an end angle of 540, or 180 + 360.
			rollers.get(0).endAngle = 180;
			for(int i=1; i<rollers.size(); ++i){
				TransformTreadRoller<EntityVehicleF_Physics> roller = rollers.get(i);
				roller.startAngle = rollers.get(i - 1).endAngle;
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
			while(rollers.get(0).startAngle < 0){
				rollers.get(0).startAngle += 360;
			}
			if(rollers.get(0).startAngle > 180){
				rollers.get(0).startAngle -= 360;
			}
			rollers.get(0).startAngle += 360;
			rollers.get(rollers.size() - 1).endAngle = rollers.get(0).startAngle;
			
			
			//Now that the endpoints are set, we can calculate the path.
			//Do this by following the start and end points at small increments.
			//First calculate the total path length, and determine the optimum spacing.
			//This is the closest value to the definition's tread spacing.
			double totalPathLength = 0;
			for(int i=0; i<rollers.size(); ++i){
				//Get roller and add roller path contribution.
				TransformTreadRoller<EntityVehicleF_Physics> roller = rollers.get(i);
				totalPathLength += 2*Math.PI*roller.radius*Math.abs(roller.endAngle - (i == 0 ? roller.startAngle - 360 : roller.startAngle))/360D;
				
				//Get next roller and add distance path contribution.
				//For points that start and end at an angle of around 0 (top of rollers) we add droop.
				//This is a hyperbolic function, so we need to calculate the integral value to account for the path.
				TransformTreadRoller<EntityVehicleF_Physics> nextRoller = i == rollers.size() - 1 ? rollers.get(0) : rollers.get(i + 1);
				double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
				if(treadPart.placementDefinition.treadDroopConstant > 0 && (roller.endAngle%360 < 10 || roller.endAngle%360 > 350) && (nextRoller.startAngle%360 < 10 || nextRoller.startAngle%360 > 350)){
					totalPathLength += 2D*treadPart.placementDefinition.treadDroopConstant*Math.sinh(straightPathLength/2D/treadPart.placementDefinition.treadDroopConstant);
				}else{
					totalPathLength += straightPathLength;
				}
			}
			
			double deltaDist = treadPart.definition.ground.spacing + (totalPathLength%treadPart.definition.ground.spacing)/(totalPathLength/treadPart.definition.ground.spacing);
			double leftoverPathLength = 0;
			double yPoint = 0;
			double zPoint = 0; 
			points = new ArrayList<Double[]>();
			for(int i=0; i<rollers.size(); ++i){
				TransformTreadRoller<EntityVehicleF_Physics> roller = rollers.get(i);
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
				TransformTreadRoller<EntityVehicleF_Physics> nextRoller = i == rollers.size() - 1 ? rollers.get(0) : rollers.get(i + 1);
				double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
				double extraPathLength = rollerPathLength + leftoverPathLength;
				double normalizedY = (nextRoller.startY - roller.endY)/straightPathLength;
				double normalizedZ = (nextRoller.startZ - roller.endZ)/straightPathLength;
				if(treadPart.placementDefinition.treadDroopConstant > 0 && (roller.endAngle%360 < 10 || roller.endAngle%360 > 350) && (nextRoller.startAngle%360 < 10 || nextRoller.startAngle%360 > 350)){
					double hyperbolicPathLength = 2D*treadPart.placementDefinition.treadDroopConstant*Math.sinh(straightPathLength/2D/treadPart.placementDefinition.treadDroopConstant);
					double hyperbolicFunctionStep = deltaDist*straightPathLength/hyperbolicPathLength;
					double hyperbolicPathMaxY = treadPart.placementDefinition.treadDroopConstant*Math.cosh((-straightPathLength/2D)/treadPart.placementDefinition.treadDroopConstant);
					double hyperbolicFunctionCurrent = 0;
					while(straightPathLength + extraPathLength - hyperbolicFunctionCurrent > hyperbolicFunctionStep){
						//Go to and add the next point on the hyperbolic path.
						if(extraPathLength > 0){
							hyperbolicFunctionCurrent += extraPathLength*hyperbolicFunctionStep;
							extraPathLength = 0;
						}else{
							hyperbolicFunctionCurrent += hyperbolicFunctionStep;
						}
						yPoint = roller.endY + normalizedY*hyperbolicFunctionCurrent + treadPart.placementDefinition.treadDroopConstant*Math.cosh((hyperbolicFunctionCurrent - straightPathLength/2D)/treadPart.placementDefinition.treadDroopConstant) - hyperbolicPathMaxY;
						zPoint = roller.endZ + normalizedZ*hyperbolicFunctionCurrent;
						points.add(new Double[]{yPoint, zPoint, roller.endAngle + 180 - Math.toDegrees(Math.asin((hyperbolicFunctionCurrent - straightPathLength/2D)/treadPart.placementDefinition.treadDroopConstant))});
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
			treadPoints.put(treadPart.entityOn.definition.systemName, points);
		}
				
		//Render the treads along their points.
		//We manually set point 0 here due to the fact it's a joint between two differing angles.
		//We also need to translate to that point to start rendering as we're currently at 0,0,0.
		//For each remaining point, we only translate the delta of the point.
		float treadLinearPosition = (float) ((Math.abs(treadPart.angularPosition) + treadPart.angularVelocity*partialTicks)*EntityVehicleF_Physics.SPEED_FACTOR);
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
}
