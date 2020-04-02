package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.instances.BlockPoleStreetlight;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleStreetlight;
import minecrafttransportsimulator.wrappers.WrapperRender;
import net.minecraft.util.math.Vec3d;

public class RenderStreetlight extends ARenderTileEntityBase<TileEntityPoleStreetlight, BlockPoleStreetlight>{
		
	public RenderStreetlight(){}
	
	@Override
	public void render(TileEntityPoleStreetlight tile, BlockPoleStreetlight block, float partialTicks){
		//Start rendering by translating to the position of the TE and rotating it.
		GL11.glPushMatrix();
		GL11.glTranslated(tile.position.x, tile.position.y, tile.position.z);
		GL11.glTranslatef(0.5F, 0F, 0.5F);
		GL11.glRotatef(-block.getRotation(tile.world, tile.position), 0, 1, 0);
	
		//Disable the lighting prior to rendering the lights.
		WrapperRender.setLightmapState(false);
		GL11.glDisable(GL11.GL_LIGHTING);
		float lightBrightness = tile.world.getLightBrightness(tile.position);
		
		//Translate and render the colored square.
		GL11.glTranslatef(0, 6.45F/16F, 6F/16F);
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
		Vec3d endpointVec = new Vec3d(0, 0, 6);
		GL11.glDepthMask(false);
		for(byte j=0; j<=2; ++j){
			drawLightCone(endpointVec, false);
    	}
		drawLightCone(endpointVec, true);
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
		GL11.glPopMatrix();
	}
}
