package minecrafttransportsimulator.rendering.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLaneConnection;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.systems.ConfigSystem;

public class RenderRoad extends ARenderTileEntityBase<TileEntityRoad>{
	private static final Map<TileEntityRoad, Map<RoadComponent, Integer>> roadDisplayListMap = new HashMap<TileEntityRoad, Map<RoadComponent, Integer>>();
	
	@Override
	public void render(TileEntityRoad road, float partialTicks){
		//Render road components.
		//First set helper variables.
		Point3d position = new Point3d(0, 0, 0);
		Point3d rotation = new Point3d(0, 0, 0);
		
		//If we haven't rendered the road yet, do so now.
		//We cache it in a DisplayList, as there are a LOT of transforms done each component.
		if(!roadDisplayListMap.containsKey(road)){
			roadDisplayListMap.put(road, new HashMap<RoadComponent, Integer>());
		}
		Map<RoadComponent, Integer> displayListMap = roadDisplayListMap.get(road);
		for(RoadComponent component : road.components.keySet()){
			ItemRoadComponent componentItem = road.components.get(component);
			
			if(!displayListMap.containsKey(component)){
				int displayListIndex = GL11.glGenLists(1);
				GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
				switch(component){
					case CORE: {
						MasterLoader.renderInterface.bindTexture(componentItem.definition.getTextureLocation(componentItem.subName));
						//Core components need to be transformed to wedges.
						List<Float[]> transformedVertices = new ArrayList<Float[]>();
						Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(componentItem.definition.getModelLocation());
						
						Point3d priorPosition = new Point3d(0, 0, 0);
						Point3d priorRotation = new Point3d(0, 0, 0);
						Point3d rotationDelta = new Point3d(0, 0, 0);
						float priorIndex = 0;
						
						GL11.glBegin(GL11.GL_TRIANGLES);
						for(float currentIndex=1; currentIndex<=road.curve.pathLength; ++currentIndex){
							//Copy the master vertices to our transformed ones.
							transformedVertices.clear();
							for(Float[][] vertexSet : parsedModel.values()){
								for(Float[] vertex : vertexSet){
									transformedVertices.add(new Float[]{vertex[0], vertex[1], vertex[2], vertex[3], vertex[4], vertex[5], vertex[6], vertex[7]});
								}
							}
							
							//Get current and prior curve position and rotation.
							//From this, we know how much to stretch the model to that point's rendering area.
							road.curve.setPointToPositionAt(priorPosition, priorIndex);
							road.curve.setPointToRotationAt(priorRotation, priorIndex);
							road.curve.setPointToPositionAt(position, currentIndex);
							road.curve.setPointToRotationAt(rotation, currentIndex);
							
							//If we are a really sharp curve, we might have inverted our model at the inner corner.
							//Check for this, and if we have done so, skip this segment.
							//If we detect this in the last 3 segments, skip right to the end.
							//This prevents a missing end segment due to collision.
							rotationDelta.setTo(rotation).subtract(priorRotation);
							Point3d testPoint1 = new Point3d(road.definition.general.borderOffset, 0, 0).rotateFine(priorRotation).add(priorPosition);
							Point3d testPoint2 = new Point3d(road.definition.general.borderOffset, 0, 0).rotateFine(rotation).add(position);
							if(currentIndex != road.curve.pathLength && (position.x - priorPosition.x)*(testPoint2.x - testPoint1.x) < 0 || (position.z - priorPosition.z)*(testPoint2.z - testPoint1.z) < 0){
								if(currentIndex != road.curve.pathLength && currentIndex + 3 > road.curve.pathLength){
									currentIndex = road.curve.pathLength - 1;
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
							if(currentIndex != road.curve.pathLength && currentIndex + 1 > road.curve.pathLength){
								currentIndex -= ((currentIndex + 1) - road.curve.pathLength);
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
			
			MasterLoader.renderInterface.bindTexture(componentItem.definition.getTextureLocation(componentItem.subName));
			GL11.glCallList(displayListMap.get(component));
		}
		
		//If we are inactive, or in devMode, render road bounds and colliding boxes.
		if(!road.isActive || ConfigSystem.configObject.clientControls.devMode.value){
			//Render the information hashes.
			//First set states.
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glLineWidth(2);
			GL11.glBegin(GL11.GL_LINES);
			
			//Render the lane start points.
			GL11.glColor3f(1, 0, 0);
			for(RoadLane lane : road.lanes){
				GL11.glVertex3d(lane.curve.startPos.x, lane.curve.startPos.y, lane.curve.startPos.z);
				GL11.glVertex3d(lane.curve.startPos.x, lane.curve.startPos.y + 3, lane.curve.startPos.z);
				GL11.glVertex3d(lane.curve.startPos.x, lane.curve.startPos.y + 3, lane.curve.startPos.z);
				rotation.set(0, road.curve.startAngle, 0);
				position.set(0, 0, 2).rotateFine(rotation);
				GL11.glVertex3d(lane.curve.startPos.x + position.x, lane.curve.startPos.y + 3 + position.y, lane.curve.startPos.z + position.z);
			}
			
			//Render the curves.
			//First render the actual curve.
			GL11.glColor3f(0, 1, 0);
			for(float f=0; f<road.curve.pathLength; f+=0.1){
				road.curve.setPointToPositionAt(position, f);
				GL11.glVertex3d(position.x, position.y, position.z);
				GL11.glVertex3d(position.x, position.y + 1.5, position.z);
			}
			
			//Now render the outer border bounds.
			GL11.glColor3f(0, 1, 1);
			for(float f=0; f<road.curve.pathLength; f+=0.1){
				road.curve.setPointToRotationAt(rotation, f);
				position.set(road.definition.general.borderOffset, 0, 0).rotateFine(rotation);
				road.curve.offsetPointByPositionAt(position, f);
				
				GL11.glVertex3d(position.x, position.y, position.z);
				GL11.glVertex3d(position.x, position.y + 1.5, position.z);
			}
			
			//Now render the lane curve segments.
			GL11.glColor3f(1, 1, 0);
			for(RoadLane lane : road.lanes){
				lane.curve.setPointToPositionAt(position, 0);
				for(float f=0; f<road.curve.pathLength; f+=0.1){
					lane.curve.setPointToPositionAt(position, f);
					GL11.glVertex3d(position.x, position.y, position.z);
					GL11.glVertex3d(position.x, position.y + 1.5, position.z);
				}	
			}
			
			//Render the lane connections.
			GL11.glColor3f(0, 0, 1);
			for(RoadLane lane : road.lanes){
				for(RoadLaneConnection connection : lane.priorConnections){
					TileEntityRoad otherRoad = road.world.getTileEntity(connection.tileLocation);
					if(otherRoad != null){
						RoadLane otherLane = otherRoad.lanes.get(connection.laneNumber);
						if(otherLane != null){
							//First render our own offset point.
							lane.curve.setPointToPositionAt(position, 0.5F);
							GL11.glVertex3d(position.x, position.y + 0.5, position.z);
							
							//Now render the connection point.
							otherLane.curve.setPointToPositionAt(rotation, connection.connectedToStart ? 0.5F : otherLane.curve.pathLength - 0.5F);
							GL11.glVertex3d(position.x, position.y + 0.5, position.z);
						}
					}
				}
			}
			
			//Set states back to normal.
			GL11.glEnd();
			GL11.glColor3f(1, 1, 1);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
	}
	
	@Override
	public boolean rotateToBlock(){
		return false;
	}
	
	@Override
	public boolean translateToSlabs(){
		return false;
	}
}
