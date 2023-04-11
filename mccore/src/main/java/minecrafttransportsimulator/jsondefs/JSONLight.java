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

    @JSONDescription("A list of conditions for determining if this light is on or not (default is always on).")
    public List<JSONCondition> activeConditions;

    @JSONDescription("A listing of value modifiers for the brightness (default is full brightness, or 1.0).\nBrightness is internally clamped from 0.0 to 1.0; you do not need to worry about clamping here.")
    public List<JSONValueModifier> brightnessValueModifiers;

    @JSONDescription("A listing of value modifiers for the red component of the RGB color of the light (default is 1.0, or full red).\nColor is internally clamped from 0.0 to 1.0; you do not need to worry about clamping here.")
    public List<JSONValueModifier> redColorValueModifiers;

    @JSONDescription("A listing of value modifiers for the red component of the RGB color of the light (default is 1.0, or full red).\nColor is internally clamped from 0.0 to 1.0; you do not need to worry about clamping here.")
    public List<JSONValueModifier> greenColorValueModifiers;

    @JSONDescription("A listing of value modifiers for the red component of the RGB color of the light (default is 1.0, or full red).\nColor is internally clamped from 0.0 to 1.0; you do not need to worry about clamping here.")
    public List<JSONValueModifier> blueColorValueModifiers;

    @JSONDescription("A listing of blendable components for this light.  Used to allow for multiple flares or beams for a single light object.")
    public List<JSONLightBlendableComponent> blendableComponents;

    public static class JSONLightBlendableComponent {
        @JSONDescription("The position at which the blendable component will be rendered at.")
        public Point3D pos;

        @JSONDescription("The axis that defines the 'front' of the blendable component.  This will be the 'normal' for the flare texture, or the direction of the beam.")
        public Point3D axis;

        @JSONDescription("The height of the flare to render.")
        public float flareHeight;

        @JSONDescription("The width of the flare to render.")
        public float flareWidth;

        @JSONDescription("The diameter of the beam to render.")
        public float beamDiameter;

        @JSONDescription("The length of the beam to render.")
        public float beamLength;
    }

    @Deprecated
    public List<JSONAnimationDefinition> brightnessAnimations;
}
