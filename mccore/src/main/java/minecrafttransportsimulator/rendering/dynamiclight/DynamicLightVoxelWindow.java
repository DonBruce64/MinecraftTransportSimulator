
package minecrafttransportsimulator.rendering.dynamiclight;

import java.util.Arrays;

/**
 * Which blocks the light pass's shadow grid holds: a window of {@link #SECTIONS_XZ} x {@link #SECTIONS_Y} x
 * {@link #SECTIONS_XZ} 16-block sections around the camera, addressed toroidally (block {@code x & 127},
 * {@code y & 63}, {@code z & 127}), so a recentre refills only the sections that entered. Holds a byte a block in an
 * atlas-shaped array (the adapter uploads it): 0 nothing, {@link #SOLID} a full block, else the id of the block's shape
 * in {@link #shapes} (a slab, stairs, a bell: its boxes cast its shadow). It tracks which held sections need reading and which changed
 * since the adapter last took them ({@link #pollChanged}): a section that entered is emptied at once, and the copy on
 * the GPU must be emptied with it, or the shaders read the blocks its slot held for the far side of the window.
 * voxel.glsl mirrors {@link #texelX}/{@link #texelY}, light_pass.fsh {@link #blocked} (which, for the shader pack
 * fallback, stops only at full blocks). Written on one thread; read from any (a stale byte only delays a shadow).
 */
public final class DynamicLightVoxelWindow {

    public static final int SECTIONS_XZ = 8;
    public static final int SECTIONS_Y = 4;
    public static final int SIZE_XZ = SECTIONS_XZ * 16;
    public static final int SIZE_Y = SECTIONS_Y * 16;
    /** The atlas the grid is stored in: one {@code SIZE_XZ}-square tile per y layer, 8 tiles a row. */
    public static final int ATLAS = 8 * SIZE_XZ;

    /** A full block's byte: 1.0 in the R8 texture voxel.glsl reads. */
    public static final byte SOLID = (byte) 0xFF;

    private static final long NONE = Long.MIN_VALUE;
    private static final int SLOTS = SECTIONS_XZ * SECTIONS_Y * SECTIONS_XZ;
    /**
     * Blocks the camera may stray out of its centre section before the window follows: a camera bobbing across a
     * section's edge (a hovering helicopter) does not empty and refill a layer of sections every frame.
     */
    private static final int HYSTERESIS = 4;

    private final long[] held = new long[SLOTS];
    private final boolean[] dirty = new boolean[SLOTS];
    private final boolean[] changed = new boolean[SLOTS];
    private final byte[] atlas = new byte[ATLAS * ATLAS];
    private final DynamicLightBlockShapes shapes = new DynamicLightBlockShapes();
    private int centreX;
    private int centreY;
    private int centreZ;
    private boolean placed;

    public DynamicLightVoxelWindow() {
        Arrays.fill(held, NONE);
    }

    /**
     * Centres the window on the section holding block {@code x, y, z} once the block is more than {@link #HYSTERESIS}
     * blocks out of the centre section; sections that enter it are emptied and need reading.
     */
    public void recentre(int x, int y, int z) {
        if (placed && near(x, centreX) && near(y, centreY) && near(z, centreZ)) {
            return;
        }
        int sx = x >> 4;
        int sy = y >> 4;
        int sz = z >> 4;
        placed = true;
        centreX = sx;
        centreY = sy;
        centreZ = sz;
        for (int ix = 0; ix < SECTIONS_XZ; ix++) {
            for (int iy = 0; iy < SECTIONS_Y; iy++) {
                for (int iz = 0; iz < SECTIONS_XZ; iz++) {
                    int wx = minSectionX() + ix;
                    int wy = minSectionY() + iy;
                    int wz = minSectionZ() + iz;
                    int slot = slot(wx, wy, wz);
                    long key = key(wx, wy, wz);
                    if (held[slot] != key) {
                        held[slot] = key;
                        dirty[slot] = true;
                        changed[slot] = true;
                        fillSection(wx, wy, wz, false);
                    }
                }
            }
        }
    }

    /** A block changed: its section, when held, needs reading again. */
    public void markBlock(int x, int y, int z) {
        markSection(x >> 4, y >> 4, z >> 4);
    }

    /** A chunk column arrived: its held sections need reading. */
    public void markColumn(int chunkX, int chunkZ) {
        for (int iy = 0; iy < SECTIONS_Y; iy++) {
            markSection(chunkX, minSectionY() + iy, chunkZ);
        }
    }

    /** Forgets everything held: a new level. */
    public void reset() {
        Arrays.fill(held, NONE);
        Arrays.fill(dirty, false);
        Arrays.fill(changed, false);
        Arrays.fill(atlas, (byte) 0);
        shapes.reset();
        placed = false;
    }

    /**
     * The held section nearest the centre that needs reading, into {@code out} as section coordinates; false when
     * none does. It no longer needs reading once returned, and counts as changed: the caller reads it now.
     */
    public boolean poll(int[] out) {
        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!dirty[slot]) {
                continue;
            }
            long key = held[slot];
            int dx = sectionX(key) - centreX;
            int dy = sectionY(key) - centreY;
            int dz = sectionZ(key) - centreZ;
            int d = dx * dx + dy * dy + dz * dz;
            if (d < bestDistance) {
                bestDistance = d;
                best = slot;
            }
        }
        if (best < 0) {
            return false;
        }
        dirty[best] = false;
        changed[best] = true;
        out[0] = sectionX(held[best]);
        out[1] = sectionY(held[best]);
        out[2] = sectionZ(held[best]);
        return true;
    }

    /**
     * A held section whose blocks changed since the last call (emptied on entering, or read), into {@code out} as
     * section coordinates; false when none.  The adapter uploads each one, so the GPU copy matches the grid.
     */
    public boolean pollChanged(int[] out) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (changed[slot]) {
                changed[slot] = false;
                out[0] = sectionX(held[slot]);
                out[1] = sectionY(held[slot]);
                out[2] = sectionZ(held[slot]);
                return true;
            }
        }
        return false;
    }

    public boolean placed() {
        return placed;
    }

    /** The window's least block corner. */
    public int minX() {
        return minSectionX() << 4;
    }

    public int minY() {
        return minSectionY() << 4;
    }

    public int minZ() {
        return minSectionZ() << 4;
    }

    /** Marks block {@code x, y, z} solid or not; the caller keeps it inside a held section. */
    public void set(int x, int y, int z, boolean solid) {
        atlas[texelY(y, z) * ATLAS + texelX(x, y)] = solid ? SOLID : 0;
    }

    /**
     * Gives block {@code x, y, z} the grid byte {@code value} of its shape ({@link DynamicLightBlockShapes#byteOf} or
     * {@link #endShape}); the caller keeps it inside a held section.
     */
    public void setShape(int x, int y, int z, int value) {
        atlas[texelY(y, z) * ATLAS + texelX(x, y)] = (byte) value;
    }

    /** The shapes the grid's bytes name: the version module adds a block state's boxes here. */
    public DynamicLightBlockShapes shapes() {
        return shapes;
    }

    /**
     * The grid byte of the boxes added to {@link #shapes} since its {@code begin}, remembered for block state
     * {@code key}.  When the shapes' ids run out, the table is dropped and the grid emptied, and every held section is
     * read again.
     */
    public int endShape(Object key) {
        int value = shapes.end(key);
        if (value == DynamicLightBlockShapes.FULL) {
            shapes.reset();
            Arrays.fill(atlas, (byte) 0);
            for (int slot = 0; slot < SLOTS; slot++) {
                if (held[slot] != NONE) {
                    dirty[slot] = true;
                    changed[slot] = true;
                }
            }
            value = shapes.end(key);
        }
        return value;
    }

    /** Marks every block of section {@code sx, sy, sz} solid or not. */
    public void fillSection(int sx, int sy, int sz, boolean solid) {
        byte v = solid ? SOLID : 0;
        for (int y = sy << 4, ye = y + 16; y < ye; y++) {
            for (int z = sz << 4, ze = z + 16; z < ze; z++) {
                int row = texelY(y, z) * ATLAS + texelX(sx << 4, y);
                Arrays.fill(atlas, row, row + 16, v);
            }
        }
    }

    /** Whether block {@code x, y, z} is held and a full block; outside the window nothing is. */
    public boolean solid(int x, int y, int z) {
        int rx = x - minX();
        int ry = y - minY();
        int rz = z - minZ();
        if (rx < 0 || ry < 0 || rz < 0 || rx >= SIZE_XZ || ry >= SIZE_Y || rz >= SIZE_XZ) {
            return false;
        }
        return atlas[texelY(y, z) * ATLAS + texelX(x, y)] == SOLID;
    }

    /**
     * Whether a full block stands between {@code from} and the lamp at {@code to}: a grid walk from {@code from}'s
     * block (not tested) that stops {@code clearance} short of the lamp. light_pass.fsh's {@code blocked} is the same
     * walk, there also stopped by the blocks' shapes.
     */
    public boolean blocked(double fx, double fy, double fz, double tx, double ty, double tz, double clearance) {
        double dx = tx - fx;
        double dy = ty - fy;
        double dz = tz - fz;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double reach = length - clearance;
        if (!(reach > 0)) {
            return false;
        }
        dx /= length;
        dy /= length;
        dz /= length;
        int cx = (int) Math.floor(fx);
        int cy = (int) Math.floor(fy);
        int cz = (int) Math.floor(fz);
        int sx = dx > 0 ? 1 : dx < 0 ? -1 : 0;
        int sy = dy > 0 ? 1 : dy < 0 ? -1 : 0;
        int sz = dz > 0 ? 1 : dz < 0 ? -1 : 0;
        double ix = sx == 0 ? 0 : 1.0 / Math.abs(dx);
        double iy = sy == 0 ? 0 : 1.0 / Math.abs(dy);
        double iz = sz == 0 ? 0 : 1.0 / Math.abs(dz);
        double nx = sx == 0 ? Double.MAX_VALUE : (sx > 0 ? cx + 1 - fx : fx - cx) * ix;
        double ny = sy == 0 ? Double.MAX_VALUE : (sy > 0 ? cy + 1 - fy : fy - cy) * iy;
        double nz = sz == 0 ? Double.MAX_VALUE : (sz > 0 ? cz + 1 - fz : fz - cz) * iz;
        while (true) {
            double t = Math.min(nx, Math.min(ny, nz));
            if (t > reach) {
                return false;
            }
            if (nx <= t) {
                cx += sx;
                nx += ix;
            } else if (ny <= t) {
                cy += sy;
                ny += iy;
            } else {
                cz += sz;
                nz += iz;
            }
            if (solid(cx, cy, cz)) {
                return true;
            }
        }
    }

    /** The atlas, row-major, {@link #ATLAS} wide: what the adapter uploads. */
    public byte[] atlas() {
        return atlas;
    }

    /** The atlas column of block {@code x, y}. */
    public static int texelX(int x, int y) {
        return ((y & (SIZE_Y - 1)) & 7) * SIZE_XZ + (x & (SIZE_XZ - 1));
    }

    /** The atlas row of block {@code y, z}. */
    public static int texelY(int y, int z) {
        return ((y & (SIZE_Y - 1)) >> 3) * SIZE_XZ + (z & (SIZE_XZ - 1));
    }

    private void markSection(int sx, int sy, int sz) {
        int slot = slot(sx, sy, sz);
        if (held[slot] == key(sx, sy, sz)) {
            dirty[slot] = true;
        }
    }

    /** Whether {@code block} lies within {@link #HYSTERESIS} blocks of {@code section}. */
    private static boolean near(int block, int section) {
        int low = section << 4;
        return block >= low - HYSTERESIS && block < low + 16 + HYSTERESIS;
    }

    private int minSectionX() {
        return centreX - SECTIONS_XZ / 2;
    }

    private int minSectionY() {
        return centreY - SECTIONS_Y / 2;
    }

    private int minSectionZ() {
        return centreZ - SECTIONS_XZ / 2;
    }

    private static int slot(int sx, int sy, int sz) {
        return ((sx & (SECTIONS_XZ - 1)) * SECTIONS_Y + (sy & (SECTIONS_Y - 1))) * SECTIONS_XZ + (sz & (SECTIONS_XZ - 1));
    }

    private static long key(int sx, int sy, int sz) {
        return ((long) (sx & 0x3FFFFF) << 42) | ((long) (sy & 0xFFFFF) << 22) | (sz & 0x3FFFFF);
    }

    private static int sectionX(long key) {
        return (int) (key << 0 >> 42);
    }

    private static int sectionY(long key) {
        return (int) (key << 22 >> 44);
    }

    private static int sectionZ(long key) {
        return (int) (key << 42 >> 42);
    }
}
