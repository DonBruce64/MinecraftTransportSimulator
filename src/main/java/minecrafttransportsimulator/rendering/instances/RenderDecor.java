package minecrafttransportsimulator.rendering.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.IFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.rendering.components.OBJParser;

public class RenderDecor extends ARenderTileEntityBase<ATileEntityBase<JSONDecor>>{
	private static final Map<JSONDecor, Integer> displayListMap = new HashMap<JSONDecor, Integer>();
		
	@Override
	public void render(ATileEntityBase<JSONDecor> tile, float partialTicks){
		//If we don't have the displaylist and texture cached, do it now.
		if(!displayListMap.containsKey(tile.definition)){
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(tile.definition.getModelLocation());
			int displayListIndex = GL11.glGenLists(1);
			
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				for(Float[] vertex : entry.getValue()){
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
				}
			}
			GL11.glEnd();
			GL11.glEndList();
			displayListMap.put(tile.definition, displayListIndex);
		}
		
		//Don't do solid model rendering on the blend pass.
		if(MasterLoader.renderInterface.getRenderPass() != 1){
			//Bind the texture and render.
			MasterLoader.renderInterface.bindTexture(tile.definition.getTextureLocation());
			GL11.glCallList(displayListMap.get(tile.definition));
			//If we are a fluid tank, render text.
			if(tile.definition.general.textObjects != null && tile instanceof IFluidTankProvider){
				FluidTank tank = ((IFluidTankProvider) tile).getTank();
				String fluidName = tank.getFluidLevel() > 0 ? MasterLoader.coreInterface.getFluidName(tank.getFluid()).toUpperCase() : "";
				String fluidLevel = MasterLoader.coreInterface.translate("tile.fuelpump.level") + String.format("%04.1f", tank.getFluidLevel()/1000F) + "b";
				String fluidDispensed = MasterLoader.coreInterface.translate("tile.fuelpump.dispensed") + String.format("%04.1f", tank.getAmountDispensed()/1000F) + "b";
				
				List<String> textLines = new ArrayList<String>();
				for(byte i=0; i<tile.definition.general.textObjects.size(); ++i){
					switch(i%3){
						case(0) :textLines.add(fluidName); break;
						case(1) :textLines.add(fluidLevel); break;
						case(2) :textLines.add(fluidDispensed); break;
					}
				}
				
				MasterLoader.renderInterface.setLightingState(false);
				MasterLoader.renderInterface.renderTextMarkings(tile.definition.general.textObjects, textLines, null, null, true);
			}
		}
	}
}
