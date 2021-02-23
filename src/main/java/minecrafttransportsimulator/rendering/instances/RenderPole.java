package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Core;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;

public class RenderPole extends ARenderTileEntityBase<TileEntityPole>{
	private static final Map<JSONPoleComponent, Map<Axis, Integer>> connectorDisplayListMap = new HashMap<JSONPoleComponent, Map<Axis, Integer>>();
	private static final Map<JSONPoleComponent, Map<Axis, Integer>> solidConnectorDisplayListMap = new HashMap<JSONPoleComponent, Map<Axis, Integer>>();
	private static final Map<JSONPoleComponent, List<RenderableModelObject<ATileEntityPole_Component>>> componentObjectListMap = new HashMap<JSONPoleComponent, List<RenderableModelObject<ATileEntityPole_Component>>>();
	
	@Override
	public void renderModel(TileEntityPole pole, float partialTicks){
		//First render all connections.
		//These are based on the pole itself, so we first need to get the pole.
		TileEntityPole_Core coreComponent = (TileEntityPole_Core) pole.components.get(Axis.NONE);
		if(coreComponent != null){
			//If we don't have the model parsed, do so now.
			if(!connectorDisplayListMap.containsKey(coreComponent.definition)){
				Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(coreComponent.definition.getModelLocation());
				
				Map<Axis, Integer> connectorDisplayLists = new HashMap<Axis, Integer>();
				Map<Axis, Integer> solidConncectorDisplayLists = new HashMap<Axis, Integer>();
				for(Axis axis : Axis.values()){
					if(parsedModel.containsKey(axis.name().toLowerCase())){
						connectorDisplayLists.put(axis, OBJParser.generateDisplayList(parsedModel.get(axis.name().toLowerCase())));
					}
					if(parsedModel.containsKey(axis.name().toLowerCase() + "_solid")){
						solidConncectorDisplayLists.put(axis, OBJParser.generateDisplayList(parsedModel.get(axis.name().toLowerCase() + "_solid")));
					}
				}
				connectorDisplayListMap.put(coreComponent.definition, connectorDisplayLists);
				solidConnectorDisplayListMap.put(coreComponent.definition, solidConncectorDisplayLists);
			}
			
			//Render the connectors.  Don't do this on the blending pass 1.
			if(InterfaceRender.getRenderPass() != 1){
				InterfaceRender.bindTexture(coreComponent.definition.getTextureLocation(coreComponent.subName));
				for(Axis axis : Axis.values()){
					if(axis.equals(Axis.NONE)){
						GL11.glCallList(connectorDisplayListMap.get(coreComponent.definition).get(axis));
					}else if(axis.blockBased){
						Point3d offset = axis.getOffsetPoint(pole.position);
						boolean adjacentPole = pole.world.getBlock(offset) instanceof BlockPole;
						boolean solidBlock = pole.world.isBlockSolid(offset, axis.getOpposite());
						boolean slabBlock = (axis.equals(Axis.DOWN) && pole.world.isBlockBottomSlab(offset)) || (axis.equals(Axis.UP) && pole.world.isBlockTopSlab(offset));
						if(adjacentPole || solidBlock){
							if(connectorDisplayListMap.get(coreComponent.definition).containsKey(axis)){
								GL11.glCallList(connectorDisplayListMap.get(coreComponent.definition).get(axis));
							}
						}
						if(solidBlock){
							if(solidConnectorDisplayListMap.get(coreComponent.definition).containsKey(axis)){
								GL11.glCallList(solidConnectorDisplayListMap.get(coreComponent.definition).get(axis));
							}
						}else if(slabBlock){
							//Slab.  Render the center and proper portion and center again to render at slab height.
							//Also render solid portion as it's a solid block.
							Axis oppositeAxis = axis.getOpposite();
							if(connectorDisplayListMap.get(coreComponent.definition).containsKey(axis)){
								GL11.glCallList(connectorDisplayListMap.get(coreComponent.definition).get(axis));
								//Offset to slab block.
								GL11.glTranslatef(0.0F, axis.yOffset, 0.0F);
								
								//Render upper and center section.  Upper joins lower above slab.
								if(connectorDisplayListMap.get(coreComponent.definition).containsKey(oppositeAxis)){
									GL11.glCallList(connectorDisplayListMap.get(coreComponent.definition).get(oppositeAxis));
								}
								GL11.glCallList(connectorDisplayListMap.get(coreComponent.definition).get(Axis.NONE));
								
								//Offset to top of slab and render solid lower connector, if we have one.
								GL11.glTranslatef(0.0F, -axis.yOffset/2F, 0.0F);
								if(solidConnectorDisplayListMap.get(coreComponent.definition).containsKey(axis)){
									GL11.glCallList(solidConnectorDisplayListMap.get(coreComponent.definition).get(axis));
								}
								
								//Translate back to the normal position.
								GL11.glTranslatef(0.0F, -axis.yOffset/2F, 0.0F);
							}
						}
					}
				}
			}
		}
		
		//Done rendering core and connections.  Render components.
		for(Axis axis : Axis.values()){
			if(!axis.equals(Axis.NONE)){
				if(pole.components.containsKey(axis)){
					//Cache the displaylists and lights if we haven't already.
					ATileEntityPole_Component component = pole.components.get(axis);
					if(!componentObjectListMap.containsKey(component.definition)){
						String modelLocation = component.definition.getModelLocation();
						Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(modelLocation);
						componentObjectListMap.put(component.definition, OBJParser.generateRenderables(component, modelLocation, parsedModel, component.definition.rendering != null ? component.definition.rendering.animatedObjects : null));
					}
					
					//Rotate to component axis and render.
					GL11.glPushMatrix();
					GL11.glRotatef(axis.yRotation, 0, 1, 0);
					GL11.glTranslatef(0, 0, pole.definition.pole.radius + 0.001F);
					
					//Render the component parts.
					InterfaceRender.setTexture(component.definition.getTextureLocation(component.subName));
					List<RenderableModelObject<ATileEntityPole_Component>> modelObjects = componentObjectListMap.get(component.definition);
					for(RenderableModelObject<ATileEntityPole_Component> modelObject : modelObjects){
						if(modelObject.applyAfter == null){
							modelObject.render(component, partialTicks, modelObjects);
						}
					}
					
					//Render any static text.
					InterfaceRender.renderTextMarkings(component, null);
					
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
}
