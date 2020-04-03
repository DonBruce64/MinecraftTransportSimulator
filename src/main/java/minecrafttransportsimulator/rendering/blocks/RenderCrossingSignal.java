package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.instances.BlockPoleCrossingSignal;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleCrossingSignal;
import minecrafttransportsimulator.wrappers.WrapperRender;

public class RenderCrossingSignal extends ARenderTileEntityBase<TileEntityPoleCrossingSignal, BlockPoleCrossingSignal>{
	
	@Override
	public void render(TileEntityPoleCrossingSignal tile, BlockPoleCrossingSignal block, float partialTicks){
		//Disable the lighting prior to rendering the lights.
		WrapperRender.setLightmapState(false);
		GL11.glDisable(GL11.GL_LIGHTING);
		float lightBrightness = tile.world.getLightBrightness(tile.position);
		
		//Get the current light state.
		Color lightColor;
		boolean shouldFlash;
		switch(tile.state){
			case WALK: lightColor = Color.GREEN; shouldFlash = false; break;
			case FLASHING_DONTWALK: lightColor = Color.RED; shouldFlash = true; break;
			case DONTWALK: lightColor = Color.RED; shouldFlash = false; break;
			default: lightColor = null; shouldFlash = false; break;		
		}
		
		//Render based on the current state.
		if(lightColor != null && (!shouldFlash || (shouldFlash && (System.currentTimeMillis()%20000 < 10000)))){
			if(lightColor.equals(Color.GREEN)){
				GL11.glTranslatef(0, -6F/16F, 0.145F);
				renderLightedSquare(3F/16F, lightBrightness*0.5F, lightColor, "textures/rendering/walk.png");
			}else{
				GL11.glTranslatef(0, -2F/16F, 0.145F);
				renderLightedSquare(3F/16F, lightBrightness*0.5F, lightColor, "textures/rendering/dontwalk.png");
			}
		}
		
		//Reset states.
		GL11.glEnable(GL11.GL_LIGHTING);
		WrapperRender.setLightmapState(true);
		GL11.glColor4f(1, 1, 1, 1);
	}
}
