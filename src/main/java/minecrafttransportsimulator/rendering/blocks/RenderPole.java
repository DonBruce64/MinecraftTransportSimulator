package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Core;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_CrossingSignal;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_StreetLight;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_TrafficSignal;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent.TextLine;
import minecrafttransportsimulator.rendering.vehicles.RenderVehicle_LightPart;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightType;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperRender;

public class RenderPole extends ARenderTileEntityBase<TileEntityPole, BlockPole>{
	private static final Map<JSONPoleComponent, Map<Axis, Integer>> connectorDisplayListMap = new HashMap<JSONPoleComponent, Map<Axis, Integer>>();
	private static final Map<JSONPoleComponent, Map<Axis, Integer>> solidConnectorDisplayListMap = new HashMap<JSONPoleComponent, Map<Axis, Integer>>();
	private static final Map<JSONPoleComponent, Integer> componentDisplayListMap = new HashMap<JSONPoleComponent, Integer>();
	private static final Map<JSONPoleComponent, List<RenderVehicle_LightPart>> componentLightMap = new HashMap<JSONPoleComponent, List<RenderVehicle_LightPart>>();
	
	@Override
	public void render(TileEntityPole tile, BlockPole block, float partialTicks){
		//First render all connections.
		//These are based on the pole itself, so we first need to get the pole.
		JSONPoleComponent definition = tile.getDefinition();
		TileEntityPole_Core coreComponent = (TileEntityPole_Core) tile.components.get(Axis.NONE);
		if(coreComponent != null){
			//If we don't have the model parsed, do so now.
			if(!connectorDisplayListMap.containsKey(definition)){
				Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(definition.packID, "objmodels/poles/" + definition.systemName + ".obj");
				
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
				connectorDisplayListMap.put(definition, connectorDisplayLists);
				solidConnectorDisplayListMap.put(definition, solidConncectorDisplayLists);
			}
			
			//Render the connectors.  Don't do this on any pass except 0.
			if(WrapperRender.getRenderPass() == 0){
				WrapperRender.bindTexture(definition.packID, "textures/poles/" + definition.systemName + ".png");
				for(Axis axis : Axis.values()){
					if(axis.equals(Axis.NONE)){
						GL11.glCallList(connectorDisplayListMap.get(definition).get(axis));
					}else{
						boolean adjacentPole = tile.world.getBlock(axis.getOffsetPoint(tile.position)) instanceof BlockPole;
						boolean solidBlock = tile.world.isBlockSolid(axis.getOffsetPoint(tile.position));
						if(adjacentPole || solidBlock){
							//System.out.println("HAVE CONNECTION ON " + axis.name());
							if(connectorDisplayListMap.get(definition).containsKey(axis)){
								GL11.glCallList(connectorDisplayListMap.get(definition).get(axis));
							}
						}
						if(solidBlock){
							//System.out.println("HAVE SOLID CONNECTION ON " + axis.name());
							if(solidConnectorDisplayListMap.get(definition).containsKey(axis)){
								GL11.glCallList(solidConnectorDisplayListMap.get(definition).get(axis));
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
						Map<String, Float[][]> parsedModel;
						if(component.definition.general.modelName != null){
							parsedModel = OBJParserSystem.parseOBJModel(component.definition.packID, "objmodels/poles/" + component.definition.general.modelName + ".obj");
						}else{
							parsedModel = OBJParserSystem.parseOBJModel(component.definition.packID, "objmodels/poles/" + component.definition.systemName + ".obj");
						}
						List<RenderVehicle_LightPart> lightParts = new ArrayList<RenderVehicle_LightPart>();
						
						int displayListIndex = GL11.glGenLists(1);
						GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
						GL11.glBegin(GL11.GL_TRIANGLES);
						for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
							if(entry.getKey().startsWith("&")){
								//Save light for special rendering.
								lightParts.add(new RenderVehicle_LightPart(entry.getKey(), entry.getValue()));
							}else{
								//Add vertices
								for(Float[] vertex : entry.getValue()){
									GL11.glTexCoord2f(vertex[3], vertex[4]);
									GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
									GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
								}
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
					GL11.glTranslatef(0, 0, tile.getDefinition().general.radius + 0.001F);
					
					//Don't do solid model rendering if it's not pass 0.
					if(WrapperRender.getRenderPass() == 0){
						WrapperRender.bindTexture(component.definition.packID, "textures/poles/" + component.definition.systemName + ".png");
						GL11.glCallList(componentDisplayListMap.get(component.definition));
					}
					
					if(component instanceof TileEntityPole_TrafficSignal){
						switch(((TileEntityPole_TrafficSignal) component).state){
						case RED: {
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.STOPLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", true);
								}
							}
							break;
						}
						case YELLOW:
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.CAUTIONLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", true);
								}
							}
							break;
						case GREEN:
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.GOLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", true);
								}
							}
							break;
						case FLASHING_RED:
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.STOPLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", false);
								}
							}
							break;
						case FLASHING_YELLOW:
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.CAUTIONLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", false);
								}
							}
							break;
						default:
							//This only happens if the light is off.
							break;
						}
					}else if(component instanceof TileEntityPole_CrossingSignal){
						switch(((TileEntityPole_CrossingSignal) component).state){
						case DONTWALK:
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.STOPLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", true);
								}
							}
							break;
						case FLASHING_DONTWALK:
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.STOPLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", false);
								}
							}
							break;
						case WALK:
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.GOLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", true);
								}
							}
							break;
						default:
							//This only happens if the light is off.
							break;
						}
					}else if(component instanceof TileEntityPole_StreetLight){
						switch(((TileEntityPole_StreetLight) component).state){
						case ON:
							for(RenderVehicle_LightPart lightPart : componentLightMap.get(component.definition)){
								if(lightPart.type.equals(LightType.STREETLIGHT)){
									lightPart.renderOnBlock(tile.world, tile.position, component.definition.packID, "textures/poles/" + component.definition.systemName + ".png", true);
								}
							}
						default:
							//This only happens if the light is off.
							break;
						}
					}else if(component instanceof TileEntityPole_Sign){
						//Render text, if we have any.
						if(component.definition.general.textLines != null){
							for(byte i=0; i<component.definition.general.textLines.length; ++i){
								TextLine text = component.definition.general.textLines[i];
								GL11.glPushMatrix();
								GL11.glTranslatef(text.xPos, text.yPos, text.zPos + 0.01F);
								GL11.glScalef(text.scale/16F, text.scale/16F, text.scale/16F);
								GL11.glRotatef(180, 1, 0, 0);
								WrapperGUI.drawText(((TileEntityPole_Sign) component).getTextLines().get(i), 0, 0, Color.decode(text.color), true, false, 0);
								GL11.glPopMatrix();
							}
							//Set color back to white to allow us to render other components.
							WrapperRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
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
