package minecrafttransportsimulator.jsondefs;

import java.util.List;

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
}
