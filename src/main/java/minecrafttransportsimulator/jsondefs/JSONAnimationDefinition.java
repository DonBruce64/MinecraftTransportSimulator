package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONAnimationDefinition{
	@JSONRequired
	@JSONDescription("The type of animation this definition will perform.")
	public AnimationComponentType animationType;
	
	@JSONRequired
	@JSONDescription("The variable to use in animation.")
	public String variable;
	
	@JSONRequired(dependentField="animationType", dependentValues={"rotation"})
	@JSONDescription("The x, y, z position that this animation should be performed at.  If this is rotation, it is the rotation point.")
	public Point3d centerPoint;
	
	@JSONRequired(dependentField="animationType", dependentValues={"rotation", "translation"})
	@JSONDescription("This is the most complex part of the animation system.  In essence, it is the vector about which this part will animate.  For translations, this is simply the direction the part will translate in.  For rotations, this is the axis the rotation is performed around, in a counter-clockwise direction (Right-Hand Rule).\nNormally, the amount an object moves is fixed.  However, if you wish to scale this movement you can multiply all the points of the axis to adjust the scale. For example, using the door variable will only rotate a door by 1 degree (because the door variable goes from 0 to 1).  However, if you multiply the axis by 60, it will then rotate 60 degrees.  This can be done for any variable and on any axis.\nNote that multi-axis rotation gets a fair bit more complicated.  If you only want an object to rotate in one axis, such as a door that's perfectly vertical, you only need to put a 60 in the Y spot and this will rotate the door about the vector pointing in the +Y direction.  If you put a -1 in the Y spot, then it will rotate about the -Y direction, which simply means it will be clockwise rather than counter-clockwise.\nHowever, with multiple axis on complex models, you'll find this single-value method won't work, and you'll need to calculate the proper axis.  This is done by taking the centerPoint, and a known point along the axis of rotation, and calculating the angles between them for the X, Y, and Z directions.  For a two-axis rotation this can be done with some simple trigonometry by finding the angle and using a sine and cosine function to get the components.\nFor a three-axis rotation (a part of a model that whose rotation axis cannot be restricted to a two-dimensional plane) this gets significantly more complex and can lead to you dealing with polar coordinates.  If you chose to do this, you can, but do note that if you don't have a good grasp of geometry you will have issues telling MTS what exactly you want it to do.  If worst comes to worst, you can ask the Discord team for help, and grab one of their auto-angle calculators.  But make sure your model is done and good before you get this far!")
	public Point3d axis;
	
	@JSONDescription("The offset for this animation.  This will add-on to the value returned by the variable.  Clamps are applied to the total value of the variable plus the offset.")
	public float offset;
	
	@JSONDescription("If true, the value returned from the animation defined before this one will be applied to this animation, in addition to the specified offset.  This allows for multiple different variables to apply animations in the same direction with a global clamp.")
	public boolean addPriorOffset;
	
	@JSONRequired(dependentField="animationType", dependentValues={"visibility"})
	@JSONDescription("If included, MTS will clamp the total animation value (the value of variable, times the scale, plus the offsets) to always be greater than this value.  If the animation is a visibility, this is the lowest value the object will be visible at.  If the animation is an inhibitor, this is the lowest value at which animations will be inhibited.")
	public float clampMin;
	
	@JSONRequired(dependentField="animationType", dependentValues={"visibility"})
	@JSONDescription("Like clampMin, but for the max value.")
	public float clampMax;
	
	@JSONDescription("If true, the absolute value of the variable will be used rather than the actual value.  Note that negative movements are still possible via a negative rotation factor or a large negative offset.")
	public boolean absolute;
	
	@JSONDescription("The duration of this animation, in ticks.  Causes the animation to be interpolated over the duration for smooth movement.  Useful for things like doors.  Only works for variables that go from 0-1.")
	public int duration;
	
	@JSONDescription("How long this animation waits to start after the variable goes to 1, in ticks.  For example, an animation with a forwardsDelay of 20 and a duration of 40 would wait one second before moving, and then take two seconds to move its entire animation.")
	public int forwardsDelay;
	
	@JSONDescription("Like forwardsDelay, but for the end of the animation.  This delay is applied when the variable goes from 1 to 0.")
	public int reverseDelay;
	
	@JSONDescription("If true, this animation will skip the forward movement time specified in the duration and will instantly move to the end of the animation.  Useful when you want uni-directional animations.  Note that this does not prevent the movement delay from forwardsDelay from applying.  This is only for the duration.")
	public boolean skipForwardsMovement;
	
	@JSONDescription("Like skipForwardsMovement, but for the reverse movement.")
	public boolean skipReverseMovement;
	
	@JSONDescription("This sound will play when the animation starts its movement forwards.  This only happens at the start of the duration, not the start of the delay.  Format is [packID:soundName]")
	public String forwardsStartSound;
	
	@JSONDescription("This sound will play when the animation ends its movement forwards.  This only happens at the end of the duration, and won't occur if the animation reverses direction beforehand.")
	public String forwardsEndSound;
	
	@JSONDescription("Like forwardsStartSound, but for reverse.")
	public String reverseStartSound;
	
	@JSONDescription("Like forwardsEndSound, but for reverse.")
	public String reverseEndSound;
	
	public static enum AnimationComponentType{
		@JSONDescription("This animation moves this component in the X/Y/Z direction based on the axis value, with the component moving the total distance specified when the value of the variable is 1.")
		TRANSLATION,
		@JSONDescription("This animation rotates this component around the X/Y/Z-axis by the number of degrees specified.  This rotation is offset by the centerPoint parameter.  Note: most animations are done in sequence, where rotation is first done around the Y-axis, then the X-axis, and then the Z-axis.  However, for the rendering of OBJ model objects, the rotation is done all at once, with the rotation assuming that the axis parameter is a vector to rotate around and the length of that vector being the amount to rotate.  This won't matter in single-axis and some double-axis rotation, but it will cause differences in movement in triple-axis rotation.  In particular is the movement of parts on vehicles versus the movement of the OBJ model components.")
		ROTATION,
		@JSONDescription("This isn't as much an animation as it is a requirement for this component to be rendered.  If the value of the variable is outside the min and max clamps, the component will not be rendered.")
		VISIBILITY,
		@JSONDescription("This animation will block, or inhibit, any animations after it if the variable value is inside the min and max clamp.  Useful for disabling animations if things aren't present or in the right sequence.")
		INHIBITOR,
		@JSONDescription("Like Inhibitor, but in this case this animation will be resumed if it was inhibited.  There is no change if the animation wasn't inhibited to begin with.")
		ACTIVATOR;
	}
}
