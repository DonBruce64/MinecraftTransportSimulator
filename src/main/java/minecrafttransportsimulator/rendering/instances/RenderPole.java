package minecrafttransportsimulator.rendering.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Core;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_StreetLight;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_StreetLight.LightState;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_TrafficSignal;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.TransformLight;

public class RenderPole extends ARenderTileEntityBase<TileEntityPole>{
	private static final Map<JSONPoleComponent, Map<Axis, Integer>> connectorDisplayListMap = new HashMap<JSONPoleComponent, Map<Axis, Integer>>();
	private static final Map<JSONPoleComponent, Map<Axis, Integer>> solidConnectorDisplayListMap = new HashMap<JSONPoleComponent, Map<Axis, Integer>>();
	private static final Map<JSONPoleComponent, Integer> componentDisplayListMap = new HashMap<JSONPoleComponent, Integer>();
	private static final Map<JSONPoleComponent, List<TransformLight>> componentLightMap = new HashMap<JSONPoleComponent, List<TransformLight>>();
	
	@Override
	public void render(TileEntityPole tile, float partialTicks){
		//First render all connections.
		//These are based on the pole itself, so we first need to get the pole.
		TileEntityPole_Core coreComponent = (TileEntityPole_Core) tile.components.get(Axis.NONE);
		if(coreComponent != null){
			//If we don't have the model parsed, do so now.
			if(!connectorDisplayListMap.containsKey(tile.definition)){
				Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(tile.definition.packID, tile.definition.getModelLocation());
				
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
				MasterLoader.renderInterface.bindTexture(tile.definition.packID, tile.definition.getTextureLocation());
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
						Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(component.definition.packID, component.definition.getModelLocation());
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
						MasterLoader.renderInterface.bindTexture(component.definition.packID, component.definition.getTextureLocation());
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
							MasterLoader.renderInterface.renderTextMarkings(component.definition.general.textObjects, ((TileEntityPole_Sign) component).getTextLines(), null, false);
						}
					}
					GL11.glPopMatrix();
				}
			}
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
	
	private static int cacheAxisVertices(Float[][] vertices){
		int displayListIndex = GL11.glGenLists(1);
		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : vertices){
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
		}
		GL11.glEnd();
		GL11.glEndList();
		return displayListIndex;
	}
}
