package minecrafttransportsimulator.rendering.instances;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLaneConnection;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.rendering.components.AModelParser;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;
import minecrafttransportsimulator.rendering.components.RenderableObject;
import minecrafttransportsimulator.systems.ConfigSystem;

public class RenderRoad extends ARenderEntityDefinable<TileEntityRoad>{
		
	@Override
	protected void renderModel(TileEntityRoad road, TransformationMatrix transform, boolean blendingEnabled, float partialTicks){
		//Don't call super, we don't want to render the normal way.
		if(road.isActive() ^ blendingEnabled){
			//Render road components.
			for(RoadComponent component : road.components.keySet()){
				if(!road.componentRenderables.containsKey(component)){;
					ItemRoadComponent componentItem = road.components.get(component);
					switch(component){
						case CORE_STATIC: {
							List<RenderableObject> parsedModel = AModelParser.parseModel(componentItem.definition.getModelLocation(componentItem.subName));
							int totalVertices = 0;
							for(RenderableObject object : parsedModel){
								totalVertices += object.vertices.capacity();
							}
							
							//Cache the model now that we know how big it is.
							FloatBuffer totalModel = FloatBuffer.allocate(totalVertices);
							for(RenderableObject object : parsedModel){
								totalModel.put(object.vertices);
							}
							totalModel.flip();
							road.componentRenderables.put(component, new RenderableObject(component.name(), componentItem.definition.getTextureLocation(componentItem.subName), new ColorRGB(), totalModel, true));
							break;
						}
						case CORE_DYNAMIC: {
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
							
							//Offset vertices to be corner-aligned, as that's how our curve aligns.
							for(int i=0; i<parsedVertices.capacity(); i+=8){
								parsedVertices.put(i+5, (float) (parsedVertices.get(i+5) - road.definition.road.cornerOffset.x));
								parsedVertices.put(i+7, (float) (parsedVertices.get(i+7) - road.definition.road.cornerOffset.z));
							}
							
							//Core components need to be transformed to wedges.
							Point3D position = new Point3D();
							RotationMatrix rotation;
							Point3D priorPosition = new Point3D();
							RotationMatrix priorRotation;
							Point3D testPoint1 = new Point3D();
							Point3D testPoint2 = new Point3D();
							Point3D vertexOffsetPriorLine = new Point3D();
							Point3D vertexOffsetCurrentLine = new Point3D();
							Point3D segmentVector = new Point3D();
							Point3D renderedVertex = new Point3D();
							float indexDelta = (float) (road.dynamicCurve.pathLength/Math.floor(road.dynamicCurve.pathLength/road.definition.road.segmentLength));
							boolean finalSegment = false;
							float priorIndex = 0;
							float currentIndex = 0;
							List<float[]> segmentVertices = new ArrayList<float[]>();
							while(!finalSegment){
								//If we are at the last index, do special logic to get the very end point.
								//We check here in case FPEs have accumulated and we won't end on the exact end segment.
								//Otherwise, increment normally.
								if(currentIndex != road.dynamicCurve.pathLength && currentIndex + indexDelta*1.25 > road.dynamicCurve.pathLength){
									currentIndex = road.dynamicCurve.pathLength;
									finalSegment = true;
								}else{
									currentIndex += indexDelta;
								}
								
								//Get current and prior curve position and rotation.
								//From this, we know how much to stretch the model to that point's rendering area.
								road.dynamicCurve.setPointToPositionAt(priorPosition, priorIndex);
								priorRotation = road.dynamicCurve.getRotationAt(priorIndex);
								priorPosition.subtract(road.dynamicCurve.startPos);
								road.dynamicCurve.setPointToPositionAt(position, currentIndex);
								rotation = road.dynamicCurve.getRotationAt(currentIndex);
								position.subtract(road.dynamicCurve.startPos);
								
								//If we are a really sharp curve, we might have inverted our model at the inner corner.
								//Check for this, and if we have done so, skip this segment.
								//If we detect this in the last 3 segments, skip right to the end.
								//This prevents a missing end segment due to collision.
								testPoint1.set(road.definition.road.roadWidth + road.definition.road.cornerOffset.x, 0, 0);
								testPoint1.rotate(priorRotation).add(priorPosition);
								testPoint2.set(road.definition.road.roadWidth + road.definition.road.cornerOffset.x, 0, 0);
								testPoint2.rotate(rotation).add(position);
								if(currentIndex != road.dynamicCurve.pathLength && ((position.x - priorPosition.x)*(testPoint2.x - testPoint1.x) < 0 || (position.z - priorPosition.z)*(testPoint2.z - testPoint1.z) < 0)){
									if(currentIndex + 3*indexDelta > road.dynamicCurve.pathLength){
										currentIndex = road.dynamicCurve.pathLength - indexDelta;
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
									vertexOffsetPriorLine.set(x, y, 0);
									vertexOffsetPriorLine.rotate(priorRotation).add(priorPosition);
									vertexOffsetCurrentLine.set(x, y, 0);
									vertexOffsetCurrentLine.rotate(rotation).add(position);
									
									segmentVector.set(vertexOffsetCurrentLine).subtract(vertexOffsetPriorLine).scale(z/road.definition.road.segmentLength);
									renderedVertex.set(vertexOffsetPriorLine).add(segmentVector);
									
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
							}
							
							//Cache and compile the segments.
							FloatBuffer convertedVertices = FloatBuffer.allocate(segmentVertices.size()*8);
							for(float[] segmentVertex : segmentVertices){
								convertedVertices.put(segmentVertex);
							}
							convertedVertices.flip();
							road.componentRenderables.put(component, new RenderableObject(component.name(), componentItem.definition.getTextureLocation(componentItem.subName), new ColorRGB(), convertedVertices, true));
							break;
						}
					}
				}
				RenderableObject object = road.componentRenderables.get(component);
				if(road.isActive()){
					object.color.setTo(ColorRGB.WHITE);
					object.alpha = 1.0F;
					object.isTranslucent = false;
				}else{
					object.color.setTo(ColorRGB.GREEN);
					object.alpha = 0.5F;
					object.isTranslucent = true;
				}
				if(road.dynamicCurve != null){
					object.transform.setTranslation(road.dynamicCurve.startPos.copy().subtract(road.position));
					object.transform.multiply(transform);
				}else{
					object.transform.set(transform);
				}
				object.render();
			}
			
			//If we are inactive render the blocking blocks and the main block.
			if(!road.isActive()){
				if(road.blockingBoundingBoxes.isEmpty()){
					road.blockingBoundingBoxes.add(new BoundingBox(new Point3D(0, 0.75, 0), 0.15, 0.75, 0.15));
					for(Point3D location : road.collidingBlockOffsets){
						road.blockingBoundingBoxes.add(new BoundingBox(location.copy().add(0, 0.55, 0), 0.55, 0.55, 0.55));
					}
				}
				boolean firstBox = true;
				for(BoundingBox blockingBox : road.blockingBoundingBoxes){
					if(firstBox){
						blockingBox.renderHolographic(transform, blockingBox.globalCenter, ColorRGB.BLUE);
						firstBox = false;
					}else{
						blockingBox.renderHolographic(transform, blockingBox.globalCenter, ColorRGB.RED);
					}
				}
			}
		}
	}
	
	@Override
	public void renderBoundingBoxes(TileEntityRoad road, TransformationMatrix transform){
		super.renderBoundingBoxes(road, transform);
		//Render all collision boxes too.
		for(Point3D blockOffset : road.collisionBlockOffsets){
			ABlockBase block = road.world.getBlock(road.position.copy().add(blockOffset));
			if(block instanceof BlockCollision){
				//Need to offset by -0.5 as the collision box bounds is centered, but the offset isn't.
				BoundingBox blockBounds = ((BlockCollision) block).blockBounds;
				blockBounds.renderWireframe(road, transform, blockOffset.copy().add(-0.5, 0, -0.5), null);
			}
		}
		//If we are in devMode, render road bounds and colliding boxes.
		if(ConfigSystem.configObject.clientControls.devMode.value){
			if(road.devRenderables.isEmpty()){
				generateDevElements(road);
			}
			Point3D invertedPosition = road.position.copy().invert();
			for(RenderableObject renderable : road.devRenderables){
				renderable.transform.setTranslation(invertedPosition).multiply(transform);
				renderable.render();
			}
		}
	}
	
	private static void generateDevElements(TileEntityRoad road){
		//Create the information hashes.
		Point3D point1 = new Point3D();
		Point3D point2 = new Point3D();
		RotationMatrix rotation;
		RenderableObject curveObject;
		if(road.dynamicCurve != null){
			//Render actual curve.
			curveObject = new RenderableObject(ColorRGB.GREEN, (int) (road.dynamicCurve.pathLength*10));
			for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
				road.dynamicCurve.setPointToPositionAt(point1, f);
				rotation = road.dynamicCurve.getRotationAt(f);
				point2.set(0, 1, 0).rotate(rotation).add(point1);
				curveObject.addLine(point1, point2);
			}
			curveObject.vertices.flip();
			road.devRenderables.add(curveObject);
			
			//Render the border bounds.
			curveObject = new RenderableObject(ColorRGB.CYAN, (int) (road.dynamicCurve.pathLength*10));
			for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
				road.dynamicCurve.setPointToPositionAt(point1, f);
				rotation = road.dynamicCurve.getRotationAt(f);
				point1.set(road.definition.road.roadWidth, 0, 0);
				
				point2.set(point1).add(0, 1, 0).rotate(rotation);
				point1.rotate(rotation);
				
				road.dynamicCurve.offsetPointByPositionAt(point1, f);
				road.dynamicCurve.offsetPointByPositionAt(point2, f);
				curveObject.addLine(point1, point2);
			}
			curveObject.vertices.flip();
			road.devRenderables.add(curveObject);
		}
		
		//Now render the lane curve segments.
		for(RoadLane lane : road.lanes){
			for(BezierCurve laneCurve : lane.curves){
				//Render the curve bearing indicator
				curveObject = new RenderableObject(ColorRGB.RED, 2);
				
				//Render vertical segment.
				rotation = laneCurve.getRotationAt(0);
				laneCurve.setPointToPositionAt(point1, 0);
				point2.set(0, 3, 0).rotate(rotation).add(point1);
				curveObject.addLine(point1, point2);
				
				//Render 1-unit path delta.
				point1.set(point2);
				rotation = laneCurve.getRotationAt(1);
				point1.set(0, 3, 0).rotate(rotation);
				laneCurve.offsetPointByPositionAt(point1, 1);
				curveObject.addLine(point1, point2);
				
				curveObject.vertices.flip();
				road.devRenderables.add(curveObject);
				
				//Render all the points on the curve.
				curveObject = new RenderableObject(ColorRGB.YELLOW, (int) (laneCurve.pathLength*10));
				for(float f=0; curveObject.vertices.hasRemaining(); f+=0.1){
					laneCurve.setPointToPositionAt(point1, f);
					rotation = laneCurve.getRotationAt(f);
					point2.set(0, 1, 0).rotate(rotation).add(point1);
					curveObject.addLine(point1, point2);
				}
				curveObject.vertices.flip();
				road.devRenderables.add(curveObject);
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
							curveObject = new RenderableObject(ColorRGB.PINK, 3);
							
							//Get the connection point.
							currentCurve.setPointToPositionAt(point1, 0.5F);
							rotation = currentCurve.getRotationAt(0.5F);
							point2.set(0, 2.0, 0).rotate(rotation).add(point1);
							
							//Render marker.
							curveObject.addLine(point1, point2);
							curveObject.vertices.flip();
							road.devRenderables.add(curveObject);
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
							curveObject = new RenderableObject(ColorRGB.ORANGE, 3);
							
							//Get the connection point.
							currentCurve.setPointToPositionAt(point1, currentCurve.pathLength - 0.5F);
							rotation = currentCurve.getRotationAt(currentCurve.pathLength - 0.5F);
							point2.set(0, 2.0, 0).rotate(rotation).add(point1);
							
							//Render marker.
							curveObject.addLine(point1, point2);
							curveObject.vertices.flip();
							road.devRenderables.add(curveObject);
						}
					}
				}
			}
		}
	}
}
