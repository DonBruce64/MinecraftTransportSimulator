package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightBody;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightRenderer;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightShaders;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightShadowAtlas;

/**
 * The GL calls the dynamic light renderer ({@link DynamicLightRenderer}, {@link DynamicLightShadowAtlas}) makes,
 * implemented once per version: mccore holds the passes, their order and their uniforms, the version holds raw GL
 * (LWJGL 2 or 3), MC's own state caches (texture binds, programs, blend and depth go through them so MC's next draw
 * finds GL as it thinks) and the bodies that cast shadows.  Matrices are 16 floats, column-major.  Textures and
 * programs are GL names; targets are the version's own framebuffer objects.  Render thread only.
 */
public interface IInterfaceDynamicLightingGL {
    /** {@link #drawFullscreen}: replaces what is under it. */
    int DRAW_REPLACE = 0;
    /** {@link #drawFullscreen}: {@code dst * (1 + src)}. */
    int DRAW_SURFACE = 1;
    /** {@link #drawFullscreen}: {@code dst + src}. */
    int DRAW_ADD = 2;
    /** {@link #drawFullscreen}: writes depth alone, always (copies a depth). */
    int DRAW_DEPTH = 3;
    /** {@link #drawFullscreen}: writes depth alone where nearer (traces blocks into a shadow tile). */
    int DRAW_DEPTH_NEARER = 4;

    /**
     * Whether this context is GL 2.1 with the extensions the passes need ({@code EXT_gpu_shader4},
     * {@code ARB_texture_float}, {@code ARB_texture_rg}, {@code ARB_framebuffer_object}) instead of GL 3.2: the
     * shared GLSL is then compiled as GLSL 1.20 ({@link DynamicLightShaders#getSource}), and the version draws without
     * vertex array objects.  macOS gives 1.12.2 and 1.16.5 such a context.
     */
    default boolean legacyContext() {
        return false;
    }

    /** Compiles and links a program, {@code Position} bound to attribute 0; 0 (logged) if a stage fails. */
    int createProgram(String vertexSource, String fragmentSource, String name);

    void useProgram(int program);

    int uniformLocation(int program, String name);

    void setUniform(int location, int value);

    void setUniform(int location, float value);

    void setUniform(int location, float x, float y);

    void setUniform(int location, float x, float y, float z);

    void setUniform(int location, float x, float y, float z, float w);

    void setUniformInts(int location, int x, int y, int z);

    /** A {@code float[]}, {@code vec2[]} or {@code vec4[]} array ({@code size} 1, 2 or 4) of {@code count} elements. */
    void setUniformArray(int location, float[] values, int size, int count);

    void setUniformMatrix(int location, float[] matrix);

    /**
     * A nearest-filtered, edge-clamped data texture: R8 bytes, or RGBA32F when {@code floats}.  Its contents start
     * zero (bytes) or undefined (floats).
     */
    int createDataTexture(int width, int height, boolean floats);

    /** The R8 region {@code x, y, width, height} of {@code texture} from {@code data}, whose rows are {@code stride} wide. */
    void uploadBytes(int texture, byte[] data, int stride, int x, int y, int width, int height);

    /** Rows {@code 0 .. rows-1} of the RGBA32F {@code texture}, {@code width} texels each, from {@code data}. */
    void uploadFloats(int texture, float[] data, int width, int rows);

    /** Binds {@code texture} (0: none) on unit {@code unit}, leaving unit 0 active. */
    void bindTexture(int unit, int texture);

    /**
     * A framebuffer of {@code width x height}: a colour texture (16-bit floats when {@code floatColor}, linear
     * filtered when {@code linear}) and, when {@code depth}, a depth texture.
     */
    Object createTarget(int width, int height, boolean depth, boolean floatColor, boolean linear);

    void deleteTarget(Object target);

    /** Draws into {@code target} (null: the main target), with a viewport over all of it when {@code viewport}. */
    void bindTarget(Object target, boolean viewport);

    /** Clears {@code target}'s colour to 0 and its depth to 1.  What is bound afterwards is undefined. */
    void clearTarget(Object target);

    int colorTexture(Object target);

    int depthTexture(Object target);

    int mainWidth();

    int mainHeight();

    /** A depth texture holding the main target's depth this frame (a version whose depth is a renderbuffer copies it). */
    int mainDepthTexture();

    void viewport(int x, int y, int width, int height);

    /** Clears the depth of the bound target's region alone, to 1. */
    void clearDepth(int x, int y, int width, int height);

    /** One triangle over the viewport from {@code gl_VertexID} with the program in use, as {@code mode} (DRAW_*). */
    void drawFullscreen(int mode);

    /**
     * Lists this frame's entities that may cast a shadow (any entity, living or not: mobs, minecarts, boats, item
     * frames, dropped items; not IV's own builder entities, which draw the whole IV scene, not an entity seated in an
     * IV seat, not an invisible one) into {@link DynamicLightShadowAtlas#addCandidate}.
     */
    void listLivingBodies(float partialTicks);

    /**
     * Draws candidate {@code body}'s entity with its own renderer into its corners
     * ({@link DynamicLightBody#addQuadCorner}, {@link DynamicLightBody#addTriangleCorner}), camera-relative, each with
     * its render type's texture and alpha cut: {@link DynamicLightBody#CUT_CUTOUT} for a solid (cutout) type,
     * {@link DynamicLightBody#CUT_BLENDED} for one that blends by alpha, and no corners for the other blended types
     * (additive eyes, glint, swirls: they hide nothing).  A renderer that fails outside its normal pass simply leaves
     * no corners.
     */
    void captureLiving(DynamicLightBody body, double cameraX, double cameraY, double cameraZ, float partialTicks);

    /** Depth-only state for {@link #drawBody}: depth test and write, no cull, no blend, no colour. */
    void beginBodies();

    /**
     * Draws {@code body} with the program in use: its captured vertex buffer with {@link DynamicLightBody#texture} bound
     * on unit 0 and {@link DynamicLightBody#cut} as its alpha cut, or its corners (positions and texture coordinates),
     * each run with its own texture on unit 0 and its own cut ({@link DynamicLightBody#runs},
     * {@link DynamicLightBody#runCut}).  The cut goes to the float uniform at {@code cutLocation} (the program's
     * {@code alphaCut}; -1: none) before each draw.  {@code lamp_body} discards what that texture cuts away: texels
     * with alpha under the cut; texture 0 casts whole.
     */
    void drawBody(DynamicLightBody body, int cutLocation);

    void endBodies();

    /** Leaves GL as MC expects it: no program, vertex array or texture of ours bound, default state, main target bound. */
    void restore();
}
