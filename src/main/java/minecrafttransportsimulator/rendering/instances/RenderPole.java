package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Core;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;
import minecrafttransportsimulator.rendering.components.ITextProvider;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;

public class RenderPole extends ARenderTileEntityBase<TileEntityPole>{
	private static final Map<JSONPoleComponent, Map<Axis, Integer>> connectorDisplayListMap = new HashMap<JSONPoleComponent, Map<Axis, Integer>>();
	private static final Map<JSONPoleComponent, Map<Axis, Integer>> solidConnectorDisplayListMap = new HashMap<JSONPoleComponent, Map<Axis, Integer>>();
	private static final Map<JSONPoleComponent, Integer> componentDisplayListMap = new HashMap<JSONPoleComponent, Integer>();
	private static final Map<JSONPoleComponent, List<RenderableModelObject>> componentObjectListMap = new HashMap<JSONPoleComponent, List<RenderableModelObject>>();
	
	@Override
	public void render(TileEntityPole tile, float partialTicks){
		//First render all connections.
		//These are based on the pole itself, so we first need to get the pole.
		TileEntityPole_Core coreComponent = (TileEntityPole_Core) tile.components.get(Axis.NONE);
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
				InterfaceRender.bindTexture(coreComponent.definition.getTextureLocation(coreComponent.item.subName));
				for(Axis axis : Axis.values()){
					if(axis.equals(Axis.NONE)){
						GL11.glCallList(connectorDisplayListMap.get(coreComponent.definition).get(axis));
					}else{
						Point3i offset = axis.getOffsetPoint(tile.position);
						boolean adjacentPole = tile.world.getBlock(offset) instanceof BlockPole;
						boolean solidBlock = tile.world.isBlockSolid(offset);
						boolean slabBlock = (axis.equals(Axis.DOWN) && tile.world.isBlockBottomSlab(offset)) || (axis.equals(Axis.UP) && tile.world.isBlockTopSlab(offset));
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
				if(tile.components.containsKey(axis)){
					//Cache the displaylists and lights if we haven't already.
					ATileEntityPole_Component component = tile.components.get(axis);
					if(!componentDisplayListMap.containsKey(component.definition)){
						String modelLocation = component.definition.getModelLocation();
						Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(modelLocation);
						componentObjectListMap.put(component.definition, OBJParser.generateRenderables(component, modelLocation, parsedModel, component.definition.rendering != null ? component.definition.rendering.animatedObjects : null));
						componentDisplayListMap.put(component.definition, OBJParser.generateDisplayList(parsedModel));
					}
					
					//Rotate to component axis and render.
					GL11.glPushMatrix();
					GL11.glRotatef(axis.yRotation, 0, 1, 0);
					GL11.glTranslatef(0, 0, tile.definition.general.radius + 0.001F);
					
					//Don't do solid model rendering on the blend pass.
					if(InterfaceRender.getRenderPass() != 1){
						InterfaceRender.setTexture(component.definition.getTextureLocation(component.item.subName));
						GL11.glCallList(componentDisplayListMap.get(component.definition));
					}
					
					//Render any static text.
					if(component instanceof ITextProvider){
						if(InterfaceRender.renderTextMarkings((ITextProvider) component, null)){
							InterfaceRender.recallTexture();
						}
					}
					
					//Render the dynamic parts.  This will mostly just be lights.
					List<RenderableModelObject> modelObjects = componentObjectListMap.get(component.definition);
					for(RenderableModelObject modelObject : modelObjects){
						if(modelObject.applyAfter == null){
							modelObject.render(component, partialTicks, modelObjects);
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
}
