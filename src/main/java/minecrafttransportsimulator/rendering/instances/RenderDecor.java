package minecrafttransportsimulator.rendering.instances;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityFluidTank;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor.TextLine;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperGame;
import minecrafttransportsimulator.wrappers.WrapperRender;

public class RenderDecor extends ARenderTileEntityBase<ATileEntityBase<JSONDecor>, IBlockTileEntity<JSONDecor>>{
	private static final Map<JSONDecor, Integer> displayListMap = new HashMap<JSONDecor, Integer>();
		
	@Override
	public void render(ATileEntityBase<JSONDecor> tile, IBlockTileEntity<JSONDecor> block, float partialTicks){
		//If we don't have the displaylist and texture cached, do it now.
		JSONDecor definition = tile.getDefinition();
		if(!displayListMap.containsKey(definition)){
			String optionalModelName = definition.general.modelName;
			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(definition.packID, "objmodels/decors/" + (optionalModelName != null ? optionalModelName : definition.systemName) + ".obj");
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
			displayListMap.put(definition, displayListIndex);
		}
		
		//Don't do solid model rendering on the blend pass.
		if(WrapperRender.getRenderPass() != 1){
			//Bind the texture and render.
			WrapperRender.bindTexture(definition.packID, "textures/decors/" + definition.systemName + ".png");
			GL11.glCallList(displayListMap.get(definition));
			//If we are a fluid tank render text.
			if(definition.general.textLines != null && tile instanceof ATileEntityFluidTank){
				ATileEntityFluidTank<JSONDecor> tank = (ATileEntityFluidTank<JSONDecor>) tile;
				WrapperRender.setLightingState(false);
				for(byte i=0; i<definition.general.textLines.length; ++i){
					TextLine text = definition.general.textLines[i];
					GL11.glPushMatrix();
					GL11.glTranslatef(text.xPos, text.yPos, text.zPos + (i < 3 ? 0.001F : -0.001F));
					GL11.glScalef(text.scale/16F, text.scale/16F, text.scale/16F);
					GL11.glRotatef(180, 1, 0, 0);
					//Need to rotate 180 for text on other sides.
					if(i >= 3){
						GL11.glRotatef(180, 0, 1, 0);	
					}
					switch(i%3){
						case(0) :{//Render fuel name.
							WrapperGUI.drawText(tank.getFluidLevel() > 0 ? WrapperGame.getFluidName(tank.getFluid()).toUpperCase() : "", 0, 0, Color.decode(text.color), true, false, 0);
							break;
						}
						case(1) :{//Render fuel level.
							String fluidLevel = String.format("%04.1f", tank.getFluidLevel()/1000F);
							WrapperGUI.drawText(WrapperGUI.translate("tile.fuelpump.level") + fluidLevel + "b", 0,  0, Color.decode(text.color), true, false, 0);
							break;
						}
						case(2) :{//Render fuel dispensed.
							String fluidDispensed = String.format("%04.1f", ((TileEntityFuelPump) tank).totalTransfered/1000F);
							WrapperGUI.drawText(WrapperGUI.translate("tile.fuelpump.dispensed") + fluidDispensed + "b", 0, 0, Color.decode(text.color), true, false, 0);
							break;
						}
					}
					GL11.glPopMatrix();
				}
			}
		}
	}
}
