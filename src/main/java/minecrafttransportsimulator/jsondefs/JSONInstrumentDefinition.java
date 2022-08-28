package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONInstrumentDefinition {
    @JSONRequired
    @JSONDescription("An entry of x, y, and z coordinates that define the center of the instrument on the vehicle.")
    public Point3D pos;

    @JSONDescription("An entry of x, y, and z rotations that tell MTS how to rotate this instrument.  By default all instruments face -z, or the rear of the vehicle.  This can be used to change rotation to fit a different spot of the vehicle if desired.")
    public RotationMatrix rot;

    @JSONDescription("The scale of the instrument.  By default instruments are 128x128.")
    public float scale;

    @JSONDescription("The x-coordinate for the center of this instrument on the HUD, in pixels.")
    public int hudX;

    @JSONDescription("The y-coordinate for the center of this instrument on the HUD, in pixels.")
    public int hudY;

    @JSONDescription("Like scale, but for the HUD and Panel instead.")
    public float hudScale;

    @JSONDescription("If included and set, then MTS will try to grab this part number for any animation done by this instrument, unless the instrument already has a part number hard-coded.  Note that this will only happen if the animation is for a part, so instruments with non-part animations may safely be put in this slot.")
    public int optionalPartNumber;

    @JSONDescription("Setting this to true will move this instrument to the panel rather than the main HUD.  Useful in multi-engine vehicles where you don't want to clog the main HUD with a buch of tachometers.")
    public boolean placeOnPanel;

    @JSONDescription("Normally vehicles come bare-bones, but in the case you want to have the instrument in this position come with the vehicle, you can set this.  If an instrument name is put here, MTS will automatically add said instrument when the vehicle is spawned for the first time.  Note that MTS won't check if the instrument actually exists, so either keep things in-house, or require packs you use as a dependency.  Also note that it is possible to combine this with an inaccessible hudX and hudY coordinate to put the instrument off the HUD.  This will effectively make this instrument permanently attached to the vehicle.")
    public String defaultInstrument;

    @JSONDescription("If this is set, then the animations on this part slot will first use the animations for this object (not the part) from the rendering section instead of the animations defined here. If the specified object has applyAfter on it itself, then the animations will be gotten recursively until an applyAfter is not found.")
    public String applyAfter;

    @JSONDescription("This is a list of animatedObjects that can be used to move this instrument on the vehicle based on the animation values.  Note that the instrument animations are applied AFTER the instrument is moved to its initial potion and rotation, and all animations are applied relative to that orientation.  As such, you will have to adjust your parameters to accommodate this.")
    public List<JSONAnimationDefinition> animations;
}
