package minecrafttransportsimulator.rendering.dynamiclight;

import minecrafttransportsimulator.baseclasses.ColorRGB;

/**
 * One light {@link DynamicLightSystem} draws this frame, whatever made it: a model lamp
 * ({@link DynamicLightOwner.Lamp}) or a group of particle lights ({@link DynamicLightSystem#submitLight}).
 * Holds its cone, reach and corona, this frame's place, brightness and color, and what the shader pack fallback
 * shows.  Render thread only.
 */
public class DynamicLightSource {
    /** Stable identity across frames (shadow tiles, block light fallback). */
    public long key;
    public final DynamicLightEmitter emitter = new DynamicLightEmitter();
    /** Its budget rank against other lights ({@link DynamicLightAim#HEADLIGHT_PRIORITY} for headlight presets). */
    public float priority = 1F;

    /** Full cone angle (0 for a point light), edge softness, reach and corona radius (0 none). */
    public float angle;
    public float soft;
    public float reach;
    public float coronaRadius;
    /** The corona's strength (bloomIntensity, 0 none) and the visible beam's share of the reach (-1: the default). */
    public float bloom = 1F;
    public float beamShare = -1F;
    /** Blocks round the lamp that cast no shadow for it ({@link DynamicLightAim#LAMP_CLEARANCE} at least). */
    public float clearance = DynamicLightAim.LAMP_CLEARANCE;

    /** Brightness and color this frame. */
    public float brightness;
    public final ColorRGB color = new ColorRGB();

    /** World place and unit direction this frame. */
    public double x;
    public double y;
    public double z;
    public float dx;
    public float dy;
    public float dz;

    /** Shader pack fallback: this frame's visible beam cone, if any. */
    public boolean showsCone;
    public float coneLength;
    public float coneTan;
    public float coneDensity;
    /** Shader pack fallback: this frame's camera-facing flare, if any. */
    public boolean showsFlare;
    public float flareSize;
    public float flareStrength;

    /** Takes the cone, reach, corona, bloom, visible length, shadow clearance and intensity-free values of {@code beam}. */
    void setBeam(DynamicLightAim.Beam beam) {
        angle = beam.angle;
        soft = beam.soft;
        reach = beam.reach;
        coronaRadius = beam.corona;
        bloom = beam.bloom;
        beamShare = beam.beamShare;
        clearance = beam.clearance;
    }
}
