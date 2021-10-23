package minecrafttransportsimulator.rendering.instances;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLaneConnection;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.AModelParser;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import minecrafttransportsimulator.systems.ConfigSystem;

public class RenderRoad extends ARenderTileEntityBase<TileEntityRoad>{
	
	@Override
	public void renderAdditionalModels(TileEntityRoad road, boolean blendingEnabled, float partialTicks){
		if(road.isActive() ^ blendingEnabled){
			//Render road components.
			for(RoadComponent component : road.components.keySet()){
				if(!road.renderableObjects.containsKey(component)){
					Point3d position = new Point3d();
					Point3d rotation = new Point3d();
					ItemRoadComponent componentItem = road.components.get(component);
					switch(component){
						case CORE_STATIC: {
							List<RenderableObject> parsedModel = AModelParser.parseModel(componentItem.definition.getModelLocation(componentItem.subName));
							int totalVertices = 0;
							for(RenderableObject object : parsedModel){
								totalVertices += object.vertices.capacity();
								for(int i=0; i<object.vertices.capacity(); i+=8){
									position.set(object.vertices.get(i+5) - 0.5, object.vertices.get(i+6), object.vertices.get(i+7) - 0.5);
									position.rotateFine(road.angles);
									object.vertices.put(i+5, (float) position.x);
									object.vertices.put(i+6, (float) position.y);
									object.vertices.put(i+7, (float) position.z);
								}
							}
							
							//Cache the model now that we know how big it is.
							FloatBuffer totalModel = FloatBuffer.allocate(totalVertices);
							for(RenderableObject object : parsedModel){
								totalModel.put(object.vertices);
							}
							totalModel.flip();
							road.renderableObjects.put(component, new RenderableObject(component.name(), componentItem.definition.getTextureLocation(componentItem.subName), new ColorRGB(), totalModel, true));
							break;
						}
						case CORE_DYNAMIC: {
							//Make sure our curve isn't null, we might have not yet created it.
							if(road.dynamicCurve != null){
								//Get model and convert to a single buffer of vertices.
								List<RenderableObject> parsedModel = AModelParser.parseModel(componentItem.definition.getModelLocation(componentItem.subName));
								int totalVertices = 0;
								for(RenderableObject object : parsedModel){
									totalVertices += object.vertices.capacity();
								}
								FloatBuffer parsedVertices = FloatBuffer.allocate(totalVertices);
								for(RenderableObject object : parsedModel){
									parsedVertices.put(object.vertices);
								}
								parsedVertices.flip();
								
								//Core components need to be transformed to wedges.
								Point3d priorPosition = new Point3d();
								Point3d priorRotation = new Point3d();
								float priorIndex = 0;
								List<float[]> segmentVertices = new ArrayList<float[]>();
								for(float currentIndex=1; currentIndex<=road.dynamicCurve.pathLength; ++currentIndex){
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
									Point3d testPoint1 = new Point3d(road.definition.road.borderOffset, 0, 0).rotateFine(priorRotation).add(priorPosition);
									Point3d testPoint2 = new Point3d(road.definition.road.borderOffset, 0, 0).rotateFine(rotation).add(position);
									if(currentIndex != road.dynamicCurve.pathLength && ((position.x - priorPosition.x)*(testPoint2.x - testPoint1.x) < 0 || (position.z - priorPosition.z)*(testPoint2.z - testPoint1.z) < 0)){
										if(currentIndex + 3 > road.dynamicCurve.pathLength){
											currentIndex = road.dynamicCurve.pathLength - 1;
										}
										continue;
									}
									
									//Depending on the vertex position in the model, transform it to match with the offset rotation.
									//This depends on how far the vertex is from the origin of the model, and how big the delta is.
									//For all points, their magnitude depends on how far away they are on the Z-axis.
									for(int i=0; i<parsedVertices.capacity(); i+=8){
										float[] convertedVertexData = new float[8]; 
										
										//Add the normals and UVs first.  These won't change.
										parsedVertices.get(convertedVertexData, 0, 5);
										
										//Now convert the XYZ points.
										float x = parsedVertices.get();
										float y = parsedVertices.get();
										float z = parsedVertices.get();
										Point3d vertexOffsetPrior = new Point3d(x, y, 0);
										vertexOffsetPrior.rotateFine(priorRotation).add(priorPosition);
										Point3d vertexOffsetCurrent = new Point3d(x, y, z);
										vertexOffsetCurrent.rotateFine(rotation).add(position);
										
										Point3d segmentVector = vertexOffsetPrior.subtract(vertexOffsetCurrent).multiply(Math.abs(z));
										Point3d renderedVertex = vertexOffsetCurrent.add(segmentVector);
										
										convertedVertexData[5] = (float) renderedVertex.x;
										convertedVertexData[6] = (float) renderedVertex.y;
										convertedVertexData[7] = (float) renderedVertex.z;
										
										//Add transformed vertices to the segment.
										segmentVertices.add(convertedVertexData);
									}
									//Rewind for next segment.
									parsedVertices.rewind();
									
									//Set the last index.
									priorIndex = currentIndex;
									
									//If we are at the last index, do special logic to get the very end point.
									if(currentIndex != road.dynamicCurve.pathLength && currentIndex + 1 > road.dynamicCurve.pathLength){
										currentIndex -= ((currentIndex + 1) - road.dynamicCurve.pathLength);
									}
								}
								
								//Cache and compile the segments.
								FloatBuffer convertedVertices = FloatBuffer.allocate(segmentVertices.size()*8);
								for(float[] segmentVertex : segmentVertices){
									convertedVertices.put(segmentVertex);
								}
								convertedVertices.flip();
								road.renderableObjects.put(component, new RenderableObject(component.name(), componentItem.definition.getTextureLocation(componentItem.subName), new ColorRGB(), convertedVertices, true));
							}
							break;
						}
						default:
							break;
					}
				}
				RenderableObject object = road.renderableObjects.get(component);
				if(road.isActive()){
					object.color.setTo(ColorRGB.WHITE);
					object.alpha = 1.0F;
					object.isTranslucent = false;
				}else{
					object.color.setTo(ColorRGB.GREEN);
					object.alpha = 0.5F;
					object.isTranslucent = true;
				}
				object.render();
			}
			
			//If we are inactive render the blocking blocks and the main block.
			if(!road.isActive()){
				InterfaceRender.setColorState(ColorRGB.RED, 0.5F);
				for(Point3d location : road.collidingBlockOffsets){
					BoundingBox blockingBox = new BoundingBox(location.copy().add(0, 0.5, 0), 0.55, 0.55, 0.55);
					GL11.glPushMatrix();
					GL11.glTranslated(location.x, location.y, location.z);
					RenderBoundingBox.renderSolid(blockingBox);
					GL11.glPopMatrix();
				}
				InterfaceRender.setColorState(ColorRGB.BLUE, 0.5F);
				BoundingBox mainBlockBox = new BoundingBox(new Point3d(0, 0.75, 0), 0.15, 1.5, 0.15);
				GL11.glPushMatrix();
				GL11.glTranslated(mainBlockBox.localCenter.x, mainBlockBox.localCenter.y, mainBlockBox.localCenter.z);
				RenderBoundingBox.renderSolid(mainBlockBox);
				GL11.glPopMatrix();
			}else{
				//If we are in devMode and have hitboxes shown, render road bounds and colliding boxes.
				if(ConfigSystem.configObject.clientControls.devMode.value && InterfaceRender.shouldRenderBoundingBoxes()){
					if(road.devObjects.isEmpty()){
						generateDevElements(road);
					}
					for(RenderableObject object : road.devObjects){
						object.render();
					}
				}
			}
		}
	}
	
	private static void generateDevElements(TileEntityRoad road){
		//Create the information hashes.
		Point3d position = new Point3d();
		Point3d rotation = new Point3d();
		RenderableObject curveObject;
		if(road.dynamicCurve != null){
			//Render actual curve.
			curveObject = new RenderableObject("curve", null, ColorRGB.GREEN, FloatBuffer.allocate((int) (road.dynamicCurve.pathLength*10)*6), false);
			curveObject.lineWidth = 2.0F;
			curveObject.disableLighting = true;
			for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
				road.dynamicCurve.setPointToPositionAt(position, f);
				
				curveObject.vertices.put((float) position.x);
				curveObject.vertices.put((float) position.y);
				curveObject.vertices.put((float) position.z);
				curveObject.vertices.put((float) position.x);
				curveObject.vertices.put((float) position.y + 1.0F);
				curveObject.vertices.put((float) position.z);
			}
			curveObject.vertices.flip();
			road.devObjects.add(curveObject);
			
			//Render the outer border bounds.
			curveObject = new RenderableObject("curve", null, ColorRGB.CYAN, FloatBuffer.allocate((int) (road.dynamicCurve.pathLength*10)*6), false);
			curveObject.lineWidth = 2.0F;
			curveObject.disableLighting = true;
			for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
				road.dynamicCurve.setPointToRotationAt(rotation, f);
				position.set(road.definition.road.borderOffset, 0, 0).rotateFine(rotation);
				road.dynamicCurve.offsetPointByPositionAt(position, f);
				
				curveObject.vertices.put((float) position.x);
				curveObject.vertices.put((float) position.y);
				curveObject.vertices.put((float) position.z);
				curveObject.vertices.put((float) position.x);
				curveObject.vertices.put((float) position.y + 1.0F);
				curveObject.vertices.put((float) position.z);
			}
			curveObject.vertices.flip();
			road.devObjects.add(curveObject);
		}
		
		//Now render the lane curve segments.
		for(RoadLane lane : road.lanes){
			for(BezierCurve laneCurve : lane.curves){
				//Render the curve bearing indicator
				curveObject = new RenderableObject("curve", null, ColorRGB.RED, FloatBuffer.allocate(4*3), false);
				curveObject.lineWidth = 2.0F;
				curveObject.disableLighting = true;
				for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
					laneCurve.setPointToPositionAt(position, f);
					
					curveObject.vertices.put((float) laneCurve.startPos.x);
					curveObject.vertices.put((float) laneCurve.startPos.y);
					curveObject.vertices.put((float) laneCurve.startPos.z);
					curveObject.vertices.put((float) laneCurve.startPos.x);
					curveObject.vertices.put((float) laneCurve.startPos.y + 3);
					curveObject.vertices.put((float) laneCurve.startPos.z);
					curveObject.vertices.put((float) laneCurve.startPos.x);
					curveObject.vertices.put((float) laneCurve.startPos.y + 3);
					curveObject.vertices.put((float) laneCurve.startPos.z);
					
					Point3d bearingPos = laneCurve.endPos.copy().subtract(laneCurve.startPos).normalize().add(laneCurve.startPos);
					curveObject.vertices.put((float) bearingPos.x);
					curveObject.vertices.put((float) bearingPos.y + 3);
					curveObject.vertices.put((float) bearingPos.z);
				}
				curveObject.vertices.flip();
				road.devObjects.add(curveObject);
				
				//Render all the points on the curve.
				curveObject = new RenderableObject("curve", null, ColorRGB.YELLOW, FloatBuffer.allocate((int) (laneCurve.pathLength*10)*6), false);
				curveObject.lineWidth = 2.0F;
				curveObject.disableLighting = true;
				for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
					laneCurve.setPointToPositionAt(position, f);
					
					curveObject.vertices.put((float) position.x);
					curveObject.vertices.put((float) position.y);
					curveObject.vertices.put((float) position.z);
					curveObject.vertices.put((float) position.x);
					curveObject.vertices.put((float) position.y + 1.0F);
					curveObject.vertices.put((float) position.z);
				}
				curveObject.vertices.flip();
				road.devObjects.add(curveObject);
			}
		}
		
		//Render the lane connections.
		for(RoadLane lane : road.lanes){
			for(List<RoadLaneConnection> curvePriorConnections : lane.priorConnections){
				BezierCurve currentCurve = lane.curves.get(lane.priorConnections.indexOf(curvePriorConnections));
				for(RoadLaneConnection priorConnection : curvePriorConnections){
					TileEntityRoad otherRoad = road.world.getTileEntity(priorConnection.tileLocation);
					if(otherRoad != null){
						RoadLane otherLane = otherRoad.lanes.get(priorConnection.laneNumber);
						if(otherLane != null){
							curveObject = new RenderableObject("curve", null, ColorRGB.PINK, FloatBuffer.allocate(6*3), false);
							curveObject.lineWidth = 2.0F;
							curveObject.disableLighting = true;
							
							//First render our own offset point.
							currentCurve.setPointToPositionAt(position, 0.5F);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 3);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 0.5F);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 0.5F);
							curveObject.vertices.put((float) position.z);
							
							//Now render the connection point.
							BezierCurve otherCurve = otherLane.curves.get(priorConnection.curveNumber);
							otherCurve.setPointToPositionAt(position, priorConnection.connectedToStart ? 0.5F : otherCurve.pathLength - 0.5F);
							position.add(otherLane.road.position).subtract(road.position);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 0.5F);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 0.5F);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 2.0F);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.flip();
							road.devObjects.add(curveObject);
						}
					}
				}
			}
		}
		for(RoadLane lane : road.lanes){
			for(List<RoadLaneConnection> curveNextConnections : lane.nextConnections){
				BezierCurve currentCurve = lane.curves.get(lane.nextConnections.indexOf(curveNextConnections));
				for(RoadLaneConnection nextConnection : curveNextConnections){
					TileEntityRoad otherRoad = road.world.getTileEntity(nextConnection.tileLocation);
					if(otherRoad != null){
						RoadLane otherLane = otherRoad.lanes.get(nextConnection.laneNumber);
						if(otherLane != null){
							curveObject = new RenderableObject("curve", null, ColorRGB.ORANGE, FloatBuffer.allocate(6*3), false);
							curveObject.lineWidth = 2.0F;
							curveObject.disableLighting = true;
							
							//First render our own offset point.
							currentCurve.setPointToPositionAt(position, currentCurve.pathLength - 0.5F);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 3);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 0.5F);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 0.5F);
							curveObject.vertices.put((float) position.z);
							
							//Now render the connection point.
							BezierCurve otherCurve = otherLane.curves.get(nextConnection.curveNumber);
							otherCurve.setPointToPositionAt(position, nextConnection.connectedToStart ? 0.5F : otherCurve.pathLength - 0.5F);
							position.add(otherLane.road.position).subtract(road.position);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 0.5F);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 0.5F);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.put((float) position.x);
							curveObject.vertices.put((float) position.y + 2.0F);
							curveObject.vertices.put((float) position.z);
							curveObject.vertices.flip();
							road.devObjects.add(curveObject);
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean disableModelRendering(TileEntityRoad road, float partialTicks){
		return true;
	}
	
	@Override
	public void adjustPositionRotation(TileEntityRoad road, float partialTicks, Point3d entityPosition, Point3d entityRotation){
		super.adjustPositionRotation(road, partialTicks, entityPosition, entityRotation);
		//Set angles to 0 as we do rendering based on the curve properties, which already have angles. 
		entityRotation.set(0, 0, 0);
	}
	
	@Override
	public boolean translateToSlabs(){
		return false;
	}
}
