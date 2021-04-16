package minecrafttransportsimulator.rendering.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLaneConnection;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.systems.ConfigSystem;

public class RenderRoad extends ARenderTileEntityBase<TileEntityRoad>{
	private static final Map<TileEntityRoad, Map<RoadComponent, Integer>> roadDisplayListMap = new HashMap<TileEntityRoad, Map<RoadComponent, Integer>>();
	
	@Override
	public void renderAdditionalModels(TileEntityRoad road, boolean blendingEnabled, float partialTicks){
		//Render road components.
		//First set helper variables.
		Point3d position = new Point3d();
		Point3d rotation = new Point3d();
		
		//If we haven't rendered the road yet, do so now.
		//We cache it in a DisplayList, as there are a LOT of transforms done each component.
		if(!roadDisplayListMap.containsKey(road)){
			roadDisplayListMap.put(road, new HashMap<RoadComponent, Integer>());
		}
		
		//If the road is inactive, we render everything as a hologram.
		if(!road.isActive()){
			if(blendingEnabled){
				InterfaceRender.setTextureState(false);
				InterfaceRender.setColorState(0.0F, 1.0F, 0.0F, 0.5F);
			}else{
				return;
			}
		}
		
		Map<RoadComponent, Integer> displayListMap = roadDisplayListMap.get(road);
		for(RoadComponent component : road.components.keySet()){
			ItemRoadComponent componentItem = road.components.get(component);
			
			if(!displayListMap.containsKey(component)){
				int displayListIndex = GL11.glGenLists(1);
				GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
				switch(component){
					case CORE: {
						Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(componentItem.definition.getModelLocation());
						GL11.glBegin(GL11.GL_TRIANGLES);
						
						//If we are a dynamic curve, cache the dynamic vertex paths.
						//If we are static, just render the model as-is.
						if(road.definition.road.isDynamic && road.dynamicCurve != null){
							//Core components need to be transformed to wedges.
							List<Float[]> transformedVertices = new ArrayList<Float[]>();
							Point3d priorPosition = new Point3d();
							Point3d priorRotation = new Point3d();
							Point3d rotationDelta = new Point3d();
							float priorIndex = 0;
							
							for(float currentIndex=1; currentIndex<=road.dynamicCurve.pathLength; ++currentIndex){
								//Copy the master vertices to our transformed ones.
								transformedVertices.clear();
								for(Float[][] vertexSet : parsedModel.values()){
									for(Float[] vertex : vertexSet){
										transformedVertices.add(new Float[]{vertex[0], vertex[1], vertex[2], vertex[3], vertex[4], vertex[5], vertex[6], vertex[7]});
									}
								}
								
								//Get current and prior curve position and rotation.
								//From this, we know how much to stretch the model to that point's rendering area.
								road.dynamicCurve.setPointToPositionAt(priorPosition, priorIndex);
								road.dynamicCurve.setPointToRotationAt(priorRotation, priorIndex);
								road.dynamicCurve.setPointToPositionAt(position, currentIndex);
								road.dynamicCurve.setPointToRotationAt(rotation, currentIndex);
								
								//If we are a really sharp curve, we might have inverted our model at the inner corner.
								//Check for this, and if we have done so, skip this segment.
								//If we detect this in the last 3 segments, skip right to the end.
								//This prevents a missing end segment due to collision.
								rotationDelta.setTo(rotation).subtract(priorRotation);
								Point3d testPoint1 = new Point3d(road.definition.road.borderOffset, 0, 0).rotateFine(priorRotation).add(priorPosition);
								Point3d testPoint2 = new Point3d(road.definition.road.borderOffset, 0, 0).rotateFine(rotation).add(position);
								if(currentIndex != road.dynamicCurve.pathLength && (position.x - priorPosition.x)*(testPoint2.x - testPoint1.x) < 0 || (position.z - priorPosition.z)*(testPoint2.z - testPoint1.z) < 0){
									if(currentIndex != road.dynamicCurve.pathLength && currentIndex + 3 > road.dynamicCurve.pathLength){
										currentIndex = road.dynamicCurve.pathLength - 1;
									}
									continue;
								}
								
								//Depending on the vertex position in the model, transform it to match with the offset rotation.
								//This depends on how far the vertex is from the origin of the model, and how big the delta is.
								//For all points, their magnitude depends on how far away they are on the Z-axis.
								for(Float[] vertex : transformedVertices){
									Point3d vertexOffsetPrior = new Point3d(vertex[0], vertex[1], 0);
									vertexOffsetPrior.rotateFine(priorRotation).add(priorPosition);
									Point3d vertexOffsetCurrent = new Point3d(vertex[0], vertex[1], vertex[2]);
									vertexOffsetCurrent.rotateFine(rotation).add(position);
									
									Point3d segmentVector = vertexOffsetPrior.copy().subtract(vertexOffsetCurrent).multiply(Math.abs(vertex[2]));
									Point3d renderedVertex = vertexOffsetCurrent.copy().add(segmentVector);
									
									GL11.glTexCoord2f(vertex[3], vertex[4]);
									GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
									GL11.glVertex3d(renderedVertex.x, renderedVertex.y, renderedVertex.z);
								}
								
								//Set the last index.
								priorIndex = currentIndex;
								
								//If we are at the last index, do special logic to get the very end point.
								if(currentIndex != road.dynamicCurve.pathLength && currentIndex + 1 > road.dynamicCurve.pathLength){
									currentIndex -= ((currentIndex + 1) - road.dynamicCurve.pathLength);
								}
							}
						}else if(!road.definition.road.isDynamic){
							for(Float[][] vertexSet : parsedModel.values()){
								for(Float[] vertex : vertexSet){
									GL11.glTexCoord2f(vertex[3], vertex[4]);
									GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
									//Need to offset by 0.5 to match the offset of the TE as we're block-aligned.
									position.set(vertex[0] - 0.5, vertex[1], vertex[2] - 0.5);
									position.rotateFine(road.rotation);
									GL11.glVertex3d(position.x, position.y, position.z);
								}
							}
						}
						GL11.glEnd();
					}
					case LEFT_BORDER:
						break;
					case RIGHT_BORDER:
						break;
					case LEFT_MARKING:
						break;
					case CENTER_MARKING:
						break;
					case RIGHT_MARKING:
						break;
					case SUPPORT:
						break;
					case UNDERLAYMENT:
						break;
					default:
						break;
				}
				GL11.glEndList();
				displayListMap.put(component, displayListIndex);
			}
			
			if(road.isActive()){
				InterfaceRender.bindTexture(componentItem.definition.getTextureLocation(componentItem.subName));
			}
			GL11.glCallList(displayListMap.get(component));
		}
		
		//If we are inactive render the blocking blocks and the main block.
		if(!road.isActive()){
			InterfaceRender.setColorState(1.0F, 0.0F, 0.0F, 0.5F);
			for(Point3d location : road.collidingBlockOffsets){
				BoundingBox blockingBox = new BoundingBox(location.copy().add(0, 0.5, 0), 0.55, 0.55, 0.55);
				GL11.glPushMatrix();
				GL11.glTranslated(location.x, location.y, location.z);
				RenderBoundingBox.renderSolid(blockingBox);
				GL11.glPopMatrix();
			}
			InterfaceRender.setColorState(0.0F, 0.0F, 1.0F, 0.5F);
			BoundingBox mainBlockBox = new BoundingBox(new Point3d(0, 0.75, 0), 0.15, 1.5, 0.15);
			GL11.glPushMatrix();
			GL11.glTranslated(mainBlockBox.localCenter.x, mainBlockBox.localCenter.y, mainBlockBox.localCenter.z);
			RenderBoundingBox.renderSolid(mainBlockBox);
			GL11.glPopMatrix();
		}
		
		//If we are in devMode, render road bounds and colliding boxes.
		if(ConfigSystem.configObject.clientControls.devMode.value){
			//Render the information hashes.
			//First set states.
			InterfaceRender.setTextureState(false);
			InterfaceRender.setSystemLightingState(false);
			GL11.glLineWidth(2);
			GL11.glBegin(GL11.GL_LINES);
			
			//Render the curves.
			//First render the actual curve if we are a dynamic road.
			if(road.dynamicCurve != null){
				//Render actual curve.
				InterfaceRender.setColorState(0, 1, 0, 1);
				for(float f=0; f<road.dynamicCurve.pathLength; f+=0.1){
					road.dynamicCurve.setPointToPositionAt(position, f);
					GL11.glVertex3d(position.x, position.y, position.z);
					GL11.glVertex3d(position.x, position.y + 1.0, position.z);
				}
				
				//Render the outer border bounds.
				InterfaceRender.setColorState(0, 1, 1, 1);
				for(float f=0; f<road.dynamicCurve.pathLength; f+=0.1){
					road.dynamicCurve.setPointToRotationAt(rotation, f);
					position.set(road.definition.road.borderOffset, 0, 0).rotateFine(rotation);
					road.dynamicCurve.offsetPointByPositionAt(position, f);
					
					GL11.glVertex3d(position.x, position.y, position.z);
					GL11.glVertex3d(position.x, position.y + 1.0, position.z);
				}
			}
			
			//Now render the lane curve segments.
			for(RoadLane lane : road.lanes){
				for(BezierCurve laneCurve : lane.curves){
					//Render the curve bearing indicator
					InterfaceRender.setColorState(1, 0, 0, 1);
					GL11.glVertex3d(laneCurve.startPos.x, laneCurve.startPos.y, laneCurve.startPos.z);
					GL11.glVertex3d(laneCurve.startPos.x, laneCurve.startPos.y + 3, laneCurve.startPos.z);
					GL11.glVertex3d(laneCurve.startPos.x, laneCurve.startPos.y + 3, laneCurve.startPos.z);
					Point3d bearingPos = laneCurve.endPos.copy().subtract(laneCurve.startPos).normalize().add(laneCurve.startPos);
					GL11.glVertex3d(bearingPos.x, bearingPos.y + 3, bearingPos.z);
					
					//Render all the points on the curve.
					InterfaceRender.setColorState(1, 1, 0, 1);
					laneCurve.setPointToPositionAt(position, 0);
					for(float f=0; f<laneCurve.pathLength; f+=0.1){
						laneCurve.setPointToPositionAt(position, f);
						GL11.glVertex3d(position.x, position.y, position.z);
						GL11.glVertex3d(position.x, position.y + 1.0, position.z);
					}
				}
			}
			
			//Render the lane connections.
			InterfaceRender.setColorState(1.0F, 0.7F, 0.7F, 1);
			for(RoadLane lane : road.lanes){
				for(List<RoadLaneConnection> curvePriorConnections : lane.priorConnections){
					BezierCurve currentCurve = lane.curves.get(lane.priorConnections.indexOf(curvePriorConnections));
					for(RoadLaneConnection priorConnection : curvePriorConnections){
						TileEntityRoad otherRoad = road.world.getTileEntity(priorConnection.tileLocation);
						if(otherRoad != null){
							RoadLane otherLane = otherRoad.lanes.get(priorConnection.laneNumber);
							if(otherLane != null){
								BezierCurve otherCurve = otherLane.curves.get(priorConnection.curveNumber);
								
								//First render our own offset point.
								currentCurve.setPointToPositionAt(position, 0.5F);
								GL11.glVertex3d(position.x, position.y + 3.0, position.z);
								GL11.glVertex3d(position.x, position.y + 0.5, position.z);
								GL11.glVertex3d(position.x, position.y + 0.5, position.z);
								
								//Now render the connection point.
								otherCurve.setPointToPositionAt(position, priorConnection.connectedToStart ? 0.5F : otherCurve.pathLength - 0.5F);
								position.add(otherLane.road.position).subtract(road.position);
								GL11.glVertex3d(position.x, position.y + 0.5, position.z);
								GL11.glVertex3d(position.x, position.y + 0.5, position.z);
								GL11.glVertex3d(position.x, position.y + 2.0, position.z);
							}
						}
					}
				}
			}
			InterfaceRender.setColorState(0.5F, 0.0F, 1.0F, 1);
			for(RoadLane lane : road.lanes){
				for(List<RoadLaneConnection> curveNextConnections : lane.nextConnections){
					BezierCurve currentCurve = lane.curves.get(lane.nextConnections.indexOf(curveNextConnections));
					for(RoadLaneConnection nextConnection : curveNextConnections){
						TileEntityRoad otherRoad = road.world.getTileEntity(nextConnection.tileLocation);
						if(otherRoad != null){
							RoadLane otherLane = otherRoad.lanes.get(nextConnection.laneNumber);
							if(otherLane != null){
								BezierCurve otherCurve = otherLane.curves.get(nextConnection.curveNumber);
								
								//First render our own offset point.
								currentCurve.setPointToPositionAt(position, currentCurve.pathLength - 0.5F);
								GL11.glVertex3d(position.x, position.y + 3.0, position.z);
								GL11.glVertex3d(position.x, position.y + 0.5, position.z);
								GL11.glVertex3d(position.x, position.y + 0.5, position.z);
								
								//Now render the connection point.
								otherCurve.setPointToPositionAt(position, nextConnection.connectedToStart ? 0.5F : otherCurve.pathLength - 0.5F);
								position.add(otherLane.road.position).subtract(road.position);
								GL11.glVertex3d(position.x, position.y + 0.5, position.z);
								GL11.glVertex3d(position.x, position.y + 0.5, position.z);
								GL11.glVertex3d(position.x, position.y + 2.0, position.z);
							}
						}
					}
				}
			}
			
			//Set states back to normal.
			GL11.glEnd();
			InterfaceRender.resetStates();
		}
	}
	
	@Override
	public String getTexture(TileEntityRoad road){
		//We never render, so we'll never call this method.
		return null;
	}
	
	@Override
	public boolean disableMainRendering(TileEntityRoad road, float partialTicks){
		return true;
	}
	
	@Override
	public boolean translateToSlabs(){
		return false;
	}
}
