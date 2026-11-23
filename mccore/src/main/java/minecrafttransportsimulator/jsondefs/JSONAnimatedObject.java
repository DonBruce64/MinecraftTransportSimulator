package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONAnimatedObject {
    @JSONRequired
    @JSONDescription("The name of the object in the model this animation definition will act on.")
    public String objectName;

    @JSONDescription("If set, this object's animations will be applied directly after the listed object.  This allows for complex animations, and saves on duplicate JSON where you want to 'attach' one object to another and have them move together.")
    public String applyAfter;

    @JSONDescription("If set, this object will do blending with visibility animations rather than being invisible or visible.  Blending clamps such that visibility below min is invisible, and visibility above max is visibile.")
    public boolean blendedAnimations;

    @JSONDescription("A listing of animation objects.  This defines the animations to be applied to the object spefieid in objectName.  If you have none, chances are you need to combine something in your model!")
    public List<JSONAnimationDefinition> animations;

    @JSONDescription("Optional outline for this model object.  Its activation animations are independent of the model object's animations.")
    public JSONOutline outline;

    public static class JSONOutline {
        @JSONDescription("Outward expansion in model units.  Defaults to 0, which disables the outline.")
        public float outlineScale;

        @JSONDescription("Hexadecimal outline color.  Defaults to white (ffffff).")
        public ColorRGB outlineColor = ColorRGB.WHITE;

        @JSONDescription("Maximum opacity of the outline, from 0 (hidden) to 1 (fully opaque).  Defaults to 1.")
        public double opacity = 1.0;

        @JSONDescription("Fade-in time in ticks.  Defaults to 0 for an instant transition.")
        public int fadeInTime;

        @JSONDescription("Fade-out time in ticks.  Defaults to 0 for an instant transition.")
        public int fadeOutTime;

        @JSONDescription("Animations that control outline activation, typically visibility checks.  If omitted or empty, the outline is always active.")
        public List<JSONAnimationDefinition> activeAnimations;
    }
}
