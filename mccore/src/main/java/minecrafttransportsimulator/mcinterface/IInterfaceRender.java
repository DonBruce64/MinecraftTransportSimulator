package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.rendering.RenderableObject;

/**
 * Interface for the various MC rendering engines.  This class has functions for
 * binding textures, changing lightmap statuses, etc.
 *
 * @author don_bruce
 */
public interface IInterfaceRender {

    /**
     * Returns a 4-float array for the block break texture at the passed-in position in the passed-in world.
     */
    float[] getBlockBreakTexture(AWrapperWorld world, Point3D position);

    /**
     * Returns a 4-float array for the default block texture.  This doesn't take into account world-state.
     */
    float[] getDefaultBlockTexture(String name);

    /**
     * Renders the item model for the passed-in stack.  Only
     * renders the item model: does not render text for counts.
     */
    void renderItemModel(IWrapperItemStack stack, Point3D translation);

    /**
     * Renders the vertices stored in the passed-in {@link RenderableObject}.
     * If the vertices should be cached per {@link RenderableObject#cacheVertices},
     * then they are done so and a pointer-index is stored into {@link RenderableObject#cachedVertexIndex}.
     * {@link RenderableObject#vertices} is then set to null to free memory.
     * If the object is ever deleted, then {@link #deleteVertices(RenderableObject)}
     * should be called to free up the respective GPU memory.
     */
    void renderVertices(RenderableObject object);

    /**
     * Deletes the cached vertices associated with the specified {@link RenderableObject}.
     */
    void deleteVertices(RenderableObject object);

    /**
     * Downloads the passed-in texture to be parsed and bound.  The texture is downloaded from the
     * URL and then added to the texture rendering system.  The integer of the The instance
     * of the texture is cached in this class once created for later use, so feel free to not
     * cache the string URL that is passed-in.  If the texture downloading was successful, null is
     * returned.  Otherwise, an error message is returned.  Use the new texture by setting 
     * {@link RenderableObject#texture} to the URL.
     */
    String downloadURLTexture(String textureURL);

    /**
     * Updates the internal lightmap to be consistent with the light at the
     * passed-in position.
     */
    void setLightingToPosition(Point3D position);

    /**
     * Returns true if bounding boxes should be rendered.
     */
    boolean shouldRenderBoundingBoxes();
}
