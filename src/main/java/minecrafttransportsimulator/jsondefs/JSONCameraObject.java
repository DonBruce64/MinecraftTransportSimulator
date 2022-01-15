package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Orientation3d;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONCameraObject{
	@JSONRequired
	@JSONDescription("An entry of x, y, and z coordinates that define the center point of where this camera will be located on the entity.  Note that FOV means this value may not be 100% accurate, so you may need to fudge this value to make things work.")
	public Point3d pos;
	
	@JSONDescription("This parameter is optional.  If included, it defines the x, y, and z rotations for this camera.")
	public Orientation3d rot;
	
	@JSONDescription("This parameter is optional.  If included, MTS will set the player's FOV to this value when they are in this camera mode.  Useful for simulating zoom functions on scopes and sights.")
	public float fovOverride;
	
	@JSONDescription("This parameter is optional.  If included, MTS will render the specified texture as an overlay when this camera is active.  This overlay will also disable the hotbar and cross-hair rendering.  The format is [packID:path/to/texture]")
	public String overlay;
	
	@JSONDescription("A listing of one or more animation objects.  There are a few caveats with cameras, however:<br><br>Cameras do not support duration/delay, for obvious reasons.  This means that they do not support sounds, as those are tied to duration/delay code.<br><br>Cameras do not support the addPriorOffset flag, though they do support clamping.<br><br>Using the visibility animation will skip rendering the camera if the camera isn't “visible”.  This can be used to dynamically enable cameras, such as those for active guns or vehicle components.  Note that the camera index will not increment during this, so if you stop rendering camera #1, then MTS will switch to camera #2 instead. This can be helpful if you want cameras to replace each other for specific actions.")
	public List<JSONAnimationDefinition> animations;
}
