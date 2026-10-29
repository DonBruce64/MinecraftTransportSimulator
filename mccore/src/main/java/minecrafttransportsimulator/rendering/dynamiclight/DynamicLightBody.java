package minecrafttransportsimulator.rendering.dynamiclight;

import java.nio.FloatBuffer;
import java.util.Arrays;

import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;

/**
 * One body that casts dynamic light shadows this frame: an IV vertex buffer the solid pass drew (its version's own
 * buffer object in {@link #buffer}, with the camera-relative model matrix it was drawn with); IV vertices the solid
 * pass drew without a buffer object (the Slow rendering mode), copied into {@link #trianglePositions} in their own
 * space, with that matrix too; or a living entity ({@link #entity}) whose renderer drew its faces into
 * {@link #addQuadCorner} and {@link #addTriangleCorner}, camera-relative.  And the sphere round it in world space.
 * Each face keeps the texture it was drawn with ({@link #texture}, or its corners' runs) and the alpha below which
 * that texture is cut away ({@link #cut}, or its run's), so it shadows only where the texture shows: a cutout draw
 * discards the texels the shadow would otherwise keep.  Pooled by {@link DynamicLightShadowAtlas}.
 */
public final class DynamicLightBody {
    /** The alpha cut of MC's entity cutout shaders: a texel under it is not drawn, so it casts nothing. */
    public static final float CUT_CUTOUT = 0.1F;
    /**
     * The alpha cut of a render type that blends by alpha (player skins, allays, dropped items): texels fully opaque
     * or fully clear in those textures cast exactly, while a half clear one (a slime's outer jelly, alpha 0.7) casts
     * nothing.
     */
    public static final float CUT_BLENDED = 0.75F;


    public double centreX;
    public double centreY;
    public double centreZ;
    public float radius;
    /** From the body's vertices to camera-relative world. */
    public final float[] model = new float[16];
    /** A captured IV body's vertex buffer (the version's own type), or null for a living body. */
    public Object buffer;
    /** The GL texture a captured buffer was drawn with, 0 for none (every face shadows). */
    public int texture;
    /** The alpha a captured buffer's texture is cut at ({@link #CUT_CUTOUT}). */
    public float cut = CUT_CUTOUT;
    /** A living body's entity (the version's own type), or null for a captured body. */
    public Object entity;
    /**
     * A living body's corners, camera-relative, or copied IV vertices' corners in their own space: quads 4 corners
     * each, triangles 3 each, 3 floats a corner.
     */
    public float[] quadPositions = new float[3 * 4096];
    public int quadCount;
    public float[] trianglePositions = new float[3 * 4096];
    public int triangleCount;
    /** The corners' texture coordinates, 2 floats a corner, as the positions. */
    public float[] quadUVs = new float[2 * 4096];
    public float[] triangleUVs = new float[2 * 4096];
    /**
     * Whether a body's corners are on the GPU this frame; {@link IInterfaceDynamicLightingGL#drawBody} sets it and keeps its own
     * GL objects in {@link #gpu}, which outlives the frame with the pooled body.
     */
    public boolean uploaded;
    public Object gpu;

    float bufferRadius;
    final float[] pose = new float[16];
    private float[] shape = new float[12];
    private boolean shaped;
    /**
     * Runs of corners drawn with one texture and alpha cut: its GL id, its first corner and its cut's float bits, a
     * triple a run.
     */
    private int[] quadRuns = new int[24];
    private int quadRunCount;
    private int[] triangleRuns = new int[24];
    private int triangleRunCount;

    void beginCaptured(Object buffer, float[] pose, float radius, int texture) {
        this.buffer = buffer;
        this.texture = texture;
        this.cut = CUT_CUTOUT;
        this.entity = null;
        System.arraycopy(pose, 0, this.pose, 0, 16);
        this.bufferRadius = radius;
        shaped = false;
    }

    /**
     * IV vertices ({@code NX, NY, NZ, U, V, X, Y, Z} a vertex, triangles) drawn with model-view {@code pose} and GL
     * {@code texture}: their positions and texture coordinates become this body's triangle corners.
     */
    void beginVertices(FloatBuffer vertices, float[] pose, int texture) {
        buffer = null;
        entity = null;
        clearLiving();
        uploaded = false;
        shaped = false;
        System.arraycopy(pose, 0, this.pose, 0, 16);
        float most = 0;
        for (int v = 0, end = vertices.limit() / 8 * 8; v < end; v += 8) {
            float x = vertices.get(v + 5);
            float y = vertices.get(v + 6);
            float z = vertices.get(v + 7);
            addTriangleCorner(x, y, z, vertices.get(v + 3), vertices.get(v + 4), texture);
            most = Math.max(most, x * x + y * y + z * z);
        }
        bufferRadius = (float) Math.sqrt(most);
    }

    /** Puts a captured body into camera-relative world once the frame's view is known. */
    void place(float[] toCamera, DynamicLightFrame frame) {
        DynamicLightMatrix.mul(toCamera, pose, model);
        radius = bufferRadius * Math.max(DynamicLightMatrix.maxScale(model), 1e-3F);
        centreX = model[12] + frame.cameraX;
        centreY = model[13] + frame.cameraY;
        centreZ = model[14] + frame.cameraZ;
    }

    void beginLiving(Object entity, double x, double y, double z, float r) {
        buffer = null;
        this.entity = entity;
        clearLiving();
        uploaded = false;
        shaped = false;
        centreX = x;
        centreY = y;
        centreZ = z;
        radius = r;
        DynamicLightMatrix.identity(model);
    }

    /** Drops a living body's corners: its renderer failed part way. */
    public void clearLiving() {
        quadCount = 0;
        triangleCount = 0;
        quadRunCount = 0;
        triangleRunCount = 0;
    }

    /** A corner of a face drawn without a texture: it shadows whole. */
    public void addQuadCorner(float x, float y, float z) {
        addQuadCorner(x, y, z, 0, 0, 0);
    }

    /** A quad's corner at texture coordinates {@code u, v} of GL {@code texture} (0 for none), cut at {@link #CUT_CUTOUT}. */
    public void addQuadCorner(float x, float y, float z, float u, float v, int texture) {
        addQuadCorner(x, y, z, u, v, texture, CUT_CUTOUT);
    }

    /**
     * A quad's corner at texture coordinates {@code u, v} of GL {@code texture} (0 for none), whose texels under alpha
     * {@code cut} cast nothing.  A change of texture or cut starts a new run.
     */
    public void addQuadCorner(float x, float y, float z, float u, float v, int texture, float cut) {
        int bits = Float.floatToRawIntBits(cut);
        if (quadRunCount == 0 || quadRuns[3 * quadRunCount - 3] != texture || quadRuns[3 * quadRunCount - 1] != bits) {
            quadRuns = addRun(quadRuns, quadRunCount++, texture, quadCount, bits);
        }
        quadUVs = addUV(quadUVs, quadCount, u, v);
        quadPositions = add(quadPositions, quadCount++, x, y, z);
    }

    /** A corner of a face drawn without a texture: it shadows whole. */
    public void addTriangleCorner(float x, float y, float z) {
        addTriangleCorner(x, y, z, 0, 0, 0);
    }

    /** A triangle's corner at texture coordinates {@code u, v} of GL {@code texture} (0 for none), cut at {@link #CUT_CUTOUT}. */
    public void addTriangleCorner(float x, float y, float z, float u, float v, int texture) {
        addTriangleCorner(x, y, z, u, v, texture, CUT_CUTOUT);
    }

    /** {@link #addQuadCorner(float, float, float, float, float, int, float)} of a triangle's corner. */
    public void addTriangleCorner(float x, float y, float z, float u, float v, int texture, float cut) {
        int bits = Float.floatToRawIntBits(cut);
        if (triangleRunCount == 0 || triangleRuns[3 * triangleRunCount - 3] != texture || triangleRuns[3 * triangleRunCount - 1] != bits) {
            triangleRuns = addRun(triangleRuns, triangleRunCount++, texture, triangleCount, bits);
        }
        triangleUVs = addUV(triangleUVs, triangleCount, u, v);
        trianglePositions = add(trianglePositions, triangleCount++, x, y, z);
    }

    /** How many runs of one texture and cut the quads ({@code quads}) or the triangles hold. */
    public int runs(boolean quads) {
        return quads ? quadRunCount : triangleRunCount;
    }

    /** Run {@code run}'s GL texture, 0 for none. */
    public int runTexture(boolean quads, int run) {
        return (quads ? quadRuns : triangleRuns)[3 * run];
    }

    /** Run {@code run}'s first corner. */
    public int runStart(boolean quads, int run) {
        return (quads ? quadRuns : triangleRuns)[3 * run + 1];
    }

    /** The alpha below which run {@code run}'s texture casts nothing. */
    public float runCut(boolean quads, int run) {
        return Float.intBitsToFloat((quads ? quadRuns : triangleRuns)[3 * run + 2]);
    }

    /** The corner after run {@code run}'s last whole face. */
    public int runEnd(boolean quads, int run) {
        int whole = quads ? quadCorners() : triangleCorners();
        return run + 1 < runs(quads) ? Math.min(runStart(quads, run + 1), whole) : whole;
    }

    private static int[] addRun(int[] runs, int run, int texture, int corner, int cutBits) {
        if (3 * run + 3 > runs.length) {
            runs = Arrays.copyOf(runs, runs.length * 2);
        }
        runs[3 * run] = texture;
        runs[3 * run + 1] = corner;
        runs[3 * run + 2] = cutBits;
        return runs;
    }

    private static float[] addUV(float[] uvs, int corner, float u, float v) {
        if (2 * corner + 2 > uvs.length) {
            uvs = Arrays.copyOf(uvs, uvs.length * 2);
        }
        uvs[2 * corner] = u;
        uvs[2 * corner + 1] = v;
        return uvs;
    }

    private static float[] add(float[] positions, int corner, float x, float y, float z) {
        int at = corner * 3;
        if (at + 3 > positions.length) {
            positions = Arrays.copyOf(positions, positions.length * 2);
        }
        positions[at] = x;
        positions[at + 1] = y;
        positions[at + 2] = z;
        return positions;
    }

    /** The whole quads' corners. */
    public int quadCorners() {
        return quadCount / 4 * 4;
    }

    /** The whole triangles' corners. */
    public int triangleCorners() {
        return triangleCount / 3 * 3;
    }

    boolean drawn() {
        return buffer != null || quadCount >= 4 || triangleCount >= 3;
    }

    /**
     * What its shadow is: a captured body's world model matrix and texture (and corner count, for copied vertices); a
     * living body's corner count, mean and spread.
     */
    float[] shape(DynamicLightFrame frame) {
        if (shaped) {
            return shape;
        }
        shaped = true;
        if (entity == null) {
            if (shape.length != 14) {
                shape = new float[14];
            }
            shape[12] = triangleCount;
            shape[13] = buffer != null ? texture : runs(false) > 0 ? runTexture(false, 0) : 0;
            shape[0] = (float) (model[12] + frame.cameraX);
            shape[1] = (float) (model[13] + frame.cameraY);
            shape[2] = (float) (model[14] + frame.cameraZ);
            shape[3] = model[0];
            shape[4] = model[1];
            shape[5] = model[2];
            shape[6] = model[4];
            shape[7] = model[5];
            shape[8] = model[6];
            shape[9] = model[8];
            shape[10] = model[9];
            shape[11] = model[10];
            return shape;
        }
        if (shape.length != 10) {
            shape = new float[10];
        }
        int q = quadCorners();
        int t = triangleCorners();
        double sx = 0, sy = 0, sz = 0, xx = 0, yy = 0, zz = 0, xy = 0, xz = 0, yz = 0;
        for (int k = 0; k < q + t; k++) {
            float[] from = k < q ? quadPositions : trianglePositions;
            int o = (k < q ? k : k - q) * 3;
            double px = from[o], py = from[o + 1], pz = from[o + 2];
            sx += px;
            sy += py;
            sz += pz;
            xx += px * px;
            yy += py * py;
            zz += pz * pz;
            xy += px * py;
            xz += px * pz;
            yz += py * pz;
        }
        double n = Math.max(1, q + t);
        double mx = sx / n, my = sy / n, mz = sz / n;
        shape[0] = q + t;
        shape[1] = (float) (mx + frame.cameraX);
        shape[2] = (float) (my + frame.cameraY);
        shape[3] = (float) (mz + frame.cameraZ);
        shape[4] = (float) (xx / n - mx * mx);
        shape[5] = (float) (yy / n - my * my);
        shape[6] = (float) (zz / n - mz * mz);
        shape[7] = (float) (xy / n - mx * my);
        shape[8] = (float) (xz / n - mx * mz);
        shape[9] = (float) (yz / n - my * mz);
        return shape;
    }
}
