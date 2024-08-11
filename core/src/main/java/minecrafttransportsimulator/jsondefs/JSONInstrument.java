package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("Instruments are most likely something you won't be messing with unless you really don't like the stock options available in the Official Content Pack.  Should you want to make your own, pay close attention to this section.  The instrument system is powerful, yet somewhat complex, making it easy to slip up in the JSON file and end up with a tachometer that looks like something clobbered together with spare parts.\nBefore you start, it's important to know the texture sheet for instruments is located in the texture folder, but NOT in a sub-folder like other pack components.  Additionally, the texture must be 1024x1024, otherwise rendering will be odd.  You may orient your instruments in this file however you like, so you shouldn't run out of space.\nIf you hadn't noticed already, the MTS instrument system works by taking a bunch of textures and layering them on top of one another to make instruments.  This is a lightweight way of rendering, and since they are textures they work with shaders straight out of the box.  MTS iterates through all the component sections when rendering instruments, so however you set them is how the instrument will render.\nThe JSON structure itself consists of two parts.  The first being the 'general' section, which is standard.  The second section is a list of Components that make up the instrument.")
public class JSONInstrument extends AJSONItem {

    @JSONRequired
    @JSONDescription("Each instrument component represents a single rendering of a texture from the master instrument PNG file.  These components may be modified in multiple ways.  The standard way is for them to be rotated, translated, made invisible, have their lighting changed, or change what sections of the texture sheet they are pulling their texture from.")
    public List<JSONInstrumentComponent> components = new ArrayList<>();

    @JSONRequired
    @JSONDescription("The texture sheet to pull this instrument from.  Defaults to instruments.png if not set.  You may use sub-folders here if you wish.")
    public String textureName;

    public static class JSONInstrumentComponent {
        @JSONDescription("The center position to render this instrument component.  By default components will be centered at the center of the instrument, but you can specify this parameter to have the component render to the left or right of the center point.")
        public int xCenter;

        @JSONDescription("Same as xCenter, but for the y coordinate.")
        public int yCenter;

        @JSONDescription("The optional scale to render this component at.  If not included, then the scale will be 1.0.")
        public float scale;

        @JSONRequired
        @JSONDescription("This is the center of the texture location of the X axis on the texture sheet.  Units are in pixels.")
        public int textureXCenter;

        @JSONRequired
        @JSONDescription("This is the center of the texture location of the Y axis on the texture sheet.  Units are in pixels.")
        public int textureYCenter;

        @JSONRequired
        @JSONDescription("The width, in pixels, of the texture section to render.")
        public int textureWidth;

        @JSONRequired
        @JSONDescription("The height, in pixels, of the texture section to render.")
        public int textureHeight;

        @JSONDescription("An optional text object.  If this textObject is set, then rendering off the instrument texture will not be performed.  Instead, text will be rendered using the parameters from the textObject.  While most parameters apply, there are some caveats:\nThe variable for the text value is fieldName.  As in, the name of the field will be passed to the animation system for the animation value.\nScale is such that 1px text == 1px of the instrument texture.\nmaxLength is not used for entering the value.  Rather, it adds leading 0s.  You may display a longer value than maxLength should you wish to do so.\nThe value displayed is rounded to the nearest whole number.")
        public JSONText textObject;

        @JSONRequired(dependentField = "textObject")
        @JSONDescription("The factor to multiply the the returned value of the textObject's fieldName before displaying it.  Has no effect if no textObject is given.")
        public float textFactor;

        @JSONDescription("Normally MTS will get the texture based on the width and height specified and rotate that around the center point.  However, there may be a case, such as an instrument without a bezel, where you want the region of texture you're grabbing from the texture sheet to rotate, and the rendering of that texture to stay fixed.  If that is the case, set this value to true.  Note that if an animation has a non-zero center point, then the window will be rotated about that center point, not 0, 0.  This allows for a window that renders the outside of a dial, or other complex rotations.")
        public boolean rotateWindow;

        @JSONDescription("Normally MTS tries to move the region of grabbed texture for translation.  If instead you want to change how much texture is grabbed, set this to true.  In this mode, MTS will move the upper bound of grabbed texture up by the translationVariable amount rather than offset the grabbed region.")
        public boolean extendWindow;

        @JSONDescription("Normally MTS tries to move the region of grabbed texture for translation.  If instead you want to move the location of where the texture is rendered, you can set this to be true.  This will lock the rendered window section in-place, and instead apply translations to the actual texture rendered position.")
        public boolean moveComponent;

        @JSONDescription("If this is true, MTS will make this texture bright when the lights on the vehicle are on.  This can be combined with overlays to make faux-instrument lighting.")
        public boolean lightUpTexture;

        @JSONDescription("If this is true, MTS will render the texture of this section as an overlay.  This does blending on what is rendered below it, though it does not make much of a difference in lighting.  If you want to make a part of an instrument bright, a lightUpTexture is probably the better option.  This overlay is mainly for adding things like instrument glass effects or lighting hues.  Do note that both of these can be combined, however, to give a lighting area of effect for things like dash lights.")
        public boolean overlayTexture;

        @JSONDescription("A list of animatedObjects that can be used to move this instrument based on the animation values.  However, since these are more for OBJ models than instruments, there are a few caveats:\naddPriorOffset has no function with instrument animations.  This is because that relies of vector-based clamping, which instrument animations do not support.\nSince instruments are 2D, only the z-axis is supported for rotation.  Similarly, only x and y translation is possible.\nInstrument Y-axis aligns with the Y-axis of texture sheets.  Therefore, the +Y direction is down, and 0,0 is top-left.  This means that a +Y translation on an instrument will actually move the instrument in a -Y direction if you are viewing the instrument on a model.")
        public List<JSONAnimationDefinition> animations;

        @Deprecated
        public boolean lightOverlay;

        @Deprecated
        public String rotationVariable;
        @Deprecated
        public float rotationOffset;
        @Deprecated
        public float rotationFactor;
        @Deprecated
        public float rotationClampMin;
        @Deprecated
        public float rotationClampMax;
        @Deprecated
        public boolean rotationAbsoluteValue;

        @Deprecated
        public String translationVariable;
        @Deprecated
        public boolean translateHorizontal;
        @Deprecated
        public float translationFactor;
        @Deprecated
        public float translationClampMin;
        @Deprecated
        public float translationClampMax;
        @Deprecated
        public boolean translationAbsoluteValue;
    }
}
