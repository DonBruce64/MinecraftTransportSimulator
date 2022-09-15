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
}
