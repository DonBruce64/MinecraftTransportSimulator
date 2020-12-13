package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadLane;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;

public class RenderRoad extends ARenderTileEntityBase<TileEntityRoad>{
	private static final Map<JSONRoadComponent, Integer> componentDisplayListMap = new HashMap<JSONRoadComponent, Integer>();
	private static int flagDisplayListIndex = -1;
	
	@Override
	public void render(TileEntityRoad road, float partialTicks){
		ItemRoadComponent coreComponent = road.components.get(RoadComponent.CORE);
		//Render road components.
		
		
		//If we are holographic, render road bounds and colliding boxes.
		if(road.isHolographic){
			//Render the information hashes.
			//First set states.
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glLineWidth(2);
			GL11.glBegin(GL11.GL_LINES);
			
			//Render the lane start points.
			GL11.glColor3f(1, 0, 0);
			for(RoadLane lane : road.lanes){
				GL11.glVertex3d(lane.startingOffset.x, lane.startingOffset.y, lane.startingOffset.z);
				GL11.glVertex3d(lane.startingOffset.x, lane.startingOffset.y + 2, lane.startingOffset.z);
			}
			
			//Render the lane paths.
			GL11.glColor3f(1, 1, 0);
			Point3d point = new Point3d(0, 0, 0);
			for(RoadLane lane : road.lanes){
				for(float f=0; f<lane.curve.pathLength; f+=0.1){
					point.setTo(lane.curve.getPointAt(f)).add(lane.startingOffset);
					GL11.glVertex3d(point.x, point.y, point.z);
					GL11.glVertex3d(point.x, point.y + 1, point.z);
				}
			}
			
			//Set states back to normal.
			GL11.glEnd();
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
		
		
		/*
		TileEntityRoad_Core coreComponent = (TileEntityRoad_Core) tile.components.get(Axis.NONE);
		if(coreComponent != null){
			//If we don't have the model parsed, do so now.
			if(!connectorDisplayListMap.containsKey(tile.definition)){
				Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(tile.definition.getModelLocation());
				
				Map<Axis, Integer> connectorDisplayLists = new HashMap<Axis, Integer>();
				Map<Axis, Integer> solidConncectorDisplayLists = new HashMap<Axis, Integer>();
				for(Axis axis : Axis.values()){
					if(parsedModel.containsKey(axis.name().toLowerCase())){
						connectorDisplayLists.put(axis, cacheAxisVertices(parsedModel.get(axis.name().toLowerCase())));
					}
					if(parsedModel.containsKey(axis.name().toLowerCase() + "_solid")){
						solidConncectorDisplayLists.put(axis, cacheAxisVertices(parsedModel.get(axis.name().toLowerCase() + "_solid")));
					}
				}
				connectorDisplayListMap.put(tile.definition, connectorDisplayLists);
				solidConnectorDisplayListMap.put(tile.definition, solidConncectorDisplayLists);
			}
			
			//Render the connectors.  Don't do this on the blending pass 1.
			if(MasterLoader.renderInterface.getRenderPass() != 1){
				MasterLoader.renderInterface.bindTexture(tile.definition.getTextureLocation());
				for(Axis axis : Axis.values()){
					if(axis.equals(Axis.NONE)){
						GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(axis));
					}else{
						Point3i offset = axis.getOffsetPoint(tile.position);
						boolean adjacentPole = tile.world.getBlock(offset) instanceof BlockPole;
						boolean solidBlock = tile.world.isBlockSolid(offset);
						boolean slabBlock = (axis.equals(Axis.DOWN) && tile.world.isBlockBottomSlab(offset)) || (axis.equals(Axis.UP) && tile.world.isBlockTopSlab(offset));
						if(adjacentPole || solidBlock){
							if(connectorDisplayListMap.get(tile.definition).containsKey(axis)){
								GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(axis));
							}
						}
						if(solidBlock){
							if(solidConnectorDisplayListMap.get(tile.definition).containsKey(axis)){
								GL11.glCallList(solidConnectorDisplayListMap.get(tile.definition).get(axis));
							}
						}else if(slabBlock){
							//Slab.  Render the center and proper portion and center again to render at slab height.
							//Also render solid portion as it's a solid block.
							Axis oppositeAxis = axis.getOpposite();
							if(connectorDisplayListMap.get(tile.definition).containsKey(axis)){
								GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(axis));
								//Offset to slab block.
								GL11.glTranslatef(0.0F, axis.yOffset, 0.0F);
								
								//Render upper and center section.  Upper joins lower above slab.
								if(connectorDisplayListMap.get(tile.definition).containsKey(oppositeAxis)){
									GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(oppositeAxis));
								}
								GL11.glCallList(connectorDisplayListMap.get(tile.definition).get(Axis.NONE));
								
								//Offset to top of slab and render solid lower connector, if we have one.
								GL11.glTranslatef(0.0F, -axis.yOffset/2F, 0.0F);
								if(solidConnectorDisplayListMap.get(tile.definition).containsKey(axis)){
									GL11.glCallList(solidConnectorDisplayListMap.get(tile.definition).get(axis));
								}
								
								//Translate back to the normal position.
								GL11.glTranslatef(0.0F, -axis.yOffset/2F, 0.0F);
							}
						}
					}
				}
			}
		}
		
		//Done rendering core and connections.  Render components now.
		for(Axis axis : Axis.values()){
			if(!axis.equals(Axis.NONE)){
				if(tile.components.containsKey(axis)){
					//Cache the displaylists and lights if we haven't already.
					ATileEntityPole_Component component = tile.components.get(axis);
					if(!componentDisplayListMap.containsKey(component.definition)){
						Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(component.definition.getModelLocation());
						List<TransformLight> lightParts = new ArrayList<TransformLight>();
						int displayListIndex = GL11.glGenLists(1);
						GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
						GL11.glBegin(GL11.GL_TRIANGLES);
						for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
							if(entry.getKey().startsWith("&")){
								//Save light for special rendering.
								lightParts.add(new TransformLight(component.definition.general.modelName, entry.getKey(), entry.getValue()));
								if(lightParts.get(lightParts.size() - 1).isLightupTexture){
									continue;
								}
							}
							//Add vertices
							for(Float[] vertex : entry.getValue()){
								GL11.glTexCoord2f(vertex[3], vertex[4]);
								GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
								GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
							}
						}
						GL11.glEnd();
						GL11.glEndList();
						
						//Put parsed model into the maps.
						componentDisplayListMap.put(component.definition, displayListIndex);
						componentLightMap.put(component.definition, lightParts);
					}
					
					//Rotate to component axis and render.
					GL11.glPushMatrix();
					GL11.glRotatef(axis.yRotation, 0, 1, 0);
					GL11.glTranslatef(0, 0, tile.definition.general.radius + 0.001F);
					
					//Don't do solid model rendering on the blend pass.
					if(MasterLoader.renderInterface.getRenderPass() != 1){
						MasterLoader.renderInterface.bindTexture(component.definition.getTextureLocation());
						GL11.glCallList(componentDisplayListMap.get(component.definition));
					}
					
					if(component instanceof TileEntityPole_TrafficSignal){
						LightType litLight;
						switch(((TileEntityPole_TrafficSignal) component).state){
							case UNLINKED: litLight = LightType.UNLINKEDLIGHT; break;
							case RED: litLight = LightType.STOPLIGHT; break;
							case YELLOW: litLight = LightType.CAUTIONLIGHT; break;
							case GREEN: litLight = LightType.GOLIGHT; break;
							default: litLight = null;
						}
						for(TransformLight lightPart : componentLightMap.get(component.definition)){
							lightPart.renderOnBlock(tile.world, tile.position, lightPart.type.equals(litLight));
						}
					}else if(component instanceof TileEntityPole_StreetLight){
						for(TransformLight lightPart : componentLightMap.get(component.definition)){
							lightPart.renderOnBlock(tile.world, tile.position, ((TileEntityPole_StreetLight) component).state.equals(LightState.ON));
						}
					}else if(component instanceof TileEntityPole_Sign){
						//Render lights, if we have any.
						for(TransformLight lightPart : componentLightMap.get(component.definition)){
							lightPart.renderOnBlock(tile.world, tile.position, true);
						}
						
						//Render text, if we have any.
						if(component.definition.general.textObjects != null){
							MasterLoader.renderInterface.renderTextMarkings(component.definition.general.textObjects, ((TileEntityPole_Sign) component).getTextLines(), null, null, false);
						}
					}
					GL11.glPopMatrix();
				}
			}
		}*/
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
