package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONSound {
    @JSONRequired
    @JSONDescription("The name for this sound.  This tells MTS where to find it.  Format is packID:soundName.  All sounds should be located in the 'sounds' folder as the first folder underneath your main pack folder.")
    public String name;

    @JSONDescription("A list of sounds to play instead of the main sound.  If this is present, then one of these at random will be played each time this sound is played rather than the normal sound.  Note that a paramter for name is still required to allow the audio system to track this sound, and should be unique to the entity this is defined on.")
    public List<String> soundVariations;

    @JSONRequired
    @JSONDescription("A required listing of animation objects for determining if this sound is active.  Visibility transforms will turn the sound on and off.  Inhibitor and activator transforms may be used in conjunction with these for advanced on/off logic.  Note that non-looping sounds are only played when all the animations in this block change the visibility state from 0 to 1.  If you want the sound to play every tick the visibility state is at 1, set repeating to true.")
    public List<JSONAnimationDefinition> activeAnimations;

    @JSONDescription("A listing of animation objects for the volume.  Leaving this blank will make for a volume of 1.0.  Translation and rotation transforms are used to adjust the volume.\nTranslation will adjust the volume linearly.  Rotation will adjust the volume in a parabolic fashion.  Both of these use a formuala type of Volume=function(variable), with the offset being used to offset the value returned by the function.\nTranslations are linear transforms, and use a formula in the form of: Volume=axis.y*variableValue + offset.  Rotations are parabolic, and use a formula in the form of: Volume=axis.x*(axis.y*variableValue - axis.z)^2 + offset.  Between these two, you have full control of the sound's volume.\nNote that volume is clamped to a minimum value of 0 when the sound is played, with sounds lower than 0 being mute.  You may add additional clamping via the clamping animations, but the resulting volume, even if not affected by the animation clamps, will still be clamped at a lower bound of 0 when the sound is actually played.\nAs an example: If you have a sound like a road noise or wind noise, you would want that sound to normally be silent until hitting a specific speed, say 0.2.  You would then want it to increase in volume, up to a max of 1.0, when you hit 0.8.  As such, you'd use a translation variable, with the axis set to [0, 0, 1.66], and an offset of -0.332.  This would result in the linear formula Volume=1.66*speed - 0.332.  So up until you got to a speed of 0.2, the volume would be 0.  After that, it would increase linearly until you reached a speed of 0.8, and the volume became 1.0.")
    public List<JSONAnimationDefinition> volumeAnimations;

    @JSONDescription("Like volumeAnimations, but for pitch.  Leaving this blank will make for a pitch of 1.0.")
    public List<JSONAnimationDefinition> pitchAnimations;

    @JSONDescription("If the sound should loop, set this to true.  Be aware that unless the volume is set to 0 or below, or the sound is blocked via visibility variables or inhibitors, then it will keep playing forever and take up a sound slot!")
    public boolean looping;

    @JSONDescription("Normally, sounds won't play if there's already a sound playing.  If you want this sound to be played every tick the activeAnimations say that it should be active, set this to true.  Mainly used for sounds on engines and wheels that can occur in the same tick back-to-back, but may be used for other things.  Looping sounds is HIGHLY preferred to this if possible.")
    public boolean forceSound;

    @JSONDescription("Normally, sounds are only checked every tick for playing.  However, some sounds, like guns with firing rates of 1 tick or less, or engine cylinder cam-based sounds, will occur more than once a tick.  You may set this to have these sounds play more than once a tick, but be warned that it will result in a drop in FPS, so only set this if absolutely required!")
    public boolean canPlayOnPartialTicks;

    @JSONDescription("This causes the sound to only play if the player is riding this entity and is in first-person.")
    public boolean isInterior;

    @JSONDescription("Like isInterior, but blocks this sound if the player is in first-person and riding this entity.")
    public boolean isExterior;

    @JSONDescription("Normally, all looping sounds have a doppler effect applied.  Setting this to true will block this.")
    public boolean blockDoppler;

    @JSONDescription("The x, y, z position that this animation should be performed at.  If this is rotation, it is the rotation point.")
    public Point3D centerPoint;

    @JSONDescription("An entry of x, y, and z coordinates that define the center point of where this sound will be played relative to the center of the object.  May be omitted if you just want the sound to play at the center.")
    public Point3D pos;

    @JSONDescription("The minimum distance for where this sound can be heard.")
    public double minDistance;

    @JSONDescription("The maximum distance as to where this sound can be heard.  If minDistance is set to 0, then the sound will have its volume scaled to be 100% at the origin, and 0% at this distance.  If minDistance is included, then no scaling will be performed.  If this and minDistance are left out, 0-64 is used.")
    public double maxDistance;

}
