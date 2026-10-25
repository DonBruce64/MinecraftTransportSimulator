package minecrafttransportsimulator.rendering.dynamiclight;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The shapes of the blocks in the shadow grid that are not full cubes (slabs, stairs, fences, bells, banners,
 * candles...): each a few boxes in block space, so their shadows take their shape.  The grid stores a block's shape as
 * its byte ({@link DynamicLightVoxelWindow#setShape}: 0 nothing, {@link DynamicLightVoxelWindow#SOLID} a full block,
 * else an id here); voxel.glsl reads the boxes from the table this keeps ({@link #data}, a row an id).  The version
 * module adds a block's boxes once per block state ({@link #begin}, {@link #box}, {@link DynamicLightVoxelWindow#endShape});
 * the same boxes share one id.  When the ids run out, the window drops the table and reads every section again.
 * Render thread only.
 */
public final class DynamicLightBlockShapes {
    /** Boxes kept per shape, the largest: voxel.glsl's SHAPE_BOXES. */
    public static final int MAX_BOXES = 8;
    /** RGBA texels a row: a box's least corner, then its greatest; the first texel's w is the row's box count. */
    public static final int TEXELS = MAX_BOXES * 2;
    /** Rows of the table: one per grid byte. */
    public static final int ROWS = 256;
    /** The last id: 0 and 255 mean nothing and a full block in the grid. */
    private static final int LAST_ID = 254;
    /** Returned by {@link #end} when the table is full. */
    static final int FULL = -1;

    private final float[] data = new float[ROWS * TEXELS * 4];
    private final Map<Object, Integer> byKey = new IdentityHashMap<>();
    private final Map<String, Integer> byShape = new HashMap<>();
    private double[] boxes = new double[16 * 6];
    private int boxCount;
    private int nextId = 1;
    private int version;

    /** The grid byte of block state {@code key}'s shape, or -1 when its boxes were not added yet. */
    public int byteOf(Object key) {
        Integer id = byKey.get(key);
        return id == null ? -1 : id;
    }

    /** Starts the boxes of a block's shape. */
    public void begin() {
        boxCount = 0;
    }

    /** One box of the shape, in block space; clipped to the block, an empty box is left out. */
    public void box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        minZ = Math.max(0, minZ);
        maxX = Math.min(1, maxX);
        maxY = Math.min(1, maxY);
        maxZ = Math.min(1, maxZ);
        if (!(maxX > minX && maxY > minY && maxZ > minZ)) {
            return;
        }
        int o = boxCount * 6;
        if (o + 6 > boxes.length) {
            boxes = Arrays.copyOf(boxes, boxes.length * 2);
        }
        boxes[o] = minX;
        boxes[o + 1] = minY;
        boxes[o + 2] = minZ;
        boxes[o + 3] = maxX;
        boxes[o + 4] = maxY;
        boxes[o + 5] = maxZ;
        boxCount++;
    }

    /**
     * The grid byte of the boxes added since {@link #begin}, remembered for {@code key}: 0 for none, the full byte for
     * one box filling the block, else the id of the shape (its {@link #MAX_BOXES} largest boxes), or {@link #FULL}.
     */
    int end(Object key) {
        int value;
        if (boxCount == 0) {
            value = 0;
        } else if (boxCount == 1 && boxes[0] == 0 && boxes[1] == 0 && boxes[2] == 0 && boxes[3] == 1 && boxes[4] == 1 && boxes[5] == 1) {
            value = DynamicLightVoxelWindow.SOLID & 0xFF;
        } else {
            keepLargest();
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < boxCount * 6; i++) {
                name.append(Math.round(boxes[i] * 4096)).append(',');
            }
            String shape = name.toString();
            Integer id = byShape.get(shape);
            if (id == null) {
                if (nextId > LAST_ID) {
                    return FULL;
                }
                id = nextId++;
                byShape.put(shape, id);
                int o = id * TEXELS * 4;
                Arrays.fill(data, o, o + TEXELS * 4, 0F);
                for (int i = 0; i < boxCount; i++) {
                    int b = i * 6;
                    int t = o + i * 8;
                    data[t] = (float) boxes[b];
                    data[t + 1] = (float) boxes[b + 1];
                    data[t + 2] = (float) boxes[b + 2];
                    data[t + 4] = (float) boxes[b + 3];
                    data[t + 5] = (float) boxes[b + 4];
                    data[t + 6] = (float) boxes[b + 5];
                }
                data[o + 3] = boxCount;
                ++version;
            }
            value = id;
        }
        byKey.put(key, value);
        return value;
    }

    /** Forgets every shape: the grid's bytes that named them must be read again. */
    void reset() {
        byKey.clear();
        byShape.clear();
        nextId = 1;
        Arrays.fill(data, 0F);
        ++version;
    }

    /** The table: {@link #ROWS} rows of {@link #TEXELS} RGBA texels, row {@code id} for shape {@code id}. */
    public float[] data() {
        return data;
    }

    /** Changes whenever the table does, so the renderer uploads it. */
    public int version() {
        return version;
    }

    /** Leaves the {@link #MAX_BOXES} largest boxes: a small one casts the least shadow. */
    private void keepLargest() {
        while (boxCount > MAX_BOXES) {
            int smallest = 0;
            for (int i = 1; i < boxCount; i++) {
                if (volume(i) < volume(smallest)) {
                    smallest = i;
                }
            }
            System.arraycopy(boxes, (boxCount - 1) * 6, boxes, smallest * 6, 6);
            boxCount--;
        }
    }

    private double volume(int i) {
        int o = i * 6;
        return (boxes[o + 3] - boxes[o]) * (boxes[o + 4] - boxes[o + 1]) * (boxes[o + 5] - boxes[o + 2]);
    }
}
