package minecrafttransportsimulator.mcinterface;

import java.io.InputStream;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightRenderer;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightVoxelWindow;

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
     * Returns the default font texture base-location folder.  This changes between game versions since new MC versions
     * don't have the texture files and we pack them in ourselves.
     */
    String getDefaultFontTextureFolder();

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

    /**
     * Returns this version's GL calls for the deferred dynamic light pass ({@link DynamicLightRenderer}), or null if
     * its context cannot draw it: dynamic lights then light the world in their fallback mode (block light, cone and flare).
     */
    default IInterfaceDynamicLightingGL getDynamicLightGL() {
        return null;
    }

    /**
     * Marks the chunk sections touching the passed-in block boxes for a mesh rebuild, so block light raised by
     * dynamic lights under a shader pack shows.  Box {@code i} is {@code boxes[6i .. 6i+5]} = minX minY minZ maxX maxY maxZ, inclusive.
     */
    default void markLightSectionsDirty(int[] boxes, int count) {
    }

    /**
     * Daylight at this point, 0..1: its open-sky share times the sun's height, weather included.  0 at night even
     * in the open.  Dims dynamic lights by day.
     */
    float getDaylight(AWrapperWorld world, Point3D position);

    /**
     * The world's rain level, 0..1, independent of position.  Thickens the air dynamic light beams show in.
     */
    float getRainLevel(AWrapperWorld world, float partialTicks);

    /**
     * Reads which blocks of the 16-block section {@code sx, sy, sz} are solid into the dynamic light block grid,
     * with {@link DynamicLightVoxelWindow#set} or {@link DynamicLightVoxelWindow#fillSection}.  Returns false if this
     * version cannot, which leaves the section empty (no block shadows).
     */
    boolean readDynamicLightSection(AWrapperWorld world, int sx, int sy, int sz, DynamicLightVoxelWindow window);
}
