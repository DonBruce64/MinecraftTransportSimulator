package minecrafttransportsimulator.mcinterface;

import java.io.InputStream;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableVertices;

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
     * Returns a stream of the texture specified.  This can vary depending on what texture packs are loaded!
     */
    InputStream getTextureStream(String name);

    /**
     * Renders the item model for the passed-in component.  Only
     * renders the item model: does not render text for counts.
     */
    void renderItemModel(GUIComponentItem component);

    /**
     * Renders according to the set data.
     * If the object is ever deleted, and {@link RenderableVertices#cacheVertices} is true,
     * then {@link #deleteVertices(RenderableData)} should be called to free up the 
     * respective GPU memory.  Calling this is not required if no caching is performed.
     * If the state of the data has changed since the last render, pass in true for the boolean.
     * This allows the rendering system to perform any re-caching as required.
     */
    void renderVertices(RenderableData data, boolean changedSinceLastRender);

    /**
     * Deletes the cached vertices associated with the specified {@link RenderableData}.
     */
    void deleteVertices(RenderableData data);

    /**
     * Binds a URL texture to a stream containing an image.  Pass in a null stream to bind the missing texture to this URL.
     * Returns true if the texture was bound, false if it couldn't be.
     */
    boolean bindURLTexture(String textureURL, InputStream strea);

    /**
     * Binds a URL GIF that was downloaded.
     * Returns true if the texture was bound, false if it couldn't be.
     */
    boolean bindURLGIF(String textureURL, ParsedGIF gif);

    /**
     * Returns an integer that represents the lighting state at the position.
     * This value is version-dependent, and should be stored in {@link RenderableData#worldLightValue}
     */
    int getLightingAtPosition(Point3D position);

    /**
     * Returns true if bounding boxes should be rendered.
     */
    boolean shouldRenderBoundingBoxes();
}
