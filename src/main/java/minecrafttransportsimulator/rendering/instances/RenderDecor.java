package minecrafttransportsimulator.rendering.instances;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import mcinterface.BuilderGUI;
import mcinterface.InterfaceGame;
import mcinterface.InterfaceRender;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.IFluidTankProvider;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor.TextLine;
import minecrafttransportsimulator.rendering.components.OBJParser;

public class RenderDecor extends ARenderTileEntityBase<ATileEntityBase<JSONDecor>, IBlockTileEntity<ATileEntityBase<JSONDecor>>>{
	private static final Map<JSONDecor, Integer> displayListMap = new HashMap<JSONDecor, Integer>();
		
	@Override
	public void render(ATileEntityBase<JSONDecor> tile, IBlockTileEntity<ATileEntityBase<JSONDecor>> block, float partialTicks){
		//If we don't have the displaylist and texture cached, do it now.
		if(!displayListMap.containsKey(tile.definition)){
			String optionalModelName = tile.definition.general.modelName;
			Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(tile.definition.packID, "objmodels/decors/" + (optionalModelName != null ? optionalModelName : tile.definition.systemName) + ".obj");
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
		if(InterfaceRender.getRenderPass() != 1){
			//Bind the texture and render.
			InterfaceRender.bindTexture(tile.definition.packID, "textures/decors/" + tile.definition.systemName + ".png");
			GL11.glCallList(displayListMap.get(tile.definition));
			//If we are a fluid tank, render text.
			if(tile.definition.general.textLines != null && tile instanceof IFluidTankProvider){
				FluidTank tank = ((IFluidTankProvider) tile).getTank();
				InterfaceRender.setLightingState(false);
				for(byte i=0; i<tile.definition.general.textLines.length; ++i){
					TextLine text = tile.definition.general.textLines[i];
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
							BuilderGUI.drawText(tank.getFluidLevel() > 0 ? InterfaceGame.getFluidName(tank.getFluid()).toUpperCase() : "", 0, 0, Color.decode(text.color), true, false, 0);
							break;
						}
						case(1) :{//Render fuel level.
							String fluidLevel = String.format("%04.1f", tank.getFluidLevel()/1000F);
							BuilderGUI.drawText(BuilderGUI.translate("tile.fuelpump.level") + fluidLevel + "b", 0,  0, Color.decode(text.color), true, false, 0);
							break;
						}
						case(2) :{//Render fuel dispensed.
							String fluidDispensed = String.format("%04.1f", tank.getAmountDispensed()/1000F);
							BuilderGUI.drawText(BuilderGUI.translate("tile.fuelpump.dispensed") + fluidDispensed + "b", 0, 0, Color.decode(text.color), true, false, 0);
							break;
						}
					}
					GL11.glPopMatrix();
				}
			}
		}
	}
}
