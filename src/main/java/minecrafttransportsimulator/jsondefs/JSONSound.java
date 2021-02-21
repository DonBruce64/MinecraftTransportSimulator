package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONSound{
	@JSONRequired
	@JSONDescription("The name for this sound.  This tells MTS where to find it.  Format is packID:soundName.  All sounds should be located in the 'sounds' folder as the first folder underneath your main pack folder.")
	public String name;
	
	@JSONDescription("A listing of animation objects for the volume.  Leaving this blank will make for a volume of 1.0 and will make the sound always play, which you probably don't want.  Visibility transforms will turn the sound on and off, while translation and rotation will adjust the volume.  If no translation or rotation transforms are preesnt, the volume will be 1.0.\nTranslation will adjust the volume linearly.  Rotation will adjust the volume in a parabolic fashion.  Both of these use a formuala type of Y=f(x), with the offset being used to offset the value returned by the animation.\nFor example: if you have a sound like a road noise or wind noise, you would want that sound to normally be silent until hitting a specific speed, say 0.2.  You would then want it to increase in volume, up to a max of 1.0, when you hit 0.8.  As such, you'd use a translation variable, with the axis set to 1.66, and an offset of -0.332.  This would result in the linear formula V=1.66*speed - 0.332.  So up until you got to a speed of 0.2, the volume would be 0.  After that, it would increase linearly until you reached a speed of 0.8, and the volume became 1.0.  For rotations, which are parabolic, the formula is in the form of V=(variable - offset)^2.  Between these two, you have full control of the sound's volume.\nNote that volume is clamped to a minimum value of 0 when the sound is played, with sounds lower than 0 being mute.  You may add additional clamping via the clamping animations, but the resulting volume, even if not affected by the animation clamps, will still be clamped at a lower bound of 0 when the sound is actually played.")
	public List<JSONAnimationDefinition> volumeAnimations;
	
	@JSONDescription("Like volumeAnimations, but for pitch.  Leaving this blank will make for a pitch of 1.0.")
	public List<JSONAnimationDefinition> pitchAnimations;
	
	@JSONDescription("If the sound should loop, set this to true.  Be aware that unless the volume is set to 0 or below, or the sound is blocked via visibility variables or inhibitors, then it will keep playing forever and take up a sound slot!")
	public boolean looping;
	
	@JSONDescription("If the sound should repeat every tick, set this to true.  Mainly used for sounds on guns that need to be played every tick the gun fires, but may be used for other things.  Looping sounds is HIGHLY preferred to this if possible.")
	public boolean repeating;
}
