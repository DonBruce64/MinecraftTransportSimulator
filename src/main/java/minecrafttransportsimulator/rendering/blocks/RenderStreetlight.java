package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.instances.BlockPoleStreetlight;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleStreetlight;
import minecrafttransportsimulator.wrappers.WrapperRender;

public class RenderStreetlight extends ARenderTileEntityBase<TileEntityPoleStreetlight, BlockPoleStreetlight>{
		
	@Override
	public void render(TileEntityPoleStreetlight tile, BlockPoleStreetlight block, float partialTicks){
		//Disable the lighting prior to rendering the lights.
		WrapperRender.setLightmapState(false);
		GL11.glDisable(GL11.GL_LIGHTING);
		float lightBrightness = tile.world.getLightBrightness(tile.position);
		
		//Translate and render the colored square.
		GL11.glTranslatef(0, -1.555F/16F, 6F/16F);
		GL11.glRotatef(90, 1, 0, 0);
		renderLightedSquare(4F/16F, lightBrightness,  Color.WHITE, "textures/rendering/light.png");
		
		//Setup for rendering the light beam.
		GL11.glPushMatrix();
    	GL11.glDisable(GL11.GL_LIGHTING);
    	GL11.glEnable(GL11.GL_BLEND);
    	GL11.glColor4f(1, 1, 1, Math.min(1.0F, lightBrightness/2F));
    	
    	//Allows making things brighter by using alpha blending.
    	GL11.glDepthMask(false);
    	GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA);
		
    	//Render multiple cones for lights.
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, -0.15F);
		Point3d endpoint = new Point3d(0D, 0D, 6D);
		GL11.glDepthMask(false);
		for(byte j=0; j<=2; ++j){
			drawLightCone(endpoint, false);
    	}
		drawLightCone(endpoint, true);
		GL11.glPopMatrix();
		
		//Turn off blending.
    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    	GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
		
		//Reset states.
		GL11.glEnable(GL11.GL_LIGHTING);
		WrapperRender.setLightmapState(true);
		GL11.glColor4f(1, 1, 1, 1);
	}
}
