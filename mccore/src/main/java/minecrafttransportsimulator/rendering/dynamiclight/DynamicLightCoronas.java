package minecrafttransportsimulator.rendering.dynamiclight;

/**
 * This frame's lamp coronas: the round glow of each lit lamp a viewer sees, drawn over the picture
 * where the scene's depth shows the lamp's lens, so a hull in front hides it.  Keeps the strongest, as many as the
 * frame draws lights ({@link #clear}, at most {@link #CAPACITY}).  Render thread only.
 */
public final class DynamicLightCoronas {
    /** Most coronas one frame draws: the rows of the corona shader's data texture. */
    public static final int CAPACITY = DynamicLightFrame.CAPACITY;
    /** RGBA texels per corona in the corona shader's data texture (lamp_corona.fsh's coronaTexel). */
    public static final int TEXELS = 3;
    private static final int STRIDE = 13;

    private final double[] data = new double[CAPACITY * STRIDE];
    private int count;
    private int limit;

    /**
     * A lamp at world {@code x, y, z} of color {@code r, g, b} (0..1) seen at {@code strength} (0..1: its
     * brightness times how much of it faces the viewer) with {@code bloom} (its bloomIntensity), shown where the scene
     * depth does not cover world point {@code testX, testY, testZ} (its lens face), ranked by strength times
     * {@code priority}; {@code dim} (0..1) is the share daylight at the lamp leaves of it.  Beyond the frame's limit,
     * it replaces the lowest ranked when it ranks higher, so particle glows never push out headlight coronas.
     */
    public void add(double x, double y, double z, double testX, double testY, double testZ, float r, float g, float b, float strength, float bloom, float priority, float dim) {
        if (!(strength > 0F) || !(bloom > 0F) || limit == 0) {
            return;
        }
        float rank = strength * priority;
        int slot = count;
        if (count == limit) {
            slot = 0;
            for (int i = 1; i < limit; i++) {
                if (rank(i) < rank(slot)) {
                    slot = i;
                }
            }
            if (rank(slot) >= rank) {
                return;
            }
        } else {
            count++;
        }
        int o = slot * STRIDE;
        data[o] = x;
        data[o + 1] = y;
        data[o + 2] = z;
        data[o + 3] = r;
        data[o + 4] = g;
        data[o + 5] = b;
        data[o + 6] = strength;
        data[o + 7] = rank;
        data[o + 8] = testX;
        data[o + 9] = testY;
        data[o + 10] = testZ;
        data[o + 11] = bloom;
        data[o + 12] = dim;
    }

    private double rank(int i) {
        return data[i * STRIDE + 7];
    }

    public int count() {
        return count;
    }

    public double x(int i) {
        return data[i * STRIDE];
    }

    public double y(int i) {
        return data[i * STRIDE + 1];
    }

    public double z(int i) {
        return data[i * STRIDE + 2];
    }

    /** The world point whose depth decides whether corona {@code i} shows: its lens face. */
    public double testX(int i) {
        return data[i * STRIDE + 8];
    }

    public double testY(int i) {
        return data[i * STRIDE + 9];
    }

    public double testZ(int i) {
        return data[i * STRIDE + 10];
    }

    public float r(int i) {
        return (float) data[i * STRIDE + 3];
    }

    public float g(int i) {
        return (float) data[i * STRIDE + 4];
    }

    public float b(int i) {
        return (float) data[i * STRIDE + 5];
    }

    public float strength(int i) {
        return (float) data[i * STRIDE + 6];
    }

    /** Corona {@code i}'s bloomIntensity: its size and brightness, and how long it grows with distance. */
    public float bloom(int i) {
        return (float) data[i * STRIDE + 11];
    }

    /** The share of corona {@code i} daylight at its lamp leaves, 0..1. */
    public float dim(int i) {
        return (float) data[i * STRIDE + 12];
    }

    /** Empties the list for a frame that keeps at most {@code most} coronas. */
    public void clear(int most) {
        count = 0;
        limit = Math.max(0, Math.min(CAPACITY, most));
    }
}
