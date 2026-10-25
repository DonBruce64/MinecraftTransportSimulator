package minecrafttransportsimulator.rendering.dynamiclight;

/**
 * This frame's lamp beams seen in the air: each a cone from its lens, integrated per pixel by the
 * light pass, which leaves what the lamp's shadow tile hides from it dark.  Keeps the densest, as many as the frame
 * draws lights ({@link #clear}, at most {@link #CAPACITY}).  Render thread only.
 */
public final class DynamicLightBeams {
    /** Most beams one frame draws: the rows of the beam shader's data texture. */
    public static final int CAPACITY = DynamicLightFrame.CAPACITY;
    /** RGBA texels per beam in the beam shader's data texture (lamp_beam.fsh's beamTexel). */
    public static final int TEXELS = 5;
    private static final int STRIDE = 11;

    private final double[] data = new double[CAPACITY * STRIDE];
    private int count;
    private int limit;

    /**
     * A beam from the lens at world {@code x, y, z} along unit {@code dx, dy, dz}, widening by {@code tan} a block,
     * {@code length} long, of color {@code r, g, b} (0..1) in air of {@code density}.  Beyond the frame's limit, it
     * replaces the thinnest when denser.
     */
    public void add(double x, double y, double z, float dx, float dy, float dz, float tan, float length, float r, float g, float b, float density) {
        if (!(density > 0F) || !(length > 0F) || limit == 0) {
            return;
        }
        int slot = count;
        if (count == limit) {
            slot = 0;
            for (int i = 1; i < limit; i++) {
                if (density(i) < density(slot)) {
                    slot = i;
                }
            }
            if (density(slot) >= density) {
                return;
            }
        } else {
            count++;
        }
        int o = slot * STRIDE;
        data[o] = x;
        data[o + 1] = y;
        data[o + 2] = z;
        data[o + 3] = dx;
        data[o + 4] = dy;
        data[o + 5] = dz;
        data[o + 6] = tan;
        data[o + 7] = length;
        data[o + 8] = r * density;
        data[o + 9] = g * density;
        data[o + 10] = b * density;
    }

    public int count() {
        return count;
    }

    /** Value {@code k} of beam {@code i}: x, y, z, dx, dy, dz, tan, length, then the color times the density. */
    public double get(int i, int k) {
        return data[i * STRIDE + k];
    }

    private double density(int i) {
        int o = i * STRIDE;
        return Math.max(data[o + 8], Math.max(data[o + 9], data[o + 10]));
    }

    /** Empties the list for a frame that keeps at most {@code most} beams. */
    public void clear(int most) {
        count = 0;
        limit = Math.max(0, Math.min(CAPACITY, most));
    }
}
