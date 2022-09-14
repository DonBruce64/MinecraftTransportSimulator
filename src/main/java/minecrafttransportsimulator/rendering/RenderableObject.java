package minecrafttransportsimulator.rendering;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Class designed to represent a renderable object.  Said object has at minimum some
 * geometry, though this can be a cached set of vertices or a hard-coded saved set.
 * It may also have a texture, though this texture may be a single solid white sheet
 * for shader-compatible solid rendering.  In this case, the color will be specified,
 * and should be used to change the color prior to rendering.  Various other properties
 * exist for lighting/blending.  In all cases, similar renderable objects should be
 * grouped together and batch-rendered for efficiency.  The primary grouping should be
 * texture, as this prevents re-binds.  Secondary should be color.  Lighting will usually
 * dictate when the object can render rather than in what order (solid vs translucent pass).
 * To assist with this, the equals() method checks texture and color and, if they are identical,
 * returns true.  This allows for said objects to be used as map-keys for easier grouping.
 * Note that this does NOT include the actual vertex data in this equality check.
 * <p>
 * For said vertex data, the data order is as follows:
 *  <ul>
 *  <li>The nX-coordinate of the normal for the vertex, in the x-dimension.
 *  <li>The nY-coordinate of the normal for the vertex, in the y-dimension.
 *  <li>The nZ-coordinate of the normal for the vertex, in the z-dimension.
 *  <li>The u-coordinate of the UV-mapping for the vertex.
 *  <li>The v-coordinate of the UV-mapping for the vertex.
 *  <li>The x-coordinate of a vertex on the model.
 *  <li>The y-coordinate of a vertex on the model.
 *  <li>The z-coordinate of a vertex on the model.
 *  </ul>
 * <p>
 *  Note that this object can render lines as well as tris.  For lines, {@link #lineWidth} should
 *  be set to a non-zero number.  If this is the case, then the buffer will be interpreted as line
 *  data and line rendering will occur.   the data format is as follows:
 * <ul>
 *  <li>The x-coordinate of the first point on the line.
 *  <li>The y-coordinate of the first point on the line.
 *  <li>The z-coordinate of the first point on the line.
 *  <li>The x-coordinate of the second point on the line.
 *  <li>The y-coordinate of the second point on the line.
 *  <li>The z-coordinate of the second point on the line.
 *  </ul>
 *
 * @author don_bruce
 */
public class RenderableObject {

    public final String name;
    public String texture;
    public final ColorRGB color;
    public FloatBuffer vertices;
    public final boolean cacheVertices;

    public boolean isTranslucent;
    public int cachedVertexIndex = -1;
    public BlendState blend = BlendState.SOLID;
    public float alpha = 1.0F;
    public float lineWidth = 0.0F;
    public final TransformationMatrix transform = new TransformationMatrix();
    public boolean disableLighting;
    public boolean ignoreWorldShading;
    public boolean enableBrightBlending;

    /**
     * The Global texture.  This contains all block/item textures for the game.  Used when rendering said blocks/items.
     **/
    public static final String GLOBAL_TEXTURE_NAME = "GLOBAL";
    /**
     * The Particle texture.  This contains all built-in particle textures for the game.  Used when rendering particles with default textures.
     **/
    public static final String PARTICLE_TEXTURE_NAME = "PARTICLE";

    private static final int[][] FACE_POINT_INDEXES = new int[][]{
            //X-axis.
            new int[]{0, 1, 3, 2}, new int[]{5, 4, 6, 7},

            //Y-axis.
            new int[]{6, 2, 3, 7}, new int[]{5, 1, 0, 4},

            //Z-axis.
            new int[]{4, 0, 2, 6}, new int[]{1, 5, 7, 3}};
    private static final float[][] FACE_NORMALS = new float[][]{
            //X-axis.
            new float[]{-1.0F, 0.0F, 0.0F}, new float[]{1.0F, 0.0F, 0.0F},

            //Y-axis.
            new float[]{0.0F, 1.0F, 0.0F}, new float[]{0.0F, -1.0F, 0.0F},

            //Z-axis.
            new float[]{0.0F, 0.0F, -1.0F}, new float[]{0.0F, 0.0F, 1.0F}};
    private static final int[][] WIREFRAME_POINT_INDEXES = new int[][]{
            //Bottom.
            new int[]{0, 1}, new int[]{4, 5}, new int[]{0, 4}, new int[]{1, 5},

            //Top.
            new int[]{2, 3}, new int[]{6, 7}, new int[]{2, 6}, new int[]{3, 7},

            //Sides.
            new int[]{0, 2}, new int[]{1, 3}, new int[]{4, 6}, new int[]{5, 7}};

    private static final int BUFFERS_PER_LINE = 6;
    private static final int BUFFERS_PER_VERTEX = 8;
    private static final int BUFFERS_PER_FACE = BUFFERS_PER_VERTEX * 3;
    //2 faces per side for tris, and 6 total sides.
    private static final int BUFFERS_PER_HOLGRAPHIC_BOX = 2 * 6 * BUFFERS_PER_FACE;
    //12 lines per box.
    private static final int BUFFERS_PER_WIREFRAME_BOX = 12 * BUFFERS_PER_LINE;

    public RenderableObject(String name, String texture, ColorRGB color, FloatBuffer vertices, boolean cacheVertices) {
        this.name = name;
        this.texture = texture;
        this.color = color;
        this.cacheVertices = cacheVertices;
        this.isTranslucent = name.toLowerCase().contains(AModelParser.TRANSLUCENT_OBJECT_NAME);
        this.vertices = vertices;
        transform.resetTransforms();
    }

    /*Shortened constructor used for lines.  Automatically sets line width and lighting.**/
    public RenderableObject(ColorRGB color, int numberLines) {
        this("", null, color, FloatBuffer.allocate(numberLines * BUFFERS_PER_LINE), false);
        lineWidth = 2.0F;
        disableLighting = true;
    }

    /*Shortened constructor used for bounding boxes.  Automatically sets lighting and translucency depending on if the box
     * is holgraphic or wireframe as defined by the final boolean.**/
    public RenderableObject(ColorRGB color, boolean holgraphic) {
        this("", holgraphic ? "mts:textures/rendering/holobox.png" : null, color, FloatBuffer.allocate(holgraphic ? BUFFERS_PER_HOLGRAPHIC_BOX : BUFFERS_PER_WIREFRAME_BOX), false);
        if (holgraphic) {
            isTranslucent = true;
            alpha = 0.5F;
        } else {
            lineWidth = 2.0F;
        }
        disableLighting = true;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof RenderableObject) {
            RenderableObject otherProperties = (RenderableObject) object;
            return this.texture.equals(otherProperties.texture) && this.color.equals(otherProperties.color);
        } else {
            return false;
        }
    }

    /**
     * Renders the vertices from this object.  If they were cached, it renders them as such and destroys
     * the reference to the static vertices object.  This is to free up the FloatBuffer for re-use.  We
     * would normally set it to null during construction, but it is realized that having this for post-processing
     * after model parsing is ideal, so it's not destroyed until render.
     */
    public void render() {
        InterfaceManager.renderingInterface.renderVertices(this);
    }

    /**
     * Adds a line to the {@link #vertices} of this object.
     */
    public void addLine(Point3D point1, Point3D point2) {
        vertices.put((float) point1.x);
        vertices.put((float) point1.y);
        vertices.put((float) point1.z);
        vertices.put((float) point2.x);
        vertices.put((float) point2.y);
        vertices.put((float) point2.z);
    }

    /**
     * sets the holographic {@link BoundingBox} box to the {@link #vertices} of this object.
     * The box added will be centered at 0,0,0.
     */
    public void setHolographicBoundingBox(BoundingBox box) {
        float[][] points = box.getEdgePoints();
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                //Add normals and UVs.  UVs get normalized later.
                vertices.put(FACE_NORMALS[i]);
                vertices.put(0.0F);
                vertices.put(0.0F);

                //If we are index 3, use point 0.
                //If we are index 4, use point 2.
                //If we are index 5, use point 3.
                switch (j) {
                    case (3):
                        vertices.put(points[FACE_POINT_INDEXES[i][0]]);
                        break;
                    case (4):
                        vertices.put(points[FACE_POINT_INDEXES[i][2]]);
                        break;
                    case (5):
                        vertices.put(points[FACE_POINT_INDEXES[i][3]]);
                        break;
                    default:
                        vertices.put(points[FACE_POINT_INDEXES[i][j]]);
                        break;
                }
            }
        }

        //Normalize UVs to align with texture.
        normalizeUVs();

        //Flip for rendering.
        vertices.flip();
    }

    /**
     * Sets the wireframe {@link BoundingBox} box to the {@link #vertices} of this object.
     * The box added will be centered at 0,0,0, and will extend to the bound of the BoundingBox.
     */
    public void setWireframeBoundingBox(BoundingBox box) {
        float[][] points = box.getEdgePoints();
        for (int[] indexes : WIREFRAME_POINT_INDEXES) {
            vertices.put(points[indexes[0]]);
            vertices.put(points[indexes[1]]);
        }
        vertices.flip();
    }

    /**
     * Normalizes the UVs in this object.  This is done to re-map them to the 0->1 texture space
     * for overridden textures such as lights and windows.
     */
    public void normalizeUVs() {
        int verticesInObject = vertices.capacity() / 8;
        for (int i = 0; i < verticesInObject; ++i) {
            if (verticesInObject > 3 && i % 6 >= 3) {
                //Second-half of a quad.
                switch (i % 6) {
                    case (3):
                        vertices.put(i * 8 + 3, 0.0F);
                        vertices.put(i * 8 + 4, 0.0F);
                        break;
                    case (4):
                        vertices.put(i * 8 + 3, 1.0F);
                        vertices.put(i * 8 + 4, 1.0F);
                        break;
                    case (5):
                        vertices.put(i * 8 + 3, 1.0F);
                        vertices.put(i * 8 + 4, 0.0F);
                        break;
                }
            } else {
                //Normal tri or first half of quad using tri mapping.
                switch (i % 6) {
                    case (0):
                    case (5):
                        vertices.put(i * 8 + 3, 0.0F);
                        vertices.put(i * 8 + 4, 0.0F);
                        break;
                    case (1):
                        vertices.put(i * 8 + 3, 0.0F);
                        vertices.put(i * 8 + 4, 1.0F);
                        break;
                    case (2):

                    case (3):
                        vertices.put(i * 8 + 3, 1.0F);
                        vertices.put(i * 8 + 4, 1.0F);
                        break;
                    case (4):
                        vertices.put(i * 8 + 3, 1.0F);
                        vertices.put(i * 8 + 4, 0.0F);
                        break;
                }
            }
        }
    }

    /**
     * Destroys this object, resetting all references in it for use in other areas.
     * Note that this is not required if {@link #cacheVertices} is false as no external
     * references will be kept in that mode.
     */
    public void destroy() {
        vertices = null;
        InterfaceManager.renderingInterface.deleteVertices(this);
    }

    public enum BlendState {
        SOLID,
        TRANSLUCENT,
        BRIGHT_BLENDED
    }
}
