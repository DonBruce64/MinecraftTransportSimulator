
package minecrafttransportsimulator.rendering.dynamiclight;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Block light raised by dynamic lights, for a shader pack that lights the world from the
 * lightmap.  The render thread submits every lit lamp each frame between {@link #beginFrame()} and
 * {@link #endFrame()}; {@link #levelAt} reads the last published snapshot from any thread, lock-free.
 * {@link #dirtyBoxes} (client thread) names the blocks whose meshes must be rebuilt.
 */
public final class DynamicLightField {

    /**
     * A lamp's reach is capped here, for the rebuild box. At 15 a headlight lit the ground it reaches 7 blocks out
     * at level 1-2 of 15 under a pack, so its beam read as dark.
     */
    public static final float MAX_INJECT_RADIUS = 32f;
    /** Minimum game ticks between two flushes of one lamp. */
    public static final int FLUSH_TICKS = 2;
    private static final double MOVE_SQ = 1.0;
    private static final double TURN_COS = Math.cos(Math.toRadians(10.0));
    private static final double INTENSITY_SHARE = 0.1;
    private static final double CONE_COS_STEP = 0.01;

    // Per lamp: x y z, dx dy dz, cosOuter cosInner, radius, intensity, spot (1/0), shadow clearance.
    private static final int STRIDE = 12;

    private static final class Snapshot {
        final long[] keys;
        final double[] data;
        final int count;

        Snapshot(long[] keys, double[] data, int count) {
            this.keys = keys;
            this.data = data;
            this.count = count;
        }
    }

    private static final Snapshot EMPTY = new Snapshot(new long[0], new double[0], 0);

    /** Last flushed state of one lamp key; {@code lit} = its box is in the world's meshes now. */
    private static final class Flushed {
        final double[] lamp = new double[STRIDE];
        final int[] box = new int[6];
        boolean lit;
        boolean flushedOnce;
        long tick;
        long seen;
    }

    private volatile Snapshot published = EMPTY;

    private long[] buildKeys = new long[16];
    private double[] buildData = new double[16 * STRIDE];
    private int buildCount;

    private final Map<Long, Flushed> flushed = new HashMap<>();
    private long pass;
    private int[] boxes = new int[6 * 16];
    private int boxCount;

    /** Starts collecting this frame's lamps. */
    public void beginFrame() {
        buildCount = 0;
    }

    /**
     * One lit lamp this frame.  {@code key} is its stable identity (owner id << 16 | component index);
     * a key submitted twice in a frame keeps the last.  A lamp with no light reach is ignored.
     */
    public void submit(long key, DynamicLightEmitter lamp) {
        if (lamp.radius <= 0F || lamp.intensity <= 0F) return;
        int slot = buildCount;
        for (int i = 0; i < buildCount; i++) {
            if (buildKeys[i] == key) { slot = i; break; }
        }
        if (slot == buildCount) {
            if (buildCount == buildKeys.length) {
                buildKeys = Arrays.copyOf(buildKeys, buildCount * 2);
                buildData = Arrays.copyOf(buildData, buildCount * 2 * STRIDE);
            }
            buildCount++;
        }
        buildKeys[slot] = key;
        int o = slot * STRIDE;
        buildData[o] = lamp.x;
        buildData[o + 1] = lamp.y;
        buildData[o + 2] = lamp.z;
        if (lamp.spot) {
            double len = Math.sqrt((double) lamp.dx * lamp.dx + (double) lamp.dy * lamp.dy + (double) lamp.dz * lamp.dz);
            if (len < 1e-6) len = 1;
            buildData[o + 3] = lamp.dx / len;
            buildData[o + 4] = lamp.dy / len;
            buildData[o + 5] = lamp.dz / len;
            buildData[o + 6] = lamp.cosOuter;
            buildData[o + 7] = lamp.cosInner;
            buildData[o + 10] = 1;
        } else {
            buildData[o + 3] = 0;
            buildData[o + 4] = 0;
            buildData[o + 5] = 0;
            buildData[o + 6] = -1;
            buildData[o + 7] = 1;
            buildData[o + 10] = 0;
        }
        buildData[o + 8] = Math.min(lamp.radius, MAX_INJECT_RADIUS);
        buildData[o + 9] = lamp.intensity;
        buildData[o + 11] = lamp.clearance;
    }

    /** Publishes this frame's lamps to {@link #levelAt}'s readers. */
    public void endFrame() {
        published = buildCount == 0 ? EMPTY
                : new Snapshot(Arrays.copyOf(buildKeys, buildCount),
                        Arrays.copyOf(buildData, buildCount * STRIDE), buildCount);
    }

    /** Whether the published snapshot holds any lamp. */
    public boolean isEmpty() {
        return published.count == 0;
    }

    /**
     * Block light 0..15 the published lamps put at a point: max over lamps of
     * {@code round(15 * min(1, (1 - d/r)^2 * intensity * cone))}. Any thread; no allocation.
     */
    public int levelAt(double x, double y, double z) {
        return levelAt(x, y, z, null);
    }

    /** {@link #levelAt(double, double, double)} with no light past a solid block of {@code blocks} (null: none). */
    public int levelAt(double x, double y, double z, DynamicLightVoxelWindow blocks) {
        Snapshot s = published;
        double[] a = s.data;
        int best = 0;
        for (int i = 0, o = 0; i < s.count; i++, o += STRIDE) {
            double ex = x - a[o], ey = y - a[o + 1], ez = z - a[o + 2];
            double d2 = ex * ex + ey * ey + ez * ez;
            double r = a[o + 8];
            if (d2 >= r * r) continue;
            double d = Math.sqrt(d2);
            double cone = 1.0;
            if (a[o + 10] != 0) {
                double cos = (ex * a[o + 3] + ey * a[o + 4] + ez * a[o + 5]) / Math.max(d, 0.001);
                cone = DynamicLightAim.cone((float) a[o + 6], (float) a[o + 7], (float) cos);
            }
            int level = level(d, r, a[o + 9], cone);
            if (level > best && (blocks == null || !blocks.blocked(x, y, z, a[o], a[o + 1], a[o + 2], a[o + 11]))) {
                best = level;
                if (best == 15) return 15;
            }
        }
        return best;
    }

    /**
     * The one lamp level formula: block light 0..15 at {@code distance} from a lamp of {@code radius},
     * {@code round(15 * min(1, (1 - d/r)^2 * intensity * cone))}, 0 at or beyond the radius.
     *
     * @param cone the spot's {@link DynamicLightAim#cone} factor, 1 for a point light
     */
    public static int level(double distance, double radius, double intensity, double cone) {
        if (!(distance < radius)) return 0;
        double f = 1.0 - distance / radius;
        double k = f * f * intensity * cone;
        if (k <= 0) return 0;
        return Math.round(15f * (float) Math.min(1.0, k));
    }

    /**
     * Boxes whose meshes must be rebuilt since the last call: for each lamp that appeared,
     * vanished, moved >= 1 block, turned >= 10 degrees or changed its light, the box it lit before
     * and the box it lights now; one flush per lamp per {@link #FLUSH_TICKS}. Returns the box count;
     * box {@code i} is {@code boxBuffer()[6i .. 6i+5]} = minX minY minZ maxX maxY maxZ, inclusive.
     */
    public int dirtyBoxes(long tick) {
        boxCount = 0;
        pass++;
        Snapshot s = published;
        for (int i = 0; i < s.count; i++) {
            int o = i * STRIDE;
            Flushed f = flushed.get(s.keys[i]);
            if (f == null) {
                f = new Flushed();
                flushed.put(s.keys[i], f);
            }
            f.seen = pass;
            if (f.lit && !changed(f.lamp, s.data, o)) continue;
            if (f.flushedOnce && tick - f.tick < FLUSH_TICKS) continue;
            if (f.lit) addBox(f.box);
            System.arraycopy(s.data, o, f.lamp, 0, STRIDE);
            boxOf(f.lamp, f.box);
            addBox(f.box);
            f.lit = true;
            f.flushedOnce = true;
            f.tick = tick;
        }
        for (Iterator<Flushed> it = flushed.values().iterator(); it.hasNext(); ) {
            Flushed f = it.next();
            if (f.seen == pass) continue;
            if (f.flushedOnce && tick - f.tick < FLUSH_TICKS) continue;
            if (f.lit) {
                addBox(f.box);
                f.lit = false;
                f.tick = tick;
            } else {
                it.remove();
            }
        }
        return boxCount;
    }

    /** Boxes of every lamp now in the meshes, then forgets them all (the next call starts afresh). */
    public int releaseAll() {
        boxCount = 0;
        for (Flushed f : flushed.values()) {
            if (f.lit) addBox(f.box);
        }
        flushed.clear();
        return boxCount;
    }

    /** The reused output of {@link #dirtyBoxes}/{@link #releaseAll}. */
    public int[] boxBuffer() {
        return boxes;
    }

    private static boolean changed(double[] was, double[] now, int o) {
        double ex = now[o] - was[0], ey = now[o + 1] - was[1], ez = now[o + 2] - was[2];
        if (ex * ex + ey * ey + ez * ez >= MOVE_SQ) return true;
        if (now[o + 10] != was[10] || now[o + 11] != was[11]) return true;
        if (Math.abs(now[o + 8] - was[8]) >= 1.0) return true;
        if (Math.abs(now[o + 9] - was[9]) > INTENSITY_SHARE * Math.max(now[o + 9], was[9])) return true;
        if (was[10] == 0) return false;
        if (now[o + 3] * was[3] + now[o + 4] * was[4] + now[o + 5] * was[5] <= TURN_COS) return true;
        return Math.abs(now[o + 6] - was[6]) > CONE_COS_STEP;
    }

    /** The blocks a lamp can light: its capped-radius cube, or for a spot the box of its cone sector. */
    static void boxOf(double[] lamp, int[] out) {
        double r = lamp[8];
        for (int axis = 0; axis < 3; axis++) {
            double lo = -r, hi = r;
            if (lamp[10] != 0) {
                double half = Math.acos(Math.max(-1.0, Math.min(1.0, lamp[6])));
                hi = sectorReach(lamp[3 + axis], half, r);
                lo = -sectorReach(-lamp[3 + axis], half, r);
            }
            out[axis] = (int) Math.floor(lamp[axis] + lo);
            out[axis + 3] = (int) Math.floor(lamp[axis] + hi);
        }
    }

    /** Farthest a spherical sector (axis cosine {@code cosAxis} to the direction, half angle {@code half}) reaches along it. */
    private static double sectorReach(double cosAxis, double half, double r) {
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, cosAxis)));
        if (angle <= half) return r;
        double past = angle - half;
        return past >= Math.PI / 2 ? 0.0 : r * Math.cos(past);
    }

    private void addBox(int[] box) {
        int o = boxCount * 6;
        if (o + 6 > boxes.length) boxes = Arrays.copyOf(boxes, boxes.length * 2);
        System.arraycopy(box, 0, boxes, o, 6);
        boxCount++;
    }
}
