package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
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
    void renderItemModel(IWrapperItemStack stack, TransformationMatrix transform);

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
     * Applies an OpenGL transform to the current pipeline based on the
     * passed-in matrix.  Allows for inverted transformation, should this
     * be desired.  In this case, the rotation will be transposed and the
     * translation will be inverted.  Fourth-row elements will be left as-is.
     */
    void applyTransformOpenGL(TransformationMatrix matrix, boolean inverted);

    /**
     * Deletes the cached vertices associated with the specified {@link RenderableObject}.
     */
    void deleteVertices(RenderableObject object);

    /**
     * Downloads the passed-in texture to be parsed and bound.  The texture is downloaded from the
     * URL and then added to the texture rendering system.  The integer of the The instance
     * of the texture is cached in this class once created for later use, so feel free to not
     * cache the string URL that is passed-in.  If the texture downloading was successful, null is
     * returned.  Otherwise, an error message is returned.  Bind the downloaded texture by calling
     * {@link #bindTexture(String)} with the passed-in URL.
     */
    String downloadURLTexture(String textureURL);

    /**
     * Helper method to completely disable or enable lighting.
     * This disables both the system lighting and internal lighting.
     */
    void setLightingState(boolean enabled);

    /**
     * Updates the internal lightmap to be consistent with the light at the
     * passed-in position.
     */
    void setLightingToPosition(Point3D position);

    /**
     * Manually enables and disables blending. Do NOT use this during normal model rendering, as it
     * can seriously mess up states.  Only use this for simple things, like GUIs or screen overlays.
     * This is not reset with resetting states, so make sure to turn it back off when you're done.
     */
    void setBlend(boolean enabled);

    /**
     * Returns true if bounding boxes should be rendered.
     */
    boolean shouldRenderBoundingBoxes();
}
