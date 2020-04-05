package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.instances.BlockFuelPump;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperGame;
import minecrafttransportsimulator.wrappers.WrapperRender;

public class RenderFuelPump extends ARenderTileEntityBase<TileEntityFuelPump, BlockFuelPump>{
	
	@Override
	public void render(TileEntityFuelPump tile, BlockFuelPump block, float partialTicks){
		//Render the light-up texture for the pump state.
		GL11.glPushMatrix();
		GL11.glScalef(0.0625F, 0.0625F, 0.0625F);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glTranslatef(0, -17.25F, -4.001F);
		GL11.glScalef(0.125F, 0.125F, 0.125F);
		GL11.glDisable(GL11.GL_LIGHTING);
		WrapperRender.setLightmapState(false);
		
		//Render fluid name text.
		WrapperGUI.drawText(tile.getFluidLevel() > 0 ? WrapperGame.getFluidName(tile.getFluid()).toUpperCase() : "", 0,  0, Color.ORANGE, true, false, 0);
		
		//Render fluid inside pump amount.
		WrapperGUI.drawText(WrapperGUI.translate("tile.fuelpump.level"), -40,  10, Color.ORANGE, false, false, 0);
		String fluidLevel = String.format("%05d", tile.getFluidLevel());
		WrapperGUI.drawText(fluidLevel + "mb", 0,  10, Color.ORANGE, false, false, 0);
		
		//Render fluid dispensed amount.
		WrapperGUI.drawText(WrapperGUI.translate("tile.fuelpump.dispensed"), -40,  20, Color.ORANGE, false, false, 0);
		String fluidDispensed = String.format("%05d", tile.totalTransfered);
		WrapperGUI.drawText(fluidDispensed + "mb", 0,  20, Color.ORANGE, false, false, 0);
		
		//Reset states.
		WrapperRender.setLightmapState(true);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}
}
