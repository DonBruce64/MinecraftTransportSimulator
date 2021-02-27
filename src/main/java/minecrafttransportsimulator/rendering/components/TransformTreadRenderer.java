package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;

/**This transform is designed to render tread paths on whatever part
 * it is placed on.  Used in conjunction with {@link TransformTreadRoller}
 * objects to define a roller path, this transform does the actual tread rendering.
 *
 * @author don_bruce
 */
public class TransformTreadRenderer<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	
	private final int displayListIndex;
	
	//Map of tread points, keyed by the vehicle the tread is on, or the parent part of the part, if this tread is a subPart.
	private static final Map<String, List<Double[]>> treadPoints = new HashMap<String, List<Double[]>>();
	
	public TransformTreadRenderer(int displayListIndex){
		super(null);
		this.displayListIndex = displayListIndex;
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, float partialTicks){
		//If we are a ground device, and we aren't a spare, do tread-path rendering.
		//Otherwise, do normal rendering by returning true.
		PartGroundDevice tread = (PartGroundDevice) entity;
		if(tread.definition.ground != null && tread.definition.ground.isTread && !tread.placementDefinition.isSpare){
			AEntityC_Definable<?> entityTreadAttachedTo = tread.placementDefinition.isSubPart ? tread.parentPart : tread.entityOn;
			String treadPathModel = entityTreadAttachedTo.definition.getModelLocation(); 
			List<Double[]> points = treadPoints.get(treadPathModel);
			
			if(points == null){
				//If we don't have the deltas, calculate them based on the points of the rollers on the model.			
				//Search through rotatable parts on the model and grab the rollers.
				Map<Integer, TransformTreadRoller<EntityVehicleF_Physics>> parsedRollers = new HashMap<Integer, TransformTreadRoller<EntityVehicleF_Physics>>();
				Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(treadPathModel);
				for(RenderableModelObject<EntityVehicleF_Physics> modelObject : OBJParser.generateRenderables((EntityVehicleF_Physics) tread.entityOn, tread.entityOn.definition.getModelLocation(), parsedModel, tread.entityOn.definition.rendering.animatedObjects)){
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
						throw new IndexOutOfBoundsException("Attempted to render roller_" + i + " on " + entityTreadAttachedTo.definition.packID + ":" + entityTreadAttachedTo.definition.systemName + ", but it was not found.  Did you not make it in the OBJ model?");
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
					if(tread.placementDefinition.treadDroopConstant > 0 && (roller.endAngle%360 < 10 || roller.endAngle%360 > 350) && (nextRoller.startAngle%360 < 10 || nextRoller.startAngle%360 > 350)){
						totalPathLength += 2D*tread.placementDefinition.treadDroopConstant*Math.sinh(straightPathLength/2D/tread.placementDefinition.treadDroopConstant);
					}else{
						totalPathLength += straightPathLength;
					}
				}
				
				double deltaDist = tread.definition.ground.spacing + (totalPathLength%tread.definition.ground.spacing)/(totalPathLength/tread.definition.ground.spacing);
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
					if(tread.placementDefinition.treadDroopConstant > 0 && (roller.endAngle%360 < 10 || roller.endAngle%360 > 350) && (nextRoller.startAngle%360 < 10 || nextRoller.startAngle%360 > 350)){
						double hyperbolicPathLength = 2D*tread.placementDefinition.treadDroopConstant*Math.sinh(straightPathLength/2D/tread.placementDefinition.treadDroopConstant);
						double hyperbolicFunctionStep = deltaDist*straightPathLength/hyperbolicPathLength;
						double hyperbolicPathMaxY = tread.placementDefinition.treadDroopConstant*Math.cosh((-straightPathLength/2D)/tread.placementDefinition.treadDroopConstant);
						double hyperbolicFunctionCurrent = 0;
						while(straightPathLength + extraPathLength - hyperbolicFunctionCurrent > hyperbolicFunctionStep){
							//Go to and add the next point on the hyperbolic path.
							if(extraPathLength > 0){
								hyperbolicFunctionCurrent += extraPathLength*hyperbolicFunctionStep;
								extraPathLength = 0;
							}else{
								hyperbolicFunctionCurrent += hyperbolicFunctionStep;
							}
							yPoint = roller.endY + normalizedY*hyperbolicFunctionCurrent + tread.placementDefinition.treadDroopConstant*Math.cosh((hyperbolicFunctionCurrent - straightPathLength/2D)/tread.placementDefinition.treadDroopConstant) - hyperbolicPathMaxY;
							zPoint = roller.endZ + normalizedZ*hyperbolicFunctionCurrent;
							points.add(new Double[]{yPoint, zPoint, roller.endAngle + 180 - Math.toDegrees(Math.asin((hyperbolicFunctionCurrent - straightPathLength/2D)/tread.placementDefinition.treadDroopConstant))});
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
				treadPoints.put(treadPathModel, points);
			}
					
			//Render the treads along their points.
			//We manually set point 0 here due to the fact it's a joint between two differing angles.
			//We also need to translate to that point to start rendering as we're currently at 0,0,0.
			//For each remaining point, we only translate the delta of the point.
			float treadLinearPosition = (float) ((Math.abs(tread.angularPosition) + tread.angularVelocity*partialTicks)*EntityVehicleF_Physics.SPEED_FACTOR);
			float treadMovementPercentage = treadLinearPosition%tread.definition.ground.spacing/tread.definition.ground.spacing;
			if(tread.angularPosition < 0){
				treadMovementPercentage = 1 - treadMovementPercentage;
			}
			Double[] point;
			Double[] nextPoint;
			double yDelta;
			double zDelta;
			double angleDelta;
			GL11.glPushMatrix();
			
			//Tread rendering is done via the thing the tread is on, which will assume the part is centered at 0, 0, 0.
			//We need to undo the offset of the tread part for this routine.
			Point3d delta = entityTreadAttachedTo instanceof APart ? ((APart) entityTreadAttachedTo).localOffset.copy() : new Point3d();
			delta.subtract(tread.localOffset);
			GL11.glTranslated(0, delta.y, delta.z);
			
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
		}else{
			//Return super.
			return super.shouldRender(entity, partialTicks);
		}
		return false;
	}
	
	@Override
	public double applyTransform(AnimationEntity provider, float partialTicks, double offset){
		//Do nothing here.
		return 0;
	}
}
