package minecrafttransportsimulator.rendering.dynamiclight;

import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;

/**
 * Cone math of one dynamic light component.  A component with a
 * {@code beamDiameter} above 0 is a spot whose full cone angle makes a spot of that diameter at
 * {@code beamLength}; one without is a point light.  {@code beamSoftness} is the outer share of
 * the half-angle that fades out.  {@link Preset} holds the lamp presets a {@code lightPreset} names.
 */
public final class DynamicLightAim {
    /** Budget rank of headlight presets against other lamps: the headlights win over a tail lamp nearer the camera. */
    public static final float HEADLIGHT_PRIORITY = 8F;
    /**
     * Built-in clearance round a lamp, blocks: blocks and bodies closer than this cast no shadow for it, so a lamp
     * touching a block, or sitting in its own housing, still lights.  A light's own is
     * {@code max(LAMP_CLEARANCE, shadowStartDistance)}.  The GLSL twins are lamp_trace.fsh's and light_pass.fsh's
     * (now a per-light value) and lamp_body.fsh's.
     */
    public static final float LAMP_CLEARANCE = 0.35F;

    /** The lamp presets, by {@code lightPreset} name. */
    public enum Preset {
        HEADLIGHT_LOW("headlight_low", 1.4F, 28F, 25F, 0.4F, 1.0F),
        HEADLIGHT_HIGH("headlight_high", 1.8F, 60F, 25F, 0.4F, 1.1F),
        /**
         * Stop, tail, turn signal and marker lamps.  A wide cone, brighter and longer than a plain brake light: IV
         * cars' lamps sit lower and aim up off the road.
         */
        BRAKE_LIGHT("brake_light", 1.0F, 4F, 150F, 0.5F, 0.7F),
        REVERSE_LIGHT("reverse_light", 0.8F, 6F, 120F, 0.4F, 0.8F);

        public final String id;
        public final float intensity;
        public final float reach;
        public final float angle;
        public final float soft;
        /** Corona radius, blocks (the typhoon30 rows' Flare). */
        public final float corona;

        Preset(String id, float intensity, float reach, float angle, float soft, float corona) {
            this.id = id;
            this.intensity = intensity;
            this.reach = reach;
            this.angle = angle;
            this.soft = soft;
            this.corona = corona;
        }

        /** The {@code lightPreset} named {@code id}, null for none or an unknown name. */
        public static Preset of(String id) {
            for (Preset preset : values()) {
                if (preset.id.equals(id)) {
                    return preset;
                }
            }
            return null;
        }

        /** Whether this is a headlight preset (ranked first by the budget). */
        public boolean isHeadlight() {
            return this == HEADLIGHT_LOW || this == HEADLIGHT_HIGH;
        }
    }

    /**
     * One beam of a lamp: intensity multiplier, full cone angle (0 = point), softness, reach, corona radius (0 none),
     * bloom (the corona's strength, 0 none), the visible beam's share of the reach (-1: the default length) and the
     * lamp's shadow clearance ({@link #LAMP_CLEARANCE} at least).
     */
    public static final class Beam {
        public final float intensity;
        public final float angle;
        public final float soft;
        public final float reach;
        public final float corona;
        public final float bloom;
        public final float beamShare;
        public final float clearance;

        Beam(float intensity, float angle, float soft, float reach, float corona, float bloom, float beamShare, float shadowStart) {
            this.intensity = intensity;
            this.angle = angle;
            this.soft = soft;
            this.reach = reach;
            this.corona = corona;
            this.bloom = bloom;
            this.beamShare = beamShare;
            this.clearance = Math.max(LAMP_CLEARANCE, shadowStart);
        }
    }

    /**
     * An authored dynamic light: its own fields, those it leaves unset taken from {@code preset} (if any).  The cone is
     * lightAngle, else from beamDiameter and beamLength, else the preset's; the reach lightReach, else beamLength, else
     * the preset's; a beamIntensity replaces the preset's intensity, a flare size its corona.  Bloom and the visible
     * beam's length and shadowStartDistance are the component's own.
     */
    public static Beam authored(JSONLightBlendableComponent component, Preset preset) {
        float flare = Math.max(component.flareWidth, component.flareHeight) / 2F;
        if (preset == null) {
            return new Beam(component.getIntensity(), component.lightAngle > 0 ? component.lightAngle : angle(component), component.getSoftness(), component.lightReach > 0 ? component.lightReach : Math.max(0F, component.beamLength), flare, component.getBloom(), component.getBeamShare(), component.getShadowStart());
        }
        boolean cone = component.beamDiameter > 0 && component.beamLength > 0;
        boolean aimed = component.axis != null && component.axis.length() > 1e-6;
        float angle = component.lightAngle > 0 ? component.lightAngle : (cone ? angle(component) : (aimed ? preset.angle : 0));
        float reach = component.lightReach > 0 ? component.lightReach : (component.beamLength > 0 ? component.beamLength : preset.reach);
        return new Beam(component.beamIntensity != 0 ? component.beamIntensity : preset.intensity, angle, component.beamSoftness != 0 ? component.getSoftness() : preset.soft, reach, flare > 0 ? flare : preset.corona, component.getBloom(), component.getBeamShare(), component.getShadowStart());
    }

    /** How much wider than the beam's half angle the corona stays visible. */
    private static final double FLARE_WIDEN = 3.0;
    /** The corona's least visibility off the beam. */
    private static final float FLARE_FLOOR = 0.45F;

    private DynamicLightAim() {
    }

    /** Full cone angle of a component in degrees, 0 for a point light. */
    public static float angle(JSONLightBlendableComponent component) {
        if (component.beamDiameter <= 0 || component.beamLength <= 0) {
            return 0;
        }
        return (float) Math.toDegrees(2 * Math.atan(component.beamDiameter / 2D / component.beamLength));
    }

    /** Cosine of the half-angle: the edge of the beam. */
    public static float cosOuter(float angle) {
        return (float) Math.cos(Math.toRadians(angle * 0.5));
    }

    /** Cosine where the fade starts, kept just inside {@link #cosOuter(float)}. */
    public static float cosInner(float angle, float soft) {
        float inner = (float) Math.cos(Math.toRadians(angle * 0.5 * (1.0 - soft)));
        return Math.max(inner, cosOuter(angle) + 1e-4F);
    }

    /** Hermite smoothstep of {@code cosAngle} from {@code cosOuter} to {@code cosInner}; light_pass.fsh's twin. */
    public static float cone(float cosOuter, float cosInner, float cosAngle) {
        float t = Math.max(0F, Math.min(1F, (cosAngle - cosOuter) / (cosInner - cosOuter)));
        return t * t * (3F - 2F * t);
    }

    /**
     * How much of the corona a viewer sees, 0..1: a point light whole; a cone by {@link #cone} over its own
     * cone widened 3x on the half angle, at least {@link #FLARE_FLOOR}.
     *
     * @param cosToViewer cosine between the beam and the direction from the lamp to the viewer
     */
    public static float flareVisibility(float angle, float soft, float cosToViewer) {
        if (angle <= 0) {
            return 1F;
        }
        double half = Math.min(Math.PI, Math.toRadians(angle * 0.5) * FLARE_WIDEN);
        float cosOuter = (float) Math.cos(half);
        float cosInner = Math.max((float) Math.cos(half * (1.0 - soft)), cosOuter + 1e-4F);
        return Math.max(FLARE_FLOOR, cone(cosOuter, cosInner, cosToViewer));
    }

    /** Distance at which a corona of bloom 1 stops growing in the world, blocks; it scales with sqrt(bloom). */
    private static final float BLOOM_GROWTH = 24F;

    /**
     * How far a corona of {@code bloom} keeps its size on screen, blocks: beyond it, it keeps its size in the world
     * instead, shrinking with distance as the lamp does.  A stronger bloom stays large for longer.
     */
    public static float bloomGrowth(float bloom) {
        return BLOOM_GROWTH * (float) Math.sqrt(Math.max(bloom, 0F));
    }

    /** The visible beam's tan of half its angle: a quarter into the soft edge, the lit cone as the eye sees it. */
    public static float beamTan(float angle, float soft) {
        return (float) Math.tan(Math.toRadians(angle * 0.5 * (1.0 - 0.25 * soft)));
    }
}
