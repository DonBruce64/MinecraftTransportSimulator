package minecrafttransportsimulator.rendering.dynamiclight;

import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Draws one frame's dynamic lights onto the main target: copies the depth, traces
 * the lamps' shadow tiles ({@link DynamicLightShadowAtlas}), hands the lights to the light pass as a float texture
 * (a row each, up to {@link DynamicLightFrame#CAPACITY}), works the light out per pixel into a float target and
 * lays it over the picture twice ({@code dst * (1 + 4 light) + 0.45 light}), then adds the beams (ray-marched at half
 * size) and the coronas.  Every GL call goes through the version's {@link IInterfaceDynamicLightingGL}, which leaves GL as MC
 * expects it when done.  Called by {@link DynamicLightSystem#renderFrame} after IV's solid pass and before its
 * blended pass.  Render thread only.
 */
public final class DynamicLightRenderer {
    /** The light over the picture: {@code dst * (1 + SURFACE_GAIN * light) + LIGHT_FLAT * light}. */
    private static final float SURFACE_GAIN = 4.0F;
    private static final float LIGHT_FLAT = 0.45F;
    /** A corona's size at full strength, least and most, shares of the screen height. */
    private static final float CORONA_SIZE = 0.12F;
    private static final float CORONA_SIZE_MIN = 0.03F;
    private static final float CORONA_SIZE_MAX = 1.0F;
    /** Most grid sections a frame uploads one by one; more (a recentre) upload the whole grid at once. */
    private static final int VOXEL_SECTION_UPLOADS = 32;
    /** Distance where a lamp's corona halves, blocks. */
    private static final float CORONA_FALLOFF = 16F;
    /** The corona's share left in full daylight. */
    private static final float CORONA_DAY = 0.35F;
    /** The beams' air is worked out at 1/this of the screen: soft by nature, a quarter of the work. */
    private static final int BEAM_DIVISOR = 2;

    private static IInterfaceDynamicLightingGL gl;
    private static boolean failed;
    private static DynamicLightProgram light;
    private static DynamicLightProgram composite;
    private static DynamicLightProgram beam;
    private static DynamicLightProgram corona;
    private static DynamicLightProgram trace;
    private static DynamicLightProgram copy;
    private static DynamicLightProgram body;

    private static int voxelTexture;
    /** The block grid's shapes ({@link DynamicLightBlockShapes}), and the table version last uploaded. */
    private static int shapeTexture;
    private static int uploadedShapes = Integer.MIN_VALUE;
    /** The frame's lights for the light pass: {@link DynamicLightFrame#TEXELS} RGBA32F texels a row, a row a light. */
    private static int lightTexture;
    /** The frame's beams and coronas for their shaders, the same way: a row each, as many as the frame has lights. */
    private static int beamTexture;
    private static int coronaTexture;
    private static int uploadedVersion = Integer.MIN_VALUE;

    private static final Target depthCopy = new Target(1, true, false);
    private static final Target lightTarget = new Target(1, false, true);
    private static final Target beamTarget = new Target(BEAM_DIVISOR, false, true);

    /** This frame's camera rotation and projection, and frustum. */
    private static final float[] view = DynamicLightMatrix.identity(new float[16]);
    private static final float[] projection = DynamicLightMatrix.identity(new float[16]);
    private static final float[] viewProj = new float[16];
    private static final float[] invViewProj = new float[16];
    private static final float[] point = new float[4];

    private static final float[] beamData = new float[DynamicLightBeams.CAPACITY * DynamicLightBeams.TEXELS * 4];
    private static final float[] coronaData = new float[DynamicLightCoronas.CAPACITY * DynamicLightCoronas.TEXELS * 4];
    private static DynamicLightFrustum frustum = DynamicLightFrustum.ACCEPT_ALL;

    private DynamicLightRenderer() {
    }

    /** Whether the passes' shaders did not build: {@link DynamicLightSystem} then lights the world in its fallback mode. */
    static boolean hasFailed() {
        return failed;
    }

    /** This frame's view rotation (the world render's pose, column-major), projection and camera frustum. */
    static void beginFrame(float[] viewRotation, float[] projectionMatrix, DynamicLightFrustum cameraFrustum) {
        System.arraycopy(viewRotation, 0, view, 0, 16);
        System.arraycopy(projectionMatrix, 0, projection, 0, 16);
        frustum = cameraFrustum;
    }

    /** Draws {@code frame} with {@code glCalls}; anything that fails to build leaves the world as it was. */
    static void render(DynamicLightFrame frame, IInterfaceDynamicLightingGL glCalls) {
        if (gl != glCalls) {
            gl = glCalls;
            initialize();
        }
        try {
            if (failed) {
                return;
            }
            DynamicLightShadowAtlas.skip();
            uploadVoxels(frame);
            draw(frame);
        } finally {
            DynamicLightShadowAtlas.frameEnd();
            gl.restore();
        }
    }

    private static void initialize() {
        light = DynamicLightProgram.create(gl, DynamicLightShaders.FULLSCREEN, DynamicLightShaders.LIGHT_PASS);
        composite = DynamicLightProgram.create(gl, DynamicLightShaders.FULLSCREEN, DynamicLightShaders.COMPOSITE);
        beam = DynamicLightProgram.create(gl, DynamicLightShaders.FULLSCREEN, DynamicLightShaders.BEAM);
        corona = DynamicLightProgram.create(gl, DynamicLightShaders.FULLSCREEN, DynamicLightShaders.CORONA);
        trace = DynamicLightProgram.create(gl, DynamicLightShaders.FULLSCREEN, DynamicLightShaders.TRACE);
        copy = DynamicLightProgram.create(gl, DynamicLightShaders.FULLSCREEN, DynamicLightShaders.COPY);
        body = DynamicLightProgram.create(gl, DynamicLightShaders.BODY_VERTEX, DynamicLightShaders.BODY_FRAGMENT);
        if (light == null || composite == null || copy == null) {
            failed = true;
            InterfaceManager.coreInterface.logError("Dynamic lights are off: their light pass shaders did not build.");
            return;
        }
        for (DynamicLightProgram program : new DynamicLightProgram[] { light, composite, beam, corona, trace, copy, body }) {
            if (program != null) {
                program.use();
                program.set("Sampler0", 0);
                program.set("Sampler1", 1);
                program.set("Sampler2", 2);
                program.set("Sampler3", 3);
                program.set("Sampler4", 4);
            }
        }
        voxelTexture = gl.createDataTexture(DynamicLightVoxelWindow.ATLAS, DynamicLightVoxelWindow.ATLAS, false);
        shapeTexture = gl.createDataTexture(DynamicLightBlockShapes.TEXELS, DynamicLightBlockShapes.ROWS, true);
        //Uniform arrays of 256 lights would pass the uniform space GL promises; a small float texture has no such limit.
        lightTexture = gl.createDataTexture(DynamicLightFrame.TEXELS, DynamicLightFrame.CAPACITY, true);
        beamTexture = gl.createDataTexture(DynamicLightBeams.TEXELS, DynamicLightBeams.CAPACITY, true);
        coronaTexture = gl.createDataTexture(DynamicLightCoronas.TEXELS, DynamicLightCoronas.CAPACITY, true);
    }

    /**
     * The grid sections that changed this frame (read, or emptied as they entered the window) to the texture; the whole
     * grid when frames passed without an upload (a shader pack frame reads sections too) or many changed at once, so
     * the texture never keeps a stale section.
     */
    private static void uploadVoxels(DynamicLightFrame frame) {
        DynamicLightVoxelWindow window = frame.voxels;
        if (window == null) {
            return;
        }
        //The shapes first: a byte uploaded below may name a shape added this frame.
        DynamicLightBlockShapes shapes = window.shapes();
        if (shapes.version() != uploadedShapes) {
            gl.uploadFloats(shapeTexture, shapes.data(), DynamicLightBlockShapes.TEXELS, DynamicLightBlockShapes.ROWS);
            uploadedShapes = shapes.version();
        }
        boolean whole = uploadedVersion == Integer.MIN_VALUE || frame.voxelVersion - uploadedVersion > 1 || (frame.voxelVersion != uploadedVersion && frame.voxelSectionCount == 0) || frame.voxelSectionCount > VOXEL_SECTION_UPLOADS;
        if (!whole && frame.voxelSectionCount == 0) {
            return;
        }
        byte[] atlas = window.atlas();
        int size = DynamicLightVoxelWindow.ATLAS;
        if (whole) {
            gl.uploadBytes(voxelTexture, atlas, size, 0, 0, size, size);
        } else {
            for (int i = 0; i < frame.voxelSectionCount; i++) {
                int x0 = frame.voxelSections[i * 3] << 4;
                int sy = frame.voxelSections[i * 3 + 1];
                int z0 = frame.voxelSections[i * 3 + 2] << 4;
                for (int y = sy << 4, ye = y + 16; y < ye; y++) {
                    gl.uploadBytes(voxelTexture, atlas, size, DynamicLightVoxelWindow.texelX(x0, y), DynamicLightVoxelWindow.texelY(y, z0), 16, 16);
                }
            }
        }
        uploadedVersion = frame.voxelVersion;
    }

    private static void draw(DynamicLightFrame frame) {
        int lightCount = frame.lights.size();
        boolean lit = lightCount > 0;
        boolean coronas = frame.coronas.count() > 0 && corona != null;
        boolean beams = frame.beams.count() > 0 && beam != null;
        if (!lit && !coronas && !beams) {
            return;
        }
        int width = gl.mainWidth();
        int height = gl.mainHeight();
        int bx = (int) Math.floor(frame.cameraX);
        int by = (int) Math.floor(frame.cameraY);
        int bz = (int) Math.floor(frame.cameraZ);
        float fx = (float) (frame.cameraX - bx);
        float fy = (float) (frame.cameraY - by);
        float fz = (float) (frame.cameraZ - bz);
        DynamicLightVoxelWindow window = frame.voxels;
        //The block shapes stay on their unit for the trace and the light pass (voxel.glsl).
        gl.bindTexture(4, shapeTexture);

        if (frame.shadows && (lit || beams)) {
            if (trace != null) {
                trace.use();
                setGrid(trace, window, bx, by, bz, fx, fy, fz);
            }
            DynamicLightShadowAtlas.render(gl, frame, view, frustum, trace, copy, body, voxelTexture);
        }

        depthCopy.resize(width, height);
        copyDepth(gl.mainDepthTexture(), depthCopy.handle);
        int depth = gl.depthTexture(depthCopy.handle);
        gl.bindTarget(null, true);

        DynamicLightMatrix.mul(projection, view, viewProj);
        DynamicLightMatrix.invert(viewProj, invViewProj);
        if (lit) {
            light.use();
            light.setMatrix("invViewProj", invViewProj);
            light.set("lightCount", lightCount);
            light.set("shadowsEnabled", frame.shadows ? 1 : 0);
            uploadLights(frame, lightCount);
            setGrid(light, window, bx, by, bz, fx, fy, fz);
            lightTarget.resize(width, height);
            gl.clearTarget(lightTarget.handle);
            gl.bindTarget(lightTarget.handle, true);
            gl.bindTexture(0, depth);
            gl.bindTexture(1, voxelTexture);
            gl.bindTexture(2, DynamicLightShadowAtlas.depthTexture(gl));
            gl.bindTexture(3, lightTexture);
            light.use();
            gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_REPLACE);

            gl.bindTarget(null, true);
            gl.bindTexture(0, gl.colorTexture(lightTarget.handle));
            composite.use();
            composite.set("sourceScale", 1F);
            composite.set("lightScale", SURFACE_GAIN);
            gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_SURFACE);
            composite.set("lightScale", LIGHT_FLAT);
            gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_ADD);
        }
        if (beams && pushBeams(frame)) {
            beamTarget.resize(width, height);
            gl.clearTarget(beamTarget.handle);
            gl.bindTarget(beamTarget.handle, true);
            gl.bindTexture(0, depth);
            gl.bindTexture(2, DynamicLightShadowAtlas.depthTexture(gl));
            gl.bindTexture(3, beamTexture);
            beam.use();
            beam.set("beamScale", (float) BEAM_DIVISOR);
            gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_REPLACE);

            gl.bindTarget(null, true);
            gl.bindTexture(0, gl.colorTexture(beamTarget.handle));
            composite.use();
            composite.set("sourceScale", (float) BEAM_DIVISOR);
            composite.set("lightScale", 1F);
            gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_ADD);
        }
        if (coronas && pushCoronas(frame, width, height)) {
            gl.bindTarget(null, true);
            gl.bindTexture(0, depth);
            gl.bindTexture(3, coronaTexture);
            corona.use();
            gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_ADD);
        }
    }

    /**
     * The frame's lights into {@link #lightTexture}, each with its shadow slot and the view its tile was drawn with:
     * the light pass reads a tile through that view, not the lamp's pose this frame, since a tile is kept while its
     * lamp barely moves.
     */
    private static void uploadLights(DynamicLightFrame frame, int lightCount) {
        float[] data = frame.lightData;
        int stride = DynamicLightFrame.TEXELS * 4;
        for (int i = 0; i < lightCount; i++) {
            int o = i * stride;
            int slot = frame.shadows ? DynamicLightShadowAtlas.slotOf(i) : -1;
            data[o + 13] = slot;
            if (slot >= 0) {
                int s = slot >= DynamicLightShadowAtlas.CUBE ? slot - DynamicLightShadowAtlas.CUBE : slot;
                double[] lamp = DynamicLightShadowAtlas.lamp(s);
                float[] axis = DynamicLightShadowAtlas.axis(s);
                data[o + 16] = (float) (lamp[0] - frame.cameraX);
                data[o + 17] = (float) (lamp[1] - frame.cameraY);
                data[o + 18] = (float) (lamp[2] - frame.cameraZ);
                data[o + 19] = DynamicLightShadowAtlas.tan(s);
                data[o + 20] = axis[0];
                data[o + 21] = axis[1];
                data[o + 22] = axis[2];
                data[o + 23] = DynamicLightShadowAtlas.far(s);
            }
        }
        gl.uploadFloats(lightTexture, data, DynamicLightFrame.TEXELS, lightCount);
    }

    /** The camera's place in the block grid, on {@code program} (in use). */
    private static void setGrid(DynamicLightProgram program, DynamicLightVoxelWindow window, int bx, int by, int bz, float fx, float fy, float fz) {
        program.setInts("voxelCamBlock", bx, by, bz);
        program.set("voxelCamFrac", fx, fy, fz);
        if (window != null) {
            program.setInts("voxelMin", window.minX(), window.minY(), window.minZ());
        }
    }

    /**
     * The frame's beams into {@link #beamTexture}, a row each ({@link DynamicLightBeams#TEXELS} texels: the lens and
     * length, the axis and tan, the colour and shadow slot, the tile's lamp and tan, the tile's axis and far plane),
     * each with its lamp's shadow tile; false when none.
     */
    private static boolean pushBeams(DynamicLightFrame frame) {
        DynamicLightBeams beams = frame.beams;
        int n = beams.count();
        float[] d = beamData;
        for (int i = 0; i < n; i++) {
            int o = i * DynamicLightBeams.TEXELS * 4;
            double x = beams.get(i, 0);
            double y = beams.get(i, 1);
            double z = beams.get(i, 2);
            d[o] = (float) (x - frame.cameraX);
            d[o + 1] = (float) (y - frame.cameraY);
            d[o + 2] = (float) (z - frame.cameraZ);
            d[o + 3] = (float) beams.get(i, 7);
            d[o + 4] = (float) beams.get(i, 3);
            d[o + 5] = (float) beams.get(i, 4);
            d[o + 6] = (float) beams.get(i, 5);
            d[o + 7] = (float) beams.get(i, 6);
            int slot = frame.shadows ? DynamicLightShadowAtlas.slotAt(x, y, z) : -1;
            d[o + 8] = (float) beams.get(i, 8);
            d[o + 9] = (float) beams.get(i, 9);
            d[o + 10] = (float) beams.get(i, 10);
            d[o + 11] = slot;
            //The tile is read through the view it was drawn with, as the light pass does; a wide spot's are cube faces.
            if (slot >= 0) {
                int s = slot >= DynamicLightShadowAtlas.CUBE ? slot - DynamicLightShadowAtlas.CUBE : slot;
                double[] lamp = DynamicLightShadowAtlas.lamp(s);
                float[] axis = DynamicLightShadowAtlas.axis(s);
                d[o + 12] = (float) (lamp[0] - frame.cameraX);
                d[o + 13] = (float) (lamp[1] - frame.cameraY);
                d[o + 14] = (float) (lamp[2] - frame.cameraZ);
                d[o + 15] = DynamicLightShadowAtlas.tan(s);
                d[o + 16] = axis[0];
                d[o + 17] = axis[1];
                d[o + 18] = axis[2];
                d[o + 19] = DynamicLightShadowAtlas.far(s);
            }
        }
        if (n == 0) {
            return false;
        }
        gl.uploadFloats(beamTexture, d, DynamicLightBeams.TEXELS, n);
        beam.use();
        beam.setMatrix("invViewProj", invViewProj);
        beam.set("beamCount", n);
        return true;
    }

    /**
     * The frame's lamps on screen into {@link #coronaTexture} (a row each, {@link DynamicLightCoronas#TEXELS} texels:
     * screen place and size, the lens point's screen place and depth, the colour): each drawn at its lamp and shown
     * where the depth does not
     * cover its lens point, sized and levelled by its strength and bloom over distance; false when none is in front of
     * the camera.  Its size on screen is worked out at most at its bloom's growth distance and kept in the world
     * beyond: a far corona shrinks as the lamp does instead of staying a fixed share of the screen.
     */
    private static boolean pushCoronas(DynamicLightFrame frame, int width, int height) {
        DynamicLightCoronas list = frame.coronas;
        float[] p = point;
        int n = 0;
        for (int i = 0; i < list.count(); i++) {
            float day = CORONA_DAY + (1F - CORONA_DAY) * list.dim(i);
            float x = (float) (list.x(i) - frame.cameraX);
            float y = (float) (list.y(i) - frame.cameraY);
            float z = (float) (list.z(i) - frame.cameraZ);
            float distance = (float) Math.sqrt(x * x + y * y + z * z);
            int o = n * DynamicLightCoronas.TEXELS * 4;
            DynamicLightMatrix.transform(viewProj, (float) (list.testX(i) - frame.cameraX), (float) (list.testY(i) - frame.cameraY), (float) (list.testZ(i) - frame.cameraZ), 1F, p);
            if (!(p[3] > 0F)) {
                continue;
            }
            coronaData[o + 4] = p[0] / p[3] * 0.5F + 0.5F;
            coronaData[o + 5] = p[1] / p[3] * 0.5F + 0.5F;
            coronaData[o + 6] = p[2] / p[3] * 0.5F + 0.5F;
            DynamicLightMatrix.transform(viewProj, x, y, z, 1F, p);
            if (!(p[3] > 0F)) {
                continue;
            }
            float nx = p[0] / p[3];
            float ny = p[1] / p[3];
            if (Math.abs(nx) > 1.5F || Math.abs(ny) > 1.5F) {
                continue;
            }
            float bloom = list.bloom(i);
            float grown = Math.min(distance, DynamicLightAim.bloomGrowth(bloom));
            float falloff = distance / CORONA_FALLOFF;
            float grownFalloff = grown / CORONA_FALLOFF;
            float s = (float) Math.sqrt(list.strength(i) * bloom / (1F + falloff * falloff)) * day;
            float size = (CORONA_SIZE_MIN + CORONA_SIZE * (float) Math.sqrt(list.strength(i) * bloom / (1F + grownFalloff * grownFalloff)) * day) * grown / Math.max(distance, 1e-3F);
            coronaData[o] = nx * 0.5F + 0.5F;
            coronaData[o + 1] = ny * 0.5F + 0.5F;
            coronaData[o + 2] = p[2] / p[3] * 0.5F + 0.5F;
            coronaData[o + 3] = Math.min(CORONA_SIZE_MAX, size);
            coronaData[o + 8] = list.r(i) * s;
            coronaData[o + 9] = list.g(i) * s;
            coronaData[o + 10] = list.b(i) * s;
            coronaData[o + 11] = 1F;
            n++;
        }
        if (n == 0) {
            return false;
        }
        gl.uploadFloats(coronaTexture, coronaData, DynamicLightCoronas.TEXELS, n);
        corona.use();
        corona.set("coronaCount", n);
        corona.set("aspect", (float) width / Math.max(1, height));
        return true;
    }

    /**
     * Depth texture {@code from} into {@code to}'s depth, drawn by the copy shader.  Not a depth blit: a blit needs
     * both depth formats identical (a main target with Forge's stencil is not), and macOS drivers drop or half-copy it.
     */
    static void copyDepth(int from, Object to) {
        gl.bindTarget(to, true);
        gl.bindTexture(0, from);
        copy.use();
        gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_DEPTH);
    }

    /** A target the size of the screen over {@link #divisor}, made or remade on first use and on a resize. */
    private static final class Target {
        final int divisor;
        final boolean depth;
        final boolean floats;
        Object handle;
        int width;
        int height;

        Target(int divisor, boolean depth, boolean floats) {
            this.divisor = divisor;
            this.depth = depth;
            this.floats = floats;
        }

        void resize(int screenWidth, int screenHeight) {
            int w = (screenWidth + divisor - 1) / divisor;
            int h = (screenHeight + divisor - 1) / divisor;
            if (handle != null && width == w && height == h) {
                return;
            }
            if (handle != null) {
                gl.deleteTarget(handle);
            }
            handle = gl.createTarget(w, h, depth, floats, divisor > 1);
            width = w;
            height = h;
        }
    }
}
