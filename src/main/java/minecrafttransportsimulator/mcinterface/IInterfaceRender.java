package minecrafttransportsimulator.mcinterface;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.rendering.components.AParticle;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, and registering rendering systems
 * for TESRs, items, and entities.
 *
 * @author don_bruce
 */
public interface IInterfaceRender{
	
	/**
	 *  Gets the current render pass.  0 for solid blocks, 1 for transparent,
	 *  and -1 for end-of world final renders.
	 */
	public int getRenderPass();
	
	/**
	 *  Returns true if bounding boxes should be rendered.
	 */
	public boolean shouldRenderBoundingBoxes();
	
	/**
	 *  Binds the passed-in texture to be rendered.  The instance of the texture is 
	 *  cached in this class once created for later use, so feel free to not cache
	 *  the string values that are passed-in.
	 */
	public void bindTexture(String textureLocation);
	
	/**
	 *  Like bindTexture, but this method also sets the texture for binding recall later via recallTexture.
	 *  This allows for us to recall specific textures anywhere in the code.  Useful when we don't know what
	 *  we will render between this call and another call, but we do know that we want this texture to be
	 *  re-bound if any other textures were bound.
	 */
	public void setTexture(String textureLocation);
	
	/**
	 *  Re-binds the last saved texture.
	 */
	public void recallTexture();
	
	/**
	 *  Helper method to completely disable or enable lighting.
	 *  This disables both the system lighting and internal lighting.
	 */
	public void setLightingState(boolean enabled);
	
	/**
	 *  Enables or disables OpenGL lighting for this draw sequence.
	 *  This effectively prevents OpenGL lighting calculations on textures.
	 *  Do note that the normal internal lightmapping will still be applied.
	 *  This can be used to prevent OpenGL from doing shadowing on things
	 *  that it gets wrong, such as text. 
	 */
	public void setSystemLightingState(boolean enabled);
	
	/**
	 *  Enables or disables internal lighting for this draw sequence.
	 *  This disables the internal lightmapping, effectively making the rendered
	 *  texture as bright as it would be during daytime.  Do note that the system
	 *  lighting calculations for shadowing will still be applied to the model.
	 */
	public void setInternalLightingState(boolean enabled);
	
	/**
	 *  Updates the internal lightmap to be consistent with the light at the
	 *  passed-in entitie's location.  This will also enable lighting should
	 *  the current render pass be -1.
	 */
	public void setLightingToEntity(AEntityBase entity);
	
	/**
	 *  Updates the internal lightmap to be consistent with the light at the
	 *  passed-in block's location.  This will also enable lighting should
	 *  the current render pass be -1.
	 */
	public void setLightingToBlock(Point3i location);
	
	/**
	 *  Sets the blend state to enabled or disabled.  Also allows for
	 *  the blend state to be set to accommodate beam lights with brightening
	 *  properties rather than regular alpha blending.
	 */
	public void setBlendState(boolean enabled, boolean brightBlend);
	
	/**
	 *  Sets MC color to the passed-in color.  Required when needing to keep MC states happy.
	 *  In particular, this is needed if colors are changed during MC internal draw calls,
	 *  such as rendering a string, changing the color, and then rendering another string.
	 */
	public void setColorState(float red, float green, float blue, float alpha);
	
	/**
	 *  Resets all the rendering states to the appropriate values for the pass we are in.
	 *  Useful after doing a rendering routine where states may not be correct for the pass.
	 */
	public void resetStates();
	
	/**
	 *  Adjusts the camera zoom, zooming in or out depending on the flag.
	 */
	public void changeCameraZoom(boolean zoomIn);
	
	/**
	 *  This method manually renders all riders on an entity.  Useful if you're rendering the entity manually
	 *  and the entity and its riders have been culled from rendering.
	 */
	public void renderEntityRiders(AEntityBase entity, float partialTicks);
	
	/**
	 *  Spawns a particle into the world.  Particles are simply entities that are client-side only.
	 *  This is handy if you have a lot of them flying around but could care less where they are and
	 *  don't want to hamper the server with tons of ticking entities.
	 */
	public void spawnParticle(AParticle particle);
	
	/**
	 *  Spawns the particles for the block at the passed-in position.
	 *  This also plays the block breaking sound.  It does not actually break
	 *  the block.  Such breakage must be done on the server.
	 */
	public void spawnBlockBreakParticles(Point3i point);
	
	/**
	 *  Renders all the text markings given the passed-in parameters.
	 *  This should only be called in pass 0, as we don't do any alpha blending in this routine.
	 *  Return true if we rendered anything.  This lets any rendering systems reset their bound texture if required.
	 *  If inheritedColor is non-null, then the passed-in color will be rendered rather than the color in the textDefinition,
	 *  should the textDefinition be set to use the inheritedColor.
	 */
	public boolean renderTextMarkings(List<JSONText> textDefinitions, List<String> textLines, String inheritedColor, String objectRendering, boolean lightsOn);
}
