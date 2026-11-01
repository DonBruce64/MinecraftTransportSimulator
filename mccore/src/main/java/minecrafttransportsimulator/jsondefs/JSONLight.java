package minecrafttransportsimulator.jsondefs;

import java.util.List;


import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONLight {
    @JSONRequired
    @JSONDescription("The name of the object in the model this light definition will act on.")
    public String objectName;

    @JSONDescription("If true, this light will be emissive and render a solid-color of light when on.  If false, only the texture will light up.  Does not affect rendering of blendableComponents.")
    public boolean emissive;

    @JSONDescription("If true, this light will have a glass cover rendered over it.  This cover will light-up with the light, so keep this in mind.")
    public boolean covered;

    @JSONDescription("If true, this light will be considered a beam and will do beam-blending.  Useful for creating your own custom beam shapes.")
    public boolean isBeam;

    @JSONDescription("Causes the light to automatically dim relative to the electric power of the thing it is on. Should normally be true to prevent vehicles from having lights with dead batteries.")
    public boolean isElectric;

    @JSONRequired(dependentField = "emissive", dependentValues = {"true"})
    @JSONDescription("A hexadecimal color code.  This tells MTS what color this light should be.  Required for emissive lights and lights with blendableComponents.")
    public ColorRGB color;

    @JSONDescription(" A listing of animations for determining the light brightness (and potentially color).  Leaving this blank or not having any active translation transforms will make for a light that is always on at 100% brightness, which you probably don't want.  Visibility transforms will turn the light on or off.  Translation transforms using the using the y-axis will add the variable value to the light brightness.  Translation transforms with the x-axis will multiply the light by the current light value.  Translation transforms with the z-axis will set the light brightness to that value, overriding any prior transform operations.  Rotation transforms will set the color with the RGB value corresponding to the XYZ parameters, overriding the color paramter.  Note that for all cases, the light brightness calculation starts at 0, so a set of animations that only multiply will just result in multiplying by 0 and a light that doesn't show up.  Not having any translation transforms will make the light 100% bright.  Inhibitor and activator transforms may be used in conjunction with these for advanced brightness logic.")
    public List<JSONAnimationDefinition> brightnessAnimations;

    @JSONDescription("A listing of blendable components for this light.  Used to allow for multiple flares or beams for a single light object.")
    public List<JSONLightBlendableComponent> blendableComponents;

    public static class JSONLightBlendableComponent {
        /** {@link #beamSoftness} when 0 or not set: the headlights' edge. */
        public static final float DEFAULT_SOFTNESS = 0.4F;

        @JSONRequired
        @JSONDescription("The position at which the blendable component will be rendered at.  For a dynamic light this is also where the light comes from.")
        public Point3D pos;

        @JSONRequired
        @JSONDescription("The axis that defines the 'front' of the blendable component.  This will be the 'normal' for the flare texture, or the direction of the beam.")
        public Point3D axis;

        @JSONDescription("The height of the flare to render.  On a dynamic light, a flare width or height above 0 draws a glowing corona at the light instead of the flare texture.")
        public float flareHeight;

        @JSONDescription("The width of the flare to render.  On a dynamic light, a flare width or height above 0 draws a glowing corona at the light instead of the flare texture.")
        public float flareWidth;

        @JSONDescription("The diameter of the beam to render.  On a dynamic light this is the diameter of the lit spot at beamLength, so the cone angle is 2*atan(beamDiameter/2/beamLength).  0 on a dynamic light makes a point light that lights in all directions.")
        public float beamDiameter;

        @JSONDescription("The length of the beam to render.  On a dynamic light this is how far the light reaches: nothing past this distance is lit.  The visible beam in the air is 0.45x this long, at most 24 blocks, unless lightBeamLength is set.")
        public float beamLength;

        @JSONDescription("If true, this component lights the world itself instead of drawing the regular beam: blocks, entities and vehicles in its cone are lit, cast shadows, and a volumetric beam and corona are drawn.  Falls back to the regular beam and flare if dynamic lights are turned off in the client config.  Under a shader pack, the light raises block light and draws a simple cone and flare instead.  Only components with this set (or with a lightPreset) are dynamic lights: mark the real lamps (headlights, tail, brake, reverse and turn signal lamps, landing and taxi lights), not cockpit or indicator LEDs.")
        public boolean isDynamicLight;

        @JSONDescription("A lamp preset that makes this component a dynamic light: 'headlight_low' (dipped beam: intensity 1.4, reach 28 blocks, 25 degree cone, softness 0.4, corona radius 1), 'headlight_high' (main beam: intensity 1.8, reach 60, 25 degrees, softness 0.4, corona 1.1), 'brake_light' (stop, tail, turn signal and marker lamps: intensity 1, reach 4, 150 degrees, softness 0.5, corona 0.7) or 'reverse_light' (intensity 0.8, reach 6, 120 degrees, softness 0.4, corona 0.8).  Headlight presets rank first when there are more lights than the client allows.  It is data only: switch it with the light's brightnessAnimations, for example on a pack's own high beam variable.  beamDiameter with beamLength, beamLength, beamSoftness, beamIntensity and the flare size override the preset's cone, reach, softness, intensity and corona.")
        public String lightPreset;

        @JSONDescription("A hexadecimal color code for this component only.  Overrides the color of the light and its color animations.  Works on regular and dynamic components.")
        public ColorRGB color;

        @JSONDescription("How soft the edge of a dynamic light's cone is, from 0 (hard edge) to 1 (fades from the center).  The outer share of the cone's half-angle given here fades out smoothly.  0 or unset means 0.4; for a hard edge use a small value such as 0.01.")
        public float beamSoftness;

        @JSONDescription("A brightness multiplier for this component.  0 or unset means 1.  On a dynamic light this is the light's intensity: 1.4 is a dipped headlight, 1.8 a main beam, 2 to 4 a stronger beam and corona.  On a lightPreset it replaces the preset's intensity.  On a dynamic light it does not change the regular flare and beam drawn with dynamic lights off.  Works on regular and dynamic components.")
        public float beamIntensity;

        @JSONDescription("A dynamic light's full cone angle in degrees, instead of the one from beamDiameter and beamLength.  Lets the regular beam drawn with dynamic lights off keep its own size.  0 or unset uses beamDiameter and beamLength, else the preset's.")
        public float lightAngle;

        @JSONDescription("How far a dynamic light reaches in blocks, instead of beamLength.  Lets the regular beam drawn with dynamic lights off keep its own length.  0 or unset uses beamLength, else the preset's.")
        public float lightReach;

        @JSONDescription("How prominent a dynamic light's bloom is: the glow around the light itself (its corona), not the beam.  0 to 999.  0 or unset means 1; a negative value draws no bloom.  Higher values make the glow larger and brighter, and let it keep growing with distance for longer: past 24 x sqrt(bloomIntensity) blocks it stops growing and shrinks with distance as the lamp does.  The bloom needs a flareWidth or flareHeight above 0 (or a lightPreset).")
        public float bloomIntensity;

        @JSONDescription("How long a dynamic light's visible beam in the air is, as a share of its lightReach, 0 to 1: 1 shows the cone from the light to the end of its reach.  A negative value shows no beam in the air.  Only changes what is seen in the air, not what the light lights.  0 or unset means 0.45 of the reach, at most 24 blocks.  Useful for strong spotlights whose beam should be seen far.")
        public float lightBeamLength;

        @JSONDescription("How far from a dynamic light, in blocks, its shadows start: blocks and models (the vehicle's own included) closer to the light than this cast no shadow for it.  Lets a lamp sit behind model geometry (a lens, a grille, a housing) that is there for looks and still shine.  0 or unset keeps only the built-in 0.35 blocks of clearance; a smaller value than that has no effect.")
        public float shadowStartDistance;

        @JSONDescription("Brightness animations for this component only, with the same rules as the light's own brightnessAnimations (translation x multiplies, y adds, z sets; rotation sets the color; visibility turns it on or off).  The result is multiplied with the light's brightness, so this can make one component blink while the others stay on.  Works on regular and dynamic components.")
        public List<JSONAnimationDefinition> brightnessAnimations;

        @JSONDescription("If true, this flare or beam is hidden while dynamic lights are on or a shader pack is active, and shown otherwise.  For stand-in flares and beams meant only for players who cannot run dynamic lights and turn them off.")
        public boolean hideWhenDynLightActive;

        /**
         * Returns the brightness multiplier of this component: {@link #beamIntensity}, with 0 meaning 1.
         */
        public float getIntensity() {
            return beamIntensity == 0 ? 1 : beamIntensity;
        }

        /** Returns {@link #bloomIntensity} clamped to 999, 1 if 0 or not set, 0 (no bloom) if negative. */
        public float getBloom() {
            return bloomIntensity == 0 ? 1 : Math.max(0F, Math.min(999F, bloomIntensity));
        }

        /** Returns {@link #shadowStartDistance}, at least 0. */
        public float getShadowStart() {
            return Math.max(0F, shadowStartDistance);
        }

        /** Returns {@link #lightBeamLength} clamped to 0..1, or -1 if 0 or not set (the default length).  A negative value gives 0 (no beam). */
        public float getBeamShare() {
            return lightBeamLength == 0 ? -1 : Math.max(0F, Math.min(1F, lightBeamLength));
        }

        /** Returns true if this component is a dynamic light: {@link #isDynamicLight}, or a {@link #lightPreset}. */
        public boolean isDynamic() {
            return isDynamicLight || lightPreset != null;
        }

        /** Returns {@link #beamSoftness} clamped to 0..1, {@link #DEFAULT_SOFTNESS} if 0 or not set. */
        public float getSoftness() {
            return beamSoftness == 0 ? DEFAULT_SOFTNESS : Math.max(0F, Math.min(1F, beamSoftness));
        }

        /**
         * Returns true if this component needs its own renderable because its color or brightness
         * can differ from the light's other components, because it is a dynamic light, or because it can be hidden
         * on its own ({@link #hideWhenDynLightActive}).
         */
        public boolean hasOwnLighting() {
            return isDynamic() || color != null || brightnessAnimations != null || getIntensity() != 1 || hideWhenDynLightActive;
        }
    }
}
