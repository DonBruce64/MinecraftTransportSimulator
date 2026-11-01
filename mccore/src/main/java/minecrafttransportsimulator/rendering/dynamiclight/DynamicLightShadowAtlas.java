package minecrafttransportsimulator.rendering.dynamiclight;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;

/**
 * The dynamic lights' shadow tiles: a depth tile seen from the lamp holding what
 * stops its light, one per spot light and six per point light (+X -X +Y -Y +Z -Z), while {@link #SLOTS} last.  A
 * spot wider than {@link #WIDE_COS} takes six faces as a point light does: one perspective tile near 180 degrees
 * spreads each texel over a quarter block or more, so its shadows break into squares.
 * The blocks are traced into it ({@code lamp_trace.fsh}, from the block grid); the bodies in its cone are drawn
 * over them, so a shadow takes a body's real shape: an IV entity by the vertex buffers its solid pass drew this
 * frame ({@link #captureBody}, or {@link #captureVertices} in the Slow rendering mode), a living entity by its own
 * renderer ({@link IInterfaceDynamicLightingGL#captureLiving}).  A body shades only beyond the lamp's clearance
 * ({@link DynamicLightEmitter#clearance}) and only where its texture shows ({@code lamp_body.fsh}), so a lamp's own
 * lens and housing never hide it, nor does an invisible part, a window's clear glass or a grille's holes.  A
 * tile out of the camera's view, or whose lamp, blocks and bodies are as last frame, is not drawn again; whoever reads
 * a tile reads it through the view it was drawn with ({@link #lamp}, {@link #axis}), never the lamp's pose this frame,
 * so a kept tile can lag its lamp but never shades a surface with itself.  IV's seated riders and invisible entities
 * cast none.
 * Render thread only.
 */
public final class DynamicLightShadowAtlas {
    static final int SLOTS = 64;
    /** {@link #slotOf}'s offset of a point light's first face (light_pass.fsh's CUBE). */
    static final int CUBE = 1000;
    /** A tile's side, texels. */
    private static final int TILE = 256;
    /** A point light's face's side, texels, in its tile's corner (light_pass.fsh's tileLight). */
    private static final int FACE_TILE = TILE / 2;
    private static final int COLUMNS = 8;
    /** The near plane, blocks: a body right at the lens still lands in the tile. */
    private static final float NEAR = 0.05F;
    /** Beyond the cone's half angle, degrees, so a body at the edge is whole in the tile. */
    private static final float MARGIN = 3F;
    /** A cube face's half view, degrees: 45 and the margin (light_pass.fsh's FACE_TAN is its tan). */
    private static final float FACE_HALF = 45F + MARGIN;
    /** The cosine of a cube face's reach for bodies: its corner, 54.7 degrees, and the margin. */
    private static final float FACE_COS = (float) Math.cos(Math.toRadians(58.0));
    private static final float[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
    /** A lamp this still (blocks, and cosine of its turn, about 0.06 degrees) keeps last frame's tile. */
    private static final float STILL = 1e-3F;
    private static final float STILL_COS = 0.9999995F;
    /** Bodies whose shape moved less than this keep last frame's tile. */
    private static final float BODY_STILL = 0.01F;
    /** A light this near its last frame's place (blocks, and cosine of its turn) keeps its slot while it moves. */
    private static final float FOLLOW = 2F;
    private static final float FOLLOW_COS = 0.9F;
    /** The cos of the widest half angle one spot tile covers (60 degrees); a wider spot takes a cube's six faces. */
    private static final float WIDE_COS = 0.5F;
    /** A light dimmer than this takes no tile and walks the grid per pixel (light_pass.fsh's blockLight). */
    private static final float SHADOW_MIN_INTENSITY = 0.25F;

    private static final float[][] VIEW_PROJ = new float[SLOTS][16];
    /** Each slot's lamp and direction, camera-relative to the frame it was placed in, plus that camera. */
    private static final double[][] LAMP = new double[SLOTS][3];
    private static final float[][] DIR = new float[SLOTS][3];
    private static final float[] FAR = new float[SLOTS];
    private static final float[] TAN = new float[SLOTS];
    private static final int[] LIGHT = new int[SLOTS];
    private static final boolean[] LIVE = new boolean[SLOTS];
    private static final boolean[] HELD = new boolean[SLOTS];
    private static final double[][] KEY_AT = new double[SLOTS][3];
    private static final float[][] KEY_DIR = new float[SLOTS][3];
    private static final boolean[] KEY_POINT = new boolean[SLOTS];
    private static int[] slotFor = new int[16];
    private static final boolean[] FACE = new boolean[SLOTS];
    private static final float[] HALF = new float[SLOTS];
    private static final int[] TRACED = new int[SLOTS];
    private static final boolean[] BODIES = new boolean[SLOTS];
    private static final float[][] BODY_SHAPES = new float[SLOTS][];
    private static float[] shapes = new float[64];
    private static final double[][] BLOCK_LAMP = new double[SLOTS][3];
    private static final float[][] BLOCK_DIR = new float[SLOTS][3];
    private static final float[] BLOCK_HALF = new float[SLOTS];
    private static final float[] BLOCK_FAR = new float[SLOTS];
    private static final float[] BLOCK_CLEAR = new float[SLOTS];
    private static final int[] BLOCK_TRACED = new int[SLOTS];

    /** This frame's IV bodies: vertex buffers drawn by the solid pass, with their camera-relative model matrix. */
    private static final List<DynamicLightBody> CAPTURED = new ArrayList<>();
    /** This frame's living candidates, their corners drawn the first time a tile needs them. */
    private static final List<DynamicLightBody> LIVING = new ArrayList<>();
    private static final List<DynamicLightBody> POOL = new ArrayList<>();
    private static boolean[] livingDrawn = new boolean[16];
    /** The bodies in the tile being drawn. */
    private static final List<DynamicLightBody> BODIES_IN_TILE = new ArrayList<>();
    private static boolean capturing;
    private static boolean rendering;
    private static IInterfaceDynamicLightingGL gl;
    private static Object target;
    /** The atlas of blocks alone; the body tiles start from it.  Laid out as {@link #target}. */
    private static Object blocks;
    private static DynamicLightFrustum frustum;
    private static DynamicLightFrame frame;
    private static float partialTick;
    /** From the space the solid pass drew in (view rotation applied) to camera-relative world. */
    private static final float[] toCamera = new float[16];
    private static final float[] projection = new float[16];
    private static final float[] scratch = new float[16];
    private static final float[] modelView = new float[16];

    static {
        for (int i = 0; i < SLOTS; i++) {
            TRACED[i] = -1;
            BLOCK_TRACED[i] = -1;
        }
    }

    private DynamicLightShadowAtlas() {
    }

    /** Starts or stops recording the solid pass's vertex buffers as bodies. */
    public static void setCapturing(boolean capture) {
        capturing = capture;
    }

    /** Whether shadow tiles are being drawn: entity renderers drawn into one should skip their name tags. */
    public static boolean isRendering() {
        return rendering;
    }

    /** Whether the solid pass's draws are being recorded as bodies now. */
    public static boolean isCapturing() {
        return capturing;
    }

    /**
     * One vertex buffer (the version's own type) the solid pass drew this frame with model-view {@code pose} (16
     * floats, column-major) and GL {@code texture} (0 for none); {@code radius} bounds its vertices in its own space.
     */
    public static void captureBody(Object buffer, float[] pose, float radius, int texture) {
        if (!capturing || buffer == null) {
            return;
        }
        DynamicLightBody body = POOL.isEmpty() ? new DynamicLightBody() : POOL.remove(POOL.size() - 1);
        body.beginCaptured(buffer, pose, radius, texture);
        CAPTURED.add(body);
    }

    /**
     * IV vertices the solid pass drew this frame without a buffer object (the Slow rendering mode), {@code NX, NY, NZ,
     * U, V, X, Y, Z} a vertex, with model-view {@code pose} (16 floats, column-major) and GL {@code texture} (0 for
     * none), read from 0 to their limit.
     */
    public static void captureVertices(FloatBuffer vertices, float[] pose, int texture) {
        if (!capturing || vertices == null || vertices.limit() < 24) {
            return;
        }
        DynamicLightBody body = POOL.isEmpty() ? new DynamicLightBody() : POOL.remove(POOL.size() - 1);
        body.beginVertices(vertices, pose, texture);
        CAPTURED.add(body);
    }

    /**
     * One living entity (the version's own type) that may cast a shadow this frame, centred at world {@code x, y, z}
     * within {@code radius}; called from {@link IInterfaceDynamicLightingGL#listLivingBodies}.
     */
    public static void addCandidate(Object entity, double x, double y, double z, float radius) {
        DynamicLightBody body = POOL.isEmpty() ? new DynamicLightBody() : POOL.remove(POOL.size() - 1);
        body.beginLiving(entity, x, y, z, radius);
        LIVING.add(body);
    }

    /** The frame is over (or starts): its captures are forgotten. */
    public static void frameEnd() {
        for (DynamicLightBody body : CAPTURED) {
            body.buffer = null;
        }
        POOL.addAll(CAPTURED);
        CAPTURED.clear();
        for (DynamicLightBody body : LIVING) {
            body.entity = null;
        }
        POOL.addAll(LIVING);
        LIVING.clear();
        frustum = null;
        frame = null;
    }

    /** Every frame first: the slots held become last frame's. */
    static void skip() {
        System.arraycopy(LIVE, 0, HELD, 0, SLOTS);
        Arrays.fill(LIVE, false);
    }

    /**
     * This frame's tiles for {@code frame}'s lights while slots remain, blocks traced by {@code trace} (its grid
     * uniforms already set by the caller), bodies drawn over a copy of the blocks; a tile out of {@code visible} is
     * left.  {@code view} is this frame's camera rotation.
     */
    static void render(IInterfaceDynamicLightingGL glCalls, DynamicLightFrame currentFrame, float[] view, DynamicLightFrustum visible, DynamicLightProgram trace, DynamicLightProgram copy, DynamicLightProgram body, int voxelTexture) {
        gl = glCalls;
        frame = currentFrame;
        frustum = visible;
        partialTick = currentFrame.partialTicks;
        DynamicLightMatrix.invert(view, toCamera);
        for (DynamicLightBody captured : CAPTURED) {
            captured.place(toCamera, frame);
        }
        POOL.addAll(LIVING);
        LIVING.clear();
        gl.listLivingBodies(partialTick);
        if (livingDrawn.length < LIVING.size()) {
            livingDrawn = new boolean[LIVING.size() * 2];
        }
        Arrays.fill(livingDrawn, false);
        int version = currentFrame.voxelVersion;
        List<DynamicLightEmitter> lights = currentFrame.lights;
        try {
            assign(lights);
            for (int i = 0; i < lights.size(); i++) {
                DynamicLightEmitter e = lights.get(i);
                int s = slotFor[i];
                if (s < 0) {
                    continue;
                }
                float reach = Math.max(e.radius, NEAR * 2F);
                if (!cube(e)) {
                    float cos = Math.max(-1F, Math.min(1F, e.cosOuter));
                    float half = (float) Math.min(Math.toRadians(85.0), Math.acos(cos) + Math.toRadians(MARGIN));
                    tile(s, i, false, e, e.dx, e.dy, e.dz, cos, half, reach, version, trace, copy, body, voxelTexture);
                } else {
                    float half = (float) Math.toRadians(FACE_HALF);
                    for (int f = 0; f < FACES.length; f++) {
                        tile(s + f, i, true, e, FACES[f][0], FACES[f][1], FACES[f][2], FACE_COS, half, reach, version, trace, copy, body, voxelTexture);
                    }
                }
            }
        } finally {
            rendering = false;
        }
        for (int s = 0; s < SLOTS; s++) {
            if (!LIVE[s]) {
                TRACED[s] = -1;
            }
        }
    }

    /**
     * {@link #slotFor} each light, or -1: a still light the slot it held, then a moving one the slot it held, then
     * the rest a free one.  A point light takes six slots from the top, so spots from the bottom never split its run.
     */
    private static void assign(List<DynamicLightEmitter> lights) {
        if (slotFor.length < lights.size()) {
            slotFor = new int[lights.size()];
        }
        Arrays.fill(slotFor, -1);
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < lights.size(); i++) {
                DynamicLightEmitter e = lights.get(i);
                if (slotFor[i] >= 0 || e.intensity < SHADOW_MIN_INTENSITY) {
                    continue;
                }
                boolean point = cube(e);
                float dx = point ? FACES[0][0] : e.dx;
                float dy = point ? FACES[0][1] : e.dy;
                float dz = point ? FACES[0][2] : e.dz;
                int s = pass == 0 ? held(point, e, dx, dy, dz, STILL, STILL_COS) : pass == 1 ? held(point, e, dx, dy, dz, FOLLOW, FOLLOW_COS) : free(point);
                if (s >= 0) {
                    slotFor[i] = s;
                    int n = point ? FACES.length : 1;
                    for (int k = s; k < s + n; k++) {
                        LIVE[k] = true;
                        KEY_POINT[k] = point;
                    }
                    KEY_AT[s][0] = e.x;
                    KEY_AT[s][1] = e.y;
                    KEY_AT[s][2] = e.z;
                    KEY_DIR[s][0] = dx;
                    KEY_DIR[s][1] = dy;
                    KEY_DIR[s][2] = dz;
                }
            }
        }
    }

    /** Whether light {@code e} takes six cube faces: a point light, or a spot too wide for one tile. */
    private static boolean cube(DynamicLightEmitter e) {
        return !e.spot || e.cosOuter < WIDE_COS;
    }

    private static int held(boolean point, DynamicLightEmitter e, float dx, float dy, float dz, float reach, float cos) {
        int step = point ? -FACES.length : 1;
        for (int s = point ? SLOTS - FACES.length : 0; s >= 0 && s < SLOTS; s += step) {
            if (HELD[s] && !LIVE[s] && KEY_POINT[s] == point && distanceSquared(KEY_AT[s], e.x, e.y, e.z) < reach * reach && (point || KEY_DIR[s][0] * dx + KEY_DIR[s][1] * dy + KEY_DIR[s][2] * dz > cos)) {
                return s;
            }
        }
        return -1;
    }

    private static int free(boolean point) {
        if (!point) {
            for (int s = 0; s < SLOTS; s++) {
                if (!LIVE[s]) {
                    return s;
                }
            }
            return -1;
        }
        for (int s = SLOTS - FACES.length; s >= 0; s -= FACES.length) {
            boolean empty = true;
            for (int k = s; k < s + FACES.length; k++) {
                empty &= !LIVE[k];
            }
            if (empty) {
                return s;
            }
        }
        return -1;
    }

    private static double distanceSquared(double[] a, double x, double y, double z) {
        double ex = a[0] - x;
        double ey = a[1] - y;
        double ez = a[2] - z;
        return ex * ex + ey * ey + ez * ez;
    }

    /**
     * Slot {@code s} for light {@code index} from {@code lamp} along {@code dx, dy, dz}, seeing {@code half} off it
     * and bodies within the cone of {@code cos}; kept as last frame when nothing in it moved.
     */
    private static void tile(int s, int index, boolean face, DynamicLightEmitter lamp, float dx, float dy, float dz, float cos, float half, float reach, int version, DynamicLightProgram trace, DynamicLightProgram copy, DynamicLightProgram body, int voxelTexture) {
        LIGHT[s] = index;
        FACE[s] = face;
        if (!seen(lamp, dx, dy, dz, (float) Math.tan(half), reach)) {
            //Nothing on screen lies in it, so nothing reads it: traced afresh once it is.  Its view still follows the
            //lamp, as a point light's faces are read through their first one's lamp.
            TRACED[s] = -1;
            LAMP[s][0] = lamp.x;
            LAMP[s][1] = lamp.y;
            LAMP[s][2] = lamp.z;
            DIR[s][0] = dx;
            DIR[s][1] = dy;
            DIR[s][2] = dz;
            FAR[s] = reach;
            TAN[s] = (float) Math.tan(half);
            return;
        }
        boolean bodies = collect(lamp, dx, dy, dz, cos, reach) && body != null && copy != null;
        boolean blocksKept = BLOCK_TRACED[s] == version && BLOCK_FAR[s] == reach && BLOCK_CLEAR[s] == lamp.clearance && BLOCK_HALF[s] == half && distanceSquared(BLOCK_LAMP[s], lamp.x, lamp.y, lamp.z) < STILL * STILL && BLOCK_DIR[s][0] * dx + BLOCK_DIR[s][1] * dy + BLOCK_DIR[s][2] * dz > STILL_COS;
        int shaped = bodies ? shapes() : 0;
        //Redrawn only for a change: a tile written every frame while the light pass reads it costs far more than its own draw.
        if (bodies == BODIES[s] && blocksKept && TRACED[s] == version && (!bodies || sameShapes(s, shaped))) {
            return;
        }
        begin();
        if (blocksKept) {
            //The view the blocks were traced under, so the bodies drawn over them line up.
            place(s, BLOCK_LAMP[s][0], BLOCK_LAMP[s][1], BLOCK_LAMP[s][2], BLOCK_DIR[s][0], BLOCK_DIR[s][1], BLOCK_DIR[s][2], half, reach);
        } else {
            place(s, lamp.x, lamp.y, lamp.z, dx, dy, dz, half, reach);
            gl.bindTarget(blocks, false);
            viewport(s);
            gl.clearDepth((s % COLUMNS) * TILE, (s / COLUMNS) * TILE, size(s), size(s));
            trace(s, lamp.clearance, trace, voxelTexture);
            BLOCK_LAMP[s][0] = lamp.x;
            BLOCK_LAMP[s][1] = lamp.y;
            BLOCK_LAMP[s][2] = lamp.z;
            BLOCK_DIR[s][0] = dx;
            BLOCK_DIR[s][1] = dy;
            BLOCK_DIR[s][2] = dz;
            BLOCK_HALF[s] = half;
            BLOCK_FAR[s] = reach;
            BLOCK_CLEAR[s] = lamp.clearance;
            BLOCK_TRACED[s] = version;
        }
        HALF[s] = half;
        TRACED[s] = version;
        BODIES[s] = bodies;
        if (bodies) {
            BODY_SHAPES[s] = Arrays.copyOf(shapes, shaped);
        }
        copyBlocks(s, copy);
        if (bodies) {
            draw(s, lamp.clearance, body);
        }
    }

    /** {@link #BODIES_IN_TILE}' shapes into {@link #shapes}; their float count. */
    private static int shapes() {
        int n = 0;
        for (DynamicLightBody b : BODIES_IN_TILE) {
            n += b.shape(frame).length;
        }
        if (shapes.length < n) {
            shapes = new float[n];
        }
        int at = 0;
        for (DynamicLightBody b : BODIES_IN_TILE) {
            float[] shape = b.shape(frame);
            System.arraycopy(shape, 0, shapes, at, shape.length);
            at += shape.length;
        }
        return n;
    }

    private static boolean sameShapes(int s, int n) {
        float[] was = BODY_SHAPES[s];
        if (was == null || was.length != n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (Math.abs(was[i] - shapes[i]) > BODY_STILL) {
                return false;
            }
        }
        return true;
    }

    /** Tile {@code s} of {@link #blocks} into {@link #target}, which is left bound with the tile's viewport. */
    private static void copyBlocks(int s, DynamicLightProgram copy) {
        gl.bindTarget(target, false);
        viewport(s);
        copy.use();
        copy.set("Sampler0", 0);
        gl.bindTexture(0, gl.depthTexture(blocks));
        gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_DEPTH);
    }

    /** Whether the camera sees any of the pyramid from {@code lamp} along {@code d}, {@code tan} wide, to {@code far}. */
    private static boolean seen(DynamicLightEmitter lamp, float dx, float dy, float dz, float tan, float far) {
        if (frustum == null) {
            return true;
        }
        boolean level = Math.abs(dy) < 0.99F;
        //side = d x (level ? up : x)
        float sx = level ? -dz : 0;
        float sy = level ? 0 : dz;
        float sz = level ? dx : -dy;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx /= sl;
        sy /= sl;
        sz /= sl;
        //up = side x d
        float ux = sy * dz - sz * dy;
        float uy = sz * dx - sx * dz;
        float uz = sx * dy - sy * dx;
        double minX = lamp.x, minY = lamp.y, minZ = lamp.z;
        double maxX = minX, maxY = minY, maxZ = minZ;
        for (int k = 0; k < 4; k++) {
            float a = (k & 1) == 0 ? -tan : tan;
            float b = (k & 2) == 0 ? -tan : tan;
            double x = lamp.x + far * (dx + a * sx + b * ux);
            double y = lamp.y + far * (dy + a * sy + b * uy);
            double z = lamp.z + far * (dz + a * sz + b * uz);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        return frustum.isVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** The shadow slot of light {@code index}: a spot's tile, a point's first face plus {@link #CUBE}, or -1. */
    static int slotOf(int index) {
        for (int s = 0; s < SLOTS; s++) {
            if (LIVE[s] && LIGHT[s] == index) {
                return FACE[s] ? s + CUBE : s;
            }
        }
        return -1;
    }

    /**
     * The slot of the light whose lamp stands at world {@code x, y, z}: a spot's drawn tile, a cube's first face plus
     * {@link #CUBE} (a cube's run is scanned from its first face), or -1.
     */
    static int slotAt(double x, double y, double z) {
        for (int s = 0; s < SLOTS; s++) {
            if (LIVE[s] && distanceSquared(LAMP[s], x, y, z) < 0.01) {
                if (FACE[s]) {
                    return s + CUBE;
                } else if (TRACED[s] >= 0) {
                    return s;
                }
            }
        }
        return -1;
    }

    /** Slot {@code s}'s lamp as its tile was drawn, world space (a point light's faces share their first's). */
    static double[] lamp(int s) {
        return LAMP[s];
    }

    /** Slot {@code s}'s axis as its tile was drawn (a cube face's own axis). */
    static float[] axis(int s) {
        return DIR[s];
    }

    /** Slot {@code s}'s far plane, blocks (its near plane is light_pass.fsh's SHADOW_NEAR). */
    static float far(int s) {
        return FAR[s];
    }

    /** Slot {@code s}'s tan of half its field of view. */
    static float tan(int s) {
        return TAN[s];
    }

    /** The depth atlas: 8 x 8 tiles, slot {@code s} at column {@code s % 8}, row {@code s / 8}. */
    static int depthTexture(IInterfaceDynamicLightingGL glCalls) {
        return target == null ? 0 : glCalls.depthTexture(target);
    }

    /** Whether a sphere at {@code x, y, z} of {@code r} meets the cone from {@code l} along unit {@code d}. */
    private static boolean inCone(DynamicLightEmitter l, float dx, float dy, float dz, float cos, float reach, double x, double y, double z, float r) {
        x -= l.x;
        y -= l.y;
        z -= l.z;
        double distance = Math.sqrt(x * x + y * y + z * z);
        if (distance > reach + r) {
            return false;
        }
        if (distance <= r) {
            return true;
        }
        double angle = Math.acos(Math.max(-1, Math.min(1, (x * dx + y * dy + z * dz) / distance)));
        return angle - Math.asin(r / distance) <= Math.acos(cos);
    }

    /** Living candidate {@code i}, its corners drawn by its renderer the first time a tile needs them. */
    private static DynamicLightBody living(int i) {
        DynamicLightBody body = LIVING.get(i);
        if (!livingDrawn[i]) {
            livingDrawn[i] = true;
            boolean was = rendering;
            rendering = true;
            try {
                gl.captureLiving(body, frame.cameraX, frame.cameraY, frame.cameraZ, partialTick);
            } catch (Exception e) {
                //A renderer that cannot draw outside its normal pass simply casts no shadow.
                body.clearLiving();
            } finally {
                rendering = was;
            }
        }
        return body;
    }

    /** The living and IV bodies in the cone; false when none. */
    private static boolean collect(DynamicLightEmitter lamp, float dx, float dy, float dz, float cos, float reach) {
        BODIES_IN_TILE.clear();
        for (int i = 0; i < LIVING.size(); i++) {
            DynamicLightBody candidate = LIVING.get(i);
            if (inCone(lamp, dx, dy, dz, cos, reach, candidate.centreX, candidate.centreY, candidate.centreZ, candidate.radius)) {
                DynamicLightBody body = living(i);
                if (body.drawn()) {
                    BODIES_IN_TILE.add(body);
                }
            }
        }
        for (DynamicLightBody body : CAPTURED) {
            if (inCone(lamp, dx, dy, dz, cos, reach, body.centreX, body.centreY, body.centreZ, body.radius)) {
                BODIES_IN_TILE.add(body);
            }
        }
        return !BODIES_IN_TILE.isEmpty();
    }

    private static void begin() {
        if (target == null) {
            target = gl.createTarget(TILE * COLUMNS, TILE * (SLOTS / COLUMNS), true, false, false);
            gl.clearTarget(target);
            blocks = gl.createTarget(TILE * COLUMNS, TILE * (SLOTS / COLUMNS), true, false, false);
            gl.clearTarget(blocks);
        }
        rendering = true;
    }

    /** Slot {@code s}'s lamp at world {@code x, y, z} along {@code d}, seeing {@code half} off it: its view. */
    private static void place(int s, double x, double y, double z, float dx, float dy, float dz, float half, float reach) {
        FAR[s] = reach;
        TAN[s] = (float) Math.tan(half);
        DynamicLightMatrix.mul(projection(half, reach), lampView(dx, dy, dz, scratch), VIEW_PROJ[s]);
        LAMP[s][0] = x;
        LAMP[s][1] = y;
        LAMP[s][2] = z;
        DIR[s][0] = dx;
        DIR[s][1] = dy;
        DIR[s][2] = dz;
    }

    private static void viewport(int s) {
        gl.viewport((s % COLUMNS) * TILE, (s / COLUMNS) * TILE, size(s), size(s));
    }

    /** {@link #projection} set for a tile seeing {@code half} off its axis to {@code reach}. */
    private static float[] projection(float half, float reach) {
        return DynamicLightMatrix.perspective(2F * half, 1F, NEAR, reach, projection);
    }

    /** A tile's view: from the lamp along {@code d}; light_pass.fsh's tileBasis is the same. */
    private static float[] lampView(float dx, float dy, float dz, float[] out) {
        boolean level = Math.abs(dy) < 0.99F;
        return DynamicLightMatrix.lookAlong(dx, dy, dz, level ? 0F : 1F, level ? 1F : 0F, 0F, out);
    }

    private static int size(int s) {
        return FACE[s] ? FACE_TILE : TILE;
    }

    /** The blocks into tile {@code s}: each texel walks the grid from the lamp ({@code lamp_trace.fsh}). */
    private static void trace(int s, float clearance, DynamicLightProgram trace, int voxelTexture) {
        if (trace == null) {
            return;
        }
        float[] tileProjection = projection((float) Math.atan(TAN[s]), FAR[s]);
        trace.use();
        trace.set("Sampler1", 1);
        trace.setMatrix("lampInvViewProj", DynamicLightMatrix.invert(VIEW_PROJ[s], scratch));
        trace.set("lampPos", (float) (LAMP[s][0] - frame.cameraX), (float) (LAMP[s][1] - frame.cameraY), (float) (LAMP[s][2] - frame.cameraZ));
        trace.set("lampForward", DIR[s][0], DIR[s][1], DIR[s][2]);
        trace.set("tile", (float) ((s % COLUMNS) * TILE), (float) ((s / COLUMNS) * TILE), (float) size(s), FAR[s]);
        trace.set("lampDepth", tileProjection[10], tileProjection[14]);
        trace.set("lampClearance", clearance);
        gl.bindTexture(1, voxelTexture);
        gl.drawFullscreen(IInterfaceDynamicLightingGL.DRAW_DEPTH_NEARER);
    }

    /** {@link #BODIES_IN_TILE} seen from slot {@code s}'s lamp into its tile, over its blocks. */
    private static void draw(int s, float clearance, DynamicLightProgram program) {
        float[] lampView = DynamicLightMatrix.translate(lampView(DIR[s][0], DIR[s][1], DIR[s][2], scratch), (float) (frame.cameraX - LAMP[s][0]), (float) (frame.cameraY - LAMP[s][1]), (float) (frame.cameraZ - LAMP[s][2]));
        program.use();
        program.setMatrix("ProjMat", projection((float) Math.atan(TAN[s]), FAR[s]));
        //Each body binds its own texture on unit 0 and sets the alpha cut of each run (IInterfaceDynamicLightingGL#drawBody).
        program.set("Sampler0", 0);
        program.set("lampClearance", clearance);
        int cutLocation = program.location("alphaCut");
        gl.beginBodies();
        for (DynamicLightBody body : BODIES_IN_TILE) {
            program.setMatrix("ModelViewMat", DynamicLightMatrix.mul(lampView, body.model, modelView));
            gl.drawBody(body, cutLocation);
        }
        gl.endBodies();
    }
}
