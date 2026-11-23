package minecrafttransportsimulator.rendering;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;

/**
 * Class designed to represent a set of vertices.  Said object has at minimum some geometry. 
 * This can be a cached set of  vertices or a hard-coded saved set.
 *
 * @author don_bruce
 */
public class RenderableVertices {
    public static final float Z_BUFFER_OFFSET = 0.001F;

    public final String name;
    /**Actual vertex data, stored as a series of tris.**/
    public final FloatBuffer vertices;
    public final boolean cacheVertices;
    public final boolean isTranslucent;
    public final boolean isErrorPlaceholder;
    public final boolean isLines;
    public final boolean isOutline;
    private Map<Float, RenderableVertices> outlines;

    /**Index offset array for quad faces required to build a quad-textured box.
     * Order is set here to reference the points in a counter-clockwise order for rendering.
     * Points are set for 6 vertices in a two-tri rendering format to be directly used in vertex data.
     */
    private static final List<List<BoxOffset>> FACE_POINT_INDEXES = Arrays.asList(
            //X-axis.
            Arrays.asList(BoxOffset.MINX_MINY_MINZ, BoxOffset.MINX_MINY_MAXZ, BoxOffset.MINX_MAXY_MAXZ, BoxOffset.MINX_MINY_MINZ, BoxOffset.MINX_MAXY_MAXZ, BoxOffset.MINX_MAXY_MINZ), Arrays.asList(BoxOffset.MAXX_MINY_MAXZ, BoxOffset.MAXX_MINY_MINZ, BoxOffset.MAXX_MAXY_MINZ, BoxOffset.MAXX_MINY_MAXZ, BoxOffset.MAXX_MAXY_MINZ, BoxOffset.MAXX_MAXY_MAXZ),

            //Y-axis.
            Arrays.asList(BoxOffset.MAXX_MAXY_MINZ, BoxOffset.MINX_MAXY_MINZ, BoxOffset.MINX_MAXY_MAXZ, BoxOffset.MAXX_MAXY_MINZ, BoxOffset.MINX_MAXY_MAXZ, BoxOffset.MAXX_MAXY_MAXZ), Arrays.asList(BoxOffset.MAXX_MINY_MAXZ, BoxOffset.MINX_MINY_MAXZ, BoxOffset.MINX_MINY_MINZ, BoxOffset.MAXX_MINY_MAXZ, BoxOffset.MINX_MINY_MINZ, BoxOffset.MAXX_MINY_MINZ),

            //Z-axis.
            Arrays.asList(BoxOffset.MAXX_MINY_MINZ, BoxOffset.MINX_MINY_MINZ, BoxOffset.MINX_MAXY_MINZ, BoxOffset.MAXX_MINY_MINZ, BoxOffset.MINX_MAXY_MINZ, BoxOffset.MAXX_MAXY_MINZ), Arrays.asList(BoxOffset.MINX_MINY_MAXZ, BoxOffset.MAXX_MINY_MAXZ, BoxOffset.MAXX_MAXY_MAXZ, BoxOffset.MINX_MINY_MAXZ, BoxOffset.MAXX_MAXY_MAXZ, BoxOffset.MINX_MAXY_MAXZ));

    /**Index offset array for lines as returned by {@link #getEdgePoints(BoundingBox)}.
     * This array is used to "build" the lines for a box referencing the actual vertex
     * data stored in that function's returned array.  Each entry contains the start and
     * end point for the line needed to create a wireframe box.
     */
    private static final List<List<BoxOffset>> WIREFRAME_POINT_INDEXES = Arrays.asList(
            //Bottom.
            Arrays.asList(BoxOffset.MINX_MINY_MINZ, BoxOffset.MINX_MINY_MAXZ), Arrays.asList(BoxOffset.MAXX_MINY_MINZ, BoxOffset.MAXX_MINY_MAXZ), Arrays.asList(BoxOffset.MINX_MINY_MINZ, BoxOffset.MAXX_MINY_MINZ), Arrays.asList(BoxOffset.MINX_MINY_MAXZ, BoxOffset.MAXX_MINY_MAXZ),

            //Top.
            Arrays.asList(BoxOffset.MINX_MAXY_MINZ, BoxOffset.MINX_MAXY_MAXZ), Arrays.asList(BoxOffset.MAXX_MAXY_MINZ, BoxOffset.MAXX_MAXY_MAXZ), Arrays.asList(BoxOffset.MINX_MAXY_MINZ, BoxOffset.MAXX_MAXY_MINZ), Arrays.asList(BoxOffset.MINX_MAXY_MAXZ, BoxOffset.MAXX_MAXY_MAXZ),

            //Sides.
            Arrays.asList(BoxOffset.MINX_MINY_MINZ, BoxOffset.MINX_MAXY_MINZ), Arrays.asList(BoxOffset.MINX_MINY_MAXZ, BoxOffset.MINX_MAXY_MAXZ), Arrays.asList(BoxOffset.MAXX_MINY_MINZ, BoxOffset.MAXX_MAXY_MINZ), Arrays.asList(BoxOffset.MAXX_MINY_MAXZ, BoxOffset.MAXX_MAXY_MAXZ));

    /**Index offset array for quad normals.
     * This array is used in conjunction with {@link #FACE_POINT_INDEXES} to set vertex data.
     */
    private static final float[][] FACE_NORMALS = new float[][] {
            //X-axis.
            new float[] { -1.0F, 0.0F, 0.0F }, new float[] { 1.0F, 0.0F, 0.0F },

            //Y-axis.
            new float[] { 0.0F, 1.0F, 0.0F }, new float[] { 0.0F, -1.0F, 0.0F },

            //Z-axis.
            new float[] { 0.0F, 0.0F, -1.0F }, new float[] { 0.0F, 0.0F, 1.0F } };

    private static final int VERTEX_BUFFER_NX_OFFSET = 0;
    private static final int VERTEX_BUFFER_NY_OFFSET = 1;
    private static final int VERTEX_BUFFER_NZ_OFFSET = 2;
    private static final int VERTEX_BUFFER_U_OFFSET = 3;
    private static final int VERTEX_BUFFER_V_OFFSET = 4;
    private static final int VERTEX_BUFFER_X_OFFSET = 5;
    private static final int VERTEX_BUFFER_Y_OFFSET = 6;
    private static final int VERTEX_BUFFER_Z_OFFSET = 7;

    private static final int QUAD_TRI1_BOTTOM_RIGHT_INDEX = 0;
    private static final int QUAD_TRI1_TOP_RIGHT_INDEX = 1;
    private static final int QUAD_TRI1_TOP_LEFT_INDEX = 2;
    private static final int QUAD_TRI2_BOTTOM_RIGHT_INDEX = 3;
    private static final int QUAD_TRI2_TOP_LEFT_INDEX = 4;
    private static final int QUAD_TRI2_BOTTOM_LEFT_INDEX = 5;

    private static final int VERTEXES_PER_FACE = 3;
    private static final int FACES_PER_QUAD = 2;
    private static final int VERTEXES_PER_QUAD = VERTEXES_PER_FACE * FACES_PER_QUAD;
    private static final int SIDES_PER_BOX = 6;

    private static final int FLOATS_PER_LINE = 6;
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int FLOATS_PER_FACE = FLOATS_PER_VERTEX * VERTEXES_PER_FACE;
    //2 faces per side for tris, and 6 total sides.
    private static final int FLOATS_PER_HOLGRAPHIC_BOX = SIDES_PER_BOX * FACES_PER_QUAD * FLOATS_PER_FACE;
    //12 lines per box.
    private static final int FLOATS_PER_WIREFRAME_BOX = 12 * FLOATS_PER_LINE;

    private static final int BEAM_CENTER_INDEX = 0;
    private static final int BEAM_OUTER1_INDEX = 1;
    private static final int BEAM_OUTER2_INDEX = 2;
    private static final int SIDES_PER_BEAM = 2;
    private static final int FACES_PER_BEAM = 40;
    private static final float BEAM_OFFSET = -0.15F;

    /**General-use constructor with no special behavior except automatically noting vertices as translucent based on the name**/
    public RenderableVertices(String name, FloatBuffer vertexData, boolean cacheVertices) {
        this(name, vertexData, cacheVertices, false);
    }

    /**General-use constructor that allows special placeholder models to force the missing texture.**/
    public RenderableVertices(String name, FloatBuffer vertexData, boolean cacheVertices, boolean isErrorPlaceholder) {
        this(name, vertexData, cacheVertices, isErrorPlaceholder, false);
    }

    private RenderableVertices(String name, FloatBuffer vertexData, boolean cacheVertices, boolean isErrorPlaceholder, boolean isOutline) {
        this.name = name;
        this.vertices = vertexData;
        this.cacheVertices = cacheVertices;
        this.isTranslucent = name.toLowerCase(Locale.ROOT).contains(AModelParser.TRANSLUCENT_OBJECT_NAME);
        this.isErrorPlaceholder = isErrorPlaceholder;
        this.isLines = false;
        this.isOutline = isOutline;
    }

    /**Constructor used for lines.**/
    public RenderableVertices(int numberLines) {
        this.name = "LINES";
        this.vertices = FloatBuffer.allocate(numberLines * FLOATS_PER_LINE);
        this.cacheVertices = false;
        this.isTranslucent = false;
        this.isErrorPlaceholder = false;
        this.isLines = true;
        this.isOutline = false;
    }

    /**Constructor used for bounding boxes.**/
    public RenderableVertices(boolean holographic) {
        this.name = holographic ? "BOX_HOLOGRAPHIC" : "BOX_WIREFRAME";
        this.vertices = FloatBuffer.allocate(holographic ? FLOATS_PER_HOLGRAPHIC_BOX : FLOATS_PER_WIREFRAME_BOX);
        this.cacheVertices = false;
        this.isTranslucent = holographic;
        this.isErrorPlaceholder = false;
        this.isLines = !holographic;
        this.isOutline = false;
    }

    /**Static method used for single 2D sprites with centered position.  Contains parameter for number of sprite segments and texture
     * Split from constructor due to identical signature.  The passed-in arrays are optional and allow for a 3D built sprite with
     * defined orientation and normals.
    **/
    public static RenderableVertices createSprite(int spriteSegments, List<TransformationMatrix> transforms, List<Point3D> normals) {
        RenderableVertices vertexObject = new RenderableVertices("2D_TEXTURE", FloatBuffer.allocate(VERTEXES_PER_QUAD * FLOATS_PER_VERTEX * spriteSegments), false);
        FloatBuffer vertices = vertexObject.vertices;
        for (int vertexIndex = 0; vertexIndex < VERTEXES_PER_QUAD * spriteSegments; ++vertexIndex) {
            if (normals != null) {
                Point3D normal = normals.get(vertexIndex / VERTEXES_PER_QUAD);
                vertices.put((float) normal.x);
                vertices.put((float) normal.y);
                vertices.put((float) normal.z);
            } else {
                vertices.put(0);
                vertices.put(0);
                vertices.put(1);
            }

            switch (vertexIndex % VERTEXES_PER_QUAD) {
                case (QUAD_TRI1_BOTTOM_RIGHT_INDEX):
                case (QUAD_TRI2_BOTTOM_RIGHT_INDEX): {
                    vertices.put(1);
                    vertices.put(1);
                    vertices.put(0.5F);
                    vertices.put(-0.5F);
                    break;
                }
                case (QUAD_TRI1_TOP_RIGHT_INDEX): {
                    vertices.put(1);
                    vertices.put(0);
                    vertices.put(0.5F);
                    vertices.put(0.5F);
                    break;
                }
                case (QUAD_TRI1_TOP_LEFT_INDEX):
                case (QUAD_TRI2_TOP_LEFT_INDEX): {
                    vertices.put(0);
                    vertices.put(0);
                    vertices.put(-0.5F);
                    vertices.put(0.5F);
                    break;
                }
                case (QUAD_TRI2_BOTTOM_LEFT_INDEX): {
                    vertices.put(0);
                    vertices.put(1);
                    vertices.put(-0.5F);
                    vertices.put(-0.5F);
                    break;
                }
            }
            //Z is always 0.
            vertices.put(0);

            //If we have a transform, apply it now.
            if (transforms != null) {
                int xOffset = vertices.position() - (FLOATS_PER_VERTEX - VERTEX_BUFFER_X_OFFSET);
                int yOffset = vertices.position() - (FLOATS_PER_VERTEX - VERTEX_BUFFER_Y_OFFSET);
                int zOffset = vertices.position() - (FLOATS_PER_VERTEX - VERTEX_BUFFER_Z_OFFSET);
                Point3D helperPoint = new Point3D(vertices.get(xOffset), vertices.get(yOffset), vertices.get(zOffset));
                helperPoint.transform(transforms.get(vertexIndex / VERTEXES_PER_QUAD));
                vertices.put(xOffset, (float) helperPoint.x);
                vertices.put(yOffset, (float) helperPoint.y);
                vertices.put(zOffset, (float) helperPoint.z);
            }
        }
        vertices.flip();
        return vertexObject;
    }

    /**Static method used for creating a cone-like light beam shape.  This is not designed to render with lighting as the normals are invalid.
    **/
    public static RenderableVertices createLightBeams(List<JSONLightBlendableComponent> beamDefs) {
        RenderableVertices vertexObject = new RenderableVertices("LIGHT_BEAMS", FloatBuffer.allocate(beamDefs.size() * SIDES_PER_BEAM * FACES_PER_BEAM * VERTEXES_PER_FACE * FLOATS_PER_VERTEX), false);
        FloatBuffer vertices = vertexObject.vertices;
        for (JSONLightBlendableComponent beamDef : beamDefs) {
            //Get the matrix that is needed to rotate points to the normalized vector.
            RotationMatrix rotation = new RotationMatrix().setToVector(beamDef.axis, false);
            Point3D vertexOffset = new Point3D();
            Point3D centerOffset = beamDef.axis.copy().scale(BEAM_OFFSET).add(beamDef.pos);
            //Go from negative to positive to render both beam-faces in the same loop.
            for (int faceIndex = -FACES_PER_BEAM; faceIndex < FACES_PER_BEAM; ++faceIndex) {
                for (int vertexIndex = 0; vertexIndex < VERTEXES_PER_FACE; ++vertexIndex) {
                    float[] newVertex = new float[FLOATS_PER_VERTEX];
                    //Don't care about normals for beam rendering as it's a blending face, so we just set them to 0.
                    newVertex[VERTEX_BUFFER_NX_OFFSET] = 0F;
                    newVertex[VERTEX_BUFFER_NY_OFFSET] = 0F;
                    newVertex[VERTEX_BUFFER_NZ_OFFSET] = 0F;

                    //Get the current UV points.
                    switch (vertexIndex) {
                        case (BEAM_CENTER_INDEX):
                            newVertex[VERTEX_BUFFER_U_OFFSET] = 0.0F;
                            newVertex[VERTEX_BUFFER_V_OFFSET] = 0.0F;
                            break;
                        case (BEAM_OUTER1_INDEX):
                            newVertex[VERTEX_BUFFER_U_OFFSET] = 0.0F;
                            newVertex[VERTEX_BUFFER_V_OFFSET] = 1.0F;
                            break;
                        case (BEAM_OUTER2_INDEX):
                            newVertex[VERTEX_BUFFER_U_OFFSET] = 1.0F;
                            newVertex[VERTEX_BUFFER_V_OFFSET] = 1.0F;
                            break;
                    }

                    if (vertexIndex == BEAM_CENTER_INDEX) {
                        vertexOffset.set(0, 0, 0);
                    } else {
                        double currentAngleRad;
                        if (faceIndex < 0) {
                            currentAngleRad = vertexIndex == BEAM_OUTER1_INDEX ? 2D * Math.PI * ((faceIndex + 1) / (double) FACES_PER_BEAM) : 2D * Math.PI * (faceIndex / (double) FACES_PER_BEAM);
                        } else {
                            currentAngleRad = vertexIndex == BEAM_OUTER1_INDEX ? 2D * Math.PI * (faceIndex / (double) FACES_PER_BEAM) : 2D * Math.PI * ((faceIndex + 1) / (double) FACES_PER_BEAM);
                        }
                        vertexOffset.x = beamDef.beamDiameter / 2F * Math.cos(currentAngleRad);
                        vertexOffset.y = beamDef.beamDiameter / 2F * Math.sin(currentAngleRad);
                        vertexOffset.z = beamDef.beamLength;
                    }
                    vertexOffset.rotate(rotation).add(centerOffset);
                    newVertex[VERTEX_BUFFER_X_OFFSET] = (float) vertexOffset.x;
                    newVertex[VERTEX_BUFFER_Y_OFFSET] = (float) vertexOffset.y;
                    newVertex[VERTEX_BUFFER_Z_OFFSET] = (float) vertexOffset.z;

                    //Add the actual vertex.
                    vertices.put(newVertex);
                }
            }
        }
        vertices.flip();
        return vertexObject;
    }

    /**
     * Returns a copy of these vertices, offset in their normal direction by the amount specified.
     */
    public RenderableVertices createOverlay(float offset) {
        RenderableVertices offsetObject = new RenderableVertices(this.name + "_OVERLAY", FloatBuffer.allocate(vertices.capacity()), false, isErrorPlaceholder);
        float[] vertexData = new float[FLOATS_PER_VERTEX];
        while (vertices.hasRemaining()) {
            vertices.get(vertexData);
            offsetObject.vertices.put(vertexData, VERTEX_BUFFER_NX_OFFSET, VERTEX_BUFFER_V_OFFSET + 1);
            offsetObject.vertices.put(vertexData[VERTEX_BUFFER_X_OFFSET] + vertexData[VERTEX_BUFFER_NX_OFFSET] * offset);
            offsetObject.vertices.put(vertexData[VERTEX_BUFFER_Y_OFFSET] + vertexData[VERTEX_BUFFER_NY_OFFSET] * offset);
            offsetObject.vertices.put(vertexData[VERTEX_BUFFER_Z_OFFSET] + vertexData[VERTEX_BUFFER_NZ_OFFSET] * offset);
        }
        vertices.rewind();
        offsetObject.vertices.flip();
        offsetObject.setTextureBounds(0, 1, 0, 1);
        return offsetObject;
    }

    /**
     * Returns a copy of these vertices in inverted order to create a back-face for this model.
     */
    public RenderableVertices createBackface() {
        RenderableVertices backfaceObject = new RenderableVertices(this.name + "_BACKFACE", FloatBuffer.allocate(vertices.capacity()), cacheVertices, isErrorPlaceholder);
        float[] vertexData = new float[FLOATS_PER_VERTEX];
        for (int backfaceVertexIndex = vertices.capacity() - FLOATS_PER_VERTEX; backfaceVertexIndex >= 0; backfaceVertexIndex -= FLOATS_PER_VERTEX) {
            vertices.get(vertexData);
            backfaceObject.vertices.position(backfaceVertexIndex);
            backfaceObject.vertices.put(vertexData);
        }
        vertices.rewind();
        backfaceObject.vertices.position(0);
        backfaceObject.vertices.limit(vertices.limit());
        return backfaceObject;
    }

    /** Shares the expanded mesh among renderables using the same model object and outline scale. */
    RenderableVertices getOutline(float scale) {
        if (outlines == null) {
            outlines = new HashMap<>();
        }
        return outlines.computeIfAbsent(scale, this::createOutline);
    }

    private RenderableVertices createOutline(float scale) {
        OutlineVertex[] corners = new OutlineVertex[vertices.limit() / FLOATS_PER_VERTEX];
        Map<OutlineVertex, OutlineVertex> weldedVertices = new HashMap<>();
        for (int i = 0; i < corners.length; ++i) {
            int position = i * FLOATS_PER_VERTEX + VERTEX_BUFFER_X_OFFSET;
            OutlineVertex vertex = new OutlineVertex(vertices.get(position), vertices.get(position + 1), vertices.get(position + 2), weldedVertices.size());
            corners[i] = weldedVertices.computeIfAbsent(vertex, key -> key);
        }

        Map<Long, OutlineEdge> edges = new HashMap<>();
        Point3D planeNormal = null;
        double planeDistance = 0;
        boolean flat = true;
        for (int i = 0; i < corners.length; i += VERTEXES_PER_FACE) {
            OutlineVertex a = corners[i];
            OutlineVertex b = corners[i + 1];
            OutlineVertex c = corners[i + 2];
            Point3D ab = b.position.copy().subtract(a.position);
            Point3D ac = c.position.copy().subtract(a.position);
            Point3D bc = c.position.copy().subtract(b.position);
            Point3D normal = ab.crossProduct(ac);
            double length = normal.length();
            if (length == 0) {
                continue;
            }
            normal.scale(1 / length);
            if (planeNormal == null) {
                planeNormal = normal;
                planeDistance = a.position.dotProduct(normal, false);
            } else if (flat && (Math.abs(planeNormal.dotProduct(normal, true)) < 1 - 1E-8
                    || Math.abs(a.position.dotProduct(planeNormal, false) - planeDistance) > 1E-6)) {
                flat = false;
            }
            //Weight by corner angle so triangulating a flat face does not skew the expansion.
            a.expansion.addScaled(normal, Math.atan2(length, ab.dotProduct(ac, false)));
            b.expansion.addScaled(normal, Math.atan2(length, -ab.dotProduct(bc, false)));
            c.expansion.addScaled(normal, Math.atan2(length, ac.dotProduct(bc, false)));
            recordOutlineEdge(edges, a, b, normal);
            recordOutlineEdge(edges, b, c, normal);
            recordOutlineEdge(edges, c, a, normal);
        }

        int boundaryCount = 0;
        for (OutlineEdge edge : edges.values()) {
            if (!edge.shared) {
                //Expand open edges sideways as well so flat meshes have a border when seen head-on.
                Point3D outward = edge.end.position.copy().subtract(edge.start.position).crossProduct(edge.normal).normalize();
                edge.start.expansion.add(outward);
                edge.end.expansion.add(outward);
                ++boundaryCount;
            }
        }
        for (OutlineVertex vertex : weldedVertices.keySet()) {
            if (flat && planeNormal != null) {
                vertex.expansion.addScaled(planeNormal, -vertex.expansion.dotProduct(planeNormal, false));
            }
            vertex.expansion.normalize().scale(scale).add(vertex.position);
        }

        int vertexCount = flat ? 2 * boundaryCount * VERTEXES_PER_QUAD : corners.length + boundaryCount * VERTEXES_PER_QUAD;
        FloatBuffer shell = FloatBuffer.allocate(vertexCount * FLOATS_PER_VERTEX);
        //Reverse the winding.  With culling and depth testing the original surface hides the interior.
        if (!flat) {
            for (int i = 0; i < corners.length; i += VERTEXES_PER_FACE) {
                putOutlineTriangle(shell, corners[i].expansion, corners[i + 2].expansion, corners[i + 1].expansion);
            }
        }
        for (OutlineEdge edge : edges.values()) {
            if (!edge.shared) {
                putOutlineTriangle(shell, edge.end.position, edge.start.position, edge.start.expansion);
                putOutlineTriangle(shell, edge.end.position, edge.start.expansion, edge.end.expansion);
                if (flat) {
                    //A flat panel only needs the border, visible from either side.
                    putOutlineTriangle(shell, edge.end.position, edge.start.expansion, edge.start.position);
                    putOutlineTriangle(shell, edge.end.position, edge.end.expansion, edge.start.expansion);
                }
            }
        }
        shell.flip();
        return new RenderableVertices(name + "_OUTLINE", shell, cacheVertices, false, true);
    }

    private static void putOutlineTriangle(FloatBuffer buffer, Point3D a, Point3D b, Point3D c) {
        putOutlineVertex(buffer, a);
        putOutlineVertex(buffer, b);
        putOutlineVertex(buffer, c);
    }

    private static void recordOutlineEdge(Map<Long, OutlineEdge> edges, OutlineVertex start, OutlineVertex end, Point3D normal) {
        long key = ((long) Math.min(start.index, end.index) << 32) | Math.max(start.index, end.index);
        OutlineEdge edge = edges.get(key);
        if (edge == null) {
            edges.put(key, new OutlineEdge(start, end, normal));
        } else if (edge.start == start ? edge.normal.dotProduct(normal, true) < 1 - 1E-8
                : edge.normal.dotProduct(normal, true) > -1 + 1E-8) {
            //A duplicate coplanar face, including a deliberately reversed face, adds no adjacency.
            edge.shared = true;
        }
    }

    private static void putOutlineVertex(FloatBuffer buffer, Point3D point) {
        //The outline uses a white texture and unlit shading.
        buffer.put(0).put(0).put(1).put(0.5F).put(0.5F);
        buffer.put((float) point.x).put((float) point.y).put((float) point.z);
    }

    private static final class OutlineVertex {
        final Point3D position;
        final Point3D expansion = new Point3D();
        final int index;

        OutlineVertex(float x, float y, float z, int index) {
            //OBJ may encode the same coordinate as either 0 or -0.
            this.position = new Point3D(x + 0.0F, y + 0.0F, z + 0.0F);
            this.index = index;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof OutlineVertex && position.equals(((OutlineVertex) other).position);
        }

        @Override
        public int hashCode() {
            int hash = Float.hashCode((float) position.x);
            hash = 31 * hash + Float.hashCode((float) position.y);
            return 31 * hash + Float.hashCode((float) position.z);
        }
    }

    private static final class OutlineEdge {
        final OutlineVertex start;
        final OutlineVertex end;
        final Point3D normal;
        boolean shared;

        OutlineEdge(OutlineVertex start, OutlineVertex end, Point3D normal) {
            this.start = start;
            this.end = end;
            this.normal = normal;
        }
    }

    /**
     * Adds a line to the {@link #vertices} of this object using Point3D objects.
     * If the last line is added, this function will automatically handle the batch ending.
     */
    public void addLine(Point3D start, Point3D end) {
        addLine((float) start.x, (float) start.y, (float) start.z, (float) end.x, (float) end.y, (float) end.z);
    }

    /**
     * Adds a line to the {@link #vertices} of this object.
     * If the last line is added, this function will automatically handle the batch ending.
     */
    public void addLine(float startX, float startY, float startZ, float endX, float endY, float endZ) {
        vertices.put(startX);
        vertices.put(startY);
        vertices.put(startZ);
        vertices.put(endX);
        vertices.put(endY);
        vertices.put(endZ);
        if (!vertices.hasRemaining()) {
            vertices.flip();
        }
    }

    /**
     * Sets the vertex data to the {@link BoundingBox} bounds.
     * The vertex data will be centered at 0,0,0.
     */
    public void setBoundingBox(BoundingBox box, boolean wireframe) {
        if (wireframe) {
            for (List<BoxOffset> indexes : WIREFRAME_POINT_INDEXES) {
                vertices.put(indexes.get(0).getEdgePoint(box));
                vertices.put(indexes.get(1).getEdgePoint(box));
            }
        } else {
            for (int boxSide = 0; boxSide < SIDES_PER_BOX; ++boxSide) {
                for (int boxVertex = 0; boxVertex < VERTEXES_PER_QUAD; ++boxVertex) {
                    //Add normals and fake UVs.  UVs get normalized later.
                    vertices.put(FACE_NORMALS[boxSide]);
                    vertices.put(0.0F);
                    vertices.put(0.0F);
                    vertices.put(FACE_POINT_INDEXES.get(boxSide).get(boxVertex).getEdgePoint(box));
                }
            }

            //Normalize UVs to align with texture.
            setTextureBounds(0.0F, 1.0F, 0.0F, 1.0F);
        }

        //Flip for rendering.
        vertices.flip();
    }

    /**
     * Sets the texture bounds for this set of vertices to the passed-in parameters.
     * Sets for every quad in the texture for objects created via {@link #createSprite(int, List, List)}
     */
    public void setTextureBounds(float u, float U, float v, float V) {
        int verticesInObject = vertices.capacity() / FLOATS_PER_VERTEX;
        for (int vertexIndex = 0; vertexIndex < verticesInObject; ++vertexIndex) {
            switch (vertexIndex % VERTEXES_PER_QUAD) {
                case (QUAD_TRI1_BOTTOM_RIGHT_INDEX):
                case (QUAD_TRI2_BOTTOM_RIGHT_INDEX): {
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_U_OFFSET, U);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_V_OFFSET, V);
                    break;
                }
                case (QUAD_TRI1_TOP_RIGHT_INDEX): {
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_U_OFFSET, U);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_V_OFFSET, v);
                    break;
                }
                case (QUAD_TRI1_TOP_LEFT_INDEX):
                case (QUAD_TRI2_TOP_LEFT_INDEX): {
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_U_OFFSET, u);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_V_OFFSET, v);
                    break;
                }
                case (QUAD_TRI2_BOTTOM_LEFT_INDEX): {
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_U_OFFSET, u);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_V_OFFSET, V);
                    break;
                }
            }
        }
    }

    /**
     * Set the vertices to the position and UV coordinates requested.  Designed only for sprites constructed via
     * {@link #createSprite(int, List, List)}, and requires the sprite index to be specified.
     */
    public void setSpriteProperties(int spriteIndex, int offsetX, int offsetY, int width, int height, float u, float v, float U, float V) {
        setSpritePropertiesAdvancedTexture(spriteIndex, offsetX, offsetY, width, height, u, v, u, V, U, V, U, v);
    }

    /**
     * Like {@link #setSpriteProperties(int, int, int, int, int, float, float, float, float)}, but with each texture coordinate specified.
     */
    public void setSpritePropertiesAdvancedTexture(int spriteIndex, int offsetX, int offsetY, int width, int height, float u1, float v1, float u2, float v2, float u3, float v3, float u4, float v4) {
        //Now populate the buffer.
        for (int vertexIndex = spriteIndex * VERTEXES_PER_QUAD; vertexIndex < (spriteIndex + 1) * VERTEXES_PER_QUAD; ++vertexIndex) {
            int quadVertexIndex = vertexIndex % VERTEXES_PER_QUAD;
            switch (quadVertexIndex) {
                case (QUAD_TRI1_BOTTOM_RIGHT_INDEX):
                case (QUAD_TRI2_BOTTOM_RIGHT_INDEX): {
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_U_OFFSET, u3);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_V_OFFSET, v3);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_X_OFFSET, offsetX + width);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_Y_OFFSET, offsetY - height);
                    break;
                }
                case (QUAD_TRI1_TOP_RIGHT_INDEX): {
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_U_OFFSET, u4);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_V_OFFSET, v4);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_X_OFFSET, offsetX + width);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_Y_OFFSET, offsetY);
                    break;
                }
                case (QUAD_TRI1_TOP_LEFT_INDEX):
                case (QUAD_TRI2_TOP_LEFT_INDEX): {
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_U_OFFSET, u1);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_V_OFFSET, v1);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_X_OFFSET, offsetX);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_Y_OFFSET, offsetY);
                    break;
                }
                case (QUAD_TRI2_BOTTOM_LEFT_INDEX): {
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_U_OFFSET, u2);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_V_OFFSET, v2);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_X_OFFSET, offsetX);
                    vertices.put(vertexIndex * FLOATS_PER_VERTEX + VERTEX_BUFFER_Y_OFFSET, offsetY - height);
                    break;
                }
            }
        }
    }

    /**
     * Enum that defines what points exist for a bounding box.
     */
    private static enum BoxOffset {
        MINX_MINY_MINZ(false, false, false),
        MINX_MINY_MAXZ(false, false, true),
        MINX_MAXY_MINZ(false, true, false),
        MINX_MAXY_MAXZ(false, true, true),
        MAXX_MINY_MINZ(true, false, false),
        MAXX_MINY_MAXZ(true, false, true),
        MAXX_MAXY_MINZ(true, true, false),
        MAXX_MAXY_MAXZ(true, true, true);

        private final boolean positiveX;
        private final boolean positiveY;
        private final boolean positiveZ;

        private BoxOffset(boolean positiveX, boolean positiveY, boolean positiveZ) {
            this.positiveX = positiveX;
            this.positiveY = positiveY;
            this.positiveZ = positiveZ;
        }

        private float[] getEdgePoint(BoundingBox box) {
            return new float[] { (float) (positiveX ? +box.widthRadius : -box.widthRadius), (float) (positiveY ? +box.heightRadius : -box.heightRadius), (float) (positiveZ ? +box.depthRadius : -box.depthRadius) };
        }
    }
}
