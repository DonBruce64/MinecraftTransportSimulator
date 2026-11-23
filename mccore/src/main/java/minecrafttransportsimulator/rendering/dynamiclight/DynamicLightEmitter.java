package minecrafttransportsimulator.rendering.dynamiclight;

/**
 * One lit lamp this frame: a point or a spot at a world position, with its color, intensity and reach.
 * An emitter (shape plus light) as one mutable object, so a frame reuses its emitters
 * instead of allocating them.  Render thread only.
 */
public final class DynamicLightEmitter {
    /** Stable identity of the lamp across frames: owner id << 16 | component index. */
    public long key;
    public double x;
    public double y;
    public double z;
    /** Unit beam direction of a spot. */
    public float dx;
    public float dy;
    public float dz;
    public boolean spot;
    /** Full cone angle in degrees and its edge softness, for a spot. */
    public float angle;
    public float soft;
    public float cosOuter;
    public float cosInner;
    /** Color, 0..1 per channel. */
    public float red;
    public float green;
    public float blue;
    /**
     * The beam seen in the air this light shows when the budget keeps it (a spot in the light pass): its length
     * (0 none), the tan its cone widens by and its density.
     */
    public float beamLength;
    public float beamTan;
    public float beamDensity;
    /** Brightness scale; in the light pass already dimmed by daylight at the lamp, under a shader pack not. */
    public float intensity;
    /** Reach in blocks: beyond this the light contributes nothing. */
    public float radius;
    /** Rank multiplier in the budget: headlight presets win over signal lamps nearer the camera. */
    public float priority = 1F;
    /**
     * Blocks round the lamp where nothing casts a shadow for it: at least {@link DynamicLightAim#LAMP_CLEARANCE}, more
     * for a lamp given a shadowStartDistance.  Merged point lights take the largest.
     */
    public float clearance = DynamicLightAim.LAMP_CLEARANCE;

    /** Copies every value of {@code other} into this emitter. */
    public DynamicLightEmitter set(DynamicLightEmitter other) {
        key = other.key;
        x = other.x;
        y = other.y;
        z = other.z;
        dx = other.dx;
        dy = other.dy;
        dz = other.dz;
        spot = other.spot;
        angle = other.angle;
        soft = other.soft;
        cosOuter = other.cosOuter;
        cosInner = other.cosInner;
        red = other.red;
        green = other.green;
        blue = other.blue;
        intensity = other.intensity;
        priority = other.priority;
        clearance = other.clearance;
        radius = other.radius;
        beamLength = other.beamLength;
        beamTan = other.beamTan;
        beamDensity = other.beamDensity;
        return this;
    }

    /** Score used to rank emitters against the budget: intensity / max(distance^2, 0.01). */
    public float scoreFrom(double viewerX, double viewerY, double viewerZ) {
        double ex = x - viewerX;
        double ey = y - viewerY;
        double ez = z - viewerZ;
        return (float) (priority * intensity / Math.max(ex * ex + ey * ey + ez * ez, 0.01));
    }

    /** Distance from this emitter to a point. */
    public double distanceTo(double px, double py, double pz) {
        double ex = x - px;
        double ey = y - py;
        double ez = z - pz;
        return Math.sqrt(ex * ex + ey * ey + ez * ez);
    }
}
